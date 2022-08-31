package com.lostsidewalk.buffy;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.lostsidewalk.buffy.ResponseMessageUtils.buildResponseMessage;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Slf4j
@RestController
@CrossOrigin(origins = "http://localhost:3000")
public class PostController {

    @Autowired
    StagingPostDao stagingPostDao;
    //
    // display the post feed
    //
    @GetMapping("/staging")
    public ResponseEntity<List<StagingPost>> getStagingPosts() {
        return ResponseEntity.ok(stagingPostDao.findAllUnpublished());
    }

    @GetMapping("/published")
    public ResponseEntity<List<StagingPost>> getPublishedPosts() {
        return ResponseEntity.ok(stagingPostDao.findAllPublished());
    }

    @Data
    static class PublicationRequest {
        boolean publish;
    }

    @PutMapping("/staging/{id}")
    public ResponseEntity<?> markPubPending(@PathVariable Long id, @RequestBody PublicationRequest pubRequest) {
        if (stagingPostDao.checkPublished(id)) {
            return ResponseEntity.badRequest().body(buildResponseMessage("Staging post Id " + id + " is already published."));
        }
        // mark for publishing
        int rowsUpdated = stagingPostDao.markPubPending(id, pubRequest.publish);
        if (rowsUpdated == 1) {
            return ResponseEntity.ok().body(buildResponseMessage((pubRequest.publish ? "Marked" : "Unmarked") + " post Id " + id + " for publication."));
        } else if (rowsUpdated == 0) {
            return ResponseEntity.badRequest().body(buildResponseMessage("Unable to " + (pubRequest.publish ? "mark" : "unmark") + " post Id " + id + " for publication."));
        } else if (rowsUpdated > 1) {
            log.error("Mark/unmark pub pending request for staging post Id " + id + " resulted in multiple entities updated, updatedCt=" + rowsUpdated + ", publish=" + pubRequest.publish);
            return ResponseEntity.internalServerError().build();
        }

        throw new RuntimeException("End of method reached");
    }

    @DeleteMapping("/staging/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (stagingPostDao.checkPublished(id)) {
            return ResponseEntity.badRequest().body(buildResponseMessage("Staging post Id " + id + " is already published."));
        }
        // delete from staging
        int rowsDeleted = stagingPostDao.deleteById(id);
        if (rowsDeleted == 1) {
            return ResponseEntity.ok().body(buildResponseMessage("Deleted staging post Id " + id));
        } else if (rowsDeleted == 0) {
            return ResponseEntity.badRequest().body(buildResponseMessage("Unable to delete staging post Id " + id));
        } else if (rowsDeleted > 1) {
            log.error("Delete request for staging post Id " + id + " resulted in multiple entities deleted, deletedCt=" + rowsDeleted);
            return ResponseEntity.internalServerError().build();
        }

        throw new RuntimeException("End of method reached");
    }

    @Data
    @AllArgsConstructor
    static class ResponseMessage {
        String message;
    }

    @Autowired
    PostPublisher postPublisher;

    @GetMapping("/staging/deploy")
    public ResponseEntity<?> deployPubPending(@RequestParam(required = false) String feed) {
        postPublisher.doPublish(feed);
        String messageBody = isBlank(feed) ? "Deployed all feeds." : "Deployed all posts to '" + feed + "'.";
        return ResponseEntity.ok().body(buildResponseMessage(messageBody));
    }
}
