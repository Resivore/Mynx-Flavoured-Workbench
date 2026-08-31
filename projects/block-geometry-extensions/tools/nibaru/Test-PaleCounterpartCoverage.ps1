$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$javaRoot = Join-Path $root 'src\nibaru\java\games\twinhead\moreslabsstairsandwalls'
$generatedRoot = Join-Path $root 'build\generated\nibaru-resources'
$modBlocks = Get-Content -Raw -LiteralPath (Join-Path $javaRoot 'block\ModBlocks.java')
$registry = Get-Content -Raw -LiteralPath (Join-Path $javaRoot 'registry\ModRegistry.java')
$profiles = Get-Content -Raw -LiteralPath (Join-Path $javaRoot 'api\material\NibaruMaterialProfiles.java')
$client = Get-Content -Raw -LiteralPath (Join-Path $root 'src\main\java\dev\aero\cnmterraincompat\CnmTerrainCompatClient.java')

$families = @(
    'stripped_pale_oak_log',
    'stripped_pale_oak_wood',
    'pale_oak_log',
    'pale_oak_wood',
    'pale_oak_leaves',
    'pale_oak_planks',
    'pale_moss_block'
)
$blockstateRoot = Join-Path $generatedRoot 'assets\more_slabs_stairs_and_walls\blockstates'
$modelRoot = Join-Path $generatedRoot 'assets\more_slabs_stairs_and_walls\models\block'
$itemRoot = Join-Path $generatedRoot 'assets\more_slabs_stairs_and_walls\items'
$lootRoot = Join-Path $generatedRoot 'data\more_slabs_stairs_and_walls\loot_table\blocks'
$effectiveResourceRoots = @(
    $generatedRoot,
    (Join-Path $root 'src\nibaru\resources'),
    (Join-Path $root 'src\main\resources')
)

$paleBlockstates = @(Get-ChildItem -LiteralPath $blockstateRoot -Filter '*pale*.json' -File)
$paleItems = @(Get-ChildItem -LiteralPath $itemRoot -Filter '*pale*.json' -File)
$paleLoot = @(Get-ChildItem -LiteralPath $lootRoot -Filter '*pale*.json' -File)
$paleResourceFiles = @(Get-ChildItem -LiteralPath $generatedRoot -Recurse -File | Where-Object {
    $_.Name -match 'pale_oak|pale_moss'
})
$paleOakResourceFiles = @(Get-ChildItem -LiteralPath $generatedRoot -Recurse -File | Where-Object {
    $_.FullName.Substring($generatedRoot.Length).Contains('pale_oak')
})

function Get-LongPathContent([string]$Path) {
    $resolved = [IO.Path]::GetFullPath($Path)
    if ($resolved.StartsWith('\\')) {
        $extended = '\\?\UNC\' + $resolved.Substring(2)
    } else {
        $extended = '\\?\' + $resolved
    }
    return [IO.File]::ReadAllText($extended)
}

$wrongTexture = @($paleResourceFiles | Where-Object {
    $_.Extension -eq '.json' -and (Get-LongPathContent $_.FullName) -match 'minecraft:block/dark_oak'
})

