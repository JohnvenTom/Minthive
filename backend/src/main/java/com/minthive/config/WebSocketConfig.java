package com.minthive.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * WebSocket 配置类
 *
 * <p>功能描述：启用 WebSocket 支持，注册 ServerEndpointExporter 用于自动扫描 @ServerEndpoint 注解</p>
 * <p>注意事项：仅在使用内置容器时需要此 Bean，外置容器部署时需移除</p>
 */
@Configuration
public class WebSocketConfig {

    /**
     * 注册 ServerEndpointExporter，启用 WebSocket 端点自动扫描
     *
     * @return ServerEndpointExporter 实例
     */
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}
