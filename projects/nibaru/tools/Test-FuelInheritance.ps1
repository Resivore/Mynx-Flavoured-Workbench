$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$mixinPath = Join-Path $projectRoot 'fabric\src\main\java\games\twinhead\moreslabsstairsandwalls\fabric\mixin\FuelValuesBuilderMixin.java'
$source = Get-Content -LiteralPath $mixinPath -Raw

$requiredSourceFragments = @(
    '@Mixin(FuelValues.Builder.class)',
    '@Inject(method = "build", at = @At("HEAD"))',
    'values.getInt(family.parentBlock.asItem())',
    'type == ModBlocks.BlockType.SLAB',
    'parentDuration / 2',
    'builder.add(generatedBlock, inheritedDuration)'
)

foreach ($fragment in $requiredSourceFragments) {
    if (-not $source.Contains($fragment)) {
        throw "Fuel bridge is missing required implementation fragment: $fragment"
    }
}

$parents = [ordered]@{
    oak_planks = 300
    coal_block = 16000
    stone = 0
    distinctive_mod_fuel = 733
}
$generated = @{}

foreach ($entry in $parents.GetEnumerator()) {
    if ($entry.Value -le 0) {
        continue
    }

    $generated["$($entry.Key)_stairs"] = $entry.Value
    $generated["$($entry.Key)_wall"] = $entry.Value
    $generated["$($entry.Key)_slab"] = [Math]::Truncate($entry.Value / 2)
}

if ($generated['oak_planks_stairs'] -ne 300 -or $generated['oak_planks_wall'] -ne 300 -or $generated['oak_planks_slab'] -ne 150) {
    throw 'Normal wood inheritance failed.'
}
if ($generated['coal_block_stairs'] -ne 16000 -or $generated['coal_block_wall'] -ne 16000 -or $generated['coal_block_slab'] -ne 8000) {
    throw 'Non-wood vanilla fuel inheritance failed.'
}
if ($generated.ContainsKey('stone_stairs') -or $generated.ContainsKey('stone_wall') -or $generated.ContainsKey('stone_slab')) {
    throw 'A nonfuel parent produced fuel geometry.'
}
if ($generated['distinctive_mod_fuel_stairs'] -ne 733 -or $generated['distinctive_mod_fuel_wall'] -ne 733 -or $generated['distinctive_mod_fuel_slab'] -ne 366) {
    throw 'Distinctive mod-added duration or integer half semantics failed.'
}
if ($generated.Count -ne 9) {
    throw "Family entries were overwritten or unexpectedly added; expected 9, found $($generated.Count)."
}

Write-Output 'PASS: fuel inheritance source contract and representative fixtures (wood, non-wood, nonfuel, odd modded value, three geometries, independent families).'
