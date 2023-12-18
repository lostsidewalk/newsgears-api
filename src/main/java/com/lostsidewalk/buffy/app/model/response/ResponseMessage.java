package com.lostsidewalk.buffy.app.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_ABSENT;

@Data
@JsonInclude(NON_ABSENT)
public class ResponseMessage {

    String responseType;

    Object message;

    public ResponseMessage(Object message) {
        this.message = message;
    }

    public ResponseMessage(String responseType, Object message) {
        this.responseType = responseType;
        this.message = message;
    }
}
