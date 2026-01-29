package uk.co.nationalninesgolf.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class NationalNinesApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(NationalNinesApiApplication.class, args);
    }
}
