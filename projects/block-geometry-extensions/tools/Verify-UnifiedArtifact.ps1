param(
    [string]$UnifiedJar = (Join-Path $PSScriptRoot '..\build\libs\cnm-nibaru-integration-4.2.1-bge.canary57.unified+26.2.jar'),
    [string]$BgeC56Jar = (Join-Path $PSScriptRoot '..\artifacts\cnm-nibaru-integration-0.8.0-bge-canary56-vertical-stairs-catalog.jar'),
    [string]$NibaruC46Jar = (Join-Path $PSScriptRoot '..\..\nibaru\artifacts\more-slabs-stairs-and-walls-4.2.0+26.2-port-canary46-bge-layer-contract.jar')
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem

function Require([bool]$Condition, [string]$Message) {
    if (-not $Condition) { throw $Message }
}

function Get-StreamSha256([System.IO.Stream]$Stream) {
    $sha = [System.Security.Cryptography.SHA256]::Create()
    try {
        return [System.BitConverter]::ToString($sha.ComputeHash($Stream)).Replace('-', '').ToLowerInvariant()
    } finally {
        $sha.Dispose()
    }
}

function Get-FileSha256([string]$Path) {
    $stream = [System.IO.File]::OpenRead($Path)
    try { return Get-StreamSha256 $stream } finally { $stream.Dispose() }
}

function Get-EntryMap([System.IO.Compression.ZipArchive]$Archive) {
    $map = @{}
    foreach ($entry in $Archive.Entries) {
        if ($entry.FullName.EndsWith('/')) { continue }
        Require (-not $map.ContainsKey($entry.FullName)) "Duplicate archive entry: $($entry.FullName)"
        $map[$entry.FullName] = $entry
    }
    return ,$map
}

function Get-EntrySha256([System.IO.Compression.ZipArchiveEntry]$Entry) {
    $stream = $Entry.Open()
    try { return Get-StreamSha256 $stream } finally { $stream.Dispose() }
}

function Get-EntryText([System.IO.Compression.ZipArchiveEntry]$Entry) {
    $reader = [System.IO.StreamReader]::new($Entry.Open(), [System.Text.Encoding]::UTF8, $true)
    try { return $reader.ReadToEnd() } finally { $reader.Dispose() }
}

function Get-TagValueSet([hashtable]$Map, [string]$Path) {
    Require $Map.ContainsKey($Path) "Missing tag entry: $Path"
    $json = Get-EntryText $Map[$Path] | ConvertFrom-Json
    Require ($null -ne $json.values) "Tag has no values array: $Path"
    $set = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::Ordinal)
    foreach ($value in @($json.values)) {
        $key = if ($value -is [string]) {
            "string:$value"
        } else {
            'json:' + ($value | ConvertTo-Json -Depth 50 -Compress)
        }
        [void]$set.Add($key)
    }
    return ,$set
}

function New-StringSet([string[]]$Values) {
    $set = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::Ordinal)
    foreach ($value in $Values) { [void]$set.Add($value) }
    return ,$set
}

$unifiedPath = (Resolve-Path -LiteralPath $UnifiedJar).Path
$c56Path = (Resolve-Path -LiteralPath $BgeC56Jar).Path
$c46Path = (Resolve-Path -LiteralPath $NibaruC46Jar).Path

Require ((Get-FileSha256 $c56Path) -eq '26c76fbd82d0d72632d151d3674ca731817f1dadaff503e46fbfb4372d880f55') `
        'Exact BGE C56 predecessor hash mismatch'
Require ((Get-FileSha256 $c46Path) -eq '3281d110f062db62e721d838a35ce915ca73dd41af098a013b52952561ceae7d') `
        'Exact Nibaru C46 predecessor hash mismatch'

