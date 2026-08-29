$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$roots = @(
    (Join-Path $projectRoot 'common\src\main\resources\assets\more_slabs_stairs_and_walls'),
    (Join-Path $projectRoot 'common\src\main\generated\assets\more_slabs_stairs_and_walls')
)
$expected = [ordered]@{
    copper_grate = 'copper_grate'
    exposed_copper_grate = 'exposed_copper_grate'
    weathered_copper_grate = 'weathered_copper_grate'
    oxidized_copper_grate = 'oxidized_copper_grate'
    waxed_copper_grate = 'copper_grate'
    waxed_exposed_copper_grate = 'exposed_copper_grate'
    waxed_weathered_copper_grate = 'weathered_copper_grate'
    waxed_oxidized_copper_grate = 'oxidized_copper_grate'
}
$suffixes = @(
    'slab', 'slab_top', 'slab_double',
    'stairs', 'stairs_inner', 'stairs_outer',
    'wall_post', 'wall_side', 'wall_side_tall', 'wall_inventory'
)
function Resolve-Asset([string]$relative) {
    $matches = @($roots | ForEach-Object { Join-Path $_ $relative } | Where-Object { Test-Path -LiteralPath $_ -PathType Leaf })
    if ($matches.Count -ne 1) { throw "Expected one resolved asset for $relative; found $($matches.Count)" }
    return $matches[0]
}
$checked = 0
foreach ($family in $expected.Keys) {
    $texture = "minecraft:block/$($expected[$family])"
    foreach ($suffix in $suffixes) {
        $model = Get-Content -LiteralPath (Resolve-Asset "models\block\$family`_$suffix.json") -Raw | ConvertFrom-Json
        $roles = @($model.textures.PSObject.Properties.Value)
        if ($roles.Count -eq 0 -or @($roles | Where-Object { $_ -ne $texture }).Count -ne 0) {
            throw "$family/$suffix does not use exact current-stage texture $texture"
        }
        if ($model.render_type -ne 'cutout') { throw "$family/$suffix does not declare cutout rendering" }
        $checked++
    }
    foreach ($geometry in @('slab', 'stairs', 'wall')) {
        $state = Get-Content -LiteralPath (Resolve-Asset "blockstates\$family`_$geometry.json") -Raw | ConvertFrom-Json
        $modelRefs = @($state.variants.PSObject.Properties.Value.model) +
            @($state.multipart | ForEach-Object { $_.apply.model })
        foreach ($reference in @($modelRefs | Where-Object { $_ })) {
            $relative = ($reference -replace '^more_slabs_stairs_and_walls:', '') + '.json'
            Resolve-Asset ("models\" + ($relative -replace '/', '\')) | Out-Null
        }
        Resolve-Asset "items\$family`_$geometry.json" | Out-Null
    }
}
"PASS: $checked native Copper Grate slab/stair/wall model JSON files use exact stage textures; 24 blockstates/items resolve"
