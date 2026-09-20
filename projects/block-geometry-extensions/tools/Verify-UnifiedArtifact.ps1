param(
    [string]$UnifiedJar = (Join-Path $PSScriptRoot '..\build\libs\BGE C84.jar'),
    [string]$AcceptedJar = (Join-Path $PSScriptRoot '..\artifacts\cnm-nibaru-integration-4.2.2-bge.canary58.glass-corner-uv+26.2.jar'),
    [string]$PredecessorJar = (Join-Path $PSScriptRoot '..\artifacts\BGE C80.jar')
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

function Get-EntryBytes([System.IO.Compression.ZipArchiveEntry]$Entry) {
    $stream = $Entry.Open()
    $memory = [System.IO.MemoryStream]::new()
    try {
        $stream.CopyTo($memory)
        return ,$memory.ToArray()
    } finally {
        $memory.Dispose()
        $stream.Dispose()
    }
}

function Get-EntryText([System.IO.Compression.ZipArchiveEntry]$Entry) {
    $reader = [System.IO.StreamReader]::new($Entry.Open(), [System.Text.Encoding]::UTF8, $true)
    try { return $reader.ReadToEnd() } finally { $reader.Dispose() }
}

function New-StringSet([string[]]$Values) {
    $set = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::Ordinal)
    foreach ($value in $Values) { [void]$set.Add($value) }
    return ,$set
}

function Test-ContainsBytes([byte[]]$Bytes, [byte[]]$Needle) {
    if ($Needle.Length -eq 0) { return $true }
    if ($Bytes.Length -lt $Needle.Length) { return $false }
    for ($offset = 0; $offset -le $Bytes.Length - $Needle.Length; $offset++) {
        $matches = $true
        for ($index = 0; $index -lt $Needle.Length; $index++) {
            if ($Bytes[$offset + $index] -ne $Needle[$index]) {
                $matches = $false
                break
            }
        }
        if ($matches) { return $true }
    }
    return $false
}

