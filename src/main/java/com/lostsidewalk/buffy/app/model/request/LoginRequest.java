package com.lostsidewalk.buffy.app.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequest {

    @NotBlank(message = "{login.error.username-is-blank}")
    @Size(max = 100, message = "{login.error.username-too-long}")
    private String username;

    @NotBlank(message = "{login.error.password-is-blank}")
    @Size(max = 256, message = "{login.error.password-too-long}")
    private String password;
}
