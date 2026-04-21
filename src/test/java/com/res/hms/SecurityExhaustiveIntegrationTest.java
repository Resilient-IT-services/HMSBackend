package com.res.hms;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.res.hms.domain.RoleCode;
import com.res.hms.domain.UserAccount;
import com.res.hms.domain.UserRoleAssignment;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@org.springframework.transaction.annotation.Transactional
class SecurityExhaustiveIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserAccountRepository userAccountRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private HospitalRepository hospitalRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private TestDataHelper testDataHelper;

  private Long testHospitalId;
  private Long secondHospitalId;

  @BeforeEach
  void setup() {
    testHospitalId = testDataHelper.createHospitalIfMissing("TEST", "Test Hospital");
    secondHospitalId = testDataHelper.createHospitalIfMissing("HOSP2", "Second Hospital");

    testDataHelper.createUserIfMissing(
        "hosp_admin",
        RoleCode.ADMIN,
        testHospitalId,
        "password123",
        passwordEncoder,
        userAccountRepository,
        roleRepository);
    testDataHelper.createUserIfMissing(
        "hosp_doctor",
        RoleCode.DOCTOR,
        testHospitalId,
        "password123",
        passwordEncoder,
        userAccountRepository,
        roleRepository);
    testDataHelper.createUserIfMissing(
        "hosp_pharmacy",
        RoleCode.PHARMACIST,
        testHospitalId,
        "password123",
        passwordEncoder,
        userAccountRepository,
        roleRepository);
  }

  private MockHttpSession loginAndSelect(String username, Long hospitalId) throws Exception {
    MockHttpSession session =
        (MockHttpSession)
            mockMvc
                .perform(
                    post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            "{\"username\": \"" + username + "\", \"password\": \"password123\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getRequest()
                .getSession();

    // Optional check for memberships
    mockMvc.perform(get("/api/v1/auth/me").session(session)).andExpect(status().isOk());

    if (hospitalId != null) {
      mockMvc
          .perform(
              post("/api/v1/auth/select-hospital")
                  .session(session)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"hospitalId\": " + hospitalId + "}"))
          .andExpect(status().isOk());
    }

    return session;
  }

  private MockHttpSession login(String username) throws Exception {
    return loginAndSelect(username, testHospitalId);
  }

  @Test
  void hospitalAdminCreateUserWithoutRole_ShouldFail() throws Exception {
    MockHttpSession session = login("hosp_admin");
    mockMvc
        .perform(
            post("/api/v1/users")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "username": "newuser1",
                                  "password": "password",
                                  "roles": [],
                                  "fullName": "New User",
                                  "gender": "MALE",
                                  "dob": "1990-01-01"
                                }
                                """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void doctorTryingToCreateUser_ShouldFail() throws Exception {
    MockHttpSession session = login("hosp_doctor");
    mockMvc
        .perform(
            post("/api/v1/users")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "username": "newuser2",
                                  "password": "password",
                                  "roles": ["NURSE"],
                                  "fullName": "New User",
                                  "gender": "MALE",
                                  "dob": "1990-01-01"
                                }
                                """))
        .andExpect(status().isForbidden());
  }

  @Test
  void hospitalAdminCreateAnotherHospitalAdmin_ShouldSucceed() throws Exception {
    MockHttpSession session = login("hosp_admin");
    mockMvc
        .perform(
            post("/api/v1/users")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "username": "another_admin",
                                  "password": "password",
                                  "roles": ["ADMIN"],
                                  "fullName": "Another Admin",
                                  "gender": "MALE",
                                  "dob": "1990-01-01"
                                }
                                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.username").value("another_admin"));
  }

  @Test
  void hospitalAdminCreateExistingUsername_ShouldFail() throws Exception {
    MockHttpSession session = login("hosp_admin");
    mockMvc
        .perform(
            post("/api/v1/users")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "username": "hosp_doctor",
                                  "password": "password",
                                  "roles": ["NURSE"],
                                  "fullName": "Dup User",
                                  "gender": "MALE",
                                  "dob": "1990-01-01"
                                }
                                """))
        .andExpect(status().isConflict());
  }

  @Test
  void hospitalAdminCreateHospital_ShouldFail() throws Exception {
    MockHttpSession session = login("hosp_admin");
    mockMvc
        .perform(
            post("/api/v1/hospitals")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "name": "Illegal Hospital",
                                  "code": "ILL",
                                  "address": "Address"
                                }
                                """))
        .andExpect(status().isForbidden());
  }

  @Test
  void pharmacyGuyCreateAppointment_ShouldFail() throws Exception {
    MockHttpSession session = login("hosp_pharmacy");
    mockMvc
        .perform(
            post("/api/v1/appointments")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "patientId": 1,
                                  "doctorId": 1,
                                  "appointmentTime": "2026-05-01T10:00:00"
                                }
                                """))
        .andExpect(status().isForbidden());
  }

  @Test
  void hospitalAdminTriesToSeeOtherHospitals_ShouldFail() throws Exception {
    MockHttpSession session = login("hosp_admin");
    mockMvc.perform(get("/api/v1/hospitals").session(session)).andExpect(status().isForbidden());
  }

  @Test
  void hospitalAdminTriesToAllocateRoleInDifferentHospital_ShouldFail() throws Exception {
    // Create a user in Hospital 2
    testDataHelper.createUserIfMissing(
        "hosp2_user",
        RoleCode.NURSE,
        secondHospitalId,
        "password123",
        passwordEncoder,
        userAccountRepository,
        roleRepository);
    Long hosp2UserId = userAccountRepository.findByUsername("hosp2_user").orElseThrow().getId();

    // Admin from Hospital 1 tries to allocate roles to user in Hospital 2
    MockHttpSession session = login("hosp_admin");
    mockMvc
        .perform(
            post("/api/v1/users/allocate")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\": " + hosp2UserId + ", \"roles\": [\"DOCTOR\"]}"))
        .andExpect(status().isNotFound()); // Strict isolation: user not found in context
  }

  @Test
  void doctorSwitchingHospitals_ShouldUpdateSession() throws Exception {
    // Create a doctor with access to two hospitals
    testDataHelper.createUserIfMissing(
        "multi_dr",
        RoleCode.DOCTOR,
        testHospitalId,
        "password123",
        passwordEncoder,
        userAccountRepository,
        roleRepository);
    UserAccount dr = userAccountRepository.findByUsername("multi_dr").orElseThrow();
    UserRoleAssignment h2Assignment = new UserRoleAssignment();
    h2Assignment.setUser(dr);
    h2Assignment.setHospitalId(secondHospitalId);
    h2Assignment.setRole(roleRepository.findByCode(RoleCode.DOCTOR).orElseThrow());
    dr.getAssignments().add(h2Assignment);
    userAccountRepository.save(dr);

    // 1. Login and select Hospital 1
    MockHttpSession session = loginAndSelect("multi_dr", testHospitalId);
    mockMvc
        .perform(get("/api/v1/auth/me").session(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.hospitalId").value(testHospitalId));

    // 2. Switch to Hospital 2
    mockMvc
        .perform(
            post("/api/v1/auth/select-hospital")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"hospitalId\": " + secondHospitalId + "}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.hospitalId").value(secondHospitalId));

    // 3. Verify 'me' now returns Hospital 2
    mockMvc
        .perform(get("/api/v1/auth/me").session(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.hospitalId").value(secondHospitalId));
  }

  @Test
  void hospitalAdminCreateUserWithoutName_ShouldFail() throws Exception {
    MockHttpSession session = login("hosp_admin");
    mockMvc
        .perform(
            post("/api/v1/users")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "username": "noname",
                                  "password": "password",
                                  "roles": ["NURSE"],
                                  "gender": "MALE",
                                  "dob": "1990-01-01"
                                }
                                """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void hospitalAdminCreateUserWithoutGender_ShouldFail() throws Exception {
    MockHttpSession session = login("hosp_admin");
    mockMvc
        .perform(
            post("/api/v1/users")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "username": "nogender",
                                  "password": "password",
                                  "roles": ["NURSE"],
                                  "fullName": "No Gender",
                                  "dob": "1990-01-01"
                                }
                                """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void hospitalAdminCreateUserWithoutDob_ShouldFail() throws Exception {
    MockHttpSession session = login("hosp_admin");
    mockMvc
        .perform(
            post("/api/v1/users")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "username": "nodob",
                                  "password": "password",
                                  "roles": ["NURSE"],
                                  "fullName": "No DOB",
                                  "gender": "MALE"
                                }
                                """))
        .andExpect(status().isBadRequest());
  }
}