function Test-AllowedChangedEntry([string]$Name) {
    return $Name -eq 'fabric.mod.json' -or $Name -eq 'META-INF/MANIFEST.MF' -or
            $Name -match '^dev/aero/cnmterraincompat/(?:BgeColumnBlock|BgeCornerBlock|BgeLayerSpecializedBlocks|ExternalMaterialBlocks|ExternalMaterialCatalog|ExternalMaterialFamilies|InsetModelContract)(?:\$.*)?\.class$' -or
            $Name -eq 'dev/aero/cnmterraincompat/BgeBlockItem.class' -or
            $Name -match '^dev/aero/cnmterraincompat/BgeMaterialBindings(?:\$.*)?\.class$' -or
            $Name -match '^dev/aero/cnmterraincompat/CnmShapeMapCandidateBridge(?:\$.*)?\.class$' -or
            $Name -match '^dev/aero/cnmterraincompat/DeferredCnmGeometryBlock(?:\$.*)?\.class$' -or
            $Name -match '^dev/aero/cnmterraincompat/ResolvedCnmCandidateData(?:\$.*)?\.class$' -or
            $Name -match '^dev/aero/cnmterraincompat/client/ResolvedCnmCandidateResources(?:\$.*)?\.class$' -or
            $Name -match '^dev/aero/cnmterraincompat/AxisModelContract(?:\$.*)?\.class$' -or
            $Name -eq 'cnm_terrain_slabs_compat.mixins.json' -or
            $Name -eq 'assets/cnm_terrain_slabs_compat/lang/en_us.json' -or
            $Name -eq 'assets/more_slabs_stairs_and_walls/lang/en_us.json' -or
            $Name -eq 'data/minecraft/tags/block/mineable/shovel.json' -or
            $Name -match '^dev/aero/cnmterraincompat/CnmTerrainCompat(?:\$.*)?\.class$' -or
            $Name -eq 'dev/aero/cnmterraincompat/CanonicalGeometryRegistry.class' -or
            $Name -eq 'games/twinhead/moreslabsstairsandwalls/registry/fabric/ModRegistry.class' -or
            $Name -match '^dev/aero/cnmterraincompat/CnmTerrainCompatClient(?:\$.*)?\.class$' -or
            $Name -match '^dev/aero/cnmterraincompat/NibaruProviderAdapter(?:\$.*)?\.class$' -or
            $Name -match '^dev/aero/cnmterraincompat/mixin/ShapeMapOrderMixin(?:\$.*)?\.class$' -or
            $Name -match '^dev/aero/cnmterraincompat/client/BgeGeneratedResources(?:\$.*)?\.class$' -or
            $Name -match '^dev/aero/cnmterraincompat/client/LayerGeneratedResources(?:\$.*)?\.class$' -or
            $Name -match '^dev/aero/cnmterraincompat/client/LayerModelProjection(?:\$.*)?\.class$' -or
            $Name -match '^dev/aero/cnmterraincompat/client/CuboidListModelProjection(?:\$.*)?\.class$' -or
            $Name -match '^dev/aero/cnmterraincompat/client/QuarterGeometryGeneratedResources(?:\$.*)?\.class$' -or
            $Name -match '^dev/aero/cnmterraincompat/mixin/BgeFuelValuesBuilderMixin(?:\$.*)?\.class$' -or
            $Name -match '^games/twinhead/moreslabsstairsandwalls/api/material/NativeAxisModelContract(?:\$.*)?\.class$' -or
            $Name -match '^games/twinhead/moreslabsstairsandwalls/api/material/CanonicalPillarTextureResolver(?:\$.*)?\.class$' -or
            $Name -match '^games/twinhead/moreslabsstairsandwalls/api/material/NibaruMaterialProfiles(?:\$.*)?\.class$' -or
            $Name -match '^games/twinhead/moreslabsstairsandwalls/api/material/NibaruMaterialProfile(?:\$.*)?\.class$' -or
            $Name -eq 'games/twinhead/moreslabsstairsandwalls/api/material/VisualProfile.class' -or
            $Name -match '^games/twinhead/moreslabsstairsandwalls/api/material/TintProfile(?:\$.*)?\.class$' -or
            $Name -match '^games/twinhead/moreslabsstairsandwalls/block/ModBlocks(?:\$.*)?\.class$' -or
            $Name -eq 'games/twinhead/moreslabsstairsandwalls/tools/NativeAxisResourceGenerator.class' -or
            $Name -eq 'dev/aero/cnmterraincompat/mixin/ClutterNoMoreVariantScanMixin.class' -or
            $Name -match '^dev/aero/cnmterraincompat/client/ExternalMaterialGeneratedResources(?:\$.*)?\.class$' -or
            $Name -match '^assets/more_slabs_stairs_and_walls/models/block/purpur_pillar_(?:slab|stairs)(?:_.*)?\.json$' -or
            $Name -match '^assets/more_slabs_stairs_and_walls/models/block/[^/]+_wall_inventory\.json$'
}

