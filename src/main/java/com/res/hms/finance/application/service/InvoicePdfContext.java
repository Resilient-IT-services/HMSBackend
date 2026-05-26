package com.res.hms.finance.application.service;

public record InvoicePdfContext(
    String hospitalName,
    String gstinNumber,
    String receptionistName,
    String patientName,
    String consultingDoctor) {}
