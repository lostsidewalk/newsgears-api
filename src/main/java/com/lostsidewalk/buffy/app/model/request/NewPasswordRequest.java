package com.lostsidewalk.buffy.app.model.request;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
public class NewPasswordRequest {

    @NotBlank(message = "{new.password.error.new-password-is-blank}")
    @Size(min = 6, max = 256, message = "{new.password.error.new-password-length}")
    private String newPassword;

    @NotBlank(message = "{new.password.error.new-password-confirmed-is-blank}")
    @Size(min = 6, max = 256, message = "{new.password.error.new-password-confirmed-length}")
    private String newPasswordConfirmed;
}
