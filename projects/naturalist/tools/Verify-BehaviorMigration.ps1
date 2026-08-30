[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$entityRoot = Join-Path $projectRoot 'common\src\main\java\com\crispytwig\naturalist\server\entity'
$javaFiles = @(Get-ChildItem -LiteralPath $entityRoot -File -Recurse -Filter '*.java')

function Assert-NoHits {
    param(
        [Parameter(Mandatory)][string]$Pattern,
        [Parameter(Mandatory)][string]$Label
    )

    $hits = @($javaFiles | Select-String -Pattern $Pattern)
    if ($hits.Count -ne 0) {
        $locations = @($hits | ForEach-Object { "$($_.Path):$($_.LineNumber)" })
        throw "$Label must be empty, but found: $($locations -join ', ')"
    }
    Write-Host "PASS  $Label is empty"
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

function Read-EntitySource {
    param([Parameter(Mandatory)][string]$RelativePath)

    return Get-Content -LiteralPath (Join-Path $entityRoot $RelativePath) -Raw
}

# API removals that accounted for the dominant shared/entity-behavior diagnostics.
Assert-NoHits '\.isClientSide\b(?!\s*\()' 'field-style Level.isClientSide access'
Assert-NoHits '\bInteractionResult\.sidedSuccess\s*\(' 'removed sidedSuccess helper calls'
Assert-NoHits '\.(?:isDay|isNight)\s*\(\s*\)' 'removed Level day/night method calls'
Assert-NoHits '\.getDayTime\s*\(\s*\)' 'removed globally shared day-clock calls'
Assert-NoHits '\.(?:getMinBuildHeight|getMaxBuildHeight)\s*\(\s*\)' 'removed build-height method calls'
Assert-NoHits '\.level\(\)\.random\b|(?<!\.)\blevel\.random\b' 'protected Level.random access'
Assert-NoHits '\.getProfiler\s*\(\s*\)' 'removed level-owned profiler access'
Assert-NoHits 'getGameRules\s*\(\s*\)\.getBoolean\s*\(' 'legacy boolean gamerule access'
Assert-NoHits '\bIngredient\.of\s*\(\s*NaturalistTags\.ItemTags\.' 'legacy tag-to-Ingredient construction'
Assert-NoHits '\bIngredient\.of\s*\(\s*BuiltInRegistries\.ITEM\.getOrThrow\s*\(' 'eager custom-tag Ingredient resolution'
Assert-NoHits '\.hasImpulse\b' 'removed entity impulse flag access'
Assert-NoHits '\.isInWaterOrBubble\s*\(\s*\)' 'removed water-or-bubble predicate calls'
Assert-NoHits '\.isControlledByLocalInstance\s*\(\s*\)' 'removed local-control movement checks'
Assert-NoHits '\.tryCheckInsideBlocks\s*\(\s*\)' 'removed inside-block effect hook calls'
Assert-NoHits '\.setCanPassDoors\s*\(' 'obsolete pass-door navigation configuration'
Assert-NoHits '\.getType\s*\(\s*\)\.is\s*\(' 'legacy entity-type tag checks'
Assert-NoHits '\.registryOrThrow\s*\(' 'legacy registry lookup access'
Assert-NoHits '\bDEFAULT_VARIANT\.location\s*\(' 'legacy variant-key identifier access'
Assert-NoHits '\bcanTakeItem\s*\(' 'removed mob pickup hook overrides/calls'
Assert-NoHits '\bisDamageSourceBlocked\s*\(' 'removed directional block predicate calls'
Assert-NoHits '\b(?:FlyingAnimal|Saddleable)\b' 'removed entity behavior interfaces'
Assert-NoHits '(?m)\b(?:public|protected)\s+boolean\s+hurt\s*\(' 'legacy boolean Entity.hurt overrides'
Assert-NoHits '(?m)\b(?:public|protected)\s+boolean\s+doHurtTarget\s*\(\s*(?:@\w+(?:\([^)]*\))?\s*)*Entity\s+\w+\s*\)' 'legacy one-argument attack overrides'
Assert-NoHits '(?m)\bcustomServerAiStep\s*\(\s*\)' 'legacy parameterless server-AI hooks'
Assert-NoHits '(?m)\bvoid\s+knockback\s*\([^)]*\bDamageSource\s+\w+\s*,\s*float\s+\w+\s*\)' 'partial 26.2 knockback overload overrides'

# Representative preservation contracts for shared and specialized behavior paths.
$surfaceClimbing = Read-EntitySource 'climbing\SurfaceClimbing.java'
Assert-Matches $surfaceClimbing 'EntityDataAccessor\s*<\s*Vector3fc\s*>' '26.2 read-only surface-normal synced data'
Assert-Matches $surfaceClimbing '(?s)travel\s*\(\s*\).*updateAttachment\s*\(.*findSteeredFace\s*\(' 'surface transition and obstacle-steering behavior'
Assert-Matches $surfaceClimbing '(?s)save\s*\(\s*ValueOutput.*ClimbNormalX.*load\s*\(\s*ValueInput.*ClimbNormalY' 'surface-normal persistence remains wired'

$groundNavigation = Read-EntitySource 'ai\navigation\BetterGroundPathNavigation.java'
Assert-Matches $groundNavigation 'PathType\.FIRE_IN_NEIGHBOR' '26.2 fire-neighbor corner avoidance'
Assert-Matches $groundNavigation 'PathType\.DAMAGING_IN_NEIGHBOR' '26.2 damaging-neighbor corner avoidance'
Assert-Matches $groundNavigation 'PathType\.WALKABLE_DOOR' 'walkable-door corner avoidance'

$parrotFlight = Read-EntitySource 'util\ParrotFlight.java'
Assert-Matches $parrotFlight 'getShoulderParrotLeft\s*\(\s*\)' 'left shoulder-parrot reference lookup'
Assert-Matches $parrotFlight 'getShoulderParrotRight\s*\(\s*\)' 'right shoulder-parrot reference lookup'

$animationSound = Read-EntitySource 'util\AnimationSoundPlayer.java'
Assert-Matches $animationSound 'instanceof\s+SmoothAnimationState' 'smooth animation sound timing path'
Assert-Matches $animationSound 'getTimeInMillis\s*\(' '26.2 vanilla animation sound timing path'

$behaviorCompat = Read-EntitySource 'util\BehaviorCompat.java'
Assert-Matches $behaviorCompat 'getItemBlockingWith\s*\(\s*\)' 'active blocking item compatibility gate'
Assert-Matches $behaviorCompat '(?s)bypassedBy\s*\(\s*\).*AbstractArrow.*getPierceLevel\s*\(\s*\).*resolveBlockedDamage' 'non-mutating 26.2 directional block predicate'
Assert-Matches $behaviorCompat '(?s)getVisibilityPercent\s*\(.*getSensing\s*\(\s*\)\.hasLineOfSight' 'client-safe non-combat targeting range and visibility parity'

$bear = Read-EntitySource 'mob\Bear.java'
Assert-Matches $bear 'implements\s+NeutralMob' 'bear anger behavior contract'
Assert-Matches $bear 'AttackPlayerNearBabiesGoal' 'bear defensive baby-targeting behavior'
Assert-Matches $bear 'stack\s*->\s*stack\.is\s*\(\s*NaturalistTags\.ItemTags\.BEAR_TEMPT_ITEMS\s*\)' 'bear tag-based tempt food semantics'
Assert-Matches $bear '(?s)BeehiveBlock\.dropHoneycomb\s*\(\s*level\s*,\s*new\s+ItemStack\s*\(\s*Items\.SHEARS\s*\)' 'bear current beehive loot context retains shears behavior'

$crab = Read-EntitySource 'mob\Crab.java'
Assert-Matches $crab '(?s)ItemTags\.SWORDS.*ItemTags\.AXES.*ItemTags\.HOES.*ItemTags\.PICKAXES.*ItemTags\.SHOVELS' 'crab current tool-tag weapon equivalence'

$snail = Read-EntitySource 'mob\Snail.java'
Assert-Matches $snail 'implements[^\r\n]*SurfaceCrawler' 'snail surface-crawling behavior contract'
Assert-Matches $snail 'EntityDataAccessor\s*<\s*Vector3fc\s*>\s+ATTACH_NORMAL' 'snail 26.2 surface-normal serializer type'

$ostrich = Read-EntitySource 'mob\Ostrich.java'
Assert-Matches $ostrich '(?s)stack\.is\s*\(\s*Items\.SADDLE\s*\).*isSaddleable\s*\(\s*\).*equipSaddle\s*\(.*stack\.shrink\s*\(' 'legacy ostrich saddle interaction fallback'
Assert-Matches $ostrich '(?s)putBoolean\s*\(\s*"Saddled".*getBooleanOr\s*\(\s*"Saddled"' 'ostrich saddle persistence remains wired'
Assert-Matches $ostrich '(?s)canUseSlot\s*\(\s*EquipmentSlot.*EquipmentSlot\.SADDLE' 'ostrich 26.2 saddle-slot eligibility bridge'
Assert-Matches $ostrich '(?s)onEquipItem\s*\(.*EquipmentSlot\.SADDLE.*entityData\.set\s*\(\s*SADDLED' 'ostrich saddle equipment/data synchronization'
Assert-Matches $ostrich '(?s)equipSaddle\s*\(.*setItemSlot\s*\(\s*EquipmentSlot\.SADDLE.*setGuaranteedDrop\s*\(\s*EquipmentSlot\.SADDLE\s*\)' 'ostrich native saddle equip and drop preservation'
$saddleTag = Get-Content -LiteralPath (Join-Path $projectRoot 'src\main\resources\data\minecraft\tags\entity_type\can_equip_saddle.json') -Raw
Assert-Matches $saddleTag '"naturalist:ostrich"' 'ostrich vanilla saddle-equipment compatibility tag'

foreach ($airMoverName in @('Bird', 'Butterfly', 'Firefly', 'Vulture')) {
    $airMover = Read-EntitySource "mob\$airMoverName.java"
    Assert-Matches $airMover 'omnidirectionalAirMover\s*\(\s*\)[^{]*\{\s*return\s+true\s*;' "$airMoverName air-movement semantics after FlyingAnimal removal"
}
$firefly = Read-EntitySource 'mob\Firefly.java'
Assert-Matches $firefly '(?s)canGlow\s*\(\s*\).*isDarkOutside\s*\(\s*\).*getMaxLocalRawBrightness' 'firefly night and local-light glow semantics'
$vulture = Read-EntitySource 'mob\Vulture.java'
Assert-Matches $vulture 'getMaxY\s*\(\s*\)\s*\+\s*1\s*-\s*ALTITUDE_MARGIN_FROM_BUILD_LIMIT' 'vulture exclusive build-limit altitude preservation'

$whale = Read-EntitySource 'mob\Whale.java'
Assert-Matches $whale 'WhaleSurfaceGoal' 'whale surfacing goal remains registered'
Assert-Matches $whale 'WhaleDiveGoal' 'whale diving goal remains registered'
Assert-Matches $whale 'WhaleSeekDeeperWaterGoal' 'whale deep-water recovery goal remains registered'

$starfish = Get-Content -LiteralPath (Join-Path $projectRoot `
    'common\src\main\java\com\crispytwig\naturalist\server\block\StarfishBlock.java') -Raw
Assert-Matches $starfish 'class\s+StarfishBlock\s+extends\s+MultifaceBlock' `
    'starfish retains multiface attachment behavior'
if ($starfish -match '\b(?:WATERLOGGED|createBlockStateDefinition|getStateForPlacement|getFluidState|updateShape)\b') {
    throw 'Starfish must inherit Minecraft 26.2 MultifaceBlock waterlogging without duplicate properties or callbacks.'
}
Write-Host 'PASS  starfish inherits 26.2 multiface waterlogging without duplicate state registration'

$bugNet = Get-Content -LiteralPath (Join-Path $projectRoot `
    'common\src\main\java\com\crispytwig\naturalist\server\item\BugNetItem.java') -Raw
Assert-Matches $bugNet '(?s)recipe\.isPresent\s*\(\s*\).*interactionTarget\.discard\s*\(\s*\)\s*;\s*return\s+InteractionResult\.SUCCESS_SERVER\s*;' `
    'server-authoritative bug-net capture preserves arm-swing broadcast'

$bucketItem = Get-Content -LiteralPath (Join-Path $projectRoot `
    'common\src\main\java\com\crispytwig\naturalist\server\item\NaturalistBucketItem.java') -Raw
Assert-Matches $bucketItem '(?s)useOn\s*\(.*ItemStack\s+replacement\s*=\s*release\s*\(.*player\.setItemInHand\s*\(\s*context\.getHand\s*\(\s*\)\s*,\s*replacement\s*\)' `
    'bucket use-on path explicitly installs the empty replacement stack'
Assert-Matches $bucketItem '(?s)allowMidWater.*InteractionResult\.SUCCESS\.heldItemTransformedTo\s*\(\s*release\s*\(' `
    'mid-water bucket release retains held-item transformation'

Write-Host 'Entity behavior migration static verification passed.'
