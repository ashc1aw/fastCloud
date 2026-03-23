package cc.ashclaw.fastCloud.gateway;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import static java.lang.IO.println;

/**
 * fastCloud Gateway 应用启动类 - JDK 25版本
 *
 * <p>该类是Spring Boot应用程序的入口点，负责启动网关服务。
 *
 * <p>技术栈：
 * <ul>
 *   <li>JDK 25 - 最新的Java开发工具包</li>
 *   <li>Spring Boot 4.0 - 应用框架</li>
 *   <li>Spring Cloud Gateway WebMVC - 网关路由</li>
 *   <li>Spring Cloud Alibaba - 云原生组件</li>
 *   <li>Nacos - 配置中心和服务发现</li>
 *   <li>Sentinel - 限流和降级</li>
 *   <li>Sa-Token - 认证授权</li>
 * </ul>
 *
 * <p>主要功能：
 * <ul>
 *   <li>请求路由转发</li>
 *   <li>身份认证授权</li>
 *   <li>IP和路径黑名单</li>
 *   <li>限流降级</li>
 *   <li>请求日志追踪</li>
 *   <li>统一异常处理</li>
 * </ul>
 *
 * <p>启动方式：
 * <pre>
 * // IDE中直接运行main方法
 * // 或命令行打包后运行：
 * java -jar fastCloud-gateway.jar
 * </pre>
 *
 * @author ashclaw
 * @since JDK 25
 */
@SpringBootApplication
public class GatewayApplication {

    /**
     * 应用入口方法
     *
     * <p>使用JDK 25的隐式public main方法特性，
     * 方法参数args用于接收命令行参数。
     *
     * <p>启动配置说明：
     * <ul>
     *   <li>BufferingApplicationStartup(2048): 设置应用启动缓冲队列大小为2048</li>
     *   <li>用于收集应用启动期间的指标数据</li>
     * </ul>
     *
     * @param args 命令行参数
     */
    void main(String[] args) {
        // 创建SpringApplication实例
        SpringApplication application = new SpringApplication(GatewayApplication.class);

        // 设置应用启动性能分析器
        application.setApplicationStartup(new BufferingApplicationStartup(2048));

        // 启动应用
        application.run(args);
    }

    /**
     * 应用启动完成后打印端口信息
     *
     * <p>该方法在应用完全启动后执行，用于打印网关服务监听的端口号。
     * 使用@Bean注解将其注册为Spring容器中的CommandLineRunner。
     *
     * <p>输出示例：
     * <pre>
     * ┠━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
     *    🌐 Gateway is listening on port: 8080
     * ┖━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
     * </pre>
     *
     * @param environment Spring环境对象，用于获取配置属性
     * @return CommandLineRunner实例
     */
    @Bean
    public CommandLineRunner printPort(Environment environment) {
        return args -> {
            // 获取服务器端口号
            String port = environment.getProperty("local.server.port");

            // 打印彩色启动信息
            println("\u001B[33m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\u001B[0m");
            println("\u001B[36m   🌐 Gateway is listening on port: \u001B[32m" + port + "\u001B[0m");
            println("\u001B[33m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\u001B[0m");
        };
    }
}