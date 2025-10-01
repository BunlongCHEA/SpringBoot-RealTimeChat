package com.project.realtimechat.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class SwaggerConfig {
	@Value("${server.port:8080}")
    private String serverPort;
	
	@Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Real Time Chat API")
                        .description("REST API for Real Time Chat Application")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("BunlongCHEA")
                                .email("bunlong@example.com")
                                .url("https://github.com/BunlongCHEA"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Development Server"),
                        new Server()
                                .url("https://your-production-url.com")
                                .description("Production Server")
                ));
    }
	
	
}
