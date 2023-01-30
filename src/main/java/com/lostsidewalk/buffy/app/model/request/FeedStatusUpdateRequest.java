package com.lostsidewalk.buffy.app.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import jakarta.validation.constraints.Size;

@Data
public class FeedStatusUpdateRequest {
    @NotNull
    @Size(max=64)
    String newStatus;
}
