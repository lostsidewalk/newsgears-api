package com.lostsidewalk.buffy.app.model.request;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import jakarta.validation.constraints.Size;

@Slf4j
@Data
public class ExportConfigRequest {

    @Size(max=2048)
    String mdTemplate;
}
