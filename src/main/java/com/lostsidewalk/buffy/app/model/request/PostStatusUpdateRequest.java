package com.lostsidewalk.buffy.app.model.request;

import lombok.Data;

import jakarta.validation.constraints.Size;

@Data
public class PostStatusUpdateRequest {
    @Size(max=64)
    String newStatus; // may be null
}
