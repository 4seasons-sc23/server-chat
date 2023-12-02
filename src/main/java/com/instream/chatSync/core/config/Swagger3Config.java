package com.instream.chatSync.core.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class Swagger3Config {
    private final Environment env;

    @Autowired
    public Swagger3Config(Environment env) {
        this.env = env;
    }

    @Bean
    public OpenAPI openAPI() {
        Info info = new Info().title("CHAT API").description("CHAT API 명세서입니다.");

        return new OpenAPI().addServersItem(new Server().url(env.getProperty("spring.webflux.base-path"))).info(info);
    }
}
