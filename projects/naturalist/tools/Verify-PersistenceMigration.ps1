[CmdletBinding()]
param(
    [Parameter()]
    [string]$OriginalJar
)

$ErrorActionPreference = 'Stop'

$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$entityRoot = Join-Path $projectRoot 'common\src\main\java\com\crispytwig\naturalist\server\entity'

function Assert-Equal {
    param(
        [Parameter(Mandatory)]$Actual,
        [Parameter(Mandatory)]$Expected,
        [Parameter(Mandatory)][string]$Label
    )

    if ($Actual -ne $Expected) {
        throw "$Label mismatch: expected '$Expected', found '$Actual'."
    }
    Write-Host "PASS  $Label = $Expected"
}

function Assert-Empty {
    param(
        [Parameter()][object[]]$Values,
        [Parameter(Mandatory)][string]$Label
    )

    $valuesArray = @($Values | Where-Object { $null -ne $_ -and "$_" -ne '' })
    if ($valuesArray.Count -ne 0) {
        throw "$Label must be empty, but found: $($valuesArray -join ', ')"
    }
    Write-Host "PASS  $Label is empty"
}

function Assert-SetEqual {
    param(
        [Parameter()][string[]]$Actual,
        [Parameter()][string[]]$Expected,
        [Parameter(Mandatory)][string]$Label
    )

    $actualSet = @($Actual | Sort-Object -Unique)
    $expectedSet = @($Expected | Sort-Object -Unique)
    $missing = @($expectedSet | Where-Object { $_ -notin $actualSet })
    $unexpected = @($actualSet | Where-Object { $_ -notin $expectedSet })
    if ($missing.Count -ne 0 -or $unexpected.Count -ne 0) {
        throw "$Label mismatch. Missing: [$($missing -join ', ')]. Unexpected: [$($unexpected -join ', ')]."
    }
    Write-Host "PASS  $Label has the expected $($expectedSet.Count) entries"
}

function Assert-Matches {
    param(
        [Parameter(Mandatory)][string]$Source,
        [Parameter(Mandatory)][string]$Pattern,
        [Parameter(Mandatory)][string]$Label
    )

    if ($Source -notmatch $Pattern) {
        throw "$Label is missing the required source contract."
    }
    Write-Host "PASS  $Label"
}

function Assert-NotMatches {
    param(
        [Parameter(Mandatory)][string]$Source,
        [Parameter(Mandatory)][string]$Pattern,
        [Parameter(Mandatory)][string]$Label
    )

    if ($Source -match $Pattern) {
        throw "$Label contains a forbidden legacy/simplified source pattern."
    }
    Write-Host "PASS  $Label"
}

function Read-EntitySource {
    param([Parameter(Mandatory)][string]$TypeName)

    $matches = @(Get-ChildItem -LiteralPath $entityRoot -File -Recurse -Filter "$TypeName.java")
    Assert-Equal $matches.Count 1 "$TypeName source file count"
    return Get-Content -LiteralPath $matches[0].FullName -Raw
}

# The baseline verifier is the single existing authority for the source JAR,
# registration/resource guard, and the preserved upstream variant invariants.
$baselineVerifier = Join-Path $PSScriptRoot 'Verify-PortBaseline.ps1'
$baselineArguments = @{}
if (-not [string]::IsNullOrWhiteSpace($OriginalJar)) {
    $baselineArguments.OriginalJar = $OriginalJar
}
& $baselineVerifier @baselineArguments

$expectedValueIoTypes = @(
    'Alligator', 'Anglerfish', 'Bass', 'Bear', 'Bird', 'Blobfish', 'Boar',
    'Butterfly', 'Capybara', 'Caterpillar', 'Catfish', 'Clam',
    'Crab', 'Deer', 'DesertScorpion', 'DirtTrail', 'Dragonfly', 'Duck', 'Elephant',
    'Firefly', 'GiantIsopod', 'Giraffe', 'GreatWhiteShark', 'Hedgehog', 'Hippo',
    'Jellyfish', 'JungleScorpion', 'KomodoDragon', 'Lion', 'Lizard', 'LizardTail',
    'MobPart', 'Mole', 'Ostrich', 'Piranha', 'Rat', 'Ray', 'Rhino', 'Snail', 'Snake',
    'Starfish', 'Tiger', 'Tortoise', 'Turkey', 'Vulture', 'Whale', 'Zebra'
)

