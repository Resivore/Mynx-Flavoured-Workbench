param(
    [Parameter(Mandatory = $true)]
    [string]$Jar
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$resolvedJar = (Resolve-Path -LiteralPath $Jar).Path
$namespace = 'more_slabs_stairs_and_walls'
$archive = [System.IO.Compression.ZipFile]::OpenRead($resolvedJar)
try {
    $names = @($archive.Entries | ForEach-Object FullName)
    $blockstates = @($names | Where-Object { $_ -like "assets/$namespace/blockstates/*.json" })
    $ids = @($blockstates | ForEach-Object { [IO.Path]::GetFileNameWithoutExtension($_) })
    $families = @($ids | ForEach-Object { $_ -replace '_(slab|stairs|wall)$', '' } | Sort-Object -Unique)
    $jsonFailures = @()
    $itemTintFailures = @()
    $wallTagValues = @()
    foreach ($entry in $archive.Entries | Where-Object { $_.FullName.EndsWith('.json') }) {
        $reader = [IO.StreamReader]::new($entry.Open())
        try {
            $json = $reader.ReadToEnd() | ConvertFrom-Json
            if ($entry.FullName -eq 'data/minecraft/tags/block/walls.json') {
                $wallTagValues = @($json.values)
            }
            if ($entry.FullName -like "assets/$namespace/items/*.json") {
                $id = [IO.Path]::GetFileNameWithoutExtension($entry.FullName)
                $family = $id -replace '_(slab|stairs|wall)$', ''
                $expectedTint = switch ($family) {
                    'grass_block' { @{ type = 'minecraft:grass'; value = $null } }
                    { $_ -in @('oak_leaves', 'jungle_leaves', 'acacia_leaves', 'dark_oak_leaves') } { @{ type = 'minecraft:constant'; value = -12012264 } }
                    'mangrove_leaves' { @{ type = 'minecraft:constant'; value = -7158200 } }
                    'spruce_leaves' { @{ type = 'minecraft:constant'; value = -10380959 } }
                    'birch_leaves' { @{ type = 'minecraft:constant'; value = -8345771 } }
                    default { $null }
                }
                if ($null -ne $expectedTint) {
                    $tints = @($json.model.tints)
                    if ($tints.Count -ne 1 -or $tints[0].type -ne $expectedTint.type -or
                        ($null -ne $expectedTint.value -and $tints[0].value -ne $expectedTint.value)) {
                        $itemTintFailures += $entry.FullName
                    }
                }
            }
        } catch {
            $jsonFailures += $entry.FullName
        } finally {
            $reader.Dispose()
        }
    }

    $expectedWallTagValues = @($ids | Where-Object { $_.EndsWith('_wall') } |
        ForEach-Object { "$namespace`:$($_)" } | Sort-Object)
    $actualWallTagValues = @($wallTagValues | Where-Object { $_ -like "$namespace`:*_wall" } | Sort-Object)
    $wallTagComplete = $expectedWallTagValues.Count -eq 311 -and
        (Compare-Object $expectedWallTagValues $actualWallTagValues).Count -eq 0
    $expectedWoolItemTags = @(
        "data/$namespace/tags/item/wool_slabs.json",
        "data/$namespace/tags/item/wool_stairs.json",
        "data/$namespace/tags/item/wool_walls.json"
    )
    $actualWoolItemTags = @($names | Where-Object { $_ -in $expectedWoolItemTags } | Sort-Object)

    $result = [ordered]@{
        sha256 = (Get-FileHash -LiteralPath $resolvedJar -Algorithm SHA256).Hash
        blockstates = $blockstates.Count
        slabs = @($ids | Where-Object { $_.EndsWith('_slab') }).Count
        stairs = @($ids | Where-Object { $_.EndsWith('_stairs') }).Count
        walls = @($ids | Where-Object { $_.EndsWith('_wall') }).Count
        families = $families.Count
        item_definitions = @($names | Where-Object { $_ -like "assets/$namespace/items/*.json" }).Count
        item_models = @($names | Where-Object { $_ -like "assets/$namespace/models/item/*.json" }).Count
        block_models = @($names | Where-Object { $_ -like "assets/$namespace/models/block/*.json" }).Count
        loot_tables = @($names | Where-Object { $_ -like "data/$namespace/loot_table/blocks/*.json" }).Count
        recipes = @($names | Where-Object { $_ -like "data/$namespace/recipe/*.json" }).Count
        legacy_recipes = @($names | Where-Object { $_ -like "data/$namespace/recipes/*.json" }).Count
        recipe_advancements = @($names | Where-Object {
            $_ -match "^data/$namespace/advancements?/recipes(?:[./]|/).*\.json$"
        }).Count
        vanilla_recipe_overrides = @($names | Where-Object {
            $_ -match '^data/minecraft/recipes?/.*\.json$'
        }).Count
        wool_item_tags = $actualWoolItemTags.Count
        invalid_json = $jsonFailures.Count
        native_walls_tagged = $actualWallTagValues.Count
        walls_tag_complete = $wallTagComplete
        invalid_item_tints = $itemTintFailures.Count
    }
} finally {
    $archive.Dispose()
}

$result | ConvertTo-Json
if ($result.blockstates -ne 866 -or $result.slabs -ne 276 -or $result.stairs -ne 279 -or
    $result.walls -ne 311 -or $result.families -ne 311 -or $result.item_definitions -ne 867 -or
    $result.item_models -ne 867 -or $result.block_models -ne 6895 -or $result.loot_tables -ne 866 -or
    $result.recipes -ne 0 -or $result.legacy_recipes -ne 0 -or $result.recipe_advancements -ne 0 -or
    $result.vanilla_recipe_overrides -ne 0 -or $result.wool_item_tags -ne 3 -or $result.invalid_json -ne 0 -or
    -not $result.walls_tag_complete -or $result.invalid_item_tints -ne 0) {
    throw 'Canary accounting differs from the required resource/geometry baseline.'
}
