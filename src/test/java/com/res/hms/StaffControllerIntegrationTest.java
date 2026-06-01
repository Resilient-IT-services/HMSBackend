package com.res.hms;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.res.hms.core.domain.CertificationLevel;
import com.res.hms.core.domain.ShiftType;
import com.res.hms.hospital.domain.Department;
import com.res.hms.hospital.domain.Hospital;
import com.res.hms.iam.domain.DoctorHospitalDetails;
import com.res.hms.iam.domain.DoctorProfile;
import com.res.hms.iam.domain.NurseHospitalDetails;
import com.res.hms.iam.domain.NurseProfile;
import com.res.hms.iam.domain.RoleCode;
import com.res.hms.iam.domain.Specialization;
import com.res.hms.iam.domain.UserAccount;
import com.res.hms.iam.domain.UserContract;
import com.res.hms.iam.infrastructure.repository.DoctorHospitalDetailsRepository;
import com.res.hms.iam.infrastructure.repository.NurseHospitalDetailsRepository;
import com.res.hms.iam.infrastructure.repository.RoleRepository;
import com.res.hms.iam.infrastructure.repository.UserAccountRepository;
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

  @Autowired
  private com.res.hms.hospital.infrastructure.repository.HospitalRepository hospitalRepository;

  @Autowired
  private com.res.hms.hospital.infrastructure.repository.DepartmentRepository departmentRepository;

  private UUID testHospitalId;
  private MockHttpSession session;
  private Department opdDepartment;
  private Department icuDepartment;

  @BeforeEach
  void setUp() throws Exception {
    testHospitalId = testDataHelper.createHospitalIfMissing("STAFF_TEST", "Staff Test Hospital");
    setUpDepartments();
    createAdminSession();
    createDoctor();
    createNurse();
    createDualRoleDoctor();
  }

  private void setUpDepartments() {
    Hospital hospital = hospitalRepository.findById(testHospitalId).get();
    opdDepartment =
        departmentRepository
            .findByName("OPD")
            .orElseGet(
                () -> {
                  Department department = new Department();
                  department.setName("OPD");
                  return departmentRepository.save(department);
                });
    hospital.getDepartments().add(opdDepartment);
    icuDepartment =
        departmentRepository
            .findByName("ICU")
            .orElseGet(
                () -> {
                  Department department = new Department();
                  department.setName("ICU");
                  return departmentRepository.save(department);
                });
    hospital.getDepartments().add(icuDepartment);
    hospitalRepository.save(hospital);
  }

  private void createAdminSession() throws Exception {
    UserAccount admin = new UserAccount();
    admin.setUsername("staff_admin");
    admin.setPasswordHash(passwordEncoder.encode("admin123"));
    admin.setActive(true);
    UserContract adminAssign = new UserContract();
    adminAssign.setUser(admin);
    adminAssign.setHospitalId(testHospitalId);
    adminAssign.setRole(roleRepository.findByCode(RoleCode.ADMIN).orElseThrow());
    admin.getContracts().add(adminAssign);
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
  }

  private void createDoctor() {
    UserAccount drUser = new UserAccount();
    drUser.setUsername("dr_jones");
    drUser.setFullName("Dr. Indiana Jones");
    drUser.setPasswordHash(passwordEncoder.encode("password"));
    drUser.setActive(true);

    DoctorProfile drProfile = new DoctorProfile();
    drProfile.setUser(drUser);
    Specialization spec = new Specialization();
    spec.setName("Archaeology");
    drProfile.getSpecializations().add(spec);
    drProfile.setLicenseNumber("LICENSE-123");
    drUser.setDoctorProfile(drProfile);

    UserContract drAssign = new UserContract();
    drAssign.setUser(drUser);
    drAssign.setHospitalId(testHospitalId);
    drAssign.getDepartments().add(opdDepartment);
    drAssign.setRole(roleRepository.findByCode(RoleCode.DOCTOR).orElseThrow());
    drUser.getContracts().add(drAssign);
    userAccountRepository.save(drUser);

    DoctorHospitalDetails drDetails = new DoctorHospitalDetails();
    drDetails.setAssignment(drAssign);
    drDetails.setConsultationFee(new BigDecimal("500.00"));
    drAssign.setDoctorDetails(drDetails);
    doctorHospitalDetailsRepository.save(drDetails);
  }

  private void createNurse() {
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

    UserContract nurseAssign = new UserContract();
    nurseAssign.setUser(nurseUser);
    nurseAssign.setHospitalId(testHospitalId);
    nurseAssign.getDepartments().add(icuDepartment);
    nurseAssign.setRole(roleRepository.findByCode(RoleCode.NURSE).orElseThrow());
    nurseUser.getContracts().add(nurseAssign);
    userAccountRepository.save(nurseUser);

    NurseHospitalDetails nurseDetails = new NurseHospitalDetails();
    nurseDetails.setAssignment(nurseAssign);
    nurseDetails.setShiftType(ShiftType.NIGHT);
    nurseAssign.setNurseDetails(nurseDetails);
    nurseHospitalDetailsRepository.save(nurseDetails);
  }

  private void createDualRoleDoctor() {
    UserAccount dualUser = new UserAccount();
    dualUser.setUsername("dual_user");
    dualUser.setFullName("Dr. Multi Role");
    dualUser.setPasswordHash(passwordEncoder.encode("password"));
    dualUser.setActive(true);

    DoctorProfile drProfileDual = new DoctorProfile();
    drProfileDual.setUser(dualUser);
    drProfileDual.setLicenseNumber("LICENSE-DUAL");
    dualUser.setDoctorProfile(drProfileDual);

    // Contract 1: Doctor
    UserContract drContractDual = new UserContract();
    drContractDual.setUser(dualUser);
    drContractDual.setHospitalId(testHospitalId);
    drContractDual.setRole(roleRepository.findByCode(RoleCode.DOCTOR).orElseThrow());
    dualUser.getContracts().add(drContractDual);

    // Contract 2: Receptionist
    UserContract recepContractDual = new UserContract();
    recepContractDual.setUser(dualUser);
    recepContractDual.setHospitalId(testHospitalId);
    recepContractDual.setRole(roleRepository.findByCode(RoleCode.RECEPTIONIST).orElseThrow());
    dualUser.getContracts().add(recepContractDual);

    dualUser = userAccountRepository.save(dualUser);

    UserContract savedDrContractDual =
        dualUser.getContracts().stream()
            .filter(c -> c.getRole().getCode() == RoleCode.DOCTOR)
            .findFirst()
            .orElseThrow();

    DoctorHospitalDetails drDetailsDual = new DoctorHospitalDetails();
    drDetailsDual.setAssignment(savedDrContractDual);
    drDetailsDual.setConsultationFee(new BigDecimal("1000.00"));
    savedDrContractDual.setDoctorDetails(drDetailsDual);
    doctorHospitalDetailsRepository.save(drDetailsDual);
  }

  @Test
  void testGetDoctor_WhenUserHasMultipleRoles_ShouldReturnUniqueResult() throws Exception {
    UserAccount dualUser = userAccountRepository.findByUsername("dual_user").orElseThrow();
    UUID doctorProfileId = dualUser.getDoctorProfile().getId();

    mockMvc
        .perform(
            get("/api/v1/staff/doctors/" + doctorProfileId)
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.fullName").value("Dr. Multi Role"))
        .andExpect(jsonPath("$.data.consultationFee").value(1000.0));
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
        .andExpect(jsonPath("$.data.totalElements").value(2));

    // Filter by department
    mockMvc
        .perform(
            get("/api/v1/staff/doctors")
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString())
                .param("department", "OPD"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1))
        .andExpect(jsonPath("$.data.content[0].fullName").value("Dr. Indiana Jones"))
        .andExpect(jsonPath("$.data.content[0].consultationFee").value(500.0));

    // Filter by name
    mockMvc
        .perform(
            get("/api/v1/staff/doctors")
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString())
                .param("fullName", "Indiana"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1))
        .andExpect(jsonPath("$.data.content[0].fullName").value("Dr. Indiana Jones"));

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
                .param("minFee", "1100"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(0));

    // Sort by fullName desc
    mockMvc
        .perform(
            get("/api/v1/staff/doctors")
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString())
                .param("sort", "user.fullName,desc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content[0].fullName").value("Dr. Multi Role"))
        .andExpect(jsonPath("$.data.content[1].fullName").value("Dr. Indiana Jones"));
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
        .andExpect(jsonPath("$.data.content[0].departments[0]").value("ICU"))
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
