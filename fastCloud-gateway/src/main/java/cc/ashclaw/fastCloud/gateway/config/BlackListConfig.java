package cc.ashclaw.fastCloud.gateway.config;

import cc.ashclaw.fastCloud.gateway.config.properties.BlackListProperties;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 黑名单过滤器配置类
 *
 * <p>该配置类负责初始化和管理黑名单过滤器的配置信息。
 * 主要功能包括：
 * <ul>
 *   <li>启用BlackListProperties配置属性类</li>
 *   <li>在应用启动时打印黑名单配置摘要信息</li>
 *   <li>支持Nacos配置中心的动态刷新（通过@RefreshScope）</li>
 * </ul>
 *
 * <p>配置项说明（对应application.yml中的security.blacklist）：
 * <ul>
 *   <li>enabled: 是否启用黑名单功能</li>
 *   <li>ip-blacklist: IP黑名单列表，支持精确IP和CIDR网段格式</li>
 *   <li>path-blacklist: 路径黑名单列表，支持通配符匹配</li>
 *   <li>order: 过滤器执行优先级，数值越小优先级越高</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>
 * # application.yml 配置示例
 * security:
 *   blacklist:
 *     enabled: true
 *     order: -100
 *     ip-blacklist:
 *       - "192.168.1.100"      # 精确IP
 *       - "10.0.0.0/24"        # CIDR网段
 *     path-blacklist:
 *       - "/admin/**"          # 通配符路径
 *       - "/api/secret"        # 精确路径
 * </pre>
 *
 * @author ashclaw
 * @since JDK 25
 * @see BlackListProperties
 * @see cc.ashclaw.fastCloud.gateway.filter.BlackListFilter
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(BlackListProperties.class)
public class BlackListConfig {

    /** 黑名单配置属性 */
    private final BlackListProperties blackListProperties;

    /**
     * 构造函数
     *
     * @param blackListProperties 黑名单配置属性（由Spring自动注入）
     */
    public BlackListConfig(BlackListProperties blackListProperties) {
        this.blackListProperties = blackListProperties;
    }

    /**
     * 初始化方法
     *
     * <p>在Bean构造完成后执行，用于打印黑名单配置摘要。
     * 该方法在应用启动时调用，用于确认配置是否正确加载。
     * 由于使用了@RefreshScope，该方法在配置刷新时也会重新执行。
     */
    @PostConstruct
    public void init() {
        log.info("黑名单过滤器配置初始化: 启用={}, IP黑名单数量={}, 路径黑名单数量={}, 执行顺序={}",
                blackListProperties.isEnabled(),
                blackListProperties.getIpBlacklist().size(),
                blackListProperties.getPathBlacklist().size(),
                blackListProperties.getOrder());
    }
}