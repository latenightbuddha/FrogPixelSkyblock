# --- CONFIGURATION PATHS ---
$RepoRoot      = "$PSScriptRoot\SkyblockExtensionPacks"
$LocalServer   = "$PSScriptRoot\run"

$DataPackName  = "FrogPixelSkyblockData"
$AssetPackName = "FrogPixelSkyblockAssets"

$TargetDataDir = "$LocalServer\world\datapacks\$DataPackName"
$TargetZipPath = "$LocalServer\resourcepacks\$AssetPackName.zip"
$ServerProps   = "$LocalServer\server.properties"

Write-Host "Starting local Skyblock deployment sync using relative paths..." -ForegroundColor Cyan

# ==========================================
# 1. DEPLOY DATA PACK (Direct Folder Sync)
# ==========================================
Write-Host "Syncing Data Pack to IntelliJ run directory..." -ForegroundColor Yellow
if (Test-Path $TargetDataDir) { Remove-Item -Recurse -Force $TargetDataDir }
New-Item -ItemType Directory -Force -Path $TargetDataDir | Out-Null

# Copy files over recursively
Copy-Item -Recurse -Force "$RepoRoot\$DataPackName\*" $TargetDataDir
Write-Host "Data Pack deployed successfully." -ForegroundColor Green


# ==========================================
# 2. BUILD RESOURCE PACK & CALCULATE CHECKSUM
# ==========================================
Write-Host "Building Resource Pack ZIP file..." -ForegroundColor Yellow
$TempZip = "$env:TEMP\$AssetPackName.zip"
if (Test-Path $TempZip) { Remove-Item -Force $TempZip }

# Safely switch to the asset pack directory relatively to zip its inner contents
Set-Location "$RepoRoot\$AssetPackName"
Compress-Archive -Path .\* -DestinationPath $TempZip -Force

# Move the completed ZIP file to the local test environment's resourcepacks folder
if (-not (Test-Path "$LocalServer\resourcepacks")) { New-Item -ItemType Directory -Path "$LocalServer\resourcepacks" | Out-Null }
Move-Item -Force $TempZip $TargetZipPath

# Return back to the script root directory
Set-Location $PSScriptRoot

# Generate the modern SHA-1 hash from the newly created zip file
Write-Host "Calculating SHA-1 Checksum..." -ForegroundColor Yellow
$FileHash = (Get-FileHash -Path $TargetZipPath -Algorithm SHA1).Hash.ToLower()
Write-Host "New SHA-1 Hash: $FileHash" -ForegroundColor Magenta

# Define the deployment targets
$DefaultTarget = "$env:APPDATA\.minecraft\resourcepacks\FrogPixelSkyblockAssets.zip"
$PrismTarget   = "$env:APPDATA\PrismLauncher\instances\26.1.2\minecraft\resourcepacks\FrogPixelSkyblockAssets.zip"

# Copy to the default client path if needed
Copy-Item -Path $TargetZipPath -Destination $DefaultTarget -Force

# Copy directly into your active Prism Launcher instance
if (Test-Path (Split-Path $PrismTarget -Parent)) {
    Copy-Item -Path $TargetZipPath -Destination $PrismTarget -Force
    Write-Host "Successfully deployed asset pack directly to Prism Launcher instance!" -ForegroundColor Green
} else {
    Write-Warning "Prism Launcher instance directory not found. Asset pack not copied there."
}

# ==========================================
# 3. UPDATE LOCAL SERVER PROPERTIES AUTOMATICALLY
# ==========================================
if (Test-Path $ServerProps) {
    Write-Host "Updating server.properties configuration..." -ForegroundColor Yellow
    
    # Format the local file URI path safely using native .NET absolute URI escaping
    #$LocalPackUrl = ([System.Uri]$TargetZipPath).AbsoluteUri
    $LocalPackUrl = "file://localhost/$($TargetZipPath.Replace('\', '/'))"
    
    # Using a completely plain string text here prevents any JSON parser from running, 
    # which eliminates the MalformedJsonException entirely!
    $PlainPrompt  = "Loading local FrogPixel Skyblock Dev Assets..."
    $StaticPackId = "7261ba74-f049-32f4-9d0a-3c494f8b6e1d"

    # Explicitly force UTF-8 decoding when reading the file lines
    $Lines = Get-Content -Path $ServerProps -Encoding UTF8

    # Filter out ANY old line containing resource-pack settings to completely clear the slate
    $CleanedLines = @()
    foreach ($Line in $Lines) {
        if ($Line -notmatch "resource-pack" -and $Line -notmatch "require-resource-pack") {
            $CleanedLines += $Line
        }
    }

    # Append fresh, plain-text configurations to the end of the array
    $CleanedLines += "require-resource-pack=true"
    $CleanedLines += "resource-pack=$LocalPackUrl"
    $CleanedLines += "resource-pack-id=$StaticPackId"
    #$CleanedLines += "resource-pack-prompt=$PlainPrompt"
    $CleanedLines += "resource-pack-sha1=$FileHash"

    # Strip out empty line breaks
    $CleanedLines = $CleanedLines | Where-Object { $_ -ne "" }

    # Save file back using clean UTF8 (No BOM)
    $Utf8NoBom = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllLines($ServerProps, $CleanedLines, $Utf8NoBom)
    
    Write-Host "server.properties successfully synchronized and scrubbed!" -ForegroundColor Green
} else {
    Write-Warning "server.properties not found at $ServerProps. Skipping file hash injection."
}

Write-Host "Deployment complete! In-game, type /reload to refresh changes..." -ForegroundColor Green
