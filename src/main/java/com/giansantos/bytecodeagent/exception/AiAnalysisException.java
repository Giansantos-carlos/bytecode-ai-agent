package com.giansantos.bytecodeagent.exception;

/** Lancada quando a chamada a API da Anthropic falha ou retorna algo inesperado. */
public class AiAnalysisException extends RuntimeException {
    public AiAnalysisException(String message) {
        super(message);
    }

    public AiAnalysisException(String message, Throwable cause) {
        super(message, cause);
    }
}
