param(
    [string]$AssetsRoot = "$PSScriptRoot\..\..\1.21.1\neoforge\src\main\resources\assets\realistictrafficcontrol"
)

$modelsRoot = Join-Path $AssetsRoot "models\block"
$texturesRoot = Join-Path $AssetsRoot "textures\block"

if (-not (Test-Path $modelsRoot)) {
    throw "Missing model directory: $modelsRoot"
}

$modelFiles = Get-ChildItem -Path $modelsRoot -Filter "*.json" -File |
    Where-Object {
        $_.Name -like "crossing*" -or
        $_.Name -like "wig*" -or
        $_.Name -like "vertical_wig*" -or
        $_.Name -like "overhead*" -or
        $_.Name -like "ped*" -or
        $_.Name -like "shunt*" -or
        $_.Name -like "traffic_rail*" -or
        $_.Name -like "*bell*" -or
        $_.Name -like "gate_*" -or
        $_.Name -like "quiet_zone*" -or
        $_.Name -like "wayside*" -or
        $_.Name -like "safetran*" -or
        $_.Name -like "teardrop*" -or
        $_.Name -like "wch_*"
    }

$textureReferences = [System.Collections.Generic.HashSet[string]]::new()
$referencePattern = [regex]'realistictrafficcontrol:block/([A-Za-z0-9_./-]+)'

foreach ($model in $modelFiles) {
    $content = Get-Content -Raw -Path $model.FullName
    # Parent model paths are not textures and are resolved separately by Minecraft.
    $content = [regex]::Replace(
        $content,
        '"parent"\s*:\s*"realistictrafficcontrol:block/[^"]+"',
        ''
    )

    foreach ($match in $referencePattern.Matches($content)) {
        [void]$textureReferences.Add($match.Groups[1].Value)
    }
}

$missing = @(
    $textureReferences |
        Sort-Object |
        Where-Object {
            -not (Test-Path (Join-Path $texturesRoot ($_.Replace("/", "\") + ".png")))
        }
)

Write-Host "Scanned $($modelFiles.Count) crossing/railroad models and $($textureReferences.Count) texture references."
if ($missing.Count -eq 0) {
    Write-Host "OK: every referenced texture exists under textures/block/."
    exit 0
}

Write-Host "Missing textures:"
$missing | ForEach-Object { Write-Host "  $_.png" }
exit 1
# Scans 1.21.1 railroad/crossing block models for texture references and checks files under textures/block/.
param(
    [string]$AssetsRoot = "$PSScriptRoot\..\..\1.21.1\neoforge\src\main\resources\assets\realistictrafficcontrol"
)

$modelsDir = Join-Path $AssetsRoot "models\block"
$texturesDir = Join-Path $AssetsRoot "textures\block"

if (-not (Test-Path $modelsDir)) {
    Write-Error "Models directory not found: $modelsDir"
    exit 1
}

$patterns = @(
    "crossing*",
    "wig*",
    "overhead*",
    "ped_crossing*",
    "shunt*",
    "traffic_rail*",
    "*bell*",
    "gate_*",
    "quiet_zone*",
    "wayside*",
    "safetran*",
    "teardrop*",
    "wch_*"
)

$modelFiles = Get-ChildItem -Path $modelsDir -Filter "*.json" -File |
    Where-Object { $name = $_.Name; $patterns | Where-Object { $name -like $_ } }

$textureRefs = [System.Collections.Generic.HashSet[string]]::new()
$refRegex = [regex]'realistictrafficcontrol:block/([a-zA-Z0-9_./-]+)'

foreach ($file in $modelFiles) {
    $content = Get-Content -Raw -Path $file.FullName
    # Model parents use the same namespace path; only scan texture declarations and face texture paths.
    $content = [regex]::Replace($content, '"parent"\s*:\s*"realistictrafficcontrol:block/[^"]+"', '')
    foreach ($match in $refRegex.Matches($content)) {
        [void]$textureRefs.Add($match.Groups[1].Value)
    }
}

$missing = @()
foreach ($ref in ($textureRefs | Sort-Object)) {
    $path = Join-Path $texturesDir "$ref.png"
    if (-not (Test-Path $path)) {
        $missing += $ref
    }
}

Write-Host "Scanned $($modelFiles.Count) model(s), $($textureRefs.Count) unique texture ref(s)."
if ($missing.Count -eq 0) {
    Write-Host "OK: All referenced textures exist under textures/block/."
    exit 0
}

Write-Host "MISSING textures ($($missing.Count)):"
$missing | ForEach-Object { Write-Host "  - $_.png" }
exit 1
