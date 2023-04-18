package com.lostsidewalk.buffy.app;

import com.lostsidewalk.buffy.app.audit.ProxyUrlHashException;
import com.lostsidewalk.buffy.app.proxy.ProxyService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

import static java.util.concurrent.TimeUnit.MINUTES;
import static org.springframework.http.CacheControl.maxAge;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;
import static org.springframework.http.ResponseEntity.ok;

@Slf4j
@RestController
public class ProxyController {

//    @Autowired
//    AppLogService appLogService;

    @Autowired
    ProxyService proxyService;

    @GetMapping("/proxy/unsecured/{hash}/")
    public ResponseEntity<byte[]> proxy(@Valid @Size(max = 1024, message = "{proxy.error.hash-too-long}") @PathVariable String hash, @Valid @Size(max = 1024) @RequestParam String url) throws IOException, ProxyUrlHashException {
        log.debug("proxy for hash={}, url={}", hash, url);
//        StopWatch stopWatch = StopWatch.createStarted();
        proxyService.validateImageUrl(url, hash);
        byte[] image = proxyService.fetch(url);
//        appLogService.logProxyFetch(hash, stopWatch, url);
        return ok()
                .contentType(APPLICATION_OCTET_STREAM)
                .cacheControl(maxAge(60, MINUTES))
                .body(image);
    }
}
