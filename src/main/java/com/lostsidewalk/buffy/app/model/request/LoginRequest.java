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
    @NotBlank
    @Size(max=100)
    private String username;
    @NotBlank
    @Size(max=256)
    private String password;
}
