package com.res.hms.billing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.res.hms.billing.dto.CreateOpdBillingRequest;
import com.res.hms.billing.repository.BillingRepository;
import com.res.hms.common.tenant.HospitalContext;
import com.res.hms.common.util.ResourceLookup;
import com.res.hms.doctor.service.DoctorService;
import com.res.hms.domain.Billing;
import com.res.hms.domain.Doctor;
import com.res.hms.domain.DoctorHospitalDetails;
import com.res.hms.domain.Patient;
import com.res.hms.domain.UserAccount;
import com.res.hms.patient.service.PatientService;
import com.res.hms.pharmacy.repository.PrescriptionRepository;
import com.res.hms.repository.DoctorHospitalDetailsRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BillingServiceTest {

  @Mock private BillingRepository billingRepository;
  @Mock private PatientService patientService;
  @Mock private HospitalContext hospitalContext;
  @Mock private ResourceLookup resourceLookup;
  @Mock private DoctorService doctorService;
  @Mock private PrescriptionRepository prescriptionRepository;
  @Mock private DoctorHospitalDetailsRepository doctorHospitalDetailsRepository;

  @InjectMocks private BillingService billingService;

  private final UUID hospitalId = UUID.randomUUID();
  private final UUID patientId = UUID.randomUUID();
  private final UUID doctorId = UUID.randomUUID();

  @Test
  void createOpdBillingPersistsDraftWithFee() {
    Patient patient = new Patient();
    patient.setId(patientId);

    UserAccount user = new UserAccount();
    user.setId(UUID.randomUUID());
    Doctor doctor = new Doctor();
    doctor.setId(doctorId);
    doctor.setUser(user);

    DoctorHospitalDetails details = new DoctorHospitalDetails();
    details.setConsultationFee(new BigDecimal("100"));
    details.setFollowUpDays(0);

    when(hospitalContext.requireHospitalId()).thenReturn(hospitalId);
    when(patientService.requirePatient(patientId, hospitalId)).thenReturn(patient);
    when(doctorService.requireDoctor(doctorId, hospitalId)).thenReturn(doctor);
    when(prescriptionRepository.findTopByPatient_IdAndDoctor_IdOrderByCreatedAtDesc(patientId, doctorId))
        .thenReturn(Optional.empty());
    when(doctorHospitalDetailsRepository.findByUserIdAndHospitalId(user.getId(), hospitalId))
        .thenReturn(Optional.of(details));
    when(billingRepository.save(any(Billing.class))).thenAnswer(i -> i.getArgument(0));

    var response = billingService.createOpdBilling(new CreateOpdBillingRequest(patientId, doctorId));

    assertEquals(new BigDecimal("100"), response.totalAmount());
    verify(billingRepository).save(any(Billing.class));
  }
}
