package cc.ashclaw.fastCloud.gateway;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import static java.lang.IO.println;

@SpringBootApplication
public class GatewayApplication {
    void main(String[] args) {
        SpringApplication application = new SpringApplication(GatewayApplication.class);
        application.setApplicationStartup(new BufferingApplicationStartup(2048));
        application.run(args);
    }

    @Bean
    public CommandLineRunner printPort(Environment environment) {
        return args -> {
            String port = environment.getProperty("local.server.port");
            println("\u001B[33m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\u001B[0m");
            println("\u001B[36m   🌐 Gateway is listening on port: \u001B[32m" + port + "\u001B[0m");
            println("\u001B[33m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\u001B[0m");
        };
    }
}
