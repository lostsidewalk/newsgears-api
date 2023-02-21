package com.lostsidewalk.buffy.app;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static com.lostsidewalk.buffy.app.model.TokenType.APP_AUTH;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = ExportController.class)
public class ExportControllerTest extends BaseWebControllerTest {

    @BeforeEach
    void test_setup() throws Exception {
        when(this.tokenService.instanceFor(APP_AUTH, "testToken")).thenReturn(TEST_JWT_UTIL);
        when(this.authService.requireAuthClaim("me")).thenReturn("testAuthClaim");
        when(this.userService.loadUserByUsername("me")).thenReturn(TEST_USER_DETAILS);
    }

    @Test
    void test_exportFeed() throws Exception {
        ;
    }

//    @Test
//    void test_previewFeed() throws Exception {
//        when(this.postPublisher.doPreview("me", "testFeedIdent", PubFormat.JSON)).thenReturn(List.of(FeedPreview.from("testFeedIdent", "{}")));
//        mockMvc.perform(MockMvcRequestBuilders
//                        .get("/feeds/preview")
//                        .queryParam("feedIdent", "testFeedIdent")
//                        .queryParam("format", PubFormat.JSON.name())
//                        .header("Authorization", "Bearer testToken")
//                        .accept(APPLICATION_JSON))
//                .andExpect(result -> {
//                    String responseContent = result.getResponse().getContentAsString();
//                    assertEquals("[{\"feedIdent\":\"testFeedIdent\",\"previewArtifact\":\"{}\"}]", responseContent);
//                })
//                .andExpect(status().isOk());
//    }
}
