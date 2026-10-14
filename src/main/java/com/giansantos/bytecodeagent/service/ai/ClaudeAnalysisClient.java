package com.giansantos.bytecodeagent.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giansantos.bytecodeagent.config.AnthropicProperties;
import com.giansantos.bytecodeagent.dto.VulnerabilityFinding;
import com.giansantos.bytecodeagent.exception.AiAnalysisException;
import com.giansantos.bytecodeagent.exception.AiNotConfiguredException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Cliente para a API de Messages da Anthropic (Claude), usado para explicar codigo e apontar vulnerabilidades. */
@Component
public class ClaudeAnalysisClient {

    private static final Logger log = LoggerFactory.getLogger(ClaudeAnalysisClient.class);
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final RestClient restClient;
    private final AnthropicProperties properties;
    private final ObjectMapper objectMapper;

    public ClaudeAnalysisClient(AnthropicProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder().baseUrl(properties.baseUrl()).build();
    }

    public AiAnalysisResult analyze(String unitName, String engine, String code) {
        if (!StringUtils.hasText(properties.apiKey())) {
            throw new AiNotConfiguredException(
                    "ANTHROPIC_API_KEY nao configurada. Defina a variavel de ambiente para habilitar a analise por IA.");
        }

        String prompt = PromptBuilder.build(unitName, engine, code);
        Map<String, Object> requestBody = Map.of(
                "model", properties.model(),
                "max_tokens", properties.maxOutputTokens(),
                "messages", List.of(Map.of("role", "user", "content", prompt)));

        Map<String, Object> response;
        try {
            response = restClient.post()
                    .uri("/messages")
                    .header("x-api-key", properties.apiKey())
                    .header("anthropic-version", ANTHROPIC_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {
                    });
        } catch (RestClientResponseException ex) {
            throw new AiAnalysisException(
                    "Falha ao chamar a API da Anthropic: " + ex.getStatusCode() + " - " + ex.getResponseBodyAsString(), ex);
        }

        String rawText = extractText(response);
        return parseResult(unitName, rawText);
    }

    @SuppressWarnings("unchecked")
    private String extractText(Map<String, Object> response) {
        Object content = response == null ? null : response.get("content");
        if (content instanceof List<?> blocks && !blocks.isEmpty() && blocks.get(0) instanceof Map<?, ?> block) {
            Object text = block.get("text");
            if (text != null) {
                return text.toString();
            }
        }
        throw new AiAnalysisException("Resposta inesperada da API da Anthropic: " + response);
    }

    private AiAnalysisResult parseResult(String unitName, String rawText) {
        String json = extractJsonObject(rawText);
        try {
            JsonNode node = objectMapper.readTree(json);
            String explanation = node.path("explanation").asText("");
            List<VulnerabilityFinding> findings = new ArrayList<>();
            for (JsonNode v : node.path("vulnerabilities")) {
                findings.add(new VulnerabilityFinding(
                        v.path("severity").asText("UNKNOWN"),
                        v.path("category").asText("unknown"),
                        v.path("description").asText(""),
                        v.path("evidence").asText("")));
            }
            return new AiAnalysisResult(explanation, findings);
        } catch (Exception ex) {
            log.warn("Nao foi possivel interpretar a resposta JSON da IA para '{}'; retornando texto bruto.", unitName);
            return new AiAnalysisResult(rawText, List.of());
        }
    }

    private String extractJsonObject(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }
}
