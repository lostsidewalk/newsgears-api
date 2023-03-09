package com.lostsidewalk.buffy.app;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lostsidewalk.buffy.feed.FeedDefinition;
import com.lostsidewalk.buffy.app.model.request.FeedConfigRequest;
import com.lostsidewalk.buffy.query.QueryDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;

import static com.lostsidewalk.buffy.app.model.TokenType.APP_AUTH;
import static com.lostsidewalk.buffy.newsapi.NewsApiImporter.NEWSAPIV2_HEADLINES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = FeedDefinitionController.class)
public class FeedDefinitionControllerTest extends BaseWebControllerTest {

    private static final Gson GSON = new Gson();

    private static final FeedDefinition TEST_FEED_DEFINITION = FeedDefinition.from(
            "testFeed",
            "Test Feed Title",
            "Test Feed Description",
            "Test Feed Generator",
            "Test Feed Transport Identifier",
            "me",
            null,
            "Test Feed Copyright",
            "en-US",
            null);
    static {
        TEST_FEED_DEFINITION.setId(1L);
    }

    private static final List<QueryDefinition> TEST_QUERY_DEFINITIONS = List.of(
            QueryDefinition.from(1L, "me", "testQueryTitle", "testQueryText", NEWSAPIV2_HEADLINES, null)
    );
    static {
        TEST_QUERY_DEFINITIONS.get(0).setId(1L);
    }

    private static final FeedConfigRequest TEST_FEED_CONFIG_REQUEST = FeedConfigRequest.from(
            "testIdent",
            "testTitle",
            "testDescription",
            "testGenerator",
            "testNewsApiV2QueryText",
            null,
            null,
            "testCopyright",
            "testLanguage",
            null);

    @BeforeEach
    void test_setup() throws Exception {
        when(this.tokenService.instanceFor(APP_AUTH, "testToken")).thenReturn(TEST_JWT_UTIL);
        when(this.authService.requireAuthClaim("me")).thenReturn("testAuthClaim");
        when(this.userService.loadUserByUsername("me")).thenReturn(TEST_USER_DETAILS);
    }

    @Test
    void test_getFeedIdents() throws Exception {
        when(this.feedDefinitionService.findIdentsByUser("me")).thenReturn(List.of("feed1", "feed2"));
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/feed_idents")
                        .header("Authorization", "Bearer testToken")
                        .accept(APPLICATION_JSON))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("[\"feed1\",\"feed2\"]", JsonArray.class), GSON.fromJson(responseContent, JsonArray.class));
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_getFeeds() {

    }

    @Test
    void test_toggleFeed() {

    }

    @Test
    void test_updateFeed() throws Exception {
        when(feedDefinitionService.findByFeedId(matches("me"), eq(1L)))
                .thenReturn(TEST_FEED_DEFINITION);
        when(queryDefinitionService.findByFeedId(matches("me"), eq(1L)))
                .thenReturn(TEST_QUERY_DEFINITIONS);
        mockMvc.perform(MockMvcRequestBuilders
                .put("/feeds/1")
                .servletPath("/feeds/1")
                .contentType(APPLICATION_JSON)
                .content(GSON.toJson(TEST_FEED_CONFIG_REQUEST))
                .header("Authorization", "Bearer testToken"))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(
                            GSON.fromJson("{\"feedDefinition\":{\"id\":1,\"ident\":\"testFeed\",\"title\":\"Test Feed Title\",\"description\":\"Test Feed Description\",\"generator\":\"Test Feed Generator\",\"transportIdent\":\"Test Feed Transport Identifier\",\"username\":\"me\",\"feedStatus\":\"ENABLED\",\"copyright\":\"Test Feed Copyright\",\"language\":\"en-US\"},\"queryDefinitions\":[{\"queryDefinition\":{\"id\":1,\"feedId\":1,\"username\":\"me\",\"queryTitle\":\"testQueryTitle\",\"queryText\":\"testQueryText\",\"queryType\":\"NEWSAPIV2_HEADLINES\"}}],\"queryMetrics\":{},\"feedImgSrc\":null}", JsonObject.class),
                            GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_createFeed() {

    }

    @Test
    void test_validateOpmlFiles() {

    }

    @Test
    void test_deleteFeedById() {

    }

    @Test
    void test_getQueries() {

    }
}
