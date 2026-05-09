$roots = @("C:\Code\HMS\HMSBackend\src\main\java", "C:\Code\HMS\HMSBackend\src\test\java")

foreach ($srcBase in $roots) {
    if (Test-Path $srcBase) {
        Get-ChildItem -Path $srcBase -Filter *.java -Recurse | ForEach-Object {
            $filePath = $_.FullName
            $dirPath = $_.DirectoryName
            
            # Calculate package name from directory path
            $package = $dirPath.Replace($srcBase, "").TrimStart("\").Replace("\", ".")
            
            if ($package) {
                $content = Get-Content $filePath
                $newContent = @()
                $packageFixed = $false
                
                foreach ($line in $content) {
                    if ($line -match "^package\s+.*;") {
                        $newContent += "package $package;"
                        $packageFixed = $true
                    } else {
                        $newContent += $line
                    }
                }
                
                if (-not $packageFixed) {
                    $newContent = @("package $package;") + $newContent
                }
                
                $newContent | Set-Content $filePath
            }
        }
    }
}
