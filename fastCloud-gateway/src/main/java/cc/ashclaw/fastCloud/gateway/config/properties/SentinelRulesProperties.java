package cc.ashclaw.fastCloud.gateway.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Sentinel限流规则配置属性类
 *
 * <p>该类用于绑定和读取application.yml中的sentinel.rules配置项，
 * 支持Nacos配置中心的动态刷新功能。
 *
 * <p>主要配置项：
 * <ul>
 *   <li>flowRules: 流量控制规则</li>
 *   <li>degradeRules: 降级规则</li>
 *   <li>systemRules: 系统规则</li>
 *   <li>authorityRules: 授权规则</li>
 * </ul>
 *
 * <p>配置示例：
 * <pre>
 * sentinel:
 *   rules:
 *     flow:
 *       - resource: "/api/users"
 *         count: 100
 *         grade: 1
 *         strategy: 0
 *     degrade:
 *       - resource: "/api/users"
 *         count: 5
 *         time-window: 10
 * </pre>
 *
 * @author ashclaw
 * @since JDK 25
 */
@Data
@NoArgsConstructor
@Configuration
@RefreshScope
@ConfigurationProperties(prefix = "sentinel.rules")
public class SentinelRulesProperties {

    /** 流量控制规则列表 */
    private List<FlowRule> flow = new ArrayList<>();

    /** 降级规则列表 */
    private List<DegradeRule> degrade = new ArrayList<>();

    /** 系统规则列表 */
    private List<SystemRule> system = new ArrayList<>();

    /** 授权规则列表 */
    private List<AuthorityRule> authority = new ArrayList<>();

    /**
     * 流量控制规则
     */
    @Data
    @NoArgsConstructor
    public static class FlowRule {
        /**
         * 资源名（请求路径）
         */
        private String resource;

        /**
         * 限流阈值
         */
        private int count;

        /**
         * 限流阈值类型
         * 0-按并发线程数，1-按QPS
         */
        private int grade = 1;

        /**
         * 流控模式
         * 0-直接，1-关联，2-排队
         */
        private int strategy = 0;

        /**
         * 流控效果
         * 0-快速失败，1-Warm Up，2-排队等待
         */
        private int controlBehavior = 0;

        /**
         * 预热时长（秒），当controlBehavior为1时生效
         */
        private int warmUpPeriodSec = 10;

        /**
         * 排队超时时间（毫秒），当controlBehavior为2时生效
         */
        private int maxQueueingTimeMs = 500;
    }

    /**
     * 降级规则
     */
    @Data
    @NoArgsConstructor
    public static class DegradeRule {
        /**
         * 资源名（请求路径）
         */
        private String resource;

        /**
         * 熔断触发最小请求数
         */
        private int count = 5;

        /**
         * 熔断时长（秒）
         */
        private int timeWindow = 10;

        /**
         * 降级策略
         * 0-慢调用比例，1-异常比例，2-异常数
         */
        private int grade = 0;

        /**
         * 慢调用比例阈值（秒），当grade为0时生效
         */
        private double slowRatioThreshold = 1.0;

        /**
         * 最小请求数，当grade为0或1时生效
         */
        private int minRequestAmount = 5;

        /**
         * 统计时长（毫秒）
         */
        private int statIntervalMs = 1000;
    }

    /**
     * 系统规则
     */
    @Data
    @NoArgsConstructor
    public static class SystemRule {
        /**
         * 系统负载阈值
         */
        private double qps = 10000;

        /**
         * 最大并发数
         */
        private int maxThread = 10000;

        /**
         * CPU使用率阈值（0-1）
         */
        private double cpuMax = 0.9;

        /**
         * 平均响应时间阈值（毫秒）
         */
        private double avgRt = 10;

        /**
         * 入口QPS阈值
         */
        private double highestSystemLoad = 3.2;
    }

    /**
     * 授权规则
     */
    @Data
    @NoArgsConstructor
    public static class AuthorityRule {
        /**
         * 资源名（请求路径）
         */
        private String resource;

        /**
         * 授权类型
         * 0-白名单，1-黑名单
         */
        private int type = 0;

        /**
         * 调用来源列表，多个用逗号分隔
         */
        private String limitApp = "default";
    }
}