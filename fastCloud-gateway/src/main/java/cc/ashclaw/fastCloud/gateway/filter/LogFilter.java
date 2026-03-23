package cc.ashclaw.fastCloud.gateway.filter;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import cc.ashclaw.common4j.web.util.IpUtil;
import cc.ashclaw.fastCloud.gateway.metrics.GatewayMetrics;
import io.micrometer.core.instrument.Timer;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * 全局请求日志过滤器 - WebMVC版本
 *
 * <p>该过滤器负责记录所有进入网关的请求日志，实现完整的请求链路追踪。
 * 主要功能包括：
 * <ul>
 *   <li>请求ID生成：为每个请求生成唯一标识符用于链路追踪</li>
 *   <li>客户端IP记录：获取并记录真实客户端IP地址</li>
 *   <li>请求耗时统计：计算请求处理时间</li>
 *   <li>慢请求告警：当请求耗时超过阈值时输出警告日志</li>
 *   <li>分级日志输出：根据响应状态码选择不同日志级别</li>
 *   <li>MDC上下文传递：支持在日志中注入请求上下文信息</li>
 * </ul>
 *
 * <p>日志输出格式：
 * <pre>
 * 请求开始 | IP: xxx.xxx.xxx.xxx | GET /api/users -> 200 (123ms)
 * </pre>
 *
 * <p>MDC上下文字段：
 * <ul>
 *   <li>requestId: 请求唯一标识符</li>
 *   <li>clientIp: 客户端真实IP地址</li>
 * </ul>
 *
 * @author ashclaw
 * @since JDK 25
 * @see MDC
 */
@Slf4j
@Component
public class LogFilter extends OncePerRequestFilter {

    /** 请求计数器（用于生成请求ID） */
    private static final AtomicLong REQUEST_COUNTER = new AtomicLong(0);

    /** MDC key: 请求ID */
    private static final String MDC_REQUEST_ID = "requestId";

    /** MDC key: 客户端IP */
    private static final String MDC_CLIENT_IP = "clientIp";

    /** 慢请求阈值（毫秒），默认3000ms */
    @Value("${request.log.slow-threshold-ms:3000}")
    private long slowThresholdMs;

    /** 指标收集器 */
    private final GatewayMetrics gatewayMetrics;

    /**
     * 构造函数
     *
     * @param gatewayMetrics 指标收集器
     */
    public LogFilter(GatewayMetrics gatewayMetrics) {
        this.gatewayMetrics = gatewayMetrics;
    }

    /**
     * 过滤器核心处理方法
     *
     * <p>该方法在每次请求时执行，主要处理流程：
     * <ol>
     *   <li>生成请求ID并放入MDC上下文</li>
     *   <li>获取客户端真实IP并放入MDC</li>
     *   <li>记录请求开始日志</li>
     *   <li>执行过滤链</li>
     *   <li>计算耗时并记录请求完成日志</li>
     *   <li>清理MDC上下文</li>
     * </ol>
     *
     * @param request  HTTP请求对象
     * @param response HTTP响应对象
     * @param filterChain 过滤器链
     * @throws ServletException 如果处理请求时发生Servlet异常
     * @throws IOException 如果处理请求时发生IO异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 启动计时器
        Timer.Sample timerSample = gatewayMetrics.startTimer();

        // 步骤1：生成请求ID并放入MDC
        long requestNumber = REQUEST_COUNTER.incrementAndGet();
        String requestId = generateRequestId(requestNumber);
        MDC.put(MDC_REQUEST_ID, requestId);

        // 增加总请求计数
        gatewayMetrics.incrementTotalRequests();

        // 步骤2：获取客户端真实IP并放入MDC
        String clientIp = IpUtil.getClientIp(request);
        MDC.put(MDC_CLIENT_IP, clientIp);

        // 步骤3：记录请求开始时间和详细信息
        long startTime = System.currentTimeMillis();
        String method = request.getMethod();
        String path = request.getRequestURI();
        String query = request.getQueryString();
        String url = query == null ? path : path + "?" + query;

        // 输出请求开始日志
        log.info("请求开始 | IP: {} | {} {}", clientIp, method, url);

        try {
            // 步骤4：执行过滤链
            filterChain.doFilter(request, response);
        } catch (Exception ex) {
            // 记录异常
            log.error("请求处理异常", ex);
            if (!response.isCommitted()) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
            throw ex;
        } finally {
            // 步骤5：计算耗时并记录请求完成日志
            long duration = System.currentTimeMillis() - startTime;
            int status = response.getStatus();

            // 记录请求耗时
            gatewayMetrics.recordDuration(timerSample);

            // 根据状态码选择日志级别
            String message = String.format("%s %s -> %d (%dms)", method, url, status, duration);
            if (status >= 200 && status < 300) {
                log.info("请求成功 | {}", message);
            } else if (status >= 400 && status < 500) {
                log.warn("客户端错误 | {}", message);
            } else if (status >= 500) {
                log.error("服务端错误 | {}", message);
            } else {
                log.info("请求完成 | {}", message);
            }

            // 步骤6：慢请求告警
            if (duration > slowThresholdMs) {
                log.warn("慢请求告警 | {} (超过 {}ms)", message, slowThresholdMs);
                gatewayMetrics.incrementSlowRequests();
            }

            // 步骤7：清理MDC（防止内存泄漏）
            MDC.clear();
        }
    }

    /**
     * 生成请求ID
     *
     * <p>请求ID格式：时间戳-计数器-随机UUID前8位
     * 示例：1743988630123-1-a1b2c3d4
     *
     * <p>组成说明：
     * <ul>
     *   <li>时间戳：保证大致递增</li>
     *   <li>计数器：保证同一毫秒内的唯一性</li>
     *   <li>随机UUID：防止猜测和碰撞</li>
     * </ul>
     *
     * @param requestNumber 请求计数器当前值
     * @return 请求唯一标识符
     */
    private String generateRequestId(long requestNumber) {
        return System.currentTimeMillis() + "-" + requestNumber + "-" +
                UUID.randomUUID().toString().substring(0, 8);
    }
}