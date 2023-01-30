package com.lostsidewalk.buffy.app;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.lostsidewalk.buffy.app.auth.CookieBuilder;
import com.lostsidewalk.buffy.app.model.AppToken;
import com.lostsidewalk.buffy.app.model.request.LoginRequest;
import com.lostsidewalk.buffy.app.token.TokenService.JwtUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static com.lostsidewalk.buffy.AuthProvider.LOCAL;
import static com.lostsidewalk.buffy.app.model.TokenType.APP_AUTH;
import static com.lostsidewalk.buffy.app.model.TokenType.APP_AUTH_REFRESH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = FeedDefinitionController.class)
public class AuthenticationControllerTest extends BaseWebControllerTest {

    @Test
    public void testCurrentUser() throws Exception {
        when(authService.getTokenCookieFromRequest(eq(APP_AUTH_REFRESH), any())).thenReturn("testCookieValue");
        JwtUtil mockJwtUtil = mock(JwtUtil.class);
        when(tokenService.instanceFor(APP_AUTH_REFRESH, "testCookieValue")).thenReturn(mockJwtUtil);
        when(mockJwtUtil.extractUsername()).thenReturn("me");
        when(authService.requireAuthClaim("me")).thenReturn("testAuthClaim");
        when(mockJwtUtil.extractValidationClaim()).thenReturn("b4223bd3427db93956acaadf9e425dd259bfb11dac44234604c819dbbf75e180");
        when(userService.loadUserByUsername("me")).thenReturn(TEST_USER_DETAILS);
        when(authService.generateAuthToken("me")).thenReturn(new AppToken("testToken", 60));
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/currentuser")
                        .servletPath("/currentuser")
                        .cookie(new CookieBuilder("newsgears-app_auth-token","testTokenValue").build())
                        .accept(APPLICATION_JSON))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"authToken\":\"testToken\",\"username\":\"me\",\"hasSubscription\":true}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(mockJwtUtil).requireNonExpired();
        verify(authService).addTokenCookieToResponse(eq(APP_AUTH_REFRESH), eq("me"), eq("testAuthClaim"), any(HttpServletResponse.class));
    }

    private static final Gson GSON = new Gson();

    @Test
    public void testAuthenticate() throws Exception {
        when(authService.requireAuthClaim("me")).thenReturn("testAuthClaim");
        when(authService.generateAuthToken("me")).thenReturn(new AppToken("testToken", 60));
        when(userService.loadUserByUsername("me")).thenReturn(TEST_USER_DETAILS);
        LoginRequest testLoginRequest = new LoginRequest("me", "testPassword");
        mockMvc.perform(MockMvcRequestBuilders
                        .post("/authenticate")
                        .contentType(APPLICATION_JSON)
                        .content(GSON.toJson(testLoginRequest))
                        .accept(APPLICATION_JSON))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"authToken\":\"testToken\",\"username\":\"me\",\"hasSubscription\":true}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(authService).requireAuthProvider("me", LOCAL);
        verify(authService).addTokenCookieToResponse(eq(APP_AUTH_REFRESH), eq("me"), eq("testAuthClaim"), any());
    }

    @Test
    public void testDeauthenticate() throws Exception {
        JwtUtil mockJwtUtil = mock(JwtUtil.class);
        when(tokenService.instanceFor(APP_AUTH, "testToken")).thenReturn(mockJwtUtil);
        when(mockJwtUtil.extractUsername()).thenReturn("me");
        when(authService.requireAuthClaim("me")).thenReturn("testAuthClaim");
        when(mockJwtUtil.extractValidationClaim()).thenReturn("b4223bd3427db93956acaadf9e425dd259bfb11dac44234604c819dbbf75e180");
        when(userService.loadUserByUsername("me")).thenReturn(TEST_USER_DETAILS);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/deauthenticate")
                        .header("Authorization", "Bearer testToken")
                        .accept(APPLICATION_JSON))
                .andExpect(status().isOk());
        verify(mockJwtUtil).requireNonExpired();
        verify(authService).requireAuthClaim("me");
    }
}
