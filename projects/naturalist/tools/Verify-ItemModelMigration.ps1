[CmdletBinding()]
param(
    [Parameter()]
    [string]$OriginalJar,

    [Parameter()]
    [string]$LegacyMinecraftJar
)

$ErrorActionPreference = 'Stop'

$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$expectedJarName = 'naturalist-2.0.3-fabric-1.21.1.jar'
$expectedJarSha256 = '3d16c975326e0df24486d44d8010d9e614fc9efdf891864de7b5a0efedfc12f9'
$legacySpawnEggResourceHashes = [ordered]@{
    'assets/minecraft/models/item/template_spawn_egg.json' =
        '2aa28f3dfc96c06980df9a6242eca2a375b18e9c5f4fa68771de3c88dcbe4e1f'
    'assets/minecraft/textures/item/spawn_egg.png' =
        'f6b985a0094408770f4b0ba919c2cb6365c6e8b65080334b33617518be2089c1'
    'assets/minecraft/textures/item/spawn_egg_overlay.png' =
        'df0075d5081a1f49f0f4de1e1f004764446ecb3233416f7cd027c58823190747'
}

function Assert-Equal {
    param($Actual, $Expected, [string]$Label)
    if ($Actual -ne $Expected) {
        throw "$Label mismatch: expected '$Expected', found '$Actual'."
    }
    Write-Host "PASS  $Label = $Expected"
}

function Assert-SetEqual {
    param([string[]]$Actual, [string[]]$Expected, [string]$Label)
    $actualSet = @($Actual | Sort-Object -Unique)
    $expectedSet = @($Expected | Sort-Object -Unique)
    $missing = @($expectedSet | Where-Object { $_ -notin $actualSet })
    $unexpected = @($actualSet | Where-Object { $_ -notin $expectedSet })
    if ($missing.Count -ne 0 -or $unexpected.Count -ne 0) {
        throw "$Label mismatch. Missing: [$($missing -join ', ')]. Unexpected: [$($unexpected -join ', ')]."
    }
    Write-Host "PASS  $Label has the expected $($expectedSet.Count) entries"
}

function Assert-Match {
    param([string]$Text, [string]$Pattern, [string]$Label)
    if ($Text -notmatch $Pattern) {
        throw "$Label is missing the required pattern: $Pattern"
    }
    Write-Host "PASS  $Label"
}

function Assert-NoMatch {
    param([string]$Text, [string]$Pattern, [string]$Label)
    if ($Text -match $Pattern) {
        throw "$Label still contains retired pattern: $Pattern"
    }
    Write-Host "PASS  $Label"
}

function Assert-Empty {
    param([object[]]$Values, [string]$Label)
    $actual = @($Values | Where-Object { $null -ne $_ -and "$_" -ne '' })
    if ($actual.Count -ne 0) {
        throw "$Label must be empty, but found: $($actual -join ', ')"
    }
    Write-Host "PASS  $Label is empty"
}

function Read-ZipJson {
    param([IO.Compression.ZipArchive]$Zip, [string]$Path)
    $entry = $Zip.GetEntry($Path)
    if ($null -eq $entry) {
        throw "Missing protected upstream entry: $Path"
    }
    $reader = [IO.StreamReader]::new($entry.Open())
    try {
        return $reader.ReadToEnd() | ConvertFrom-Json
    }
    finally {
        $reader.Dispose()
    }
}

function Get-ZipEntrySha256 {
    param([IO.Compression.ZipArchive]$Zip, [string]$Path)
    $entry = $Zip.GetEntry($Path)
    if ($null -eq $entry) {
        throw "Missing legacy Minecraft entry: $Path"
    }
    $sha256 = [Security.Cryptography.SHA256]::Create()
    $stream = $entry.Open()
    try {
        return [Convert]::ToHexString($sha256.ComputeHash($stream)).ToLowerInvariant()
    }
    finally {
        $stream.Dispose()
        $sha256.Dispose()
    }
}

