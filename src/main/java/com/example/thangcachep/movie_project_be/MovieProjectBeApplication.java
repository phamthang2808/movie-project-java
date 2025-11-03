package com.example.thangcachep.movie_project_be;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling  // Enable scheduled tasks (VIP expiration check)
public class MovieProjectBeApplication {

    public static void main(String[] args) {
        SpringApplication.run(MovieProjectBeApplication.class, args);
    }

}
