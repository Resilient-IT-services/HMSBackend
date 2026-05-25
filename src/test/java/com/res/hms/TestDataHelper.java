package com.res.hms;

import com.res.hms.clinical.domain.Patient;
import com.res.hms.clinical.domain.PatientVisit;
import com.res.hms.clinical.domain.VisitStatus;
import com.res.hms.clinical.domain.VisitType;
import com.res.hms.clinical.infrastructure.repository.PatientRepository;
import com.res.hms.clinical.infrastructure.repository.PatientVisitRepository;
import com.res.hms.core.domain.Gender;
import com.res.hms.finance.domain.Invoice;
import com.res.hms.finance.domain.InvoiceStatus;
import com.res.hms.finance.infrastructure.repository.InvoiceRepository;
import com.res.hms.hospital.domain.Hospital;
import com.res.hms.hospital.domain.HospitalStatus;
import com.res.hms.hospital.infrastructure.repository.HospitalRepository;
import com.res.hms.iam.domain.Role;
import com.res.hms.iam.domain.RoleCode;
import com.res.hms.iam.domain.UserAccount;
import com.res.hms.iam.domain.UserContract;
import com.res.hms.iam.infrastructure.repository.RoleRepository;
import com.res.hms.iam.infrastructure.repository.UserAccountRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TestDataHelper {

  @Autowired private HospitalRepository hospitalRepository;

  @Autowired private PatientRepository patientRepository;

  @Autowired private PatientVisitRepository patientVisitRepository;

  @Autowired private InvoiceRepository invoiceRepository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public PatientVisit createTestVisit(UUID hospitalId, UUID patientId) {
    PatientVisit v = new PatientVisit();
    v.setHospitalId(hospitalId);
    v.setPatientId(patientId);
    v.setVisitNumber("VST-" + UUID.randomUUID().toString().substring(0, 8));
    v.setVisitType(VisitType.OPD);
    v.setStatus(VisitStatus.ACTIVE);
    v.setStartedAt(java.time.Instant.now());
    return patientVisitRepository.save(v);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Invoice createTestInvoice(UUID hospitalId, UUID patientId, UUID visitId) {
    Invoice i = new Invoice();
    i.setHospitalId(hospitalId);
    i.setPatientId(patientId);
    i.setVisitId(visitId);
    i.setInvoiceNumber("INV-" + UUID.randomUUID().toString().substring(0, 8));
    i.setStatus(InvoiceStatus.UNPAID);
    i.setTotalAmount(BigDecimal.valueOf(1000.00));
    i.setAmountPaid(BigDecimal.ZERO);
    return invoiceRepository.save(i);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Patient createTestPatient(UUID hospitalId) {
    Patient p = new Patient();
    p.setHospitalId(hospitalId);
    p.setFullName("Test Patient");
    p.setGender(Gender.FEMALE);
    p.setPatientCode("P-" + UUID.randomUUID().toString().substring(0, 8));
    return patientRepository.save(p);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public UUID createHospitalIfMissing(String code, String name) {
    return hospitalRepository
        .findByCode(code)
        .map(Hospital::getId)
        .orElseGet(
            () -> {
              Hospital h = new Hospital();
              h.setCode(code);
              h.setName(name);
              h.setAddress("Address");
              h.setStatus(HospitalStatus.ACTIVE);
              return hospitalRepository.save(h).getId();
            });
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void createUserIfMissing(
      String username,
      RoleCode roleCode,
      UUID hospitalId,
      String password,
      PasswordEncoder encoder,
      UserAccountRepository userRepo,
      RoleRepository roleRepo) {
    if (userRepo.existsByUsername(username)) {
      return;
    }

    UserAccount user = new UserAccount();
    user.setUsername(username);
    user.setPasswordHash(encoder.encode(password));
    user.setFullName(username + " Full Name");
    user.setActive(true);

    Role role =
        roleRepo
            .findByCode(roleCode)
            .orElseThrow(() -> new RuntimeException("Role not found: " + roleCode));

    UserContract assignment = new UserContract();
    assignment.setUser(user);
    assignment.setHospitalId(hospitalId);
    assignment.setRole(role);
    user.getContracts().add(assignment);

    userRepo.save(user);
  }

  @Autowired
  private com.res.hms.pharmacy.infrastructure.repository.MedicineRepository medicineRepository;

  @Autowired
  private com.res.hms.pharmacy.infrastructure.repository.PharmacyStockRepository
      pharmacyStockRepository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public com.res.hms.pharmacy.domain.Medicine createTestMedicine(
      UUID hospitalId, String name, String strength) {
    com.res.hms.pharmacy.domain.Medicine m = new com.res.hms.pharmacy.domain.Medicine();
    m.setName(name);
    m.setStrength(strength);
    m.setCategory("General");
    return medicineRepository.save(m);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public com.res.hms.pharmacy.domain.PharmacyStock addStock(
      UUID hospitalId,
      com.res.hms.pharmacy.domain.Medicine medicine,
      int quantity,
      BigDecimal unitPrice,
      String batch) {
    com.res.hms.pharmacy.domain.PharmacyStock s = new com.res.hms.pharmacy.domain.PharmacyStock();
    s.setHospitalId(hospitalId);
    s.setMedicine(medicine);
    s.setQuantity(quantity);
    s.setSellingPrice(unitPrice);
    s.setPurchasePrice(unitPrice.subtract(BigDecimal.ONE));
    s.setBatchNumber(batch);
    s.setExpiryDate(java.time.LocalDate.now().plusYears(1));
    return pharmacyStockRepository.save(s);
  }
}
