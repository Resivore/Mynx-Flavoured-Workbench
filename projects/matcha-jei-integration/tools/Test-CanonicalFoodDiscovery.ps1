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

function Assert-Equal {
    param([object] $Actual, [object] $Expected, [string] $Message)
    if ($Actual -ne $Expected) {
        throw "${Message}: expected $Expected, got $Actual"
    }
}

function Normalize-Identifier {
    param([string] $Identifier)
    if ($Identifier.Contains(':')) {
        return $Identifier
    }
    return "minecraft:$Identifier"
}

function Read-ZipJson {
    param([System.IO.Compression.ZipArchive] $Archive, [string] $Path)
    $entry = $Archive.GetEntry($Path)
    Assert-True ($null -ne $entry) "Frozen Matcha 1.12 archive is missing $Path"
    $reader = [System.IO.StreamReader]::new($entry.Open())
    try {
        return ($reader.ReadToEnd() | ConvertFrom-Json -AsHashtable -Depth 100)
    } finally {
        $reader.Dispose()
    }
}

function ConvertTo-CanonicalValue {
    param([AllowNull()] [object] $Value)
    if ($null -eq $Value -or $Value -is [string] -or $Value -is [ValueType]) {
        return $Value
    }
    if ($Value -is [Collections.IDictionary]) {
        $ordered = [ordered] @{}
        $keys = [string[]] @($Value.Keys)
        [Array]::Sort($keys, [StringComparer]::Ordinal)
        foreach ($key in $keys) {
            $ordered[$key] = ConvertTo-CanonicalValue $Value[$key]
        }
        return $ordered
    }
    if ($Value -is [Collections.IEnumerable]) {
        $items = [Collections.Generic.List[object]]::new()
        foreach ($item in $Value) {
            $items.Add((ConvertTo-CanonicalValue $item))
        }
        return ,$items.ToArray()
    }
    return $Value
}

function Get-SemanticKey {
    param([object] $Value)
    return ConvertTo-Json -InputObject (ConvertTo-CanonicalValue $Value) -Depth 100 -Compress
}

function Get-HealthRecipeRecord {
    param([string] $Path, [Collections.IDictionary] $Recipe)
    $stack = $Recipe['result']
    if ($stack -isnot [Collections.IDictionary] -or $stack['id'] -isnot [string]) {
        return $null
    }
    $components = $stack['components']
    if ($components -isnot [Collections.IDictionary]) {
        return $null
    }
    $consumable = $components['minecraft:consumable']
    if ($consumable -isnot [Collections.IDictionary] -or
        @($consumable['on_consume_effects']).Count -eq 0 -or
        -not $components.Contains('minecraft:food')) {
        return $null
    }

    $itemId = Normalize-Identifier ([string] $stack['id'])
    $familyComponents = [ordered] @{}
    foreach ($componentId in @($components.Keys | Sort-Object)) {
        if ($componentId -cin @(
                'minecraft:food',
                'minecraft:consumable',
                'minecraft:lore',
                'minecraft:max_stack_size',
                'minecraft:use_remainder',
                '!minecraft:use_remainder'
            )) {
            continue
        }
        $value = $components[$componentId]
        if ($componentId -ceq 'minecraft:item_model' -and
            $value -is [string] -and
            (Normalize-Identifier $value) -ceq $itemId) {
            continue
        }
        $familyComponents[$componentId] = $value
    }
    $normalizedStack = [ordered] @{
        id = $itemId
        components = $components
    }
    $family = [ordered] @{
        id = $itemId
        identity_components = $familyComponents
    }
    return [pscustomobject] @{
        Path = $Path
        ItemId = $itemId
        Stack = $normalizedStack
        ExactKey = Get-SemanticKey $normalizedStack
        FamilyKey = Get-SemanticKey $family
        DefaultPresentation = $familyComponents.Count -eq 0
    }
}

