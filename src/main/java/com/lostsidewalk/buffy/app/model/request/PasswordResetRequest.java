package com.lostsidewalk.buffy.app.model.request;

import lombok.Data;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
public class PasswordResetRequest {
    @NotBlank
    @Size(max=100)
    private String username;
    @NotBlank
    @Email
    @Size(max=512)
    private String email;
}
