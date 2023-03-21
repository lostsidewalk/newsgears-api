package com.lostsidewalk.buffy.app;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.lostsidewalk.buffy.app.model.request.FeedDiscoveryRequest;
import com.lostsidewalk.buffy.discovery.FeedDiscoveryInfo;
import com.lostsidewalk.buffy.post.ContentObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.ArrayList;

import static com.lostsidewalk.buffy.app.model.TokenType.APP_AUTH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = FeedDiscoveryController.class)
public class FeedDiscoveryControllerTest extends BaseWebControllerTest {

    @BeforeEach
    void test_setup() throws Exception {
        when(this.tokenService.instanceFor(APP_AUTH, "testToken")).thenReturn(TEST_JWT_UTIL);
        when(this.authService.requireAuthClaim("me")).thenReturn("testAuthClaim");
        when(this.userService.loadUserByUsername("me")).thenReturn(TEST_USER_DETAILS);
    }

    protected static final ContentObject TEST_POST_TITLE = ContentObject.from("text", "testPostTitle");

    protected static final ContentObject TEST_POST_DESCRIPTION = ContentObject.from("text", "testPostDescription");

    private static final FeedDiscoveryInfo TEST_FEED_DISCOVERY_INFO = FeedDiscoveryInfo.from(
            "testUrl",
            200,
            "OK",
            null,
            null,
            null,
            TEST_POST_TITLE,
            TEST_POST_DESCRIPTION,
            "testFeedType",
            "testAuthor",
            "testCopyright",
            "testDocs",
            "testEncoding",
            "testGenerator",
            null,
            null,
            "en-US",
            "testLink",
            "testManagingEditor",
            null,
            "testStyleSheet",
            singletonArrayList("testSupportedType"),
            "testWebMaster",
            "testUri",
            singletonArrayList("testCategory"),
            null,
            false
    );
    static {
        TEST_FEED_DISCOVERY_INFO.setId(1L);
    }

    private static final Gson GSON = new Gson();

    private static final FeedDiscoveryRequest TEST_FEED_DISCOVERY_REQUEST = new FeedDiscoveryRequest(
            "http://test.com/rss",
            "testUsername",
            "testPassword"
    );

    @Test
    void test_getFeedDiscovery() throws Exception {
        when(this.feedDiscoveryService.performDiscovery(eq("http://test.com/rss"), eq("testUsername"), eq("testPassword"))).thenReturn(TEST_FEED_DISCOVERY_INFO);
        when(this.thumbnailService.addThumbnailToResponse(any(FeedDiscoveryInfo.class))).thenCallRealMethod();
        mockMvc.perform(MockMvcRequestBuilders
                        .post("/discovery")
                        .contentType(APPLICATION_JSON)
                        .content(GSON.toJson(TEST_FEED_DISCOVERY_REQUEST))
                        .header("Authorization", "Bearer testToken")
                        .accept(APPLICATION_JSON))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"id\":1,\"feedUrl\":\"testUrl\",\"httpStatusCode\":200,\"httpStatusMessage\":\"OK\",\"redirectFeedUrl\":null,\"redirectHttpStatusCode\":null,\"redirectHttpStatusMessage\":null,\"title\":{\"type\":\"text\",\"value\":\"testPostTitle\"},\"description\":{\"type\":\"text\",\"value\":\"testPostDescription\"},\"feedType\":\"testFeedType\",\"author\":\"testAuthor\",\"copyright\":\"testCopyright\",\"docs\":\"testDocs\",\"encoding\":\"testEncoding\",\"generator\":\"testGenerator\",\"image\":null,\"icon\":null,\"language\":\"en-US\",\"link\":\"testLink\",\"managingEditor\":\"testManagingEditor\",\"publishedDate\":null,\"styleSheet\":\"testStyleSheet\",\"supportedTypes\":[\"testSupportedType\"],\"webMaster\":\"testWebMaster\",\"uri\":\"testUri\",\"categories\":[\"testCategory\"],\"sampleEntries\":null,\"urlUpgradable\":false}", JsonObject.class),
                            GSON.fromJson(responseContent, JsonObject.class)
                    );
                })
                .andExpect(status().isOk());
    }

    private static <T> ArrayList<T> singletonArrayList(T item) {
        if (item != null) {
            ArrayList<T> l = new ArrayList<>();
            l.add(item);
            return l;
        }

        return null;
    }
}
