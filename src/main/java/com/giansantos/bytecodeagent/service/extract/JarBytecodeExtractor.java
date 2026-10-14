package com.giansantos.bytecodeagent.service.extract;

import com.giansantos.bytecodeagent.config.AnalysisProperties;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Extrai e desmonta bytecode JVM de arquivos {@code .class} soltos ou de dentro de um {@code .jar}.
 * Protege contra "zip bomb" limitando o total de bytes descomprimidos lidos de um jar.
 */
@Component
public class JarBytecodeExtractor implements CodeExtractor {

    private final AnalysisProperties properties;

    public JarBytecodeExtractor(AnalysisProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean supports(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        return lower.endsWith(".class") || lower.endsWith(".jar");
    }

    @Override
    public List<CodeUnit> extract(byte[] fileBytes, String filename, int maxUnits) throws IOException {
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".class")) {
            String className = filename.substring(0, filename.length() - ".class".length());
            return List.of(new CodeUnit(className, BytecodeDisassembler.ENGINE, BytecodeDisassembler.disassemble(fileBytes)));
        }
        return extractFromJar(fileBytes, maxUnits);
    }

    private List<CodeUnit> extractFromJar(byte[] jarBytes, int maxUnits) throws IOException {
        List<CodeUnit> units = new ArrayList<>();
        long budgetRemaining = properties.maxZipBytes();

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(jarBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null && units.size() < maxUnits) {
                String name = entry.getName();
                if (entry.isDirectory() || !name.endsWith(".class")
                        || name.equals("module-info.class") || name.endsWith("package-info.class")) {
                    continue;
                }

                byte[] classBytes;
                try {
                    classBytes = readCapped(zis, budgetRemaining);
                } catch (IOException tooBig) {
                    throw new IllegalArgumentException(
                            "O arquivo .jar excede o limite de " + properties.maxZipBytes()
                                    + " bytes descomprimidos permitido (possivel zip bomb).");
                }
                budgetRemaining -= classBytes.length;

                String className = name.substring(0, name.length() - ".class".length());
                units.add(new CodeUnit(className, BytecodeDisassembler.ENGINE, BytecodeDisassembler.disassemble(classBytes)));
            }
        }
        return units;
    }

    /** Le o stream ate o fim do entry atual, abortando se exceder o orcamento de bytes permitido. */
    private byte[] readCapped(InputStream in, long budget) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        long total = 0;
        int read;
        while ((read = in.read(chunk)) != -1) {
            total += read;
            if (total > budget) {
                throw new IOException("budget exceeded");
            }
            buffer.write(chunk, 0, read);
        }
        return buffer.toByteArray();
    }
}
