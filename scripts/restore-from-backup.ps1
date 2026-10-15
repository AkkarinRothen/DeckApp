param(
    [string]$BundleFile
)

$ErrorActionPreference = "Stop"

Write-Host "=====================================================" -ForegroundColor Cyan
Write-Host "     DECKAPP - RESTAURACION ANTE DESASTRES GIT       " -ForegroundColor Cyan
Write-Host "=====================================================" -ForegroundColor Cyan

$backupDir = Join-Path $PSScriptRoot "..\.backups"
if (-not (Test-Path $backupDir)) {
    Write-Host "No se encontro el directorio de respaldos (.backups)." -ForegroundColor Yellow
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
    
    Write-Host ""
    $selection = Read-Host "Selecciona el numero del respaldo a inspeccionar/restaurar"
    $idx = [int]$selection - 1
    if ($idx -ge 0 -and $idx -lt $bundles.Count) {
        $BundleFile = $bundles[$idx].FullName
    } else {
        Write-Host "Seleccion invalida." -ForegroundColor Red
        exit 1
    }
}

Write-Host ""
Write-Host "[*] Verificando integridad del bundle: $BundleFile..." -ForegroundColor Green
git bundle verify "$BundleFile"

Write-Host ""
Write-Host "Contenido del bundle (ramas y tags preservados):" -ForegroundColor Cyan
git bundle list-heads "$BundleFile"

Write-Host ""
$action = Read-Host "Deseas restaurar/actualizar las referencias locales con este bundle? (s/n)"
if ($action -match "^[sS]") {
    Write-Host "[*] Extrayendo ramas y tags desde el bundle..." -ForegroundColor Green
    git fetch "$BundleFile" "*:*"
    Write-Host " [OK] Restauracion completada exitosamente desde el respaldo local." -ForegroundColor Green
} else {
    Write-Host "Operacion finalizada sin modificaciones." -ForegroundColor Gray
}
