package com.res.hms.doctor.service;

import com.res.hms.common.exception.ApiException;
import com.res.hms.doctor.dto.ScheduleDTO;
import com.res.hms.user.dto.UserDetailsDTO;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class DoctorScheduleValidator {

  public void validateOverlaps(UserDetailsDTO dto) {
    if (dto.getMemberships() == null) {
      return;
    }

    List<ScheduleDTO> allSchedules = new ArrayList<>();
    for (UserDetailsDTO.HospitalMembershipDTO membership : dto.getMemberships()) {
      if (membership.getRoleAssignments() == null) continue;
      for (UserDetailsDTO.RoleAssignmentDTO ra : membership.getRoleAssignments()) {
        if (ra.getSchedules() != null && "DOCTOR".equals(ra.getRoleCode())) {
          allSchedules.addAll(ra.getSchedules());
        }
      }
    }

    if (allSchedules.isEmpty()) {
      return;
    }

    Map<Integer, List<ScheduleDTO>> schedulesByDay =
        allSchedules.stream().collect(Collectors.groupingBy(ScheduleDTO::getDayOfWeek));

    for (Map.Entry<Integer, List<ScheduleDTO>> entry : schedulesByDay.entrySet()) {
      List<ScheduleDTO> dailySchedules = entry.getValue();
      for (int i = 0; i < dailySchedules.size(); i++) {
        for (int j = i + 1; j < dailySchedules.size(); j++) {
          ScheduleDTO s1 = dailySchedules.get(i);
          ScheduleDTO s2 = dailySchedules.get(j);

          if (isOverlapping(s1, s2)) {
            throw new ApiException(
                "SCHEDULE_OVERLAP",
                String.format(
                    "Doctor has overlapping schedules on %s: %s-%s and %s-%s",
                    entry.getKey(), s1.getStartTime(), s1.getEndTime(), s2.getStartTime(), s2.getEndTime()),
                HttpStatus.CONFLICT);
          }
        }
      }
    }
  }

  private boolean isOverlapping(ScheduleDTO s1, ScheduleDTO s2) {
    return s1.getStartTime().isBefore(s2.getEndTime()) && s2.getStartTime().isBefore(s1.getEndTime());
  }
}