function Test-AllowedNewEntry([string]$Name) {
    return $Name -match '^games/twinhead/moreslabsstairsandwalls/api/material/NibaruMaterialProfiles\$VanillaFamily\.class$' -or
            $Name -match '^dev/aero/cnmterraincompat/BgeLayerSpecializedBlocks(?:\$.*)?\.class$' -or
            $Name -match '^dev/aero/cnmterraincompat/FarmlandSlabBlock(?:\$.*)?\.class$' -or
            $Name -match '^dev/aero/cnmterraincompat/FarmlandSlabTilling(?:\$.*)?\.class$' -or
            $Name -match '^dev/aero/cnmterraincompat/mixin/HoeItemAccessor(?:\$.*)?\.class$' -or
            $Name -match '^assets/cnm_terrain_slabs_compat/blockstates/farmland_slab\.json$' -or
            $Name -match '^assets/cnm_terrain_slabs_compat/models/block/farmland_slab(?:_top|_double)?(?:_moist)?\.json$' -or
            $Name -match '^data/cnm_terrain_slabs_compat/loot_table/blocks/farmland_slab\.json$' -or
            $Name -match '^dev/aero/cnmterraincompat/CnmTerrainCompatClient(?:\$.*)?\.class$' -or
            $Name -match '^dev/aero/cnmterraincompat/CanonicalShapeMapAudit(?:\$.*)?\.class$' -or
            $Name -match '^dev/aero/cnmterraincompat/(?:EnderscapeMaterialGeometry|ExternalMaterialStateBridge)(?:\$.*)?\.class$' -or
            $Name -match '^dev/aero/cnmterraincompat/ExternalMaterial(?:Blocks|Catalog|Families|GeneratedData)(?:\$.*)?\.class$' -or
            $Name -match '^dev/aero/cnmterraincompat/client/ExternalMaterialGeneratedResources(?:\$.*)?\.class$' -or
            $Name -match '^dev/aero/cnmterraincompat/mixin/(?:MacawsPaths|MynxTrees|Ribbits|Bbb|Enderscape)InitializationMixin(?:\$.*)?\.class$' -or
            $Name -match '^dev/aero/cnmterraincompat/client/BgeGeneratedResourceWriter(?:\$.*)?\.class$' -or
            $Name -match '^dev/aero/cnmterraincompat/BgeMaterialBindings(?:\$.*)?\.class$' -or
            $Name -match '^dev/aero/cnmterraincompat/BgeSurfaceGeometry(?:\$.*)?\.class$' -or
            $Name -match '^dev/aero/cnmterraincompat/HugeMushroom(?:GeometryBlocks|Material)(?:\$.*)?\.class$' -or
            $Name -match '^dev/aero/cnmterraincompat/HugeMushroom(?:Surface|SlabBlock|VerticalSlabBlock|ColumnBlock|LayerBlock|StepBlock|CornerBlock|StairsBlock|WallBlock)(?:\$.*)?\.class$' -or
            $Name -eq 'dev/aero/cnmterraincompat/FullOccupancyNormalizer.class' -or
            $Name -eq 'dev/aero/cnmterraincompat/mixin/BlockItemPlacementMixin.class' -or
            $Name -match '^dev/aero/cnmterraincompat/CnmShapeMapCandidateBridge(?:\$.*)?\.class$' -or
            $Name -match '^dev/aero/cnmterraincompat/DeferredCnmGeometryBlock(?:\$.*)?\.class$' -or
            $Name -match '^dev/aero/cnmterraincompat/ResolvedCnmCandidateData(?:\$.*)?\.class$' -or
            $Name -match '^dev/aero/cnmterraincompat/client/ResolvedCnmCandidateResources(?:\$.*)?\.class$' -or
            $Name -match '^games/twinhead/moreslabsstairsandwalls/api/material/CanonicalPillarTextureResolver(?:\$.*)?\.class$' -or
            $Name -match '^assets/more_slabs_stairs_and_walls/blockstates/purpur_pillar_(?:slab|stairs)\.json$' -or
            $Name -match '^assets/more_slabs_stairs_and_walls/models/block/purpur_pillar_(?:slab|stairs)(?:_.*)?\.json$' -or
            $Name -eq 'assets/more_slabs_stairs_and_walls/models/block/purpur_pillar_wall_inventory.json' -or
            $Name -eq 'games/twinhead/moreslabsstairsandwalls/tools/NativeMaterialResourceGenerator.class' -or
            $Name -match '^assets/more_slabs_stairs_and_walls/(?:blockstates|items|models/(?:block|item))/[a-z0-9_]+_(?:slab|stairs|wall)(?:_(?:top|bottom|double|inner|outer|post|side(?:_tall)?|inventory))?\.json$' -or
            $Name -match '^data/more_slabs_stairs_and_walls/loot_table/blocks/[a-z0-9_]+_(?:slab|stairs|wall)\.json$'
}

