[CmdletBinding()]
param(
    [string]$JavaHome = $env:JAVA_HOME,
    [string]$MinecraftJar,
    [string]$Artifact
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$commonMixinSource = Join-Path $projectRoot 'common\src\main\java\com\fizzware\dramaticdoors\mixin'
$commonMixinConfigPath = Join-Path $projectRoot 'common\src\main\resources\dramaticdoors.mixins.json'
$fabricMixinConfigPath = Join-Path $projectRoot 'fabric\src\main\resources\dramaticdoors_fabric.mixins.json'
$legacyRefmapPath = Join-Path $projectRoot 'fabric\src\main\resources\dramaticdoors.refmap.json'

function Assert-True {
    param(
        [bool]$Condition,
        [string]$Message
    )

    if (-not $Condition) {
        throw $Message
    }
}

function Assert-ExactSequence {
    param(
        [string[]]$Actual,
        [string[]]$Expected,
        [string]$Label
    )

    Assert-True ($Actual.Count -eq $Expected.Count) "$Label count mismatch: expected $($Expected.Count), found $($Actual.Count)."
    for ($index = 0; $index -lt $Expected.Count; $index++) {
        Assert-True ($Actual[$index] -ceq $Expected[$index]) "$Label mismatch at index ${index}: expected '$($Expected[$index])', found '$($Actual[$index])'."
    }
}

if ([string]::IsNullOrWhiteSpace($MinecraftJar)) {
    $minecraftCache = Join-Path $env:USERPROFILE '.gradle\caches\fabric-loom\minecraftMaven\net\minecraft\minecraft-merged-deobf\26.2'
    $minecraftCandidates = @(Get-ChildItem -LiteralPath $minecraftCache -File -Filter 'minecraft-merged-deobf-26.2.jar')
    Assert-True ($minecraftCandidates.Count -eq 1) "Expected one resolved Minecraft 26.2 merged-deobf JAR under '$minecraftCache'; found $($minecraftCandidates.Count)."
    $MinecraftJar = $minecraftCandidates[0].FullName
}

$MinecraftJar = (Resolve-Path -LiteralPath $MinecraftJar).Path
Assert-True ([System.IO.Path]::GetFileName($MinecraftJar) -ceq 'minecraft-merged-deobf-26.2.jar') "Selector verification must use the resolved Minecraft 26.2 merged-deobf JAR, not '$MinecraftJar'."

if (-not [string]::IsNullOrWhiteSpace($JavaHome)) {
    $javapExe = Join-Path $JavaHome 'bin\javap.exe'
} else {
    $javapCommand = Get-Command javap.exe -ErrorAction Stop
    $javapExe = $javapCommand.Source
}
$javapExe = (Resolve-Path -LiteralPath $javapExe).Path

$expectedMixins = @(
    'AdvancementManagerMixin',
    'RecipeManagerMixin',
    'BlockLootMixin',
    'WalkNodeEvaluatorMixin',
    'DoorBlockMixin',
    'FenceGateBlockMixin',
    'DoorInteractGoalMixin',
    'OpenDoorsTaskMixin',
    'PiglinBrainMixin',
    'VillagerMixin',
    'WitchMixin'
)

$targets = @(
    [pscustomobject]@{ Mixin = 'AdvancementManagerMixin'; Class = 'net.minecraft.server.ServerAdvancementManager'; Method = 'apply'; Selector = 'apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V'; Descriptor = '(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V' },
    [pscustomobject]@{ Mixin = 'RecipeManagerMixin'; Class = 'net.minecraft.world.item.crafting.RecipeManager'; Method = 'prepare'; Selector = 'prepare(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)Lnet/minecraft/world/item/crafting/RecipeMap;'; Descriptor = '(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)Lnet/minecraft/world/item/crafting/RecipeMap;' },
    [pscustomobject]@{ Mixin = 'BlockLootMixin'; Class = 'net.minecraft.world.level.block.state.BlockBehaviour'; Method = 'getDrops'; Selector = 'getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/storage/loot/LootParams$Builder;)Ljava/util/List;'; Descriptor = '(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/storage/loot/LootParams$Builder;)Ljava/util/List;' },
    [pscustomobject]@{ Mixin = 'WalkNodeEvaluatorMixin'; Class = 'net.minecraft.world.level.pathfinder.WalkNodeEvaluator'; Method = 'getPathTypeFromState'; Selector = 'getPathTypeFromState(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/pathfinder/PathType;'; Descriptor = '(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/pathfinder/PathType;' },
    [pscustomobject]@{ Mixin = 'DoorBlockMixin'; Class = 'net.minecraft.world.level.block.DoorBlock'; Method = '<init>'; Selector = '<init>(Lnet/minecraft/world/level/block/state/properties/BlockSetType;Lnet/minecraft/world/level/block/state/BlockBehaviour$Properties;)V'; Descriptor = '(Lnet/minecraft/world/level/block/state/properties/BlockSetType;Lnet/minecraft/world/level/block/state/BlockBehaviour$Properties;)V' },
    [pscustomobject]@{ Mixin = 'DoorBlockMixin'; Class = 'net.minecraft.world.level.block.DoorBlock'; Method = 'createBlockStateDefinition'; Selector = 'createBlockStateDefinition(Lnet/minecraft/world/level/block/state/StateDefinition$Builder;)V'; Descriptor = '(Lnet/minecraft/world/level/block/state/StateDefinition$Builder;)V' },
    [pscustomobject]@{ Mixin = 'DoorBlockMixin'; Class = 'net.minecraft.world.level.block.DoorBlock'; Method = 'getStateForPlacement'; Selector = 'getStateForPlacement(Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/level/block/state/BlockState;'; Descriptor = '(Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/level/block/state/BlockState;' },
    [pscustomobject]@{ Mixin = 'DoorBlockMixin'; Class = 'net.minecraft.world.level.block.DoorBlock'; Method = 'updateShape'; Selector = 'updateShape(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/world/level/ScheduledTickAccess;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/util/RandomSource;)Lnet/minecraft/world/level/block/state/BlockState;'; Descriptor = '(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/world/level/ScheduledTickAccess;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/util/RandomSource;)Lnet/minecraft/world/level/block/state/BlockState;' },
    [pscustomobject]@{ Mixin = 'DoorBlockMixin'; Class = 'net.minecraft.world.level.block.DoorBlock'; Method = 'setPlacedBy'; Selector = 'setPlacedBy(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;)V'; Descriptor = '(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;)V' },
    [pscustomobject]@{ Mixin = 'FenceGateBlockMixin'; Class = 'net.minecraft.world.level.block.FenceGateBlock'; Method = '<init>'; Selector = '<init>(Lnet/minecraft/world/level/block/state/properties/WoodType;Lnet/minecraft/world/level/block/state/BlockBehaviour$Properties;)V'; Descriptor = '(Lnet/minecraft/world/level/block/state/properties/WoodType;Lnet/minecraft/world/level/block/state/BlockBehaviour$Properties;)V' },
    [pscustomobject]@{ Mixin = 'FenceGateBlockMixin'; Class = 'net.minecraft.world.level.block.FenceGateBlock'; Method = 'createBlockStateDefinition'; Selector = 'createBlockStateDefinition(Lnet/minecraft/world/level/block/state/StateDefinition$Builder;)V'; Descriptor = '(Lnet/minecraft/world/level/block/state/StateDefinition$Builder;)V' },
    [pscustomobject]@{ Mixin = 'FenceGateBlockMixin'; Class = 'net.minecraft.world.level.block.FenceGateBlock'; Method = 'getStateForPlacement'; Selector = 'getStateForPlacement(Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/level/block/state/BlockState;'; Descriptor = '(Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/level/block/state/BlockState;' },
    [pscustomobject]@{ Mixin = 'FenceGateBlockMixin'; Class = 'net.minecraft.world.level.block.FenceGateBlock'; Method = 'updateShape'; Selector = 'updateShape(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/world/level/ScheduledTickAccess;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/util/RandomSource;)Lnet/minecraft/world/level/block/state/BlockState;'; Descriptor = '(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/world/level/ScheduledTickAccess;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/util/RandomSource;)Lnet/minecraft/world/level/block/state/BlockState;' },
    [pscustomobject]@{ Mixin = 'DoorInteractGoalMixin'; Class = 'net.minecraft.world.entity.ai.goal.DoorInteractGoal'; Method = 'isOpen'; Selector = 'isOpen()Z'; Descriptor = '()Z' },
    [pscustomobject]@{ Mixin = 'DoorInteractGoalMixin'; Class = 'net.minecraft.world.entity.ai.goal.DoorInteractGoal'; Method = 'setOpen'; Selector = 'setOpen(Z)V'; Descriptor = '(Z)V' },
    [pscustomobject]@{ Mixin = 'DoorInteractGoalMixin'; Class = 'net.minecraft.world.entity.ai.goal.DoorInteractGoal'; Method = 'canUse'; Selector = 'canUse()Z'; Descriptor = '()Z' },
    [pscustomobject]@{ Mixin = 'OpenDoorsTaskMixin'; Class = 'net.minecraft.world.entity.ai.behavior.InteractWithDoor'; Method = 'closeDoorsThatIHaveOpenedOrPassedThrough'; Selector = 'closeDoorsThatIHaveOpenedOrPassedThrough(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/pathfinder/Node;Lnet/minecraft/world/level/pathfinder/Node;Ljava/util/Set;Ljava/util/Optional;)V'; Descriptor = '(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/pathfinder/Node;Lnet/minecraft/world/level/pathfinder/Node;Ljava/util/Set;Ljava/util/Optional;)V' },
    [pscustomobject]@{ Mixin = 'PiglinBrainMixin'; Class = 'net.minecraft.world.entity.monster.piglin.Piglin'; Method = 'makeBrain'; Selector = 'makeBrain(Lnet/minecraft/world/entity/ai/Brain$Packed;)Lnet/minecraft/world/entity/ai/Brain;'; Descriptor = '(Lnet/minecraft/world/entity/ai/Brain$Packed;)Lnet/minecraft/world/entity/ai/Brain;' },
    [pscustomobject]@{ Mixin = 'VillagerMixin'; Class = 'net.minecraft.world.entity.npc.villager.Villager'; Method = 'registerBrainGoals'; Selector = 'registerBrainGoals(Lnet/minecraft/world/entity/ai/Brain;)V'; Descriptor = '(Lnet/minecraft/world/entity/ai/Brain;)V' },
    [pscustomobject]@{ Mixin = 'WitchMixin'; Class = 'net.minecraft.world.entity.monster.Witch'; Method = 'registerGoals'; Selector = 'registerGoals()V'; Descriptor = '()V' }
)

$invocationTargets = @(
    [pscustomobject]@{ Mixin = 'BlockLootMixin'; Class = 'net.minecraft.world.level.block.state.BlockBehaviour'; Method = 'getDrops'; MethodDescriptor = '(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/storage/loot/LootParams$Builder;)Ljava/util/List;'; Selector = 'Lnet/minecraft/world/level/storage/loot/LootTable;getRandomItems(Lnet/minecraft/world/level/storage/loot/LootParams;)Lit/unimi/dsi/fastutil/objects/ObjectArrayList;'; JavapMember = 'net/minecraft/world/level/storage/loot/LootTable.getRandomItems:(Lnet/minecraft/world/level/storage/loot/LootParams;)Lit/unimi/dsi/fastutil/objects/ObjectArrayList;'; ExpectedCount = 1 },
    [pscustomobject]@{ Mixin = 'WalkNodeEvaluatorMixin'; Class = 'net.minecraft.world.level.pathfinder.WalkNodeEvaluator'; Method = 'getPathTypeFromState'; MethodDescriptor = '(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/pathfinder/PathType;'; Selector = 'Lnet/minecraft/world/level/block/state/BlockState;getBlock()Lnet/minecraft/world/level/block/Block;'; JavapMember = 'net/minecraft/world/level/block/state/BlockState.getBlock:()Lnet/minecraft/world/level/block/Block;'; ExpectedCount = 1 },
    [pscustomobject]@{ Mixin = 'DoorInteractGoalMixin'; Class = 'net.minecraft.world.entity.ai.goal.DoorInteractGoal'; Method = 'canUse'; MethodDescriptor = '()Z'; Selector = 'Lnet/minecraft/world/level/block/DoorBlock;isWoodenDoor(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Z'; JavapMember = 'net/minecraft/world/level/block/DoorBlock.isWoodenDoor:(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Z'; ExpectedCount = 2 },
    [pscustomobject]@{ Mixin = 'OpenDoorsTaskMixin'; Class = 'net.minecraft.world.entity.ai.behavior.InteractWithDoor'; Method = 'closeDoorsThatIHaveOpenedOrPassedThrough'; MethodDescriptor = '(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/pathfinder/Node;Lnet/minecraft/world/level/pathfinder/Node;Ljava/util/Set;Ljava/util/Optional;)V'; Selector = 'Lnet/minecraft/server/level/ServerLevel;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;'; JavapMember = 'net/minecraft/server/level/ServerLevel.getBlockState:(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;'; ExpectedCount = 1 }
)

$commonConfig = Get-Content -LiteralPath $commonMixinConfigPath -Raw | ConvertFrom-Json
$fabricConfig = Get-Content -LiteralPath $fabricMixinConfigPath -Raw | ConvertFrom-Json
Assert-True ([bool]$commonConfig.required) 'The common mixin configuration must remain required.'
Assert-True ([int]$commonConfig.injectors.defaultRequire -eq 1) 'The common mixin configuration must retain defaultRequire=1.'
Assert-ExactSequence -Actual @($commonConfig.mixins) -Expected $expectedMixins -Label 'Declared common mixin order'
Assert-True (-not ($commonConfig.PSObject.Properties.Name -contains 'refmap')) 'The common mixin config still declares the obsolete refmap.'
Assert-True (-not ($fabricConfig.PSObject.Properties.Name -contains 'refmap')) 'The Fabric mixin config still declares the obsolete refmap.'
Assert-True (-not (Test-Path -LiteralPath $legacyRefmapPath)) 'The obsolete, inactive-package refmap is still present.'

$sourceText = foreach ($mixin in $expectedMixins) {
    $sourcePath = Join-Path $commonMixinSource ($mixin + '.java')
    Assert-True (Test-Path -LiteralPath $sourcePath -PathType Leaf) "Missing declared mixin source '$sourcePath'."
    Get-Content -LiteralPath $sourcePath -Raw
}
$activeSource = [regex]::Replace(($sourceText -join "`n"), '/\*.*?\*/', '', [System.Text.RegularExpressions.RegexOptions]::Singleline)
Assert-True (-not [regex]::IsMatch($activeSource, 'require\s*=\s*0')) 'A required injection has been suppressed with require=0.'

$sourceMethodSelectors = @([regex]::Matches($activeSource, 'method\s*=\s*"([^"]+)"') | ForEach-Object { $_.Groups[1].Value } | Sort-Object)
$expectedMethodSelectors = @($targets | ForEach-Object { $_.Selector } | Sort-Object)
Assert-ExactSequence -Actual $sourceMethodSelectors -Expected $expectedMethodSelectors -Label 'Active @Inject method selector inventory'

$sourceInvocationSelectors = @([regex]::Matches($activeSource, 'target\s*=\s*"([^"]+)"') | ForEach-Object { $_.Groups[1].Value } | Sort-Object)
$expectedInvocationSelectors = @($invocationTargets | ForEach-Object { $_.Selector } | Sort-Object)
Assert-ExactSequence -Actual $sourceInvocationSelectors -Expected $expectedInvocationSelectors -Label 'Active @At member selector inventory'

$javapCache = @{}
function Get-JavapOutput {
    param(
        [string]$ClassName,
        [switch]$Bytecode
    )

    $cacheKey = $ClassName + '|' + [string]$Bytecode.IsPresent
    if (-not $javapCache.ContainsKey($cacheKey)) {
        $arguments = @('-classpath', $MinecraftJar, '-p', '-s')
        if ($Bytecode.IsPresent) {
            $arguments += '-c'
        }
        $arguments += $ClassName
        $lines = @(& $javapExe @arguments 2>&1)
        Assert-True ($LASTEXITCODE -eq 0) "javap failed for '$ClassName': $($lines -join [Environment]::NewLine)"
        $javapCache[$cacheKey] = [string[]]$lines
    }
    return [string[]]$javapCache[$cacheKey]
}

function Find-Method {
    param(
        [string]$ClassName,
        [string]$MethodName,
        [string]$Descriptor,
        [switch]$Bytecode
    )

    $lines = Get-JavapOutput -ClassName $ClassName -Bytecode:$Bytecode
    $simpleClassName = ($ClassName -split '\.')[-1]
    $signatureNeedle = if ($MethodName -ceq '<init>') { $simpleClassName + '(' } else { $MethodName + '(' }
    for ($descriptorIndex = 0; $descriptorIndex -lt $lines.Count; $descriptorIndex++) {
        if ($lines[$descriptorIndex].Trim() -cne ('descriptor: ' + $Descriptor)) {
            continue
        }
        $signatureIndex = $descriptorIndex - 1
        while ($signatureIndex -ge 0 -and [string]::IsNullOrWhiteSpace($lines[$signatureIndex])) {
            $signatureIndex--
        }
        if ($signatureIndex -ge 0 -and $lines[$signatureIndex].Contains($signatureNeedle)) {
            return [pscustomobject]@{ Lines = $lines; SignatureIndex = $signatureIndex; DescriptorIndex = $descriptorIndex }
        }
    }
    throw "Required target '$ClassName.$MethodName$Descriptor' was not found in the resolved Minecraft 26.2 bytecode."
}

foreach ($target in $targets) {
    $null = Find-Method -ClassName $target.Class -MethodName $target.Method -Descriptor $target.Descriptor
}

foreach ($invocation in $invocationTargets) {
    $method = Find-Method -ClassName $invocation.Class -MethodName $invocation.Method -Descriptor $invocation.MethodDescriptor -Bytecode
    $endIndex = $method.Lines.Count
    for ($index = $method.DescriptorIndex + 1; $index -lt $method.Lines.Count; $index++) {
        if ($method.Lines[$index] -match '^  \S.*\(.*\).*[;{]$') {
            $endIndex = $index
            break
        }
    }
    $methodBlock = ($method.Lines[$method.SignatureIndex..($endIndex - 1)] -join "`n")
    $actualCount = [regex]::Matches($methodBlock, [regex]::Escape($invocation.JavapMember)).Count
    Assert-True ($actualCount -eq $invocation.ExpectedCount) "@At target '$($invocation.Selector)' in '$($invocation.Class).$($invocation.Method)' expected $($invocation.ExpectedCount) bytecode match(es), found $actualCount."
}

$artifactLabel = 'source-only'
if (-not [string]::IsNullOrWhiteSpace($Artifact)) {
    $Artifact = (Resolve-Path -LiteralPath $Artifact).Path
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [System.IO.Compression.ZipFile]::OpenRead($Artifact)
    try {
        $entryNames = @($archive.Entries | ForEach-Object { $_.FullName })
        Assert-True (-not ($entryNames -contains 'dramaticdoors.refmap.json')) 'The built artifact still packages the obsolete refmap.'
        foreach ($mixin in $expectedMixins) {
            $classEntry = 'com/fizzware/dramaticdoors/mixin/' + $mixin + '.class'
            Assert-True ($entryNames -contains $classEntry) "Built artifact is missing '$classEntry'."
        }
        foreach ($configName in @('dramaticdoors.mixins.json', 'dramaticdoors_fabric.mixins.json')) {
            $configEntry = $archive.GetEntry($configName)
            Assert-True ($null -ne $configEntry) "Built artifact is missing '$configName'."
            $reader = [System.IO.StreamReader]::new($configEntry.Open())
            try {
                $packagedConfig = $reader.ReadToEnd() | ConvertFrom-Json
            } finally {
                $reader.Dispose()
            }
            Assert-True (-not ($packagedConfig.PSObject.Properties.Name -contains 'refmap')) "Packaged '$configName' still declares the obsolete refmap."
            if ($configName -ceq 'dramaticdoors.mixins.json') {
                Assert-ExactSequence -Actual @($packagedConfig.mixins) -Expected $expectedMixins -Label 'Packaged common mixin order'
            }
        }
    } finally {
        $archive.Dispose()
    }
    $artifactLabel = [System.IO.Path]::GetFileName($Artifact)
}

$minecraftHash = (Get-FileHash -LiteralPath $MinecraftJar -Algorithm SHA256).Hash
Write-Output "DRAMATIC_DOORS_MIXIN_SELECTORS_26_2_OK: 11 mixins; 20 required injections; 4 invocation selectors; minecraft-sha256=$minecraftHash; artifact=$artifactLabel"
