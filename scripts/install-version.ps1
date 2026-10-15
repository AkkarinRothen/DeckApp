<#
.SYNOPSIS
    Instalador hibrido de versiones de DeckApp: instala directamente en tu dispositivo
    Android mediante ADB (si esta conectado) o abre el Explorador de Windows para copiarlo.
.DESCRIPTION
    1. Lista los APKs historicos de la boveda .backups/apks/ y el ultimo build local.
    2. Permite seleccionar que version instalar.
    3. Detecta dispositivos o emuladores conectados con ADB.
    4. Si hay dispositivo: ejecuta adb install -r -d.
    5. Si no hay dispositivo: abre la carpeta en Windows Explorer.
.EXAMPLE
    .\scripts\install-version.ps1
    .\scripts\install-version.ps1 -Latest
#>
param(
    [string]$ApkPath,
    [switch]$Latest
)

$ErrorActionPreference = "Stop"

Write-Host "=====================================================" -ForegroundColor Cyan
Write-Host "     DECKAPP - INSTALADOR HIBRIDO DE VERSIONES       " -ForegroundColor Cyan
Write-Host "=====================================================" -ForegroundColor Cyan

$projectRoot = (Resolve-Path "$PSScriptRoot\..").Path
$vaultDir = Join-Path $projectRoot ".backups\apks"
$latestDebugApk = Join-Path $projectRoot "app\build\outputs\apk\debug\app-debug.apk"

# 1. Recolectar APKs disponibles
$candidates = @()

if (Test-Path $latestDebugApk) {
    $candidates += Get-Item $latestDebugApk
}

if (Test-Path $vaultDir) {
    $vaultApks = Get-ChildItem -Path $vaultDir -Filter "*.apk" | Sort-Object LastWriteTime -Descending
    $candidates += $vaultApks
}

if ($candidates.Count -eq 0) {
    Write-Host "No se encontraron archivos APK compilados ni archivados." -ForegroundColor Yellow
    Write-Host "Puedes generar uno con: .\gradlew :app:assembleDebug o .\scripts\checkpoint.ps1 -BuildApk" -ForegroundColor Gray
    exit 0
}

# 2. Seleccionar APK
$selectedApk = $null

if ($ApkPath) {
    if (Test-Path $ApkPath) {
        $selectedApk = (Resolve-Path $ApkPath).Path
    } else {
        Write-Host "Error: El archivo APK especificado no existe: $ApkPath" -ForegroundColor Red
        exit 1
    }
} elseif ($Latest) {
    $selectedApk = $candidates[0].FullName
} else {
    Write-Host "APKs disponibles para instalar:" -ForegroundColor Yellow
    for ($i = 0; $i -lt $candidates.Count; $i++) {
        $apk = $candidates[$i]
        $sizeMB = [math]::Round($apk.Length / 1MB, 2)
        $tagLabel = if ($apk.FullName -eq $latestDebugApk) { "[ULTIMO BUILD LOCAL]" } else { "[BOVEDA HISTORICA]" }
        Write-Host "  [$($i+1)] $($apk.Name) - $sizeMB MB $tagLabel ($($apk.LastWriteTime))" -ForegroundColor White
    }
    
    Write-Host ""
    $selection = Read-Host "Ingresa el numero del APK que deseas instalar (1-$($candidates.Count))"
    $idx = [int]$selection - 1
    if ($idx -ge 0 -and $idx -lt $candidates.Count) {
        $selectedApk = $candidates[$idx].FullName
    } else {
        Write-Host "Seleccion invalida." -ForegroundColor Red
        exit 1
    }
}

Write-Host "`n[*] APK seleccionado: $selectedApk" -ForegroundColor Green

# 3. Localizar ejecutable ADB
$adbPath = $null
if (Get-Command "adb" -ErrorAction SilentlyContinue) {
    $adbPath = "adb"
} else {
    $sdkAdb = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
    if (Test-Path $sdkAdb) {
        $adbPath = $sdkAdb
    }
}

# 4. Verificar dispositivos conectados con ADB
$deviceConnected = $false
$targetDevice = $null

if ($adbPath) {
    try {
        $devicesOutput = & $adbPath devices
        $lines = $devicesOutput -split "`n" | Where-Object { $_ -match "\tdevice" }
        if ($lines -and $lines.Count -gt 0) {
            $deviceConnected = $true
            $targetDevice = ($lines[0] -split "`t")[0].Trim()
        }
    } catch {
        # ADB fallo o no responde
    }
}

# 5. Ejecutar instalacion o fallback a explorador
if ($deviceConnected -and $targetDevice) {
    Write-Host "[*] Dispositivo Android detectado via ADB: $targetDevice" -ForegroundColor Green
    Write-Host "[*] Instalando APK (manteniendo datos existentes)..." -ForegroundColor Green
    
    $installOutput = & $adbPath -s $targetDevice install -r -d "$selectedApk"
    Write-Host $installOutput
    
    if ($installOutput -match "Success") {
        Write-Host "`n=====================================================" -ForegroundColor Green
        Write-Host " [OK] APLICACION INSTALADA EXITOSAMENTE EN TU DISPOSITIVO!" -ForegroundColor Green
        Write-Host "=====================================================" -ForegroundColor Green
    } else {
        Write-Host "`n[!] ADB no pudo completar la instalacion automatica." -ForegroundColor Yellow
        Write-Host "Abriendo Explorador de archivos para instalacion manual..." -ForegroundColor Gray
        explorer.exe /select,"$selectedApk"
    }
} else {
    Write-Host "`n[i] No se detecto ningun dispositivo o emulador conectado por USB/WiFi con ADB activado." -ForegroundColor Yellow
    Write-Host "[*] Abriendo la carpeta del APK en el Explorador de Windows..." -ForegroundColor Green
    Write-Host "    -> Puedes transferir el APK a tu telefono por cable USB, Google Drive o WhatsApp/Telegram." -ForegroundColor Gray
    
    explorer.exe /select,"$selectedApk"
    
    Write-Host "`n=====================================================" -ForegroundColor Green
    Write-Host " [OK] ARCHIVO LISTO EN EL EXPLORADOR:" -ForegroundColor Green
    Write-Host "  $selectedApk" -ForegroundColor White
    Write-Host "=====================================================" -ForegroundColor Green
}
