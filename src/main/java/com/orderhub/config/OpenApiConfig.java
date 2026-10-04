package com.orderhub.config;

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

    @Bean
    public OpenAPI orderHubOpenAPI(@Value("${server.port:8080}") String serverPort) {
        return new OpenAPI()
                .info(new Info()
                        .title("OrderHub - Omnichannel OMS Core Engine API")
                        .version("1.0.0")
                        .description("Hệ thống quản lý đơn hàng và tồn kho đa kênh (OMS) tập trung: " +
                                "Hỗ trợ xử lý bất đồng bộ, chống Race Condition khi đặt hàng, " +
                                "tự động hủy đơn treo quá hạn và lưu vết sổ cái kho (Stock Ledger).")
                        .contact(new Contact()
                                .name("OrderHub Engineering Team")
                                .email("dev@orderhub.local"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server().url("http://localhost:" + serverPort).description("Local Development Server"),
                        new Server().url("https://orderhub-api.onrender.com").description("Production Cloud Server")
                ));
    }
}