package com.lostsidewalk.buffy.app.query;

import com.google.gson.JsonObject;
import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataConflictException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.model.request.Subscription;
import com.lostsidewalk.buffy.subscription.SubscriptionDefinition;
import com.lostsidewalk.buffy.subscription.SubscriptionDefinitionDao;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.*;

import static com.lostsidewalk.buffy.rss.RssImporter.RSS;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Slf4j
@Service
public class SubscriptionDefinitionService {

    @Autowired
    SubscriptionDefinitionDao subscriptionDefinitionDao;

    public SubscriptionDefinition findById(String username, Long id) throws DataAccessException {
        return subscriptionDefinitionDao.findById(username, id);
    }

    public List<SubscriptionDefinition> findByUsername(String username) throws DataAccessException {
        return subscriptionDefinitionDao.findByUsername(username);
    }

    public List<SubscriptionDefinition> findByQueueId(String username, Long id) throws DataAccessException {
        return subscriptionDefinitionDao.findByQueueId(username, id);
    }
    //
    // called by queue definition controller
    //
    public List<SubscriptionDefinition> updateSubscriptions(String username, Long queueId, List<Subscription> subscriptions) throws DataAccessException {
        //
        Map<Long, SubscriptionDefinition> currentSubscriptionDefinitionsById = subscriptionDefinitionDao.findByQueueId(username, queueId, RSS)
                .stream().collect(toMap(SubscriptionDefinition::getId, v -> v));
        //
        List<SubscriptionDefinition> updatedSubscriptions = new ArrayList<>();
        //
        if (isNotEmpty(subscriptions)) {
            for (Subscription r : subscriptions) {
                if (r.getId() != null) {
                    SubscriptionDefinition q = currentSubscriptionDefinitionsById.get(r.getId());
                    if (q != null) {
                        if (needsUpdate(r, q)) {
                            q.setTitle(r.getTitle());
                            q.setImgUrl(r.getImgUrl());
                            q.setUrl(r.getUrl());
                            String feedUsername = r.getUsername();
                            String feedPassword = r.getPassword();
                            if (StringUtils.isNotBlank(feedUsername) || StringUtils.isNotBlank(feedPassword)) {
                                q.setQueryConfig(serializeQueryConfig(r));
                            } else {
                                q.setQueryConfig(null);
                            }
                            updatedSubscriptions.add(q);
                        }
                    }
                } // r.getId() == null case is ignored
            }
            if (isNotEmpty(updatedSubscriptions)) {
                subscriptionDefinitionDao.updateSubscriptions(updatedSubscriptions.stream().map(u -> new Object[] {
                        u.getTitle(),
                        u.getImgUrl(),
                        u.getUrl(),
                        u.getQueryType(),
                        u.getImportSchedule(),
                        u.getQueryConfig(),
                        u.getId()
                }).collect(toList()));
            }
        }

        return updatedSubscriptions;
    }
    //
    // called by query creation task processor
    //
    List<SubscriptionDefinition> addSubscriptions(String username, Long queueId, List<Subscription> subscriptions) throws DataAccessException, DataUpdateException, DataConflictException {
        List<SubscriptionDefinition> createdQueries = new ArrayList<>();
        if (isNotEmpty(subscriptions)) {
            createdQueries.addAll(doAddSubscriptions(username, queueId, subscriptions));
        }

        return createdQueries;
    }

    private List<SubscriptionDefinition> doAddSubscriptions(String username, Long queueId, List<Subscription> subscriptions) throws DataAccessException, DataUpdateException, DataConflictException {
        List<SubscriptionDefinition> adds = new ArrayList<>();
        for (Subscription r : subscriptions) {
            String url = r.getUrl();
            if (isBlank(url)) {
                log.warn("Query is missing a URL, skipping...");
                continue;
            }
            String title = r.getTitle();
            String imageUrl = r.getImgUrl();
            SubscriptionDefinition newQuery = SubscriptionDefinition.from(
                    queueId,
                    username,
                    title,
                    imageUrl,
                    url,
                    RSS,
                    "A",
                    serializeQueryConfig(r)
            );
            adds.add(newQuery);
        }
        List<Long> subscriptionIds = subscriptionDefinitionDao.add(adds);

        return subscriptionDefinitionDao.findByIds(username, subscriptionIds);
    }

    private static boolean containsIgnoreCase(List<String> strSet, String str) {
        if (isNotEmpty(strSet)) {
            return strSet.stream().anyMatch(s -> s.equalsIgnoreCase(str));
        }
        return false;
    }
    //
    // called by queue definition controller
    //
    public List<SubscriptionDefinition> createQueries(String username, Long queueId, List<Subscription> subscriptions) throws DataAccessException, DataUpdateException, DataConflictException {
        List<SubscriptionDefinition> createdSubscriptions = new ArrayList<>();
        if (isNotEmpty(subscriptions)) {
            List<SubscriptionDefinition> toImport = new ArrayList<>();
            if (isNotEmpty(subscriptions)) {
                List<String> existingSubscriptionUrls = subscriptionDefinitionDao.getSubscriptionUrlsByUsername(username);
                List<SubscriptionDefinition> adds = new ArrayList<>();
                for (Subscription r : subscriptions) {
                    if (containsIgnoreCase(existingSubscriptionUrls, r.getUrl())) {
                        log.debug("Feed subscription is already defined for user, feedUrl={}, username={}", r.getUrl(), username);
                        continue;
                    }
                    SubscriptionDefinition newQuery = SubscriptionDefinition.from(
                            queueId,
                            username,
                            r.getTitle(),
                            r.getImgUrl(),
                            r.getUrl(),
                            RSS,
                            "A",
                            serializeQueryConfig(r)
                    );
                    adds.add(newQuery);
                }
                if (isNotEmpty(adds)) {
                    // apply additions
                    List<Long> subscriptionIds = subscriptionDefinitionDao.add(adds);
                    // re-select
                    List<SubscriptionDefinition> newSubscriptions = subscriptionDefinitionDao.findByIds(username, subscriptionIds);
                    // mark for import
                    toImport.addAll(newSubscriptions);
                }
            }

            createdSubscriptions.addAll(toImport);
        }

        return createdSubscriptions;
    }
    //
    // called by queue definition controller
    //
    public void deleteSubscriptionById(String username, Long queueId, Long subscriptionId) throws DataAccessException, DataUpdateException {
        // delete this query
        subscriptionDefinitionDao.deleteById(username, queueId, subscriptionId);
    }

    private boolean needsUpdate(Subscription subscription, SubscriptionDefinition q) {
        if (!StringUtils.equals(q.getTitle(), subscription.getTitle())) {
            return true;
        }
        if (!StringUtils.equals(q.getImgUrl(), subscription.getImgUrl())) {
            return true;
        }
        if (!StringUtils.equals(q.getUrl(), subscription.getUrl())) {
            return true;
        }
        return !Objects.equals(serializeQueryConfig(subscription), q.getQueryConfig());
    }

    private Serializable serializeQueryConfig(Subscription subscription) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("username", subscription.getUsername());
        jsonObject.addProperty("password", subscription.getPassword());
        return jsonObject.toString();
    }
}
