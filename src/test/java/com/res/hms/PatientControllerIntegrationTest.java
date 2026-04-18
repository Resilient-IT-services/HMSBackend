package com.res.hms;

import com.res.hms.domain.RoleCode;
import com.res.hms.domain.UserAccount;
import com.res.hms.repository.HospitalRepository;
import com.res.hms.repository.RoleRepository;
import com.res.hms.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PatientControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserAccountRepository userAccountRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private HospitalRepository hospitalRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void ensureHospitalAdminExists() {
        if (userAccountRepository.existsByUsername("hospital_admin")) {
            return;
        }

        UserAccount hospitalAdmin = new UserAccount();
        hospitalAdmin.setHospitalId(hospitalRepository.findByCode("TEST").orElseThrow().getId());
        hospitalAdmin.setUsername("hospital_admin");
        hospitalAdmin.setPasswordHash(passwordEncoder.encode("admin123"));
        hospitalAdmin.getRoles().add(roleRepository.findByCode(RoleCode.ADMIN).orElseThrow());
        hospitalAdmin.setActive(true);
        userAccountRepository.save(hospitalAdmin);
    }

    @Test
    void patientEndpointsRequireAuthenticationAndUseSessionCookie() throws Exception {
        mockMvc.perform(get("/api/v1/patients"))
                .andExpect(status().isUnauthorized());

        MockHttpSession session = (MockHttpSession) mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "hospital_admin",
                                  "password": "admin123"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getRequest()
                .getSession(false);

        mockMvc.perform(post("/api/v1/patients")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "full_name": "Asha Nair",
                                  "gender": "FEMALE",
                                  "dob": "1992-08-14",
                                  "phone": "9999999999",
                                  "address": "Kochi"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.full_name").value("Asha Nair"));

        mockMvc.perform(get("/api/v1/patients").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].full_name").value("Asha Nair"));
    }
}
