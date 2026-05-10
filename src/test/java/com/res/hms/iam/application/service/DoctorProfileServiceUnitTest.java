package com.res.hms.iam.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.res.hms.clinical.domain.DoctorAvailabilityException;
import com.res.hms.clinical.domain.DoctorSchedule;
import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.core.util.ResourceLookup;
import com.res.hms.iam.application.repository.DoctorProfileRepository;
import com.res.hms.iam.domain.DoctorProfile;
import com.res.hms.iam.domain.Role;
import com.res.hms.iam.domain.RoleCode;
import com.res.hms.iam.domain.UserAccount;
import com.res.hms.iam.domain.UserRoleAssignment;
import com.res.hms.iam.infrastructure.repository.UserRoleAssignmentRepository;
import com.res.hms.clinical.infrastructure.repository.DoctorScheduleRepository;
import com.res.hms.clinical.infrastructure.repository.DoctorAvailabilityExceptionRepository;
import com.res.hms.clinical.infrastructure.repository.AppointmentRepository;

@ExtendWith(MockitoExtension.class)
class DoctorProfileServiceUnitTest {

    @Mock private DoctorProfileRepository doctorProfileRepository;
    @Mock private HospitalContext hospitalContext;
    @Mock private UserRoleAssignmentRepository assignmentRepository;
    @Mock private DoctorAvailabilityExceptionRepository availabilityExceptionRepository;
    @Mock private DoctorScheduleRepository scheduleRepository;
    @Mock private AppointmentRepository appointmentRepository;

    @InjectMocks
    private DoctorProfileService doctorProfileService;

    private UUID hospitalId;
    private UUID doctorId;
    private DoctorProfile doctorProfile;
    private UserAccount user;

    @BeforeEach
    void setUp() {
        hospitalId = UUID.randomUUID();
        doctorId = UUID.randomUUID();
        user = new UserAccount();
        user.setId(UUID.randomUUID());
        
        doctorProfile = new DoctorProfile();
        doctorProfile.setId(doctorId);
        doctorProfile.setUser(user);
        
        Role drRole = new Role();
        drRole.setCode(RoleCode.DOCTOR);
        
        UserRoleAssignment assignment = new UserRoleAssignment();
        assignment.setHospitalId(hospitalId);
        assignment.setRole(drRole);
        assignment.setUser(user);
        user.getAssignments().add(assignment);
    }

    @Test
    void testGetAvailableSlots_FullDayOff() {
        when(hospitalContext.requireHospitalId()).thenReturn(hospitalId);
        when(doctorProfileRepository.findById(doctorId)).thenReturn(Optional.of(doctorProfile));
        
        DoctorAvailabilityException ex = new DoctorAvailabilityException();
        ex.setType("FULL_DAY_OFF");
        when(availabilityExceptionRepository.findByDoctorIdAndHospitalIdAndDate(any(), eq(hospitalId), any()))
            .thenReturn(List.of(ex));

        var response = doctorProfileService.getAvailableSlots(doctorId, LocalDate.now().plusDays(1));

        assertTrue(response.getSessions().isEmpty());
    }

    @Test
    void testGetAvailableSlots_RegularSchedule() {
        LocalDate nextMonday = LocalDate.now().plusWeeks(1);
        if (nextMonday.getDayOfWeek().getValue() != 1) {
            nextMonday = nextMonday.plusDays(8 - nextMonday.getDayOfWeek().getValue());
        }

        when(hospitalContext.requireHospitalId()).thenReturn(hospitalId);
        when(doctorProfileRepository.findById(doctorId)).thenReturn(Optional.of(doctorProfile));
        when(availabilityExceptionRepository.findByDoctorIdAndHospitalIdAndDate(any(), any(), any())).thenReturn(List.of());
        when(appointmentRepository.findByDoctorIdAndDate(any(), any())).thenReturn(List.of());
        
        DoctorSchedule schedule = new DoctorSchedule();
        schedule.setDayOfWeek((short) 1);
        schedule.setStartTime(LocalTime.of(9, 0));
        schedule.setEndTime(LocalTime.of(12, 0));
        schedule.setMaxPatients(10);
        schedule.setSlotDurationMinutes(20);
        
        when(scheduleRepository.findByAssignmentId(any())).thenReturn(List.of(schedule));

        var response = doctorProfileService.getAvailableSlots(doctorId, nextMonday);

        assertFalse(response.getSessions().isEmpty());
        assertEquals(LocalTime.of(9, 0), response.getSessions().get(0).getStartTime());
        assertEquals(10, response.getSessions().get(0).getMaxPatients());
    }
}
