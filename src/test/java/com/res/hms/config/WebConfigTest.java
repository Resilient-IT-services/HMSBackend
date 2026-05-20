package com.res.hms.config;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class WebConfigTest {

  private final WebConfig.StringToLocalDateConverter converter = new WebConfig.StringToLocalDateConverter();

  @Test
  void testConvertSimpleDate() {
    LocalDate result = converter.convert("2026-05-20");
    assertEquals(LocalDate.of(2026, 5, 20), result);
  }

  @Test
  void testConvertDateTimeWithZ() {
    LocalDate result = converter.convert("2026-05-20T18:30:00.000Z");
    assertEquals(LocalDate.of(2026, 5, 20), result);
  }

  @Test
  void testConvertDateTimeWithOffset() {
    LocalDate result = converter.convert("2026-05-20T18:30:00+05:30");
    assertEquals(LocalDate.of(2026, 5, 20), result);
  }

  @Test
  void testConvertLocalDateTime() {
    LocalDate result = converter.convert("2026-05-20T10:40:36");
    assertEquals(LocalDate.of(2026, 5, 20), result);
  }

  @Test
  void testConvertNullOrBlank() {
    assertNull(converter.convert(null));
    assertNull(converter.convert(""));
    assertNull(converter.convert("  "));
  }

  @Test
  void testConvertInvalidFormat() {
    assertThrows(IllegalArgumentException.class, () -> converter.convert("not-a-date"));
  }
}
