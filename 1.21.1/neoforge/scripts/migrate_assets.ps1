# Migrate 1.12.2 RTC assets into the 1.21.1 NeoForge resource tree.
$ErrorActionPreference = 'Stop'
$srcRoot = 'C:\Users\Gamea\Documents\GitHub\Realistic-Traffic-Control\1.12.2\src\main\resources\assets\realistictrafficcontrol'
$jarExtract = 'C:\Users\Gamea\Documents\GitHub\Realistic-Traffic-Control\1.21.1\neoforge\_asset_extract\assets\realistictrafficcontrol'
$destRoot = 'C:\Users\Gamea\Documents\GitHub\Realistic-Traffic-Control\1.21.1\neoforge\src\main\resources\assets\realistictrafficcontrol'
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)

function Write-Utf8Json($path, $content) {
    $dir = Split-Path $path -Parent
    if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }
    [System.IO.File]::WriteAllText($path, $content, $utf8NoBom)
}

function Fix-ModelJson($text) {
    $text = $text -replace 'realistictrafficcontrol:blocks/', 'realistictrafficcontrol:block/'
    $text = $text -replace 'realistictrafficcontrol:items/', 'realistictrafficcontrol:item/'
    # 1.12 parent refs without folder -> block/
    $text = $text -replace '"parent"\s*:\s*"realistictrafficcontrol:([a-z0-9_]+)"', '"parent": "realistictrafficcontrol:block/$1"'
    return $text
}

# --- 1. Textures from JAR (blocks/ -> block/, items/ -> item/) ---
$texSrc = Join-Path $jarExtract 'textures'
$texDest = Join-Path $destRoot 'textures'
foreach ($folder in @('blocks', 'items', 'gui', 'entity', 'models', 'effects')) {
    $from = Join-Path $texSrc $folder
    if (-not (Test-Path $from)) { continue }
    $toName = switch ($folder) { 'blocks' { 'block' } 'items' { 'item' } default { $folder } }
    $to = Join-Path $texDest $toName
    if (Test-Path $to) { Remove-Item $to -Recurse -Force }
    Copy-Item $from $to -Recurse -Force
}
# Copy sounds.json sibling sounds folder if present
$soundsFrom = Join-Path $jarExtract 'sounds'
if (Test-Path $soundsFrom) {
    $soundsTo = Join-Path $destRoot 'sounds'
    if (Test-Path $soundsTo) { Remove-Item $soundsTo -Recurse -Force }
    Copy-Item $soundsFrom $soundsTo -Recurse -Force
}

# --- 2. Block + item models from 1.12.2 source ---
$blockModelsSrc = Join-Path $srcRoot 'models\block'
$blockModelsDest = Join-Path $destRoot 'models\block'
Get-ChildItem $blockModelsSrc -Filter '*.json' | ForEach-Object {
    $fixed = Fix-ModelJson ([System.IO.File]::ReadAllText($_.FullName))
    Write-Utf8Json (Join-Path $blockModelsDest $_.Name) $fixed
}

$itemModelsSrc = Join-Path $srcRoot 'models\item'
$itemModelsDest = Join-Path $destRoot 'models\item'
Get-ChildItem $itemModelsSrc -Filter '*.json' | ForEach-Object {
    $fixed = Fix-ModelJson ([System.IO.File]::ReadAllText($_.FullName))
    Write-Utf8Json (Join-Path $itemModelsDest $_.Name) $fixed
}

$rotatedBlocks = @(
    'pole','wood_pole','plus_pole','t_pole','d_pole','dh_pole','c_pole','ch_pole','h_pole','u_t_pole',
    'pole_base','stand','generator','tag','cone','channelizer','drum',
    'crossing_gate_pole','quiet_zone_signal','gate_guard','overhead','overhead_pole','overhead_crossbuck',
    'crossing_gate_crossbuck','concrete_barrier',
    'traffic_sensor_left','traffic_sensor_straight','traffic_sensor_right','pedestrian_button'
)

# --- 3. Rotation blockstates (y-rotation applied at render time via RotatedBlockModelWrapper) ---
foreach ($name in $rotatedBlocks) {
    $variants = @()
    foreach ($k in 0..15) {
        $variants += "    `"rotation=$k`": { `"model`": `"realistictrafficcontrol:block/$name`" }"
    }
    $json = "{`n  `"variants`": {`n" + ($variants -join ",`n") + "`n  }`n}"
    Write-Utf8Json (Join-Path $destRoot "blockstates\$name.json") $json
    Write-Utf8Json (Join-Path $destRoot "models\item\$name.json") "{ `"parent`": `"realistictrafficcontrol:block/$name`" }"
}

# Horizontal pole (facing property)
Write-Utf8Json (Join-Path $destRoot 'blockstates\horizontal_pole.json') @'
{
  "variants": {
    "facing=north": { "model": "realistictrafficcontrol:block/horizontal_pole" },
    "facing=south": { "model": "realistictrafficcontrol:block/horizontal_pole", "y": 180 },
    "facing=west": { "model": "realistictrafficcontrol:block/horizontal_pole", "y": 270 },
    "facing=east": { "model": "realistictrafficcontrol:block/horizontal_pole", "y": 90 }
  }
}
'@
Write-Utf8Json (Join-Path $destRoot 'models\item\horizontal_pole.json') '{ "parent": "realistictrafficcontrol:block/horizontal_pole" }'

