package com.lostsidewalk.buffy.app.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lostsidewalk.buffy.app.model.request.QueueConfigRequest;
import lombok.Data;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

@Data
@JsonInclude(NON_EMPTY)
public class OpmlConfigResponse {
    List<QueueConfigRequest> queueConfigRequests;
    List<String> errors;

    OpmlConfigResponse(List<QueueConfigRequest> queueConfigRequests, List<String> errors) {
        this.queueConfigRequests = queueConfigRequests;
        this.errors = errors;
    }

    public static OpmlConfigResponse from(List<QueueConfigRequest> queueConfigRequests, List<String> errors) {
        return new OpmlConfigResponse(queueConfigRequests, errors);
    }
}
