package com.lostsidewalk.buffy.app;

import com.lostsidewalk.buffy.post.StagingPost;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

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

    private static final List<StagingPost> TEST_STAGING_POSTS = List.of(
            StagingPost.from(
                    "testImporterId",
                    "testFeedIdent",
                    "testImporterDesc",
                    null, // source
                    "testSourceName",
                    "testSourceUrl",
                    "testPostTitle",
                    "testPostDesc",
                    "testPostUrl",
                    "testPostImgUrl",
                    null, // import timestamp
                    "testPostHash",
                    "me",
                    "testPostComment",
                    true, // is publsihed
                    "testPostRights",
                    "testXmlBase",
                    "testContributorName",
                    "testContributorEmail",
                    "testAuthorName",
                    "testAuthorEmail",
                    "testPostCategory",
                    null, // publish timestamp
                    null, // expiration timestamp
                    "testEnclosureUrl",
                    null // last updated timestamp
            )
    );

    @Test
    void test_getStagingPosts() throws Exception {
        when(this.stagingPostService.getStagingPosts("me", List.of(1L))).thenReturn(TEST_STAGING_POSTS);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/staging")
                        .queryParam("feedIds", "1")
                        .header("Authorization", "Bearer testToken")
                        .accept(APPLICATION_JSON))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("{\"stagingPosts\":[{\"post\":{\"id\":null,\"importerId\":\"testImporterId\",\"feedIdent\":\"testFeedIdent\",\"importerDesc\":\"testImporterDesc\",\"sourceObj\":null,\"sourceName\":\"testSourceName\",\"sourceUrl\":\"testSourceUrl\",\"postTitle\":\"testPostTitle\",\"postDesc\":\"testPostDesc\",\"postUrl\":\"testPostUrl\",\"postImgUrl\":\"testPostImgUrl\",\"postImgTransportIdent\":\"1AA5D827D4FFA9BD7DB928C0AE3DBF84\",\"postHash\":\"testPostHash\",\"username\":\"me\",\"postComment\":\"testPostComment\",\"postStatus\":null,\"postRights\":\"testPostRights\",\"xmlBase\":\"testXmlBase\",\"contributorName\":\"testContributorName\",\"contributorEmail\":\"testContributorEmail\",\"authorName\":\"testAuthorName\",\"authorEmail\":\"testAuthorEmail\",\"postCategory\":\"testPostCategory\",\"enclosureUrl\":\"testEnclosureUrl\",\"importTimestamp\":null,\"publishTimestamp\":null,\"expirationTimestamp\":null,\"lastUpdatedTimestamp\":null,\"published\":true},\"postImgSrc\":null}]}", responseContent);
                })
                .andExpect(status().isOk());
    }
}
