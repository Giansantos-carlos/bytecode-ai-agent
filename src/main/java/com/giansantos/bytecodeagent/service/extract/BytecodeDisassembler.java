package com.giansantos.bytecodeagent.service.extract;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.TraceClassVisitor;
import org.objectweb.asm.util.Textifier;

import java.io.PrintWriter;
import java.io.StringWriter;

/** Desmonta bytecode JVM bruto (.class) em uma listagem legivel, no estilo "javap -c". */
public final class BytecodeDisassembler {

    public static final String ENGINE = "ASM";

    private BytecodeDisassembler() {
    }

    public static String disassemble(byte[] classBytes) {
        ClassReader reader = new ClassReader(classBytes);
        StringWriter out = new StringWriter();
        TraceClassVisitor visitor = new TraceClassVisitor(null, new Textifier(), new PrintWriter(out));
        reader.accept(visitor, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return out.toString();
    }
}
