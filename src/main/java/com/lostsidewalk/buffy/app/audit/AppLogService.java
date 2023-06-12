package com.lostsidewalk.buffy.app.audit;

import com.lostsidewalk.buffy.publisher.Publisher.PubResult;
import com.lostsidewalk.buffy.auth.User;
import com.lostsidewalk.buffy.app.model.request.FeedStatusUpdateRequest;
import com.lostsidewalk.buffy.app.model.request.PostStatusUpdateRequest;
import com.lostsidewalk.buffy.app.model.request.SettingsUpdateRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.stereotype.Service;

import java.util.List;

import static java.lang.System.arraycopy;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@Slf4j(topic = "appLog")
@Service
public class AppLogService {

    public void logFeedFetch(String username, StopWatch stopWatch, int feedCt, int queryCt) {
        auditLog("feed-fetch", "feedCt={}, queryCt={}", username, stopWatch, feedCt, queryCt);
    }

    public void logFeedStatusUpdate(String username, StopWatch stopWatch, Long id, FeedStatusUpdateRequest feedStatusUpdateRequest, int rowsUpdated) {
        auditLog("feed-status-update", "id={}, feedStatusUpdateRequest={}, rowsUpdated={}", username, stopWatch, id, feedStatusUpdateRequest, rowsUpdated);
    }

    public void logFeedUpdate(String username, StopWatch stopWatch, Long id) {
        auditLog("feed-update", "id={}", username, stopWatch, id);
    }

    public void logFeedCreate(String username, StopWatch stopWatch, int length, int size) {
        auditLog("feed-create", "length={}, size={}", username, stopWatch, length, size);
    }

    public void logAddQueries(String username, StopWatch stopWatch, int size) {
        auditLog("add-queries", "size={}", username, stopWatch, size);
    }

    public void logUpdateSubscriptions(String username, StopWatch stopWatch, int size) {
        auditLog("updates-queries", "size={}", username, stopWatch, size);
    }

    public void logOpmlPreview(String username, StopWatch stopWatch, int opmlFileCt, int queueConfigRequestCt, int errorCt) {
        auditLog("opml-preview", "opmlFileCt={}, queueConfigRequestCt={}, errorCt={}",
                username, stopWatch, opmlFileCt, queueConfigRequestCt, errorCt);
    }

    public void logThumbnailPreview(String username, StopWatch stopWatch, int errorCt) {
        auditLog("thumbnail-preview", "errorCt={}", username, stopWatch, errorCt);
    }

    public void logRandomThumbnailPreview(String username, StopWatch stopWatch) {
        auditLog("random-thumbnail-preview", null, username, stopWatch);
    }

    public void logFeedDelete(String username, StopWatch stopWatch, int deleteCt) {
        auditLog("feed-delete", "deleteCt={}", username, stopWatch, deleteCt);
    }

    public void logQueryDelete(String username, StopWatch stopWatch, int deleteCt) {
        auditLog("query-delete", "deleteCt={}", username, stopWatch, deleteCt);
    }

    public void logLatestQueryMetricsFetch(String username, StopWatch stopWatch, int resultCt) {
        auditLog("latest-query-metrics-fetch", "resultCt={}", username, stopWatch, resultCt);
    }

    public void logFeedDiscovery(String username, StopWatch stopWatch, String url) {
        auditLog("feed-discovery", "url={}", username, stopWatch, url);
    }

    public void logCollectionFetch(String username, StopWatch stopWatch, String collectionName) {
        auditLog("collection-fetch", "collectionName={}", username, stopWatch, collectionName);
    }

    public void logFeedRecommendation(String username, StopWatch stopWatch, String url) {
        auditLog("feed-recommendation", "url={}", username, stopWatch, url);
    }

    public void logCheckoutSessionCreate(String username, StopWatch stopWatch) {
        auditLog("checkout-session-create", null, username, stopWatch);
    }

    public void logSubscriptionFetch(String username, StopWatch stopWatch, int subscriptionCt) {
        auditLog("subscription-fetch", "subscriptionCt={}", username, stopWatch, subscriptionCt);
    }

    public void logSubscriptionCancel(String username, StopWatch stopWatch) {
        auditLog("subscription-cancel", null, username, stopWatch);
    }

    public void logSubscriptionResume(String username, StopWatch stopWatch) {
        auditLog("subscription-resume", null, username, stopWatch);
    }

    public void logOpmlExport(String username, StopWatch stopWatch) {
        auditLog("opml-export", null, username, stopWatch);
    }

    public void logSettingsFetch(String username, StopWatch stopWatch) {
        auditLog("settings-fetch", null, username, stopWatch);
    }

    public void logSettingsUpdate(String username, StopWatch stopWatch, SettingsUpdateRequest settingsUpdateRequest) {
        auditLog("settings-update", "settingsUpdateRequest={}", username, stopWatch, settingsUpdateRequest);
    }

