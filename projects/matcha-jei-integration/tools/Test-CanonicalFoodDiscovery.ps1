[CmdletBinding()]
param(
    [string] $ProjectRoot = (Split-Path -Parent $PSScriptRoot),
    [string] $WorkbenchRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..\..')).Path
)

$ErrorActionPreference = 'Stop'

function Assert-True {
    param([bool] $Condition, [string] $Message)
    if (-not $Condition) {
        throw $Message
    }
}

function Read-ZipJson {
    param([System.IO.Compression.ZipArchive] $Archive, [string] $Path)
    $entry = $Archive.GetEntry($Path)
    Assert-True ($null -ne $entry) "Frozen Matcha 1.12 archive is missing $Path"
    $reader = [System.IO.StreamReader]::new($entry.Open())
    try {
        return ($reader.ReadToEnd() | ConvertFrom-Json)
    } finally {
        $reader.Dispose()
    }
}

function Assert-HealthFoodShape {
    param([object] $Recipe, [string] $Name)
    $components = $Recipe.result.components
    Assert-True ($null -ne $components.'minecraft:consumable') "$Name does not override minecraft:consumable"
    Assert-True (@($components.'minecraft:consumable'.on_consume_effects).Count -gt 0) `
        "$Name lacks minecraft:consumable.on_consume_effects"
}

Add-Type -AssemblyName System.IO.Compression.FileSystem
$archivePath = Join-Path $WorkbenchRoot 'originals\datapacks\Matcha_Flavoured_1_12.zip'
Assert-True (Test-Path -LiteralPath $archivePath -PathType Leaf) "Missing frozen Matcha 1.12 archive: $archivePath"
$archive = [System.IO.Compression.ZipFile]::OpenRead($archivePath)
try {
    $bread = Read-ZipJson $archive 'data/food/recipe/bread.json'
    $bakedPotato = Read-ZipJson $archive 'data/food/recipe/baked_potato.json'
    $cookedPufferfish = Read-ZipJson $archive 'data/food/recipe/cooked_pufferfish.json'

    Assert-HealthFoodShape $bread 'bread.json'
    Assert-HealthFoodShape $bakedPotato 'baked_potato.json'
    Assert-HealthFoodShape $cookedPufferfish 'cooked_pufferfish.json'
    Assert-True ($bread.result.id -ceq 'minecraft:bread') 'bread.json must retain the bread base item'
    Assert-True ($bread.result.components.'minecraft:item_model' -ceq 'minecraft:bread') `
        'bread.json must retain the default bread model'
    Assert-True ($null -eq $bakedPotato.result.components.'minecraft:item_model') `
        'baked_potato.json must exercise the no-model-override form'
    Assert-True ($cookedPufferfish.result.id -ceq 'minecraft:cooked_cod') `
        'cooked_pufferfish.json must exercise a shared food base'
    Assert-True ($cookedPufferfish.result.components.'minecraft:item_model' -ceq 'minecraft:cooked_pufferfish') `
        'cooked_pufferfish.json must remain a distinct custom-model variant'

    $glowBerries = Read-ZipJson $archive 'data/minecraft/loot_table/food/glow_berries.json'
    $glowFunctions = @($glowBerries.pools[0].entries[0].functions)
    $glowComponents = @($glowFunctions | Where-Object { $_.function -ceq 'minecraft:set_components' })[0].components
    Assert-True ($null -eq $glowComponents.'minecraft:food') `
        'glow_berries must retain its effect-only consumable form as a regression fixture'
    Assert-True (@($glowComponents.'minecraft:consumable'.on_consume_effects).Count -gt 0) `
        'glow_berries must retain Matcha consume effects as a regression fixture'

    $glowMash = Read-ZipJson $archive 'data/food/recipe/glow_mash.json'
    Assert-HealthFoodShape $glowMash 'glow_mash.json'
    Assert-True ($glowMash.result.id -ceq 'minecraft:rotten_flesh') `
        'glow_mash.json must retain the rotten_flesh carrier as a regression fixture'
} finally {
    $archive.Dispose()
}

$replacementSource = Get-Content -Raw (Join-Path $ProjectRoot 'src\main\java\dev\resivore\matchajei\client\MatchaCanonicalFoodReplacements.java')
$creativeCatalogSource = Get-Content -Raw (Join-Path $ProjectRoot 'src\main\java\dev\resivore\matchajei\client\MatchaCreativeCatalog.java')
$creativeSource = Get-Content -Raw (Join-Path $ProjectRoot 'src\main\java\dev\resivore\matchajei\client\MatchaCreativeSearchEntries.java')
$jeiSource = Get-Content -Raw (Join-Path $ProjectRoot 'src\main\java\dev\resivore\matchajei\client\MatchaJeiRuntimeData.java')

Assert-True ($replacementSource -match 'DataComponents\.CONSUMABLE') 'C6 replacement predicate no longer requires minecraft:consumable.'
Assert-True ($replacementSource -match 'onConsumeEffects\(\)\.isEmpty\(\)') 'C6 replacement predicate no longer requires consume effects.'
Assert-True ($replacementSource -match 'components\(\)\.has\(DataComponents\.FOOD\)') `
    'C6 replacement predicate no longer limits itself to ordinary food bases.'
Assert-True ($replacementSource -notmatch 'Optional<FoodProperties> food') `
    'C7 must not require a minecraft:food patch; Glow Berries only patch consumable.'
Assert-True ($replacementSource -match 'DataComponents\.ITEM_MODEL') `
    'C6 replacement predicate no longer preserves custom-model food variants.'
Assert-True ($creativeCatalogSource -match 'SUPPRESSED_DEFAULT_SEARCH_ENTRIES') `
    'Creative suppression ownership is not tracked separately from Matcha contributions.'
Assert-True ($creativeCatalogSource -match 'MODIFY_OUTPUT_ALL') `
    'C7 must filter category Search sources before vanilla indexes the global Search tab.'
Assert-True ($creativeCatalogSource -match 'END_CLIENT_TICK') `
    'C7 must retry payload-driven Search replacement after the play client is ready.'
Assert-True ($creativeSource -notmatch 'tryRebuildTabContents|buildContents\(|getDisplayItems\(\)\.clear') `
    'C6 must not reintroduce a global Creative/Search rebuild or clear.'
Assert-True ($jeiSource -match 'removeCanonicalFoodDefaults') 'JEI does not consume the C6 default-food predicate.'
Assert-True ($jeiSource -match 'suppressedVanilla') 'JEI suppressed defaults are not retained for restoration.'

Write-Host 'C6 canonical food discovery verification passed.'
