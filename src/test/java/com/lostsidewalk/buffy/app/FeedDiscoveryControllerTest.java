package com.lostsidewalk.buffy.app;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.lostsidewalk.buffy.discovery.FeedDiscoveryInfo;
import com.lostsidewalk.buffy.post.ContentObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static com.lostsidewalk.buffy.app.model.TokenType.APP_AUTH;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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
            singletonList("testSupportedType"),
            "testWebMaster",
            "testUri",
            singletonList("testCategory"),
            null,
            false
    );
    static {
        TEST_FEED_DISCOVERY_INFO.setId(1L);
    }

    private static final Gson GSON = new Gson();

    @Test
    void test_getFeedDiscovery() throws Exception {
        when(this.feedDiscoveryService.performDiscovery("http://test.com/rss")).thenReturn(TEST_FEED_DISCOVERY_INFO);
        when(this.thumbnailService.addThumbnailToResponse(any(FeedDiscoveryInfo.class))).thenCallRealMethod();
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/discovery/").queryParam("url", "http://test.com/rss")
                        .header("Authorization", "Bearer testToken")
                        .accept(APPLICATION_JSON))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"id\":1,\"feedUrl\":\"testUrl\",\"title\":{\"type\":\"text\",\"value\":\"testPostTitle\"},\"description\":{\"type\":\"text\",\"value\":\"testPostDescription\"},\"feedType\":\"testFeedType\",\"author\":\"testAuthor\",\"copyright\":\"testCopyright\",\"docs\":\"testDocs\",\"encoding\":\"testEncoding\",\"generator\":\"testGenerator\",\"image\":null,\"icon\":null,\"language\":\"en-US\",\"link\":\"testLink\",\"managingEditor\":\"testManagingEditor\",\"publishedDate\":null,\"styleSheet\":\"testStyleSheet\",\"supportedTypes\":[\"testSupportedType\"],\"webMaster\":\"testWebMaster\",\"uri\":\"testUri\",\"categories\":[\"testCategory\"],\"sampleEntries\":null}", JsonObject.class),
                            GSON.fromJson(responseContent, JsonObject.class)
                    );
                })
                .andExpect(status().isOk());
    }
}
