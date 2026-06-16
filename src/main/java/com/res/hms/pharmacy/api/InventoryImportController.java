package com.res.hms.pharmacy.api;

import com.res.hms.pharmacy.application.dto.ImportResponseDto;
import com.res.hms.pharmacy.application.service.ConsumableImportService;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/medicine-master")
@RequiredArgsConstructor
public class InventoryImportController {

  private final ConsumableImportService importService;

  @PostMapping("/import-inventory-items")
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ResponseEntity<ImportResponseDto> importInventoryItems(
      @RequestParam("file") MultipartFile file) {
    ImportResponseDto response = importService.importConsumables(file);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/consumables/template")
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public void downloadConsumableTemplate(HttpServletResponse response) throws IOException {
    byte[] template =
        com.res.hms.pharmacy.application.service.PharmacyTemplateService
            .generateConsumableTemplate();
    response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    response.setHeader(
        "Content-Disposition", "attachment; filename=Consumable_Import_Template.xlsx");
    response.getOutputStream().write(template);
  }
}