    public void logDisplaySettingsFetch(String username, StopWatch stopWatch) {
        auditLog("display-settings-fetch", null, username, stopWatch);
    }

    public void logDisplaySettingsUpdate(String username, StopWatch stopWatch) {
        auditLog("display-settings-update", "displaySettingsUpdateRequest={}", username, stopWatch);
    }

    public void logStagingPostFetch(String username, StopWatch stopWatch, int queueIdCt, int stagingPostCt) {
        auditLog("staging-post-fetch", "queueIdCt={}, stagingPostCt={}, queryMetricsCt={}", username, stopWatch, queueIdCt, stagingPostCt);
    }

    public void logStagingPostReadStatusUpdate(String username, StopWatch stopWatch, Long id, PostStatusUpdateRequest postStatusUpdateRequest, int rowsUpdated) {
        auditLog("staging-post-read-status-update", "id={}, postStatusUpdateRequest={}, rowsUpdated={}", username, stopWatch, id, postStatusUpdateRequest, rowsUpdated);
    }

    public void logFeedReadStatusUpdate(String username, StopWatch stopWatch, Long id, PostStatusUpdateRequest postStatusUpdateRequest, int rowsUpdated) {
        auditLog("feed-read-status-update", "id={}, postStatusUpdateRequest={}, rowsUpdated={}", username, stopWatch, id, postStatusUpdateRequest, rowsUpdated);
    }

    public void logStagingPostPubStatusUpdate(String username, StopWatch stopWatch, Long id, PostStatusUpdateRequest postStatusUpdateRequest, int rowsUpdated, List<PubResult> publicationResults) {
        auditLog("staging-post-pub-status-update", "id={}, postStatusUpdateRequest={}, rowsUpdated={}, publicationResults={}", username, stopWatch, id, postStatusUpdateRequest, rowsUpdated, publicationResults);
    }

    public void logPasswordResetInit(String username, StopWatch stopWatch) {
        auditLog("password-reset-init", null, username, stopWatch);
    }

    public void logPasswordResetContinue(String username, StopWatch stopWatch) {
        auditLog("password-reset-continue", null, username, stopWatch);
    }

    public void logPasswordResetFinalize(String username, StopWatch stopWatch) {
        auditLog("password-reset-finalize", null, username, stopWatch);
    }

    public void logUserRegistration(String username, StopWatch stopWatch) {
        auditLog("user-registration", null, username, stopWatch);
    }

    public void logUserVerification(String username, StopWatch stopWatch) {
        auditLog("user-verification", null, username, stopWatch);
    }

    public void logUserDeregistration(String username, StopWatch stopWatch) {
        auditLog("user-deregistration", null, username, stopWatch);
    }

    public void logUserUpdate(User user, StopWatch stopWatch) {
        auditLog("user-update", null, user.getUsername(), stopWatch);
    }

//    public void logProxyFetch(String hash, StopWatch stopWatch, String url) {
//        auditLog("proxy-fetch", "hash={}, url={}", null, stopWatch, hash, url);
//    }

    //

    private static void auditLog(String logTag, String formatStr, String username, StopWatch stopWatch, Object... args) {
        String fullFormatStr = "eventType={}, username={}, startTime={}, endTime={}, duration={}";
        if (isNotEmpty(formatStr)) {
            fullFormatStr += (", " + formatStr);
        }
        Object[] allArgs = new Object[args.length + 5];
        allArgs[0] = logTag;
        allArgs[1] = username;
        allArgs[2] = stopWatch.getStartTime();
        allArgs[3] = stopWatch.getStopTime();
        allArgs[4] = stopWatch.getTime();
        arraycopy(args, 0, allArgs, 5, args.length);
        log.info(fullFormatStr, allArgs);
    }

    public void logCustomerCreated(User user, String emailAddress, String customerId) {
        log.info("Processed customer-created event: Setting customer for userId={}, emailAddress={}, customerId={}",
                user.getId(), emailAddress, customerId);
    }

    public void logCustomerSubscriptionDeleted(User user, String customerId) {
        log.info("Customer subscription deleted userId={}, customerId={}", user.getId(), customerId);
    }

    public void logCustomerSubscriptionUpdated(User user, String customerId, String subStatus) {
        log.info("Processed customer-subscription-updated event: Updating subscription status to subStatus={} for customerId={}, userId={}, emailAddress={}",
                subStatus, customerId, user.getId(), user.getEmailAddress());
    }

    public void logInvoicePaid(User user, String emailAddress) {
        log.info("Processed invoice-paid event: Updating subscription expiration to expDate={} for userId={}, emailAddress={}, status={}",
                user.getSubscriptionExpDate(), user.getId(), emailAddress, user.getSubscriptionStatus());
    }
}
