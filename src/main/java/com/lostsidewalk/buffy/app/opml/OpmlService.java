package com.lostsidewalk.buffy.app.opml;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.app.audit.OpmlException;
import com.lostsidewalk.buffy.app.model.request.QueueConfigRequest;
import com.lostsidewalk.buffy.app.model.request.Subscription;
import com.lostsidewalk.buffy.queue.QueueDefinition;
import com.lostsidewalk.buffy.queue.QueueDefinitionDao;
import com.lostsidewalk.buffy.subscription.SubscriptionDefinition;
import com.lostsidewalk.buffy.subscription.SubscriptionDefinitionDao;
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

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.lostsidewalk.buffy.app.utils.WordUtils.randomWords;
import static java.nio.charset.StandardCharsets.UTF_8;
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
    QueueDefinitionDao queueDefinitionDao;

    @Autowired
    SubscriptionDefinitionDao subscriptionDefinitionDao;

    public List<QueueConfigRequest> parseOpmlFile(InputStream inputStream) throws OpmlException, IOException {
        //
        // extract the OPML text from the input stream for logging
        //
        StringBuilder opmlTextBuilder = new StringBuilder();
        try (Reader reader = new BufferedReader(new InputStreamReader(inputStream, Charset.forName(UTF_8.name())))) {
            int c;
            while ((c = reader.read()) != -1) {
                opmlTextBuilder.append((char) c);
            }
        }
        String opmlText = opmlTextBuilder.toString();
        log.info("OPML parse, file={}", opmlText);
        //
        // re-create the input stream for processing
        //
        InputStream textStream = new ByteArrayInputStream(opmlText.getBytes(UTF_8));
        try {
            WireFeedInput input = new WireFeedInput();
            InputStreamReader isr = new InputStreamReader(textStream);
            Opml feed = (Opml) input.build(isr);
            List<QueueConfigRequest> queueConfigRequests = importOpml(feed);
            for (QueueConfigRequest queueConfigRequest : queueConfigRequests) {
                Set<ConstraintViolation<QueueConfigRequest>> constraintViolations = validator.validate(queueConfigRequest);
                if (isNotEmpty(constraintViolations)) {
                    throw new QueueConfigRequestValidationException(constraintViolations);
                }
            }
            return queueConfigRequests;
        } catch (Exception e) {
            throw new OpmlException(e.getMessage());
        }
    }

    private List<QueueConfigRequest> importOpml(Opml opml) {

        List<QueueConfigRequest> queueConfigRequests = new ArrayList<>();

        String ident = generateRandomQueueIdent();
        String title = opml.getTitle();
        String description = getDescription(getOwner(opml));
        List<Outline> outlines = opml.getOutlines();
        List<Subscription> subscriptions = new ArrayList<>();
        for (Outline outline : outlines) {
            if (equalsIgnoreCase(outline.getType(), "rss")) {
                Subscription subscription = new Subscription(generateRandomId(), outline.getXmlUrl(), outline.getTitle(), null, null, null);
                Set<ConstraintViolation<Subscription>> constraintViolations = validator.validate(subscription);
                if (isNotEmpty(constraintViolations)) {
                    throw new SubscriptionValidationException(constraintViolations);
                } else {
                    subscriptions.add(subscription);
                }
            } else {
                queueConfigRequests.addAll(buildQueueConfigRequests(singletonList(outline)));
            }
        }
        QueueConfigRequest topLevelQueueConfigRequest = null;
        if (!subscriptions.isEmpty()) {
            topLevelQueueConfigRequest = QueueConfigRequest.from(
                    ident,
                    title,
                    description,
                    "FeedGears 0.4",
                    subscriptions,
                    null,
                    null,
                    null,
                    null);
        }
        if (topLevelQueueConfigRequest != null) {
            queueConfigRequests.add(topLevelQueueConfigRequest);
        }

        return queueConfigRequests;
    }

    private List<QueueConfigRequest> buildQueueConfigRequests(List<Outline> outlines) {

        List<QueueConfigRequest> queueConfigRequests = new ArrayList<>();
        for (Outline outline : outlines) {
            String ident = generateRandomQueueIdent();
            String title = outline.getTitle();
            String description = null;
            if (isBlank(title)) {
                title = outline.getText();
            } else {
                description = outline.getText();
            }
            List<Outline> children = outline.getChildren();
            List<Subscription> subscriptions = new ArrayList<>();
            for (Outline child : children) {
                if (equalsIgnoreCase(child.getType(), "rss")) {
                    Subscription subscription = new Subscription(generateRandomId(), child.getXmlUrl(), child.getTitle(), null, null, null);
                    Set<ConstraintViolation<Subscription>> constraintViolations = validator.validate(subscription);
                    if (isNotEmpty(constraintViolations)) {
                        throw new SubscriptionValidationException(constraintViolations);
                    }
                    subscriptions.add(subscription);
                } else {
                    List<QueueConfigRequest> fromChildren = buildQueueConfigRequests(singletonList(child));
                    if (isNotEmpty(fromChildren)) {
                        queueConfigRequests.addAll(fromChildren);
                    }
                }
            }
            if (isNotBlank(title) || isNotEmpty(subscriptions)) {
                queueConfigRequests.add(QueueConfigRequest.from(
                        ident,
                        title,
                        description,
                        "FeedGears 0.4",
                        subscriptions,
                        null,
                        null,
                        null,
                        null
                ));
            }
        }

        return queueConfigRequests;
    }

    //
    //
    //

    private static String generateRandomQueueIdent() {
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
        List<QueueDefinition> feedDefinitions = queueDefinitionDao.findByUser(username);
        Opml opml = new Opml();
        opml.setTitle("FeedGears OPML Export for " + username);
        if (isNotEmpty(feedDefinitions)) {
            for (QueueDefinition f : feedDefinitions) {
                List<SubscriptionDefinition> q = subscriptionDefinitionDao.findByQueueId(username, f.getId());
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

    private Outline convertFeedDefinitionToOutline(QueueDefinition feedDefinition, List<SubscriptionDefinition> subscriptionDefinitions) {
        Outline outline = new Outline();
        outline.setTitle(feedDefinition.getTitle());
        outline.setText(feedDefinition.getDescription());
        for (SubscriptionDefinition q : subscriptionDefinitions) {
            String queryType = q.getQueryType();
            if (queryType.equals("RSS") || queryType.equals("ATOM")) {
                outline.getChildren().add(convertSubscriptionDefinitionToOutline(q));
            }
        }

        return outline;
    }

    private Outline convertSubscriptionDefinitionToOutline(SubscriptionDefinition subscriptionDefinition) {
        URL xmlUrl = null;
        try {
            xmlUrl = new URL(subscriptionDefinition.getUrl());
        } catch (MalformedURLException ignored) {}

        return new Outline(null, xmlUrl, null);
    }

    //
    //
    //

    public static class SubscriptionValidationException extends ValidationException {
        SubscriptionValidationException(Set<ConstraintViolation<Subscription>> constraintViolations) {
            super(constraintViolations.stream().map(ConstraintViolation::getMessage).collect(joining("; ")));
        }
    }

    public static class QueueConfigRequestValidationException extends ValidationException {
        QueueConfigRequestValidationException(Set<ConstraintViolation<QueueConfigRequest>> constraintViolations) {
            super(constraintViolations.stream().map(ConstraintViolation::getMessage).collect(joining("; ")));
        }
    }
}
