package cc.ashclaw.fastCloud.gateway.filter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import cc.ashclaw.common4j.web.util.IpUtil;
import cc.ashclaw.fastCloud.gateway.metrics.GatewayMetrics;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import cc.ashclaw.common4j.core.util.StringUtil;
import cc.ashclaw.fastCloud.gateway.config.properties.BlackListProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * 黑名单过滤器 - WebMVC版本
 *
 * <p>该过滤器是网关安全防护的第一道屏障，用于拦截恶意请求。
 * 主要功能包括：
 * <ul>
 *   <li>IP黑名单拦截：支持精确IP和CIDR网段格式</li>
 *   <li>路径黑名单拦截：支持精确路径和通配符匹配</li>
 *   <li>请求统计：记录总请求数和拦截次数</li>
 * </ul>
 *
 * <p>过滤器执行流程：
 * <pre>
 * 1. 检查黑名单功能是否启用（enabled）
 * 2. 获取客户端真实IP（通过IpUtil工具类）
 * 3. 检查IP是否在黑名单中（支持CIDR网段匹配）
 * 4. 检查请求路径是否在黑名单中（支持通配符匹配）
 * 5. 拦截或放行请求
 * </pre>
 *
 * <p>配置优先级：
 * IP黑名单检查优先级高于路径黑名单，
 * 即当IP和路径都在黑名单中时，只记录IP拦截日志。
 *
 * @author ashclaw
 * @since JDK 25
 * @see BlackListProperties
 * @see IpUtil
 */
@Slf4j
@Component
public class BlackListFilter extends OncePerRequestFilter implements Ordered {

    /** 黑名单配置属性 */
    private final BlackListProperties blackListProperties;

    /** 指标收集器 */
    private final GatewayMetrics gatewayMetrics;

    /** 已拦截请求计数 */
    private static final AtomicLong BLOCKED_COUNT = new AtomicLong(0);

    /** 总请求计数 */
    private static final AtomicLong TOTAL_REQUESTS = new AtomicLong(0);

    /**
     * 构造函数
     *
     * @param blackListProperties 黑名单配置属性（由Spring自动注入）
     * @param gatewayMetrics 指标收集器
     */
    public BlackListFilter(BlackListProperties blackListProperties, GatewayMetrics gatewayMetrics) {
        this.blackListProperties = blackListProperties;
        this.gatewayMetrics = gatewayMetrics;
    }

    /**
     * 过滤器核心处理方法
     *
     * <p>该方法在每次请求时执行，主要处理逻辑：
     * <ol>
     *   <li>递增总请求计数器</li>
     *   <li>检查黑名单功能是否启用</li>
     *   <li>获取客户端IP并检查是否在IP黑名单中</li>
     *   <li>获取请求路径并检查是否在路径黑名单中</li>
     *   <li>如被拦截，返回403 Forbidden响应</li>
     * </ol>
     *
     * @param request  HTTP请求对象
     * @param response HTTP响应对象
     * @param filterChain 过滤器链
     * @throws ServletException 如果处理请求时发生Servlet异常
     * @throws IOException 如果处理请求时发生IO异常
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        TOTAL_REQUESTS.incrementAndGet();

        // 检查黑名单功能是否启用
        if (!blackListProperties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        // 获取客户端真实IP和请求路径
        String clientIp = IpUtil.getClientIp(request);
        String path = request.getRequestURI();

        // 检查IP黑名单
        if (isIpBlacklisted(clientIp)) {
            log.warn("IP黑名单拦截 - IP: {}, Path: {}, Method: {}",
                    clientIp, path, request.getMethod());
            BLOCKED_COUNT.incrementAndGet();
            gatewayMetrics.incrementBlocked();
            blockRequest(response, "IP地址已被列入黑名单");
            return;
        }

        // 检查路径黑名单
        if (isPathBlacklisted(path)) {
            log.warn("路径黑名单拦截 - IP: {}, Path: {}, Method: {}",
                    clientIp, path, request.getMethod());
            BLOCKED_COUNT.incrementAndGet();
            gatewayMetrics.incrementBlocked();
            blockRequest(response, "访问路径已被列入黑名单");
            return;
        }

        // 通过检查，继续处理
        filterChain.doFilter(request, response);
    }

    /**
     * 获取过滤器优先级
     *
     * <p>实现Ordered接口，使过滤器可以正确排序执行。
     * 数值越小优先级越高，会越早执行。
     *
     * @return 过滤器优先级顺序值
     */
    @Override
    public int getOrder() {
        return blackListProperties.getOrder();
    }

