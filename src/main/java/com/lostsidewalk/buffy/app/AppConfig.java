package com.lostsidewalk.buffy.app;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Configuration
public class AppConfig {

    @Bean
    ConcurrentHashMap<String, Integer> errorStatusMap() {
        return new ConcurrentHashMap<>();
    }
}
