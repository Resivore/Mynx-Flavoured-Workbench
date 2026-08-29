$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$common = Join-Path $root 'common\src\main\java\games\twinhead\moreslabsstairsandwalls'
$profiles = Get-Content -Raw -LiteralPath (Join-Path $common 'api\material\NibaruMaterialProfiles.java')
$contract = Get-Content -Raw -LiteralPath (Join-Path $common 'api\material\NibaruMaterialProfile.java')
$honey = Get-Content -Raw -LiteralPath (Join-Path $common 'block\honey\HoneySemantics.java')
$slime = Get-Content -Raw -LiteralPath (Join-Path $common 'block\slime\SlimeSemantics.java')
$sticky = Get-Content -Raw -LiteralPath (Join-Path $common 'block\slime\StickyMaterialSemantics.java')
$piston = Get-Content -Raw -LiteralPath (Join-Path $root 'fabric\src\main\java\games\twinhead\moreslabsstairsandwalls\fabric\mixin\PistonHandlerMixin.java')

$checks = [ordered]@{
    'provider profile version retains Honey/Slime semantics in current catalog' = $profiles.Contains('canary40-pale-coverage-v1')
    'pure inset visual contract is provider-owned' = ($contract.Contains('record InsetVisualContract') -and $contract.Contains('ShellTexture'))
    'Honey publishes canonical side top bottom roles' = ($profiles.Contains('honey_block_side') -and $profiles.Contains('honey_block_top') -and $profiles.Contains('honey_block_bottom'))
    'Honey and Slime publish distinct inset dimensions' = ($profiles.Contains('1, 1, NibaruMaterialProfile.InsetVisualContract') -and $profiles.Contains('3, 2, NibaruMaterialProfile.InsetVisualContract'))
    'Honey slide contact consumes supplied collision geometry' = ($honey.Contains('VoxelShape collisionShape') -and $honey.Contains('collisionShape.toAabbs()'))
    'Honey negative space requires actual boundary contact' = ($honey.Contains('Math.abs(entityBox.maxX - occupied.minX)') -and $honey.Contains('Math.abs(entityBox.minZ - occupied.maxZ)'))
    'Slime semantics preserve fall and horizontal callbacks' = ($slime.Contains('suppressFallDamage') -and $slime.Contains('modifyHorizontalMovement'))
    'typed sticky classifier uses material profiles' = ($sticky.Contains('NibaruMaterialProfiles.fromBlock') -and $sticky.Contains('profile.family() == ModBlocks.HONEY_BLOCK') -and $sticky.Contains('profile.family() == ModBlocks.SLIME_BLOCK'))
    'typed sticky classifier has no registry-name parsing' = ($sticky -notmatch 'getPath|substring|replace\(|toString\(')
    'Fabric piston mixin delegates only classification semantics' = ($piston.Contains('StickyMaterialSemantics.isSticky') -and $piston.Contains('StickyMaterialSemantics.isOpposedPair'))
    'provider semantics contain no CNM classes' = (($honey + $slime + $sticky) -notmatch 'clutternomore|cnmterraincompat|VerticalSlabBlock|StepBlock')
}

$glazedHashes = [ordered]@{
    'glazed_terracotta_slab.json' = '330B49153B99D4D4A0D9715A9FE433ABAC259431B1A76921C610E7462270BF51'
    'glazed_terracotta_slab_top.json' = '71394FB27DAAAD34D6C4500B6A2D7743939176CAA3EB0A4E0E07AFF8DD2D3940'
    'glazed_terracotta_stairs.json' = '0A9312B25D28827CF63F8258ABBEFF252AE6317E4153C17C8B3A75BC99A484A3'
    'glazed_terracotta_stairs_inner.json' = 'D0F45585F6313441F320F3E8D9138BD3093E08E1D403EC2D2ABABBA8BE3F5BFA'
    'glazed_terracotta_stairs_outer.json' = '7BBDDDCB9D32F94907AF180D98B88B000348FF928A51591B605A55377FBB8D8C'
}
$glazedRoot = Join-Path $root 'common\src\main\resources\assets\more_slabs_stairs_and_walls\models\block'
foreach ($entry in $glazedHashes.GetEnumerator()) {
    $actual = (Get-FileHash -LiteralPath (Join-Path $glazedRoot $entry.Key) -Algorithm SHA256).Hash
    $checks["Canary 38 Glazed resource frozen: $($entry.Key)"] = $actual -eq $entry.Value
}

$failed = @($checks.GetEnumerator() | Where-Object { -not $_.Value })
foreach ($check in $checks.GetEnumerator()) {
    '{0}: {1}' -f ($(if ($check.Value) { 'PASS' } else { 'FAIL' })), $check.Key
}
if ($failed.Count -gt 0) { throw "Honey/Slime provider fixture failed: $($failed.Key -join ', ')" }
