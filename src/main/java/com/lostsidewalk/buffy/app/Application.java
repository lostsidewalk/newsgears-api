package com.lostsidewalk.buffy.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAutoConfiguration
@EnableScheduling
@EnableConfigurationProperties
@PropertySource("classpath:secret.properties")
@ComponentScan({"com.lostsidewalk.buffy", "com.listsidewalk.buffy.app", "com.lostsidewalk.buffy.newsapi", "com.lostsidewalk.buffy.rss"})
@Configuration
public class Application {

    public static void main(String[] args) {
        //
        // startup/context config
        //
        SpringApplication.run(Application.class, args);
    }
}
