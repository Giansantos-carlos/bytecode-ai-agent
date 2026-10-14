package com.giansantos.bytecodeagent.service.ai;

/** Monta o prompt enviado a IA para explicar uma unidade de codigo e apontar vulnerabilidades. */
public final class PromptBuilder {

    private PromptBuilder() {
    }

    public static String build(String unitName, String engine, String code) {
        return """
                Voce e um assistente de seguranca especializado em engenharia reversa defensiva.
                Abaixo esta um trecho de codigo (%s) extraido da unidade "%s" por meio do motor %s.

                Responda APENAS com um objeto JSON valido, sem markdown e sem texto adicional, no formato:
                {
                  "explanation": "explicacao simplificada, em portugues, do que a logica desta unidade faz",
                  "vulnerabilities": [
                    {
                      "severity": "LOW|MEDIUM|HIGH|CRITICAL",
                      "category": "categoria curta, ex: hardcoded-secret, command-injection, insecure-crypto, path-traversal, unsafe-deserialization",
                      "description": "descricao da vulnerabilidade e por que e um risco",
                      "evidence": "trecho especifico do codigo que motivou o achado"
                    }
                  ]
                }

                Se nenhuma vulnerabilidade for encontrada, retorne "vulnerabilities": [].

                Codigo:
                ---
                %s
                ---
                """.formatted(engine, unitName, engine, code);
    }
}
