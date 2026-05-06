package wevioo.se.authms.Config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

@Configuration
@EnableJpaRepositories(basePackages = "wevioo.se.authms.Repositories")
public class InfraConfig {

    @Value("${app.user-service.url:http://localhost:8085}")
    private String userServiceUrl;


    @Bean
    public RestClient restClient() {
        return RestClient.builder()
                .baseUrl(userServiceUrl)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Accept",       MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}

