package com.res.hms.clinical.application.appointment.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AppointmentSlotService {

  public static final int SLOT_DURATION_MINUTES = 15;

  public List<LocalTime> generateSlots(LocalTime start, LocalTime end) {
    List<LocalTime> slots = new ArrayList<>();
    if (start == null || end == null || !start.isBefore(end)) {
      return slots;
    }

    LocalTime slot = start;
    while (slot.isBefore(end)) {
      slots.add(slot);
      slot = slot.plusMinutes(SLOT_DURATION_MINUTES);
    }
    return slots;
  }

  public boolean isPastSlot(LocalDate date, LocalTime slot) {
    LocalDate today = LocalDate.now();
    LocalTime now = LocalTime.now();
    return date.isBefore(today) || (date.equals(today) && !slot.isAfter(now));
  }

  public boolean isAlignedToSlot(LocalTime time) {
    return time != null
        && time.getMinute() % SLOT_DURATION_MINUTES == 0
        && time.getSecond() == 0
        && time.getNano() == 0;
  }
}
