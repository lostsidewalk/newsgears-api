package com.lostsidewalk.buffy.app.model.response;

import lombok.Data;

@Data
public class RegistartionResponse {
    String username;
    String password;

    public RegistartionResponse(String username, String password) {
        setUsername(username);
        setPassword(password);
    }
}
