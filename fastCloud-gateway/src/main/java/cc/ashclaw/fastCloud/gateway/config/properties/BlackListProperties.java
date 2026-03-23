package cc.ashclaw.fastCloud.gateway.config.properties;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 黑名单配置属性类
 *
 * <p>该类用于绑定和读取application.yml中的security.blacklist配置项，
 * 支持Nacos配置中心的动态刷新功能。
 *
 * <p>主要配置项：
 * <ul>
 *   <li>enabled: 是否启用黑名单功能，默认true</li>
 *   <li>order: 过滤器执行优先级，数值越小优先级越高，默认-100</li>
 *   <li>ipBlacklist: IP黑名单列表，支持精确IP和CIDR网段格式</li>
 *   <li>pathBlacklist: 路径黑名单列表，支持通配符匹配</li>
 * </ul>
 *
 * <p>IP黑名单格式说明：
 * <ul>
 *   <li>精确IP: "192.168.1.100"</li>
 *   <li>CIDR网段: "10.0.0.0/24", "172.16.0.0/16"</li>
 * </ul>
 *
 * <p>路径黑名单格式说明：
 * <ul>
 *   <li>精确路径: "/admin/user"</li>
 *   <li>通配符路径: "/admin/**", "/api/secret/*"</li>
 * </ul>
 *
 * @author ashclaw
 * @since JDK 25
 * @see <a href="https://commons.apache.org/proper/commons-net/utils_net/CIDRUtils.html">Apache CIDRUtils</a>
 */
@Data
@NoArgsConstructor
@RefreshScope
@ConfigurationProperties(prefix = "security.black-list")
public class BlackListProperties {

    /**
     * IP黑名单列表
     *
     * <p>支持的格式：
     * <ul>
     *   <li>精确IP: "192.168.1.100"</li>
     *   <li>CIDR网段: "10.0.0.0/24"</li>
     * </ul>
     */
    private List<String> ipBlacklist = new ArrayList<>();

    /**
     * 路径黑名单列表
     *
     * <p>支持的格式：
     * <ul>
     *   <li>精确路径: "/admin/user"</li>
     *   <li>通配符路径: "/admin/**", "/api/secret/*"</li>
     * </ul>
     */
    private List<String> pathBlacklist = new ArrayList<>();

    /**
     * 是否启用黑名单功能
     *
     * <p>默认为true，设置为false时将跳过所有黑名单检查。
     */
    private boolean enabled = true;

    /**
     * 过滤器执行优先级
     *
     * <p>数值越小优先级越高，在过滤器链中会优先执行。
     * 建议设置为较低数值（如-100）以确保在认证过滤器之前执行。
     */
    private int order = -100;
}