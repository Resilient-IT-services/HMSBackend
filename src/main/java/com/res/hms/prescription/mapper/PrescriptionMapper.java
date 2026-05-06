package com.res.hms.prescription.mapper;

import com.res.hms.domain.Prescription;
import com.res.hms.domain.PrescriptionItem;
import com.res.hms.prescription.dto.PrescriptionDTO;
import com.res.hms.prescription.dto.PrescriptionItemDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PrescriptionMapper {
  @Mapping(target = "patientId", source = "patient.id")
  @Mapping(target = "patientName", source = "patient.fullName")
  @Mapping(target = "doctorId", source = "doctorProfile.id")
  @Mapping(target = "doctorName", source = "doctorProfile.fullName")
  @Mapping(target = "appointmentId", source = "appointment.id")
  @Mapping(target = "createdAt", source = "appointment.appointmentTime")
  PrescriptionDTO toDto(Prescription prescription);

  @Mapping(target = "medicineId", source = "medicine.id")
  @Mapping(target = "medicineName", source = "medicine.name")
  PrescriptionItemDTO toDto(PrescriptionItem item);
}
