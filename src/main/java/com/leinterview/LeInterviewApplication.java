package com.leinterview;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class LeInterviewApplication {

    public static void main(String[] args) {
        SpringApplication.run(LeInterviewApplication.class, args);
    }
}
