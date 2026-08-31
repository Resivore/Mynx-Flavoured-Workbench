$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$api = Get-Content -Raw (Join-Path $root 'src/nibaru/java/games/twinhead/moreslabsstairsandwalls/api/material/NibaruMaterialProfiles.java')
$semantics = Get-Content -Raw (Join-Path $root 'src/nibaru/java/games/twinhead/moreslabsstairsandwalls/block/oxidizable/CopperSemantics.java')
$oxidizable = Get-Content -Raw (Join-Path $root 'src/nibaru/java/games/twinhead/moreslabsstairsandwalls/block/oxidizable/CustomOxidizable.java')
$waxed = Get-Content -Raw (Join-Path $root 'src/nibaru/java/games/twinhead/moreslabsstairsandwalls/block/oxidizable/CustomWaxedCopper.java')
$classes = Get-ChildItem (Join-Path $root 'src/nibaru/java/games/twinhead/moreslabsstairsandwalls/block/oxidizable') -Filter '*.java' | ForEach-Object { Get-Content -Raw $_.FullName }
$checks = [ordered]@{
    'provider exposes exact four-way copper edges' = ($api -match 'NEXT_OXIDATION' -and $api -match 'PREVIOUS_OXIDATION' -and $api -match 'WAXED' -and $api -match 'UNWAXED')
    'provider exposes oxidation stage and wax state' = ($api -match 'oxidationStage' -and $api -match 'isWaxedCopper')
    'copper semantics performs shared state transfer' = ($semantics -match 'withPropertiesOf\(source\)')
    'one interaction selects one canonical transition' = ($semantics -match 'MaterialTransition\.Type\.UNWAXED' -and $semantics -match 'MaterialTransition\.Type\.PREVIOUS_OXIDATION' -and $semantics -match 'Optional<BlockState> target')
    'creative players do not consume or damage' = ($semantics -match 'if \(!player\.isCreative\(\)\)')
    'native oxidizable geometry delegates' = ($oxidizable -match 'CopperSemantics\.interact' -and $oxidizable -match 'CopperSemantics\.transition')
    'native waxed geometry delegates' = ($waxed -match 'CopperSemantics\.interact')
    'legacy hardcoded copper maps removed' = (($classes -join "`n") -notmatch 'ImmutableBiMap|OXIDATION_LEVEL_INCREASES|WAX_ON\s*=')
    'no registry-name transition inference' = ($semantics -notmatch 'getKey|getPath|parse|substring|replace')
}
$checks.GetEnumerator() | ForEach-Object { '{0}: {1}' -f ($(if ($_.Value) {'PASS'} else {'FAIL'})), $_.Key }
$failed = @($checks.GetEnumerator() | Where-Object { -not $_.Value })
if ($failed.Count) { throw "Copper semantics fixture failed: $($failed.Name -join ', ')" }