$javaFiles = @(Get-ChildItem -LiteralPath $entityRoot -File -Recurse -Filter '*.java')
$savePattern = '(?m)\b(?:public|protected)\s+void\s+addAdditionalSaveData\s*\(\s*(?:@\w+(?:\([^)]*\))?\s*)*ValueOutput\s+\w+\s*\)'
$loadPattern = '(?m)\b(?:public|protected)\s+void\s+readAdditionalSaveData\s*\(\s*(?:@\w+(?:\([^)]*\))?\s*)*ValueInput\s+\w+\s*\)'
$legacySaveLoadPattern = '(?m)\b(?:addAdditionalSaveData|readAdditionalSaveData)\s*\(\s*(?:@\w+(?:\([^)]*\))?\s*)*CompoundTag\b'

$valueIoTypes = @()
$partialValueIoTypes = @()
$legacySaveLoadTypes = @()
foreach ($file in $javaFiles) {
    $source = Get-Content -LiteralPath $file.FullName -Raw
    $hasSave = $source -match $savePattern
    $hasLoad = $source -match $loadPattern
    if ($hasSave -and $hasLoad) {
        $valueIoTypes += $file.BaseName
    }
    elseif ($hasSave -or $hasLoad) {
        $partialValueIoTypes += $file.BaseName
    }
    if ($source -match $legacySaveLoadPattern) {
        $legacySaveLoadTypes += $file.BaseName
    }
}

Assert-Empty $partialValueIoTypes 'partial ValueInput/ValueOutput entity save/load pairs'
Assert-Empty $legacySaveLoadTypes 'legacy CompoundTag entity save/load signatures'
Assert-SetEqual $valueIoTypes $expectedValueIoTypes 'current ValueInput/ValueOutput entity save/load files'
Assert-Equal $valueIoTypes.Count 47 'current ValueInput/ValueOutput entity save/load file count'

# Forty-five entity classes own their variant save/load pair directly. BlackBear
# and Mammoth deliberately inherit the exact same preservation path from Bear
# and Elephant, completing coverage of all 47 retained variant entity types.
$directVariantTypes = @(
    'Alligator', 'Anglerfish', 'Bass', 'Bear', 'Bird', 'Blobfish', 'Boar',
    'Butterfly', 'Capybara', 'Caterpillar', 'Catfish', 'Clam', 'Crab', 'Deer',
    'DesertScorpion', 'Dragonfly', 'Duck', 'Elephant', 'Firefly', 'GiantIsopod',
    'Giraffe', 'GreatWhiteShark', 'Hedgehog', 'Hippo', 'Jellyfish', 'JungleScorpion',
    'KomodoDragon', 'Lion', 'Lizard', 'LizardTail', 'Mole', 'Ostrich', 'Piranha',
    'Rat', 'Ray', 'Rhino', 'Snail', 'Snake', 'Starfish', 'Tiger', 'Tortoise',
    'Turkey', 'Vulture', 'Whale', 'Zebra'
)
Assert-Equal $directVariantTypes.Count 45 'direct variant persistence class count'
foreach ($typeName in $directVariantTypes) {
    $source = Read-EntitySource $typeName
    Assert-Matches $source '\bthis\.saveVariant\s*\(' "$typeName variant save wiring"
    Assert-Matches $source '\bthis\.loadVariant\s*\(' "$typeName variant load wiring"
}

$blackBearSource = Read-EntitySource 'BlackBear'
$mammothSource = Read-EntitySource 'Mammoth'
Assert-Matches $blackBearSource '\bclass\s+BlackBear\s+extends\s+Bear\b' 'BlackBear inherits Bear variant persistence'
Assert-Matches $mammothSource '\bclass\s+Mammoth\s+extends\s+Elephant\b' 'Mammoth inherits Elephant variant persistence'

$variantInterfacePath = Join-Path $entityRoot 'variant\DataDrivenVariantAnimal.java'
$variantInterface = Get-Content -LiteralPath $variantInterfacePath -Raw
$persistenceHelperPath = Join-Path $entityRoot 'persistence\NaturalistEntityPersistence.java'
$persistenceHelper = Get-Content -LiteralPath $persistenceHelperPath -Raw
Assert-Matches $variantInterface 'saveVariant\s*\(\s*ValueOutput\s+\w+\s*\)' 'shared ValueOutput variant method'
Assert-Matches $variantInterface 'loadVariant\s*\(\s*ValueInput\s+\w+\s*\)' 'shared ValueInput variant method'
Assert-Matches $variantInterface 'NaturalistEntityPersistence\.saveVariant\s*\(' 'shared variant save adapter wiring'
Assert-Matches $variantInterface 'NaturalistEntityPersistence\.readVariant\s*\(' 'shared variant load adapter wiring'
Assert-Matches $persistenceHelper 'putString\s*\(\s*VARIANT_TAG\s*,\s*variant\.toString\s*\(\s*\)\s*\)' 'variant identifiers remain namespaced strings'
Assert-Matches $persistenceHelper 'getString\s*\(\s*VARIANT_TAG\s*\)' 'string variant read path'
Assert-Matches $persistenceHelper 'getInt\s*\(\s*VARIANT_TAG\s*\)' 'legacy numeric variant read path'
Assert-Matches $persistenceHelper 'Math\.floorMod\s*\(' 'legacy malformed-range variant tolerance'

