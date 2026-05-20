package com.res.hms.config;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

  @Override
  public void addFormatters(FormatterRegistry registry) {
    registry.addConverter(new StringToLocalDateConverter());
  }

  /**
   * Custom converter to handle various date formats, including ISO-8601 date-time strings (e.g.,
   * "2026-05-20T18:30:00.000Z") when a LocalDate is expected.
   */
  public static class StringToLocalDateConverter implements Converter<String, LocalDate> {
    @Override
    public LocalDate convert(String source) {
      if (source == null || source.isBlank()) {
        return null;
      }

      source = source.trim();

      try {
        // 1. Try standard ISO Date (yyyy-MM-dd)
        return LocalDate.parse(source, DateTimeFormatter.ISO_LOCAL_DATE);
      } catch (Exception e) {
        try {
          // 2. Try parsing as ISO Date Time (with offset/zone)
          // and convert to system default timezone before extracting the date part.
          // This ensures that "2026-05-19T18:30:00Z" becomes "2026-05-20" in IST (+05:30).
          return ZonedDateTime.parse(source)
              .withZoneSameInstant(java.time.ZoneId.systemDefault())
              .toLocalDate();
        } catch (Exception ex1) {
          try {
            // 3. Try parsing as ISO Local Date Time (no offset) and extracting the date part
            return java.time.LocalDateTime.parse(source).toLocalDate();
          } catch (Exception ex2) {
            throw new IllegalArgumentException(
                "Invalid date format: " + source + ". Expected yyyy-MM-dd or ISO-8601 date-time.");
          }
        }
      }
    }
  }
}
