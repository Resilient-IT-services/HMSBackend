package com.res.hms.config;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class WebConfigTest {

  private final WebConfig.StringToLocalDateConverter converter =
      new WebConfig.StringToLocalDateConverter();

  @Test
  void testConvertSimpleDate() {
    LocalDate result = converter.convert("2026-05-20");
    assertEquals(LocalDate.of(2026, 5, 20), result, "Simple date conversion failed");
  }

  @Test
  void testConvertDateTimeWithZ() {
    LocalDate result = converter.convert("2026-05-20T18:30:00.000Z");
    // If the system default is IST (+05:30), 18:30Z becomes 00:00 on the 21st.
    // We expect the converter to shift the date accordingly.
    LocalDate expected =
        java.time.ZonedDateTime.parse("2026-05-20T18:30:00.000Z")
            .withZoneSameInstant(java.time.ZoneId.systemDefault())
            .toLocalDate();
    assertEquals(expected, result, "ISO with Z conversion failed");
  }

  @Test
  void testConvertDateTimeWithOffset() {
    LocalDate result = converter.convert("2026-05-20T18:30:00+05:30");
    assertEquals(LocalDate.of(2026, 5, 20), result, "ISO with offset conversion failed");
  }

  @Test
  void testConvertLocalDateTime() {
    LocalDate result = converter.convert("2026-05-20T10:40:36");
    assertEquals(LocalDate.of(2026, 5, 20), result, "Local date time conversion failed");
  }

  @Test
  void testConvertNullOrBlank() {
    assertAll(
        "Conversion of null or blank",
        () -> assertNull(converter.convert(null), "Null should return null"),
        () -> assertNull(converter.convert(""), "Empty string should return null"),
        () -> assertNull(converter.convert("  "), "Blank string should return null"));
  }

  @Test
  void testConvertInvalidFormat() {
    assertThrows(IllegalArgumentException.class, () -> converter.convert("not-a-date"));
  }
}
