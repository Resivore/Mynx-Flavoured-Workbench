$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$spread = Join-Path $root 'src\nibaru\java\games\twinhead\moreslabsstairsandwalls\block\spreadable'
$semantics = Get-Content -Raw -LiteralPath (Join-Path $spread 'SpreadableSemantics.java')
$geometry = Get-Content -Raw -LiteralPath (Join-Path $spread 'SpreadableGeometry.java')
$slab = Get-Content -Raw -LiteralPath (Join-Path $spread 'SpreadableSlab.java')
$stairs = Get-Content -Raw -LiteralPath (Join-Path $spread 'SpreadableStairs.java')
$wall = Get-Content -Raw -LiteralPath (Join-Path $spread 'SpreadableWall.java')
$mixin = Get-Content -Raw -LiteralPath (Join-Path $root 'src\nibaru\java\games\twinhead\moreslabsstairsandwalls\mixin\SpreadableBlockMixin.java')
$checks = [ordered]@{
    'public geometry-neutral API exists' = ($semantics -match 'public final class SpreadableSemantics' -and $semantics -notmatch 'clutternomore|cnmterrain')
    'geometry capability is CNM-neutral' = ($geometry -match 'interface SpreadableGeometry' -and $geometry -notmatch 'clutternomore|cnmterrain')
    'pair registration supports external derived geometry' = ($semantics -match 'public static void registerPair')
    'pair uniqueness covers both base and spreadable identity' = ($semantics -match 'pair\.base == base \|\| pair\.spreadable == spreadable')
    'state conversion copies common geometry properties' = ($semantics -match 'copyShared')
    'shared lifecycle owns survival death and spread' = ($semantics -match 'public static boolean canSurvive' -and $semantics -match 'public static void randomTick' -and $semantics -match 'public static boolean trySpread')
    'bottom slab exposure and water semantics preserved' = ($slab -match 'SlabType\.BOTTOM' -and $slab -match 'WATERLOGGED.*Exposure\.BLOCKED.*Exposure\.EXPOSED')
    'native slab delegates shared lifecycle' = ($slab -match 'SpreadableSemantics\.randomTick')
    'native stairs delegate shared lifecycle' = ($stairs -match 'SpreadableSemantics\.randomTick')
    'native wall delegates shared lifecycle' = ($wall -match 'SpreadableSemantics\.randomTick')
    'vanilla source mixin delegates shared conversion' = ($mixin -match 'SpreadableSemantics\.trySpread')
    'native classes no longer own competing random spread loops' = (@($slab,$stairs,$wall | Where-Object { $_ -notmatch 'randomTick\([^\)]*\)\s*\{\s*SpreadableSemantics\.randomTick\([^;]+;\s*\}' }).Count -eq 0)
}
$failed = @($checks.GetEnumerator() | Where-Object { -not $_.Value })
$checks.GetEnumerator() | ForEach-Object { '{0}: {1}' -f ($(if ($_.Value) { 'PASS' } else { 'FAIL' })), $_.Key }
if ($failed.Count) { throw "Shared spreadable semantics fixture failed: $($failed.Name -join ', ')" }
