package com.giansantos.bytecodeagent.web;

import com.giansantos.bytecodeagent.config.AnthropicProperties;
import com.giansantos.bytecodeagent.config.JadxProperties;
import com.giansantos.bytecodeagent.dto.AnalyzeResponse;
import com.giansantos.bytecodeagent.dto.HealthStatus;
import com.giansantos.bytecodeagent.service.AnalysisService;
import com.giansantos.bytecodeagent.service.extract.ApkJadxExtractor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/analysis")
@Tag(name = "Analise", description = "Desmontagem/decompilacao de bytecode e deteccao de vulnerabilidades via IA")
public class AnalysisController {

    private final AnalysisService analysisService;
    private final AnthropicProperties anthropicProperties;
    private final JadxProperties jadxProperties;
    private final ApkJadxExtractor apkJadxExtractor;

    public AnalysisController(AnalysisService analysisService, AnthropicProperties anthropicProperties,
                               JadxProperties jadxProperties, ApkJadxExtractor apkJadxExtractor) {
        this.analysisService = analysisService;
        this.anthropicProperties = anthropicProperties;
        this.jadxProperties = jadxProperties;
        this.apkJadxExtractor = apkJadxExtractor;
    }

    @PostMapping(value = "/bytecode", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Analisa bytecode Java (.class ou .jar)",
            description = "Desmonta cada classe (bytecode JVM via ASM, estilo javap -c) e envia o resultado "
                    + "para a IA explicar a logica e apontar possiveis vulnerabilidades.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Analise concluida",
                    content = @Content(schema = @Schema(implementation = AnalyzeResponse.class))),
            @ApiResponse(responseCode = "400", description = "Arquivo invalido, nao suportado ou excede limites"),
            @ApiResponse(responseCode = "502", description = "Falha ao consultar a API da Anthropic"),
            @ApiResponse(responseCode = "503", description = "ANTHROPIC_API_KEY nao configurada")
    })
    public AnalyzeResponse analyzeBytecode(
            @Parameter(description = "Arquivo .class ou .jar", required = true)
            @RequestPart("file") MultipartFile file) throws IOException {
        return analysisService.analyze(file);
    }

    @PostMapping(value = "/apk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Analisa um APK Android via JADX",
            description = "Decompila o APK usando o JADX (ferramenta externa, precisa estar instalada) "
                    + "e envia as classes Java geradas para a IA explicar a logica e apontar vulnerabilidades.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Analise concluida",
                    content = @Content(schema = @Schema(implementation = AnalyzeResponse.class))),
            @ApiResponse(responseCode = "400", description = "Arquivo invalido"),
            @ApiResponse(responseCode = "502", description = "Falha ao consultar a API da Anthropic"),
            @ApiResponse(responseCode = "503", description = "JADX ou ANTHROPIC_API_KEY nao disponiveis")
    })
    public AnalyzeResponse analyzeApk(
            @Parameter(description = "Arquivo .apk", required = true)
            @RequestPart("file") MultipartFile file) throws IOException {
        return analysisService.analyze(file);
    }

    @GetMapping("/health")
    @Operation(summary = "Status do agente",
            description = "Indica se a chave da Anthropic e o executavel do JADX estao disponiveis no ambiente.")
    public HealthStatus health() {
        return new HealthStatus(
                StringUtils.hasText(anthropicProperties.apiKey()),
                apkJadxExtractor.isAvailable(),
                jadxProperties.executable());
    }
}