# Traffic light blocks (rotation + cover + pole)
$trafficLights = @(
    @{ Name = 'traffic_light'; Cover = 'traffic_light_covered'; Backing = $true },
    @{ Name = 'traffic_light_hoz'; Cover = 'traffic_light_hoz_covered'; Backing = $false },
    @{ Name = 'traffic_light_1'; Cover = 'traffic_light1_covered'; Backing = $false },
    @{ Name = 'traffic_light_2'; Cover = 'traffic_light2_covered'; Backing = $false },
    @{ Name = 'traffic_light_2_hoz'; Cover = 'traffic_light2_hoz_covered'; Backing = $false },
    @{ Name = 'traffic_light_4'; Cover = 'traffic_light4_covered'; Backing = $false },
    @{ Name = 'traffic_light_4_hoz'; Cover = 'traffic_light4_hoz_covered'; Backing = $false },
    @{ Name = 'traffic_light_5'; Cover = 'traffic_light5_covered'; Backing = $false },
    @{ Name = 'traffic_light_5_hoz'; Cover = 'traffic_light5_hoz_covered'; Backing = $false },
    @{ Name = 'traffic_light_doghouse'; Cover = 'traffic_lightdoghouse_covered'; Backing = $false },
    @{ Name = 'traffic_light_6'; Cover = 'traffic_light6_covered'; Backing = $false },
    @{ Name = 'traffic_light_7'; Cover = 'traffic_light7_covered'; Backing = $false },
    @{ Name = 'traffic_light_8'; Cover = 'traffic_light8_covered'; Backing = $false }
)
foreach ($entry in $trafficLights) {
    $name = $entry.Name
    $coverModel = $entry.Cover
    $parts = @(
        "    { `"apply`": { `"model`": `"realistictrafficcontrol:block/$name`" } }",
        "    { `"when`": { `"cover`": `"true`" }, `"apply`": { `"model`": `"realistictrafficcontrol:block/$coverModel`" } }"
    )
    if ($entry.Backing) {
        $parts += "    { `"apply`": { `"model`": `"realistictrafficcontrol:block/traffic_light_backing`" } }"
    }
    $json = "{`n  `"multipart`": [`n" + ($parts -join ",`n") + "`n  ]`n}"
    Write-Utf8Json (Join-Path $destRoot "blockstates\$name.json") $json
}

# Frame items -> block model parent
$frames = @(
    'traffic_light_frame','traffic_light_hoz_frame','traffic_light_1_frame','traffic_light_2_frame',
    'traffic_light_2_hoz_frame','traffic_light_4_frame','traffic_light_4_hoz_frame','traffic_light_5_frame',
    'traffic_light_5_hoz_frame','traffic_light_doghouse_frame','traffic_light_6_frame','traffic_light_7_frame',
    'traffic_light_8_frame'
)
$frameToBlock = @{
    'traffic_light_frame'='traffic_light'; 'traffic_light_hoz_frame'='traffic_light_hoz'
    'traffic_light_1_frame'='traffic_light_1'; 'traffic_light_2_frame'='traffic_light_2'
    'traffic_light_2_hoz_frame'='traffic_light_2_hoz'; 'traffic_light_4_frame'='traffic_light_4'
    'traffic_light_4_hoz_frame'='traffic_light_4_hoz'; 'traffic_light_5_frame'='traffic_light_5'
    'traffic_light_5_hoz_frame'='traffic_light_5_hoz'; 'traffic_light_doghouse_frame'='traffic_light_doghouse'
    'traffic_light_6_frame'='traffic_light_6'; 'traffic_light_7_frame'='traffic_light_7'
    'traffic_light_8_frame'='traffic_light_8'
}
foreach ($item in $frames) {
    $block = $frameToBlock[$item]
    Write-Utf8Json (Join-Path $destRoot "models\item\$item.json") "{ `"parent`": `"realistictrafficcontrol:block/$block`" }"
}

# Bell blocks - simple model
$bells = @('wch_bell','wayside_horn','wch_mechanical_bell','teardrop_bell','safetran_type_1','safetran_type_3','safetran_mechanical')
foreach ($name in $bells) {
    Write-Utf8Json (Join-Path $destRoot "blockstates\$name.json") "{ `"variants`": { `"`": { `"model`": `"realistictrafficcontrol:block/$name`" } } }"
    Write-Utf8Json (Join-Path $destRoot "models\item\$name.json") "{ `"parent`": `"realistictrafficcontrol:block/$name`" }"
}

# Control box
Write-Utf8Json (Join-Path $destRoot 'blockstates\traffic_light_control_box.json') '{ "variants": { "": { "model": "realistictrafficcontrol:block/traffic_light_control_box" } } }'
Write-Utf8Json (Join-Path $destRoot 'models\item\traffic_light_control_box.json') '{ "parent": "realistictrafficcontrol:block/traffic_light_control_box" }'

# Bulb item uses generated model from 1.12 if exists, else keep redstone placeholder
$bulb12 = Join-Path $itemModelsSrc 'traffic_light_bulb.json'
if (Test-Path $bulb12) {
    $fixed = Fix-ModelJson ([System.IO.File]::ReadAllText($bulb12))
    Write-Utf8Json (Join-Path $destRoot 'models\item\traffic_light_bulb.json') $fixed
}

Write-Host 'Asset migration complete.'