$unified = [System.IO.Compression.ZipFile]::OpenRead($unifiedPath)
$c56 = [System.IO.Compression.ZipFile]::OpenRead($c56Path)
$c46 = [System.IO.Compression.ZipFile]::OpenRead($c46Path)
try {
    $unifiedMap = Get-EntryMap $unified
    $c56Map = Get-EntryMap $c56
    $c46Map = Get-EntryMap $c46

    $descriptors = @($unifiedMap.Keys | Where-Object { $_ -eq 'fabric.mod.json' -or $_.EndsWith('/fabric.mod.json') })
    Require ($descriptors.Count -eq 1 -and $descriptors[0] -eq 'fabric.mod.json') `
            "Expected one root Fabric descriptor; found: $($descriptors -join ', ')"
    $nestedJars = @($unifiedMap.Keys | Where-Object { $_.EndsWith('.jar', [System.StringComparison]::OrdinalIgnoreCase) })
    Require ($nestedJars.Count -eq 0) "Nested JARs are forbidden: $($nestedJars -join ', ')"

    $metadataText = Get-EntryText $unifiedMap['fabric.mod.json']
    $metadata = $metadataText | ConvertFrom-Json
    Require ($metadata.id -eq 'cnm_terrain_slabs_compat') 'Unified primary Fabric ID changed'
    Require ($metadata.version -eq '4.2.1-bge.canary57.unified+26.2') 'Unified Fabric version changed'
    Require (@($metadata.provides).Count -eq 1 -and $metadata.provides[0] -eq 'more_slabs_stairs_and_walls') `
            'Unified descriptor must provide exactly the legacy Nibaru ID'
    Require ($metadata.PSObject.Properties.Name -notcontains 'jars') 'Unified descriptor must not declare nested JARs'
    Require ($metadata.depends.PSObject.Properties.Name -notcontains 'more_slabs_stairs_and_walls') `
            'Unified descriptor retains a legacy-ID self-dependency'
    Require (@($metadata.entrypoints.main).Count -eq 1 -and $metadata.entrypoints.main[0] -eq 'dev.aero.cnmterraincompat.CnmTerrainCompat') `
            'Unified descriptor must expose one BGE common entrypoint'
    Require (@($metadata.entrypoints.client).Count -eq 1 -and $metadata.entrypoints.client[0] -eq 'dev.aero.cnmterraincompat.CnmTerrainCompatClient') `
            'Unified descriptor must expose one BGE client entrypoint'
    Require ($metadataText -notmatch 'MoreSlabsStairsAndWallsFabric') 'Obsolete Nibaru wrapper entrypoint remains in metadata'
    $expectedMixins = New-StringSet @(
        'more_slabs_stairs_and_walls-common.mixins.json',
        'more_slabs_stairs_and_walls.mixins.json',
        'cnm_terrain_slabs_compat.mixins.json'
    )
    $actualMixins = New-StringSet @($metadata.mixins)
    Require $actualMixins.SetEquals($expectedMixins) 'Unified mixin configuration set is incomplete or duplicated'
    foreach ($mixin in $expectedMixins) { Require $unifiedMap.ContainsKey($mixin) "Packaged mixin config missing: $mixin" }

    $tagPaths = @(
        'data/minecraft/tags/block/dirt.json',
        'data/minecraft/tags/block/enables_bubble_column_drag_down.json',
        'data/minecraft/tags/block/enables_bubble_column_push_up.json',
        'data/minecraft/tags/block/leaves.json',
        'data/minecraft/tags/block/mineable/hoe.json',
        'data/minecraft/tags/block/mineable/shovel.json',
        'data/minecraft/tags/block/slabs.json',
        'data/minecraft/tags/block/soul_fire_base_blocks.json',
        'data/minecraft/tags/block/soul_speed_blocks.json'
    )
    foreach ($path in $tagPaths) {
        $expected = Get-TagValueSet $c46Map $path
        [void]$expected.UnionWith((Get-TagValueSet $c56Map $path))
        $actual = Get-TagValueSet $unifiedMap $path
        Require $actual.SetEquals($expected) "Shared tag is not the exact C56+C46 set union: $path"
        $tagJson = Get-EntryText $unifiedMap[$path] | ConvertFrom-Json
        Require (@($tagJson.values).Count -eq $actual.Count) "Unified shared tag contains duplicate values: $path"
        Require ($tagJson.replace -eq $false) "Shared tag must retain additive semantics: $path"
    }

    $expectedC56Changes = New-StringSet (@(
        'fabric.mod.json',
        'dev/aero/cnmterraincompat/CnmTerrainCompat.class',
        'dev/aero/cnmterraincompat/CnmTerrainCompatClient.class',
        'dev/aero/cnmterraincompat/CnmTerrainCompatClient$1.class',
        'dev/aero/cnmterraincompat/mixin/ClutterNoMoreVariantScanMixin.class'
    ) + $tagPaths)
    $c56Changed = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::Ordinal)
    $c56Identical = 0
    foreach ($name in $c56Map.Keys) {
        Require $unifiedMap.ContainsKey($name) "Unified artifact lost BGE C56 entry: $name"
        if ((Get-EntrySha256 $c56Map[$name]) -eq (Get-EntrySha256 $unifiedMap[$name])) {
            $c56Identical++
        } else {
            [void]$c56Changed.Add($name)
        }
    }
    Require $c56Changed.SetEquals($expectedC56Changes) `
            "Unexplained BGE C56 archive differences: $(@($c56Changed) -join ', ')"

    $wrapperClasses = New-StringSet @(
        'games/twinhead/moreslabsstairsandwalls/fabric/MoreSlabsStairsAndWallsFabric.class',
        'games/twinhead/moreslabsstairsandwalls/fabric/MoreSlabsStairsAndWallsFabricClient.class',
        'games/twinhead/moreslabsstairsandwalls/fabric/MoreSlabsStairsAndWallsFabricClient$1.class'
    )
    $expectedC46Changes = New-StringSet (@('fabric.mod.json', 'LICENSE') + $tagPaths)
    $c46Missing = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::Ordinal)
    $c46Changed = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::Ordinal)
    $c46Identical = 0
    foreach ($name in $c46Map.Keys) {
        if (-not $unifiedMap.ContainsKey($name)) {
            [void]$c46Missing.Add($name)
            continue
        }
        if ((Get-EntrySha256 $c46Map[$name]) -eq (Get-EntrySha256 $unifiedMap[$name])) {
            $c46Identical++
        } else {
            [void]$c46Changed.Add($name)
        }
    }
    Require $c46Missing.SetEquals($wrapperClasses) `
            "Unified artifact lost unexpected Nibaru C46 entries: $(@($c46Missing) -join ', ')"
    Require $c46Changed.SetEquals($expectedC46Changes) `
            "Unexplained Nibaru C46 archive differences: $(@($c46Changed) -join ', ')"
    foreach ($wrapper in $wrapperClasses) { Require (-not $unifiedMap.ContainsKey($wrapper)) "Obsolete wrapper class remains: $wrapper" }

    $nibaruClasses = @($c46Map.Keys | Where-Object {
        $_ -match '^games/twinhead/moreslabsstairsandwalls/.+\.class$' -and -not $wrapperClasses.Contains($_)
    })
    Require ($nibaruClasses.Count -eq 158) "Expected 158 retained Nibaru implementation classes; found $($nibaruClasses.Count)"
    foreach ($name in $nibaruClasses) {
        Require ((Get-EntrySha256 $c46Map[$name]) -eq (Get-EntrySha256 $unifiedMap[$name])) `
                "Retained Nibaru implementation class changed: $name"
    }

    $glassGeneratorEntries = @($c56Map.Keys | Where-Object {
        $_ -match '^dev/aero/cnmterraincompat/(?:client/(?:BgeGeneratedResources|CornerColumnModelProjection|CuboidListModelProjection|GeneratedItemModelSupport|QuarterGeometryGeneratedResources)|QuarterGeometryGeneratedData).*\.class$'
    })
    Require ($glassGeneratorEntries.Count -ge 18) 'Glass/Corner generator chain inventory is unexpectedly incomplete'
    foreach ($name in $glassGeneratorEntries) {
        Require ((Get-EntrySha256 $c56Map[$name]) -eq (Get-EntrySha256 $unifiedMap[$name])) `
                "C56 glass-Corner generator bytecode changed: $name"
    }
    $glassTemplates = @($c56Map.Keys | Where-Object {
        $_ -match '^assets/clutternomore/models/block/templates/provider/glass_.+\.json$'
    })
    Require ($glassTemplates.Count -eq 5) "Expected five C56 glass provider templates; found $($glassTemplates.Count)"
    foreach ($name in $glassTemplates) {
        Require ((Get-EntrySha256 $c56Map[$name]) -eq (Get-EntrySha256 $unifiedMap[$name])) `
                "C56 glass provider template changed: $name"
    }
    $nibaruGlassInputs = @($c46Map.Keys | Where-Object {
        $_ -match '^assets/more_slabs_stairs_and_walls/.+(?:glass|glazed).+\.json$'
    })
    Require ($nibaruGlassInputs.Count -gt 100) 'Nibaru glass input inventory is unexpectedly incomplete'
    foreach ($name in $nibaruGlassInputs) {
        Require ((Get-EntrySha256 $c46Map[$name]) -eq (Get-EntrySha256 $unifiedMap[$name])) `
                "C46 glass input changed: $name"
    }

    Require $unifiedMap.ContainsKey('META-INF/licenses/more-slabs-stairs-and-walls-LGPL-3.0-or-later.txt') `
            'Packaged LGPL license is missing'
    Require ((Get-EntrySha256 $c46Map['LICENSE']) -eq `
            (Get-EntrySha256 $unifiedMap['META-INF/licenses/more-slabs-stairs-and-walls-LGPL-3.0-or-later.txt'])) `
            'Packaged LGPL license differs from exact Nibaru C46 license'
    Require ((Get-EntrySha256 $c56Map['LICENSE']) -eq (Get-EntrySha256 $unifiedMap['LICENSE'])) `
            'BGE root license changed during consolidation'
    Require $unifiedMap.ContainsKey('META-INF/NOTICE') 'Unified component and licensing notice is missing'

    $knownUnion = New-StringSet (@($c56Map.Keys) + @($c46Map.Keys))
    $extras = New-StringSet @($unifiedMap.Keys | Where-Object { -not $knownUnion.Contains($_) })
    $expectedExtras = New-StringSet @(
        'META-INF/licenses/more-slabs-stairs-and-walls-LGPL-3.0-or-later.txt',
        'META-INF/NOTICE'
    )
    Require $extras.SetEquals($expectedExtras) "Unexpected entries outside the C56+C46 union: $(@($extras) -join ', ')"

    $namespace = 'more_slabs_stairs_and_walls'
    $blockstates = @($unifiedMap.Keys | Where-Object { $_ -match "^assets/$namespace/blockstates/[^/]+\.json$" })
    $ids = @($blockstates | ForEach-Object { [System.IO.Path]::GetFileNameWithoutExtension($_) })
    $slabs = @($ids | Where-Object { $_.EndsWith('_slab') })
    $stairs = @($ids | Where-Object { $_.EndsWith('_stairs') })
    $walls = @($ids | Where-Object { $_.EndsWith('_wall') })
    $families = @($ids | ForEach-Object { $_ -replace '_(slab|stairs|wall)$', '' } | Sort-Object -Unique)
    $itemDefinitions = @($unifiedMap.Keys | Where-Object { $_ -match "^assets/$namespace/items/[^/]+\.json$" })
    $itemModels = @($unifiedMap.Keys | Where-Object { $_ -match "^assets/$namespace/models/item/[^/]+\.json$" })
    $blockModels = @($unifiedMap.Keys | Where-Object { $_ -match "^assets/$namespace/models/block/.+\.json$" })
    $lootTables = @($unifiedMap.Keys | Where-Object { $_ -match "^data/$namespace/loot_table/blocks/[^/]+\.json$" })
    $recipes = @($unifiedMap.Keys | Where-Object { $_ -match "^data/$namespace/recipes?/.*\.json$" })
    Require ($blockstates.Count -eq 866 -and $slabs.Count -eq 276 -and $stairs.Count -eq 279 -and $walls.Count -eq 311) `
            'Native registry/resource inventory changed'
    Require ($families.Count -eq 311) 'Canonical family inventory changed'
    Require ($itemDefinitions.Count -eq 867 -and $itemModels.Count -eq 867 -and $blockModels.Count -eq 6895) `
            'Native client resource inventory changed'
    Require ($lootTables.Count -eq 866 -and $recipes.Count -eq 0) 'Native server resource inventory changed'
    $wallValues = Get-TagValueSet $unifiedMap 'data/minecraft/tags/block/walls.json'
    $nativeWallValues = @($wallValues | Where-Object { $_.StartsWith("string:${namespace}:") })
    Require ($nativeWallValues.Count -eq 311) 'Native wall tag inventory changed'

    [ordered]@{
        result = 'PASS'
        unified = [ordered]@{
            filename = [System.IO.Path]::GetFileName($unifiedPath)
            size = (Get-Item -LiteralPath $unifiedPath).Length
            sha256 = Get-FileSha256 $unifiedPath
            fabric_descriptors = $descriptors.Count
            nested_jars = $nestedJars.Count
        }
        bge_c56 = [ordered]@{ identical_entries = $c56Identical; intentional_changes = $c56Changed.Count; missing = 0 }
        nibaru_c46 = [ordered]@{ identical_entries = $c46Identical; intentional_changes = $c46Changed.Count; removed_wrapper_classes = $c46Missing.Count }
        native_inventory = [ordered]@{
            blockstates = $blockstates.Count
            slabs = $slabs.Count
            stairs = $stairs.Count
            walls = $walls.Count
            families = $families.Count
            item_definitions = $itemDefinitions.Count
            item_models = $itemModels.Count
            block_models = $blockModels.Count
            loot_tables = $lootTables.Count
            recipes = $recipes.Count
        }
        compatibility = [ordered]@{
            shared_tag_unions = $tagPaths.Count
            retained_nibaru_classes = $nibaruClasses.Count
            glass_generator_entries_unchanged = $glassGeneratorEntries.Count
            glass_templates_unchanged = $glassTemplates.Count
            glass_inputs_unchanged = $nibaruGlassInputs.Count
        }
    } | ConvertTo-Json -Depth 10
} finally {
    $unified.Dispose()
    $c56.Dispose()
    $c46.Dispose()
}