if ([string]::IsNullOrWhiteSpace($OriginalJar)) {
    $candidatePaths = @(
        (Join-Path $projectRoot "..\..\originals\mods\$expectedJarName"),
        (Join-Path $projectRoot "..\..\..\..\..\originals\mods\$expectedJarName")
    )
    $OriginalJar = $candidatePaths |
        ForEach-Object { [IO.Path]::GetFullPath($_) } |
        Where-Object { Test-Path -LiteralPath $_ -PathType Leaf } |
        Select-Object -First 1
}
if ([string]::IsNullOrWhiteSpace($OriginalJar) -or -not (Test-Path -LiteralPath $OriginalJar -PathType Leaf)) {
    throw "Original JAR not found. Pass -OriginalJar with the path to $expectedJarName."
}
$OriginalJar = (Resolve-Path -LiteralPath $OriginalJar).Path
Assert-Equal (Get-FileHash -LiteralPath $OriginalJar -Algorithm SHA256).Hash.ToLowerInvariant() `
    $expectedJarSha256 'original JAR SHA-256'

if ([string]::IsNullOrWhiteSpace($LegacyMinecraftJar) -or
        -not (Test-Path -LiteralPath $LegacyMinecraftJar -PathType Leaf)) {
    throw 'Minecraft 1.21.1 client JAR not found. Pass -LegacyMinecraftJar with its absolute path.'
}
$LegacyMinecraftJar = (Resolve-Path -LiteralPath $LegacyMinecraftJar).Path

$registrySource = Get-Content -LiteralPath (Join-Path $projectRoot `
    'common\src\main\java\com\crispytwig\naturalist\registry\NaturalistRegistry.java') -Raw
$spawnEggPattern = 'registerItem\("(?<id>[a-z0-9_]+_spawn_egg)",\s*properties\s*->\s*' +
    'Services\.REGISTRY\.createSpawnEgg\([^,]+,\s*(?<primary>\d+),\s*' +
    '(?<secondary>\d+),\s*properties\)\)'
$spawnEggMatches = [regex]::Matches($registrySource, $spawnEggPattern,
    [Text.RegularExpressions.RegexOptions]::Singleline)
Assert-Equal $spawnEggMatches.Count 46 'spawn-egg color registration count'
$spawnEggColors = [ordered]@{}
foreach ($match in $spawnEggMatches) {
    $itemId = $match.Groups['id'].Value
    if ($spawnEggColors.Contains($itemId)) {
        throw "Duplicate spawn-egg color registration: $itemId"
    }
    $primaryRgb = [int]$match.Groups['primary'].Value
    $secondaryRgb = [int]$match.Groups['secondary'].Value
    foreach ($color in @($primaryRgb, $secondaryRgb)) {
        if ($color -lt 0 -or $color -gt 0xFFFFFF) {
            throw "Spawn-egg RGB value is outside 24-bit range for ${itemId}: $color"
        }
    }
    $spawnEggColors[$itemId] = [pscustomobject]@{
        PrimaryRgb = $primaryRgb
        SecondaryRgb = $secondaryRgb
        PrimaryArgb = $primaryRgb - 0x1000000
        SecondaryArgb = $secondaryRgb - 0x1000000
    }
}
$spawnEggIds = @($spawnEggColors.Keys)

