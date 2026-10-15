<#
.SYNOPSIS
    Script de recuperación ante desastres: restaura el repositorio a partir de
    un archivo .bundle de seguridad local en .backups/
.DESCRIPTION
    Permite verificar o restaurar ramas y tags desde un backup offline si GitHub
    ha sobrescrito o eliminado commits.
.EXAMPLE
    .\scripts\restore-from-backup.ps1
#>
param(
    [string]$BundleFile
)

$ErrorActionPreference = "Stop"

Write-Host "=====================================================" -ForegroundColor Cyan
Write-Host "     DECKAPP - RESTAURACIÓN ANTE DESASTRES GIT       " -ForegroundColor Cyan
Write-Host "=====================================================" -ForegroundColor Cyan

$backupDir = Join-Path $PSScriptRoot "..\.backups"
if (-not (Test-Path $backupDir)) {
    Write-Host "No se encontró el directorio de respaldos (.backups)." -ForegroundColor Yellow
    exit 0
}

if (-not $BundleFile) {
    $bundles = Get-ChildItem -Path $backupDir -Filter "*.bundle" | Sort-Object LastWriteTime -Descending
    if (-not $bundles) {
        Write-Host "No se encontraron archivos .bundle de respaldo en $backupDir." -ForegroundColor Yellow
        exit 0
    }
    
    Write-Host "Respaldos .bundle disponibles:" -ForegroundColor Yellow
    for ($i = 0; $i -lt $bundles.Count; $i++) {
        $b = $bundles[$i]
        $sizeMB = [math]::Round($b.Length / 1MB, 2)
        Write-Host "  [$($i+1)] $($b.Name) ($sizeMB MB - $($b.LastWriteTime))" -ForegroundColor White
    }
    
    $selection = Read-Host "`nSelecciona el número del respaldo a inspeccionar/restaurar"
    $idx = [int]$selection - 1
    if ($idx -ge 0 -and $idx -lt $bundles.Count) {
        $BundleFile = $bundles[$idx].FullName
    } else {
        Write-Host "Selección inválida." -ForegroundColor Red
        exit 1
    }
}

Write-Host "`n[*] Verificando integridad del bundle: $BundleFile..." -ForegroundColor Green
git bundle verify "$BundleFile"

Write-Host "`nContenido del bundle (ramas y tags preservados):" -ForegroundColor Cyan
git bundle list-heads "$BundleFile"

$action = Read-Host "`nDeseas restaurar/actualizar las referencias locales con este bundle? (s/n)"
if ($action -match "^[sS]") {
    Write-Host "[*] Extrayendo ramas y tags desde el bundle..." -ForegroundColor Green
    git fetch "$BundleFile" "*:*"
    Write-Host " [✓] Restauración completada exitosamente desde el respaldo local." -ForegroundColor Green
} else {
    Write-Host "Operación finalizada sin modificaciones." -ForegroundColor Gray
}
