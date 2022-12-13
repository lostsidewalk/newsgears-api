package com.lostsidewalk.buffy.app;

import com.lostsidewalk.buffy.app.model.response.StripeResponse;
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
@WebMvcTest(controllers = OrderController.class)
public class OrderControllerTest extends BaseWebControllerTest {

    @BeforeEach
    void test_setup() throws Exception {
        when(this.tokenService.instanceFor(APP_AUTH, "testToken")).thenReturn(TEST_JWT_UTIL);
        when(this.authService.requireAuthClaim("me")).thenReturn("testAuthClaim");
        when(this.userService.loadUserByUsername("me")).thenReturn(TEST_USER_DETAILS);
    }

    private static final StripeResponse TEST_STRIPE_RESPONSE = StripeResponse.from("testSessionId", "testSessionUrl");
    @Test
    void test_initCheckout() throws Exception {
        when(this.stripeOrderService.createCheckoutSession("me")).thenReturn(TEST_STRIPE_RESPONSE);
        mockMvc.perform(MockMvcRequestBuilders
                        .post("/order")
                        .header("Authorization", "Bearer testToken")
                        .accept(APPLICATION_JSON))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("{\"sessionId\":\"testSessionId\",\"sessionUrl\":\"testSessionUrl\"}", responseContent);
                })
                .andExpect(status().isOk());
    }
}
