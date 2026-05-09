$mappings = @(
    # Modules to Packages (Catch-all for simple renames)
    @{ old = "com.res.hms.common"; new = "com.res.hms.core" },
    
    # Specific common sub-packages
    @{ old = "com.res.hms.core.api"; new = "com.res.hms.core.api" },
    @{ old = "com.res.hms.core.exception"; new = "com.res.hms.core.exception" },
    @{ old = "com.res.hms.core.tenant"; new = "com.res.hms.core.tenant" },
    @{ old = "com.res.hms.core.util"; new = "com.res.hms.core.util" },
    @{ old = "com.res.hms.core.validation"; new = "com.res.hms.core.validation" },
    @{ old = "com.res.hms.core.repository"; new = "com.res.hms.core.repository" },

    # ---------------------------------------------------------
    # CLINICAL MODULE
    # ---------------------------------------------------------
    @{ old = "com.res.hms.clinical.domain"; new = "com.res.hms.clinical.domain" },
    @{ old = "com.res.hms.clinical.api"; new = "com.res.hms.clinical.api" },
    @{ old = "com.res.hms.clinical.application.appointment.service"; new = "com.res.hms.clinical.application.appointment.service" },
    @{ old = "com.res.hms.clinical.application.patient.service"; new = "com.res.hms.clinical.application.patient.service" },
    @{ old = "com.res.hms.clinical.application.prescription.service"; new = "com.res.hms.clinical.application.prescription.service" },
    @{ old = "com.res.hms.clinical.application.vital.service"; new = "com.res.hms.clinical.application.vital.service" },
    @{ old = "com.res.hms.clinical.application.appointment.dto"; new = "com.res.hms.clinical.application.appointment.dto" },
    @{ old = "com.res.hms.clinical.application.patient.dto"; new = "com.res.hms.clinical.application.patient.dto" },
    @{ old = "com.res.hms.clinical.application.prescription.dto"; new = "com.res.hms.clinical.application.prescription.dto" },
    @{ old = "com.res.hms.clinical.application.vital.dto"; new = "com.res.hms.clinical.application.vital.dto" },
    @{ old = "com.res.hms.clinical.infrastructure.repository"; new = "com.res.hms.clinical.infrastructure.repository" },
    
    # Legacy mappings for Clinical
    @{ old = "com.res.hms.appointment.service"; new = "com.res.hms.clinical.application.appointment.service" },
    @{ old = "com.res.hms.appointment.dto"; new = "com.res.hms.clinical.application.appointment.dto" },
    @{ old = "com.res.hms.patient.service"; new = "com.res.hms.clinical.application.patient.service" },
    @{ old = "com.res.hms.patient.dto"; new = "com.res.hms.clinical.application.patient.dto" },
    @{ old = "com.res.hms.prescription.service"; new = "com.res.hms.clinical.application.prescription.service" },
    @{ old = "com.res.hms.prescription.dto"; new = "com.res.hms.clinical.application.prescription.dto" },
    @{ old = "com.res.hms.vital.service"; new = "com.res.hms.clinical.application.vital.service" },
    @{ old = "com.res.hms.vital.dto"; new = "com.res.hms.clinical.application.vital.dto" },
    @{ old = "com.res.hms.appointment.repository"; new = "com.res.hms.clinical.infrastructure.repository" },

    # ---------------------------------------------------------
    # IAM MODULE
    # ---------------------------------------------------------
    @{ old = "com.res.hms.iam.domain"; new = "com.res.hms.iam.domain" },
    @{ old = "com.res.hms.iam.api"; new = "com.res.hms.iam.api" },
    @{ old = "com.res.hms.iam.application.service"; new = "com.res.hms.iam.application.service" },
    @{ old = "com.res.hms.iam.application.dto"; new = "com.res.hms.iam.application.dto" },
    @{ old = "com.res.hms.iam.infrastructure.repository"; new = "com.res.hms.iam.infrastructure.repository" },
    @{ old = "com.res.hms.iam.infrastructure.security"; new = "com.res.hms.iam.infrastructure.security" },
    
    # Legacy mappings for IAM
    @{ old = "com.res.hms.user.service"; new = "com.res.hms.iam.application.service" },
    @{ old = "com.res.hms.user.dto"; new = "com.res.hms.iam.application.dto" },
    @{ old = "com.res.hms.staff.service"; new = "com.res.hms.iam.application.service" },
    @{ old = "com.res.hms.staff.dto"; new = "com.res.hms.iam.application.dto" },
    @{ old = "com.res.hms.doctor.service"; new = "com.res.hms.iam.application.service" },
    @{ old = "com.res.hms.doctor.dto"; new = "com.res.hms.iam.application.dto" },
    @{ old = "com.res.hms.nurse.service"; new = "com.res.hms.iam.application.service" },
    @{ old = "com.res.hms.nurse.dto"; new = "com.res.hms.iam.application.dto" },
    @{ old = "com.res.hms.user.security"; new = "com.res.hms.iam.infrastructure.security" },
    @{ old = "com.res.hms.doctor.repository"; new = "com.res.hms.iam.infrastructure.repository" },
    @{ old = "com.res.hms.nurse.repository"; new = "com.res.hms.iam.infrastructure.repository" },

    # ---------------------------------------------------------
    # PHARMACY MODULE
    # ---------------------------------------------------------
    @{ old = "com.res.hms.pharmacy.domain"; new = "com.res.hms.pharmacy.domain" },
    @{ old = "com.res.hms.pharmacy.api"; new = "com.res.hms.pharmacy.api" },
    @{ old = "com.res.hms.pharmacy.application.service"; new = "com.res.hms.pharmacy.application.service" },
    @{ old = "com.res.hms.pharmacy.application.dto"; new = "com.res.hms.pharmacy.application.dto" },
    @{ old = "com.res.hms.pharmacy.infrastructure.repository"; new = "com.res.hms.pharmacy.infrastructure.repository" },
    
    # Legacy mappings for Pharmacy
    @{ old = "com.res.hms.pharmacy.service"; new = "com.res.hms.pharmacy.application.service" },
    @{ old = "com.res.hms.pharmacy.dto"; new = "com.res.hms.pharmacy.application.dto" },

    # ---------------------------------------------------------
    # FINANCE MODULE
    # ---------------------------------------------------------
    @{ old = "com.res.hms.finance.domain"; new = "com.res.hms.finance.domain" },
    @{ old = "com.res.hms.finance.api"; new = "com.res.hms.finance.api" },
    @{ old = "com.res.hms.finance.application.service"; new = "com.res.hms.finance.application.service" },
    @{ old = "com.res.hms.finance.application.dto"; new = "com.res.hms.finance.application.dto" },
    @{ old = "com.res.hms.finance.infrastructure.repository"; new = "com.res.hms.finance.infrastructure.repository" },
    
    # Legacy mappings for Finance
    @{ old = "com.res.hms.billing.service"; new = "com.res.hms.finance.application.service" },
    @{ old = "com.res.hms.billing.dto"; new = "com.res.hms.finance.application.dto" },
    @{ old = "com.res.hms.billing.repository"; new = "com.res.hms.finance.infrastructure.repository" },
    @{ old = "com.res.hms.report.service"; new = "com.res.hms.finance.application.service" },
    @{ old = "com.res.hms.report.dto"; new = "com.res.hms.finance.application.dto" },

    # ---------------------------------------------------------
    # HOSPITAL MODULE
    # ---------------------------------------------------------
    @{ old = "com.res.hms.hospital.domain"; new = "com.res.hms.hospital.domain" },
    @{ old = "com.res.hms.hospital.api"; new = "com.res.hms.hospital.api" },
    @{ old = "com.res.hms.hospital.application.service"; new = "com.res.hms.hospital.application.service" },
    @{ old = "com.res.hms.hospital.application.dto"; new = "com.res.hms.hospital.application.dto" },
    @{ old = "com.res.hms.hospital.infrastructure.repository"; new = "com.res.hms.hospital.infrastructure.repository" },
    
    # Legacy mappings for Hospital
    @{ old = "com.res.hms.hospital.service"; new = "com.res.hms.hospital.application.service" },
    @{ old = "com.res.hms.hospital.dto"; new = "com.res.hms.hospital.application.dto" },
    @{ old = "com.res.hms.hospital.repository"; new = "com.res.hms.hospital.infrastructure.repository" },

    # ---------------------------------------------------------
    # CORE DOMAIN / DOMAIN (the most common broken ones)
    # ---------------------------------------------------------
    @{ old = "com.res.hms.domain"; new = "com.res.hms.core.domain" } # Catch-all for domain entities
)

