package wevioo.se.authms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient
@EnableJpaAuditing
@EnableScheduling
@ConfigurationPropertiesScan(basePackages = "wevioo.se.authms")
public class AuthMsApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthMsApplication.class, args);
    }

}
