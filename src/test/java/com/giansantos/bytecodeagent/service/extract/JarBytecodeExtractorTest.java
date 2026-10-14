package com.giansantos.bytecodeagent.service.extract;

import com.giansantos.bytecodeagent.config.AnalysisProperties;
import com.giansantos.bytecodeagent.fixture.Sample;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JarBytecodeExtractorTest {

    private final AnalysisProperties properties = new AnalysisProperties(15, 6000, 50L * 1024 * 1024);
    private final JarBytecodeExtractor extractor = new JarBytecodeExtractor(properties);

    @Test
    void supportsClassAndJarFilesOnly() {
        assertThat(extractor.supports("Sample.class")).isTrue();
        assertThat(extractor.supports("app.jar")).isTrue();
        assertThat(extractor.supports("app.apk")).isFalse();
    }

    @Test
    void extractsSingleClassFile() throws IOException {
        byte[] classBytes = readClassBytes(Sample.class);

        List<CodeUnit> units = extractor.extract(classBytes, "Sample.class", 10);

        assertThat(units).hasSize(1);
        assertThat(units.getFirst().engine()).isEqualTo("ASM");
        assertThat(units.getFirst().source()).contains("greet");
    }

    @Test
    void extractsClassesFromJar() throws IOException {
        byte[] jarBytes = buildJarWith(Sample.class);

        List<CodeUnit> units = extractor.extract(jarBytes, "app.jar", 10);

        assertThat(units).hasSize(1);
        assertThat(units.getFirst().name()).isEqualTo("com/giansantos/bytecodeagent/fixture/Sample");
        assertThat(units.getFirst().source()).contains("ARETURN");
    }

    @Test
    void rejectsJarExceedingDecompressedByteBudget() throws IOException {
        AnalysisProperties tinyBudget = new AnalysisProperties(15, 6000, 10L);
        JarBytecodeExtractor strictExtractor = new JarBytecodeExtractor(tinyBudget);
        byte[] jarBytes = buildJarWith(Sample.class);

        assertThatThrownBy(() -> strictExtractor.extract(jarBytes, "app.jar", 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("zip bomb");
    }

    private byte[] readClassBytes(Class<?> clazz) throws IOException {
        String resource = "/" + clazz.getName().replace('.', '/') + ".class";
        try (InputStream in = clazz.getResourceAsStream(resource)) {
            assertThat(in).isNotNull();
            return in.readAllBytes();
        }
    }

    private byte[] buildJarWith(Class<?> clazz) throws IOException {
        byte[] classBytes = readClassBytes(clazz);
        String entryName = clazz.getName().replace('.', '/') + ".class";

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            zos.putNextEntry(new ZipEntry(entryName));
            zos.write(classBytes);
            zos.closeEntry();
        }
        return baos.toByteArray();
    }
}
