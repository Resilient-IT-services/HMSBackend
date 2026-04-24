package com.res.hms.nurse.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.res.hms.common.tenant.HospitalContext;
import com.res.hms.common.util.ResourceLookup;
import com.res.hms.domain.Nurse;
import com.res.hms.domain.UserAccount;
import com.res.hms.nurse.dto.CreateNurseRequest;
import com.res.hms.nurse.repository.NurseRepository;
import com.res.hms.repository.UserAccountRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class NurseServiceTest {

  @Mock private NurseRepository nurseRepository;
  @Mock private UserAccountRepository userAccountRepository;
  @Mock private HospitalContext hospitalContext;
  @Mock private ResourceLookup resourceLookup;

  @InjectMocks private NurseService nurseService;

  @Test
  void createPersistsNurse() {
    UserAccount user = new UserAccount();
    user.setId(UUID.nameUUIDFromBytes("20".getBytes()));
    user.setFullName("Sara");

    when(hospitalContext.requireHospitalId()).thenReturn(UUID.nameUUIDFromBytes("4".getBytes()));
    when(resourceLookup.findRequired(
            eq(UUID.nameUUIDFromBytes("20".getBytes())),
            eq(UUID.nameUUIDFromBytes("4".getBytes())),
            any(),
            anyString()))
        .thenReturn(user);
    when(nurseRepository.save(any(Nurse.class)))
        .thenAnswer(
            invocation -> {
              Nurse nurse = invocation.getArgument(0);
              nurse.setId(UUID.nameUUIDFromBytes("40".getBytes()));
              return nurse;
            });

    var response =
        nurseService.create(new CreateNurseRequest(UUID.nameUUIDFromBytes("20".getBytes()), "ICU"));

    assertEquals("Sara", response.fullName());
  }

  @Test
  void listReturnsMappedNurses() {
    UserAccount user = new UserAccount();
    user.setFullName("Sara");

    Nurse nurse = new Nurse();
    nurse.setId(UUID.nameUUIDFromBytes("4".getBytes()));
    nurse.setUser(user);
    nurse.setDepartment("ICU");
    when(hospitalContext.requireHospitalId()).thenReturn(UUID.nameUUIDFromBytes("4".getBytes()));
    when(nurseRepository.findAllByHospitalIdOrderByCreatedAtDesc(
            eq(UUID.nameUUIDFromBytes("4".getBytes())), any(PageRequest.class)))
        .thenReturn(new PageImpl<>(List.of(nurse)));

    var response = nurseService.list(0, 10);

    assertEquals(1, response.content().size());
    assertEquals("ICU", response.content().getFirst().department());
  }
}
