package com.lostsidewalk.buffy.app.model.request;

import lombok.Data;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
public class RegistrationRequest {
    @NotBlank
    @Size(max=100)
    String username;
    @NotBlank
    @Email
    @Size(max=512)
    String email;
    @NotBlank
    @Size(min=6, max=256)
    String password;
}
