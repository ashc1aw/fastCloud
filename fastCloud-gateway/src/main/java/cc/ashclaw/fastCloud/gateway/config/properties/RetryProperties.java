package cc.ashclaw.fastCloud.gateway.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 重试过滤器配置属性类
 *
 * <p>该类用于配置请求重试策略，支持Nacos配置中心的动态刷新。
 *
 * <p>主要配置项：
 * <ul>
 *   <li>enabled: 是否启用重试</li>
 *   <li>maxAttempts: 最大重试次数</li>
 *   <li>retryableStatusCodes: 可重试的HTTP状态码</li>
 *   <li>retryableMethods: 可重试的HTTP方法</li>
 *   <li>backoff: 退避策略配置</li>
 * </ul>
 *
 * <p>配置示例：
 * <pre>
 * retry:
 *   enabled: true
 *   max-attempts: 3
 *   retryable-status-codes: 500,502,503,504
 *   retryable-methods: GET,POST
 *   backoff:
 *     initial-interval: 100
 *     multiplier: 2.0
 *     max-interval: 5000
 * </pre>
 *
 * @author ashclaw
 * @since JDK 25
 */
@Data
@Configuration
@RefreshScope
@ConfigurationProperties(prefix = "retry")
public class RetryProperties {

    /** 是否启用重试功能 */
    private boolean enabled = true;

    /** 最大重试次数 */
    private int maxAttempts = 3;

    /** 可重试的状态码列表 */
    private List<Integer> retryableStatusCodes = new ArrayList<>();

    /** 可重试的HTTP方法列表 */
    private List<String> retryableMethods = new ArrayList<>();

    /** 退避策略配置 */
    private Backoff backoff = new Backoff();

    public RetryProperties() {
        this.retryableStatusCodes.add(500);
        this.retryableStatusCodes.add(502);
        this.retryableStatusCodes.add(503);
        this.retryableStatusCodes.add(504);
        this.retryableMethods.add("GET");
        this.retryableMethods.add("POST");
        this.retryableMethods.add("PUT");
        this.retryableMethods.add("DELETE");
    }

    /**
     * 退避策略配置
     */
    @Data
    @NoArgsConstructor
    public static class Backoff {
        /** 初始间隔时间（毫秒） */
        private long initialInterval = 100;

        /** 间隔时间倍增因子 */
        private double multiplier = 2.0;

        /** 最大间隔时间（毫秒） */
        private long maxInterval = 5000;
    }
}