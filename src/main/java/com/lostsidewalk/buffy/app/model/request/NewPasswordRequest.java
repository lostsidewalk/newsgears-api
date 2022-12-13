package com.lostsidewalk.buffy.app.model.request;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
public class NewPasswordRequest {
    @NotBlank
    @Size(min=6, max=256)
    private String newPassword;
    @NotBlank
    @Size(min=6, max=256)
    private String newPasswordConfirmed;
}
