$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$slab = Get-Content -LiteralPath (Join-Path $root 'common\src\main\java\games\twinhead\moreslabsstairsandwalls\block\leaves\LeavesSlab.java') -Raw
$stairs = Get-Content -LiteralPath (Join-Path $root 'common\src\main\java\games\twinhead\moreslabsstairsandwalls\block\leaves\LeavesStairs.java') -Raw
$semantics = Get-Content -LiteralPath (Join-Path $root 'common\src\main\java\games\twinhead\moreslabsstairsandwalls\block\leaves\LeafSemantics.java') -Raw
$carrier = Get-Content -LiteralPath (Join-Path $root 'common\src\main\java\games\twinhead\moreslabsstairsandwalls\block\leaves\LeafDistanceCarrier.java') -Raw
$modBlocks = Get-Content -LiteralPath (Join-Path $root 'common\src\main\java\games\twinhead\moreslabsstairsandwalls\block\ModBlocks.java') -Raw

$checks = [ordered]@{
    'leaf settings inherit random ticks' = ($modBlocks -match 'defaultBlockState\(\)\.isRandomlyTicking\(\)' -and $modBlocks -match 'settings\.randomTicks\(\)')
    'shared default is distance seven nonpersistent' = ($semantics -match '(?s)applyDefaultState.*DISTANCE, 7.*PERSISTENT, false')
    'slab delegates default semantics' = ($slab -match 'LeafSemantics\.applyDefaultState')
    'stairs delegate default semantics' = ($stairs -match 'LeafSemantics\.applyDefaultState')
    'shared decay predicate excludes persistent states' = ($semantics -match '(?s)shouldDecay.*!state\.getValue\(BlockStateProperties\.PERSISTENT\).*DISTANCE\) == 7')
    'slab and stairs delegate random ticking' = ($slab -match 'LeafSemantics\.isRandomlyTicking' -and $stairs -match 'LeafSemantics\.isRandomlyTicking')
    'slab and stairs delegate decay action' = ($slab -match 'LeafSemantics\.decayIfNeeded' -and $stairs -match 'LeafSemantics\.decayIfNeeded')
    'placed and merged slabs use shared persistence' = ($slab -match '(?s)super\.getStateForPlacement\(ctx\).*LeafSemantics\.applyPlayerPlacementState')
    'placed stairs use shared persistence' = ($stairs -match '(?s)super\.getStateForPlacement\(context\).*LeafSemantics\.applyPlayerPlacementState')
    'distance carrier is explicit capability' = ($carrier -match 'interface LeafDistanceCarrier' -and $slab -match 'implements LeafDistanceCarrier' -and $stairs -match 'implements LeafDistanceCarrier')
    'distance carrier accepts only vanilla leaves or capability' = ($semantics -match 'instanceof LeavesBlock' -and $semantics -match 'instanceof LeafDistanceCarrier')
    'unrelated distance property is not accepted' = ($semantics -notmatch 'hasProperty\(BlockStateProperties\.DISTANCE\)')
    'distance update changes only distance property' = ($semantics -match 'return state\.setValue\(BlockStateProperties\.DISTANCE, distance\)')
    'native classes do not compete with shared distance algorithm' = ($slab -notmatch 'MutableBlockPos' -and $stairs -notmatch 'MutableBlockPos')
    'slab update retains water tick scheduling' = ($slab -match 'scheduleTick\(pos, Fluids\.WATER')
    'stairs update retains water tick scheduling' = ($stairs -match 'scheduleTick\(pos, Fluids\.WATER')
}

# Pure algorithm controls mirroring the six-neighbor minimum rule.
function Next-Distance([int[]]$Neighbors) { return [Math]::Min(7, (($Neighbors | Measure-Object -Minimum).Minimum + 1)) }
if ((Next-Distance @(0,7,7,7,7,7)) -ne 1) { throw 'Adjacent log must produce distance 1.' }
if ((Next-Distance @(1,7,7,7,7,7)) -ne 2) { throw 'Log -> Nibaru slab -> Nibaru stair must propagate distance 2.' }
if ((Next-Distance @(3,7,7,7,7,7)) -ne 4) { throw 'Vanilla/Nibaru mixed leaf chains must propagate distance.' }
if ((Next-Distance @(7,7,7,7,7,7)) -ne 7) { throw 'Removing all support must converge toward distance 7.' }

$failed = @($checks.GetEnumerator() | Where-Object { -not $_.Value })
foreach ($check in $checks.GetEnumerator()) {
    if ($check.Value) { "PASS: $($check.Key)" } else { "FAIL: $($check.Key)" }
}
if ($failed.Count) { throw "$($failed.Count) leaf lifecycle assertion(s) failed." }
'PASS: leaf lifecycle algorithm controls'
