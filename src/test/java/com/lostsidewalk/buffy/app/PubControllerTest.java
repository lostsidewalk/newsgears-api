package com.lostsidewalk.buffy.app;

import com.lostsidewalk.buffy.FeedPreview;
import com.lostsidewalk.buffy.Publisher.PubFormat;
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
@WebMvcTest(controllers = PubController.class)
public class PubControllerTest extends BaseWebControllerTest {

    @BeforeEach
    void test_setup() throws Exception {
        when(this.tokenService.instanceFor(APP_AUTH, "testToken")).thenReturn(TEST_JWT_UTIL);
        when(this.authService.requireAuthClaim("me")).thenReturn("testAuthClaim");
        when(this.userService.loadUserByUsername("me")).thenReturn(TEST_USER_DETAILS);
    }

    @Test
    void test_previewFeed() throws Exception {
        when(this.postPublisher.doPreview("me", "testFeedIdent", PubFormat.JSON)).thenReturn(List.of(FeedPreview.from("testFeedIdent", "{}")));
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/feeds/preview")
                        .queryParam("feedIdent", "testFeedIdent")
                        .queryParam("format", PubFormat.JSON.name())
                        .header("Authorization", "Bearer testToken")
                        .accept(APPLICATION_JSON))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("[{\"feedIdent\":\"testFeedIdent\",\"previewArtifact\":\"{}\"}]", responseContent);
                })
                .andExpect(status().isOk());
    }
}
