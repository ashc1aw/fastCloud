package cc.ashclaw.fastCloud.gateway.config;

import cc.ashclaw.fastCloud.gateway.handler.SentinelHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 网关配置类 - WebMVC版本
 *
 * <p>该配置类负责网关的整体配置，包括：
 * <ul>
 *   <li>Sentinel限流拦截器注册</li>
 *   <li>拦截器路径模式配置</li>
 * </ul>
 *
 * <p>SentinelHandler通过HandlerInterceptor机制实现限流功能，
 * 在请求处理前通过SphU.entry()检查是否被限流或降级。
 *
 * <p>拦截器配置说明：
 * <ul>
 *   <li>匹配路径: /** (所有路径)</li>
 *   <li>排除路径: /actuator/** (监控端点), /favicon.ico (网站图标)</li>
 * </ul>
 *
 * <p>执行流程：
 * <pre>
 * 请求 → SentinelHandler.preHandle() → SphU.entry()检查 → 通过则继续，否则返回限流响应
 *                                            ↓
 *                                      FilterChain.doFilter()
 *                                            ↓
 *                                      HandlerInterceptor.afterCompletion()
 * </pre>
 *
 * @author ashclaw
 * @since JDK 25
 * @see SentinelHandler
 * @see WebMvcConfigurer
 */
@Slf4j
@Configuration
public class GateWayConfig implements WebMvcConfigurer {

    /** Sentinel限流处理器 */
    private final SentinelHandler sentinelHandler;

    /**
     * 构造函数
     *
     * @param sentinelHandler Sentinel限流处理器（由Spring自动注入）
     */
    public GateWayConfig(SentinelHandler sentinelHandler) {
        this.sentinelHandler = sentinelHandler;
    }

    /**
     * 注册拦截器
     *
     * <p>配置Sentinel限流拦截器的路径匹配规则。
     * 所有请求都会经过Sentinel的资源检查，实现基于URI的限流。
     *
     * @param registry 拦截器注册表
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        log.info("注册Sentinel限流拦截器");
        registry.addInterceptor(sentinelHandler)
                .addPathPatterns("/**")
                .excludePathPatterns("/actuator/**", "/favicon.ico");
    }
}