[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$sourcePath = Join-Path $PSScriptRoot '..\common\src\main\java\com\fizzware\dramaticdoors\mixin\FenceGateBlockMixin.java'
$source = Get-Content -LiteralPath $sourcePath -Raw -Encoding UTF8
$descriptor = 'updateShape(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/world/level/ScheduledTickAccess;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/util/RandomSource;)Lnet/minecraft/world/level/block/state/BlockState;'
$oldDescriptorFragment = 'Lnet/minecraft/core/Direction;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelAccessor;'

$expectedInjection = '@Inject(at = @At("HEAD"), method = "' + $descriptor + '")'
if (-not $source.Contains($expectedInjection)) {
    throw 'FenceGateBlockMixin does not target the exact Minecraft 26.2 updateShape descriptor at HEAD.'
}
if ($source.Contains($oldDescriptorFragment)) {
    throw 'FenceGateBlockMixin still contains the obsolete six-argument updateShape descriptor.'
}

$handlerStart = $source.IndexOf('private void injectUpdateShape(', [StringComparison]::Ordinal)
$handlerEnd = $source.IndexOf('public FluidState getFluidState', $handlerStart, [StringComparison]::Ordinal)
if ($handlerStart -lt 0 -or $handlerEnd -le $handlerStart) {
    throw 'FenceGateBlockMixin updateShape handler could not be isolated.'
}
$handler = $source.Substring($handlerStart, $handlerEnd - $handlerStart)
foreach ($required in @(
    'LevelReader level',
    'ScheduledTickAccess tickAccess',
    'RandomSource random',
    'if (stateIn.getValue(WATERLOGGED))',
    'tickAccess.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(level));'
)) {
    if (-not $handler.Contains($required)) { throw "FenceGateBlockMixin handler lacks: $required" }
}
foreach ($forbidden in @(
    'stateIn = stateIn.setValue(WATERLOGGED',
    'callback.setReturnValue',
    'callback.cancel()'
)) {
    if ($handler.Contains($forbidden)) { throw "FenceGateBlockMixin handler retains ineffective return-state behavior: $forbidden" }
}

Write-Output 'FENCE_GATE_MIXIN_26_2_STATIC_OK'