$legacyRemapPath = Join-Path $entityRoot 'variant\LegacyVariantRemap.java'
$legacyRemap = Get-Content -LiteralPath $legacyRemapPath -Raw
$entityTypeMixinPath = Join-Path $projectRoot 'common\src\main\java\com\crispytwig\naturalist\mixin\EntityTypeMixin.java'
$entityTypeMixin = Get-Content -LiteralPath $entityTypeMixinPath -Raw
Assert-Matches $entityTypeMixin 'by\(Lnet/minecraft/world/level/storage/ValueInput;\)Ljava/util/Optional;' 'legacy entity remap targets current ValueInput selector'
Assert-Matches $entityTypeMixin 'cancellable\s*=\s*true' 'legacy entity remap can replace the retired entity type'
Assert-Matches $entityTypeMixin 'NaturalistEntityTypes\.BIRD\.get\s*\(' 'legacy bird IDs map to generic Bird'
Assert-Matches $entityTypeMixin 'NaturalistEntityTypes\.SNAKE\.get\s*\(' 'legacy snake IDs map to generic Snake'
Assert-Matches $variantInterface 'getString\s*\(\s*"id"\s*\)[\s\S]*LegacyVariantRemap::variantForLegacyEntityId' 'legacy entity ID supplies variant during ValueInput load'
foreach ($legacyId in @('bluejay', 'cardinal', 'robin', 'sparrow', 'canary', 'finch', 'coral_snake', 'rattlesnake')) {
    Assert-Matches $legacyRemap ([regex]::Escape("naturalist:$legacyId")) "legacy $legacyId identity retained"
}

$neutralTypes = @('Bear', 'Boar', 'Elephant', 'Ostrich', 'Snake')
$serializedAngerTypes = @('Bear', 'Elephant', 'Ostrich', 'Snake')
foreach ($typeName in $neutralTypes) {
    $source = Read-EntitySource $typeName
    Assert-Matches $source '\bimplements\b[^\{]*\bNeutralMob\b' "$typeName remains a NeutralMob"
    Assert-Matches $source '\bEntityReference\s*<\s*LivingEntity\s*>\s+persistentAngerTarget\b' "$typeName anger target EntityReference"
    Assert-Matches $source '\blong\s+getPersistentAngerEndTime\s*\(' "$typeName long anger end-time getter"
    Assert-Matches $source '\bvoid\s+setPersistentAngerEndTime\s*\(\s*long\b' "$typeName long anger end-time setter"
    Assert-NotMatches $source '\b(?:get|set)RemainingPersistentAngerTime\s*\(' "$typeName has no removed countdown anger API"
    Assert-NotMatches $source '\bUUID\s+persistentAngerTarget\b' "$typeName has no raw UUID anger field"
}

foreach ($typeName in $serializedAngerTypes) {
    $source = Read-EntitySource $typeName
    Assert-Matches $source '\baddPersistentAngerSaveData\s*\(' "$typeName current anger save wiring"
    Assert-Matches $source '\breadPersistentAngerSaveData\s*\(' "$typeName current anger load wiring"
    Assert-Matches $source 'NaturalistEntityPersistence\.loadLegacyAngerTarget\s*\(' "$typeName legacy AngryAt fallback wiring"
}
Assert-Matches $persistenceHelper 'readReference\s*\([^;]*"AngryAt"\s*\)' 'shared legacy AngryAt UUID read'
Assert-Matches $persistenceHelper 'setPersistentAngerTarget\s*\(' 'shared legacy AngryAt reference restore'
Assert-Matches $persistenceHelper 'EntityReference\.getLivingEntity\s*\(' 'shared legacy AngryAt live-target resolution'
Assert-Matches $persistenceHelper '\.setTarget\s*\(' 'shared legacy AngryAt target restore'
$boarSource = Read-EntitySource 'Boar'
Assert-NotMatches $boarSource '\b(?:add|read)PersistentAngerSaveData\s*\(' 'Boar preserves upstream non-persisted anger behavior'