Add-Type -AssemblyName System.IO.Compression.FileSystem -ErrorAction SilentlyContinue
$legacyZip = [IO.Compression.ZipFile]::OpenRead($LegacyMinecraftJar)
try {
    foreach ($resourcePath in $legacySpawnEggResourceHashes.Keys) {
        Assert-Equal (Get-ZipEntrySha256 $legacyZip $resourcePath) `
            $legacySpawnEggResourceHashes[$resourcePath] "$resourcePath SHA-256"
    }
}
finally {
    $legacyZip.Dispose()
}

$wrappedItemIds = @(
    'catfish_bucket', 'bass_bucket', 'duck_bucket', 'crab', 'caterpillar', 'butterfly',
    'rat', 'scorpion', 'hedgehog', 'snail', 'starfish_bucket',
    'giant_isopod_bucket', 'anglerfish_bucket', 'jellyfish_bucket', 'ray_bucket',
    'blobfish_bucket', 'piranha_bucket'
)
$retainedInventoryItemIds = @(
    'glow_goop', 'capture_net', 'music_disc_wild_ones', 'music_disc_death_by_hogs', 'duck_egg',
    'chrysalis', 'red_starfish', 'orange_starfish', 'blue_starfish', 'purple_starfish',
    'alligator_egg', 'tortoise_egg', 'ostrich_egg', 'snail_eggs'
)
$compatibilityItemIds = @($wrappedItemIds + @('knapsack') + $retainedInventoryItemIds)
$variantRegistryCounts = [ordered]@{
    anglerfish = 1
    butterfly = 6
    crab = 4
    giant_isopod = 1
    hedgehog = 3
    jellyfish = 4
    rat = 2
    ray = 2
    starfish = 3
}
$dyeNames = @(
    'white', 'orange', 'magenta', 'light_blue', 'yellow', 'lime', 'pink', 'gray',
    'light_gray', 'cyan', 'purple', 'blue', 'brown', 'green', 'red', 'black'
)

Add-Type -AssemblyName System.IO.Compression.FileSystem -ErrorAction SilentlyContinue
$zip = [IO.Compression.ZipFile]::OpenRead($OriginalJar)
try {
    $spawnEggModels = @($zip.Entries | Where-Object {
        $_.FullName -match '^assets/naturalist/models/item/([a-z0-9_]+_spawn_egg)\.json$'
    })
    Assert-Equal $spawnEggModels.Count 47 'protected upstream spawn-egg model count'
    $protectedSpawnEggIds = @($spawnEggModels | ForEach-Object {
        [regex]::Match($_.FullName,
            '^assets/naturalist/models/item/([a-z0-9_]+_spawn_egg)\.json$').Groups[1].Value
    })
    Assert-SetEqual $protectedSpawnEggIds @($spawnEggIds + 'ant_spawn_egg') `
        'protected upstream spawn-egg model ids'
    foreach ($itemId in $spawnEggIds) {
        $spawnEggModel = Read-ZipJson $zip "assets/naturalist/models/item/$itemId.json"
        Assert-Equal ([string]$spawnEggModel.parent) 'minecraft:item/template_spawn_egg' `
            "$itemId legacy template parent"
    }

    $variantModels = @($zip.Entries | Where-Object {
        $_.FullName -match '^assets/naturalist/models/item/variant/.+\.json$'
    })
    Assert-Equal $variantModels.Count 34 'protected variant cuboid model count'

    $variantDefinitions = @($zip.Entries | Where-Object {
        $_.FullName -match '^data/naturalist/naturalist/([a-z0-9_]+)_variant/[^/]+\.json$'
    })
    $itemModelLinks = @()
    foreach ($entry in $variantDefinitions) {
        $json = Read-ZipJson $zip $entry.FullName
        if ($null -ne $json.item_model) {
            $registry = [regex]::Match($entry.FullName,
                '^data/naturalist/naturalist/([a-z0-9_]+)_variant/').Groups[1].Value
            $itemModelLinks += [pscustomobject]@{
                Registry = $registry
                Model = [string]$json.item_model
            }
        }
    }
    Assert-Equal $itemModelLinks.Count 26 'variant definitions with item_model'
    Assert-SetEqual @($itemModelLinks.Registry) @($variantRegistryCounts.Keys) `
        'variant registries with item-model selection'
    foreach ($registry in $variantRegistryCounts.Keys) {
        Assert-Equal @($itemModelLinks | Where-Object Registry -eq $registry).Count `
            $variantRegistryCounts[$registry] "$registry item_model link count"
    }
    foreach ($link in $itemModelLinks) {
        $parts = $link.Model -split ':', 2
        $modelEntry = "assets/$($parts[0])/models/$($parts[1]).json"
        if ($null -eq $zip.GetEntry($modelEntry)) {
            throw "Variant item_model '$($link.Model)' does not resolve to $modelEntry."
        }
    }
    Write-Host 'PASS  all variant item_model links resolve without flattening identifiers'

    $propertyTargets = @()
    foreach ($itemId in @('snail')) {
        $json = Read-ZipJson $zip "assets/naturalist/models/item/$itemId.json"
        Assert-Equal @($json.overrides).Count 16 "$itemId color override count"
        for ($index = 0; $index -lt 16; ++$index) {
            $override = @($json.overrides)[$index]
            $propertyNames = @($override.predicate.PSObject.Properties.Name)
            Assert-SetEqual $propertyNames @('minecraft:color') "$itemId color property at index $index"
            $actualThreshold = [double]$override.predicate.'minecraft:color'
            $expectedThreshold = $index / 15.0
            if ([Math]::Abs($actualThreshold - $expectedThreshold) -gt 0.000001) {
                throw "$itemId color threshold $index mismatch: expected $expectedThreshold, found $actualThreshold."
            }
            $expectedModel = "naturalist:item/$itemId/$($dyeNames[$index])"
            Assert-Equal ([string]$override.model) $expectedModel "$itemId color model at index $index"
            $propertyTargets += $expectedModel
        }
    }
    $knapsack = Read-ZipJson $zip 'assets/naturalist/models/item/knapsack.json'
    Assert-Equal @($knapsack.overrides).Count 1 'knapsack override count'
    Assert-Equal ([double]$knapsack.overrides[0].predicate.'naturalist:filled') 1.0 `
        'knapsack filled threshold'
    Assert-Equal ([string]$knapsack.overrides[0].model) 'naturalist:item/knapsack_filled' `
        'knapsack filled model'
    $propertyTargets += 'naturalist:item/knapsack_filled'
    Assert-Equal @($propertyTargets | Sort-Object -Unique).Count 17 'numeric-property target model count'
    foreach ($model in $propertyTargets) {
        $parts = $model -split ':', 2
        if ($null -eq $zip.GetEntry("assets/$($parts[0])/models/$($parts[1]).json")) {
            throw "Property target '$model' is missing from the protected upstream corpus."
        }
    }
    Write-Host 'PASS  all protected numeric-property target models resolve'

    foreach ($itemId in $compatibilityItemIds) {
        if ($null -eq $zip.GetEntry("assets/naturalist/models/item/$itemId.json")) {
            throw "Generated client-item root '$itemId' has no protected legacy cuboid model."
        }
    }
    Write-Host 'PASS  all generated client-item roots reference protected legacy cuboid models'
}
finally {
    $zip.Dispose()
}

foreach ($itemId in $wrappedItemIds) {
    Assert-Match $registrySource ('registerItem\("' + [regex]::Escape($itemId) + '"') `
        "wrapped item registration $itemId"
}
$fabricRegistryHelperSource = Get-Content -LiteralPath (Join-Path $projectRoot `
    'fabric\src\main\java\com\crispytwig\naturalist\fabric\platform\FabricRegistryHelper.java') -Raw
Assert-Match $fabricRegistryHelperSource `
    'new\s+SpawnEggItem\(properties\.spawnEgg\(type\.get\(\)\)\)' `
    '26.2 spawn-egg entity binding'

$variantSource = Get-Content -LiteralPath (Join-Path $projectRoot `
    'common\src\main\java\com\crispytwig\naturalist\client\model\item\VariantItemModels.java') -Raw
Assert-Match $variantSource 'instanceof\s+NaturalistBucketItem' 'registry-wide NaturalistBucketItem collector'
Assert-Match $variantSource 'MODEL_FOLDER\s*=\s*"item/variant"' 'protected variant-model scan folder'
Assert-Match $variantSource 'DataComponents\.CUSTOM_DATA' 'variant selection custom-data input'
Assert-Match $variantSource 'readVariantId\(customData\.copyTag\(\)' 'string/legacy variant reader input'
Assert-Match $variantSource 'registryAccess\(\)' 'world registry variant lookup'
Assert-Match $variantSource '\.itemModel\(\)' 'data-driven MobVariant item_model selection'

$wrapperSource = Get-Content -LiteralPath (Join-Path $projectRoot `
    'common\src\main\java\com\crispytwig\naturalist\client\model\item\VariantAwareItemModel.java') -Raw
Assert-Match $wrapperSource 'implements\s+ItemModel' '26.2 variant ItemModel adapter'
Assert-Match $wrapperSource 'appendModelIdentityElement\(this\)' 'variant model identity tracking'
Assert-Match $wrapperSource 'selected\s*!=\s*null\s*\?\s*selected\s*:\s*this\.parent' `
    'variant parent fallback'
Assert-Match $wrapperSource '\.update\(renderState, stack, resolver, displayContext, level, owner, seed\)' `
    'variant evaluation-context forwarding'

$propertySource = Get-Content -LiteralPath (Join-Path $projectRoot `
    'common\src\main\java\com\crispytwig\naturalist\NaturalistClient.java') -Raw
Assert-Match $propertySource 'Identifier\.withDefaultNamespace\("color"\)' 'minecraft:color identifier'
Assert-Match $propertySource 'Naturalist\.location\("filled"\)' 'naturalist:filled identifier'
Assert-Match $propertySource 'copyTag\(\)\.getIntOr\("Color",\s*0\)\s*/\s*15\.0F' `
    'Color custom-data numeric input and scale'
Assert-Match $propertySource 'KnapsackItem\.isFilled\(stack\)' 'exact knapsack filled predicate'

$propertyRegistrySource = Get-Content -LiteralPath (Join-Path $projectRoot `
    'common\src\main\java\com\crispytwig\naturalist\client\model\item\NaturalistItemModelProperties.java') -Raw
Assert-Match $propertyRegistrySource 'Mth\.clamp\(this\.unclampedCall\([\s\S]+0\.0F,\s*1\.0F\)' `
    'legacy numeric-property clamping contract'

$propertyWrapperSource = Get-Content -LiteralPath (Join-Path $projectRoot `
    'common\src\main\java\com\crispytwig\naturalist\client\model\item\PropertyAwareItemModel.java') -Raw
Assert-Match $propertyWrapperSource 'implements\s+ItemModel' '26.2 numeric-property ItemModel adapter'
Assert-Match $propertyWrapperSource 'appendModelIdentityElement\(this\)' 'numeric-property model identity tracking'
Assert-Match $propertyWrapperSource 'selected\s*!=\s*null\s*\?\s*selected\s*:\s*this\.parent' `
    'numeric-property parent fallback'
Assert-Match $propertyWrapperSource '\.update\(renderState, stack, resolver, displayContext, level, owner, seed\)' `
    'numeric-property evaluation-context forwarding'

$fabricSource = Get-Content -LiteralPath (Join-Path $projectRoot `
    'fabric\src\main\java\com\crispytwig\naturalist\fabric\client\NaturalistFabricClient.java') -Raw
Assert-Match $fabricSource 'sharedState\.resourceManager\(\)' '26.2 preparable reload resource manager'
Assert-Match $fabricSource 'context\.addModel\(' 'Fabric 26.2 extra-model registration'
Assert-Match $fabricSource 'modifyItemModelAfterBake\(\)' 'Fabric 26.2 item-model bake hook'
Assert-Match $fabricSource 'ctx\.itemId\(\)' 'Fabric direct item identifier context'
Assert-Match $fabricSource 'CuboidItemModelWrapper\.Unbaked' 'protected cuboid to 26.2 ItemModel adapter'
Assert-Match $fabricSource 'new\s+PropertyAwareItemModel' 'numeric-property model wrapper'
Assert-Match $fabricSource 'new\s+VariantAwareItemModel' 'variant model wrapper'
Assert-Match $fabricSource 'requiredModels\.addAll\(prepared\.variantModels\(\)\)' `
    'all scanned variant models supplied to variant-aware wrappers'
Assert-Match $fabricSource 'missingItemModel\(ctx\.transformation\(\)\)' `
    'explicit unavailable model IDs preserve Minecraft missing-model behavior'
Assert-Match $fabricSource 'NaturalistClient\.registerItemProperties\(\);' `
    'numeric item-property registration call'
Assert-Match $fabricSource 'registerVariantItemModels\(\);' 'dynamic item-model hook call'
if ($fabricSource.IndexOf('NaturalistClient.registerItemProperties();') -gt
        $fabricSource.IndexOf('registerVariantItemModels();')) {
    throw 'Numeric item properties must be registered before the model-loading hook snapshots them.'
}
Write-Host 'PASS  numeric item properties register before model-loading hook'
if ($fabricSource.IndexOf('new PropertyAwareItemModel') -gt
        $fabricSource.IndexOf('new VariantAwareItemModel')) {
    throw 'Property wrapper must remain inside the variant wrapper to preserve Snail fallback behavior.'
}
Write-Host 'PASS  property-inside-variant wrapper precedence'

$parserSource = Get-Content -LiteralPath (Join-Path $projectRoot `
    'common\src\main\java\com\crispytwig\naturalist\client\model\item\LegacyItemModelResources.java') -Raw
$overrideSource = Get-Content -LiteralPath (Join-Path $projectRoot `
    'common\src\main\java\com\crispytwig\naturalist\client\model\item\ItemModelOverrides.java') -Raw
$scopedSource = $variantSource + $wrapperSource + $propertySource + $fabricSource +
    $propertyRegistrySource + $propertyWrapperSource + $parserSource + $overrideSource
foreach ($retired in @(
    '\bBakedModel\b', '\bItemOverrides\b', '\bClampedItemPropertyFunction\b',
    '\bModelIdentifier\b', 'context\.addModels\(', 'modifyModelAfterBake\(',
    'topLevelId\(', 'getUnsafe\('
)) {
    Assert-NoMatch $scopedSource $retired "retired API guard $retired"
}

$buildSource = Get-Content -LiteralPath (Join-Path $projectRoot 'build.gradle') -Raw
$listMatch = [regex]::Match($buildSource,
    '(?s)def\s+dynamicClientItemModelIds\s*=\s*\[(?<items>.*?)\]')
if (-not $listMatch.Success) {
    throw 'build.gradle is missing dynamicClientItemModelIds.'
}
$generatedIds = @([regex]::Matches($listMatch.Groups['items'].Value, "'([a-z0-9_]+)'") |
    ForEach-Object { $_.Groups[1].Value })
Assert-SetEqual $generatedIds $compatibilityItemIds 'generated dynamic client-item root ids'
Assert-Match $buildSource "assets/naturalist/items/\$\{itemId\}\.json" `
    'generated client-item definition path'
Assert-Match $buildSource 'model:\s*"naturalist:item/\$\{itemId\}"' `
    'generated client-item legacy cuboid reference'
Assert-Match $buildSource "providers\.gradleProperty\('naturalistLegacyMinecraftJar'\)" `
    'explicit legacy Minecraft input property'
foreach ($resourceHash in $legacySpawnEggResourceHashes.Values) {
    Assert-Match $buildSource ([regex]::Escape($resourceHash)) `
        "legacy spawn-egg resource hash $resourceHash"
}
Assert-Match $buildSource 'stageLegacySpawnEggResources' `
    'verified legacy spawn-egg staging task'
Assert-Match $buildSource 'spawnEggColors\.size\(\)\s*!=\s*46' `
    '46-entry spawn-egg source parser guard'
Assert-Match $buildSource 'value:\s*primaryArgb[\s\S]+value:\s*secondaryArgb' `
    'primary-before-secondary tint generation'
Assert-Match $buildSource 'processResources[\s\S]+generateItemModelCompatibilityResources' `
    'client-item generator processResources wiring'
Assert-Match $buildSource 'processResources[\s\S]+stageLegacySpawnEggResources' `
    'legacy spawn-egg processResources wiring'
Assert-Match $buildSource 'sourcesJar[\s\S]+stageLegacySpawnEggResources' `
    'legacy spawn-egg sourcesJar wiring'

$generatedRoot = Join-Path $projectRoot 'build\generated\item-model-compat\assets\naturalist\items'
if (-not (Test-Path -LiteralPath $generatedRoot -PathType Container)) {
    throw 'Generated client-item roots are absent. Run the Gradle generateItemModelCompatibilityResources task first.'
}
$generatedFiles = @(Get-ChildItem -LiteralPath $generatedRoot -File -Filter '*.json')
Assert-SetEqual @($generatedFiles.BaseName) @($compatibilityItemIds + $spawnEggIds) `
    'generated client-item files'
foreach ($itemId in $compatibilityItemIds) {
    $file = Get-Item -LiteralPath (Join-Path $generatedRoot "$itemId.json")
    $json = Get-Content -LiteralPath $file.FullName -Raw | ConvertFrom-Json
    Assert-Equal ([string]$json.model.type) 'minecraft:model' "$itemId root model type"
    Assert-Equal ([string]$json.model.model) "naturalist:item/$itemId" `
        "$itemId root cuboid model"
    $tintCount = if ($null -eq $json.model.tints) { 0 } else { @($json.model.tints).Count }
    Assert-Equal $tintCount 0 "$itemId root tint count"
}
foreach ($itemId in $spawnEggIds) {
    $file = Get-Item -LiteralPath (Join-Path $generatedRoot "$itemId.json")
    $json = Get-Content -LiteralPath $file.FullName -Raw | ConvertFrom-Json
    Assert-Equal ([string]$json.model.type) 'minecraft:model' "$itemId root model type"
    Assert-Equal ([string]$json.model.model) "naturalist:item/$itemId" `
        "$itemId root protected model"
    $tints = @($json.model.tints)
    Assert-Equal $tints.Count 2 "$itemId tint count"
    Assert-Equal ([string]$tints[0].type) 'minecraft:constant' "$itemId primary tint type"
    Assert-Equal ([int]$tints[0].value) $spawnEggColors[$itemId].PrimaryArgb `
        "$itemId primary signed-ARGB tint"
    Assert-Equal ([string]$tints[1].type) 'minecraft:constant' "$itemId secondary tint type"
    Assert-Equal ([int]$tints[1].value) $spawnEggColors[$itemId].SecondaryArgb `
        "$itemId secondary signed-ARGB tint"
}

$stagedLegacyRoot = Join-Path $projectRoot 'build\generated\legacy-spawn-egg-resources'
if (-not (Test-Path -LiteralPath $stagedLegacyRoot -PathType Container)) {
    throw 'Staged legacy spawn-egg resources are absent. Run the Gradle stageLegacySpawnEggResources task first.'
}
$stagedLegacyFiles = @(Get-ChildItem -LiteralPath $stagedLegacyRoot -File -Recurse)
$stagedLegacyPaths = @($stagedLegacyFiles | ForEach-Object {
    $_.FullName.Substring($stagedLegacyRoot.Length + 1).Replace('\', '/')
})
Assert-SetEqual $stagedLegacyPaths @($legacySpawnEggResourceHashes.Keys) `
    'staged legacy spawn-egg resources'
foreach ($resourcePath in $legacySpawnEggResourceHashes.Keys) {
    $stagedPath = Join-Path $stagedLegacyRoot $resourcePath.Replace('/', '\')
    Assert-Equal (Get-FileHash -LiteralPath $stagedPath -Algorithm SHA256).Hash.ToLowerInvariant() `
        $legacySpawnEggResourceHashes[$resourcePath] "staged $resourcePath SHA-256"
}

$repoRootOutput = @(& git -C $projectRoot rev-parse --show-toplevel 2>&1)
if ($LASTEXITCODE -ne 0) {
    throw "Unable to locate Git worktree: $($repoRootOutput -join [Environment]::NewLine)"
}
$repoRoot = [IO.Path]::GetFullPath($repoRootOutput[-1])
$projectPrefix = (& git -C $projectRoot rev-parse --show-prefix).TrimEnd('/')
$trackedProjectFiles = @(& git -C $repoRoot ls-files -- $projectPrefix)
$trackedSpawnEggCompatibilityResources = @($trackedProjectFiles | Where-Object {
    $_ -match '/assets/minecraft/(?:models/item/template_spawn_egg\.json|' +
        'textures/item/spawn_egg(?:_overlay)?\.png)$' -or
    $_ -match '/assets/naturalist/items/[a-z0-9_]+_spawn_egg\.json$'
})
Assert-Empty $trackedSpawnEggCompatibilityResources `
    'tracked protected/generated spawn-egg compatibility resources'

Write-Host ''
Write-Host 'Naturalist 26.2 dynamic item-model/property migration verified.'
Write-Host 'Contracts: 17 wrapped items; 14 retained inventory roots; 34 variant models; 26 links; 16 colors; 1 filled override; 46 two-tint spawn eggs; 78 generated roots.'
