package cc.ashclaw.fastCloud.gateway.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 跨域配置属性类
 *
 * <p>该类用于绑定和读取application.yml中的cors配置项，
 * 支持Nacos配置中心的动态刷新功能。
 *
 * <p>主要配置项：
 * <ul>
 *   <li>allowedOrigins: 允许的请求来源，多个用逗号分隔</li>
 *   <li>allowedMethods: 允许的HTTP方法</li>
 *   <li>allowedHeaders: 允许的请求头</li>
 *   <li>allowCredentials: 是否允许携带凭证</li>
 *   <li>maxAge: 预检请求缓存时间（秒）</li>
 *   <li>exposedHeaders: 前端可访问的响应头</li>
 * </ul>
 *
 * @author ashclaw
 * @since JDK 25
 */
@Data
@NoArgsConstructor
@Configuration
@RefreshScope
@ConfigurationProperties(prefix = "cors")
public class CorsProperties {

    /**
     * 是否启用跨域功能
     */
    private boolean enabled = true;

    /**
     * 允许的请求来源
     * <p>支持具体域名，如：https://example.com
     * <p>生产环境建议设置为具体域名，不使用*
     * <p>多个域名用逗号分隔
     */
    private String allowedOrigins = "*";

    /**
     * 允许的HTTP方法
     */
    private String allowedMethods = "GET,POST,PUT,DELETE,PATCH,OPTIONS,HEAD";

    /**
     * 允许的请求头
     * <p>*表示允许所有请求头
     * <p>常见配置：Authorization,Content-Type,Accept,Origin,X-Requested-With
     */
    private String allowedHeaders = "*";

    /**
     * 是否允许携带凭证（Cookie、Authorization等）
     * <p>当设置为true时，allowedOrigins不能设置为*
     */
    private boolean allowCredentials = true;

    /**
     * 预检请求缓存时间（秒）
     * <p>设置Access-Control-Max-Age头，表示预检请求结果可以缓存多久
     * <p>默认1小时（3600秒）
     */
    private long maxAge = 3600;

    /**
     * 前端可访问的响应头
     * <p>默认暴露Content-Disposition,Authorization,X-Total-Count
     */
    private String exposedHeaders = "Content-Disposition,Authorization,X-Total-Count";
}