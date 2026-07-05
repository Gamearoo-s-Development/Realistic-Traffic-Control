# Convert 1.12 Forge type-3 barrier blockstates to 1.21 multipart JSON.
$ErrorActionPreference = 'Stop'
$destRoot = 'C:\Users\Gamea\Documents\GitHub\Realistic-Traffic-Control\1.21.1\neoforge\src\main\resources\assets\realistictrafficcontrol'
$blockstatesRoot = Join-Path $destRoot 'blockstates'
$modelsRoot = Join-Path $destRoot 'models\block'
$itemRoot = Join-Path $destRoot 'models\item'
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)

function Write-Utf8Json($path, $content) {
    [System.IO.File]::WriteAllText($path, $content, $utf8NoBom)
}

function Merge-PreviewModel {
    param(
        [string]$BarsFile,
        [string]$OutName
    )
    $bars = Get-Content (Join-Path $modelsRoot $BarsFile) -Raw | ConvertFrom-Json
    $left = Get-Content (Join-Path $modelsRoot 'type_3_barrier_stand_left.json') -Raw | ConvertFrom-Json
    $right = Get-Content (Join-Path $modelsRoot 'type_3_barrier_stand_right.json') -Raw | ConvertFrom-Json

    $textures = [ordered]@{}
    foreach ($prop in $bars.textures.PSObject.Properties) {
        $textures[$prop.Name] = $prop.Value
    }
    $textures['1'] = 'realistictrafficcontrol:block/generic'

    function Rename-StandTexture($elements) {
        $clone = $elements | ConvertTo-Json -Depth 20 | ConvertFrom-Json
        foreach ($element in $clone) {
            foreach ($faceProp in $element.faces.PSObject.Properties) {
                if ($faceProp.Value.texture -eq '#0') {
                    $faceProp.Value.texture = '#1'
                }
            }
        }
        return $clone
    }

    $merged = [ordered]@{
        credit = 'Merged for inventory preview (bars + both stands)'
        textures = $textures
        elements = @($bars.elements + (Rename-StandTexture $left.elements) + (Rename-StandTexture $right.elements))
    }
    if ($bars.display) { $merged.display = $bars.display }
    $out = ($merged | ConvertTo-Json -Depth 20)
    Write-Utf8Json (Join-Path $modelsRoot $OutName) $out
}

Merge-PreviewModel -BarsFile 'type_3_barrier_bars.json' -OutName 'type_3_barrier_bars_with_stands.json'
Merge-PreviewModel -BarsFile 'type_3_barrier_bars_right.json' -OutName 'type_3_barrier_bars_right_with_stands.json'

function Write-Type3Blockstate {
    param(
        [string]$FileName,
        [string]$BarsModel
    )
    $facings = @(
        @{ Name = 'north'; Y = 0 },
        @{ Name = 'south'; Y = 180 },
        @{ Name = 'east'; Y = 90 },
        @{ Name = 'west'; Y = 270 }
    )
    $parts = @()
    foreach ($f in $facings) {
        $barsApply = if ($f.Y -eq 0) {
            "{ `"model`": `"realistictrafficcontrol:block/$BarsModel`" }"
        } else {
            "{ `"model`": `"realistictrafficcontrol:block/$BarsModel`", `"y`": $($f.Y) }"
        }
        $standLeftApply = if ($f.Y -eq 0) {
            "{ `"model`": `"realistictrafficcontrol:block/type_3_barrier_stand_left`" }"
        } else {
            "{ `"model`": `"realistictrafficcontrol:block/type_3_barrier_stand_left`", `"y`": $($f.Y) }"
        }
        $standRightApply = if ($f.Y -eq 0) {
            "{ `"model`": `"realistictrafficcontrol:block/type_3_barrier_stand_right`" }"
        } else {
            "{ `"model`": `"realistictrafficcontrol:block/type_3_barrier_stand_right`", `"y`": $($f.Y) }"
        }

        $parts += "    { `"when`": { `"facing`": `"$($f.Name)`", `"isfurthestleft`": false, `"isfurthestright`": false }, `"apply`": $barsApply }"
        $parts += "    { `"when`": { `"facing`": `"$($f.Name)`", `"isfurthestleft`": true, `"isfurthestright`": false }, `"apply`": [ $barsApply, $standLeftApply ] }"
        $parts += "    { `"when`": { `"facing`": `"$($f.Name)`", `"isfurthestleft`": false, `"isfurthestright`": true }, `"apply`": [ $barsApply, $standRightApply ] }"
        $parts += "    { `"when`": { `"facing`": `"$($f.Name)`", `"isfurthestleft`": true, `"isfurthestright`": true }, `"apply`": [ $barsApply, $standLeftApply, $standRightApply ] }"
    }
    $json = "{`n  `"multipart`": [`n" + ($parts -join ",`n") + "`n  ]`n}"
    Write-Utf8Json (Join-Path $blockstatesRoot $FileName) $json
}

Write-Type3Blockstate -FileName 'type_3_barrier.json' -BarsModel 'type_3_barrier_bars'
Write-Type3Blockstate -FileName 'type_3_barrier_right.json' -BarsModel 'type_3_barrier_bars_right'

Write-Utf8Json (Join-Path $itemRoot 'type_3_barrier.json') '{ "parent": "realistictrafficcontrol:block/type_3_barrier_bars_with_stands" }'
Write-Utf8Json (Join-Path $itemRoot 'type_3_barrier_right.json') '{ "parent": "realistictrafficcontrol:block/type_3_barrier_bars_right_with_stands" }'

Write-Host 'Type-3 barrier blockstates, preview models, and item models regenerated.'
