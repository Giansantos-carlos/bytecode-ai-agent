package com.giansantos.bytecodeagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Status de disponibilidade das integracoes do agente")
public record HealthStatus(

        @Schema(description = "Indica se ANTHROPIC_API_KEY esta configurada")
        boolean anthropicConfigured,

        @Schema(description = "Indica se o executavel do JADX foi encontrado (necessario para /apk)")
        boolean jadxAvailable,

        @Schema(description = "Executavel/caminho do JADX configurado", example = "jadx")
        String jadxExecutable
) {
}
