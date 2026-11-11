package com.minthive.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 行为防刷限额配置
 *
 * <p>功能描述：绑定 application.yml 中的 rate-limit.* 配置，
 * 控制单用户每日发帖、评论、私信的次数上限</p>
 *
 * <p>注意事项：值小于等于 0 表示关闭该项限制</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "rate-limit")
public class ActionRateLimitProperties {

    /** 每日发帖上限 */
    private int postDaily = 50;

    /** 每日评论上限 */
    private int commentDaily = 200;

    /** 每日私信上限 */
    private int messageDaily = 100;
}
