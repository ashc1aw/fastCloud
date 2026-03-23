package cc.ashclaw.fastCloud.gateway.filter;

import cc.ashclaw.fastCloud.gateway.config.properties.CorsProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 跨域请求过滤器 - WebMVC版本
 *
 * <p>该过滤器负责处理所有跨域（CORS）请求，设置正确的响应头信息。
 * 配置通过CorsProperties类从application.yml读取，支持Nacos动态刷新。
 *
 * <p>主要功能：
 * <ul>
 *   <li>允许配置的跨域请求来源</li>
 *   <li>支持标准HTTP方法配置</li>
 *   <li>允许携带认证信息（Cookie、Authorization等）</li>
 *   <li>暴露自定义响应头供前端读取</li>
 *   <li>缓存预检请求结果减少OPTIONS请求</li>
 * </ul>
 *
 * <p>过滤器优先级设置为最高，确保跨域处理在其他过滤器之前执行。
 *
 * @author ashclaw
 * @since JDK 25
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CrosFilter extends OncePerRequestFilter {

    private final CorsProperties corsProperties;

    public CrosFilter(CorsProperties corsProperties) {
        this.corsProperties = corsProperties;
    }

    /**
     * 过滤器核心处理方法
     *
     * <p>处理所有跨域请求，对于OPTIONS请求直接返回，对于其他请求添加CORS响应头。
     *
     * @param request HTTP请求对象
     * @param response HTTP响应对象
     * @param filterChain 过滤器链
     * @throws ServletException 如果处理请求时发生Servlet异常
     * @throws IOException 如果处理请求时发生IO异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!corsProperties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String origin = request.getHeader("Origin");

        if (log.isDebugEnabled()) {
            log.debug("CORS处理 - 来源: {}, 方法: {}, URI: {}",
                    origin, request.getMethod(), request.getRequestURI());
        }

        String allowOrigin = "*".equals(corsProperties.getAllowedOrigins())
                ? origin != null ? origin : "*"
                : corsProperties.getAllowedOrigins();

        response.setHeader("Access-Control-Allow-Origin", allowOrigin);
        response.setHeader("Access-Control-Allow-Methods", corsProperties.getAllowedMethods());
        response.setHeader("Access-Control-Allow-Headers", corsProperties.getAllowedHeaders());

        if (corsProperties.isAllowCredentials()) {
            response.setHeader("Access-Control-Allow-Credentials", "true");
        }

        response.setHeader("Access-Control-Max-Age", String.valueOf(corsProperties.getMaxAge()));
        response.setHeader("Access-Control-Expose-Headers", corsProperties.getExposedHeaders());

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("text/plain");
            response.getWriter().flush();
            return;
        }

        filterChain.doFilter(request, response);
    }
}