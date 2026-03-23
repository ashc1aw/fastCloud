package cc.ashclaw.fastCloud.gateway.config.properties;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 白名单配置属性类
 *
 * <p>该类用于绑定和读取application.yml中的security.whiteList配置项，
 * 支持Nacos配置中心的动态刷新功能。
 *
 * <p>白名单用于配置不需要进行身份验证的请求路径。
 * 配置在白名单中的路径将跳过AuthFilter的登录检查。
 *
 * <p>路径匹配规则：
 * <ul>
 *   <li>精确匹配: "/auth/login"</li>
 *   <li>通配符匹配: "/auth/**" (匹配/auth/下的所有路径)</li>
 * </ul>
 *
 * <p>配置示例：
 * <pre>
 * security:
 *   whiteList:
 *     whites:
 *       - "/auth/**"
 *       - "/public/**"
 *       - "/favicon.ico"
 *       - "/actuator/health"
 * </pre>
 *
 * @author ashclaw
 * @since JDK 25
 * @see cc.ashclaw.fastCloud.gateway.filter.AuthFilter
 */
@Data
@NoArgsConstructor
@RefreshScope
@ConfigurationProperties(prefix = "security.white-list")
public class WhiteListProperties {

    /**
     * 白名单路径列表
     *
     * <p>配置不需要身份验证的请求路径。
     * 支持精确匹配和通配符匹配两种格式。
     *
     * <p>匹配规则：
     * <ul>
     *   <li>精确匹配: "/auth/login" 精确匹配 "/auth/login"</li>
     *   <li>通配符匹配: "/auth/**" 匹配 "/auth/login", "/auth/logout" 等</li>
     * </ul>
     */
    private List<String> whites = new ArrayList<>();
}