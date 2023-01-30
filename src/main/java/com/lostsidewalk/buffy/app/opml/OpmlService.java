package com.lostsidewalk.buffy.app.opml;

import com.lostsidewalk.buffy.DataAccessException;
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
import java.util.*;

import static java.util.stream.Collectors.joining;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
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

    public List<FeedConfigRequest> parseOpmlFile(InputStream inputStream) throws OpmlException {
        try {
            WireFeedInput input = new WireFeedInput();
            InputStreamReader isr = new InputStreamReader(inputStream);
            Opml feed = (Opml) input.build(isr);

            return buildFeedConfigRequests(EMPTY, feed.getOutlines());
        } catch (Exception e) {
            throw new OpmlException(e.getMessage());
        }
    }

    static final Comparator<Outline> OUTLINE_COMPARATOR = (outline, other) -> {
        boolean outlineIsRss = startsWithIgnoreCase(outline.getType(), "rss");
        boolean otherIsRss = startsWithIgnoreCase(other.getType(), "rss");
        if (outlineIsRss && !otherIsRss) {
            return -1;
        } else if (otherIsRss && !outlineIsRss) {
            return 1;
        } else {
            return 0;
        }
    };

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

    private List<FeedConfigRequest> buildFeedConfigRequests(String prefix, List<Outline> outlines) {
        List<FeedConfigRequest> feedConfigRequests = new ArrayList<>();
        for (Outline outline : outlines) {
            String titleWithPrefix = isEmpty(prefix) ? outline.getTitle() : (prefix + " - " +  outline.getTitle());
            FeedConfigRequest feedConfigRequest = FeedConfigRequest.from(
                    titleWithPrefix,
                    titleWithPrefix,
                    outline.getText(),
                    null, // generator
                    null, // NewsApiV2 query
                    new ArrayList<>(), // rssAtomFeedUrls
                    null, // export config
                    null, // copyright
                    null, // language
                    null // image source
            );
            List<Outline> children = outline.getChildren();
            if (isNotEmpty(children)) {
                children.sort(OUTLINE_COMPARATOR);
                Iterator<Outline> childIter = children.iterator();
                while (childIter.hasNext()) {
                    Outline nextChild = childIter.next();
                    if (startsWithIgnoreCase(nextChild.getType(), "rss")) {
                        RssAtomUrl rssAtomUrl = new RssAtomUrl(nextLong(), nextChild.getXmlUrl());
                        Set<ConstraintViolation<RssAtomUrl>> constraintViolations = validator.validate(rssAtomUrl);
                        if (isNotEmpty(constraintViolations)) {
                            throw new RssAtomUrlValidationException(constraintViolations);
                        }
                        feedConfigRequest.getRssAtomFeedUrls().add(rssAtomUrl);
                        childIter.remove();
                    } else {
                        break;
                    }
                }
                Set<ConstraintViolation<FeedConfigRequest>> constraintViolations = validator.validate(feedConfigRequest);
                if (isNotEmpty(constraintViolations)) {
                    throw new FeedConfigRequestValidationException(constraintViolations);
                }
                feedConfigRequests.add(feedConfigRequest);
                if (!children.isEmpty()) {
                    feedConfigRequests.addAll(buildFeedConfigRequests(outline.getTitle(), children));
                }
            }
        }

        return feedConfigRequests;
    }
}
/*
 * for ea. outline:
 *   add a feed config request with this parent name + this name
 *   does it contain children?
 *     yes:
 *       sort the children, RSS first
 *       add all RSS URL children
 *       anything left?
 *         yes:
 *           recurse over non-RSS/URL children with parent name eq the outline name
 *         no:
 *           continue
 *     no:
 *       continue
 */
