package com.giansantos.bytecodeagent.service.extract;

/** Uma unidade de codigo extraida de um artefato (uma classe), pronta para ser analisada pela IA. */
public record CodeUnit(String name, String engine, String source) {
}
