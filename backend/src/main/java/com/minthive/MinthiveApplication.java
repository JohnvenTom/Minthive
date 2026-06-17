package com.minthive;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * MintHive 兴趣社交平台后端启动类
 *
 * <p>功能描述：SpringBoot 应用入口，启用异步支持，扫描 MyBatis Mapper 接口</p>
 * <p>注意事项：MapperScan 路径必须覆盖所有 mapper 子包</p>
 */
@SpringBootApplication
@EnableAsync
@MapperScan("com.minthive.mapper")
public class MinthiveApplication {

    /**
     * 主方法，启动 SpringBoot 应用
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(MinthiveApplication.class, args);
        System.out.println("====================================");
        System.out.println("  MintHive 兴趣社交平台后端启动成功  ");
        System.out.println("  访问地址: http://localhost:8080    ");
        System.out.println("  接口文档: http://localhost:8080/doc.html");
        System.out.println("====================================");
    }
}
