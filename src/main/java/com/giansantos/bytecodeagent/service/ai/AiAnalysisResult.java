package com.giansantos.bytecodeagent.service.ai;

import com.giansantos.bytecodeagent.dto.VulnerabilityFinding;

import java.util.List;

public record AiAnalysisResult(String explanation, List<VulnerabilityFinding> vulnerabilities) {
}
