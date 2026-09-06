param(
    [string]$ProjectRoot = (Split-Path -Parent $PSScriptRoot)
)

$ErrorActionPreference = 'Stop'
$ProjectRoot = [IO.Path]::GetFullPath($ProjectRoot)
$staged = Join-Path $ProjectRoot 'build\generated\original-resources'
if (-not (Test-Path -LiteralPath $staged -PathType Container)) {
    throw 'Staged Naturalist resources are absent. Run stageOriginalResources first.'
}

function Assert-Contains([string]$Text, [string]$Needle, [string]$Label) {
    if (-not $Text.Contains($Needle)) { throw "$Label is missing '$Needle'." }
}
function Assert-NotContains([string]$Text, [string]$Needle, [string]$Label) {
    if ($Text.Contains($Needle)) { throw "$Label still contains '$Needle'." }
}
function Read-Text([string]$Path) { Get-Content -LiteralPath $Path -Raw }

$registry = Read-Text (Join-Path $ProjectRoot 'common\src\main\java\com\crispytwig\naturalist\registry\NaturalistRegistry.java')
$removedRegistrations = @(
    'bushmeat', 'fur', 'tooth', 'fat', 'hide', 'morsel', 'cooked_egg', 'antler',
    'duck', 'cooked_duck', 'venison', 'cooked_venison', 'drumstick', 'cooked_drumstick',
    'mammoth_meat', 'cooked_mammoth_meat', 'lizard_tail', 'cooked_lizard_tail',
    'catfish', 'cooked_catfish', 'bass', 'cooked_bass', 'anglerfish', 'cooked_anglerfish',
    'blobfish', 'cooked_blobfish', 'piranha', 'cooked_piranha', 'clam_meat',
    'cooked_clam_meat', 'crab_meat', 'cooked_crab_meat', 'scorpion_poison_gland',
    'queen_ant', 'ant_hill', 'snail_shell', 'azure_froglass', 'verdant_froglass',
    'crimson_froglass', 'shellstone', 'whistle', 'plush_bear'
)
foreach ($id in $removedRegistrations) {
    if ($registry -match ('register(?:Item|Block|BlockOnly)\("' + [regex]::Escape($id) + '"')) {
        throw "Retired registry ID naturalist:$id is still registered."
    }
}

$bear = Read-Text (Join-Path $ProjectRoot 'common\src\main\java\com\crispytwig\naturalist\server\entity\mob\Bear.java')
$scorpion = Read-Text (Join-Path $ProjectRoot 'common\src\main\java\com\crispytwig\naturalist\server\entity\mob\Scorpion.java')
$whale = Read-Text (Join-Path $ProjectRoot 'common\src\main\java\com\crispytwig\naturalist\server\entity\mob\Whale.java')
Assert-Contains $bear 'Items.RABBIT_HIDE' 'Bear shearing'
Assert-Contains $scorpion 'Items.RABBIT' 'Scorpion healing'
Assert-NotContains $scorpion 'LIZARD_TAIL' 'Scorpion healing'
Assert-NotContains $whale 'CRAB_MEAT' 'Whale food support'

$removedItemIds = @(
    'naturalist:bushmeat', 'naturalist:cooked_bushmeat', 'naturalist:fur', 'naturalist:tooth',
    'naturalist:fat', 'naturalist:hide', 'naturalist:morsel', 'naturalist:cooked_egg',
    'naturalist:antler', 'naturalist:venison', 'naturalist:cooked_venison',
    'naturalist:drumstick', 'naturalist:cooked_drumstick', 'naturalist:mammoth_meat',
    'naturalist:cooked_mammoth_meat', 'naturalist:lizard_tail', 'naturalist:cooked_lizard_tail',
    'naturalist:duck', 'naturalist:catfish', 'naturalist:cooked_catfish', 'naturalist:bass',
    'naturalist:cooked_bass', 'naturalist:anglerfish', 'naturalist:cooked_anglerfish',
    'naturalist:blobfish', 'naturalist:cooked_blobfish', 'naturalist:piranha', 'naturalist:cooked_piranha', 'naturalist:clam_meat',
    'naturalist:cooked_clam_meat', 'naturalist:crab_meat', 'naturalist:cooked_crab_meat',
    'naturalist:scorpion_poison_gland', 'naturalist:whistle', 'naturalist:plush_bear'
)
$itemData = Get-ChildItem -LiteralPath $staged -Recurse -File -Filter '*.json' | Where-Object {
    $_.FullName -match '[\\/]tags[\\/]item[\\/]' -or $_.FullName -match '[\\/]recipe[\\/]' -or $_.FullName -match '[\\/]loot_table[\\/]'
}
foreach ($file in $itemData) {
    $text = Read-Text $file.FullName
    foreach ($id in $removedItemIds) { Assert-NotContains $text ('"' + $id + '"') $file.FullName }
}

foreach ($needle in @('whistle', 'plush_bear', 'teddy_bear')) {
    Get-ChildItem -LiteralPath $staged -Recurse -File | ForEach-Object {
        Assert-NotContains (Read-Text $_.FullName) $needle $_.FullName
    }
}

$anglerLoot = Get-ChildItem -LiteralPath (Join-Path $staged 'data\naturalist\loot_table\entities') -File |
    Where-Object { (Read-Text $_.FullName) -match 'minecraft:glow_ink_sac' }
if ($anglerLoot.Count -ne 1) { throw 'Expected exactly one Anglerfish Glow Ink Sac loot table.' }
Assert-NotContains (Read-Text $anglerLoot.FullName) 'minecraft:glowstone_dust' 'Anglerfish loot'

$goopRecipe = Join-Path $ProjectRoot 'src\main\resources\data\naturalist\recipe\glow_goop_from_glow_berries.json'
$goopText = Read-Text $goopRecipe
Assert-Contains $goopText 'minecraft:crafting_shapeless' 'Glow Goop recipe'
Assert-Contains $goopText 'minecraft:glow_berries' 'Glow Goop recipe'
Assert-Contains $goopText 'naturalist:glow_goop' 'Glow Goop recipe'
Get-ChildItem -LiteralPath (Join-Path $staged 'data\naturalist\recipe') -File | ForEach-Object {
    $text = Read-Text $_.FullName
    if ($text.Contains('naturalist:glow_goop') -and $text -match 'minecraft:(smelting|smoking|campfire_cooking)') {
        throw "Retired Glow Berry cooking recipe survived: $($_.Name)"
    }
}

$build = Read-Text (Join-Path $ProjectRoot 'build.gradle')
foreach ($id in @('glow_goop', 'capture_net', 'music_disc_wild_ones', 'music_disc_death_by_hogs', 'duck_egg',
        'chrysalis', 'red_starfish', 'orange_starfish', 'blue_starfish', 'purple_starfish',
        'alligator_egg', 'tortoise_egg', 'ostrich_egg', 'snail_eggs')) {
    Assert-Contains $build "'$id'" '26.2 client-item generator'
    $root = Join-Path $ProjectRoot "build\generated\item-model-compat\assets\naturalist\items\$id.json"
    if (-not (Test-Path -LiteralPath $root -PathType Leaf)) { throw "Missing generated client-item root for $id." }
    Assert-Contains (Read-Text $root) "naturalist:item/$id" "client-item root $id"
}

Write-Host 'Naturalist content cull and retained 26.2 item roots verified.'
