package com.res.hms.ipd.application.mapper;

import com.res.hms.ipd.application.dto.AdmissionResponseDTO;
import com.res.hms.ipd.application.dto.BedDTO;
import com.res.hms.ipd.application.dto.BedTransferResponse;
import com.res.hms.ipd.application.dto.ProgressNoteResponse;
import com.res.hms.ipd.application.dto.WardDTO;
import com.res.hms.ipd.domain.Admission;
import com.res.hms.ipd.domain.Bed;
import com.res.hms.ipd.domain.BedTransferHistory;
import com.res.hms.ipd.domain.ProgressNote;
import com.res.hms.ipd.domain.Ward;
import org.springframework.stereotype.Component;

@Component
public class IpdMapper {

  public WardDTO toDto(Ward ward) {
    WardDTO dto = new WardDTO();
    dto.setId(ward.getId());
    dto.setWardCode(ward.getWardCode());
    dto.setWardName(ward.getWardName());
    dto.setWardType(ward.getWardType());
    dto.setTotalBeds(ward.getTotalBeds());
    dto.setActive(ward.isActive());
    dto.setRemarks(ward.getRemarks());
    return dto;
  }

  public Ward toEntity(WardDTO dto) {
    Ward ward = new Ward();
    ward.setId(dto.getId());
    ward.setWardCode(dto.getWardCode());
    ward.setWardName(dto.getWardName());
    ward.setWardType(dto.getWardType());
    ward.setTotalBeds(dto.getTotalBeds());
    ward.setActive(dto.isActive());
    ward.setRemarks(dto.getRemarks());
    return ward;
  }

  public BedDTO toDto(Bed bed) {
    BedDTO dto = new BedDTO();
    dto.setId(bed.getId());
    dto.setWardId(bed.getWardId());
    dto.setBedNumber(bed.getBedNumber());
    dto.setStatus(bed.getStatus());
    dto.setActive(bed.isActive());
    if (bed.getWard() != null) {
      dto.setWardName(bed.getWard().getWardName());
    }
    return dto;
  }

  public Bed toEntity(BedDTO dto) {
    Bed bed = new Bed();
    bed.setId(dto.getId());
    bed.setWardId(dto.getWardId());
    bed.setBedNumber(dto.getBedNumber());
    bed.setStatus(dto.getStatus());
    bed.setActive(dto.isActive());
    return bed;
  }

  public AdmissionResponseDTO toDto(Admission admission) {
    AdmissionResponseDTO dto = new AdmissionResponseDTO();
    dto.setId(admission.getId());
    dto.setAdmissionNumber(admission.getAdmissionNumber());
    dto.setPatientId(admission.getPatientId());
    if (admission.getPatient() != null) {
      dto.setPatientName(admission.getPatient().getFullName());
    }
    dto.setDoctorId(admission.getDoctorId());
    if (admission.getDoctor() != null) {
      dto.setDoctorName(admission.getDoctor().getFullName());
    }
    dto.setBedId(admission.getBedId());
    if (admission.getBed() != null) {
      dto.setBedNumber(admission.getBed().getBedNumber());
      if (admission.getBed().getWard() != null) {
        dto.setWardName(admission.getBed().getWard().getWardName());
      }
    }
    dto.setAdmissionDateTime(admission.getAdmissionDateTime());
    dto.setStatus(admission.getStatus());
    dto.setAdmissionReason(admission.getAdmissionReason());
    dto.setRemarks(admission.getRemarks());
    return dto;
  }

  public BedTransferResponse toDto(BedTransferHistory history) {
    BedTransferResponse dto = new BedTransferResponse();
    dto.setId(history.getId());
    dto.setAdmissionId(history.getAdmissionId());
    dto.setFromBedId(history.getFromBedId());
    if (history.getFromBed() != null) {
      dto.setFromBedNumber(history.getFromBed().getBedNumber());
      if (history.getFromBed().getWard() != null) {
        dto.setFromWardName(history.getFromBed().getWard().getWardName());
      }
    }
    dto.setToBedId(history.getToBedId());
    if (history.getToBed() != null) {
      dto.setToBedNumber(history.getToBed().getBedNumber());
      if (history.getToBed().getWard() != null) {
        dto.setToWardName(history.getToBed().getWard().getWardName());
      }
    }
    dto.setTransferReason(history.getTransferReason());
    dto.setRemarks(history.getRemarks());
    dto.setTransferredBy(history.getTransferredBy());
    if (history.getTransferredByUser() != null) {
      dto.setTransferredByName(history.getTransferredByUser().getFullName());
    }
    dto.setTransferredAt(history.getTransferredAt());
    return dto;
  }

  public ProgressNoteResponse toDto(ProgressNote note) {
    ProgressNoteResponse dto = new ProgressNoteResponse();
    dto.setId(note.getId());
    dto.setDoctorId(note.getDoctorId());
    if (note.getDoctor() != null) {
      dto.setDoctorName(note.getDoctor().getFullName());
    }
    dto.setNote(note.getNote());
    dto.setCreatedAt(note.getCreatedAt());
    return dto;
  }
}
