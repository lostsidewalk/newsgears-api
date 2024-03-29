package com.lostsidewalk.buffy.app;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.lostsidewalk.buffy.post.*;
import com.rometools.modules.itunes.EntryInformationImpl;
import com.rometools.modules.itunes.ITunes;
import com.rometools.modules.mediarss.MediaEntryModuleImpl;
import com.rometools.modules.mediarss.types.Metadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Date;
import java.util.List;

import static com.lostsidewalk.buffy.app.model.TokenType.APP_AUTH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = StagingPostController.class)
public class StagingPostControllerTest extends BaseWebControllerTest {

    @BeforeEach
    void test_setup() throws Exception {
        when(this.tokenService.instanceFor(APP_AUTH, "testToken")).thenReturn(TEST_JWT_UTIL);
        when(this.authService.requireAuthClaim("me")).thenReturn("testAuthClaim");
        when(this.userService.loadUserByUsername("me")).thenReturn(TEST_USER_DETAILS);
    }

    protected static final ContentObject TEST_POST_TITLE = new ContentObject();
    static {
        TEST_POST_TITLE.setType("text");
        TEST_POST_TITLE.setValue("testPostTitle");
        TEST_POST_TITLE.setIdent("testPostTitleIdent");
    }

    protected static final ContentObject TEST_POST_DESCRIPTION = new ContentObject();
    static {
        TEST_POST_TITLE.setType("text");
        TEST_POST_TITLE.setValue("testPostDescription");
        TEST_POST_TITLE.setIdent("testPostDescriptionIdent");
    }

    protected static final ContentObject TEST_POST_CONTENT = new ContentObject();
    static {
        TEST_POST_TITLE.setType("text");
        TEST_POST_TITLE.setValue("testPostContent");
        TEST_POST_TITLE.setIdent("testPostTitleContent");
    }

    protected static final PostMedia TEST_POST_MEDIA;
    static {
        MediaEntryModuleImpl testMediaEntryModule = new MediaEntryModuleImpl();
        Metadata metadata = new Metadata();
        testMediaEntryModule.setMetadata(metadata);
        TEST_POST_MEDIA = PostMedia.from(testMediaEntryModule);
    }

    protected static final PostITunes TEST_POST_ITUNES;
    static {
        ITunes testITunes = new EntryInformationImpl();
        TEST_POST_ITUNES = PostITunes.from(testITunes);
    }

    protected static final PostUrl TEST_POST_URL = new PostUrl();
    static {
        TEST_POST_URL.setTitle("testUrlTitle");
        TEST_POST_URL.setRel("testUrlRel");
        TEST_POST_URL.setHref("testUrlHref");
        TEST_POST_URL.setHreflang("testUrlHreflang");
        TEST_POST_URL.setType("testUrlType");
    }

    protected static final PostPerson TEST_POST_CONTRIBUTOR = new PostPerson();
    static {
        TEST_POST_CONTRIBUTOR.setName("testContributorName");
        TEST_POST_CONTRIBUTOR.setUri("testContributorUri");
        TEST_POST_CONTRIBUTOR.setEmail("testContributorEmail");
    }

    protected static final PostPerson TEST_POST_AUTHOR = new PostPerson();
    static {
        TEST_POST_AUTHOR.setName("testAuthorName");
        TEST_POST_AUTHOR.setUri("testAuthorUri");
        TEST_POST_AUTHOR.setEmail("testAuthorEmail");
    }

    protected static final PostEnclosure TEST_POST_ENCLOSURE = new PostEnclosure();
    static {
        TEST_POST_ENCLOSURE.setType("testEnclosureType");
        TEST_POST_ENCLOSURE.setUrl("testEnclosureUrl");
        TEST_POST_ENCLOSURE.setLength(4821L);
    }

    private static final Date NOW = new Date(10_000_000L);

    private static final StagingPost TEST_STAGING_POST = StagingPost.from(
            "testImporterId",
            1L,
            "testImporterDesc",
            2L,
            TEST_POST_TITLE,
            TEST_POST_DESCRIPTION,
            List.of(TEST_POST_CONTENT),
            TEST_POST_MEDIA,
            TEST_POST_ITUNES,
            "testPostUrl",
            List.of(TEST_POST_URL),
            "testPostImgUrl",
            null, // import timestamp
            "testPostHash",
            "me",
            "testPostComment",
            "testPostRights",
            List.of(TEST_POST_CONTRIBUTOR),
            List.of(TEST_POST_AUTHOR),
            List.of("testPostCategory"),
            null, // publish timestamp
            null, // expiration timestamp
            List.of(TEST_POST_ENCLOSURE),
            null // last updated timestamp
    );
    static {
        TEST_STAGING_POST.setCreated(NOW);
    }

    private static final List<StagingPost> TEST_STAGING_POSTS = List.of(TEST_STAGING_POST);

    private static final Gson GSON = new Gson();

    @Test
    void test_getStagingPosts() throws Exception {
        when(this.stagingPostDao.findByUserAndQueueIds("me", List.of(1L))).thenReturn(TEST_STAGING_POSTS);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/staging")
                        .queryParam("queueIds", "1")
                        .header("Authorization", "Bearer testToken")
                        .accept(APPLICATION_JSON))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"stagingPosts\":[{\"post\":{\"importerId\":\"testImporterId\",\"queueId\":1,\"importerDesc\":\"testImporterDesc\",\"subscriptionId\":2,\"postTitle\":{\"ident\":\"testPostTitleContent\",\"type\":\"text\",\"value\":\"testPostContent\"},\"postDesc\":{},\"postContents\":[{}],\"postMedia\":{\"postMediaMetadata\":{}},\"postITunes\":{\"isBlock\":false,\"isExplicit\":false,\"isCloseCaptioned\":false},\"postUrl\":\"testPostUrl\",\"postUrls\":[{\"title\":\"testUrlTitle\",\"type\":\"testUrlType\",\"href\":\"testUrlHref\",\"hreflang\":\"testUrlHreflang\",\"rel\":\"testUrlRel\"}],\"postComment\":\"testPostComment\",\"postRights\":\"testPostRights\",\"contributors\":[{\"name\":\"testContributorName\",\"email\":\"testContributorEmail\",\"uri\":\"testContributorUri\"}],\"authors\":[{\"name\":\"testAuthorName\",\"email\":\"testAuthorEmail\",\"uri\":\"testAuthorUri\"}],\"postCategories\":[\"testPostCategory\"],\"enclosures\":[{\"url\":\"testEnclosureUrl\",\"type\":\"testEnclosureType\",\"length\":4821}],\"created\":\"1970-01-01T02:46:40.000+00:00\",\"archived\":false,\"published\":false}}]}",
                            JsonObject.class), GSON.fromJson(responseContent, JsonObject.class)
                    );
                })
                .andExpect(status().isOk());
    }
}