$unifiedPath = (Resolve-Path -LiteralPath $UnifiedJar).Path
$acceptedPath = (Resolve-Path -LiteralPath $AcceptedJar).Path
$predecessorPath = (Resolve-Path -LiteralPath $PredecessorJar).Path

Require ((Get-FileSha256 $acceptedPath) -eq '1a4e4d1cd9c8709720ec84975e70caffb5552ac676537b9bbae42dca96567e87') `
        'Exact accepted unified BGE C58 boundary hash mismatch'
Require ((Get-FileSha256 $predecessorPath) -eq '5bc7c23a3724a1e20bc2142459f33d2a45a19295c27d40197c5fc48d6e43b0fc') `
        'Exact BGE C80 predecessor hash mismatch'
Require ([System.IO.Path]::GetFileName($unifiedPath) -ceq 'BGE C84.jar') `
        'Distributable artifact filename is not exactly BGE C84.jar'

$unified = [System.IO.Compression.ZipFile]::OpenRead($unifiedPath)
$accepted = [System.IO.Compression.ZipFile]::OpenRead($acceptedPath)
$predecessor = [System.IO.Compression.ZipFile]::OpenRead($predecessorPath)
try {
    $unifiedMap = Get-EntryMap $unified
    $acceptedMap = Get-EntryMap $accepted
    $predecessorMap = Get-EntryMap $predecessor

    $descriptors = @($unifiedMap.Keys | Where-Object { $_ -eq 'fabric.mod.json' -or $_.EndsWith('/fabric.mod.json') })
    Require ($descriptors.Count -eq 1 -and $descriptors[0] -eq 'fabric.mod.json') `
            "Expected one root Fabric descriptor; found: $($descriptors -join ', ')"
    $nestedJars = @($unifiedMap.Keys | Where-Object { $_.EndsWith('.jar', [System.StringComparison]::OrdinalIgnoreCase) })
    Require ($nestedJars.Count -eq 0) "Nested JARs are forbidden: $($nestedJars -join ', ')"
    $cnmClasses = @($unifiedMap.Keys | Where-Object { $_ -match '^dev/tazer/clutternomore/' })
    Require ($cnmClasses.Count -eq 0) "C84 must retain stock CNM as an external dependency: $($cnmClasses -join ', ')"

    $metadataText = Get-EntryText $unifiedMap['fabric.mod.json']
    $metadata = $metadataText | ConvertFrom-Json
    Require ($metadata.id -eq 'cnm_terrain_slabs_compat') 'Unified primary Fabric ID changed'
    Require ($metadata.version -eq '4.2.28-bge.canary84.ender-material-state-bridge+26.2') `
            'Unified Fabric version is not exact C84'
    Require ($metadata.name -eq ('Block Geometry Extensions Canary 84 ' + [char]0x2014 + ' Enderscape Material-State Bridge')) `
            'Unified Fabric display name is not exact C84'
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
    $integrationMixins = Get-EntryText $unifiedMap['cnm_terrain_slabs_compat.mixins.json']
    foreach ($providerHook in @('MacawsPathsInitializationMixin', 'MynxTreesInitializationMixin', 'RibbitsInitializationMixin', 'BbbInitializationMixin', 'EnderscapeInitializationMixin')) {
        Require ($integrationMixins -match [regex]::Escape($providerHook)) "Provider completion hook is not packaged: $providerHook"
    }
    Require ($integrationMixins -match 'HoeItemAccessor') 'C72 HoeItem tillable-map accessor is not packaged'
    Require ($integrationMixins -match 'BlockItemPlacementMixin') 'C73 placement normalization mixin is not packaged'
    Require ($integrationMixins -notmatch 'AxeItemAccessor') 'C83 must not package the retired C79 axe accessor'

    $missing = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::Ordinal)
    $changed = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::Ordinal)
    $identical = 0
    foreach ($name in $acceptedMap.Keys) {
        if (-not $unifiedMap.ContainsKey($name)) {
            [void]$missing.Add($name)
            continue
        }
        if ((Get-EntrySha256 $acceptedMap[$name]) -eq (Get-EntrySha256 $unifiedMap[$name])) {
            $identical++
        } else {
            [void]$changed.Add($name)
        }
    }
    Require ($missing.Count -eq 0) "C73 lost retained accepted-C58 entries: $(@($missing) -join ', ')"

    $newEntries = New-StringSet @($unifiedMap.Keys | Where-Object { -not $acceptedMap.ContainsKey($_) })
    $unexpectedChanges = @($changed | Where-Object { -not (Test-AllowedChangedEntry $_) })
    $unexpectedNew = @($newEntries | Where-Object { -not (Test-AllowedNewEntry $_) })
    Require ($unexpectedChanges.Count -eq 0) `
            "C73 changed entries outside its exact implementation whitelist: $($unexpectedChanges -join ', ')"
    Require ($unexpectedNew.Count -eq 0) `
            "C73 added entries outside its exact implementation/resource whitelist: $($unexpectedNew -join ', ')"

    foreach ($required in @(
        'fabric.mod.json',
        'assets/cnm_terrain_slabs_compat/lang/en_us.json',
        'data/minecraft/tags/block/mineable/shovel.json',
        'dev/aero/cnmterraincompat/CnmTerrainCompat.class',
        'dev/aero/cnmterraincompat/BgeColumnBlock.class',
        'dev/aero/cnmterraincompat/NibaruProviderAdapter.class',
        'dev/aero/cnmterraincompat/mixin/ShapeMapOrderMixin.class',
        'dev/aero/cnmterraincompat/client/BgeGeneratedResources.class',
        'dev/aero/cnmterraincompat/client/LayerGeneratedResources.class',
        'dev/aero/cnmterraincompat/client/QuarterGeometryGeneratedResources.class',
        'dev/aero/cnmterraincompat/mixin/BgeFuelValuesBuilderMixin.class',
        'games/twinhead/moreslabsstairsandwalls/api/material/NativeAxisModelContract.class',
        'games/twinhead/moreslabsstairsandwalls/api/material/NibaruMaterialProfiles.class'
    )) {
        Require $changed.Contains($required) "Required retained post-C58 archive change is absent: $required"
    }
    foreach ($required in @(
        'dev/aero/cnmterraincompat/CnmShapeMapCandidateBridge.class',
        'dev/aero/cnmterraincompat/ResolvedCnmCandidateData.class',
        'dev/aero/cnmterraincompat/client/ResolvedCnmCandidateResources.class',
        'dev/aero/cnmterraincompat/ExternalMaterialCatalog.class',
        'dev/aero/cnmterraincompat/CanonicalShapeMapAudit.class',
        'dev/aero/cnmterraincompat/ExternalMaterialBlocks.class',
        'dev/aero/cnmterraincompat/ExternalMaterialFamilies.class',
        'dev/aero/cnmterraincompat/ExternalMaterialGeneratedData.class',
        'dev/aero/cnmterraincompat/client/ExternalMaterialGeneratedResources.class',
        'dev/aero/cnmterraincompat/mixin/MacawsPathsInitializationMixin.class',
        'dev/aero/cnmterraincompat/mixin/MynxTreesInitializationMixin.class',
            'dev/aero/cnmterraincompat/mixin/RibbitsInitializationMixin.class',
            'dev/aero/cnmterraincompat/mixin/BbbInitializationMixin.class'
    )) {
        Require $newEntries.Contains($required) "Required retained post-C58 class is absent: $required"
    }
    foreach ($required in @(
        'dev/aero/cnmterraincompat/client/BgeGeneratedResourceWriter.class',
        'dev/aero/cnmterraincompat/mixin/EnderscapeInitializationMixin.class',
        'dev/aero/cnmterraincompat/ExternalMaterialStateBridge.class',
        'dev/aero/cnmterraincompat/EnderscapeMaterialGeometry.class'
    )) {
        Require $newEntries.Contains($required) "Required C84 class is absent: $required"
    }
    Require (-not $unifiedMap.ContainsKey('dev/aero/cnmterraincompat/CnmAxisFamilyBridge.class')) `
            'C83 must not package the retired registry-wide C79 axis scanner'
    Require (-not $unifiedMap.ContainsKey('dev/aero/cnmterraincompat/mixin/AxeItemAccessor.class')) `
            'C83 must not package the retired C79 axe accessor'
    foreach ($required in @(
        'dev/aero/cnmterraincompat/FarmlandSlabBlock.class',
        'dev/aero/cnmterraincompat/FarmlandSlabBlock$1.class',
        'dev/aero/cnmterraincompat/FarmlandSlabTilling.class',
        'dev/aero/cnmterraincompat/FarmlandSlabTilling$Rule.class',
        'dev/aero/cnmterraincompat/mixin/HoeItemAccessor.class',
        'assets/cnm_terrain_slabs_compat/blockstates/farmland_slab.json',
        'assets/cnm_terrain_slabs_compat/models/block/farmland_slab.json',
        'assets/cnm_terrain_slabs_compat/models/block/farmland_slab_moist.json',
        'assets/cnm_terrain_slabs_compat/models/block/farmland_slab_top.json',
        'assets/cnm_terrain_slabs_compat/models/block/farmland_slab_top_moist.json',
        'assets/cnm_terrain_slabs_compat/models/block/farmland_slab_double.json',
        'assets/cnm_terrain_slabs_compat/models/block/farmland_slab_double_moist.json',
        'data/cnm_terrain_slabs_compat/loot_table/blocks/farmland_slab.json'
    )) {
        Require $newEntries.Contains($required) "Required C73-retained Farmland Slab entry is absent: $required"
    }
    Require (-not $unifiedMap.ContainsKey('assets/cnm_terrain_slabs_compat/items/farmland_slab.json')) `
            'C73 must not expose a Farmland Slab item definition'
    Require (-not $unifiedMap.ContainsKey('assets/cnm_terrain_slabs_compat/models/item/farmland_slab.json')) `
            'C73 must not expose a Farmland Slab item model'

    $predecessorMissing = New-StringSet @($predecessorMap.Keys | Where-Object { -not $unifiedMap.ContainsKey($_) })
    $predecessorChanged = New-StringSet @($predecessorMap.Keys | Where-Object {
        $unifiedMap.ContainsKey($_) -and
                (Get-EntrySha256 $predecessorMap[$_]) -ne (Get-EntrySha256 $unifiedMap[$_])
    })
    $predecessorNew = New-StringSet @($unifiedMap.Keys | Where-Object { -not $predecessorMap.ContainsKey($_) })
    $requiredPredecessorChanges = New-StringSet @(
        'fabric.mod.json',
        'dev/aero/cnmterraincompat/CnmTerrainCompat.class',
        'dev/aero/cnmterraincompat/CnmShapeMapCandidateBridge.class',
        'dev/aero/cnmterraincompat/BgeMaterialBindings.class',
        'games/twinhead/moreslabsstairsandwalls/api/material/NibaruMaterialProfiles.class',
        'games/twinhead/moreslabsstairsandwalls/api/material/NativeAxisModelContract.class',
        'assets/more_slabs_stairs_and_walls/models/block/purpur_pillar_wall_inventory.json'
    )
    Require ($predecessorMissing.Count -eq 0) `
            "C84 lost exact C80 predecessor entries: $(@($predecessorMissing) -join ', ')"
    Require (@($predecessorChanged | Where-Object { -not (Test-AllowedChangedEntry $_) }).Count -eq 0) `
            "C84 changed entries outside its C80-bounded scope: $(@($predecessorChanged | Where-Object { -not (Test-AllowedChangedEntry $_) }) -join ', ')"
    Require (@($requiredPredecessorChanges | Where-Object { -not $predecessorChanged.Contains($_) }).Count -eq 0) `
            "C84 omitted required C80-bounded changes: $(@($requiredPredecessorChanges | Where-Object { -not $predecessorChanged.Contains($_) }) -join ', ')"
    Require (@($predecessorNew | Where-Object { -not (Test-AllowedNewEntry $_) }).Count -eq 0) `
            "C84 added entries beyond its exact C80-bounded scope: $(@($predecessorNew | Where-Object { -not (Test-AllowedNewEntry $_) }) -join ', ')"

    $interiorTexture = [System.Text.Encoding]::UTF8.GetBytes('ribbits:block/toadstool_inside')
    foreach ($name in $unifiedMap.Keys) {
        Require (-not (Test-ContainsBytes (Get-EntryBytes $unifiedMap[$name]) $interiorTexture)) `
                "C78 packages a generated-geometry route to the forbidden Ribbits interior texture: $name"
    }

    $predecessorLanguage = (Get-EntryText $predecessorMap['assets/cnm_terrain_slabs_compat/lang/en_us.json']) | ConvertFrom-Json
    $c73Language = (Get-EntryText $unifiedMap['assets/cnm_terrain_slabs_compat/lang/en_us.json']) | ConvertFrom-Json
    $predecessorLanguageProperties = @($predecessorLanguage.PSObject.Properties)
    $c73LanguageProperties = @($c73Language.PSObject.Properties)
    Require ($c73LanguageProperties.Count -eq $predecessorLanguageProperties.Count) `
            'C73 unexpectedly changed the Farmland Slab language inventory'
    foreach ($property in $predecessorLanguageProperties) {
        Require ($c73Language.PSObject.Properties.Name -contains $property.Name) `
                "C73 removed language key: $($property.Name)"
        Require ($c73Language.$($property.Name) -eq $property.Value) `
                "C73 changed retained language key: $($property.Name)"
    }
    Require ($c73Language.'block.cnm_terrain_slabs_compat.farmland_slab' -eq 'Farmland Slab') `
            'C73 Farmland Slab language entry is absent or incorrect'

    $predecessorShovel = (Get-EntryText $predecessorMap['data/minecraft/tags/block/mineable/shovel.json']) | ConvertFrom-Json
    $c73Shovel = (Get-EntryText $unifiedMap['data/minecraft/tags/block/mineable/shovel.json']) | ConvertFrom-Json
    $predecessorShovelValues = New-StringSet @($predecessorShovel.values)
    $c73ShovelValues = New-StringSet @($c73Shovel.values)
    Require ($c73Shovel.replace -eq $predecessorShovel.replace -and
            $c73ShovelValues.SetEquals($predecessorShovelValues)) `
            'C73 unexpectedly changed the Farmland Slab shovel tag'

    $forbiddenNames = @($unifiedMap.Keys | Where-Object {
        $_ -match '(?i)(^|/)uv bbmodel(?:\.zip|/|$)' -or
        $_ -match '(?i)(^|/)BlockSprite_glass\.png$' -or
        $_ -match '(?i)(^|/)glass_corner_north_east\.bbmodel$' -or
        $_ -match '(?i)\.bbmodel$'
    })
    Require ($forbiddenNames.Count -eq 0) `
            "Source archive/model/texture member leaked into production: $($forbiddenNames -join ', ')"

    $forbiddenDigests = New-StringSet @(
        '0be697e533f7de0dbb27eef37f13166d7da3f75a47d3a74839d4753b8b05c07',
        'be65b86e763dec985c900ebd23115fd5a07b78a0b882228589abdab1799d3743'
    )
    foreach ($name in $unifiedMap.Keys) {
        Require (-not $forbiddenDigests.Contains((Get-EntrySha256 $unifiedMap[$name]))) `
                "Supplied or embedded source texture bytes leaked as archive entry: $name"
    }

    $pngSignature = [byte[]](0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a)
    foreach ($name in @($changed) + @($newEntries)) {
        $bytes = Get-EntryBytes $unifiedMap[$name]
        Require (-not (Test-ContainsBytes $bytes $pngSignature)) `
                "Changed/new C73 entry embeds raw PNG source bytes: $name"
        $text = [System.Text.Encoding]::UTF8.GetString($bytes)
        Require ($text -notmatch '(?i)data:image/png;base64|iVBORw0KGgo|uv bbmodel(?:\.zip)?|BlockSprite_glass\.png|glass_corner_north_east\.bbmodel|\.bbmodel') `
                "Changed/new C73 entry embeds a forbidden source name or texture encoding: $name"
    }

    foreach ($path in @(
        'LICENSE',
        'META-INF/NOTICE',
        'META-INF/licenses/more-slabs-stairs-and-walls-LGPL-3.0-or-later.txt'
    )) {
        Require $unifiedMap.ContainsKey($path) "Required unified license/provenance entry is missing: $path"
        Require ((Get-EntrySha256 $unifiedMap[$path]) -eq (Get-EntrySha256 $acceptedMap[$path])) `
                "Retained unified license/provenance entry changed: $path"
    }

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
    Require ($blockstates.Count -eq 875 -and $slabs.Count -eq 279 -and $stairs.Count -eq 282 -and $walls.Count -eq 314) `
            'Native registry/resource inventory changed'
    Require ($families.Count -eq 314) 'Canonical family inventory changed'
    Require ($itemDefinitions.Count -eq 876 -and $itemModels.Count -eq 876 -and $blockModels.Count -eq 7067) `
            'Native client resource inventory changed'
    Require ($lootTables.Count -eq 875 -and $recipes.Count -eq 0) 'Native server resource inventory changed'

    $resourceEntries = @($unifiedMap.Keys | Where-Object {
        $_ -match '^(?:assets|data)/' -or $_ -match '(?:^|/)pack\.mcmeta$' -or $_ -match '\.mixins\.json$'
    })
    foreach ($name in $resourceEntries) {
        if (-not $acceptedMap.ContainsKey($name)) {
            Require (Test-AllowedNewEntry $name) "C73 added an unexpected packaged production resource: $name"
        } elseif (-not (Test-AllowedChangedEntry $name)) {
            Require ((Get-EntrySha256 $unifiedMap[$name]) -eq (Get-EntrySha256 $acceptedMap[$name])) `
                    "C73 changed a retained accepted-C58 production resource: $name"
        }
    }

    [ordered]@{
        result = 'PASS'
        c83 = [ordered]@{
            filename = [System.IO.Path]::GetFileName($unifiedPath)
            size = (Get-Item -LiteralPath $unifiedPath).Length
            sha256 = Get-FileSha256 $unifiedPath
            fabric_version = $metadata.version
            fabric_descriptors = $descriptors.Count
            nested_jars = $nestedJars.Count
            packaged_cnm_classes = $cnmClasses.Count
        }
        exact_accepted_c58_delta = [ordered]@{
            predecessor_sha256 = Get-FileSha256 $acceptedPath
            byte_identical_entries = $identical
            intentional_changed_entries = $changed.Count
            authored_new_entries = $newEntries.Count
            missing_entries = $missing.Count
        }
        exact_predecessor_c80_delta = [ordered]@{
            predecessor_sha256 = Get-FileSha256 $predecessorPath
            intentional_changed_entries = $predecessorChanged.Count
            authored_new_entries = $predecessorNew.Count
            missing_entries = $predecessorMissing.Count
        }
        source_input_exclusions = [ordered]@{
            forbidden_names = $forbiddenNames.Count
            forbidden_texture_digests = 0
            embedded_png_signatures = 0
            embedded_base64_or_source_names = 0
        }
        retained_inventory = [ordered]@{
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
            production_resource_entries = $resourceEntries.Count
        }
    } | ConvertTo-Json -Depth 10
} finally {
    $unified.Dispose()
    $accepted.Dispose()
    $predecessor.Dispose()
}
