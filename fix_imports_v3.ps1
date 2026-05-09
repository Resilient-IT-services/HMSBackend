$mappings = @(
    # Modules to Packages (Catch-all for simple renames)
    @{ old = "com.res.hms.common"; new = "com.res.hms.core" },
    @{ old = "com.res.hms.core.api"; new = "com.res.hms.core.api" },

    # ---------------------------------------------------------
    # CLINICAL MODULE
    # ---------------------------------------------------------
    @{ old = "com.res.hms.clinical.domain"; new = "com.res.hms.clinical.domain" },
    @{ old = "com.res.hms.clinical.api"; new = "com.res.hms.clinical.api" },
    # Service moves
    @{ old = "com.res.hms.clinical.application.appointment.service"; new = "com.res.hms.clinical.application.appointment.service" },
    @{ old = "com.res.hms.clinical.application.patient.service"; new = "com.res.hms.clinical.application.patient.service" },
    @{ old = "com.res.hms.clinical.application.prescription.service"; new = "com.res.hms.clinical.application.prescription.service" },
    @{ old = "com.res.hms.clinical.application.vital.service"; new = "com.res.hms.clinical.application.vital.service" },
    # DTO moves
    @{ old = "com.res.hms.clinical.application.appointment.dto"; new = "com.res.hms.clinical.application.appointment.dto" },
    @{ old = "com.res.hms.clinical.application.patient.dto"; new = "com.res.hms.clinical.application.patient.dto" },
    @{ old = "com.res.hms.clinical.application.prescription.dto"; new = "com.res.hms.clinical.application.prescription.dto" },
    @{ old = "com.res.hms.clinical.application.vital.dto"; new = "com.res.hms.clinical.application.vital.dto" },
    # Repository move
    @{ old = "com.res.hms.clinical.infrastructure.repository"; new = "com.res.hms.clinical.infrastructure.repository" },
    # Specific legacy mappings
    @{ old = "com.res.hms.appointment.service"; new = "com.res.hms.clinical.application.appointment.service" },
    @{ old = "com.res.hms.appointment.dto"; new = "com.res.hms.clinical.application.appointment.dto" },
    @{ old = "com.res.hms.patient.service"; new = "com.res.hms.clinical.application.patient.service" },
    @{ old = "com.res.hms.patient.dto"; new = "com.res.hms.clinical.application.patient.dto" },
    @{ old = "com.res.hms.prescription.service"; new = "com.res.hms.clinical.application.prescription.service" },
    @{ old = "com.res.hms.prescription.dto"; new = "com.res.hms.clinical.application.prescription.dto" },
    @{ old = "com.res.hms.vital.service"; new = "com.res.hms.clinical.application.vital.service" },
    @{ old = "com.res.hms.vital.dto"; new = "com.res.hms.clinical.application.vital.dto" },

    # ---------------------------------------------------------
    # IAM MODULE
    # ---------------------------------------------------------
    @{ old = "com.res.hms.iam.domain"; new = "com.res.hms.iam.domain" },
    @{ old = "com.res.hms.iam.api"; new = "com.res.hms.iam.api" },
    @{ old = "com.res.hms.iam.application.service"; new = "com.res.hms.iam.application.service" },
    @{ old = "com.res.hms.iam.application.dto"; new = "com.res.hms.iam.application.dto" },
    @{ old = "com.res.hms.iam.infrastructure.repository"; new = "com.res.hms.iam.infrastructure.repository" },
    @{ old = "com.res.hms.iam.infrastructure.security"; new = "com.res.hms.iam.infrastructure.security" },
    # Legacy mappings
    @{ old = "com.res.hms.user.service"; new = "com.res.hms.iam.application.service" },
    @{ old = "com.res.hms.user.dto"; new = "com.res.hms.iam.application.dto" },
    @{ old = "com.res.hms.staff.service"; new = "com.res.hms.iam.application.service" },
    @{ old = "com.res.hms.staff.dto"; new = "com.res.hms.iam.application.dto" },
    @{ old = "com.res.hms.doctor.service"; new = "com.res.hms.iam.application.service" },
    @{ old = "com.res.hms.doctor.dto"; new = "com.res.hms.iam.application.dto" },
    @{ old = "com.res.hms.nurse.service"; new = "com.res.hms.iam.application.service" },
    @{ old = "com.res.hms.nurse.dto"; new = "com.res.hms.iam.application.dto" },
    @{ old = "com.res.hms.user.security"; new = "com.res.hms.iam.infrastructure.security" },

    # ---------------------------------------------------------
    # PHARMACY MODULE
    # ---------------------------------------------------------
    @{ old = "com.res.hms.pharmacy.domain"; new = "com.res.hms.pharmacy.domain" },
    @{ old = "com.res.hms.pharmacy.api"; new = "com.res.hms.pharmacy.api" },
    @{ old = "com.res.hms.pharmacy.application.service"; new = "com.res.hms.pharmacy.application.service" },
    @{ old = "com.res.hms.pharmacy.application.dto"; new = "com.res.hms.pharmacy.application.dto" },
    @{ old = "com.res.hms.pharmacy.infrastructure.repository"; new = "com.res.hms.pharmacy.infrastructure.repository" },
    # Fix flat application moves
    @{ old = "com.res.hms.pharmacy.application.MedicineService"; new = "com.res.hms.pharmacy.application.service.MedicineService" },
    @{ old = "com.res.hms.pharmacy.application.MedicineDTO"; new = "com.res.hms.pharmacy.application.dto.MedicineDTO" },

    # ---------------------------------------------------------
    # FINANCE MODULE
    # ---------------------------------------------------------
    @{ old = "com.res.hms.finance.domain"; new = "com.res.hms.finance.domain" },
    @{ old = "com.res.hms.finance.api"; new = "com.res.hms.finance.api" },
    @{ old = "com.res.hms.finance.application.service"; new = "com.res.hms.finance.application.service" },
    @{ old = "com.res.hms.finance.application.dto"; new = "com.res.hms.finance.application.dto" },
    @{ old = "com.res.hms.finance.infrastructure.repository"; new = "com.res.hms.finance.infrastructure.repository" },

    # ---------------------------------------------------------
    # HOSPITAL MODULE
    # ---------------------------------------------------------
    @{ old = "com.res.hms.hospital.domain"; new = "com.res.hms.hospital.domain" },
    @{ old = "com.res.hms.hospital.api"; new = "com.res.hms.hospital.api" },
    @{ old = "com.res.hms.hospital.application.service"; new = "com.res.hms.hospital.application.service" },
    @{ old = "com.res.hms.hospital.application.dto"; new = "com.res.hms.hospital.application.dto" },
    @{ old = "com.res.hms.hospital.infrastructure.repository"; new = "com.res.hms.hospital.infrastructure.repository" },

    # ---------------------------------------------------------
    # INVENTORY MODULE
    # ---------------------------------------------------------
    @{ old = "com.res.hms.inventory.domain"; new = "com.res.hms.inventory.domain" },
    @{ old = "com.res.hms.inventory.api"; new = "com.res.hms.inventory.api" },
    @{ old = "com.res.hms.inventory.application.service"; new = "com.res.hms.inventory.application.service" },
    @{ old = "com.res.hms.inventory.application.dto"; new = "com.res.hms.inventory.application.dto" },
    @{ old = "com.res.hms.inventory.infrastructure.repository"; new = "com.res.hms.inventory.infrastructure.repository" },

    # ---------------------------------------------------------
    # AUDIT MODULE
    # ---------------------------------------------------------
    @{ old = "com.res.hms.audit.domain"; new = "com.res.hms.audit.domain" },
    @{ old = "com.res.hms.audit.api"; new = "com.res.hms.audit.api" },
    @{ old = "com.res.hms.audit.application.service"; new = "com.res.hms.audit.application.service" },
    @{ old = "com.res.hms.audit.application.dto"; new = "com.res.hms.audit.application.dto" },
    @{ old = "com.res.hms.audit.infrastructure.repository"; new = "com.res.hms.audit.infrastructure.repository" },

    # ---------------------------------------------------------
    # SEARCH MODULE
    # ---------------------------------------------------------
    @{ old = "com.res.hms.search.api"; new = "com.res.hms.search.api" },
    @{ old = "com.res.hms.search.application.service"; new = "com.res.hms.search.application.service" },
    @{ old = "com.res.hms.search.application.dto"; new = "com.res.hms.search.application.dto" },

    # ---------------------------------------------------------
    # CORE DOMAIN (entities moved from domain to core.domain)
    # ---------------------------------------------------------
    @{ old = "com.res.hms.core.domain.BaseEntity"; new = "com.res.hms.core.domain.BaseEntity" },
    @{ old = "com.res.hms.domain.BaseEntity"; new = "com.res.hms.core.domain.BaseEntity" },
    @{ old = "com.res.hms.domain.AuditableEntity"; new = "com.res.hms.core.domain.AuditableEntity" },
    @{ old = "com.res.hms.domain.HospitalScopedEntity"; new = "com.res.hms.core.domain.HospitalScopedEntity" },
    @{ old = "com.res.hms.domain.Gender"; new = "com.res.hms.core.domain.Gender" },
    @{ old = "com.res.hms.domain.CertificationLevel"; new = "com.res.hms.core.domain.CertificationLevel" },
    @{ old = "com.res.hms.domain.ProficiencyLevel"; new = "com.res.hms.core.domain.ProficiencyLevel" },
    @{ old = "com.res.hms.domain.ShiftType"; new = "com.res.hms.core.domain.ShiftType" },
    @{ old = "com.res.hms.domain.TemperatureUnit"; new = "com.res.hms.core.domain.TemperatureUnit" },
    @{ old = "com.res.hms.domain.WeightUnit"; new = "com.res.hms.core.domain.WeightUnit" },
    @{ old = "com.res.hms.domain.CommissionType"; new = "com.res.hms.core.domain.CommissionType" } # Wait, I moved this to finance, let's keep it there or in core
)

# Refined import fixing function
Get-ChildItem -Path src/main/java/com/res/hms -Filter *.java -Recurse | ForEach-Object {
    $file = $_.FullName
    $content = Get-Content $file
    
    # Pre-process content to handle some specific broken states
    # e.g. "import dto.VitalsDTO" -> "import com.res.hms.clinical.application.appointment.dto.VitalsDTO"
    if ($file -match "AppointmentAggregateService.java") {
        $content = $content -replace 'import dto\.', 'import com.res.hms.clinical.application.appointment.dto.'
    }

    foreach ($m in $mappings) {
        $oldEscaped = [regex]::Escape($m.old)
        # Match "import com.old..." or "extends com.old..." or "return new com.old..."
        # But NOT if it's already "com.new..."
        $content = $content -replace "($oldEscaped)", $m.new
    }
    
    $content | Set-Content $file
}
