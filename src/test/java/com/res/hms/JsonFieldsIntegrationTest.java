package com.res.hms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.res.hms.audit.application.dto.AuditDetail;
import com.res.hms.audit.domain.AuditAction;
import com.res.hms.audit.domain.AuditLog;
import com.res.hms.audit.infrastructure.repository.AuditLogRepository;
import com.res.hms.core.domain.ProficiencyLevel;
import com.res.hms.hospital.application.dto.HospitalSettings;
import com.res.hms.hospital.domain.Hospital;
import com.res.hms.hospital.domain.HospitalStatus;
import com.res.hms.hospital.infrastructure.repository.HospitalRepository;
import com.res.hms.iam.application.dto.DoctorEducation;
import com.res.hms.iam.application.dto.ReceptionistFluency;
import com.res.hms.iam.application.repository.DoctorProfileRepository;
import com.res.hms.iam.domain.DoctorProfile;
import com.res.hms.iam.domain.ReceptionistProfile;
import com.res.hms.iam.domain.UserAccount;
import com.res.hms.iam.infrastructure.repository.ReceptionistProfileRepository;
import com.res.hms.iam.infrastructure.repository.UserAccountRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class JsonFieldsIntegrationTest {

  @Autowired private AuditLogRepository auditLogRepository;
  @Autowired private DoctorProfileRepository doctorRepository;
  @Autowired private HospitalRepository hospitalRepository;
  @Autowired private ReceptionistProfileRepository receptionistProfileRepository;
  @Autowired private UserAccountRepository userAccountRepository;

  @Test
  void auditLogDetailsJsonMapping() {
    AuditLog log = new AuditLog();
    log.setEntityName("TestEntity");
    log.setAction(AuditAction.CREATE);
    log.setCreatedAt(LocalDateTime.now());
    log.setUpdatedAt(LocalDateTime.now());

    AuditDetail detail =
        AuditDetail.builder().before(Map.of("key", "old")).after(Map.of("key", "new")).build();
    log.setDetails(detail);

    AuditLog saved = auditLogRepository.save(log);
    assertNotNull(saved.getId());

    AuditLog fetched = auditLogRepository.findById(saved.getId()).orElseThrow();
    assertEquals("old", fetched.getDetails().getBefore().get("key"));
    assertEquals("new", fetched.getDetails().getAfter().get("key"));
  }

  @Test
  void doctorEducationJsonMapping() {
    UserAccount user = new UserAccount();
    user.setUsername("doc_json_test");
    user.setPasswordHash("hash");
    user.setCreatedAt(LocalDateTime.now());
    user.setUpdatedAt(LocalDateTime.now());
    userAccountRepository.save(user);

    DoctorProfile doctor = new DoctorProfile();
    doctor.setUser(user);
    doctor.setSpecialization("General");

    DoctorEducation edu =
        DoctorEducation.builder()
            .qualifications(
                List.of(
                    DoctorEducation.Qualification.builder()
                        .degree("MBBS")
                        .institution("Medical College")
                        .year(2010)
                        .build()))
            .build();
    doctor.setEducation(edu);

    DoctorProfile saved = doctorRepository.save(doctor);
    DoctorProfile fetched = doctorRepository.findById(saved.getId()).orElseThrow();

    assertEquals(1, fetched.getEducation().getQualifications().size());
    assertEquals("MBBS", fetched.getEducation().getQualifications().get(0).getDegree());
  }

  @Test
  void hospitalSettingsJsonMapping() {
    Hospital hospital = new Hospital();
    hospital.setName("JSON Hospital");
    hospital.setCode("JHOSP");
    hospital.setStatus(HospitalStatus.ACTIVE);
    hospital.setCreatedAt(LocalDateTime.now());
    hospital.setUpdatedAt(LocalDateTime.now());

    HospitalSettings settings =
        HospitalSettings.builder()
            .timezone("UTC")
            .currency("USD")
            .opdEnabled(true)
            .configurations(Map.of("theme", "dark"))
            .build();
    hospital.setSettings(settings);

    Hospital saved = hospitalRepository.save(hospital);
    Hospital fetched = hospitalRepository.findById(saved.getId()).orElseThrow();

    assertEquals("UTC", fetched.getSettings().getTimezone());
    assertEquals("dark", fetched.getSettings().getConfigurations().get("theme"));
  }

  @Test
  void receptionistFluencyJsonMapping() {
    UserAccount user = new UserAccount();
    user.setUsername("recep_json_test");
    user.setPasswordHash("hash");
    user.setCreatedAt(LocalDateTime.now());
    user.setUpdatedAt(LocalDateTime.now());
    userAccountRepository.save(user);

    ReceptionistProfile profile = new ReceptionistProfile();
    profile.setUser(user);

    ReceptionistFluency fluency =
        ReceptionistFluency.builder()
            .fluencies(
                List.of(
                    new ReceptionistFluency.LanguageEntry("English", ProficiencyLevel.NATIVE),
                    new ReceptionistFluency.LanguageEntry("Spanish", ProficiencyLevel.FLUENT)))
            .build();
    profile.setLanguagesSpoken(fluency);

    ReceptionistProfile saved = receptionistProfileRepository.save(profile);
    ReceptionistProfile fetched =
        receptionistProfileRepository.findById(saved.getId()).orElseThrow();

    assertEquals(2, fetched.getLanguagesSpoken().getFluencies().size());
    assertEquals("English", fetched.getLanguagesSpoken().getFluencies().get(0).getLanguage());
  }
}
