package com.giansantos.bytecodeagent.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI bytecodeAgentOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Bytecode & Disassembly AI Analysis Agent")
                        .description("""
                                Agente de IA para analise de bytecode e desmontagem de binarios Java \
                                (.class/.jar) e Android (.apk, via JADX). Desmonta o codigo, envia os \
                                trechos para a API da Anthropic (Claude) e retorna explicacoes \
                                simplificadas da logica oculta e possiveis vulnerabilidades de \
                                seguranca. Destinado a engenharia reversa defensiva, auditoria de \
                                codigo e fins educacionais.""")
                        .version("1.0.0")
                        .contact(new Contact().name("Gian Santos").email("giansantos.carlos@gmail.com")));
    }
}
