$srcBase = "C:\Code\HMS\HMSBackend\src\main\java"

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
        
        # If no package line was found, add it (unlikely but safe)
        if (-not $packageFixed) {
            $newContent = @("package $package;") + $newContent
        }
        
        $newContent | Set-Content $filePath
    }
}