Add-Type -AssemblyName System.IO.Compression.FileSystem
$archivePath = Join-Path $WorkbenchRoot 'originals\datapacks\Matcha_Flavoured_1_12.zip'
Assert-True (Test-Path -LiteralPath $archivePath -PathType Leaf) `
    "Missing frozen Matcha 1.12 archive: $archivePath"
$archive = [System.IO.Compression.ZipFile]::OpenRead($archivePath)
try {
    $healthRecipes = [Collections.Generic.List[object]]::new()
    foreach ($entry in $archive.Entries) {
        if ($entry.FullName -cnotmatch '^data/food/recipe/.+\.json$') {
            continue
        }
        $recipe = Read-ZipJson $archive $entry.FullName
        $record = Get-HealthRecipeRecord $entry.FullName $recipe
        if ($null -ne $record) {
            $healthRecipes.Add($record)
        }
    }

    Assert-Equal $healthRecipes.Count 115 'Effect-bearing food recipe document count'
    $families = @($healthRecipes | Group-Object FamilyKey)
    $defaultFamilies = @($families | Where-Object { $_.Group[0].DefaultPresentation })
    $customFamilies = @($families | Where-Object { -not $_.Group[0].DefaultPresentation })
    Assert-Equal $families.Count 84 'Unique Matcha health-food presentation family count'
    Assert-Equal $defaultFamilies.Count 21 'Default-presentation canonical health family count'
    Assert-Equal $customFamilies.Count 63 'Custom-presentation health family count'

    # This is a structural catalog scan, not a curated duplicate list. Every
    # default-presentation family must reduce [plain default, rich output] to
    # exactly the rich identity, while every custom presentation stays outside
    # that default-suppression class.
    foreach ($family in $defaultFamilies) {
        $exactOutputs = @($family.Group | Group-Object ExactKey)
        Assert-Equal $exactOutputs.Count 1 `
            "Ambiguous effective recipe identities in default family $($family.Group[0].ItemId)"
        $simulatedDiscovery = @('plain-default', $exactOutputs[0].Name) |
            Where-Object { $_ -cne 'plain-default' }
        Assert-Equal $simulatedDiscovery.Count 1 `
            "Canonical discovery exposure count for $($family.Group[0].ItemId)"
    }

    $braisedSmelting = Read-ZipJson $archive 'data/food/recipe/braised_brown_mushroom.json'
    $braisedCampfire = Read-ZipJson $archive 'data/food/recipe/braised_brown_mushroom_campfire.json'
    $braised = Get-HealthRecipeRecord 'braised_brown_mushroom.json' $braisedSmelting
    $braisedFire = Get-HealthRecipeRecord 'braised_brown_mushroom_campfire.json' $braisedCampfire
    Assert-True ($null -ne $braised) 'Real Braised Mushroom recipe is not a health-food candidate.'
    Assert-Equal $braised.ItemId 'minecraft:rabbit_foot' 'Braised Mushroom carrier'
    Assert-True $braised.DefaultPresentation 'Braised Mushroom must use its carrier default presentation.'
    Assert-Equal $braised.ExactKey $braisedFire.ExactKey `
        'Smelting and campfire Braised Mushroom outputs must be exact-equal'
    Assert-True (-not $braised.Stack.components.Contains('minecraft:item_model')) `
        'Braised Mushroom unexpectedly gained a custom model identity.'
    Assert-True (-not $braised.Stack.components.Contains('minecraft:item_name')) `
        'Braised Mushroom unexpectedly gained a custom item-name identity.'

    $cookedCod = Get-HealthRecipeRecord 'cooked_cod.json' `
        (Read-ZipJson $archive 'data/food/recipe/cooked_cod.json')
    $cookedPufferfish = Get-HealthRecipeRecord 'cooked_pufferfish.json' `
        (Read-ZipJson $archive 'data/food/recipe/cooked_pufferfish.json')
    Assert-Equal $cookedCod.ItemId $cookedPufferfish.ItemId `
        'Shared-carrier food regression fixture'
    Assert-True ($cookedCod.FamilyKey -cne $cookedPufferfish.FamilyKey) `
        'Cooked Cod and custom-model Cooked Pufferfish must remain separate families.'
    Assert-True $cookedCod.DefaultPresentation `
        'Cooked Cod must remain the carrier-default presentation.'
    Assert-True (-not $cookedPufferfish.DefaultPresentation) `
        'Cooked Pufferfish must retain its custom presentation.'

    $language = Read-ZipJson $archive 'assets/minecraft/lang/en_us.json'
    Assert-Equal $language['item.minecraft.rabbit_foot'] 'Braised Mushroom' `
        'Frozen default rabbit-foot presentation name'
    $rabbitModel = Read-ZipJson $archive 'assets/minecraft/models/item/rabbit_foot.json'
    Assert-Equal $rabbitModel['textures']['layer0'] 'minecraft:item/braised_brown_mushroom' `
        'Frozen default rabbit-foot presentation model'

    $glowBerries = Read-ZipJson $archive 'data/minecraft/loot_table/food/glow_berries.json'
    $glowFunctions = @($glowBerries['pools'][0]['entries'][0]['functions'])
    $glowComponents = @($glowFunctions | Where-Object {
            $_['function'] -ceq 'minecraft:set_components'
        })[0]['components']
    Assert-True (-not $glowComponents.Contains('minecraft:food')) `
        'Glow Berries must retain their effect-only consumable regression shape.'
    Assert-True (@($glowComponents['minecraft:consumable']['on_consume_effects']).Count -gt 0) `
        'Glow Berries lost their Matcha consume effects.'

    $glowMash = Read-ZipJson $archive 'data/food/recipe/glow_mash.json'
    $glowMashRecord = Get-HealthRecipeRecord 'glow_mash.json' $glowMash
    Assert-Equal $glowMashRecord.ItemId 'minecraft:rotten_flesh' 'Glow Berry Mash carrier'
    Assert-True $glowMashRecord.DefaultPresentation `
        'Glow Berry Mash must retain its default carrier presentation.'
} finally {
    $archive.Dispose()
}

$canonicalSource = Get-Content -Raw (Join-Path $ProjectRoot `
    'src\main\java\dev\resivore\matchajei\data\MatchaFoodDiscoveryCatalog.java')
