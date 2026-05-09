$srcBase = "C:\Code\HMS\HMSBackend\src"

# 1. Fix common -> core globally
Get-ChildItem -Path $srcBase -Filter *.java -Recurse | ForEach-Object {
    $content = Get-Content $_.FullName
    $content = $content -replace 'com\.res\.hms\.common', 'com.res.hms.core'
    
    # Also fix some specific broken imports I noticed in previous logs
    $content = $content -replace 'import dto\.', 'import com.res.hms.clinical.application.appointment.dto.'
    
    $content | Set-Content $_.FullName
}

# 2. Re-run global import fixer to align with current class map
& powershell.exe -NoProfile -ExecutionPolicy Bypass -File C:\Code\HMS\HMSBackend\global_import_fixer.ps1
