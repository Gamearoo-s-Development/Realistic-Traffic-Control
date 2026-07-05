# Copy any 1.12.2 recipes not yet present in 1.21.1 (basic ore_dict -> tag conversion).
$ErrorActionPreference = 'Stop'
$src = 'C:\Users\Gamea\Documents\GitHub\Realistic-Traffic-Control\1.12.2\src\main\resources\assets\realistictrafficcontrol\recipes'
$dest = 'C:\Users\Gamea\Documents\GitHub\Realistic-Traffic-Control\1.21.1\neoforge\src\main\resources\data\realistictrafficcontrol\recipes'

$oreMap = @{
    'cobblestone' = 'minecraft:cobblestone'
    'ingotIron' = 'minecraft:iron_ingot'
    'ingotGold' = 'minecraft:gold_ingot'
    'blockIron' = 'minecraft:iron_block'
    'blockRedstone' = 'minecraft:redstone_block'
    'dustRedstone' = 'minecraft:redstone'
    'dyeOrange' = 'minecraft:orange_dye'
    'dyeWhite' = 'minecraft:white_dye'
}

Get-ChildItem $src -Filter '*.json' | ForEach-Object {
    $outPath = Join-Path $dest $_.Name
    if (Test-Path $outPath) { return }
    $text = [System.IO.File]::ReadAllText($_.FullName)
    $text = $text -replace '"type"\s*:\s*"forge:ore_dict"\s*,\s*"ore"\s*:\s*"([^"]+)"', {
        param($m)
        $ore = $m.Groups[1].Value
        if ($oreMap.ContainsKey($ore)) {
            '"item": "' + $oreMap[$ore] + '"'
        } else {
            '"tag": "c:' + ($ore.ToLower() -replace 'ingot','') + '"'
        }
    }
    $text = $text -replace '"item"\s*:\s*"realistictrafficcontrol:([^"]+)"', {
        param($m)
        $id = $m.Groups[1].Value
        if ($id -like 'traffic_light_bulb*') {
            '"id": "realistictrafficcontrol:traffic_light_bulb", "components": { "realistictrafficcontrol:bulb_type": 0 }'
        } else {
            '"id": "realistictrafficcontrol:' + $id + '"'
        }
    }
    $text = $text -replace '"result"\s*:\s*\{', '"result": {'
    [System.IO.File]::WriteAllText($outPath, $text)
    Write-Host "Added recipe: $($_.Name)"
}

Write-Host 'Missing recipe sync complete.'
