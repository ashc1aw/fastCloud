package cc.ashclaw.fastCloud.gateway.filter;

import cc.ashclaw.fastCloud.common.satoken.util.LoginHelper;
import cc.ashclaw.common4j.core.enums.HttpStatus;
import cc.ashclaw.common4j.core.exception.SseException;
import cc.ashclaw.common4j.core.util.StringUtil;
import cc.ashclaw.common4j.web.util.IpUtil;
import cc.ashclaw.fastCloud.gateway.config.properties.WhiteListProperties;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Sa-Token认证授权过滤器 - WebMVC版本
 *
 * <p>该过滤器是网关安全防护的核心组件，负责验证请求的登录状态和权限。
 * 主要功能包括：
 * <ul>
 *   <li>登录状态验证：通过Sa-Token检查用户是否已登录</li>
 *   <li>白名单放行：配置在白名单中的路径跳过认证检查</li>
 *   <li>客户端ID校验：防止Token被盗用</li>
 *   <li>多端登录控制：支持不同设备类型的登录验证</li>
 * </ul>
 *
 * <p>过滤器执行流程：
 * <pre>
 * 1. 检查是否是排除的路径（静态资源、健康检查等）
 * 2. 检查是否在白名单中
 * 3. 调用StpUtil.checkLogin()验证登录状态
 * 4. 校验客户端ID（从Header或参数中获取）
 * 5. 放行或返回认证错误
 * </pre>
 *
 * <p>SSE特殊处理：
 * 对于SSE长连接请求，认证失败时抛出SseException，
 * 由GlobalExceptionHandler统一处理返回SSE格式的错误响应。
 *
 * @author ashclaw
 * @since JDK 25
 * @see WhiteListProperties
 * @see LoginHelper
 * @see cn.dev33.satoken
 */
@Slf4j
@Component
public class AuthFilter extends OncePerRequestFilter implements Ordered {

    /** 白名单配置属性 */
    private final WhiteListProperties whiteListProperties;

    /** 用户名（从Nacos元数据读取） */
    @Value("${spring.cloud.nacos.discovery.metadata.username:admin}")
    private String userName;

    /** 密码（从Nacos元数据读取） */
    @Value("${spring.cloud.nacos.discovery.metadata.userpassword:admin}")
    private String password;

    /** 过滤器执行优先级 */
    @Setter
    private int order = -50;

    /**
     * 构造函数
     *
     * @param whiteListProperties 白名单配置属性（由Spring自动注入）
     */
    public AuthFilter(WhiteListProperties whiteListProperties) {
        this.whiteListProperties = whiteListProperties;
    }

    /**
     * 过滤器核心处理方法
     *
     * <p>该方法在每次请求时执行，主要处理逻辑：
     * <ol>
     *   <li>检查请求路径是否需要跳过认证</li>
     *   <li>验证用户登录状态</li>
     *   <li>校验客户端ID一致性</li>
     *   <li>处理认证失败的情况</li>
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
        String path = request.getRequestURI();

        // 步骤1：检查是否是排除的路径（静态资源、健康检查等）
        if (isExcludedPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 步骤2：检查是否在白名单中
        if (isInWhiteList(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 步骤3：检查登录状态
            StpUtil.checkLogin();

            // 步骤4：验证客户端ID匹配（防止Token被盗用）
            String headerCid = request.getHeader(LoginHelper.CLIENT_KEY);
            String paramCid = request.getParameter(LoginHelper.CLIENT_KEY);
            String clientId = (String) StpUtil.getExtra(LoginHelper.CLIENT_KEY);

            // 客户端ID三方任一匹配即可
            if (!StringUtil.equalsAny(clientId, headerCid, paramCid)) {
                log.warn("客户端ID验证失败 - IP: {}, Path: {}, ClientId: {}",
                        IpUtil.getClientIp(request), path, clientId);
                throw NotLoginException.newInstance(StpUtil.getLoginType(),
                        "401", "客户端ID与Token不匹配",
                        StpUtil.getTokenValue());
            }

            // 认证通过，继续处理
            filterChain.doFilter(request, response);

        } catch (NotLoginException e) {
            // SSE请求认证失败，抛出SseException由GlobalExceptionHandler处理
            if (path.contains("sse")) {
                throw new SseException(e.getCode(), e.getMessage());
            } else {
                // 普通请求返回JSON格式错误
                handleAuthError(response, e.getMessage());
            }
        } catch (Exception e) {
            log.error("认证过程发生异常", e);
            handleAuthError(response, "认证失败，无法访问系统资源");
        }
    }

    /**
     * 获取过滤器优先级
     *
     * @return 过滤器优先级顺序值
     */
    @Override
    public int getOrder() {
        return order;
    }

    /**
     * 检查是否是排除的路径
     *
     * <p>这些路径不需要认证：
     * <ul>
     *   <li>/favicon.ico - 网站图标</li>
     *   <li>/actuator/** - 监控端点</li>
     *   <li>/static/** - 静态资源</li>
     *   <li>/public/** - 公共资源</li>
     * </ul>
     *
     * @param path 请求路径
     * @return true表示是排除路径，false表示需要认证
     */
    private boolean isExcludedPath(String path) {
        return path.equals("/favicon.ico")
                || path.startsWith("/actuator")
                || path.startsWith("/static")
                || path.startsWith("/public");
    }

    /**
     * 检查是否在白名单中
     *
     * <p>白名单路径不需要认证，支持通配符匹配。
     *
     * @param path 请求路径
     * @return true表示在白名单中，false表示不在
     */
    private boolean isInWhiteList(String path) {
        return whiteListProperties.getWhites().stream()
                .anyMatch(pattern -> matchPattern(pattern, path));
    }

    /**
     * 模式匹配
     *
     * <p>支持通配符匹配：
     * <ul>
     *   <li>* 匹配任意字符（包括路径分隔符）</li>
     * </ul>
     *
     * @param pattern 匹配模式
     * @param path 请求路径
     * @return true表示匹配成功，false表示匹配失败
     */
    private boolean matchPattern(String pattern, String path) {
        if (pattern.contains("*")) {
            // 通配符转换为正则表达式
            String regex = pattern.replace("*", ".*");
            return path.matches(regex);
        }
        return path.equals(pattern);
    }

    /**
     * 处理认证错误
     *
     * <p>返回标准的JSON格式错误响应：
     * <pre>
     * {
     *   "code": 401,
     *   "msg": "认证失败原因",
     *   "data": null
     * }
     * </pre>
     *
     * @param response HTTP响应对象
     * @param message 错误提示信息
     * @throws IOException 如果写入响应时发生IO异常
     */
    private void handleAuthError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.getCode());
        response.setContentType("application/json;charset=UTF-8");

        String responseBody = String.format(
                "{\"code\": %d, \"msg\": \"%s\", \"data\": null}",
                HttpStatus.UNAUTHORIZED.getCode(), message
        );

        response.getWriter().write(responseBody);
        response.getWriter().flush();
    }
}