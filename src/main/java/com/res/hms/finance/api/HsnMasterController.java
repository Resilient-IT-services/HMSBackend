package com.res.hms.finance.api;

import com.res.hms.core.api.ApiResponse;
import com.res.hms.finance.application.dto.HsnMasterDTO;
import com.res.hms.finance.application.service.HsnMasterService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/finance/hsn-master")
@RequiredArgsConstructor
public class HsnMasterController {

  private final HsnMasterService hsnMasterService;

  @GetMapping
  public ApiResponse<List<HsnMasterDTO>> getAll() {
    return ApiResponse.success(hsnMasterService.getAll());
  }

  @PostMapping
  public ApiResponse<HsnMasterDTO> createOrUpdate(@RequestBody HsnMasterDTO dto) {
    return ApiResponse.success(hsnMasterService.createOrUpdate(dto));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable UUID id) {
    hsnMasterService.delete(id);
    return ApiResponse.success(null);
  }
}
