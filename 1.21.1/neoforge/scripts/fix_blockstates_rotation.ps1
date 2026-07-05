# Regenerate blockstates without invalid 1.21.1 y-rotations (337, 315, ...).
# Rotation is applied at render time by RotatedBlockModelWrapper.
$ErrorActionPreference = 'Stop'
$destRoot = 'C:\Users\Gamea\Documents\GitHub\Realistic-Traffic-Control\1.21.1\neoforge\src\main\resources\assets\realistictrafficcontrol'
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)

function Write-Utf8Json($path, $content) {
    $dir = Split-Path $path -Parent
    if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }
    [System.IO.File]::WriteAllText($path, $content, $utf8NoBom)
}

$rotatedBlocks = @(
    'pole','wood_pole','plus_pole','t_pole','d_pole','dh_pole','c_pole','ch_pole','h_pole','u_t_pole',
    'pole_base','stand','generator','tag','cone','channelizer','drum',
    'crossing_gate_pole','quiet_zone_signal','gate_guard','overhead','overhead_pole','overhead_crossbuck',
    'crossing_gate_crossbuck','concrete_barrier',
    'traffic_sensor_left','traffic_sensor_straight','traffic_sensor_right','pedestrian_button',
    'traffic_light_5_upper',
    'wch_bell','wayside_horn','wch_mechanical_bell','teardrop_bell','safetran_type_1','safetran_type_3','safetran_mechanical'
)

foreach ($name in $rotatedBlocks) {
    $variants = @()
    foreach ($k in 0..15) {
        $variants += "    `"rotation=$k`": { `"model`": `"realistictrafficcontrol:block/$name`" }"
    }
    $json = "{`n  `"variants`": {`n" + ($variants -join ",`n") + "`n  }`n}"
    Write-Utf8Json (Join-Path $destRoot "blockstates\$name.json") $json
}

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

$upperVariants = @()
foreach ($k in 0..15) {
    $upperVariants += "    `"rotation=$k`": { `"model`": `"realistictrafficcontrol:block/light_source`" }"
}
$upperJson = "{`n  `"variants`": {`n" + ($upperVariants -join ",`n") + "`n  }`n}"
Write-Utf8Json (Join-Path $destRoot "blockstates\traffic_light_5_upper.json") $upperJson

Write-Host 'Blockstates regenerated without invalid y-rotations.'
