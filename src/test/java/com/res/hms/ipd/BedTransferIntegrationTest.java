package com.res.hms.ipd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.res.hms.TestDataHelper;
import com.res.hms.clinical.domain.Patient;
import com.res.hms.iam.domain.RoleCode;
import com.res.hms.iam.domain.UserAccount;
import com.res.hms.iam.infrastructure.repository.RoleRepository;
import com.res.hms.iam.infrastructure.repository.UserAccountRepository;
import com.res.hms.ipd.application.dto.TransferBedRequest;
import com.res.hms.ipd.domain.Admission;
import com.res.hms.ipd.domain.AdmissionStatus;
import com.res.hms.ipd.domain.Bed;
import com.res.hms.ipd.domain.BedStatus;
import com.res.hms.ipd.domain.Ward;
import com.res.hms.ipd.domain.WardType;
import com.res.hms.ipd.infrastructure.repository.AdmissionRepository;
import com.res.hms.ipd.infrastructure.repository.BedRepository;
import com.res.hms.ipd.infrastructure.repository.WardRepository;
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
class BedTransferIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserAccountRepository userAccountRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private TestDataHelper testDataHelper;
  @Autowired private WardRepository wardRepository;
  @Autowired private BedRepository bedRepository;
  @Autowired private AdmissionRepository admissionRepository;
  @Autowired private ObjectMapper objectMapper;

  private UUID testHospitalId;
  private MockHttpSession session;
  private UserAccount adminUser;

  @BeforeEach
  void setup() throws Exception {
    testHospitalId = testDataHelper.createHospitalIfMissing("TRANSFER_TEST", "Transfer Hospital");

    String username = "admin_transfer_test";
    testDataHelper.createUserIfMissing(
        username,
        RoleCode.ADMIN,
        testHospitalId,
        "admin123",
        passwordEncoder,
        userAccountRepository,
        roleRepository);

    adminUser = userAccountRepository.findByUsername(username).orElseThrow();

    session =
        (MockHttpSession)
            mockMvc
                .perform(
                    post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            "{\"username\": \"admin_transfer_test\", \"password\": \"admin123\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getRequest()
                .getSession(false);
  }

  @Test
  void testBedTransferSuccess() throws Exception {
    // 1. Create a Patient
    Patient patient = testDataHelper.createTestPatient(testHospitalId);

    // 2. Create Wards and Beds
    Ward ward = new Ward();
    ward.setHospitalId(testHospitalId);
    ward.setWardCode("G-WARD");
    ward.setWardName("General Ward");
    ward.setWardType(WardType.GENERAL);
    ward.setTotalBeds(2);
    ward.setActive(true);
    ward = wardRepository.save(ward);

    Bed currentBed = new Bed();
    currentBed.setHospitalId(testHospitalId);
    currentBed.setWardId(ward.getId());
    currentBed.setBedNumber("B1");
    currentBed.setStatus(BedStatus.OCCUPIED);
    currentBed.setActive(true);
    currentBed = bedRepository.save(currentBed);

    Bed targetBed = new Bed();
    targetBed.setHospitalId(testHospitalId);
    targetBed.setWardId(ward.getId());
    targetBed.setBedNumber("B2");
    targetBed.setStatus(BedStatus.AVAILABLE);
    targetBed.setActive(true);
    targetBed = bedRepository.save(targetBed);

    // 3. Create Admission
    Admission admission = new Admission();
    admission.setHospitalId(testHospitalId);
    admission.setAdmissionNumber("ADM-001");
    admission.setPatientId(patient.getId());
    admission.setDoctorId(adminUser.getId());
    admission.setBedId(currentBed.getId());
    admission.setStatus(AdmissionStatus.ADMITTED);
    admission = admissionRepository.save(admission);

    // 4. Perform Transfer Request
    TransferBedRequest request = new TransferBedRequest();
    request.setToBedId(targetBed.getId());
    request.setTransferReason("Patient requires more space");
    request.setRemarks("Moved to B2");

    mockMvc
        .perform(
            post("/api/v1/ipd/admissions/" + admission.getId() + "/transfer")
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.fromBedId").value(currentBed.getId().toString()))
        .andExpect(jsonPath("$.data.toBedId").value(targetBed.getId().toString()))
        .andExpect(jsonPath("$.data.transferReason").value("Patient requires more space"))
        .andExpect(jsonPath("$.data.remarks").value("Moved to B2"))
        .andExpect(jsonPath("$.data.transferredBy").value(adminUser.getId().toString()));

    // 5. Verify database states
    Bed updatedOldBed = bedRepository.findById(currentBed.getId()).orElseThrow();
    Bed updatedNewBed = bedRepository.findById(targetBed.getId()).orElseThrow();
    Admission updatedAdmission = admissionRepository.findById(admission.getId()).orElseThrow();

    assertEquals(BedStatus.AVAILABLE, updatedOldBed.getStatus());
    assertEquals(BedStatus.OCCUPIED, updatedNewBed.getStatus());
    assertEquals(targetBed.getId(), updatedAdmission.getBedId());

    // 6. Fetch Transfers list
    mockMvc
        .perform(
            get("/api/v1/ipd/admissions/" + admission.getId() + "/transfers")
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data.length()").value(1))
        .andExpect(jsonPath("$.data[0].fromBedId").value(currentBed.getId().toString()))
        .andExpect(jsonPath("$.data[0].toBedId").value(targetBed.getId().toString()));
  }

  @Test
  void testBedTransferValidationFailures() throws Exception {
    Patient patient = testDataHelper.createTestPatient(testHospitalId);

    Ward ward = new Ward();
    ward.setHospitalId(testHospitalId);
    ward.setWardCode("G-WARD-VAL");
    ward.setWardName("General Ward Val");
    ward.setWardType(WardType.GENERAL);
    ward.setTotalBeds(2);
    ward.setActive(true);
    ward = wardRepository.save(ward);

    Bed currentBed = new Bed();
    currentBed.setHospitalId(testHospitalId);
    currentBed.setWardId(ward.getId());
    currentBed.setBedNumber("B10");
    currentBed.setStatus(BedStatus.OCCUPIED);
    currentBed.setActive(true);
    currentBed = bedRepository.save(currentBed);

    Bed unavailableBed = new Bed();
    unavailableBed.setHospitalId(testHospitalId);
    unavailableBed.setWardId(ward.getId());
    unavailableBed.setBedNumber("B11");
    unavailableBed.setStatus(BedStatus.OCCUPIED);
    unavailableBed.setActive(true);
    unavailableBed = bedRepository.save(unavailableBed);

    // Admission status CANCELLED
    Admission cancelledAdmission = new Admission();
    cancelledAdmission.setHospitalId(testHospitalId);
    cancelledAdmission.setAdmissionNumber("ADM-002");
    cancelledAdmission.setPatientId(patient.getId());
    cancelledAdmission.setDoctorId(adminUser.getId());
    cancelledAdmission.setBedId(currentBed.getId());
    cancelledAdmission.setStatus(AdmissionStatus.CANCELLED);
    cancelledAdmission = admissionRepository.save(cancelledAdmission);

    // Try transferring cancelled admission
    TransferBedRequest request = new TransferBedRequest();
    request.setToBedId(unavailableBed.getId());

    mockMvc
        .perform(
            post("/api/v1/ipd/admissions/" + cancelledAdmission.getId() + "/transfer")
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("INVALID_ADMISSION_STATUS"));

    // Admission status ADMITTED
    Admission admission = new Admission();
    admission.setHospitalId(testHospitalId);
    admission.setAdmissionNumber("ADM-003");
    admission.setPatientId(patient.getId());
    admission.setDoctorId(adminUser.getId());
    admission.setBedId(currentBed.getId());
    admission.setStatus(AdmissionStatus.ADMITTED);
    admission = admissionRepository.save(admission);

    // Try transferring to same bed
    request.setToBedId(currentBed.getId());
    mockMvc
        .perform(
            post("/api/v1/ipd/admissions/" + admission.getId() + "/transfer")
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("INVALID_BED_TRANSFER"));

    // Try transferring to occupied/unavailable bed
    request.setToBedId(unavailableBed.getId());
    mockMvc
        .perform(
            post("/api/v1/ipd/admissions/" + admission.getId() + "/transfer")
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("BED_NOT_AVAILABLE"));
  }
}
