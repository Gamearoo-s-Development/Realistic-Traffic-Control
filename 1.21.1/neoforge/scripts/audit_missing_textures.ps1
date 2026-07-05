# Report PNG texture paths referenced by models/items that are missing on disk.
$ErrorActionPreference = 'Stop'
$assetsRoot = 'C:\Users\Gamea\Documents\GitHub\Realistic-Traffic-Control\1.21.1\neoforge\src\main\resources\assets\realistictrafficcontrol'
$texRoot = Join-Path $assetsRoot 'textures'

$refs = [System.Collections.Generic.HashSet[string]]::new([StringComparer]::OrdinalIgnoreCase)
Get-ChildItem $assetsRoot -Recurse -Filter '*.json' | ForEach-Object {
    $content = [System.IO.File]::ReadAllText($_.FullName)
    foreach ($match in [regex]::Matches($content, '"(?:layer0|layer1|layer2|particle|[0-9]+)"\s*:\s*"realistictrafficcontrol:(block|item|gui|entity|models|effects)/[^"]+"')) {
        if ($match.Value -match 'realistictrafficcontrol:(block|item|gui|entity|models|effects)/[^"]+') {
            [void]$refs.Add($Matches[0])
        }
    }
    foreach ($match in [regex]::Matches($content, '"textures"\s*:\s*\{[^}]+\}')) {
        foreach ($tex in [regex]::Matches($match.Value, 'realistictrafficcontrol:(block|item|gui|entity|models|effects)/[^"]+')) {
            [void]$refs.Add($tex.Value)
        }
    }
}

$missing = @()
foreach ($ref in ($refs | Sort-Object)) {
    $rel = $ref -replace '^realistictrafficcontrol:', '' -replace '/', '\'
    $path = Join-Path $texRoot ($rel + '.png')
    if (-not (Test-Path $path)) {
        $missing += $ref
    }
}

Write-Host "Texture refs in models/items: $($refs.Count)"
Write-Host "Missing: $($missing.Count)"
$missing | ForEach-Object { Write-Host "  $_" }
if ($missing.Count -gt 0) { exit 1 }
