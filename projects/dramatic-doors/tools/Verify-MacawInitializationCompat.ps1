[CmdletBinding()]
param(
    [string]$Artifact
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$registryPath = Join-Path $projectRoot 'common\src\main\java\com\fizzware\dramaticdoors\registry\DDRegistry.java'
$stablePath = Join-Path $projectRoot 'common\src\main\java\com\fizzware\dramaticdoors\blocks\TallStableDoorBlock.java'
$slidingPath = Join-Path $projectRoot 'common\src\main\java\com\fizzware\dramaticdoors\blocks\TallSlidingDoorBlock.java'
$mixinPath = Join-Path $projectRoot 'common\src\main\java\com\fizzware\dramaticdoors\mixin\DoorBlockMixin.java'

function Assert-True {
    param(
        [bool]$Condition,
        [string]$Message
    )

    if (-not $Condition) {
        throw $Message
    }
}

function Get-NormalizedSource {
    param([string]$Path)

    $source = Get-Content -LiteralPath $Path -Raw -Encoding UTF8
    return [regex]::Replace($source, '\s+', ' ').Trim()
}

$registry = Get-NormalizedSource $registryPath
$stable = Get-NormalizedSource $stablePath
$sliding = Get-NormalizedSource $slidingPath
$mixin = Get-NormalizedSource $mixinPath

Assert-True ($registry.Contains('createSlidingDoorBlock(shortname, block, blocksettype, false)') -and
    $registry.Contains('createSlidingDoorBlock(tallname, block, blocksettype, true)')) 'Sliding registration must pass each destination registry name into its factory.'
Assert-True ($registry.Contains('createStableDoorBlock(shortname, block, blocksettype, false)') -and
    $registry.Contains('createStableDoorBlock(tallname, block, blocksettype, true)')) 'Stable registration must pass each destination registry name into its factory.'
Assert-True ($registry.Contains('protected static Block createSlidingDoorBlock(String name, Block block, BlockSetType blocksettype, boolean isTall)') -and
    $registry.Contains('Properties properties = Properties.ofFullCopy(block).setId(blockKey(name)); return new TallSlidingDoorBlock(blocksettype, properties);')) 'Sliding factory must preserve copied source behavior while assigning the new Dramatic Doors block key before construction.'
Assert-True ($registry.Contains('protected static Block createStableDoorBlock(String name, Block block, BlockSetType blocksettype, boolean isTall)') -and
    $registry.Contains('Properties properties = Properties.ofFullCopy(block).setId(blockKey(name)); return new TallStableDoorBlock(blocksettype, properties);')) 'Stable factory must preserve copied source behavior while assigning the new Dramatic Doors block key before construction.'
Assert-True (-not $registry.Contains('return new TallSlidingDoorBlock(blocksettype, block);') -and
    -not $registry.Contains('return new TallStableDoorBlock(blocksettype, block);')) 'DDRegistry stable/sliding factories must not construct from a raw source Block.'

Assert-True ($stable.Contains('public TallStableDoorBlock(BlockSetType blockset, Properties properties) { super(blockset, properties); }')) 'TallStableDoorBlock must forward keyed Properties unchanged to TallDoorBlock.'
Assert-True ($sliding.Contains('public TallSlidingDoorBlock(BlockSetType blockset, Properties properties) { this(blockset, properties, SlidingDoorType.MACAW); }') -and
    $sliding.Contains('public TallSlidingDoorBlock(BlockSetType blockset, Properties properties, SlidingDoorType type) { super(blockset, properties); this.doorType = type; }')) 'TallSlidingDoorBlock must preserve its door type while forwarding keyed Properties unchanged.'

Assert-True ($mixin.Contains('BlockState defaultState = ((DoorBlock)(Object)this).defaultBlockState(); if (defaultState.hasProperty(WATERLOGGED)) { ((DoorBlock)(Object)this).registerDefaultState(defaultState.setValue(WATERLOGGED, false)); }')) 'DoorBlock constructor enhancement must not write WATERLOGGED when the target state definition omits it.'
Assert-True ($mixin.Contains('if (!defaultState.hasProperty(WATERLOGGED)) { return; } BlockPos blockpos = context.getClickedPos();')) 'Placement injection must defer to DoorBlock/subclass behavior when WATERLOGGED is absent.'
Assert-True ($mixin.Contains('if (facingState.getBlock() instanceof DoorBlock && !facingState.hasProperty(WATERLOGGED)) { return; }')) 'Vertical update handling must defer when the counterpart DoorBlock state lacks WATERLOGGED.'
Assert-True ($mixin.Contains('if (!state.hasProperty(WATERLOGGED)) { return; } boolean waterfilled = level.getFluidState(pos.above()).getType() == Fluids.WATER;')) 'setPlacedBy injection must defer when WATERLOGGED is absent.'
Assert-True ($mixin.Contains('return state.hasProperty(WATERLOGGED) && state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);')) 'Fluid-state lookup must preserve Block behavior when WATERLOGGED is absent.'
Assert-True ($mixin.Contains('return state.hasProperty(WATERLOGGED) && SimpleWaterloggedBlock.super.canPlaceLiquid(entity, level, pos, state, fluid);')) 'Property-less DoorBlock states must not advertise liquid placement.'
Assert-True ($mixin.Contains('return state.hasProperty(WATERLOGGED) && SimpleWaterloggedBlock.super.placeLiquid(level, pos, state, fluidState);')) 'Liquid placement must not read or write a missing WATERLOGGED property.'
Assert-True ($mixin.Contains('return state.hasProperty(WATERLOGGED) ? SimpleWaterloggedBlock.super.pickupBlock(entity, level, pos, state) : ItemStack.EMPTY;')) 'Liquid pickup must not read or write a missing WATERLOGGED property.'

$artifactLabel = 'source-only'
if (-not [string]::IsNullOrWhiteSpace($Artifact)) {
    $artifactPath = (Resolve-Path -LiteralPath $Artifact).Path
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [IO.Compression.ZipFile]::OpenRead($artifactPath)
    try {
        foreach ($entryName in @(
            'com/fizzware/dramaticdoors/registry/DDRegistry.class',
            'com/fizzware/dramaticdoors/blocks/TallStableDoorBlock.class',
            'com/fizzware/dramaticdoors/blocks/TallSlidingDoorBlock.class',
            'com/fizzware/dramaticdoors/mixin/DoorBlockMixin.class'
        )) {
            Assert-True ($null -ne $archive.GetEntry($entryName)) "Packaged Canary lacks $entryName."
        }

        $metadataEntry = $archive.GetEntry('fabric.mod.json')
        Assert-True ($null -ne $metadataEntry) 'Packaged Canary lacks fabric.mod.json.'
        $reader = [IO.StreamReader]::new($metadataEntry.Open())
        try { $metadata = $reader.ReadToEnd() | ConvertFrom-Json }
        finally { $reader.Dispose() }
        Assert-True ($metadata.id -ceq 'dramaticdoors' -and $metadata.version -ceq '1.20.1-3.3.3+26.2-workbench-canary5') "Unexpected packaged identity: $($metadata.id) $($metadata.version)."
    }
    finally {
        $archive.Dispose()
    }
    $artifactLabel = [IO.Path]::GetFileName($artifactPath)
}

Write-Output "DRAMATIC_DOORS_MACAW_INITIALIZATION_COMPAT_OK: keyed stable/sliding factories; WATERLOGGED-optional DoorBlock mixin; artifact=$artifactLabel"
