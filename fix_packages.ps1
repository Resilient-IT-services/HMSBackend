$base = "src/main/java/com/res/hms"

function Fix-Package($dir, $newPkg) {
    if (Test-Path "$base/$dir") {
        Get-ChildItem -Path "$base/$dir" -Filter *.java -Recurse | ForEach-Object {
            $content = Get-Content $_.FullName
            $content = $content -replace '^package .*$', "package $newPkg;"
            $content | Set-Content $_.FullName
        }
    }
}

# Core
Fix-Package "core/api" "com.res.hms.core.api"
Fix-Package "core/domain" "com.res.hms.core.domain"
Fix-Package "core/exception" "com.res.hms.core.exception"
Fix-Package "core/tenant" "com.res.hms.core.tenant"
Fix-Package "core/util" "com.res.hms.core.util"
Fix-Package "core/validation" "com.res.hms.core.validation"
Fix-Package "core/repository" "com.res.hms.core.repository"

# IAM
Fix-Package "iam/api" "com.res.hms.iam.api"
Fix-Package "iam/domain" "com.res.hms.iam.domain"
Fix-Package "iam/application" "com.res.hms.iam.application"
Fix-Package "iam/infrastructure/repository" "com.res.hms.iam.infrastructure.repository"
Fix-Package "iam/infrastructure/security" "com.res.hms.iam.infrastructure.security"

# Clinical
Fix-Package "clinical/api" "com.res.hms.clinical.api"
Fix-Package "clinical/domain" "com.res.hms.clinical.domain"
Fix-Package "clinical/application/appointment" "com.res.hms.clinical.application.appointment"
Fix-Package "clinical/application/patient" "com.res.hms.clinical.application.patient"
Fix-Package "clinical/application/prescription" "com.res.hms.clinical.application.prescription"
Fix-Package "clinical/application/vital" "com.res.hms.clinical.application.vital"
Fix-Package "clinical/infrastructure/repository" "com.res.hms.clinical.infrastructure.repository"

# Pharmacy
Fix-Package "pharmacy/api" "com.res.hms.pharmacy.api"
Fix-Package "pharmacy/domain" "com.res.hms.pharmacy.domain"
Fix-Package "pharmacy/application" "com.res.hms.pharmacy.application"
Fix-Package "pharmacy/infrastructure/repository" "com.res.hms.pharmacy.infrastructure.repository"

# Finance
Fix-Package "finance/api" "com.res.hms.finance.api"
Fix-Package "finance/domain" "com.res.hms.finance.domain"
Fix-Package "finance/application" "com.res.hms.finance.application"
Fix-Package "finance/infrastructure/repository" "com.res.hms.finance.infrastructure.repository"

# Inventory
Fix-Package "inventory/api" "com.res.hms.inventory.api"
Fix-Package "inventory/domain" "com.res.hms.inventory.domain"
Fix-Package "inventory/application" "com.res.hms.inventory.application"
Fix-Package "inventory/infrastructure/repository" "com.res.hms.inventory.infrastructure.repository"

# Hospital
Fix-Package "hospital/api" "com.res.hms.hospital.api"
Fix-Package "hospital/domain" "com.res.hms.hospital.domain"
Fix-Package "hospital/application" "com.res.hms.hospital.application"
Fix-Package "hospital/infrastructure/repository" "com.res.hms.hospital.infrastructure.repository"

# Audit
Fix-Package "audit/api" "com.res.hms.audit.api"
Fix-Package "audit/domain" "com.res.hms.audit.domain"
Fix-Package "audit/application" "com.res.hms.audit.application"
Fix-Package "audit/infrastructure/repository" "com.res.hms.audit.infrastructure.repository"

# Search
Fix-Package "search/api" "com.res.hms.search.api"
Fix-Package "search/application" "com.res.hms.search.application"

# Report (if any left)
Fix-Package "report/api" "com.res.hms.report.api"
Fix-Package "report/application" "com.res.hms.report.application"
