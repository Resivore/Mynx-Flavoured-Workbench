[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$blockRoot = Join-Path $projectRoot 'common\src\main\java\com\crispytwig\naturalist\server\block'
$entityRoot = Join-Path $projectRoot 'common\src\main\java\com\crispytwig\naturalist\server\entity'

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

function Assert-NoHits {
    param(
        [Parameter(Mandatory)][IO.FileInfo[]]$Files,
        [Parameter(Mandatory)][string]$Pattern,
        [Parameter(Mandatory)][string]$Label
    )

    $hits = @($Files | Select-String -Pattern $Pattern)
    if ($hits.Count -ne 0) {
        $locations = @($hits | ForEach-Object { "$($_.Path):$($_.LineNumber)" })
        throw "$Label must be empty, but found: $($locations -join ', ')"
    }
    Write-Host "PASS  $Label is empty"
}

function Read-BlockSource {
    param([Parameter(Mandatory)][string]$Name)

    return Get-Content -LiteralPath (Join-Path $blockRoot $Name) -Raw
}

$eggFiles = @(
    Get-Item -LiteralPath (Join-Path $blockRoot 'NaturalistEggBlock.java')
    Get-Item -LiteralPath (Join-Path $blockRoot 'AlligatorEggBlock.java')
    Get-Item -LiteralPath (Join-Path $blockRoot 'OstrichEggBlock.java')
    Get-Item -LiteralPath (Join-Path $blockRoot 'TortoiseEggBlock.java')
    Get-Item -LiteralPath (Join-Path $blockRoot 'SnailEggBlock.java')
)

# Removed APIs and stale callback shapes in the migrated family.
Assert-NoHits $eggFiles '\.isClientSide\b(?!\s*\()' 'field-style Level.isClientSide access'
Assert-NoHits $eggFiles '(?<!\.)\blevel\.random\b|\bserverLevel\.random\b' 'protected Level.random access'
Assert-NoHits $eggFiles '\.getTimeOfDay\s*\(' 'removed celestial-time access'
Assert-NoHits $eggFiles 'getGameRules\s*\(\s*\)\.getBoolean\s*\(' 'legacy boolean gamerule access'
Assert-NoHits $eggFiles '\.getType\s*\(\s*\)\.is\s*\(' 'legacy entity-type tag access'
Assert-NoHits $eggFiles '\.create\s*\(\s*level\s*\)' 'spawn creation without a 26.2 reason'
Assert-NoHits $eggFiles '\.moveTo\s*\(' 'removed entity moveTo positioning'
Assert-NoHits $eggFiles '\bEntityType\.FALLING_BLOCK\b' 'singular pre-26.2 falling-block entity constant'
Assert-NoHits $eggFiles '(?s)updateShape\s*\(\s*[^,]+,\s*(?:@\w+\s*)*Direction\b' 'legacy direction-first updateShape callback'
Assert-NoHits $eggFiles '(?s)entityInside\s*\([^)]*Entity\s+\w+\s*\)\s*\{' 'legacy four-argument entityInside callback'

$shared = Read-BlockSource 'NaturalistEggBlock.java'
Assert-Matches $shared 'EnvironmentAttributes\.TURTLE_EGG_HATCH_CHANCE' '26.2 turtle-egg hatch timeline attribute'
Assert-Matches $shared '(?s)hatchChance\s*>\s*0\.0F.*getRandom\s*\(\s*\)\.nextFloat\s*\(\s*\)\s*<\s*hatchChance' 'dawn-window and one-in-500-equivalent chance sampling'
Assert-Matches $shared '(?s)state\.getValue\s*\(\s*HATCH\s*\).*hatchStage\s*<\s*2.*setValue\s*\(\s*HATCH\s*,\s*hatchStage\s*\+\s*1\s*\).*removeBlock\s*\(\s*pos\s*,\s*false\s*\)' 'shared HATCH 0-to-2 progression and no-drop hatch removal'
Assert-Matches $shared '(?s)onPlace\s*\(.*!level\.isClientSide\s*\(\s*\).*levelEvent\s*\(\s*2005\s*,\s*pos\s*,\s*0\s*\)' 'shared placement particle event'
Assert-Matches $shared '(?s)isSteppingCarefully\s*\(\s*\).*destroyEgg\s*\([^;]*100\s*\).*instanceof\s+Zombie.*destroyEgg\s*\([^;]*3\s*\)' 'careful-step immunity and step/fall destruction odds'
Assert-Matches $shared 'fallOn\s*\([^)]*double\s+fallDistance\s*\)' '26.2 double-distance fall callback'
Assert-Matches $shared '(?s)instanceof\s+ServerLevel\s+serverLevel.*entity\.is\s*\(\s*NaturalistTags\.EntityTypes\.SAFE_EGG_WALKERS\s*\).*instanceof\s+LivingEntity.*instanceof\s+Player\s*\|\|\s*level\.getGameRules\s*\(\s*\)\.get\s*\(\s*GameRules\.MOB_GRIEFING\s*\)' 'server-only parent/tag/player/mob-griefing trample gate'
Assert-Matches $shared '(?s)supportsEggClusters\s*\(\s*\).*eggCount\s*>\s*1.*setValue\s*\(\s*EGGS\s*,\s*eggCount\s*-\s*1\s*\).*GameEvent\.BLOCK_DESTROY.*levelEvent\s*\(\s*2001' 'cluster decrement event and particle contract'
Assert-Matches $shared '(?s)EntitySpawnReason\.BREEDING.*setAge\s*\(\s*-24000\s*\).*snapTo\s*\(' '26.2 baby spawn reason, age, and positioning API'
if ($shared -match '\b(?:onSand|isSand|canSurvive)\s*\(') {
    throw 'Naturalist turtle-derived hatch progression must remain independent of vanilla surface gating.'
}
if ($shared -match '\bsuper\.onPlace\s*\(') {
    throw 'Naturalist placement must not add vanilla TurtleEggBlock sand-specific placement behavior.'
}
if ($shared -match '\bplayerDestroy\s*\(') {
    throw 'Naturalist shared egg code must retain inherited TurtleEggBlock mining/Silk Touch behavior.'
}
Write-Host 'PASS  Naturalist hatch progression has no vanilla surface gate'
Write-Host 'PASS  inherited TurtleEggBlock mining and Silk Touch path remains selected'

$alligator = Read-BlockSource 'AlligatorEggBlock.java'
$tortoise = Read-BlockSource 'TortoiseEggBlock.java'
$ostrich = Read-BlockSource 'OstrichEggBlock.java'
$snail = Read-BlockSource 'SnailEggBlock.java'

foreach ($species in @(
    @{ Name = 'Alligator'; Source = $alligator; Type = 'ALLIGATOR'; Parent = 'Alligator'; Prefix = 'GATOR' },
    @{ Name = 'Tortoise'; Source = $tortoise; Type = 'TORTOISE'; Parent = 'Tortoise'; Prefix = 'TORTOISE' }
)) {
    Assert-Matches $species.Source ('extends\s+NaturalistEggBlock') "$($species.Name) shared egg lifecycle"
    Assert-Matches $species.Source ('instanceof\s+' + $species.Parent) "$($species.Name) parent trampling immunity"
    Assert-Matches $species.Source 'shouldUpdateAtNaturalistDawn\s*\(\s*level\s*,\s*pos\s*\)' "$($species.Name) shared hatch timeline"
    Assert-Matches $species.Source '(?s)index\s*<\s*state\.getValue\s*\(\s*EGGS\s*\).*levelEvent\s*\(\s*2001.*0\.3D\s*\+\s*index\s*\*\s*0\.2D\s*,\s*0\.3D\s*,\s*0\.0F' "$($species.Name) clustered hatch count, particles, and offsets"
    foreach ($soundKind in @('BREAK', 'CRACK', 'HATCH')) {
        Assert-Matches $species.Source ("NaturalistSoundEvents\.$($species.Prefix)_EGG_$soundKind\.get\s*\(\s*\)") "$($species.Name) $($soundKind.ToLowerInvariant()) sound"
    }
}

Assert-Matches $tortoise 'IntegerProperty\.create\s*\(\s*"variant"\s*,\s*0\s*,\s*2\s*\)' 'Tortoise variant state range'
Assert-Matches $tortoise '(?s)setValue\s*\(\s*VARIANT\s*,\s*0\s*\).*createBlockStateDefinition.*builder\.add\s*\(\s*VARIANT\s*\)' 'Tortoise variant state default and registration'
Assert-Matches $tortoise '(?s)getCloneItemStack\s*\([^)]*boolean\s+includeData\s*\).*super\.getCloneItemStack\s*\([^;]*includeData\s*\).*DataDrivenVariantAnimal\.VARIANT_TAG.*DataComponents\.CUSTOM_DATA' 'Tortoise 26.2 clone stack variant data'
Assert-Matches $tortoise 'baby\s*->\s*baby\.setVariantByLegacyIndex\s*\(\s*variant\s*\)' 'Tortoise hatchling variant propagation'
Assert-Matches $tortoise '(?s)playerDestroy\s*\(.*Items\.COMMAND_BLOCK.*levelEvent\s*\(\s*2001.*for\s*\(\s*int\s+i\s*=\s*0\s*;\s*i\s*<\s*eggCount.*spawnBaby' 'Tortoise command-block debug hatch path'

Assert-Matches $ostrich 'extends\s+NaturalistEggBlock' 'Ostrich shared egg lifecycle'
Assert-Matches $ostrich 'random\.nextInt\s*\(\s*3\s*\)\s*==\s*0' 'Ostrich one-in-three hatch progression'
Assert-Matches $ostrich '(?s)spawnHatchlings.*levelEvent\s*\(\s*2001.*spawnBaby\s*\([^;]*0\.3D\s*,\s*0\.3D\s*,\s*0\.0F' 'Ostrich single hatchling and fixed offset'
Assert-Matches $ostrich 'supportsEggClusters\s*\(\s*\)[^{]*\{\s*return\s+false\s*;' 'Ostrich whole-egg destruction'
Assert-Matches $ostrich 'canBeReplaced\s*\([^)]*\)[^{]*\{\s*return\s+false\s*;' 'Ostrich non-clusterable placement'
Assert-Matches $ostrich 'Block\.box\s*\(\s*5\.0\s*,\s*0\.0\s*,\s*5\.0\s*,\s*11\.0\s*,\s*8\.0\s*,\s*11\.0\s*\)' 'Ostrich egg shape'
Assert-Matches $ostrich '(?s)playerWillDestroy.*!level\.isClientSide\s*\(\s*\).*!player\.getAbilities\s*\(\s*\)\.instabuild.*afterEggDestroyed.*instanceof\s+Player.*angerNearbyOstriches' 'Ostrich player mining and trampling anger paths'
Assert-Matches $ostrich '(?s)new\s+AABB\s*\(\s*pos\s*\)\.inflate\s*\(\s*16\.0\s*,\s*8\.0\s*,\s*16\.0\s*\).*!entity\.isBaby\s*\(\s*\).*!entity\.isTame\s*\(\s*\).*entity\.owns\s*\(\s*pos\s*\).*onOwnedEggDestroyed' 'Ostrich owner-aware nearby adult anger selection'

Assert-Matches $snail 'Block\.box\s*\(\s*0\.0\s*,\s*0\.0\s*,\s*0\.0\s*,\s*16\.0\s*,\s*1\.5\s*,\s*16\.0\s*\)' 'Snail egg shape'
Assert-Matches $snail '(?s)onPlace.*scheduleTick.*nextInt\s*\(\s*600\s*,\s*2400\s*\)' 'Snail 600-through-2399 hatch delay'
Assert-Matches $snail 'isFaceSturdy\s*\(\s*level\s*,\s*pos\.below\s*\(\s*\)\s*,\s*Direction\.UP\s*\)' 'Snail sturdy-top-face support rule'
Assert-Matches $snail '(?s)updateShape\s*\([^)]*LevelReader[^)]*ScheduledTickAccess[^)]*RandomSource[^)]*\).*direction\s*==\s*Direction\.DOWN.*Blocks\.AIR\.defaultBlockState\s*\(\s*\).*super\.updateShape\s*\(\s*state\s*,\s*level\s*,\s*ticks\s*,\s*pos\s*,\s*direction\s*,\s*neighborPos\s*,\s*neighborState\s*,\s*random\s*\)' 'Snail 26.2 neighbor-shape callback and downward support loss'
Assert-Matches $snail '(?s)entityInside\s*\([^)]*InsideBlockEffectApplier\s+effectApplier\s*,\s*boolean\s+isPrecise\s*\).*entity\.is\s*\(\s*EntityTypes\.FALLING_BLOCK\s*\).*destroyBlock' 'Snail 26.2 falling-block collision callback'
Assert-Matches $snail '(?s)hatchSnailEgg.*destroyBlock.*SoundEvents\.FROGSPAWN_HATCH.*spawnBabySnails' 'Snail no-drop hatch and frogspawn sound'
Assert-Matches $snail '(?s)nextInt\s*\(\s*2\s*,\s*6\s*\).*EntitySpawnReason\.BREEDING.*snapTo.*nextInt\s*\(\s*1\s*,\s*361\s*\).*setPersistenceRequired\s*\(\s*\).*setAge\s*\(\s*-6000\s*\)' 'Snail child count, spawn reason, orientation, persistence, and age'
Assert-Matches $snail 'Mth\.clamp\s*\(\s*random\.nextDouble\s*\(\s*\)\s*,\s*d\s*,\s*1\.0\s*-\s*d\s*\)' 'Snail clamped spawn offsets'

$layGoal = Get-Content -LiteralPath (Join-Path $entityRoot 'ai\goal\LayEggGoal.java') -Raw
Assert-Matches $layGoal '(?s)getLayEggCounter\s*\(\s*\)\s*>\s*this\.adjustedTickDelay\s*\(\s*60\s*\).*getRandom\s*\(\s*\)\.nextInt\s*\(\s*4\s*\)\s*\+\s*1' 'LayEggGoal timing and one-to-four egg count'
Assert-Matches $layGoal '(?s)blockPos\.above\s*\(\s*\).*setBlock\s*\(\s*eggPos\s*,\s*eggState\s*,\s*3\s*\).*onEggLaid\s*\(\s*eggPos\s*\).*setHasEgg\s*\(\s*false\s*\).*setLayingEgg\s*\(\s*false\s*\).*setInLoveTime\s*\(\s*600\s*\)' 'LayEggGoal placement callback and post-lay reset'
Assert-Matches $layGoal '(?s)isEmptyBlock\s*\(\s*pos\.above\s*\(\s*\)\s*\).*getEggLayableBlockTag\s*\(\s*\)' 'LayEggGoal empty-above and species surface tag rule'

$eggAnimal = Get-Content -LiteralPath (Join-Path $entityRoot 'base\EggLayingAnimal.java') -Raw
Assert-Matches $eggAnimal '(?s)default\s+BlockState\s+createEggBlockState\s*\(\s*int\s+eggCount\s*\).*getEggBlock\s*\(\s*\)\.defaultBlockState\s*\(\s*\)' 'single-egg species ignore randomized cluster count'

$alligatorEntity = Get-Content -LiteralPath (Join-Path $entityRoot 'mob\Alligator.java') -Raw
$tortoiseEntity = Get-Content -LiteralPath (Join-Path $entityRoot 'mob\Tortoise.java') -Raw
$ostrichEntity = Get-Content -LiteralPath (Join-Path $entityRoot 'mob\Ostrich.java') -Raw
$snailEntity = Get-Content -LiteralPath (Join-Path $entityRoot 'mob\Snail.java') -Raw
Assert-Matches $alligatorEntity '(?s)getEggBlock.*NaturalistRegistry\.ALLIGATOR_EGG.*createEggBlockState.*setValue\s*\(\s*AlligatorEggBlock\.EGGS\s*,\s*eggCount\s*\).*getEggLayableBlockTag.*NaturalistTags\.BlockTags\.ALLIGATOR_EGG_LAYABLE_ON' 'Alligator egg block, cluster count, and layable surface tag'
Assert-Matches $tortoiseEntity '(?s)getEggBlock.*NaturalistRegistry\.TORTOISE_EGG.*createEggBlockState.*setValue\s*\(\s*TurtleEggBlock\.EGGS\s*,\s*eggCount\s*\).*setValue\s*\(\s*TortoiseEggBlock\.VARIANT\s*,\s*this\.getLegacyVariantIndex\s*\(\s*\)\s*\).*getEggLayableBlockTag.*NaturalistTags\.BlockTags\.TORTOISE_EGG_LAYABLE_ON' 'Tortoise egg block, cluster/variant state, and layable surface tag'
Assert-Matches $ostrichEntity '(?s)getEggBlock.*NaturalistRegistry\.OSTRICH_EGG.*getEggLayableBlockTag.*NaturalistTags\.BlockTags\.OSTRICH_EGG_LAYABLE_ON' 'Ostrich egg block and layable surface tag'
Assert-Matches $ostrichEntity '(?s)onEggLaid\s*\([^)]*BlockPos\s+pos\s*\).*isTame\s*\(\s*\).*MAX_TRACKED_EGGS.*ownedEggs\.add\s*\(\s*pos\.immutable\s*\(\s*\)\s*\)' 'Ostrich wild-parent egg ownership registration'
Assert-Matches $snailEntity '(?s)getEggBlock.*NaturalistRegistry\.SNAIL_EGGS.*getEggLayableBlockTag.*NaturalistTags\.BlockTags\.ALLIGATOR_EGG_LAYABLE_ON' 'Snail egg block and preserved shared Alligator layable-surface tag'

Write-Host 'Egg-block migration static verification passed.'
