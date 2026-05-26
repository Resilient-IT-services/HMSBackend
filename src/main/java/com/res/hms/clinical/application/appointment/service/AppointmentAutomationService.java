package com.res.hms.clinical.application.appointment.service;

import com.res.hms.clinical.domain.Appointment;
import com.res.hms.clinical.domain.AppointmentStatus;
import com.res.hms.clinical.infrastructure.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentAutomationService {

    private final AppointmentRepository appointmentRepository;

    /**
     * Automatically cancels appointments that were not completed by the end of their scheduled day.
     * Runs daily at 1:00 AM.
     */
    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void autoCancelOverdueAppointments() {
        log.info("Starting auto-cancellation of overdue appointments...");

        try {
            // Threshold: Start of today (00:00:00). 
            // Any appointment before this that is not terminal should be cancelled.
            Instant threshold = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC);
            
            List<Appointment> overdue = appointmentRepository.findOverdueIncompleteAppointments(threshold);
            
            if (overdue.isEmpty()) {
                log.info("No overdue appointments found for cancellation.");
                return;
            }

            log.info("Found {} overdue appointments. Updating status to CANCELLED.", overdue.size());

            overdue.forEach(appointment -> {
                appointment.setStatus(AppointmentStatus.CANCELLED);
                String systemNote = "[System] Automatically cancelled due to inactivity/end of day.";
                appointment.setNotes(
                    (appointment.getNotes() != null && !appointment.getNotes().isEmpty() 
                        ? appointment.getNotes() + "\n" 
                        : "") + systemNote
                );
            });

            appointmentRepository.saveAll(overdue);
            log.info("Successfully cancelled {} overdue appointments.", overdue.size());
        } catch (Exception e) {
            log.error("Error occurred during auto-cancellation of appointments: ", e);
            // We don't rethrow here to prevent scheduler from dying, 
            // but Transactional will rollback if an exception reaches its boundary.
            throw e; 
        }
    }
}
