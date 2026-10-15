param(
    [string]$Tag,
    [string]$Message,
    [switch]$SyncRelease,
    [switch]$PushRemote,
    [switch]$NonInteractive
)

$ErrorActionPreference = "Stop"

Write-Host "=====================================================" -ForegroundColor Cyan
Write-Host "   DECKAPP - GENERADOR DE CHECKPOINT Y RESPALDO GIT   " -ForegroundColor Cyan
Write-Host "=====================================================" -ForegroundColor Cyan

# 1. Verificar estado de Git
$gitStatus = git status --porcelain
if ($gitStatus) {
    Write-Host "[!] Tienes cambios sin commitear en el repositorio:" -ForegroundColor Yellow
    git status -s
    if ($NonInteractive) {
        throw "El arbol de trabajo no esta limpio. Realiza commit antes de crear el checkpoint."
    }
    $commitResp = Read-Host "Deseas commitear estos cambios ahora? (s/n)"
    if ($commitResp -match "^[sS]") {
        $cMsg = Read-Host "Mensaje del commit"
        if (-not $cMsg) { $cMsg = "chore: checkpoint snapshot $(Get-Date -Format 'yyyy-MM-dd HH:mm')" }
        git add -A
        git commit -m "$cMsg"
    } else {
        Write-Host "Operacion cancelada. El arbol de trabajo debe estar limpio." -ForegroundColor Red
        exit 1
    }
}

# 2. Obtener Nombre de Tag y Mensaje
if (-not $Tag) {
    if ($NonInteractive) { throw "Debes especificar -Tag en modo no interactivo." }
    $Tag = Read-Host "Nombre del Tag/Version (ej. sprint-58 o v0.1.2)"
}
if (-not $Tag) {
    Write-Host "Error: El nombre del tag no puede estar vacio." -ForegroundColor Red
    exit 1
}

# Comprobar si el tag ya existe
$tagExists = git tag -l $Tag
if ($tagExists) {
    Write-Host "Error: El tag '$Tag' ya existe en Git." -ForegroundColor Red
    exit 1
}

if (-not $Message) {
    if ($NonInteractive) { $Message = "Punto de control $Tag" }
    else {
        $Message = Read-Host "Descripcion del Checkpoint (ej. Sincronizacion Sprint 58 Atlas VTT)"
        if (-not $Message) { $Message = "Punto de control $Tag" }
    }
}

# 3. Crear el Tag anotado
Write-Host "[*] Creando Tag anotado: $Tag..." -ForegroundColor Green
git tag -a "$Tag" -m "$Message"

# 4. Crear Respaldo Local .bundle
$backupDir = Join-Path $PSScriptRoot "..\.backups"
if (-not (Test-Path $backupDir)) {
    New-Item -ItemType Directory -Path $backupDir -Force | Out-Null
}

$dateStr = Get-Date -Format "yyyyMMdd_HHmmss"
$invalidChars = [System.IO.Path]::GetInvalidFileNameChars()
$safeTag = -join ($Tag.ToCharArray() | ForEach-Object { if ($invalidChars -contains $_) { '_' } else { $_ } })
$bundlePath = Join-Path $backupDir "bundle_${safeTag}_${dateStr}.bundle"

Write-Host "[*] Generando copia de seguridad local (bundle completo)..." -ForegroundColor Green
git bundle create "$bundlePath" --all

# Verificar el bundle
Write-Host "[*] Verificando integridad del bundle local..." -ForegroundColor Green
git bundle verify "$bundlePath"
Write-Host "    -> Bundle validado exitosamente en: $bundlePath" -ForegroundColor DarkGreen

# 5. Sincronizar rama release (Opcional)
if (-not $SyncRelease -and -not $NonInteractive) {
    $resp = Read-Host "Deseas promover este checkpoint a la rama estable 'release'? (s/n)"
    if ($resp -match "^[sS]") { $SyncRelease = $true }
}

if ($SyncRelease) {
    $currentBranch = (git branch --show-current).Trim()
    Write-Host "[*] Actualizando rama 'release' con el checkpoint '$Tag'..." -ForegroundColor Green
    
    # Crear rama release si no existe
    $releaseExists = git branch --list "release"
    if (-not $releaseExists) {
        git branch release
    }
    
    git checkout release
    git merge "$Tag" --ff-only
    git checkout $currentBranch
    Write-Host "    -> Rama 'release' actualizada con exito." -ForegroundColor DarkGreen
}

# 6. Sincronizar con GitHub (Opcional)
if (-not $PushRemote -and -not $NonInteractive) {
    $resp = Read-Host "Deseas subir el tag '$Tag' y ramas a GitHub ahora? (s/n)"
    if ($resp -match "^[sS]") { $PushRemote = $true }
}

if ($PushRemote) {
    Write-Host "[*] Subiendo tags y ramas a GitHub de forma segura..." -ForegroundColor Green
    git push origin "$Tag"
    git push origin master
    $releaseExists = git branch --list "release"
    if ($releaseExists) {
        git push origin release
    }
    Write-Host "    -> Sincronizacion con GitHub completada." -ForegroundColor DarkGreen
}

Write-Host ""
Write-Host "=====================================================" -ForegroundColor Green
Write-Host " [OK] CHECKPOINT '$Tag' CREADO CON EXITO!" -ForegroundColor Green
Write-Host "  - Tag Git: $Tag" -ForegroundColor White
Write-Host "  - Respaldo local offline: $bundlePath" -ForegroundColor White
Write-Host "=====================================================" -ForegroundColor Green
