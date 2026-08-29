$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path

$treeMixin = Get-Content -LiteralPath (Join-Path $root 'common\src\main\java\games\twinhead\moreslabsstairsandwalls\mixin\TreeDecoratorGeneratorMixin.java') -Raw
$alterMixin = Get-Content -LiteralPath (Join-Path $root 'common\src\main\java\games\twinhead\moreslabsstairsandwalls\mixin\AlterGroundDecoratorMixin.java') -Raw
$conversion = Get-Content -LiteralPath (Join-Path $root 'common\src\main\java\games\twinhead\moreslabsstairsandwalls\block\spreadable\PodzolGeometryConversion.java') -Raw
$client = Get-Content -LiteralPath (Join-Path $root 'fabric\src\main\java\games\twinhead\moreslabsstairsandwalls\fabric\MoreSlabsStairsAndWallsFabricClient.java') -Raw

if ($treeMixin -match 'public\s+static|protected\s+static') {
    throw 'TreeDecoratorGeneratorMixin contains a non-private static method that Mixin will reject.'
}
if ($treeMixin -notmatch 'PodzolGeometryConversion\.convert' -or $alterMixin -notmatch 'PodzolGeometryConversion\.convert') {
    throw 'Both tree-decorator paths must delegate to the ordinary podzol conversion utility.'
}
foreach ($required in @('GRASS_BLOCK', 'PODZOL', 'DIRT', 'COARSE_DIRT', 'MYCELIUM', 'ROOTED_DIRT',
                         'withPropertiesOf')) {
    if ($conversion -notmatch [regex]::Escape($required)) { throw "Missing podzol conversion invariant: $required" }
}
if ($conversion -match 'BlockType\.WALL\)\s*continue') {
    throw 'Podzol conversion must preserve wall geometry instead of falling through to a full block.'
}
if ($client -match 'BlockColors\.createDefault') {
    throw 'Client tint setup must not initialize Fabric against a temporary BlockColors table.'
}
foreach ($required in @('BlockTintSources.grassBlock()', 'BlockTintSources.foliage()', '0xFF619961', '0xFF80A755')) {
    if ($client -notmatch [regex]::Escape($required)) { throw "Missing exact vanilla tint source: $required" }
}

foreach ($model in @('template_grass_slab.json', 'template_grass_stairs.json', 'template_grass_wall_post.json',
                      'template_leaves_slab.json', 'template_leaves_stairs.json', 'template_leaves_wall_post.json')) {
    $path = Join-Path $root "common\src\main\resources\assets\more_slabs_stairs_and_walls\models\block\$model"
    if ((Get-Content -LiteralPath $path -Raw) -notmatch '"tintindex"\s*:\s*0') {
        throw "Representative tinted model lacks tintindex 0: $model"
    }
}

$generator = Get-Content -LiteralPath (Join-Path $root 'tools\Generate-CurrentResources.ps1') -Raw
foreach ($required in @('minecraft:grass', '-12012264', '-7158200', '-10380959', '-8345771', 'definition.model.tints')) {
    if ($generator -notmatch [regex]::Escape($required)) { throw "Missing item-preview tint invariant: $required" }
}

'PASS: podzol/mixin and block/item tint architecture assertions'
