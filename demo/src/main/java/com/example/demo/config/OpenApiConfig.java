package com.example.demo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("NettoApi - Swedish Tax Calculator")
                        .version("1.0.0")
                        .description("""
                                API for calculating Swedish net salary (nettolön) from gross salary.
                                
                                Features:
                                - Municipal and regional tax calculation
                                - State tax (statlig skatt) for high incomes
                                - Basic deduction (grundavdrag)
                                - Job tax credit (jobbskatteavdrag)
                                - Church fee (kyrkoavgift) for members
                                - Burial fee (begravningsavgift)
                                
                                Designed for integration with MuleSoft API Gateway.
                                """)
                        .contact(new Contact()
                                .name("NettoApi Team")
                                .email("api@example.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local development server")
                ));
    }
}
