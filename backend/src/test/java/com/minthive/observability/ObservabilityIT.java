package com.minthive.observability;

import com.minthive.support.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 可观测性集成测试
 *
 * <p>功能描述：验证独立管理端口上的 Actuator 健康检查与业务指标真实可用，
 * 避免指标只在代码里注册但未随管理端口暴露</p>
 * <p>注意事项：管理端口在 it 配置中为 0（随机端口），通过 {@link LocalManagementPort} 获取；
 * 管理端口仅供内网监控，不参与 /api 业务路由</p>
 */
class ObservabilityIT extends BaseIntegrationTest {

    @LocalManagementPort
    private int managementPort;

    @Test
    void healthEndpointReportsUp() {
        ResponseEntity<String> response = new RestTemplate()
                .getForEntity(url("/actuator/health"), String.class);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody() != null && response.getBody().contains("\"status\":\"UP\""),
                "健康检查应返回 UP，实际: " + response.getBody());
    }

    @Test
    void businessMetricsAreExposed() {
        assertMetricExposed("minthive.audit.dropped");
        assertMetricExposed("minthive.message.push.failure");
        assertMetricExposed("minthive.ws.online");
    }

    /**
     * 断言某个指标已通过 /actuator/metrics 暴露
     *
     * @param name Micrometer 指标名
     */
    private void assertMetricExposed(String name) {
        ResponseEntity<String> response = new RestTemplate()
                .getForEntity(url("/actuator/metrics/" + name), String.class);

        assertEquals(200, response.getStatusCode().value(), "指标未暴露: " + name);
        assertTrue(response.getBody() != null && response.getBody().contains("\"name\":\"" + name + "\""),
                "指标响应缺少名称: " + response.getBody());
    }

    /**
     * 拼接管理端口地址
     *
     * @param path 管理端点路径
     * @return 完整 URL
     */
    private String url(String path) {
        return "http://127.0.0.1:" + managementPort + path;
    }
}
