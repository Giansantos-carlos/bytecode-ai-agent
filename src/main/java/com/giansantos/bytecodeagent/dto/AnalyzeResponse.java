package com.giansantos.bytecodeagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Resposta consolidada da analise de um artefato (.class/.jar/.apk)")
public record AnalyzeResponse(

        @Schema(description = "Nome do arquivo enviado", example = "app-release.apk")
        String artifact,

        @Schema(description = "Motor de extracao usado para o artefato", example = "ASM")
        String engine,

        @Schema(description = "Quantidade de unidades (classes) efetivamente analisadas")
        int classesAnalyzed,

        @Schema(description = "Resumo geral da analise")
        String overallSummary,

        @Schema(description = "Analise detalhada por classe")
        List<ClassAnalysis> classes
) {
}
