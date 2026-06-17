package com.minthive.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j / OpenAPI3 文档配置类
 *
 * <p>功能描述：配置 Swagger 接口文档元信息，访问地址 /doc.html</p>
 * <p>注意事项：生产环境建议关闭文档以避免接口泄露</p>
 */
@Configuration
public class Knife4jConfig {

    /**
     * 构建 OpenAPI 文档信息
     *
     * @return OpenAPI 实例
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MintHive 兴趣社交平台 API 文档")
                        .description("MintHive 兴趣社交平台后端接口文档")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("MintHive Team")
                                .email("dev@minthive.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}
