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


# ==========================================
# 3. UPDATE LOCAL SERVER PROPERTIES AUTOMATICALLY
# ==========================================
if (Test-Path $ServerProps) {
    Write-Host "Updating server.properties configuration..." -ForegroundColor Yellow
    
    # Format the local file URI path to point directly to the built zip file
    $RawPath = $TargetZipPath.Replace("\", "/")
    $LocalPackUrl = "file:///$RawPath"
    $PromptJson = '{"text":"Loading local FrogPixel Skyblock Dev Assets...","color":"green"}'
    
    # Static UUID ensuring a seamless workspace state across clone branches
    $StaticPackId = "7261ba74-f049-32f4-9d0a-3c494f8b6e1d"

    # Read existing properties file contents
    $Content = Get-Content $ServerProps
    
    # Clean up or update the target configuration values dynamically
    $PropertiesToUpdate = @{
        "resource-pack-sha1="    = "resource-pack-sha1=$FileHash"
        "resource-pack="         = "resource-pack=$LocalPackUrl"
        "require-resource-pack=" = "require-resource-pack=true"
        "resource-pack-prompt="   = "resource-pack-prompt=$PromptJson"
        "resource-pack-id="       = "resource-pack-id=$StaticPackId"
    }

    foreach ($Key in $PropertiesToUpdate.Keys) {
        if ($Content -match [regex]::Escape($Key)) {
            $Content = $Content -replace "$([regex]::Escape($Key)).*", $PropertiesToUpdate[$Key]
        } else {
            $Content += $PropertiesToUpdate[$Key]
        }
    }
    
    Set-Content -Path $ServerProps -Value $Content
    Write-Host "server.properties successfully synchronized with Static UUID!" -ForegroundColor Green
} else {
    Write-Warning "server.properties not found at $ServerProps. Skipping file hash injection."
}

Write-Host "Deployment complete! In-game, type /reload to refresh changes." -ForegroundColor Green