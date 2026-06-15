package com.res.hms.clinical.application.appointment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.res.hms.clinical.application.appointment.dto.AppointmentDTO;
import com.res.hms.clinical.application.appointment.dto.AppointmentStatsDTO;
import com.res.hms.clinical.application.appointment.dto.AppointmentSummaryDTO;
import com.res.hms.clinical.application.appointment.dto.UpdateAppointmentStatusRequest;
import com.res.hms.clinical.application.prescription.repository.PrescriptionRepository;
import com.res.hms.clinical.application.prescription.service.PrescriptionService;
import com.res.hms.clinical.domain.Appointment;
import com.res.hms.clinical.domain.AppointmentStatus;
import com.res.hms.clinical.infrastructure.repository.AppointmentRepository;
import com.res.hms.core.api.PageResponse;
import com.res.hms.core.exception.ApiException;
import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.core.util.ResourceLookup;
import com.res.hms.hospital.infrastructure.repository.HospitalRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

  @Mock private AppointmentRepository appointmentRepository;
  @Mock private PrescriptionRepository prescriptionRepository;
  @Mock private PrescriptionService prescriptionService;
  @Mock private HospitalContext hospitalContext;
  @Mock private ResourceLookup resourceLookup;
  @Mock private HospitalRepository hospitalRepository;
  @Mock private EntityManager entityManager;
  @Mock private java.time.Clock clock;

  @InjectMocks private AppointmentService appointmentService;

  private UUID hospitalId;
  private UUID appointmentId;

  @BeforeEach
  void setUp() {
    hospitalId = UUID.randomUUID();
    appointmentId = UUID.randomUUID();
    lenient().when(clock.instant()).thenReturn(Instant.now());
  }

  @Test
  void testGetById() {
    when(hospitalContext.requireHospitalId()).thenReturn(hospitalId);
    Appointment appointment = new Appointment();
    appointment.setId(appointmentId);
    appointment.setAppointmentTime(Instant.now());
    when(appointmentRepository.findByIdAndHospitalId(appointmentId, hospitalId))
        .thenReturn(Optional.of(appointment));

    AppointmentDTO result = appointmentService.getById(appointmentId);
    assertNotNull(result);
    assertEquals(appointmentId, result.getId());
  }

  @Test
  void testGetByIdNotFound() {
    when(hospitalContext.requireHospitalId()).thenReturn(hospitalId);
    when(appointmentRepository.findByIdAndHospitalId(appointmentId, hospitalId))
        .thenReturn(Optional.empty());

    assertThrows(ApiException.class, () -> appointmentService.getById(appointmentId));
  }

  @Test
  @SuppressWarnings("unchecked")
  void testList() {
    when(hospitalContext.requireHospitalId()).thenReturn(hospitalId);
    Pageable pageable = PageRequest.of(0, 10);
    Appointment appointment = new Appointment();
    appointment.setId(appointmentId);
    appointment.setAppointmentTime(Instant.now());

    Page<Appointment> page = new PageImpl<>(List.of(appointment));
    when(appointmentRepository.findAll(any(Specification.class), any(Pageable.class)))
        .thenReturn(page);

    PageResponse<AppointmentDTO> result =
        appointmentService.list(pageable, null, Collections.emptyMap());
    assertNotNull(result);
    assertEquals(1, result.content().size());
  }

  @Test
  @SuppressWarnings("unchecked")
  void testListSummary() {
    when(hospitalContext.requireHospitalId()).thenReturn(hospitalId);
    Pageable pageable = PageRequest.of(0, 10);
    Appointment appointment = new Appointment();
    appointment.setId(appointmentId);
    appointment.setAppointmentTime(Instant.now());

    Page<Appointment> page = new PageImpl<>(List.of(appointment));
    when(appointmentRepository.findAll(any(Specification.class), any(Pageable.class)))
        .thenReturn(page);

    PageResponse<AppointmentSummaryDTO> result =
        appointmentService.listSummary(pageable, null, Collections.emptyMap());
    assertNotNull(result);
    assertEquals(1, result.content().size());
  }

  @Test
  void testUpdateStatus() {
    when(hospitalContext.requireHospitalId()).thenReturn(hospitalId);
    Appointment appointment = new Appointment();
    appointment.setId(appointmentId);
    appointment.setStatus(AppointmentStatus.SCHEDULED);
    appointment.setAppointmentTime(Instant.now());

    when(resourceLookup.findRequired(eq(appointmentId), eq(hospitalId), any(), anyString()))
        .thenReturn(appointment);
    when(appointmentRepository.save(any())).thenReturn(appointment);

    UpdateAppointmentStatusRequest request =
        new UpdateAppointmentStatusRequest(AppointmentStatus.COMPLETED);
    AppointmentDTO result = appointmentService.updateStatus(appointmentId, request);

    assertNotNull(result);
    assertEquals(AppointmentStatus.COMPLETED, appointment.getStatus());
  }

  @Test
  void testUpdateStatusReceptionistRestriction() {
    when(hospitalContext.requireHospitalId()).thenReturn(hospitalId);
    when(hospitalContext.isReceptionist()).thenReturn(true);

    Appointment appointment = new Appointment();
    appointment.setId(appointmentId);
    appointment.setStatus(AppointmentStatus.CONSULTATION_STARTED);
    appointment.setAppointmentTime(Instant.now());

    when(resourceLookup.findRequired(eq(appointmentId), eq(hospitalId), any(), anyString()))
        .thenReturn(appointment);

    UpdateAppointmentStatusRequest request =
        new UpdateAppointmentStatusRequest(AppointmentStatus.CANCELLED);
    assertThrows(ApiException.class, () -> appointmentService.updateStatus(appointmentId, request));
  }

  @Test
  void testGetStats() {
    when(hospitalContext.requireHospitalId()).thenReturn(hospitalId);

    CriteriaBuilder cb = mock(CriteriaBuilder.class);
    CriteriaQuery<Object[]> query = mock(CriteriaQuery.class);
    Root<Appointment> root = mock(Root.class);

    when(entityManager.getCriteriaBuilder()).thenReturn(cb);
    when(cb.createQuery(Object[].class)).thenReturn(query);
    when(query.from(Appointment.class)).thenReturn(root);

    jakarta.persistence.TypedQuery<Object[]> typedQuery =
        mock(jakarta.persistence.TypedQuery.class);
    when(entityManager.createQuery(query)).thenReturn(typedQuery);

    List<Object[]> queryResults = new ArrayList<>();
    queryResults.add(new Object[] {AppointmentStatus.SCHEDULED, 5L});
    when(typedQuery.getResultList()).thenReturn(queryResults);

    // For upcoming count
    CriteriaQuery<Long> countQuery = mock(CriteriaQuery.class);
    when(cb.createQuery(Long.class)).thenReturn(countQuery);
    Root<Appointment> countRoot = mock(Root.class);
    when(countQuery.from(Appointment.class)).thenReturn(countRoot);
    jakarta.persistence.TypedQuery<Long> countTypedQuery =
        mock(jakarta.persistence.TypedQuery.class);
    when(entityManager.createQuery(countQuery)).thenReturn(countTypedQuery);
    when(countTypedQuery.getSingleResult()).thenReturn(10L);

    AppointmentStatsDTO result = appointmentService.getStats(null, Collections.emptyMap());

    assertNotNull(result);
    assertEquals(5L, result.getTotal());
    assertEquals(10L, result.getUpcoming());
    assertEquals(5L, result.getByStatus().get(AppointmentStatus.SCHEDULED));
  }

  @Test
  void testToDtoNull() {
    assertNull(appointmentService.toDto(null));
    assertNull(appointmentService.toSummaryDto(null));
  }
}
