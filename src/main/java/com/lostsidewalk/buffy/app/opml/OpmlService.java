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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.lostsidewalk.buffy.app.utils.WordUtils.randomWords;
import static java.util.Collections.singletonList;
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
            List<FeedConfigRequest> feedConfigRequests = importOpml(feed);
            for (FeedConfigRequest feedConfigRequest : feedConfigRequests) {
                Set<ConstraintViolation<FeedConfigRequest>> constraintViolations = validator.validate(feedConfigRequest);
                if (isNotEmpty(constraintViolations)) {
                    throw new FeedConfigRequestValidationException(constraintViolations);
                }
            }
            return feedConfigRequests;
        } catch (Exception e) {
            throw new OpmlException(e.getMessage());
        }
    }

    private List<FeedConfigRequest> importOpml(Opml opml) {

        List<FeedConfigRequest> feedConfigRequests = new ArrayList<>();

        String ident = generateRandomFeedIdent();
        String title = opml.getTitle();
        String description = getDescription(getOwner(opml));
        List<Outline> outlines = opml.getOutlines();
        List<RssAtomUrl> rssAtomUrls = new ArrayList<>();
        for (Outline outline : outlines) {
            if (equalsIgnoreCase(outline.getType(), "rss")) {
                RssAtomUrl rssAtomUrl = new RssAtomUrl(generateRandomId(), outline.getXmlUrl(), outline.getTitle(), null, null, null);
                Set<ConstraintViolation<RssAtomUrl>> constraintViolations = validator.validate(rssAtomUrl);
                if (isNotEmpty(constraintViolations)) {
                    throw new RssAtomUrlValidationException(constraintViolations);
                } else {
                    rssAtomUrls.add(rssAtomUrl);
                }
            } else {
                feedConfigRequests.addAll(buildFeedConfigRequests(singletonList(outline)));
            }
        }
        FeedConfigRequest topLevelFeedConfigRequest = null;
        if (!rssAtomUrls.isEmpty()) {
            topLevelFeedConfigRequest = FeedConfigRequest.from(
                    ident,
                    title,
                    description,
                    "FeedGears 0.4",
                    rssAtomUrls,
                    null,
                    null,
                    null,
                    null);
        }
        if (topLevelFeedConfigRequest != null) {
            feedConfigRequests.add(topLevelFeedConfigRequest);
        }

        return feedConfigRequests;
    }

    private List<FeedConfigRequest> buildFeedConfigRequests(List<Outline> outlines) {

        List<FeedConfigRequest> feedConfigRequests = new ArrayList<>();
        for (Outline outline : outlines) {
            String ident = generateRandomFeedIdent();
            String title = outline.getTitle();
            String description = outline.getText();
            List<Outline> children = outline.getChildren();
            List<RssAtomUrl> rssAtomUrls = new ArrayList<>();
            for (Outline child : children) {
                if (equalsIgnoreCase(child.getType(), "rss")) {
                    RssAtomUrl rssAtomUrl = new RssAtomUrl(generateRandomId(), child.getXmlUrl(), child.getTitle(), null, null, null);
                    Set<ConstraintViolation<RssAtomUrl>> constraintViolations = validator.validate(rssAtomUrl);
                    if (isNotEmpty(constraintViolations)) {
                        throw new RssAtomUrlValidationException(constraintViolations);
                    }
                    rssAtomUrls.add(rssAtomUrl);
                } else {
                    feedConfigRequests.addAll(buildFeedConfigRequests(outline.getChildren()));
                }
            }
            if (isNotBlank(title) || isNotEmpty(rssAtomUrls)) {
                feedConfigRequests.add(FeedConfigRequest.from(
                        ident,
                        title,
                        description,
                        "FeedGears 0.4",
                        rssAtomUrls,
                        null,
                        null,
                        null,
                        null
                ));
            }
        }

        return feedConfigRequests;
    }

    //
    //
    //

    private static String generateRandomFeedIdent() {
        return randomWords();
    }

    private static long generateRandomId() {
        return nextLong();
    }

    private static final String OWNER_NAME_AND_EMAIL_TEMPLATE = "%s (%s)";

    private static String getOwner(Opml opml) {
        String ownerName = opml.getOwnerName();
        String ownerEmail = opml.getOwnerEmail();
        String ownerId = opml.getOwnerId();
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

    private static final String QUEUE_CREATED_BY_TEMPLATE = "Queue created via OMPL import, owner=by %s";

    private static String getDescription(String owner) {
        return isNotBlank(owner) ? String.format(QUEUE_CREATED_BY_TEMPLATE, owner) : EMPTY;
    }

    //
    //
    //

    public String generateOpml(String username) throws DataAccessException, OpmlException {
        List<FeedDefinition> feedDefinitions = feedDefinitionDao.findByUser(username);
        Opml opml = new Opml();
        opml.setTitle("FeedGears OPML Export for " + username);
        if (isNotEmpty(feedDefinitions)) {
            for (FeedDefinition f : feedDefinitions) {
                List<QueryDefinition> q = queryDefinitionDao.findByFeedId(username, f.getId());
                opml.getOutlines().add(convertFeedDefinitionToOutline(f, q));
            }
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

    //
    //
    //

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
}
