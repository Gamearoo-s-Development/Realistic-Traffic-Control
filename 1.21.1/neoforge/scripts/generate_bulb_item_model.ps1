# traffic_light_bulb.json uses overrides; geometry lives in traffic_light_bulb_base.json.
$ErrorActionPreference = 'Stop'
$dest = 'C:\Users\Gamea\Documents\GitHub\Realistic-Traffic-Control\1.21.1\neoforge\src\main\resources\assets\realistictrafficcontrol\models\item\traffic_light_bulb.json'
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)

$map = @{
    0  = 'traffic_light_bulb_red'
    1  = 'traffic_light_bulb_yellow'
    2  = 'traffic_light_bulb_green'
    3  = 'traffic_light_bulb_red_arrow_left'
    4  = 'traffic_light_bulb_yellow_arrow_left'
    5  = 'traffic_light_bulb_green_arrow_left'
    6  = 'traffic_light_bulb_cross'
    7  = 'traffic_light_bulb_dont_cross'
    8  = 'traffic_light_bulb_red_arrow_right'
    9  = 'traffic_light_bulb_yellow_arrow_right'
    10 = 'traffic_light_bulb_green_arrow_right'
    11 = 'traffic_light_bulb_no_right_turn'
    12 = 'traffic_light_bulb_no_left_turn'
    13 = 'traffic_light_bulb_straight_red'
    14 = 'traffic_light_bulb_straight_yellow'
    15 = 'traffic_light_bulb_straight_green'
    16 = 'traffic_light_bulb_red_arrow_uturn'
    17 = 'traffic_light_bulb_yellow_arrow_uturn'
    18 = 'traffic_light_bulb_green_arrow_uturn'
    19 = 'traffic_light_bulb_yellow_arrow_left2'
    20 = 'traffic_light_bulb_yellow_arrow_right2'
    21 = 'traffic_light_bulb_yellow_arrow_uturn2'
    22 = 'traffic_light_bulb_red2'
    23 = 'traffic_light_bulb_red_x'
    24 = 'traffic_light_bulb_green_down'
    25 = 'traffic_light_bulb_red_arrow_right2'
    26 = 'traffic_light_bulb_red_arrow_left2'
    27 = 'traffic_light_bulb_red_arrow_uturn2'
    28 = 'traffic_light_bulb_yellow_arrow_left2'
    29 = 'traffic_light_bulb_yellow_arrow_right2'
    30 = 'traffic_light_bulb_yellow_arrow_uturn2'
    31 = 'traffic_light_bulb_green_arrow_left'
    32 = 'traffic_light_bulb_green_arrow_right'
    33 = 'traffic_light_bulb_green_arrow_uturn'
    34 = 'traffic_light_bulb_yellow_x'
}

$overrides = @()
foreach ($entry in ($map.GetEnumerator() | Sort-Object Name)) {
    if ($entry.Key -eq 0) { continue }
    $idx = [double]$entry.Key
    $model = $entry.Value
    $overrides += "    { `"predicate`": { `"realistictrafficcontrol:bulb_type`": $idx }, `"model`": `"realistictrafficcontrol:item/$model`" }"
}

$json = @"
{
  "parent": "realistictrafficcontrol:item/traffic_light_bulb_red",
  "overrides": [
$($overrides -join ",`n")
  ]
}
"@

[System.IO.File]::WriteAllText($dest, $json, $utf8NoBom)
Write-Host "Wrote bulb item model with $($map.Count) variants."
