package com.lostsidewalk.buffy.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableAutoConfiguration
@EnableScheduling
@EnableConfigurationProperties
@EnableTransactionManagement
@EnableCaching
@PropertySource("classpath:secret.properties")
@ComponentScan({
        "com.lostsidewalk.buffy",
        "com.listsidewalk.buffy.app",
        "com.lostsidewalk.buffy.newsapi",
        "com.lostsidewalk.buffy.rss",
        "com.lostsidewalk.buffy.mail",
        "com.lostsidewalk.buffy.order",
})
@Configuration
public class Application {

    public static void main(String[] args) {
        //
        // startup/context config
        //
        SpringApplication.run(Application.class, args);
    }
}
