$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$pathStairs = Get-Content -Raw -LiteralPath (Join-Path $projectRoot 'src\nibaru\java\games\twinhead\moreslabsstairsandwalls\block\dirt\PathStairs.java')
$modelRoot = Join-Path $projectRoot 'src\nibaru\resources\assets\more_slabs_stairs_and_walls\models\block'

$checks = [ordered]@{
    'bottom stair unions upper corner volumes' = ($pathStairs -match 'BOTTOM_SHAPES = makePathShapes\(BOTTOM_SHAPE, TOP_NORTH_WEST_CORNER_SHAPE')
    'top stair unions lower corner volumes' = ($pathStairs -match 'TOP_SHAPES = makePathShapes\(TOP_SHAPE, BOTTOM_NORTH_WEST_CORNER_SHAPE')
    'outline shape is the corrected path stair shape' = ($pathStairs -match 'public VoxelShape getShape')
    'placement retains shared Path lifecycle' = ($pathStairs -match 'PathSemantics\.placementState')
}

Get-ChildItem -LiteralPath $modelRoot -Filter 'dirt_path_stairs*.json' | ForEach-Object {
    $json = Get-Content -Raw -LiteralPath $_.FullName
    $json | ConvertFrom-Json | Out-Null
    if ($json -match '"cullface"\s*:\s*"(?:north|east|south|west)"') {
        throw "$($_.Name) retains an unsafe lateral cullface on partial stair geometry"
    }
}
$checks['all six native Path Stair models avoid partial-neighbor lateral culling'] =
        ((Get-ChildItem -LiteralPath $modelRoot -Filter 'dirt_path_stairs*.json').Count -eq 6)

$checks.GetEnumerator() | ForEach-Object { '{0}: {1}' -f ($(if ($_.Value) { 'PASS' } else { 'FAIL' })), $_.Key }
$failed = @($checks.GetEnumerator() | Where-Object { -not $_.Value })
if ($failed.Count) { throw "Native Path contract fixture failed: $($failed.Name -join ', ')" }
