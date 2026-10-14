package com.giansantos.bytecodeagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Resultado da analise de uma unidade de codigo (classe)")
public record ClassAnalysis(

        @Schema(description = "Nome da classe/unidade analisada", example = "com/example/LoginService")
        String unitName,

        @Schema(description = "Motor usado para extrair o codigo", example = "ASM")
        String engine,

        @Schema(description = "Codigo desmontado/decompilado enviado para a IA (pode estar truncado)")
        String code,

        @Schema(description = "Explicacao simplificada, gerada pela IA, da logica da unidade")
        String aiExplanation,

        @Schema(description = "Vulnerabilidades apontadas pela IA para esta unidade")
        List<VulnerabilityFinding> vulnerabilities
) {
}
