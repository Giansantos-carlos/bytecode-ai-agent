package com.giansantos.bytecodeagent.service.extract;

import com.giansantos.bytecodeagent.config.JadxProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Decompila um {@code .apk} Android usando o JADX (ferramenta externa, nao embarcada) e le as
 * classes Java geradas. Requer o executavel {@code jadx} disponivel no ambiente (ver {@link JadxProperties}).
 */
@Component
public class ApkJadxExtractor implements CodeExtractor {

    private static final Logger log = LoggerFactory.getLogger(ApkJadxExtractor.class);
    public static final String ENGINE = "JADX";

    private final JadxProperties properties;

    public ApkJadxExtractor(JadxProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean supports(String filename) {
        return filename.toLowerCase(Locale.ROOT).endsWith(".apk");
    }

    public boolean isAvailable() {
        try {
            Process process = new ProcessBuilder(properties.executable(), "--version")
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            return finished && process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public List<CodeUnit> extract(byte[] fileBytes, String filename, int maxUnits) throws IOException {
        if (!isAvailable()) {
            throw new UnsupportedOperationException(
                    "JADX nao encontrado (executavel '" + properties.executable() + "'). "
                            + "Instale o JADX (https://github.com/skylot/jadx) e configure a variavel "
                            + "de ambiente JADX_EXECUTABLE com o caminho do binario, ou adicione-o ao PATH.");
        }

        Path apkFile = Files.createTempFile("bytecode-agent-", ".apk");
        Path outputDir = Files.createTempDirectory("bytecode-agent-out-");
        try {
            Files.write(apkFile, fileBytes);
            runJadx(apkFile, outputDir);
            return collectJavaSources(outputDir, maxUnits);
        } finally {
            deleteRecursively(apkFile);
            deleteRecursively(outputDir);
        }
    }

    private void runJadx(Path apkFile, Path outputDir) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(
                properties.executable(), "-d", outputDir.toString(), apkFile.toString())
                .redirectErrorStream(true);
        Process process = builder.start();
        String output;
        try {
            output = new String(process.getInputStream().readAllBytes());
            boolean finished = process.waitFor(properties.timeoutSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException("JADX excedeu o tempo limite de " + properties.timeoutSeconds() + "s.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Execucao do JADX interrompida.", e);
        }
        if (process.exitValue() != 0) {
            throw new IllegalStateException("JADX falhou (exit " + process.exitValue() + "): "
                    + truncate(output, 2000));
        }
    }

    private List<CodeUnit> collectJavaSources(Path outputDir, int maxUnits) throws IOException {
        List<CodeUnit> units = new ArrayList<>();
        try (Stream<Path> files = Files.walk(outputDir)) {
            List<Path> javaFiles = files
                    .filter(p -> p.toString().endsWith(".java"))
                    .sorted()
                    .limit(maxUnits)
                    .toList();
            for (Path file : javaFiles) {
                String relative = outputDir.relativize(file).toString();
                String content = Files.readString(file);
                units.add(new CodeUnit(relative, ENGINE, content));
            }
        }
        return units;
    }

    private void deleteRecursively(Path root) {
        if (!Files.exists(root)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException e) {
                    log.warn("Falha ao remover arquivo temporario {}", p, e);
                }
            });
        } catch (IOException e) {
            log.warn("Falha ao limpar diretorio temporario {}", root, e);
        }
    }

    private String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
