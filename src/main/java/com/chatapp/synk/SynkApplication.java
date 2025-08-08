package com.chatapp.synk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.chatapp.synk", "com.api.emailservice"})
public class SynkApplication {
    public static void main(String[] args) {
        SpringApplication.run(SynkApplication.class, args);
    }
}
