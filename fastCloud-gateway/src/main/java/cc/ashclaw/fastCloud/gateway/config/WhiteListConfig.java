package cc.ashclaw.fastCloud.gateway.config;

import cc.ashclaw.fastCloud.gateway.config.properties.WhiteListProperties;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 白名单过滤器配置类
 *
 * <p>该配置类负责初始化和管理白名单过滤器的配置信息。
 * 主要功能包括：
 * <ul>
 *   <li>启用WhiteListProperties配置属性类</li>
 *   <li>在应用启动时打印白名单配置摘要信息</li>
 *   <li>支持Nacos配置中心的动态刷新（通过@RefreshScope）</li>
 * </ul>
 *
 * <p>配置项说明（对应application.yml中的security.whiteList）：
 * <ul>
 *   <li>whites: 白名单路径列表，支持精确匹配和通配符匹配</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>
 * # application.yml 配置示例
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
 * @see WhiteListProperties
 * @see cc.ashclaw.fastCloud.gateway.filter.AuthFilter
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(WhiteListProperties.class)
public class WhiteListConfig {

    /** 白名单配置属性 */
    private final WhiteListProperties whiteListProperties;

    /**
     * 构造函数
     *
     * @param whiteListProperties 白名单配置属性（由Spring自动注入）
     */
    public WhiteListConfig(WhiteListProperties whiteListProperties) {
        this.whiteListProperties = whiteListProperties;
    }

    /**
     * 初始化方法
     *
     * <p>在Bean构造完成后执行，用于打印白名单配置摘要。
     * 该方法在应用启动时调用，用于确认配置是否正确加载。
     * 由于使用了@RefreshScope，该方法在配置刷新时也会重新执行。
     */
    @PostConstruct
    public void init() {
        log.info("白名单过滤器配置初始化: 白名单路径数量={}", whiteListProperties.getWhites().size());
    }
}