package com.lostsidewalk.buffy.app.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import jakarta.validation.constraints.Size;

@Data
public class FeedStatusUpdateRequest {

    @NotNull(message = "{feed.status.update.error.new-status-is-blank}")
    @Size(max = 64, message = "{feed.status.update.error.new-status-too-long}")
    String newStatus;
}
