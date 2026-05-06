package com.res.hms.pharmacy.mapper;

import com.res.hms.domain.PharmacyStock;
import com.res.hms.pharmacy.dto.PharmacyStockDTO;
import java.time.LocalDate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PharmacyStockMapper {
  @Mapping(target = "medicineId", source = "stock.medicine.id")
  @Mapping(target = "medicineName", source = "stock.medicine.name")
  @Mapping(target = "genericName", source = "stock.medicine.genericName")
  @Mapping(target = "composition", source = "stock.medicine.composition")
  @Mapping(target = "totalQuantity", source = "totalQuantity")
  @Mapping(target = "price", source = "stock.sellingPrice")
  PharmacyStockDTO toDto(PharmacyStock stock, Long totalQuantity);

  default PharmacyStockDTO toDto(PharmacyStock stock) {
    return toDto(stock, null);
  }
}
