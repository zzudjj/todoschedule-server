package com.djj.todoscheduleserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TodoscheduleServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(TodoscheduleServerApplication.class, args);
    }

}
