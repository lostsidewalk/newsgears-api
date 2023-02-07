package com.lostsidewalk.buffy.app;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.app.audit.AppLogService;
import com.lostsidewalk.buffy.app.audit.OpmlException;
import com.lostsidewalk.buffy.app.opml.OpmlService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.lostsidewalk.buffy.app.user.UserRoles.VERIFIED_ROLE;
import static org.springframework.http.CacheControl.noCache;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;
import static org.springframework.http.ResponseEntity.ok;

@Slf4j
@RestController
public class ExportController {

    @Autowired
    AppLogService appLogService;

    @Autowired
    OpmlService opmlService;
    //
    // export feeds
    //
    @GetMapping("/feeds/opml")
    @Secured({VERIFIED_ROLE})
    @Transactional
    public ResponseEntity<Resource> exportOpml(Authentication authentication) throws OpmlException, DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("exportOpml for user={}", username);
        StopWatch stopWatch = StopWatch.createStarted();
        String opmlArtifact = opmlService.generateOpml(username);
        stopWatch.stop();
        appLogService.logOpmlExport(username, stopWatch);

        return ok()
                .contentType(APPLICATION_OCTET_STREAM)
                .cacheControl(noCache())
                .header(CONTENT_DISPOSITION, "attachment; filename=\"feedgears_opml_export.xml\"")
                .body(new ByteArrayResource(opmlArtifact.getBytes()));
    }
}
