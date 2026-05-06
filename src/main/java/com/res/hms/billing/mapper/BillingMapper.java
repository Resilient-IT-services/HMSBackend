package com.res.hms.billing.mapper;

import com.res.hms.billing.dto.BillingDTO;
import com.res.hms.billing.dto.BillingItemDTO;
import com.res.hms.domain.Billing;
import com.res.hms.domain.BillingItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BillingMapper {
  @Mapping(target = "patientId", source = "patient.id")
  @Mapping(target = "patientName", source = "patient.fullName")
  @Mapping(target = "appointmentId", source = "appointment.id")
  BillingDTO toDto(Billing billing);

  @Mapping(target = "id", ignore = true)
  BillingItemDTO toDto(BillingItem item);
}
