package com.giansantos.bytecodeagent.service;

import com.giansantos.bytecodeagent.config.AnalysisProperties;
import com.giansantos.bytecodeagent.dto.AnalyzeResponse;
import com.giansantos.bytecodeagent.dto.ClassAnalysis;
import com.giansantos.bytecodeagent.service.ai.AiAnalysisResult;
import com.giansantos.bytecodeagent.service.ai.ClaudeAnalysisClient;
import com.giansantos.bytecodeagent.service.extract.CodeExtractor;
import com.giansantos.bytecodeagent.service.extract.CodeUnit;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Orquestra extracao de codigo + analise por IA para um artefato enviado. */
@Service
public class AnalysisService {

    private final List<CodeExtractor> extractors;
    private final ClaudeAnalysisClient aiClient;
    private final AnalysisProperties analysisProperties;

    public AnalysisService(List<CodeExtractor> extractors, ClaudeAnalysisClient aiClient,
                            AnalysisProperties analysisProperties) {
        this.extractors = extractors;
        this.aiClient = aiClient;
        this.analysisProperties = analysisProperties;
    }

    public AnalyzeResponse analyze(MultipartFile file) throws IOException {
        String filename = requireFilename(file);
        CodeExtractor extractor = extractors.stream()
                .filter(e -> e.supports(filename))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tipo de arquivo nao suportado: " + filename + ". Use .class, .jar ou .apk."));

        List<CodeUnit> units = extractor.extract(file.getBytes(), filename, analysisProperties.maxClassesPerRequest());
        if (units.isEmpty()) {
            throw new IllegalArgumentException("Nenhuma classe pode ser extraida de " + filename);
        }

        List<ClassAnalysis> results = analyzeUnits(units);
        String summary = buildSummary(results);
        return new AnalyzeResponse(filename, units.getFirst().engine(), results.size(), summary, results);
    }

    private List<ClassAnalysis> analyzeUnits(List<CodeUnit> units) {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<ClassAnalysis>> futures = units.stream()
                    .map(unit -> executor.submit(() -> analyzeUnit(unit)))
                    .toList();

            List<ClassAnalysis> results = new ArrayList<>();
            for (Future<ClassAnalysis> future : futures) {
                results.add(future.get());
            }
            return results;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Analise interrompida.", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("Falha ao analisar unidade de codigo.", cause);
        }
    }

    private ClassAnalysis analyzeUnit(CodeUnit unit) {
        String truncated = truncate(unit.source(), analysisProperties.maxSnippetChars());
        AiAnalysisResult result = aiClient.analyze(unit.name(), unit.engine(), truncated);
        return new ClassAnalysis(unit.name(), unit.engine(), truncated, result.explanation(), result.vulnerabilities());
    }

    private String truncate(String source, int max) {
        return source.length() <= max ? source : source.substring(0, max) + "\n/* ... truncado ... */";
    }

    private String buildSummary(List<ClassAnalysis> results) {
        long vulnCount = results.stream().mapToLong(r -> r.vulnerabilities().size()).sum();
        return "Analisadas " + results.size() + " unidade(s); " + vulnCount
                + " possivel(is) vulnerabilidade(s) identificada(s) pela IA.";
    }

    private String requireFilename(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("Arquivo enviado sem nome.");
        }
        return name;
    }
}
