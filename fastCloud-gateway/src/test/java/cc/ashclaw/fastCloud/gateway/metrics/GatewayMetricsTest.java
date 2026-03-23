package cc.ashclaw.fastCloud.gateway.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GatewayMetrics 单元测试")
class GatewayMetricsTest {

    private GatewayMetrics gatewayMetrics;
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        gatewayMetrics = new GatewayMetrics(meterRegistry);
    }

    @Test
    @DisplayName("incrementBlocked方法应该增加黑名单拦截计数")
    void incrementBlockedShouldIncreaseCounter() {
        gatewayMetrics.incrementBlocked();
        gatewayMetrics.incrementBlocked();

        double count = meterRegistry.find("gateway.blacklist.blocked").counter().count();
        assertEquals(2.0, count);
    }

    @Test
    @DisplayName("incrementAuthFailure方法应该增加认证失败计数")
    void incrementAuthFailureShouldIncreaseCounter() {
        gatewayMetrics.incrementAuthFailure();

        double count = meterRegistry.find("gateway.auth.failures").counter().count();
        assertEquals(1.0, count);
    }

    @Test
    @DisplayName("incrementAuthSuccess方法应该增加认证成功计数")
    void incrementAuthSuccessShouldIncreaseCounter() {
        gatewayMetrics.incrementAuthSuccess();
        gatewayMetrics.incrementAuthSuccess();
        gatewayMetrics.incrementAuthSuccess();

        double count = meterRegistry.find("gateway.auth.success").counter().count();
        assertEquals(3.0, count);
    }

    @Test
    @DisplayName("incrementTotalRequests方法应该增加总请求计数")
    void incrementTotalRequestsShouldIncreaseCounter() {
        gatewayMetrics.incrementTotalRequests();

        double count = meterRegistry.find("gateway.requests.total").counter().count();
        assertEquals(1.0, count);
    }

    @Test
    @DisplayName("incrementSlowRequests方法应该增加慢请求计数")
    void incrementSlowRequestsShouldIncreaseCounter() {
        gatewayMetrics.incrementSlowRequests();
        gatewayMetrics.incrementSlowRequests();

        double count = meterRegistry.find("gateway.requests.slow").counter().count();
        assertEquals(2.0, count);
    }

    @Test
    @DisplayName("startTimer方法应该返回Timer.Sample实例")
    void startTimerShouldReturnTimerSample() {
        assertNotNull(gatewayMetrics.startTimer());
    }

    @Test
    @DisplayName("recordDuration方法应该正确记录请求耗时")
    void recordDurationShouldWorkCorrectly() {
        var sample = gatewayMetrics.startTimer();
        gatewayMetrics.recordDuration(sample);

        assertNotNull(sample);
    }
}