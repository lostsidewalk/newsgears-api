package com.lostsidewalk.buffy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.lostsidewalk.buffy.ResponseMessageUtils.buildResponseMessage;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Slf4j
@RestController
@CrossOrigin(origins = "http://localhost:8081")
public class PublishingController {

    @Autowired
    PostPublisher postPublisher;

    @GetMapping("/staging/deploy")
    public ResponseEntity<?> deployPubPending(@RequestParam(required = false) String tag) {
        postPublisher.doPublish(tag);
        String messageBody = isBlank(tag) ? "Deployed all feeds." : "Deployed all posts to '" + tag + "' feed.";
        return ResponseEntity.ok().body(buildResponseMessage(messageBody));
    }
}