# 1. Build a refined class map from src/main/java
$srcBase = "C:\Code\HMS\HMSBackend\src\main\java"
$classMap = @{}
Get-ChildItem -Path $srcBase -Filter *.java -Recurse | ForEach-Object {
    $className = [System.IO.Path]::GetFileNameWithoutExtension($_.Name)
    $dirPath = $_.DirectoryName
    $package = $dirPath.Replace($srcBase, "").TrimStart("\").Replace("\", ".")
    $classMap[$className] = $package
}

# 2. Iterate through ALL Java files (Main and Test) and fix imports
Get-ChildItem -Path "C:\Code\HMS\HMSBackend\src" -Filter *.java -Recurse | ForEach-Object {
    $file = $_.FullName
    $content = Get-Content $file
    $changed = $false
    $newContent = @()

    foreach ($line in $content) {
        $newLine = $line
        
        # Pattern 1: Fully qualified com.res.hms.*
        if ($newLine -match "com\.res\.hms\.[a-zA-Z0-9.]+") {
            foreach ($m in $mappings) {
                if ($newLine -match [regex]::Escape($m.old)) {
                    $newLine = $newLine -replace [regex]::Escape($m.old), $m.new
                    $changed = $true
                }
            }
        }

        # Pattern 2: Explicit imports that might be missed by simple rename
        if ($newLine -match "^import\s+com\.res\.hms\.(.*)\.([^.;\s]+);") {
            $className = $matches[2]
            if ($classMap.ContainsKey($className)) {
                $correctPkg = $classMap[$className]
                $correctImport = "import $correctPkg.$className;"
                if ($newLine.Trim() -ne $correctImport) {
                    $newLine = $correctImport
                    $changed = $true
                }
            }
        }

        $newContent += $newLine
    }

    if ($changed) {
        $newContent | Set-Content $file
    }
}
