package com.lostsidewalk.buffy.app.broker;

import com.google.gson.JsonElement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class HelloWorldHandler implements MessageHandler<String> {

    @Override
    public String handleMessage(JsonElement payload, String username, String destination) {
        return "greetings, nerd";
    }
}
