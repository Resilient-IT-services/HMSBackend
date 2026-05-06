package com.res.hms.inventory.mapper;

import com.res.hms.domain.InventoryStock;
import com.res.hms.domain.Item;
import com.res.hms.inventory.dto.InventoryStockDTO;
import com.res.hms.inventory.dto.ItemDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface InventoryMapper {
  ItemDTO toDto(Item item);
  Item toEntity(ItemDTO dto);

  @Mapping(target = "itemName", ignore = true) // Set manually or via service
  InventoryStockDTO toDto(InventoryStock stock);
}
