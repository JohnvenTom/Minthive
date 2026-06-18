package com.minthive;

import com.minthive.config.EnvLoader;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.ai.autoconfigure.openai.OpenAiAutoConfiguration;

/**
 * MintHive 兴趣社交平台后端启动类
 *
 * <p>功能描述：SpringBoot 应用入口，启用异步支持，扫描 MyBatis Mapper 接口</p>
 * <p>注意事项：排除 OpenAiAutoConfiguration，项目使用自定义 AiService 双模式实现</p>
 */
@SpringBootApplication(exclude = {OpenAiAutoConfiguration.class})
@EnableAsync
@MapperScan("com.minthive.mapper")
public class MinthiveApplication {

    /**
     * 主方法，启动 SpringBoot 应用
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        // 在 Spring 启动前加载 .env 环境变量，使 yml 中的 ${ENV_KEY:默认值} 能正确解析
        EnvLoader.load();
        SpringApplication.run(MinthiveApplication.class, args);
        System.out.println("====================================");
        System.out.println("  MintHive 兴趣社交平台后端启动成功  ");
        System.out.println("  访问地址: http://localhost:8080    ");
        System.out.println("  接口文档: http://localhost:8080/doc.html");
        System.out.println("====================================");
    }
}
