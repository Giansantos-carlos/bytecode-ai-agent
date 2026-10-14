package com.giansantos.bytecodeagent.service.extract;

import com.giansantos.bytecodeagent.fixture.Sample;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

class BytecodeDisassemblerTest {

    @Test
    void disassemblesKnownMethodAndInstructions() throws IOException {
        byte[] classBytes = readClassBytes(Sample.class);

        String listing = BytecodeDisassembler.disassemble(classBytes);

        assertThat(listing)
                .contains("greet")
                .contains("ARETURN");
    }

    private byte[] readClassBytes(Class<?> clazz) throws IOException {
        String resource = "/" + clazz.getName().replace('.', '/') + ".class";
        try (InputStream in = clazz.getResourceAsStream(resource)) {
            assertThat(in).as("class resource " + resource).isNotNull();
            return in.readAllBytes();
        }
    }
}
