package vn.affkit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AffkitBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(AffkitBackendApplication.class, args);
    }

}