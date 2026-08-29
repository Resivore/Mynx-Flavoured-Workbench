$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$profiles = Get-Content -Raw -LiteralPath (Join-Path $projectRoot 'common\src\main\java\games\twinhead\moreslabsstairsandwalls\api\material\NibaruMaterialProfiles.java')
$profile = Get-Content -Raw -LiteralPath (Join-Path $projectRoot 'common\src\main\java\games\twinhead\moreslabsstairsandwalls\api\material\NibaruMaterialProfile.java')
$blocks = Get-Content -Raw -LiteralPath (Join-Path $projectRoot 'common\src\main\java\games\twinhead\moreslabsstairsandwalls\block\ModBlocks.java')
$generated = Join-Path $projectRoot 'common\src\main\generated\assets\more_slabs_stairs_and_walls\models\block'
$quartz = Get-Content -Raw -LiteralPath (Join-Path $generated 'quartz_pillar_slab.json') | ConvertFrom-Json
$smooth = Get-Content -Raw -LiteralPath (Join-Path $generated 'smooth_quartz_wall_inventory.json') | ConvertFrom-Json
$checks = [ordered]@{
    'profile exposes explicit tint contract' = ($profile -match 'TintProfile tintProfile')
    'profile exposes semantic texture roles' = ($profile -match 'String side, String top, String bottom, String overlay, String particle')
    'profile exposes render orientation and double policies' = ($profile -match 'RenderLayer renderLayer' -and $profile -match 'OrientationPolicy orientationPolicy' -and $profile -match 'DoubleFormPolicy doubleFormPolicy')
    'profile exposes structured UV sampling policy' = ($profile -match 'SurfaceSamplingPolicy surfaceSamplingPolicy' -and $profile -match 'NATIVE_STAIR_SURFACE_BAND' -and $profile -match 'PATH_LOWERED_SURFACE')
    'true leaf behavior derives from canonical LeavesBlock' = ($profiles -match 'family\.parentBlock instanceof LeavesBlock')
    'wart leaf class reuse is excluded from visual semantics' = ($profiles -match 'ModelType\.LEAVES.*!\(family\.parentBlock instanceof LeavesBlock\)')
    'Grass and Mycelium tint are independently classified' = ($profiles -match 'family == ModBlocks\.GRASS_BLOCK' -and $profiles -match '!\(family\.parentBlock instanceof LeavesBlock\)\) return TintProfile\.NONE')
    'historical GRASS families select native stair surface band' = ($profiles -match 'family\.modelType == ModBlocks\.ModelType\.GRASS' -and $profiles -match 'NATIVE_STAIR_SURFACE_BAND')
    'Dirt Path selects lowered surface policy' = ($profiles -match 'family\.modelType == ModBlocks\.ModelType\.PATH' -and $profiles -match 'PATH_LOWERED_SURFACE')
    'Quartz provider role uses current side texture' = ($blocks -match 'setTextures\("quartz_pillar_side", "quartz_pillar_top"\)')
    'generated Quartz native model uses current side texture' = ($quartz.textures.side -eq 'minecraft:block/quartz_pillar_side')
    'Smooth Quartz remains canonical bottom texture' = ($smooth.textures.wall -eq 'minecraft:block/quartz_block_bottom')
    'ROOTS profile publishes distinct side top and particle roles' = ($profiles -match 'visual == VisualProfile\.ROOTS' -and $profiles -match 'side = base \+ "_side"' -and $profiles -match 'top = base \+ "_top"' -and $profiles -match 'bottom = top' -and $profiles -match 'particle = visual == VisualProfile\.ROOTS \? side : bottom')
    'Muddy Mangrove Roots remains cube-bottom-top' = ($blocks -match 'MUDDY_MANGROVE_ROOTS\(builder\(Blocks\.MUDDY_MANGROVE_ROOTS\).*ModelType\.CUBE_BOTTOM_TOP')
}
$checks.GetEnumerator() | ForEach-Object { '{0}: {1}' -f ($(if ($_.Value) { 'PASS' } else { 'FAIL' })), $_.Key }
$failed = @($checks.GetEnumerator() | Where-Object { -not $_.Value })
if ($failed.Count) { throw "Nibaru visual-semantics fixture failed: $($failed.Name -join ', ')" }
