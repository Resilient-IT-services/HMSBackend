package com.res.hms.finance.application.service;

import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.finance.domain.Invoice;
import com.res.hms.finance.domain.InvoiceItem;
import com.res.hms.hospital.domain.Hospital;
import com.res.hms.hospital.infrastructure.repository.HospitalRepository;
import com.res.hms.iam.domain.UserAccount;
import com.res.hms.iam.infrastructure.repository.UserAccountRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InvoicePdfContextFactory {

  private final HospitalRepository hospitalRepository;
  private final HospitalContext hospitalContext;
  private final UserAccountRepository userAccountRepository;

  public InvoicePdfContext create(Invoice invoice) {
    UUID contextHospitalId = hospitalContext.getHospitalIdOrNull();
    UUID receptionistId = hospitalContext.currentUserIdOrNull();
    UUID effectiveHospitalId =
        (contextHospitalId != null) ? contextHospitalId : invoice.getHospitalId();

    Hospital hospital = null;
    if (effectiveHospitalId != null) {
      hospital = hospitalRepository.findById(effectiveHospitalId).orElse(null);
    }

    String hospitalName =
        (hospital != null && hospital.getName() != null)
            ? hospital.getName().toUpperCase()
            : "CAREFLOW DIGITAL HEALTHCARE";

    String gstinNumber =
        (hospital != null && hospital.getGstin() != null) ? hospital.getGstin() : "N/A";

    String receptionistName = "N/A";
    if (receptionistId != null) {
      receptionistName =
          userAccountRepository
              .findById(receptionistId)
              .map(UserAccount::getFullName)
              .orElse("N/A");
    }

    String patientName = "Unknown Patient";
    String patientPhone = "N/A";
    boolean isWalkIn = true;
    if (invoice.getPatient() != null) {
      patientName = invoice.getPatient().getFullName();
      patientPhone = invoice.getPatient().getPhone();
      isWalkIn = false;
    } else if (invoice.getCustomerName() != null) {
      patientName = invoice.getCustomerName();
      patientPhone = invoice.getCustomerPhone() != null ? invoice.getCustomerPhone() : "N/A";
    }

    String patientCode =
        (invoice.getPatient() != null && invoice.getPatient().getPatientCode() != null)
            ? invoice.getPatient().getPatientCode()
            : "N/A";

    String consultingDoctor = "N/A";
    if (invoice.getItems() != null && !invoice.getItems().isEmpty()) {
      InvoiceItem firstItem = invoice.getItems().get(0);
      if (firstItem.getDoctorName() != null) {
        consultingDoctor = firstItem.getDoctorName();
      }
    }

    return new InvoicePdfContext(
        hospitalName,
        gstinNumber,
        receptionistName,
        patientName,
        patientCode,
        patientPhone,
        consultingDoctor,
        isWalkIn,
        invoice.getVisitId());
  }
}