$followingPetSource = Read-EntitySource 'FollowingPet'
Assert-Matches $followingPetSource 'savePet\s*\([^)]*ValueOutput' 'FollowingPet current value-output save contract'
Assert-Matches $followingPetSource 'loadPet\s*\([^)]*ValueInput' 'FollowingPet current value-input load contract'
Assert-Matches $followingPetSource 'read\s*\(\s*"FollowingOwner"\s*,\s*Codec\.BOOL\s*\)\.ifPresent' 'FollowingPet missing-value policy'

$dyeableSource = Read-EntitySource 'DyeableAnimal'
Assert-Matches $dyeableSource 'saveDye\s*\([^)]*ValueOutput' 'DyeableAnimal current value-output save contract'
Assert-Matches $dyeableSource 'loadDye\s*\([^)]*ValueInput' 'DyeableAnimal current value-input load contract'
Assert-Matches $dyeableSource 'saveDye\s*\([^)]*CompoundTag' 'DyeableAnimal caught-item CompoundTag save contract'
Assert-Matches $dyeableSource 'loadDye\s*\([^)]*CompoundTag' 'DyeableAnimal caught-item CompoundTag load contract'

$huntingSource = Read-EntitySource 'HuntingAnimal'
Assert-Matches $huntingSource 'saveHuntingCooldown\s*\(\s*ValueOutput' 'HuntingAnimal current value-output save contract'
Assert-Matches $huntingSource 'loadHuntingCooldown\s*\(\s*ValueInput' 'HuntingAnimal current value-input load contract'
Assert-Matches $huntingSource 'getIntOr\s*\(\s*HUNTING_COOLDOWN_TAG\s*,\s*0\s*\)' 'HuntingAnimal missing cooldown default'

$surfaceClimbingSource = Read-EntitySource 'SurfaceClimbing'
Assert-Matches $surfaceClimbingSource 'save\s*\(\s*ValueOutput' 'SurfaceClimbing current value-output save contract'
Assert-Matches $surfaceClimbingSource 'load\s*\(\s*ValueInput' 'SurfaceClimbing current value-input load contract'
Assert-Matches $surfaceClimbingSource 'read\s*\(\s*"ClimbNormalY"[^;]*isPresent' 'SurfaceClimbing optional attachment guard'

$catchableSource = Read-EntitySource 'Catchable'
Assert-Matches $catchableSource 'getOwnerReference\s*\(' 'caught tame owner uses current EntityReference getter'
Assert-Matches $catchableSource 'owner\.getUUID\s*\(' 'caught tame owner retains UUID custom-data encoding'
Assert-Matches $catchableSource 'setOwnerReference\s*\(\s*EntityReference\.of\s*\(' 'caught tame owner restores current EntityReference'

$commonJavaRoot = Join-Path $projectRoot 'common\src\main\java'
$removedOwnerApiHits = @(Get-ChildItem -LiteralPath $commonJavaRoot -File -Recurse -Filter '*.java' |
    Select-String -Pattern '\b(?:get|set)OwnerUUID\s*\(')
Assert-Empty $removedOwnerApiHits 'removed raw tame-owner UUID API calls'

$parrotFlightPath = Join-Path $entityRoot 'util\ParrotFlight.java'
$parrotFlightSource = Get-Content -LiteralPath $parrotFlightPath -Raw
Assert-Matches $parrotFlightSource 'getShoulderParrotLeft\s*\(\s*\)\.isPresent\s*\(' 'left shoulder-parrot current reference lookup'
Assert-Matches $parrotFlightSource 'getShoulderParrotRight\s*\(\s*\)\.isPresent\s*\(' 'right shoulder-parrot current reference lookup'
Assert-NotMatches $parrotFlightSource 'EntityType::byString' 'shoulder-entity ID has no removed lookup API'

