package com.res.hms;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@SpringBootTest
@ActiveProfiles("test")
public class MappingCheckTest {

  @Autowired private RequestMappingHandlerMapping handlerMapping;

  @Test
  void testMappingExists() {
    boolean found =
        handlerMapping.getHandlerMethods().keySet().stream()
            .anyMatch(
                info ->
                    info.toString().contains("/api/v1/pharmacy/stock")
                        && info.toString().contains("GET"));
    assertTrue(found, "Mapping GET /api/v1/pharmacy/stock was not found!");
  }
}
