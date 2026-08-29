[CmdletBinding()]
param(
    [Parameter()]
    [string]$OriginalJar
)

$ErrorActionPreference = 'Stop'

$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$expectedJarName = 'naturalist-2.0.3-fabric-1.21.1.jar'
$expectedJarSha256 = '3d16c975326e0df24486d44d8010d9e614fc9efdf891864de7b5a0efedfc12f9'

function Assert-Equal {
    param(
        [Parameter(Mandatory)]$Actual,
        [Parameter(Mandatory)]$Expected,
        [Parameter(Mandatory)][string]$Label
    )

    if ($Actual -ne $Expected) {
        throw "$Label mismatch: expected '$Expected', found '$Actual'."
    }
    Write-Host "PASS  $Label = $Expected"
}

function Assert-Empty {
    param(
        [Parameter()][object[]]$Values,
        [Parameter(Mandatory)][string]$Label
    )

    $valuesArray = @($Values | Where-Object { $null -ne $_ -and "$_" -ne '' })
    if ($valuesArray.Count -ne 0) {
        throw "$Label must be empty, but found: $($valuesArray -join ', ')"
    }
    Write-Host "PASS  $Label is empty"
}

function Assert-SetEqual {
    param(
        [Parameter()][string[]]$Actual,
        [Parameter()][string[]]$Expected,
        [Parameter(Mandatory)][string]$Label
    )

    $actualSet = @($Actual | Sort-Object -Unique)
    $expectedSet = @($Expected | Sort-Object -Unique)
    $missing = @($expectedSet | Where-Object { $_ -notin $actualSet })
    $unexpected = @($actualSet | Where-Object { $_ -notin $expectedSet })
    if ($missing.Count -ne 0 -or $unexpected.Count -ne 0) {
        throw "$Label mismatch. Missing: [$($missing -join ', ')]. Unexpected: [$($unexpected -join ', ')]."
    }
    Write-Host "PASS  $Label has the expected $($expectedSet.Count) entries"
}

if ([string]::IsNullOrWhiteSpace($OriginalJar)) {
    $candidatePaths = @(
        (Join-Path $projectRoot "..\..\originals\mods\$expectedJarName"),
        (Join-Path $projectRoot "..\..\..\..\..\originals\mods\$expectedJarName")
    )
    $OriginalJar = $candidatePaths |
        ForEach-Object { [IO.Path]::GetFullPath($_) } |
        Where-Object { Test-Path -LiteralPath $_ -PathType Leaf } |
        Select-Object -First 1
}

if ([string]::IsNullOrWhiteSpace($OriginalJar) -or -not (Test-Path -LiteralPath $OriginalJar -PathType Leaf)) {
    throw "Original JAR not found. Pass -OriginalJar with the path to $expectedJarName."
}
$OriginalJar = (Resolve-Path -LiteralPath $OriginalJar).Path

$actualHash = (Get-FileHash -LiteralPath $OriginalJar -Algorithm SHA256).Hash.ToLowerInvariant()
Assert-Equal $actualHash $expectedJarSha256 'original JAR SHA-256'

$properties = @{}
foreach ($line in Get-Content -LiteralPath (Join-Path $projectRoot 'gradle.properties')) {
    if ($line -match '^\s*([^#!][^=]*)=(.*)$') {
        $properties[$matches[1].Trim()] = $matches[2].Trim()
    }
}
Assert-Equal $properties.minecraft_version '26.2' 'Gradle Minecraft target'
Assert-Equal $properties.fabric_api_version '0.157.0+26.2' 'Workbench Fabric API baseline'
Assert-Equal $properties.lambdynamiclights_version '4.12.2+26.2' 'Workbench LambDynamicLights API baseline'
Assert-Equal $properties.upstream_original_filename $expectedJarName 'declared original filename'
Assert-Equal $properties.upstream_original_sha256 $expectedJarSha256 'declared original SHA-256'

$metadataPath = Join-Path $projectRoot 'src\main\resources\fabric.mod.json'
$metadata = Get-Content -LiteralPath $metadataPath -Raw | ConvertFrom-Json
Assert-Equal $metadata.id 'naturalist' 'Fabric mod id'
Assert-Equal $metadata.depends.minecraft '~${minecraft_version}' 'Fabric metadata Minecraft constraint'
Assert-Equal $metadata.depends.java '>=25' 'Fabric metadata Java constraint'
Assert-SetEqual @($metadata.depends.PSObject.Properties.Name) @('fabricloader', 'fabric-api', 'minecraft', 'java') 'Fabric required dependency ids'
Assert-SetEqual @($metadata.suggests.PSObject.Properties.Name) @('lambdynlights') 'Fabric suggested dependency ids'

$expectedEntities = @(
    'alligator', 'anglerfish', 'ant', 'bass', 'bear', 'bird', 'black_bear', 'blobfish',
    'boar', 'butterfly', 'capybara', 'carried_food', 'caterpillar', 'catfish', 'clam',
    'crab', 'deer', 'desert_scorpion', 'dirt_trail', 'dragonfly', 'duck', 'duck_egg',
    'elephant', 'firefly', 'giant_isopod', 'giraffe', 'great_white_shark', 'hedgehog',
    'hippo', 'jellyfish', 'jungle_scorpion', 'komodo_dragon', 'lion', 'lizard',
    'lizard_tail', 'mammoth', 'mole', 'ostrich', 'piranha', 'rat', 'ray', 'rhino',
    'snail', 'snake', 'starfish', 'tiger', 'tortoise', 'turkey', 'vulture', 'whale', 'zebra'
)
$utilityEntities = @('carried_food', 'dirt_trail', 'duck_egg')

