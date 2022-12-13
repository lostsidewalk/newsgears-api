package com.lostsidewalk.buffy.app;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.FeedPreview;
import com.lostsidewalk.buffy.PostPublisher;
import com.lostsidewalk.buffy.Publisher.PubFormat;
import com.lostsidewalk.buffy.Publisher.PubResult;
import com.lostsidewalk.buffy.app.audit.AppLogService;
import com.lostsidewalk.buffy.app.model.response.DeployResponse;
import com.lostsidewalk.buffy.app.opml.OpmlException;
import com.lostsidewalk.buffy.app.opml.OpmlService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

import static com.lostsidewalk.buffy.app.user.UserRoles.VERIFIED_ROLE;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.collections4.CollectionUtils.size;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;
import static org.springframework.http.ResponseEntity.badRequest;
import static org.springframework.http.ResponseEntity.ok;

@Slf4j
@RestController
public class PubController {

    @Autowired
    AppLogService appLogService;

    @Autowired
    OpmlService opmlService;

    @Autowired
    PostPublisher postPublisher;
    //
    // preview feed
    //
    @GetMapping("/feeds/preview")
    @Secured({VERIFIED_ROLE})
    public ResponseEntity<?> previewFeed(@Valid @NotBlank @Size(max=256) @RequestParam String feedIdent,
                                         @Valid @NotBlank @Size(max=16) @RequestParam String format,
                                         Authentication authentication) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        PubFormat pubFormat = PubFormat.byName(format);
        if (pubFormat == null) {
            return badRequest().body("Invalid format: " + format);
        }
        log.debug("previewFeed for user={}, feedIdent={}, format={}", username, feedIdent, pubFormat.name());
        StopWatch stopWatch = StopWatch.createStarted();
        List<FeedPreview> previewPosts = postPublisher.doPreview(username, feedIdent, pubFormat);
        stopWatch.stop();
        appLogService.logFeedPreview(username, stopWatch, feedIdent, pubFormat, size(previewPosts));
        return ok().body(previewPosts);
    }
    //
    // deploy feed
    //
    @GetMapping("/feeds/deploy")
    @Secured({VERIFIED_ROLE})
    @Transactional
    public ResponseEntity<?> deployFeed(@Valid @NotBlank @Size(max=256) @RequestParam String feedIdent, Authentication authentication) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deployFeed for user={}, feedIdent={}", username, feedIdent);
        StopWatch stopWatch = StopWatch.createStarted();
        List<PubResult> publicationResults = postPublisher.publishFeed(username, feedIdent);
        stopWatch.stop();
        appLogService.logFeedDeploy(username, stopWatch, publicationResults);
        if (isNotEmpty(publicationResults)) {
            List<DeployResponse> deployResponses = publicationResults.stream()
                    .map(DeployResponse::from)
                    .collect(toList());

            return ok().body(deployResponses);
        }

        return ok(EMPTY);
    }
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
                .cacheControl(CacheControl.noCache())
                .header(CONTENT_DISPOSITION, "attachment; filename=\"feedgears_opml_export.xml\"")
                .body(new ByteArrayResource(opmlArtifact.getBytes()));
    }
}
