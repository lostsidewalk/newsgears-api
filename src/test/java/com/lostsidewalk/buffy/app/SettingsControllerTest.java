package com.lostsidewalk.buffy.app;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.lostsidewalk.buffy.FrameworkConfig;
import com.lostsidewalk.buffy.auth.AuthProvider;
import com.lostsidewalk.buffy.auth.User;
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
@WebMvcTest(controllers = SettingsController.class)
public class SettingsControllerTest extends BaseWebControllerTest {

    @BeforeEach
    void test_setup() throws Exception {
        when(this.tokenService.instanceFor(APP_AUTH, "testToken")).thenReturn(TEST_JWT_UTIL);
        when(this.authService.requireAuthClaim("me")).thenReturn("testAuthClaim");
        when(this.userService.loadUserByUsername("me")).thenReturn(TEST_USER_DETAILS);
    }

    private static final FrameworkConfig TEST_FRAMEWORK_CONFIG = new FrameworkConfig();

    private static final Gson GSON = new Gson();

    private static final User TEST_USER = new User("me", "testEmailAddress", AuthProvider.LOCAL, "testAuthProviderId", "testAuthProviderProfileImgUrl", "testAuthProviderUsername");
    static {
        TEST_USER.setId(1L);
    }

    @Test
    void test_getSettings() throws Exception {
        when(this.frameworkConfigDao.findByUserId(1L)).thenReturn(TEST_FRAMEWORK_CONFIG);
        when(this.userDao.findByName("me")).thenReturn(TEST_USER);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/settings")
                        .header("Authorization", "Bearer testToken")
                        .accept(APPLICATION_JSON))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(
                            GSON.fromJson("{\"username\":\"me\",\"emailAddress\":\"testEmailAddress\",\"authProvider\":\"LOCAL\",\"authProviderProfileImgUrl\":\"testAuthProviderProfileImgUrl\",\"authProviderUsername\":\"testAuthProviderUsername\",\"frameworkConfig\":{\"userId\":null,\"notifications\":{},\"display\":{}}}", JsonObject.class),
                            GSON.fromJson(responseContent, JsonObject.class)
                    );
                })
                .andExpect(status().isOk());
    }
}
