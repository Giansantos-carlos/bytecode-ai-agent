package com.giansantos.bytecodeagent.service;

import com.giansantos.bytecodeagent.config.AnalysisProperties;
import com.giansantos.bytecodeagent.dto.AnalyzeResponse;
import com.giansantos.bytecodeagent.dto.VulnerabilityFinding;
import com.giansantos.bytecodeagent.fixture.Sample;
import com.giansantos.bytecodeagent.service.ai.AiAnalysisResult;
import com.giansantos.bytecodeagent.service.ai.ClaudeAnalysisClient;
import com.giansantos.bytecodeagent.service.extract.JarBytecodeExtractor;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class AnalysisServiceTest {

    private final AnalysisProperties properties = new AnalysisProperties(15, 6000, 50L * 1024 * 1024);
    private final JarBytecodeExtractor jarExtractor = new JarBytecodeExtractor(properties);
    private final ClaudeAnalysisClient aiClient = Mockito.mock(ClaudeAnalysisClient.class);
    private final AnalysisService analysisService = new AnalysisService(List.of(jarExtractor), aiClient, properties);

    @Test
    void analyzesJarAndAggregatesAiFindings() throws IOException {
        when(aiClient.analyze(any(), any(), any())).thenReturn(new AiAnalysisResult(
                "Metodo concatena uma saudacao ao nome recebido.",
                List.of(new VulnerabilityFinding("LOW", "info-leak", "Retorna dado de entrada sem sanitizar.", "greet"))));

        byte[] jarBytes = buildJarWith(Sample.class);
        MockMultipartFile file = new MockMultipartFile("file", "app.jar", "application/java-archive", jarBytes);

        AnalyzeResponse response = analysisService.analyze(file);

        assertThat(response.artifact()).isEqualTo("app.jar");
        assertThat(response.engine()).isEqualTo("ASM");
        assertThat(response.classesAnalyzed()).isEqualTo(1);
        assertThat(response.classes()).hasSize(1);
        assertThat(response.classes().getFirst().vulnerabilities()).hasSize(1);
        assertThat(response.overallSummary()).contains("1 unidade(s)").contains("1 possivel");
        Mockito.verify(aiClient).analyze(any(), any(), any());
    }

    @Test
    void rejectsUnsupportedFileType() {
        MockMultipartFile file = new MockMultipartFile("file", "app.txt", "text/plain", "hi".getBytes());

        assertThat(org.assertj.core.api.Assertions.catchThrowable(() -> analysisService.analyze(file)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nao suportado");
    }

    private byte[] buildJarWith(Class<?> clazz) throws IOException {
        String resource = "/" + clazz.getName().replace('.', '/') + ".class";
        byte[] classBytes;
        try (InputStream in = clazz.getResourceAsStream(resource)) {
            assertThat(in).isNotNull();
            classBytes = in.readAllBytes();
        }
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
