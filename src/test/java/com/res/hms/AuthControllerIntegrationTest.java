package com.res.hms;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void loginCreatesHttpSessionAndAllowsProfileLookup() throws Exception {
    // 1. Login -> returns success with null data
    MockHttpSession session =
        (MockHttpSession)
            mockMvc
                .perform(
                    post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                                {
                                  "username": "admin",
                                  "password": "admin123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getRequest()
                .getSession(false);

    org.junit.jupiter.api.Assertions.assertNotNull(session);

    // 2. Must call /me to get profile
    mockMvc
        .perform(get("/api/v1/auth/me").session(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.username").value("admin"))
        .andExpect(jsonPath("$.data.userId").exists())
        .andExpect(jsonPath("$.data.roles").value(hasItem("SUPER_ADMIN")))
        .andExpect(jsonPath("$.data.hospitals").isArray())
        .andExpect(jsonPath("$.data.hospitalId").doesNotExist());
  }
}
