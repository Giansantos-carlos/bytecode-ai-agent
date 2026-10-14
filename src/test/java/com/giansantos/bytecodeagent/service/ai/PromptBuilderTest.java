package com.giansantos.bytecodeagent.service.ai;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PromptBuilderTest {

    @Test
    void buildsPromptWithUnitNameEngineAndCode() {
        String prompt = PromptBuilder.build("com/example/Login", "ASM", "ALOAD 0\nARETURN");

        assertThat(prompt)
                .contains("com/example/Login")
                .contains("ASM")
                .contains("ALOAD 0")
                .contains("JSON")
                .contains("vulnerabilities");
    }
}
