package com.lostsidewalk.buffy.app.model.request;

import com.lostsidewalk.buffy.FrameworkConfig;
import lombok.Data;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

@Data
public class SettingsUpdateRequest {
    @Email
    @Size(max=512)
    String emailAddress;
    FrameworkConfig frameworkConfig;
}
