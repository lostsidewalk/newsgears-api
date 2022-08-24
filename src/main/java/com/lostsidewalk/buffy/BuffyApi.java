package com.lostsidewalk.buffy;

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
@ComponentScan({"com.lostsidewalk.buffy", "com.lostsidewalk.buffy.newsapi"})
@Configuration
public class BuffyApi {

    public static void main(String[] args) {
        //
        // startup/context config
        //
        SpringApplication.run(BuffyApi.class, args);
    }
}
