package cc.ashclaw.fastCloud.gateway.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Gateway自定义指标收集器
 *
 * <p>该类用于收集和暴露Gateway特有的业务指标，
 * 配合Prometheus实现监控可视化。
 *
 * <p>主要指标：
 * <ul>
 *   <li>gateway.blacklist.blocked - 黑名单拦截次数</li>
 *   <li>gateway.auth.failures - 认证失败次数</li>
 *   <li>gateway.auth.success - 认证成功次数</li>
 *   <li>gateway.requests.total - 总请求数</li>
 *   <li>gateway.requests.slow - 慢请求数</li>
 *   <li>gateway.requests.duration - 请求处理时间</li>
 * </ul>
 *
 * @author ashclaw
 * @since JDK 25
 */
@Component
public class GatewayMetrics {

    private final Counter blockedCounter;
    private final Counter authFailureCounter;
    private final Counter authSuccessCounter;
    private final Counter totalRequestsCounter;
    private final Counter slowRequestsCounter;
    private final Timer requestDurationTimer;

    public GatewayMetrics(MeterRegistry registry) {
        this.blockedCounter = Counter.builder("gateway.blacklist.blocked")
                .description("黑名单拦截次数")
                .tag("type", "blacklist")
                .register(registry);

        this.authFailureCounter = Counter.builder("gateway.auth.failures")
                .description("认证失败次数")
                .tag("type", "auth")
                .register(registry);

        this.authSuccessCounter = Counter.builder("gateway.auth.success")
                .description("认证成功次数")
                .tag("type", "auth")
                .register(registry);

        this.totalRequestsCounter = Counter.builder("gateway.requests.total")
                .description("总请求数")
                .register(registry);

        this.slowRequestsCounter = Counter.builder("gateway.requests.slow")
                .description("慢请求数")
                .register(registry);

        this.requestDurationTimer = Timer.builder("gateway.requests.duration")
                .description("请求处理时间")
                .register(registry);
    }

    /**
     * 增加黑名单拦截计数
     */
    public void incrementBlocked() {
        blockedCounter.increment();
    }

    /**
     * 增加认证失败计数
     */
    public void incrementAuthFailure() {
        authFailureCounter.increment();
    }

    /**
     * 增加认证成功计数
     */
    public void incrementAuthSuccess() {
        authSuccessCounter.increment();
    }

    /**
     * 增加总请求计数
     */
    public void incrementTotalRequests() {
        totalRequestsCounter.increment();
    }

    /**
     * 增加慢请求计数
     */
    public void incrementSlowRequests() {
        slowRequestsCounter.increment();
    }

    /**
     * 记录请求处理时间
     */
    public Timer.Sample startTimer() {
        return Timer.start();
    }

    /**
     * 停止计时器并记录时间
     */
    public void recordDuration(Timer.Sample sample) {
        sample.stop(requestDurationTimer);
    }
}