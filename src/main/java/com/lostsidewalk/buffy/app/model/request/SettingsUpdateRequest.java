package com.lostsidewalk.buffy.app.model.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lostsidewalk.buffy.FrameworkConfig;
import lombok.Data;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

@Data
@JsonInclude(NON_EMPTY)
public class SettingsUpdateRequest {

    @Email
    @Size(max=512)
    String emailAddress;

    FrameworkConfig frameworkConfig;
}
