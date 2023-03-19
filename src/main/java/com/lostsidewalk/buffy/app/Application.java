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
        "com.lostsidewalk.buffy.rss",
        "com.lostsidewalk.buffy.mail",
        "com.lostsidewalk.buffy.order",
})
@Configuration
public class Application {

    public static void main(String[] args) {
        //
        // set global timeouts and startup/context config
        //
        System.setProperty("sun.net.client.defaultConnectTimeout", "2000");
        System.setProperty("sun.net.client.defaultReadTimeout", "4000");
        System.setProperty("http.agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36");
        System.setProperty("http.keepAlive", "true");
        SpringApplication.run(Application.class, args);
    }
}
