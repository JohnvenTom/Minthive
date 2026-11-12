package com.minthive.audit;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 操作审计配置
 *
 * <p>功能描述：控制审计开关、异步队列、快照上限、可信代理与排除表</p>
 * <p>注意事项：trusted-proxies 只填写直接连接后端的代理地址，填写错误会导致客户端 IP 解析失真</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.audit")
public class AuditProperties {

    /** 是否启用操作审计 */
    private boolean enabled = true;

    /** 异步写入队列容量 */
    private int queueCapacity = 5000;

    /** 单批落库条数 */
    private int batchSize = 200;

    /** 单条 before/after 快照上限（字节） */
    private int snapshotMaxBytes = 64 * 1024;

    /** 可信代理地址列表（精确匹配 RemoteAddr） */
    private List<String> trustedProxies = new ArrayList<>();

    /** 需要排除审计的表名（大小写不敏感） */
    private List<String> excludeTables = new ArrayList<>();
}
