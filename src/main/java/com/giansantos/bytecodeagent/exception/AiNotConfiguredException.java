package com.giansantos.bytecodeagent.exception;

/** Lancada quando a chave da Anthropic nao esta configurada no ambiente. */
public class AiNotConfiguredException extends RuntimeException {
    public AiNotConfiguredException(String message) {
        super(message);
    }
}
