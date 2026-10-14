package com.giansantos.bytecodeagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jadx")
public record JadxProperties(String executable, long timeoutSeconds) {
}
