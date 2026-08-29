[CmdletBinding()]
param(
    [string] $MatchaArchive,
    [string] $VanillaJar,
    [string] $Artifact
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

Add-Type -AssemblyName System.IO.Compression.FileSystem

$projectRoot = Split-Path -Parent $PSScriptRoot
$repoRoot = (Resolve-Path (Join-Path $projectRoot '..\..')).Path

if ([string]::IsNullOrWhiteSpace($MatchaArchive)) {
    $MatchaArchive = Join-Path $repoRoot 'originals\datapacks\Matcha_Flavoured_1_12.zip'
}

if ([string]::IsNullOrWhiteSpace($VanillaJar)) {
    $VanillaJar = Join-Path $env:USERPROFILE '.gradle\caches\fabric-loom\26.2\minecraft-client.jar'
}

$expectedMatchaHash = '6209783021C358044ABEDABACEE471FAFF5BD4080437D4E3B5E51963F1804248'
$expectedVanillaHash = '40896EE9F1E2BEC3C934DAAC7E93D41E9E3D9C2F8AE0CA366D52FFBFD1AFA290'

$styleContracts = [ordered]@{
    'village_plains.json' = [ordered]@{
        Biomes = @('minecraft:plains', 'minecraft:sunflower_plains')
        StartPool = 'minecraft:village/plains/town_centers'
    }
    'village_desert.json' = [ordered]@{
        Biomes = @('minecraft:desert')
        StartPool = 'minecraft:village/desert/town_centers'
    }
    'village_savanna.json' = [ordered]@{
        Biomes = @('minecraft:savanna')
        StartPool = 'minecraft:village/savanna/town_centers'
    }
    'village_snowy.json' = [ordered]@{
        Biomes = @('minecraft:snowy_plains')
        StartPool = 'minecraft:village/snowy/town_centers'
    }
    'village_taiga.json' = [ordered]@{
        Biomes = @(
            'minecraft:taiga',
            'minecraft:old_growth_pine_taiga',
            'minecraft:old_growth_spruce_taiga'
        )
        StartPool = 'minecraft:village/taiga/town_centers'
    }
}

$expectedPayload = @('pack.mcmeta') + @(
    $styleContracts.Keys | ForEach-Object {
        "data/minecraft/worldgen/structure/$_"
    }
)
$expectedPayload = @($expectedPayload | Sort-Object)

function Assert-True {
    param(
        [bool] $Condition,
        [string] $Message
    )

    if (-not $Condition) {
        throw "Verification failed: $Message"
    }
}

function Assert-SequenceEqual {
    param(
        [object[]] $Actual,
        [object[]] $Expected,
        [string] $Message
    )

    $actualStrings = @($Actual | ForEach-Object { [string] $_ })
    $expectedStrings = @($Expected | ForEach-Object { [string] $_ })
    $equal = $actualStrings.Count -eq $expectedStrings.Count

    if ($equal) {
        for ($index = 0; $index -lt $actualStrings.Count; $index++) {
            if ($actualStrings[$index] -cne $expectedStrings[$index]) {
                $equal = $false
                break
            }
        }
    }

    if (-not $equal) {
        throw "Verification failed: $Message`nExpected: $($expectedStrings -join ', ')`nActual: $($actualStrings -join ', ')"
    }
}

function Get-ZipText {
    param(
        $Archive,
        [string] $EntryName
    )

    $entry = $Archive.GetEntry($EntryName)
    Assert-True ($null -ne $entry) "missing ZIP entry $EntryName"
    $reader = [System.IO.StreamReader]::new($entry.Open(), [System.Text.Encoding]::UTF8, $true)
    try {
        return $reader.ReadToEnd()
    }
    finally {
        $reader.Dispose()
    }
}

function Get-ZipBytes {
    param(
        $Archive,
        [string] $EntryName
    )

    $entry = $Archive.GetEntry($EntryName)
    Assert-True ($null -ne $entry) "missing ZIP entry $EntryName"
    $inputStream = $entry.Open()
    $memoryStream = [System.IO.MemoryStream]::new()
    try {
        $inputStream.CopyTo($memoryStream)
        return ,$memoryStream.ToArray()
    }
    finally {
        $memoryStream.Dispose()
        $inputStream.Dispose()
    }
}

function Test-ByteArrayEqual {
    param(
        [byte[]] $First,
        [byte[]] $Second
    )

    if ($First.Length -ne $Second.Length) {
        return $false
    }

    for ($index = 0; $index -lt $First.Length; $index++) {
        if ($First[$index] -ne $Second[$index]) {
            return $false
        }
    }

    return $true
}

function ConvertTo-SemanticJson {
    param($Value)
    return ($Value | ConvertTo-Json -Depth 100 -Compress)
}

$matchaPath = (Resolve-Path -LiteralPath $MatchaArchive).Path
$vanillaPath = (Resolve-Path -LiteralPath $VanillaJar).Path

$matchaHash = (Get-FileHash -LiteralPath $matchaPath -Algorithm SHA256).Hash
$vanillaHash = (Get-FileHash -LiteralPath $vanillaPath -Algorithm SHA256).Hash
Assert-True ($matchaHash -ceq $expectedMatchaHash) 'Matcha archive SHA-256 does not match the pinned 1.12 input'
Assert-True ($vanillaHash -ceq $expectedVanillaHash) 'vanilla client JAR SHA-256 does not match the pinned Minecraft 26.2 reference'

$payload = @('pack.mcmeta')
$payload += Get-ChildItem -LiteralPath (Join-Path $projectRoot 'data') -Recurse -File | ForEach-Object {
    [System.IO.Path]::GetRelativePath($projectRoot, $_.FullName).Replace('\', '/')
}
$payload = @($payload | Sort-Object)
Assert-SequenceEqual $payload $expectedPayload 'deployable source inventory must be pack.mcmeta plus exactly five village structure JSONs'

foreach ($relativePath in $expectedPayload) {
    $sourcePath = Join-Path $projectRoot $relativePath.Replace('/', '\')
    Assert-True (Test-Path -LiteralPath $sourcePath -PathType Leaf) "missing source payload $relativePath"
    try {
        $null = Get-Content -LiteralPath $sourcePath -Raw | ConvertFrom-Json
    }
    catch {
        throw "Verification failed: invalid JSON in $relativePath - $($_.Exception.Message)"
    }
}

$packMeta = Get-Content -LiteralPath (Join-Path $projectRoot 'pack.mcmeta') -Raw | ConvertFrom-Json
Assert-SequenceEqual @($packMeta.PSObject.Properties.Name) @('pack') 'pack.mcmeta must contain only the pack object'
Assert-SequenceEqual @($packMeta.pack.PSObject.Properties.Name | Sort-Object) @('description', 'max_format', 'min_format') 'pack.mcmeta pack object must contain only description, min_format, and max_format'
Assert-SequenceEqual @($packMeta.pack.min_format) @(107, 1) 'pack.mcmeta min_format must be the exact [107, 1] tuple'
Assert-SequenceEqual @($packMeta.pack.max_format) @(107, 1) 'pack.mcmeta max_format must be the exact [107, 1] tuple'
Assert-True (-not [string]::IsNullOrWhiteSpace([string] $packMeta.pack.description)) 'pack.mcmeta description must be non-empty'

$matchaZip = [System.IO.Compression.ZipFile]::OpenRead($matchaPath)
$vanillaZip = [System.IO.Compression.ZipFile]::OpenRead($vanillaPath)
try {
    foreach ($fileName in $styleContracts.Keys) {
        $entryName = "data/minecraft/worldgen/structure/$fileName"
        $source = Get-Content -LiteralPath (Join-Path $projectRoot $entryName.Replace('/', '\')) -Raw | ConvertFrom-Json
        $matcha = Get-ZipText $matchaZip $entryName | ConvertFrom-Json
        $vanilla = Get-ZipText $vanillaZip $entryName | ConvertFrom-Json
        $contract = $styleContracts[$fileName]

        Assert-SequenceEqual @($matcha.biomes) @($contract.Biomes) "$fileName pinned Matcha biome contract changed"
        Assert-SequenceEqual @($source.biomes) @($matcha.biomes) "$fileName must preserve Matcha's exact biome array and order"
        Assert-True ([string] $source.start_pool -ceq [string] $contract.StartPool) "$fileName has the wrong style-specific start pool"

        $sourceProperties = @($source.PSObject.Properties.Name | Sort-Object)
        $vanillaProperties = @($vanilla.PSObject.Properties.Name | Sort-Object)
        Assert-SequenceEqual $sourceProperties $vanillaProperties "$fileName property set must match pristine vanilla 26.2"

        foreach ($property in $vanillaProperties) {
            if ($property -ceq 'biomes') {
                continue
            }

            $sourceValue = ConvertTo-SemanticJson $source.$property
            $vanillaValue = ConvertTo-SemanticJson $vanilla.$property
            Assert-True ($sourceValue -ceq $vanillaValue) "$fileName field '$property' differs from pristine vanilla 26.2"
        }
    }

    $structureSet = Get-ZipText $matchaZip 'data/minecraft/worldgen/structure_set/villages.json' | ConvertFrom-Json
    Assert-True ([string] $structureSet.placement.type -ceq 'minecraft:random_spread') 'Matcha village placement type is not random_spread'
    Assert-True ([int] $structureSet.placement.spacing -eq 80) 'Matcha village spacing is not 80'
    Assert-True ([int] $structureSet.placement.separation -eq 50) 'Matcha village separation is not 50'
    Assert-True ([int] $structureSet.placement.salt -eq 10387312) 'Matcha village salt is not 10387312'

    $expectedStructures = @(
        'minecraft:village_plains',
        'minecraft:village_desert',
        'minecraft:village_savanna',
        'minecraft:village_snowy',
        'minecraft:village_taiga'
    )
    Assert-SequenceEqual @($structureSet.structures.structure) $expectedStructures 'Matcha village structure-set membership or order changed'
    Assert-True (@($structureSet.structures | Where-Object { [int] $_.weight -ne 1 }).Count -eq 0) 'Matcha village structure weights must all remain 1'
}
finally {
    $vanillaZip.Dispose()
    $matchaZip.Dispose()
}

Assert-True (-not (Test-Path -LiteralPath (Join-Path $projectRoot 'data\minecraft\worldgen\structure_set\villages.json'))) 'compatibility pack must not override minecraft:villages'

if (-not [string]::IsNullOrWhiteSpace($Artifact)) {
    $artifactPath = (Resolve-Path -LiteralPath $Artifact).Path
    $artifactZip = [System.IO.Compression.ZipFile]::OpenRead($artifactPath)
    try {
        $fileEntries = @($artifactZip.Entries | Where-Object { -not [string]::IsNullOrEmpty($_.Name) })
        foreach ($entry in $fileEntries) {
            $entryName = $entry.FullName
            Assert-True (-not $entryName.Contains('\')) "artifact entry uses a backslash path: $entryName"
            Assert-True (-not $entryName.StartsWith('/')) "artifact entry is absolute: $entryName"
            Assert-True ($entryName -notmatch '^[A-Za-z]:') "artifact entry has a drive root: $entryName"
            Assert-True ($entryName -notmatch '(^|/)\.\.?(/|$)') "artifact entry contains traversal syntax: $entryName"
        }

        $artifactPayload = @($fileEntries.FullName | Sort-Object)
        Assert-SequenceEqual $artifactPayload $expectedPayload 'artifact must contain exactly the six deployable files at ZIP root'

        $strictUtf8 = [System.Text.UTF8Encoding]::new($false, $true)
        foreach ($relativePath in $expectedPayload) {
            [byte[]] $artifactBytes = Get-ZipBytes $artifactZip $relativePath
            [byte[]] $sourceBytes = [System.IO.File]::ReadAllBytes((Join-Path $projectRoot $relativePath.Replace('/', '\')))
            Assert-True (Test-ByteArrayEqual $artifactBytes $sourceBytes) "artifact entry $relativePath is not byte-identical to source"
            try {
                $artifactText = $strictUtf8.GetString($artifactBytes)
                $null = $artifactText | ConvertFrom-Json
            }
            catch {
                throw "Verification failed: artifact entry $relativePath is not strict UTF-8 JSON - $($_.Exception.Message)"
            }
        }
    }
    finally {
        $artifactZip.Dispose()
    }

    $artifactInfo = Get-Item -LiteralPath $artifactPath
    $artifactHash = (Get-FileHash -LiteralPath $artifactPath -Algorithm SHA256).Hash
    Write-Host "Artifact: $($artifactInfo.Name)"
    Write-Host "Artifact size: $($artifactInfo.Length) bytes"
    Write-Host "Artifact SHA-256: $artifactHash"
}

Write-Host 'PASS: pack.mcmeta and every payload JSON parsed.'
Write-Host 'PASS: exactly five village structure overrides are present.'
Write-Host 'PASS: Matcha biome arrays and pristine vanilla 26.2 jigsaw fields match.'
Write-Host 'PASS: Matcha remains sole owner of the 80/50/10387312 village structure set.'
Write-Host 'PASS: no pools, processors, templates, loot, trade, function, or Java payload is included.'
Write-Host 'Classification: STATICALLY VERIFIED / RUNTIME UNTESTED / NOT DEPLOYED.'