    /**
     * 检查IP是否在黑名单中
     *
     * <p>支持两种格式的IP黑名单：
     * <ul>
     *   <li>精确IP：如 "192.168.1.100"</li>
     *   <li>CIDR网段：如 "10.0.0.0/24"</li>
     * </ul>
     *
     * @param ip 待检查的客户端IP
     * @return true表示IP在黑名单中，false表示不在
     */
    private boolean isIpBlacklisted(String ip) {
        List<String> ipBlacklist = blackListProperties.getIpBlacklist();
        if (ipBlacklist.isEmpty()) {
            return false;
        }

        return ipBlacklist.stream().anyMatch(blackIp -> {
            if (blackIp.contains("/")) {
                // CIDR网段匹配
                return IpUtil.isIpInCidr(ip, blackIp);
            } else {
                // 精确IP匹配
                return ip.equals(blackIp);
            }
        });
    }

    /**
     * 检查路径是否在黑名单中
     *
     * <p>支持两种格式的路径黑名单：
     * <ul>
     *   <li>精确路径：如 "/admin/user"</li>
     *   <li>通配符路径：如 "/admin/**", "/api/*"</li>
     * </ul>
     *
     * @param path 待检查的请求路径
     * @return true表示路径在黑名单中，false表示不在
     */
    private boolean isPathBlacklisted(String path) {
        List<String> pathBlacklist = blackListProperties.getPathBlacklist();
        if (pathBlacklist.isEmpty()) {
            return false;
        }

        return pathBlacklist.stream().anyMatch(blackPath -> {
            if (blackPath.contains("*")) {
                // 通配符匹配，转换为正则表达式
                String regex = blackPath.replace("*", ".*");
                return path.matches(regex);
            } else {
                // 精确路径匹配
                return path.equals(blackPath);
            }
        });
    }

    /**
     * 拦截请求并返回错误响应
     *
     * <p>返回标准的JSON格式错误响应：
     * <pre>
     * {
     *   "code": 403,
     *   "msg": "拦截原因",
     *   "data": null
     * }
     * </pre>
     *
     * @param response HTTP响应对象
     * @param message 拦截提示信息
     * @throws IOException 如果写入响应时发生IO异常
     */
    private void blockRequest(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");

        String responseBody = String.format(
                "{\"code\": %d, \"msg\": \"%s\", \"data\": null}",
                HttpServletResponse.SC_FORBIDDEN, message
        );

        response.getWriter().write(responseBody);
        response.getWriter().flush();
    }

    /**
     * 获取已拦截的请求总数
     *
     * @return 被黑名单拦截的请求数量
     */
    public static long getBlockedCount() {
        return BLOCKED_COUNT.get();
    }

    /**
     * 获取总请求数
     *
     * @return 过滤器处理的总请求数量
     */
    public static long getTotalRequests() {
        return TOTAL_REQUESTS.get();
    }

    /**
     * 计算拦截率
     *
     * @return 拦截率百分比（0-100）
     */
    public static double getBlockRate() {
        long total = TOTAL_REQUESTS.get();
        if (total == 0) {
            return 0.0;
        }
        return (double) BLOCKED_COUNT.get() / total * 100;
    }
}