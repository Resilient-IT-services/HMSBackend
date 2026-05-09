$srcBase = "C:\Code\HMS\HMSBackend\src\main\java"

# 1. First, build a map of all class names to their NEW absolute package
$classMap = @{}
Get-ChildItem -Path $srcBase -Filter *.java -Recurse | ForEach-Object {
    $className = [System.IO.Path]::GetFileNameWithoutExtension($_.Name)
    $dirPath = $_.DirectoryName
    $package = $dirPath.Replace($srcBase, "").TrimStart("\").Replace("\", ".")
    
    # Store the mapping
    $classMap[$className] = $package
}

# 2. Iterate through all files and fix imports
Get-ChildItem -Path $srcBase -Filter *.java -Recurse | ForEach-Object {
    $filePath = $_.FullName
    $content = Get-Content $filePath
    $newContent = @()
    $changed = $false
    
    foreach ($line in $content) {
        if ($line -match "^import\s+com\.res\.hms\.(.*)\.([^.;\s]+);") {
            $fullOldPkg = $matches[1]
            $className = $matches[2]
            
            if ($classMap.ContainsKey($className)) {
                $newPkg = $classMap[$className]
                $newLine = "import $newPkg.$className;"
                if ($line.Trim() -ne $newLine) {
                    $newContent += $newLine
                    $changed = $true
                    continue
                }
            }
        }
        $newContent += $line
    }
    
    if ($changed) {
        $newContent | Set-Content $filePath
    }
}
