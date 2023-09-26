package com.lostsidewalk.buffy.app.model.response;

import com.lostsidewalk.buffy.publisher.Publisher.PubResult;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Date;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

/**
 * A response model for a queue deployment result.
 */
@Data
public class DeployResponse {

    /**
     * The timestamp at which the queue was deployed to this publisher.
     */
    @NotNull(message = "{deploy.response.error.timestamp-is-null}")
    Date timestamp;

    /**
     * The identifier of the publisher (i.e., RSS_20 vs ATOM_10, etc.)
     */
    @NotBlank(message = "{deploy.response.error.publisher-ident-is-blank}")
    @Size(max = 64, message = "{deploy.response.error.publisher-ident-is-too-long}")
    String publisherIdent;

    /**
     * The URL of the deployed artifact.
     */
    String url;

    private DeployResponse(Date timestamp, String publisherIdent, String url) {
        this.timestamp = timestamp;
        this.publisherIdent = publisherIdent;
        this.url = url;
    }

    /**
     * Static factory method to convert PubResults into DeployResponse data transfer objects.
     *
     * @param publicationResults a mapping of publisher identifier to PubResult objects
     * @return a mapping of publisher identifier to DeployResponse objects
     */
    public static Map<String, DeployResponse> from(Map<String, PubResult> publicationResults) {
        return publicationResults.entrySet().stream().collect(toMap(Map.Entry::getKey, e -> {
            PubResult p = e.getValue();
            return new DeployResponse(p.getPubDate(), e.getKey(), p.getUrl());
        }));
    }
}
