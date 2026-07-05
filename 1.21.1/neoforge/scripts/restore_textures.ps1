# Restore real textures from the 1.12.2 built JAR (fixes black placeholder poles/generic).
$ErrorActionPreference = 'Stop'
$jar = 'C:\Users\Gamea\Documents\GitHub\Realistic-Traffic-Control\1.12.2\build\libs\realistictrafficcontrol.1.12.2-3.2.0.jar'
$extractRoot = 'C:\Users\Gamea\Documents\GitHub\Realistic-Traffic-Control\1.21.1\neoforge\_asset_extract'
$destRoot = 'C:\Users\Gamea\Documents\GitHub\Realistic-Traffic-Control\1.21.1\neoforge\src\main\resources\assets\realistictrafficcontrol'

if (-not (Test-Path $jar)) {
    Write-Error "JAR not found: $jar"
}

if (Test-Path $extractRoot) { Remove-Item $extractRoot -Recurse -Force }
New-Item -ItemType Directory -Path $extractRoot -Force | Out-Null

& 'C:\Program Files\Java\jdk-17\bin\jar.exe' xf $jar -C $extractRoot 'assets/realistictrafficcontrol/textures/' 2>&1 | Out-Null
# jar xf doesn't support -C on all versions; use Expand-Archive fallback
if (-not (Test-Path "$extractRoot\assets\realistictrafficcontrol\textures")) {
    Push-Location $extractRoot
    & 'C:\Program Files\Java\jdk-17\bin\jar.exe' xf $jar
    Pop-Location
}

$texSrc = Join-Path $extractRoot 'assets\realistictrafficcontrol\textures'
$texDest = Join-Path $destRoot 'textures'

foreach ($folder in @('blocks', 'items', 'gui', 'entity', 'models', 'effects')) {
    $from = Join-Path $texSrc $folder
    if (-not (Test-Path $from)) { continue }
    $toName = switch ($folder) { 'blocks' { 'block' } 'items' { 'item' } default { $folder } }
    $to = Join-Path $texDest $toName
    if (Test-Path $to) { Remove-Item $to -Recurse -Force }
    Copy-Item $from $to -Recurse -Force
}

# 1.21.1 requires lowercase texture paths
$renames = @{
    'block\blackSmooth.png' = 'block\black_smooth.png'
    'block\orangeSmooth.png' = 'block\orange_smooth.png'
    'block\signs\signBase.png' = 'block\signs\sign_base.png'
    'block\signs\signError.png' = 'block\signs\sign_error.png'
    'block\signs\signPost.png' = 'block\signs\sign_post.png'
}
foreach ($entry in $renames.GetEnumerator()) {
    $src = Join-Path $texDest $entry.Key
    $dst = Join-Path $texDest $entry.Value
    if (Test-Path $src) {
        $dir = Split-Path $dst -Parent
        if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }
        Move-Item $src $dst -Force
    }
}

$count = (Get-ChildItem $texDest -Recurse -Filter '*.png' | Measure-Object).Count
$generic = Join-Path $texDest 'block\generic.png'
$genericSize = if (Test-Path $generic) { (Get-Item $generic).Length } else { 0 }
Write-Host "Restored $count PNG textures. generic.png size: $genericSize bytes."
