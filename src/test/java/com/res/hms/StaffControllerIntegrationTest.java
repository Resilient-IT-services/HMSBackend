package com.res.hms;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.res.hms.domain.CertificationLevel;
import com.res.hms.domain.DoctorHospitalDetails;
import com.res.hms.domain.DoctorProfile;
import com.res.hms.domain.NurseHospitalDetails;
import com.res.hms.domain.NurseProfile;
import com.res.hms.domain.RoleCode;
import com.res.hms.domain.ShiftType;
import com.res.hms.domain.UserAccount;
import com.res.hms.domain.UserRoleAssignment;
import com.res.hms.doctor.repository.DoctorHospitalDetailsRepository;
import com.res.hms.nurse.repository.NurseHospitalDetailsRepository;
import com.res.hms.user.repository.RoleRepository;
import com.res.hms.user.repository.UserAccountRepository;
import java.math.BigDecimal;
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
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class StaffControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserAccountRepository userAccountRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private DoctorHospitalDetailsRepository doctorHospitalDetailsRepository;
  @Autowired private NurseHospitalDetailsRepository nurseHospitalDetailsRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private TestDataHelper testDataHelper;

  private UUID testHospitalId;
  private MockHttpSession session;

  @BeforeEach
  void setUp() throws Exception {
    testHospitalId = testDataHelper.createHospitalIfMissing("STAFF_TEST", "Staff Test Hospital");

    // Create Admin for session
    UserAccount admin = new UserAccount();
    admin.setUsername("staff_admin");
    admin.setPasswordHash(passwordEncoder.encode("admin123"));
    admin.setActive(true);
    UserRoleAssignment adminAssign = new UserRoleAssignment();
    adminAssign.setUser(admin);
    adminAssign.setHospitalId(testHospitalId);
    adminAssign.setRole(roleRepository.findByCode(RoleCode.ADMIN).orElseThrow());
    admin.getAssignments().add(adminAssign);
    userAccountRepository.save(admin);

    session =
        (MockHttpSession)
            mockMvc
                .perform(
                    post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"staff_admin\", \"password\": \"admin123\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getRequest()
                .getSession();

    // Create a Doctor
    UserAccount drUser = new UserAccount();
    drUser.setUsername("dr_jones");
    drUser.setFullName("Dr. Indiana Jones");
    drUser.setPasswordHash(passwordEncoder.encode("password"));
    drUser.setActive(true);

    DoctorProfile drProfile = new DoctorProfile();
    drProfile.setUser(drUser);
    drProfile.setSpecialization("Archaeology");
    drProfile.setLicenseNumber("LICENSE-123");
    drUser.setDoctorProfile(drProfile);

    UserRoleAssignment drAssign = new UserRoleAssignment();
    drAssign.setUser(drUser);
    drAssign.setHospitalId(testHospitalId);
    drAssign.setDepartmentName("OPD");
    drAssign.setRole(roleRepository.findByCode(RoleCode.DOCTOR).orElseThrow());
    drUser.getAssignments().add(drAssign);
    userAccountRepository.save(drUser);

    DoctorHospitalDetails drDetails = new DoctorHospitalDetails();
    drDetails.setAssignment(drAssign);
    drDetails.setConsultationFee(new BigDecimal("500.00"));
    drAssign.setDoctorDetails(drDetails);
    doctorHospitalDetailsRepository.save(drDetails);

    // Create a Nurse
    UserAccount nurseUser = new UserAccount();
    nurseUser.setUsername("nurse_ratched");
    nurseUser.setFullName("Nurse Ratched");
    nurseUser.setPasswordHash(passwordEncoder.encode("password"));
    nurseUser.setActive(true);

    NurseProfile nurseProfile = new NurseProfile();
    nurseProfile.setUser(nurseUser);
    nurseProfile.setCertificationLevel(CertificationLevel.RN);
    nurseProfile.setNursingLicenseNo("NURSE-999");
    nurseUser.setNurseProfile(nurseProfile);

    UserRoleAssignment nurseAssign = new UserRoleAssignment();
    nurseAssign.setUser(nurseUser);
    nurseAssign.setHospitalId(testHospitalId);
    nurseAssign.setDepartmentName("ICU");
    nurseAssign.setRole(roleRepository.findByCode(RoleCode.NURSE).orElseThrow());
    nurseUser.getAssignments().add(nurseAssign);
    userAccountRepository.save(nurseUser);

    NurseHospitalDetails nurseDetails = new NurseHospitalDetails();
    nurseDetails.setAssignment(nurseAssign);
    nurseDetails.setShiftType(ShiftType.NIGHT);
    nurseAssign.setNurseDetails(nurseDetails);
    nurseHospitalDetailsRepository.save(nurseDetails);
  }

  @Test
  void testGetDoctorsWithFilters() throws Exception {
    // Basic list
    mockMvc
        .perform(
            get("/api/v1/staff/doctors")
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content[0].fullName").value("Dr. Indiana Jones"))
        .andExpect(jsonPath("$.data.content[0].departmentName").value("OPD"))
        .andExpect(jsonPath("$.data.content[0].consultationFee").value(500.0));

    // Filter by department
    mockMvc
        .perform(
            get("/api/v1/staff/doctors")
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString())
                .param("department", "OPD"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1));

    // Filter by name
    mockMvc
        .perform(
            get("/api/v1/staff/doctors")
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString())
                .param("name", "Indiana"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1));

    // Filter by fee range - match
    mockMvc
        .perform(
            get("/api/v1/staff/doctors")
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString())
                .param("minFee", "400")
                .param("maxFee", "600"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1));

    // Filter by fee range - no match
    mockMvc
        .perform(
            get("/api/v1/staff/doctors")
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString())
                .param("minFee", "600"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(0));
  }

  @Test
  void testGetNursesWithFilters() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/staff/nurses")
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content[0].fullName").value("Nurse Ratched"))
        .andExpect(jsonPath("$.data.content[0].departmentName").value("ICU"))
        .andExpect(jsonPath("$.data.content[0].shiftType").value("NIGHT"));

    mockMvc
        .perform(
            get("/api/v1/staff/nurses")
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString())
                .param("department", "ICU"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1));
  }
}
