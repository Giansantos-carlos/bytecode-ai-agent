package com.giansantos.bytecodeagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BytecodeAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(BytecodeAgentApplication.class, args);
    }
}