$entityTypesPath = Join-Path $projectRoot 'common\src\main\java\com\crispytwig\naturalist\registry\NaturalistEntityTypes.java'
$entityTypesSource = Get-Content -LiteralPath $entityTypesPath -Raw
$sourceEntities = @(
    [regex]::Matches($entityTypesSource, '\bregister\("([a-z0-9_]+)"') |
        ForEach-Object { $_.Groups[1].Value }
)
Assert-Equal $sourceEntities.Count 51 'source entity registration count'
Assert-SetEqual $sourceEntities $expectedEntities 'source entity registration ids'

Add-Type -AssemblyName System.IO.Compression.FileSystem -ErrorAction SilentlyContinue
$zip = [IO.Compression.ZipFile]::OpenRead($OriginalJar)
try {
    $variantEntries = @($zip.Entries | Where-Object {
        $_.FullName -match '^data/naturalist/naturalist/([a-z0-9_]+)_variant/[^/]+\.json$'
    })
    $variantRegistries = @($variantEntries | ForEach-Object {
        if ($_.FullName -match '^data/naturalist/naturalist/([a-z0-9_]+)_variant/') {
            $matches[1]
        }
    } | Sort-Object -Unique)
    $expectedVariantRegistries = @($expectedEntities | Where-Object { $_ -notin $utilityEntities })

    Assert-Equal $variantEntries.Count 99 'upstream variant JSON count'
    Assert-Equal $variantRegistries.Count 48 'upstream variant registry count'
    Assert-SetEqual $variantRegistries $expectedVariantRegistries 'upstream variant registry ids'

    $lootEntries = @($zip.Entries | Where-Object {
        $_.FullName -match '^data/naturalist/loot_table/entities/([a-z0-9_]+)\.json$'
    })
    $lootEntities = @($lootEntries | ForEach-Object {
        if ($_.FullName -match '^data/naturalist/loot_table/entities/([a-z0-9_]+)\.json$') {
            $matches[1]
        }
    })
    $expectedLootEntities = @($expectedVariantRegistries | Where-Object { $_ -ne 'starfish' })

    Assert-Equal $lootEntries.Count 47 'upstream entity loot JSON count'
    Assert-SetEqual $lootEntities $expectedLootEntities 'upstream entity loot table ids'
}
finally {
    $zip.Dispose()
}

$authoredResourceRoot = Join-Path $projectRoot 'src\main\resources'
$authoredResourceFiles = @(
    Get-ChildItem -LiteralPath $authoredResourceRoot -File -Recurse |
        ForEach-Object { $_.FullName.Substring($authoredResourceRoot.Length + 1).Replace('\', '/') }
)
Assert-SetEqual $authoredResourceFiles @(
    'data/minecraft/tags/entity_type/can_equip_saddle.json'
    'fabric.mod.json'
) 'authored resource files'

$localProtectedFiles = @()
foreach ($relativePath in @('common\src\main\resources', 'fabric\src\main\resources')) {
    $fullPath = Join-Path $projectRoot $relativePath
    if (Test-Path -LiteralPath $fullPath -PathType Container) {
        $localProtectedFiles += Get-ChildItem -LiteralPath $fullPath -File -Recurse |
            ForEach-Object { $_.FullName.Substring($projectRoot.Length + 1).Replace('\', '/') }
    }
}
Assert-Empty $localProtectedFiles 'local upstream common/fabric resource trees'

$repoRootOutput = @(& git -C $projectRoot rev-parse --show-toplevel 2>&1)
if ($LASTEXITCODE -ne 0) {
    throw "Unable to locate Git worktree: $($repoRootOutput -join [Environment]::NewLine)"
}
$repoRoot = [IO.Path]::GetFullPath($repoRootOutput[-1])
$projectPrefixOutput = @(& git -C $projectRoot rev-parse --show-prefix 2>&1)
if ($LASTEXITCODE -ne 0) {
    throw "Unable to locate project prefix: $($projectPrefixOutput -join [Environment]::NewLine)"
}
$projectPrefix = $projectPrefixOutput[-1].TrimEnd('/')
$trackedProjectFiles = @(& git -C $repoRoot ls-files -- $projectPrefix 2>&1)
if ($LASTEXITCODE -ne 0) {
    throw "Unable to inspect tracked files: $($trackedProjectFiles -join [Environment]::NewLine)"
}

$trackedProtectedFiles = @($trackedProjectFiles | Where-Object {
    $_ -match '/(?:common|fabric)/src/main/resources/' -or
    $_ -match '/build/generated/original-resources/' -or
    $_ -match '/(?:assets|data)/naturalist/' -or
    $_ -match '/naturalist(?:\.fieldguide)?\.mixins\.json$' -or
    $_ -match '/naturalist\.accesswidener$'
})
Assert-Empty $trackedProtectedFiles 'tracked protected upstream resource corpus'

Write-Host ''
Write-Host 'Naturalist 26.2 preservation baseline verified.'
Write-Host "Original: $OriginalJar"
Write-Host 'Counts: 51 entity types; 48 variant registries; 99 variant JSONs; 47 entity loot JSONs.'
