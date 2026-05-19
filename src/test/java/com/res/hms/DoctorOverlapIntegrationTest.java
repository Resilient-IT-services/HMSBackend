package com.res.hms;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.res.hms.core.domain.Gender;
import com.res.hms.core.exception.ApiException;
import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.iam.application.dto.ScheduleDTO;
import com.res.hms.iam.application.dto.UserDetailsDTO;
import com.res.hms.iam.application.service.UserAggregateService;
import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DoctorOverlapIntegrationTest {

  @Autowired private UserAggregateService userAggregateService;
  @Autowired private TestDataHelper testDataHelper;
  @MockBean private HospitalContext hospitalContext;

  private UUID hospA;
  private UUID hospB;

  @BeforeEach
  void setup() {
    hospA = testDataHelper.createHospitalIfMissing("HOSP_A", "Hospital A");
    hospB = testDataHelper.createHospitalIfMissing("HOSP_B", "Hospital B");
    when(hospitalContext.isSuperAdmin()).thenReturn(true);
  }

  @Test
  void testOverlappingSchedulesAcrossHospitalsFails() {
    UserDetailsDTO request =
        UserDetailsDTO.builder()
            .username("overlap_dr")
            .fullName("Overlap Dr")
            .gender(Gender.MALE)
            .dob(LocalDate.of(1980, 1, 1))
            .doctorInfo(UserDetailsDTO.DoctorInfo.builder().specializations(java.util.List.of("General")).build())
            .memberships(
                List.of(
                    UserDetailsDTO.HospitalMembershipDTO.builder()
                        .hospitalId(hospA)
                        .roleAssignments(
                            List.of(
                                UserDetailsDTO.RoleAssignmentDTO.builder()
                                    .roleCode("DOCTOR")
                                    .schedules(
                                        List.of(
                                            ScheduleDTO.builder()
                                                .dayOfWeek(DayOfWeek.MONDAY) // Monday
                                                .startTime(LocalTime.of(16, 0)) // 4 PM
                                                .endTime(LocalTime.of(18, 0)) // 6 PM
                                                .build()))
                                    .build()))
                        .build(),
                    UserDetailsDTO.HospitalMembershipDTO.builder()
                        .hospitalId(hospB)
                        .roleAssignments(
                            List.of(
                                UserDetailsDTO.RoleAssignmentDTO.builder()
                                    .roleCode("DOCTOR")
                                    .schedules(
                                        List.of(
                                            ScheduleDTO.builder()
                                                .dayOfWeek(DayOfWeek.MONDAY) // Monday
                                                .startTime(LocalTime.of(15, 0)) // 3 PM
                                                .endTime(LocalTime.of(17, 0)) // 5 PM
                                                .build()))
                                    .build()))
                        .build()))
            .build();

    assertThrows(
        ApiException.class,
        () -> userAggregateService.saveUserAggregate(request),
        "Should throw exception due to overlapping schedules");
  }
}
