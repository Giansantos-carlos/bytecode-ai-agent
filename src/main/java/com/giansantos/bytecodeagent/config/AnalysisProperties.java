package com.giansantos.bytecodeagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.analysis")
public record AnalysisProperties(int maxClassesPerRequest, int maxSnippetChars, long maxZipBytes) {
}
