package com.res.hms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.res.hms.audit.dto.AuditDetail;
import com.res.hms.audit.repository.AuditLogRepository;
import com.res.hms.doctor.dto.DoctorEducation;
import com.res.hms.doctor.repository.DoctorRepository;
import com.res.hms.domain.AuditLog;
import com.res.hms.domain.Doctor;
import com.res.hms.domain.Hospital;
import com.res.hms.domain.ProficiencyLevel;
import com.res.hms.domain.ReceptionistProfile;
import com.res.hms.domain.UserAccount;
import com.res.hms.hospital.dto.HospitalSettings;
import com.res.hms.repository.HospitalRepository;
import com.res.hms.repository.ReceptionistProfileRepository;
import com.res.hms.repository.UserAccountRepository;
import com.res.hms.user.dto.ReceptionistFluency;
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
  @Autowired private DoctorRepository doctorRepository;
  @Autowired private HospitalRepository hospitalRepository;
  @Autowired private ReceptionistProfileRepository receptionistProfileRepository;
  @Autowired private UserAccountRepository userAccountRepository;

  @Test
  void auditLogDetailsJsonMapping() {
    AuditLog log = new AuditLog();
    log.setEntityName("TestEntity");
    log.setAction("CREATE");
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

    Doctor doctor = new Doctor();
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

    Doctor saved = doctorRepository.save(doctor);
    Doctor fetched = doctorRepository.findById(saved.getId()).orElseThrow();

    assertEquals(1, fetched.getEducation().getQualifications().size());
    assertEquals("MBBS", fetched.getEducation().getQualifications().get(0).getDegree());
  }

  @Test
  void hospitalSettingsJsonMapping() {
    Hospital hospital = new Hospital();
    hospital.setName("JSON Hospital");
    hospital.setCode("JHOSP");
    hospital.setStatus("ACTIVE");
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
        receptionistProfileRepository.findById(saved.getUserId()).orElseThrow();

    assertEquals(2, fetched.getLanguagesSpoken().getFluencies().size());
    assertEquals("English", fetched.getLanguagesSpoken().getFluencies().get(0).getLanguage());
  }
}