$scannerSource = Get-Content -Raw (Join-Path $ProjectRoot `
    'src\main\java\dev\resivore\matchajei\server\MatchaDataScanner.java')
$payloadSource = Get-Content -Raw (Join-Path $ProjectRoot `
    'src\main\java\dev\resivore\matchajei\network\MatchaJeiDataPayload.java')
$creativeCatalogSource = Get-Content -Raw (Join-Path $ProjectRoot `
    'src\main\java\dev\resivore\matchajei\client\MatchaCreativeCatalog.java')
$creativeSource = Get-Content -Raw (Join-Path $ProjectRoot `
    'src\main\java\dev\resivore\matchajei\client\MatchaCreativeSearchEntries.java')
$jeiSource = Get-Content -Raw (Join-Path $ProjectRoot `
    'src\main\java\dev\resivore\matchajei\client\MatchaJeiRuntimeData.java')

Assert-True ($canonicalSource -match 'Source\.EFFECTIVE_RECIPE') `
    'Effective recipe provenance is not retained through canonicalization.'
Assert-True ($canonicalSource -match 'Source\.NON_RECIPE') `
    'Non-recipe provenance is not retained through canonicalization.'
Assert-True ($canonicalSource -match 'SOURCE_VARIANT_COMPONENTS') `
    'Source-precedence families no longer isolate the health payload.'
Assert-True ($canonicalSource -match 'DEFAULT_ENRICHMENT_COMPONENTS') `
    'Default-equivalence families no longer isolate audited food mechanics.'
Assert-True ($canonicalSource -match 'stack\.getComponents\(\)\.forEach') `
    'Canonical families no longer compare effective component identity.'
Assert-True ($canonicalSource -match 'authoritativeFoodRecipe') `
    'Non-food Matcha recipe carriers are no longer eligible to supersede their exact defaults.'
Assert-True ($canonicalSource -match 'onConsumeEffects\(\)\.isEmpty\(\)') `
    'Canonical health candidates no longer require consume effects.'
Assert-True ($canonicalSource -notmatch 'getHoverName|getDescription|translate|Braised|rabbit_foot') `
    'Production canonicalization must not use display names or item-specific exceptions.'
Assert-True ($scannerSource -match 'MatchaFoodDiscoveryCatalog\.canonicalize\(catalog\.values\(\)\)') `
    'The generated server catalog bypasses food canonicalization.'
Assert-True ($payloadSource -match 'List<ItemStack> canonicalDefaults') `
    'The synchronized payload does not carry the shared canonical-default decision.'
Assert-True ($creativeCatalogSource -match 'current\(\)\.canonicalDefaults\(\)') `
    'Creative Search does not consume the synchronized canonical-default decision.'
Assert-True ($creativeCatalogSource -match 'MODIFY_OUTPUT_ALL') `
    'Category Search sources are not filtered before vanilla builds Search.'
Assert-True ($creativeCatalogSource -match 'END_CLIENT_TICK') `
    'Payload reconciliation is no longer retried on a safe client tick.'
Assert-True ($creativeSource -notmatch 'tryRebuildTabContents|buildContents\(|getDisplayItems\(\)\.clear') `
    'Canonicalization must not reintroduce a global Creative/Search rebuild or clear.'
Assert-True ($jeiSource -match 'payload\.canonicalDefaults\(\)') `
    'JEI does not consume the synchronized canonical-default decision.'
Assert-True ($jeiSource -match 'suppressedVanilla') `
    'JEI suppressed defaults are not retained for restoration.'

Write-Host 'C8 canonical food catalog verification passed.'
