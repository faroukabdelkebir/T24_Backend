package wevioo.se.userms;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;


import java.util.Set;

@SpringBootApplication
@EnableDiscoveryClient
public class UserMsApplication {

    public static void main(String[] args) { SpringApplication.run(UserMsApplication.class, args);}


}
