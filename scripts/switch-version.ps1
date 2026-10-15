<#
.SYNOPSIS
    Permite cambiar de forma segura entre diferentes versiones y checkpoints del proyecto,
    o regresar a la versión más reciente en 'master'.
.DESCRIPTION
    1. Verifica que el árbol de trabajo no tenga cambios sin guardar.
    2. Lista los checkpoints disponibles si no se pasa -Version.
    3. Permite volver a 'master' con el parámetro -BackToMaster o seleccionar un tag.
.EXAMPLE
    .\scripts\switch-version.ps1 -Version "sprint-58"
    .\scripts\switch-version.ps1 -BackToMaster
#>
param(
    [string]$Version,
    [switch]$BackToMaster,
    [switch]$CreateBranch
)

$ErrorActionPreference = "Stop"

Write-Host "=====================================================" -ForegroundColor Cyan
Write-Host "     DECKAPP - SELECTOR Y NAVEGADOR DE VERSIONES     " -ForegroundColor Cyan
Write-Host "=====================================================" -ForegroundColor Cyan

# 1. Comprobar que no haya cambios sin guardar
$gitStatus = git status --porcelain
if ($gitStatus) {
    Write-Host "[!] ADVERTENCIA: Tienes cambios sin commitear en tu árbol de trabajo:" -ForegroundColor Red
    git status -s
    Write-Host "`nPor seguridad, commitea o haz 'git stash' de tus cambios antes de cambiar de versión." -ForegroundColor Yellow
    exit 1
}

$currentBranch = (git branch --show-current).Trim()

# 2. Si se solicitó volver a master
if ($BackToMaster) {
    Write-Host "[*] Regresando a la rama de desarrollo principal 'master'..." -ForegroundColor Green
    git checkout master
    Write-Host " [✓] Ahora estás en 'master' (último estado de desarrollo)." -ForegroundColor Green
    exit 0
}

# 3. Listar tags disponibles si no se especificó $Version
if (-not $Version) {
    $tags = git tag -l --sort=-creatordate
    if (-not $tags) {
        Write-Host "No se encontraron tags de versiones en el repositorio." -ForegroundColor Yellow
        Write-Host "Puedes crear uno con .\scripts\checkpoint.ps1" -ForegroundColor Gray
        exit 0
    }
    
    Write-Host "Versiones y Checkpoints disponibles:" -ForegroundColor Yellow
    $tagList = @($tags)
    for ($i = 0; $i -lt $tagList.Count; $i++) {
        $t = $tagList[$i]
        $msg = git tag -l --format="%(contents:subject) (creado %(creatordate:short))" $t
        Write-Host "  [$($i+1)] $t - $msg" -ForegroundColor White
    }
    Write-Host "  [0] Regresar a la última versión en 'master'" -ForegroundColor Cyan

    $selection = Read-Host "`nIngresa el número de la versión deseada (o 0 para master)"
    if ($selection -eq "0") {
        git checkout master
        Write-Host " [✓] Regresaste a 'master'." -ForegroundColor Green
        exit 0
    }
    
    $idx = [int]$selection - 1
    if ($idx -ge 0 -and $idx -lt $tagList.Count) {
        $Version = $tagList[$idx]
    } else {
        Write-Host "Selección inválida." -ForegroundColor Red
        exit 1
    }
}

# 4. Cambiar a la versión elegida
Write-Host "[*] Verificando tag '$Version'..." -ForegroundColor Green
$tagCheck = git tag -l $Version
if (-not $tagCheck) {
    Write-Host "Error: No existe el tag o versión '$Version'." -ForegroundColor Red
    exit 1
}

if ($CreateBranch) {
    $branchName = "preview/$Version"
    Write-Host "[*] Creando y cambiando a rama temporal '$branchName'..." -ForegroundColor Green
    git checkout -B "$branchName" "$Version"
} else {
    Write-Host "[*] Cambiando a '$Version' en modo inspección segura (detached HEAD)..." -ForegroundColor Green
    git checkout "$Version"
}

Write-Host "`n=====================================================" -ForegroundColor Green
Write-Host " [✓] CAMBIO DE VERSIÓN COMPLETADO: $Version" -ForegroundColor Green
Write-Host "  - Estás visualizando el estado exacto de: $Version" -ForegroundColor White
Write-Host "  - Para volver al trabajo normal, ejecuta:" -ForegroundColor Gray
Write-Host "      .\scripts\switch-version.ps1 -BackToMaster" -ForegroundColor Cyan
Write-Host "=====================================================" -ForegroundColor Green
