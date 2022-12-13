package com.lostsidewalk.buffy.app;

import com.lostsidewalk.buffy.app.model.response.FeedDiscoveryInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static com.lostsidewalk.buffy.app.model.TokenType.APP_AUTH;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

    private static final FeedDiscoveryInfo TEST_FEED_DISCOVERY_INFO = FeedDiscoveryInfo.from(
            "testTitle",
            "testDescription",
            "testFeedType",
            "testAuthor",
            "testCcopyright",
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
            null,
            "testWebMaster",
            "testUri",
            null
    );

    @Test
    void test_getFeedDiscovery() throws Exception {
        when(this.feedDiscoveryService.performDiscovery("http://test.com/rss")).thenReturn(TEST_FEED_DISCOVERY_INFO);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/discovery/").queryParam("url", "http://test.com/rss")
                        .header("Authorization", "Bearer testToken")
                        .accept(APPLICATION_JSON))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("{\"title\":\"testTitle\",\"description\":\"testDescription\",\"feedType\":\"testFeedType\",\"author\":\"testAuthor\",\"copyright\":\"testCcopyright\",\"docs\":\"testDocs\",\"encoding\":\"testEncoding\",\"generator\":\"testGenerator\",\"image\":null,\"icon\":null,\"language\":\"en-US\",\"link\":\"testLink\",\"managingEditor\":\"testManagingEditor\",\"publishedDate\":null,\"styleSheet\":\"testStyleSheet\",\"supportedTypes\":null,\"webMaster\":\"testWebMaster\",\"uri\":\"testUri\",\"sampleEntries\":null}", responseContent);
                })
                .andExpect(status().isOk());
    }
}
