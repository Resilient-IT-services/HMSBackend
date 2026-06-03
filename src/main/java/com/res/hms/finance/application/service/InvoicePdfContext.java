package com.res.hms.finance.application.service;

import java.util.UUID;

public record InvoicePdfContext(
    String hospitalName,
    String gstinNumber,
    String receptionistName,
    String patientName,
    String patientCode,
    String patientPhone,
    String consultingDoctor,
    boolean isWalkIn,
    UUID visitId) {}