# No cross-family reference is currently legitimate. A future exception must be
# path-specific here and documented project-locally alongside the relationship.
$allowedPaleOakDarkOakReferences = @()
$wrongPaleOakReferences = @($paleOakResourceFiles | Where-Object {
    $relative = $_.FullName.Substring($generatedRoot.Length).TrimStart('\').Replace('\', '/')
    (Get-LongPathContent $_.FullName).Contains('dark_oak') -and
        $allowedPaleOakDarkOakReferences -notcontains $relative
})

$providerRecipeResources = @()
$providerRecipeAdvancements = @()
$vanillaRecipeOverrides = @()
foreach ($resourceRoot in $effectiveResourceRoots) {
    if (-not (Test-Path -LiteralPath $resourceRoot -PathType Container)) { continue }
    foreach ($file in Get-ChildItem -LiteralPath $resourceRoot -Recurse -Filter '*.json' -File) {
        $relative = $file.FullName.Substring($resourceRoot.Length).TrimStart('\').Replace('\', '/')
        if ($relative -match '^data/more_slabs_stairs_and_walls/recipes?/.+\.json$') {
            $providerRecipeResources += $file
        }
        if ($relative -match '^data/more_slabs_stairs_and_walls/advancements?/recipes(?:[./]|/).+\.json$') {
            $providerRecipeAdvancements += $file
        }
        if ($relative -match '^data/minecraft/recipes?/.+\.json$') {
            $vanillaRecipeOverrides += $file
        }
    }
}
$vanillaPaleOakRecipeOverrides = @($vanillaRecipeOverrides | Where-Object { $_.Name -like '*pale_oak*' })

function Get-ResourceAggregate([string]$Pattern) {
    $resourceRoot = Join-Path $root 'src\nibaru\resources'
    $resolved = (Resolve-Path -LiteralPath $resourceRoot).Path
    $lines = @(Get-ChildItem -LiteralPath $resourceRoot -Recurse -File |
        Where-Object { $_.FullName -match $Pattern } |
        Sort-Object FullName |
        ForEach-Object {
            $relative = $_.FullName.Substring($resolved.Length).TrimStart('\').Replace('\', '/')
            $text = [System.IO.File]::ReadAllText($_.FullName)
            $canonical = $text.Replace("`r`n", "`n").Replace("`r", "`n").Replace("`n", "`r`n")
            $fileSha = [Security.Cryptography.SHA256]::Create()
            try {
                $hash = ([BitConverter]::ToString($fileSha.ComputeHash(
                    [Text.UTF8Encoding]::new($false).GetBytes($canonical)))).Replace('-', '')
            } finally {
                $fileSha.Dispose()
            }
            "$relative|$hash"
        })
    $sha = [Security.Cryptography.SHA256]::Create()
    try {
        return ([BitConverter]::ToString($sha.ComputeHash([Text.Encoding]::UTF8.GetBytes(($lines -join "`n"))))).Replace('-', '')
    } finally {
        $sha.Dispose()
    }
}

$honeyAggregate = [string](Get-ResourceAggregate 'honey_block')
$slimeAggregate = [string](Get-ResourceAggregate 'slime_block')

$checks = [ordered]@{
    'exact seven Pale source profiles declared' = (@($families | Where-Object {
        $modBlocks -match "(?m)^\s*$($_.ToUpperInvariant())\(builder\(Blocks\.$($_.ToUpperInvariant())\)"
    }).Count -eq 7)
    'Pale Oak Planks is wall-only like Dark Oak Planks' = $modBlocks.Contains('PALE_OAK_PLANKS(builder(Blocks.PALE_OAK_PLANKS).axe().wallOnly())')
    'Pale Oak Planks Wall exists exactly once' = (@($paleBlockstates | Where-Object Name -eq 'pale_oak_planks_wall.json').Count -eq 1)
    'Pale Oak Planks adds no duplicate native slab or stairs' = (-not (Test-Path (Join-Path $blockstateRoot 'pale_oak_planks_slab.json')) -and -not (Test-Path (Join-Path $blockstateRoot 'pale_oak_planks_stairs.json')))
    'Pale counterpart adds exactly nineteen native blocks' = ($paleBlockstates.Count -eq 19 -and $paleItems.Count -eq 19 -and $paleLoot.Count -eq 19)
    'Pale log and wood stripping use exact Pale targets' = ($modBlocks.Contains('PALE_OAK_LOG(builder(Blocks.PALE_OAK_LOG).axe().modelType(ModelType.LOG).associatedBlock(STRIPPED_PALE_OAK_LOG))') -and $modBlocks.Contains('PALE_OAK_WOOD(builder(Blocks.PALE_OAK_WOOD).axe().setAllTexture("pale_oak_log").associatedBlock(STRIPPED_PALE_OAK_WOOD))'))
    'native registry delegates Pale log and wood stripping' = ($registry.Contains('PALE_OAK_LOG,') -and $registry.Contains('PALE_OAK_WOOD,'))
    'Pale Oak Leaves uses shared leaf block classes' = ($registry.Contains('PALE_OAK_LEAVES,') -and $modBlocks.Contains('modelType(ModelType.LEAVES).addBlockTags(BlockTags.LEAVES)'))
    'Pale Oak Leaves is explicitly untinted like vanilla' = ($profiles.Contains('CHERRY_LEAVES, PALE_OAK_LEAVES, AZALEA_LEAVES') -and $client.Contains('case PALE_OAK_LEAVES -> List.of();'))
    'Pale Moss mirrors ordinary Moss model classification' = $modBlocks.Contains('PALE_MOSS_BLOCK(builder(Blocks.PALE_MOSS_BLOCK).hoe())')
    'all Pale native resources use canonical Pale textures' = ($wrongTexture.Count -eq 0)
    'no Pale Oak resource contains an unintended Dark Oak reference' = ($wrongPaleOakReferences.Count -eq 0)
    'provider emits no current or legacy recipe resources' = ($providerRecipeResources.Count -eq 0)
    'provider emits no current or legacy recipe advancements' = ($providerRecipeAdvancements.Count -eq 0)
    'provider does not override vanilla recipes' = ($vanillaRecipeOverrides.Count -eq 0)
    'provider does not override vanilla Pale Oak full-block recipes' = ($vanillaPaleOakRecipeOverrides.Count -eq 0)
    'Pale log side and end roles are canonical' = ((Get-Content -Raw (Join-Path $modelRoot 'pale_oak_log_slab.json')).Contains('minecraft:block/pale_oak_log_top') -and (Get-Content -Raw (Join-Path $modelRoot 'pale_oak_log_slab.json')).Contains('minecraft:block/pale_oak_log"'))
    'stripped Pale log side and end roles are canonical' = ((Get-Content -Raw (Join-Path $modelRoot 'stripped_pale_oak_log_slab.json')).Contains('minecraft:block/stripped_pale_oak_log_top') -and (Get-Content -Raw (Join-Path $modelRoot 'stripped_pale_oak_log_slab.json')).Contains('minecraft:block/stripped_pale_oak_log"'))
    'Pale Moss resources use canonical texture' = ((Get-Content -Raw (Join-Path $modelRoot 'pale_moss_block_slab.json')).Contains('minecraft:block/pale_moss_block'))
    'Honey static resources remain frozen' = ($honeyAggregate.Trim() -eq 'DC0960E585FC2AA1000BFD616DA9A137C0CBAF9A86B1215EFA22533F23B7A62C')
    'Slime static resources remain frozen' = ($slimeAggregate.Trim() -eq '73F7C9656C26A450F566EC60FF1A53FD012C80B766CD3DD4F21C8B986086F138')
}

$failed = @($checks.GetEnumerator() | Where-Object { -not $_.Value })
foreach ($check in $checks.GetEnumerator()) {
    '{0}: {1}' -f ($(if ($check.Value) { 'PASS' } else { 'FAIL' })), $check.Key
}
'Honey aggregate: {0}' -f $honeyAggregate
'Slime aggregate: {0}' -f $slimeAggregate
if ($failed.Count -gt 0) { throw "Pale counterpart fixture failed: $($failed.Key -join ', ')" }

[ordered]@{
    profilesAdded = 7
    nativeBlocksAdded = $paleBlockstates.Count
    generatedPaleFiles = $paleResourceFiles.Count
    providerRecipes = $providerRecipeResources.Count
    providerRecipeAdvancements = $providerRecipeAdvancements.Count
    vanillaRecipeOverrides = $vanillaRecipeOverrides.Count
    unintendedDarkOakReferences = $wrongPaleOakReferences.Count
    expectedCnmTargets = 14
    honeyResourceAggregate = $honeyAggregate
    slimeResourceAggregate = $slimeAggregate
    result = 'PASS'
} | ConvertTo-Json
