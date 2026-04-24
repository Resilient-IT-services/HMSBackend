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
import java.util.UUID;
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

  private UUID testHospitalId;
  private UUID secondHospitalId;

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

  private MockHttpSession login(String username) throws Exception {
    return (MockHttpSession)
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\": \"" + username + "\", \"password\": \"password123\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getRequest()
            .getSession();
  }

  @Test
  void hospitalAdminCreateUserWithoutRole_ShouldFail() throws Exception {
    MockHttpSession session = login("hosp_admin");
    mockMvc
        .perform(
            post("/api/v1/users")
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString())
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
                .header("X-Hospital-Id", testHospitalId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "username": "newuser2",
                                  "password": "password",
                                  "roles": ["NURSE"],
                                  "fullName": "New User",
                                  "gender": "MALE",
                                  "dob": "1990-01-01",
                                  "department": "XYZ"
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
                .header("X-Hospital-Id", testHospitalId.toString())
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
                .header("X-Hospital-Id", testHospitalId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "username": "hosp_doctor",
                                  "password": "password",
                                  "roles": ["NURSE"],
                                  "fullName": "Dup User",
                                  "gender": "MALE",
                                  "dob": "1990-01-01",
                                  "department": "department"
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
                .header("X-Hospital-Id", testHospitalId.toString())
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
                .header("X-Hospital-Id", testHospitalId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    String.format(
                        """
                                {
                                  "patientId": "%s",
                                  "doctorId": "%s",
                                  "appointmentTime": "2026-05-01T10:00:00"
                                }
                                """,
                        UUID.randomUUID(), UUID.randomUUID())))
        .andExpect(status().isForbidden());
  }

  @Test
  void hospitalAdminTriesToSeeOtherHospitals_ShouldFail() throws Exception {
    MockHttpSession session = login("hosp_admin");
    mockMvc
        .perform(
            get("/api/v1/hospitals")
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString()))
        .andExpect(status().isForbidden());
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
    UUID hosp2UserId = userAccountRepository.findByUsername("hosp2_user").orElseThrow().getId();

    // Admin from Hospital 1 tries to allocate roles to user in Hospital 2
    MockHttpSession session = login("hosp_admin");
    mockMvc
        .perform(
            post("/api/v1/users/allocate")
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\": \"" + hosp2UserId + "\", \"roles\": [\"DOCTOR\"]}"))
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

    // 1. Login
    MockHttpSession session = login("multi_dr");

    // 2. Access with Hospital 1 header
    mockMvc
        .perform(
            get("/api/v1/auth/me")
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.hospitalId").value(testHospitalId.toString()));

    // 3. Access with Hospital 2 header
    mockMvc
        .perform(
            get("/api/v1/auth/me")
                .session(session)
                .header("X-Hospital-Id", secondHospitalId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.hospitalId").value(secondHospitalId.toString()));
  }

  @Test
  void hospitalAdminCreateUserWithoutName_ShouldFail() throws Exception {
    MockHttpSession session = login("hosp_admin");
    mockMvc
        .perform(
            post("/api/v1/users")
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString())
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
                .header("X-Hospital-Id", testHospitalId.toString())
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
                .header("X-Hospital-Id", testHospitalId.toString())
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
