$mappings = @(
    @{ old = "com.res.hms.common"; new = "com.res.hms.core" },
    
    # Clinical
    @{ old = "com.res.hms.domain.Appointment"; new = "com.res.hms.clinical.domain.Appointment" },
    @{ old = "com.res.hms.domain.AppointmentStatus"; new = "com.res.hms.clinical.domain.AppointmentStatus" },
    @{ old = "com.res.hms.domain.DoctorAvailabilityException"; new = "com.res.hms.clinical.domain.DoctorAvailabilityException" },
    @{ old = "com.res.hms.domain.DoctorSchedule"; new = "com.res.hms.clinical.domain.DoctorSchedule" },
    @{ old = "com.res.hms.domain.Patient"; new = "com.res.hms.clinical.domain.Patient" },
    @{ old = "com.res.hms.domain.Prescription"; new = "com.res.hms.clinical.domain.Prescription" },
    @{ old = "com.res.hms.domain.PrescriptionItem"; new = "com.res.hms.clinical.domain.PrescriptionItem" },
    @{ old = "com.res.hms.domain.SessionPeriod"; new = "com.res.hms.clinical.domain.SessionPeriod" },
    @{ old = "com.res.hms.domain.Vitals"; new = "com.res.hms.clinical.domain.Vitals" },
    @{ old = "com.res.hms.appointment.service"; new = "com.res.hms.clinical.application.appointment.service" },
    @{ old = "com.res.hms.appointment.dto"; new = "com.res.hms.clinical.application.appointment.dto" },
    @{ old = "com.res.hms.patient.service"; new = "com.res.hms.clinical.application.patient.service" },
    @{ old = "com.res.hms.patient.dto"; new = "com.res.hms.clinical.application.patient.dto" },
    @{ old = "com.res.hms.prescription.service"; new = "com.res.hms.clinical.application.prescription.service" },
    @{ old = "com.res.hms.prescription.dto"; new = "com.res.hms.clinical.application.prescription.dto" },
    @{ old = "com.res.hms.vital.service"; new = "com.res.hms.clinical.application.vital.service" },
    @{ old = "com.res.hms.vital.dto"; new = "com.res.hms.clinical.application.vital.dto" },
    @{ old = "com.res.hms.appointment.repository"; new = "com.res.hms.clinical.infrastructure.repository" },

    # IAM
    @{ old = "com.res.hms.domain.UserAccount"; new = "com.res.hms.iam.domain.UserAccount" },
    @{ old = "com.res.hms.domain.Role"; new = "com.res.hms.iam.domain.Role" },
    @{ old = "com.res.hms.domain.UserRoleAssignment"; new = "com.res.hms.iam.domain.UserRoleAssignment" },
    @{ old = "com.res.hms.domain.AdminHospitalDetails"; new = "com.res.hms.iam.domain.AdminHospitalDetails" },
    @{ old = "com.res.hms.domain.DoctorHospitalDetails"; new = "com.res.hms.iam.domain.DoctorHospitalDetails" },
    @{ old = "com.res.hms.domain.DoctorProfile"; new = "com.res.hms.iam.domain.DoctorProfile" },
    @{ old = "com.res.hms.domain.LabTechProfile"; new = "com.res.hms.iam.domain.LabTechProfile" },
    @{ old = "com.res.hms.domain.NurseHospitalDetails"; new = "com.res.hms.iam.domain.NurseHospitalDetails" },
    @{ old = "com.res.hms.domain.NurseProfile"; new = "com.res.hms.iam.domain.NurseProfile" },
    @{ old = "com.res.hms.domain.PharmacistHospitalDetails"; new = "com.res.hms.iam.domain.PharmacistHospitalDetails" },
    @{ old = "com.res.hms.domain.PharmacistProfile"; new = "com.res.hms.iam.domain.PharmacistProfile" },
    @{ old = "com.res.hms.domain.ReceptionistHospitalDetails"; new = "com.res.hms.iam.domain.ReceptionistHospitalDetails" },
    @{ old = "com.res.hms.domain.ReceptionistProfile"; new = "com.res.hms.iam.domain.ReceptionistProfile" },
    @{ old = "com.res.hms.domain.RoleCode"; new = "com.res.hms.iam.domain.RoleCode" },
    @{ old = "com.res.hms.user.service"; new = "com.res.hms.iam.application.service" },
    @{ old = "com.res.hms.user.dto"; new = "com.res.hms.iam.application.dto" },
    @{ old = "com.res.hms.staff.service"; new = "com.res.hms.iam.application.service" },
    @{ old = "com.res.hms.staff.dto"; new = "com.res.hms.iam.application.dto" },
    @{ old = "com.res.hms.doctor.service"; new = "com.res.hms.iam.application.service" },
    @{ old = "com.res.hms.doctor.dto"; new = "com.res.hms.iam.application.dto" },
    @{ old = "com.res.hms.nurse.service"; new = "com.res.hms.iam.application.service" },
    @{ old = "com.res.hms.nurse.dto"; new = "com.res.hms.iam.application.dto" },
    @{ old = "com.res.hms.repository"; new = "com.res.hms.iam.infrastructure.repository" }, # Fallback for moved repos

    # Pharmacy
    @{ old = "com.res.hms.domain.Medicine"; new = "com.res.hms.pharmacy.domain.Medicine" },
    @{ old = "com.res.hms.domain.HospitalMedicineCatalog"; new = "com.res.hms.pharmacy.domain.HospitalMedicineCatalog" },
    @{ old = "com.res.hms.domain.PharmacyStock"; new = "com.res.hms.pharmacy.domain.PharmacyStock" },
    @{ old = "com.res.hms.domain.PharmacySale"; new = "com.res.hms.pharmacy.domain.PharmacySale" },
    @{ old = "com.res.hms.domain.PharmacySaleItem"; new = "com.res.hms.pharmacy.domain.PharmacySaleItem" },
    @{ old = "com.res.hms.domain.PharmacyReturn"; new = "com.res.hms.pharmacy.domain.PharmacyReturn" },
    @{ old = "com.res.hms.pharmacy.service"; new = "com.res.hms.pharmacy.application" }, # Agent moved files here
    @{ old = "com.res.hms.pharmacy.dto"; new = "com.res.hms.pharmacy.application" },

    # Finance
    @{ old = "com.res.hms.domain.Billing"; new = "com.res.hms.finance.domain.Billing" },
    @{ old = "com.res.hms.domain.BillingItem"; new = "com.res.hms.finance.domain.BillingItem" },
    @{ old = "com.res.hms.domain.BillingSourceType"; new = "com.res.hms.finance.domain.BillingSourceType" },
    @{ old = "com.res.hms.domain.BillingStatus"; new = "com.res.hms.finance.domain.BillingStatus" },
    @{ old = "com.res.hms.domain.CommissionType"; new = "com.res.hms.finance.domain.CommissionType" },
    @{ old = "com.res.hms.billing.service"; new = "com.res.hms.finance.application" },
    @{ old = "com.res.hms.billing.dto"; new = "com.res.hms.finance.application" },
    @{ old = "com.res.hms.report.service"; new = "com.res.hms.finance.application" },
    @{ old = "com.res.hms.report.dto"; new = "com.res.hms.finance.application" },

    # Inventory
    @{ old = "com.res.hms.domain.Item"; new = "com.res.hms.inventory.domain.Item" },
    @{ old = "com.res.hms.domain.InventoryStock"; new = "com.res.hms.inventory.domain.InventoryStock" },
    @{ old = "com.res.hms.domain.InventoryTransaction"; new = "com.res.hms.inventory.domain.InventoryTransaction" },
    @{ old = "com.res.hms.domain.InventoryTransactionType"; new = "com.res.hms.inventory.domain.InventoryTransactionType" },
    @{ old = "com.res.hms.inventory.service"; new = "com.res.hms.inventory.application" },
    @{ old = "com.res.hms.inventory.dto"; new = "com.res.hms.inventory.application" },

    # Hospital
    @{ old = "com.res.hms.domain.Hospital"; new = "com.res.hms.hospital.domain.Hospital" },
    @{ old = "com.res.hms.domain.HospitalStatus"; new = "com.res.hms.hospital.domain.HospitalStatus" },
    @{ old = "com.res.hms.domain.VitalRangeConfig"; new = "com.res.hms.hospital.domain.VitalRangeConfig" },
    @{ old = "com.res.hms.domain.VitalType"; new = "com.res.hms.hospital.domain.VitalType" },
    @{ old = "com.res.hms.hospital.service"; new = "com.res.hms.hospital.application" },
    @{ old = "com.res.hms.hospital.dto"; new = "com.res.hms.hospital.application" },

    # Audit
    @{ old = "com.res.hms.domain.AuditLog"; new = "com.res.hms.audit.domain.AuditLog" },
    @{ old = "com.res.hms.domain.AuditAction"; new = "com.res.hms.audit.domain.AuditAction" },
    @{ old = "com.res.hms.audit.service"; new = "com.res.hms.audit.application" },
    @{ old = "com.res.hms.audit.dto"; new = "com.res.hms.audit.application" },

    # Search
    @{ old = "com.res.hms.search.service"; new = "com.res.hms.search.application" },
    @{ old = "com.res.hms.search.dto"; new = "com.res.hms.search.application" },

    # Core (from domain)
    @{ old = "com.res.hms.domain.BaseEntity"; new = "com.res.hms.core.domain.BaseEntity" },
    @{ old = "com.res.hms.domain.AuditableEntity"; new = "com.res.hms.core.domain.AuditableEntity" },
    @{ old = "com.res.hms.domain.HospitalScopedEntity"; new = "com.res.hms.core.domain.HospitalScopedEntity" },
    @{ old = "com.res.hms.domain.Gender"; new = "com.res.hms.core.domain.Gender" },
    @{ old = "com.res.hms.domain.CertificationLevel"; new = "com.res.hms.core.domain.CertificationLevel" },
    @{ old = "com.res.hms.domain.ProficiencyLevel"; new = "com.res.hms.core.domain.ProficiencyLevel" },
    @{ old = "com.res.hms.domain.ShiftType"; new = "com.res.hms.core.domain.ShiftType" },
    @{ old = "com.res.hms.domain.TemperatureUnit"; new = "com.res.hms.core.domain.TemperatureUnit" },
    @{ old = "com.res.hms.domain.WeightUnit"; new = "com.res.hms.core.domain.WeightUnit" },

    # Repositories specific
    @{ old = "com.res.hms.iam.infrastructure.repository.DoctorAvailabilityExceptionRepository"; new = "com.res.hms.clinical.infrastructure.repository.DoctorAvailabilityExceptionRepository" },
    @{ old = "com.res.hms.iam.infrastructure.repository.DoctorScheduleRepository"; new = "com.res.hms.clinical.infrastructure.repository.DoctorScheduleRepository" },
    @{ old = "com.res.hms.iam.infrastructure.repository.HospitalMedicineCatalogRepository"; new = "com.res.hms.pharmacy.infrastructure.repository.HospitalMedicineCatalogRepository" },
    @{ old = "com.res.hms.iam.infrastructure.repository.HospitalRepository"; new = "com.res.hms.hospital.infrastructure.repository.HospitalRepository" },
    @{ old = "com.res.hms.iam.infrastructure.repository.InventoryStockRepository"; new = "com.res.hms.inventory.infrastructure.repository.InventoryStockRepository" },
    @{ old = "com.res.hms.iam.infrastructure.repository.InventoryTransactionRepository"; new = "com.res.hms.inventory.infrastructure.repository.InventoryTransactionRepository" },
    @{ old = "com.res.hms.iam.infrastructure.repository.ItemRepository"; new = "com.res.hms.inventory.infrastructure.repository.ItemRepository" },
    @{ old = "com.res.hms.iam.infrastructure.repository.MedicineRepository"; new = "com.res.hms.pharmacy.infrastructure.repository.MedicineRepository" },
    @{ old = "com.res.hms.iam.infrastructure.repository.PharmacyReturnRepository"; new = "com.res.hms.pharmacy.infrastructure.repository.PharmacyReturnRepository" },
    @{ old = "com.res.hms.iam.infrastructure.repository.PharmacySaleItemRepository"; new = "com.res.hms.pharmacy.infrastructure.repository.PharmacySaleItemRepository" },
    @{ old = "com.res.hms.iam.infrastructure.repository.PharmacySaleRepository"; new = "com.res.hms.pharmacy.infrastructure.repository.PharmacySaleRepository" },
    @{ old = "com.res.hms.iam.infrastructure.repository.PharmacyStockRepository"; new = "com.res.hms.pharmacy.infrastructure.repository.PharmacyStockRepository" },
    @{ old = "com.res.hms.iam.infrastructure.repository.VitalRangeConfigRepository"; new = "com.res.hms.hospital.infrastructure.repository.VitalRangeConfigRepository" }
)

Get-ChildItem -Path src/main/java/com/res/hms -Filter *.java -Recurse | ForEach-Object {
    $file = $_.FullName
    $content = Get-Content $file
    foreach ($m in $mappings) {
        $content = $content -replace [regex]::Escape($m.old), $m.new
    }
    $content | Set-Content $file
}
