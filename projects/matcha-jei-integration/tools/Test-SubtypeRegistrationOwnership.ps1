$ErrorActionPreference = 'Stop'

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$sourcePath = Join-Path $projectRoot 'src\main\java\dev\resivore\matchajei\client\MatchaJeiPlugin.java'
$manifestPath = Join-Path $projectRoot 'src\main\resources\matcha_jei_integration\component-item-ids.txt'
$source = Get-Content -LiteralPath $sourcePath -Raw -Encoding UTF8
$match = [regex]::Match(
    $source,
    'JEI_OWNED_SUBTYPE_ITEMS\s*=\s*Set\.of\s*\((?<items>.*?)\);',
    [Text.RegularExpressions.RegexOptions]::Singleline
)
if (-not $match.Success) {
    throw 'JEI-owned subtype exclusion set is missing.'
}

$actual = @(
    [regex]::Matches($match.Groups['items'].Value, '"(?<item>[^"]+)"') |
        ForEach-Object { $_.Groups['item'].Value } |
        Sort-Object
)
$expected = @(
    'decorated_pot',
    'enchanted_book',
    'firework_rocket',
    'firework_star',
    'goat_horn',
    'light',
    'lingering_potion',
    'ominous_bottle',
    'painting',
    'potion',
    'shield',
    'splash_potion',
    'suspicious_stew',
    'tipped_arrow'
)
if (($actual | ConvertTo-Json -Compress) -cne ($expected | ConvertTo-Json -Compress)) {
    throw "JEI-owned subtype exclusions changed: $($actual -join ', ')"
}
if ($source -notmatch 'if\s*\(\s*JEI_OWNED_SUBTYPE_ITEMS\.contains\s*\([^)]*\)\s*\)\s*\{\s*continue\s*;\s*\}') {
    throw 'JEI-owned subtype items are not skipped before companion registration.'
}

$componentItems = @(
    Get-Content -LiteralPath $manifestPath -Encoding UTF8 |
        ForEach-Object { $_.Trim() } |
        Where-Object { $_ -and -not $_.StartsWith('#') }
)
$actualIntersection = @(
    $actual |
        Where-Object { "minecraft:$_" -cin $componentItems } |
        Sort-Object
)
$expectedIntersection = @(
    'enchanted_book',
    'goat_horn',
    'potion',
    'shield',
    'splash_potion',
    'tipped_arrow'
)
if (($actualIntersection | ConvertTo-Json -Compress) -cne ($expectedIntersection | ConvertTo-Json -Compress)) {
    throw "Audited JEI-owned component intersection changed: $($actualIntersection -join ', ')"
}

Write-Output 'SUBTYPE_REGISTRATION_OWNERSHIP_OK: JEI retains all 14 vanilla subtype interpreters; audited Matcha intersection=6'