$knapsackPath = Join-Path $projectRoot 'common\src\main\java\com\crispytwig\naturalist\server\item\KnapsackItem.java'
$knapsackSource = Get-Content -LiteralPath $knapsackPath -Raw
Assert-Matches $knapsackSource 'TagValueOutput\.createWithContext\s*\(' 'Knapsack captured entity contextual value output'
Assert-Matches $knapsackSource 'ScopedCollector\s+problems' 'Knapsack captured entity scoped problem reporting'
Assert-Matches $knapsackSource 'mob\.problemPath\s*\(' 'Knapsack capture diagnostics retain entity path'
Assert-Matches $knapsackSource '\bmob\.save\s*\(\s*output\s*\)' 'Knapsack captured entity current save call'
Assert-Matches $knapsackSource '\boutput\.buildResult\s*\(' 'Knapsack captured entity custom-data bridge'
Assert-Matches $knapsackSource 'TagValueInput\.create\s*\(' 'Knapsack captured entity contextual value input'
Assert-Matches $knapsackSource 'loadEntityRecursive\s*\([^;]*EntitySpawnReason\.LOAD' 'Knapsack captured entity current recursive load call'
Assert-Matches $knapsackSource '\be\.snapTo\s*\(' 'Knapsack captured entity current release positioning'
Assert-Matches $knapsackSource 'BuiltInRegistries\.ENTITY_TYPE::getOptional' 'Knapsack captured entity current tooltip lookup'
Assert-Matches $knapsackSource 'ComponentSerialization\.CODEC' 'Knapsack captured entity current custom-name tooltip codec'
Assert-Matches $knapsackSource 'TooltipDisplay\s+display' 'Knapsack current tooltip signature'
Assert-Matches $knapsackSource 'tooltip\.accept\s*\(' 'Knapsack current tooltip consumer'
Assert-NotMatches $knapsackSource '\.isClientSide\b(?!\s*\()' 'Knapsack has no removed client-side field access'
Assert-NotMatches $knapsackSource 'InteractionResult\.sidedSuccess' 'Knapsack has no removed sided-result helper'
Assert-NotMatches $knapsackSource '\bmob\.save\s*\(\s*entityTag\s*\)' 'Knapsack has no removed CompoundTag entity save call'
Assert-NotMatches $knapsackSource 'loadEntityRecursive\s*\(\s*tag\s*,\s*serverLevel\s*,' 'Knapsack has no removed CompoundTag entity load call'

$snakeSource = Read-EntitySource 'Snake'
Assert-Matches $snakeSource '\bthis\.saveVariant\s*\(' 'Snake reptile variant save representative'
Assert-Matches $snakeSource '\bthis\.loadVariant\s*\(' 'Snake reptile variant load representative'
Assert-Matches $snakeSource '\bEntityReference\s*<\s*LivingEntity\s*>' 'Snake reptile anger reference representative'

$elephantSource = Read-EntitySource 'Elephant'
Assert-Matches $elephantSource 'NaturalistEntityPersistence\.saveFixedInventory\s*\([^;]*this\.inventory\s*\)' 'Elephant fixed-slot inventory save wiring'
Assert-Matches $elephantSource 'NaturalistEntityPersistence\.loadFixedInventory\s*\([^;]*this\.inventory\s*\)' 'Elephant fixed-slot inventory load wiring'
Assert-Matches $persistenceHelper 'ContainerHelper\.saveAllItems\s*\(' 'fixed-slot inventory uses vanilla save helper'
Assert-Matches $persistenceHelper 'ContainerHelper\.loadAllItems\s*\(' 'fixed-slot inventory uses vanilla load helper'

$ostrichSource = Read-EntitySource 'Ostrich'
Assert-Matches $ostrichSource 'store\s*\(\s*"OwnedEggs"\s*,\s*Codec\.LONG\.listOf\s*\(\s*\)' 'Ostrich owned eggs use current long-list codec'
Assert-Matches $ostrichSource 'read\s*\(\s*"OwnedEggs"\s*,\s*Codec\.LONG\.listOf\s*\(\s*\)' 'Ostrich owned eggs retain codec read path'

$ratSource = Read-EntitySource 'Rat'
Assert-Matches $ratSource 'NaturalistEntityPersistence\.savePackedInventory\s*\([^;]*"CarriedItems"' 'Rat packed carried inventory save wiring'
Assert-Matches $ratSource 'NaturalistEntityPersistence\.loadPackedInventory\s*\([^;]*"CarriedItems"' 'Rat packed carried inventory load wiring'
Assert-Matches $persistenceHelper '\.storeAsItemList\s*\(' 'packed inventory preserves compact save semantics'
Assert-Matches $persistenceHelper 'input\.list\s*\([^;]*ItemStack\.CODEC\s*\)\.ifPresent\s*\(\s*inventory::fromItemList\s*\)' 'packed inventory preserves compact optional load semantics'

Assert-Matches $persistenceHelper 'EntityReference\.store\s*\(' 'shared entity-reference save adapter'
Assert-Matches $persistenceHelper 'EntityReference\.read\s*\(' 'shared entity-reference load adapter'

Write-Host ''
Write-Host 'Naturalist 26.2 persistence migration verified.'
Write-Host 'Coverage: 47 ValueInput/ValueOutput pairs; 45 direct + 2 inherited variant entities.'
Write-Host 'References: 5 NeutralMob anger owners and shared entity-reference adapters.'
Write-Host 'Inventories: Elephant fixed-slot and Rat compact carried-item semantics.'
