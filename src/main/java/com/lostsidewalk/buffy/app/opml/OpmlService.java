package com.lostsidewalk.buffy.app.opml;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.app.audit.OpmlException;
import com.lostsidewalk.buffy.app.model.request.FeedConfigRequest;
import com.lostsidewalk.buffy.app.model.request.RssAtomUrl;
import com.lostsidewalk.buffy.feed.FeedDefinition;
import com.lostsidewalk.buffy.feed.FeedDefinitionDao;
import com.lostsidewalk.buffy.query.QueryDefinition;
import com.lostsidewalk.buffy.query.QueryDefinitionDao;
import com.rometools.opml.feed.opml.Opml;
import com.rometools.opml.feed.opml.Outline;
import com.rometools.opml.io.impl.OPML20Generator;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.WireFeedInput;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ValidationException;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.jdom2.Document;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayListWithCapacity;
import static com.lostsidewalk.buffy.app.utils.WordUtils.randomWords;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.collections4.CollectionUtils.size;
import static org.apache.commons.lang3.RandomUtils.nextLong;
import static org.apache.commons.lang3.StringUtils.*;

@Slf4j
@Service
public class OpmlService {

    @Autowired
    Validator validator;

    @Autowired
    FeedDefinitionDao feedDefinitionDao;

    @Autowired
    QueryDefinitionDao queryDefinitionDao;

    public FeedConfigRequest parseOpmlFile(InputStream inputStream) throws OpmlException {
        try {
            WireFeedInput input = new WireFeedInput();
            InputStreamReader isr = new InputStreamReader(inputStream);
            Opml feed = (Opml) input.build(isr);
            String ident = generateRandomFeedIdent();
            String title = feed.getTitle();
            String description = getDescription(getOwner(feed));
            FeedConfigRequest feedConfigRequest = buildFeedConfigRequest(ident, title, description, feed.getOutlines());
            Set<ConstraintViolation<FeedConfigRequest>> constraintViolations = validator.validate(feedConfigRequest);
            if (isNotEmpty(constraintViolations)) {
                throw new FeedConfigRequestValidationException(constraintViolations);
            }
            return feedConfigRequest;
        } catch (Exception e) {
            throw new OpmlException(e.getMessage());
        }
    }

    private static String generateRandomFeedIdent() {
        return randomWords();
    }

    private static final String OWNER_NAME_AND_EMAIL_TEMPLATE = "%s (%s)";

    private String getOwner(Opml feed) {
        String ownerName = feed.getOwnerName();
        String ownerEmail = feed.getOwnerEmail();
        String ownerId = feed.getOwnerId();
        if (isNoneBlank(ownerName, ownerEmail)) {
            return String.format(OWNER_NAME_AND_EMAIL_TEMPLATE, ownerName, ownerEmail);
        } else if (isNotBlank(ownerName)) {
            return ownerName;
        } else if (isNotBlank(ownerEmail)) {
            return ownerEmail;
        } else if (isNotBlank(ownerId)) {
            return ownerId;
        }

        return EMPTY;
    }

    private static final String QUEUE_CREATED_BY_TEMPLATE = "Queue created by %s";

    private String getDescription(String owner) {
        return isNotBlank(owner) ? String.format(QUEUE_CREATED_BY_TEMPLATE, owner) : EMPTY;
    }

    public String generateOpml(String username) throws DataAccessException, OpmlException {
        List<FeedDefinition> feedDefinitions = feedDefinitionDao.findByUser(username);
        Opml opml = new Opml();
        opml.setTitle("OPML Export for " + username);
        for (FeedDefinition f : feedDefinitions) {
            List<QueryDefinition> q = queryDefinitionDao.findByFeedId(username, f.getId());
            opml.getOutlines().add(convertFeedDefinitionToOutline(f, q));
        }
        OPML20Generator opml20Generator = new OPML20Generator();
        try {
            Document opmlDocument = opml20Generator.generate(opml);
            XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
            return outputter.outputString(opmlDocument);
        } catch (FeedException e) {
            throw new OpmlException(e.getMessage());
        }
    }

    private Outline convertFeedDefinitionToOutline(FeedDefinition feedDefinition, List<QueryDefinition> queryDefinitions) {
        Outline outline = new Outline();
        outline.setTitle(feedDefinition.getTitle());
        outline.setText(feedDefinition.getDescription());
        for (QueryDefinition q : queryDefinitions) {
            String queryType = q.getQueryType();
            if (queryType.equals("RSS") || queryType.equals("ATOM")) {
                outline.getChildren().add(convertQueryDefinitionToOutline(q));
            }
        }

        return outline;
    }

    private Outline convertQueryDefinitionToOutline(QueryDefinition queryDefinition) {
        URL xmlUrl = null;
        try {
            xmlUrl = new URL(queryDefinition.getQueryText());
        } catch (MalformedURLException ignored) {}

        return new Outline(null, xmlUrl, null);
    }

    public static class RssAtomUrlValidationException extends ValidationException {
        RssAtomUrlValidationException(Set<ConstraintViolation<RssAtomUrl>> constraintViolations) {
            super(constraintViolations.stream().map(ConstraintViolation::getMessage).collect(joining("; ")));
        }
    }

    public static class FeedConfigRequestValidationException extends ValidationException {
        FeedConfigRequestValidationException(Set<ConstraintViolation<FeedConfigRequest>> constraintViolations) {
            super(constraintViolations.stream().map(ConstraintViolation::getMessage).collect(joining("; ")));
        }
    }

    private FeedConfigRequest buildFeedConfigRequest(String ident, String title, String description, List<Outline> outlines) {

        // ea. outline is a query

        return FeedConfigRequest.from(
                ident,
                title,
                description,
                null,
                null,
                buildRssAtomFeedUrls(outlines, 0),
                null,
                null,
                null,
                null
        );
    }

    private List<RssAtomUrl> buildRssAtomFeedUrls(List<Outline> outlines, int depth) {
        if (depth > 2) {
            return emptyList();
        }
        int outlineCt = size(outlines);
        List<RssAtomUrl> rssAtomUrls = newArrayListWithCapacity(outlineCt);
        for (Outline outline  : outlines) {
            if (startsWithIgnoreCase(outline.getType(), "rss")) {
                RssAtomUrl rssAtomUrl = new RssAtomUrl(nextLong(), outline.getXmlUrl());
                Set<ConstraintViolation<RssAtomUrl>> constraintViolations = validator.validate(rssAtomUrl);
                if (isNotEmpty(constraintViolations)) {
                    throw new RssAtomUrlValidationException(constraintViolations);
                }
                rssAtomUrls.add(rssAtomUrl);
            } else {
                List<Outline> children = outline.getChildren();
                rssAtomUrls.addAll(buildRssAtomFeedUrls(children, depth + 1));
            }
        }

        return rssAtomUrls;
    }
}
