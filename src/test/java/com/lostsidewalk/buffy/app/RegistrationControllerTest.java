package com.lostsidewalk.buffy.app;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = QueueDefinitionController.class)
public class RegistrationControllerTest extends BaseWebControllerTest {

    @Test
    public void testRegistration() {
        
    }
}
