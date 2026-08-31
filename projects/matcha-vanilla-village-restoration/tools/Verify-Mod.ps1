[CmdletBinding()]
param(
    [string] $Artifact,
    [string] $VanillaJar
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

Add-Type -AssemblyName System.IO.Compression.FileSystem

$projectRoot = Split-Path -Parent $PSScriptRoot
$repoRoot = (Resolve-Path (Join-Path $projectRoot '..\..')).Path

if ([string]::IsNullOrWhiteSpace($Artifact)) {
    $Artifact = Join-Path $projectRoot 'build\libs\matcha-vanilla-village-restoration-0.2.0-canary2.jar'
}
if ([string]::IsNullOrWhiteSpace($VanillaJar)) {
    $VanillaJar = Join-Path $env:USERPROFILE '.gradle\caches\fabric-loom\26.2\minecraft-client.jar'
}

$c1Path = Join-Path $projectRoot 'artifacts\matcha-vanilla-village-restoration-0.1.0-canary1.zip'
$expectedC1Hash = '2B0D29EE6389C457B801091AA7469E6C53468944DB3EDE83760DC7D8FB49B8E4'
$expectedVanillaHash = '40896EE9F1E2BEC3C934DAAC7E93D41E9E3D9C2F8AE0CA366D52FFBFD1AFA290'
$expectedMatchaHash = '6209783021C358044ABEDABACEE471FAFF5BD4080437D4E3B5E51963F1804248'
$builtInPrefix = 'resourcepacks/vanilla_villages/'

$styleContracts = [ordered]@{
    'village_desert.json' = [ordered]@{
        Biomes = @('minecraft:desert')
        StartPool = 'minecraft:village/desert/town_centers'
    }
    'village_plains.json' = [ordered]@{
        Biomes = @('minecraft:plains', 'minecraft:sunflower_plains')
        StartPool = 'minecraft:village/plains/town_centers'
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

function Assert-True {
    param([bool] $Condition, [string] $Message)
    if (-not $Condition) {
        throw "Verification failed: $Message"
    }
}

function Assert-SequenceEqual {
    param([object[]] $Actual, [object[]] $Expected, [string] $Message)
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

function Get-ZipBytes {
    param($Archive, [string] $EntryName)
    $entry = $Archive.GetEntry($EntryName)
    Assert-True ($null -ne $entry) "missing ZIP/JAR entry $EntryName"
    $input = $entry.Open()
    $memory = [System.IO.MemoryStream]::new()
    try {
        $input.CopyTo($memory)
        return ,$memory.ToArray()
    }
    finally {
        $memory.Dispose()
        $input.Dispose()
    }
}

function Get-ZipText {
    param($Archive, [string] $EntryName)
    [byte[]] $bytes = Get-ZipBytes $Archive $EntryName
    return [System.Text.UTF8Encoding]::new($false, $true).GetString($bytes)
}

function Test-ByteArrayEqual {
    param([byte[]] $First, [byte[]] $Second)
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

$artifactPath = (Resolve-Path -LiteralPath $Artifact).Path
$vanillaPath = (Resolve-Path -LiteralPath $VanillaJar).Path
$resolvedC1Path = (Resolve-Path -LiteralPath $c1Path).Path

Assert-True ((Get-FileHash -LiteralPath $resolvedC1Path -Algorithm SHA256).Hash -ceq $expectedC1Hash) `
    'retained runtime-passed C1 ZIP identity changed'
Assert-True ((Get-FileHash -LiteralPath $vanillaPath -Algorithm SHA256).Hash -ceq $expectedVanillaHash) `
    'vanilla Minecraft 26.2 reference identity changed'

$expectedBuiltInPayload = @($builtInPrefix + 'pack.mcmeta') + @(
    $styleContracts.Keys | ForEach-Object {
        $builtInPrefix + 'data/minecraft/worldgen/structure/' + $_
    }
)
$expectedBuiltInPayload = @($expectedBuiltInPayload | Sort-Object)

$expectedArtifactFiles = @(
    'META-INF/MANIFEST.MF',
    'dev/resivore/matchavillagerestoration/MatchaVanillaVillageRestoration.class',
    'dev/resivore/matchavillagerestoration/VillagePackPrecedence.class',
    'dev/resivore/matchavillagerestoration/mixin/MultiPackResourceManagerMixin.class',
    'fabric.mod.json',
    'matcha_vanilla_village_restoration.mixins.json'
) + $expectedBuiltInPayload
$expectedArtifactFiles = @($expectedArtifactFiles | Sort-Object)

$artifactZip = [System.IO.Compression.ZipFile]::OpenRead($artifactPath)
$c1Zip = [System.IO.Compression.ZipFile]::OpenRead($resolvedC1Path)
$vanillaZip = [System.IO.Compression.ZipFile]::OpenRead($vanillaPath)
try {
    $artifactFiles = @(
        $artifactZip.Entries |
            Where-Object { -not [string]::IsNullOrEmpty($_.Name) } |
            ForEach-Object { $_.FullName } |
            Sort-Object
    )
    Assert-SequenceEqual $artifactFiles $expectedArtifactFiles `
        'C2 JAR must contain only three runtime classes, Fabric metadata, and the exact six-file built-in pack'

    $builtInFiles = @($artifactFiles | Where-Object { $_.StartsWith($builtInPrefix) })
    Assert-SequenceEqual $builtInFiles $expectedBuiltInPayload `
        'built-in server-data pack inventory changed'
    Assert-True (@($artifactFiles | Where-Object { $_ -match '(^|/)structure_set/' }).Count -eq 0) `
        'C2 JAR must not package any structure-set override'
    Assert-True (@($artifactFiles | Where-Object {
        $_ -match '(^|/)(worldgen/template_pool|worldgen/processor_list|structures|loot_table|function)/'
    }).Count -eq 0) 'C2 JAR contains an excluded data resource class'

    foreach ($fileName in $styleContracts.Keys) {
        $c1Entry = 'data/minecraft/worldgen/structure/' + $fileName
        $c2Entry = $builtInPrefix + $c1Entry
        [byte[]] $c1Bytes = Get-ZipBytes $c1Zip $c1Entry
        [byte[]] $c2Bytes = Get-ZipBytes $artifactZip $c2Entry
        Assert-True (Test-ByteArrayEqual $c1Bytes $c2Bytes) `
            "$fileName is not byte-identical to runtime-passed C1"

        $c2 = Get-ZipText $artifactZip $c2Entry | ConvertFrom-Json
        $vanilla = Get-ZipText $vanillaZip $c1Entry | ConvertFrom-Json
        $contract = $styleContracts[$fileName]
        Assert-SequenceEqual @($c2.biomes) @($contract.Biomes) `
            "$fileName no longer has the exact Matcha biome array"
        Assert-True ([string] $c2.start_pool -ceq [string] $contract.StartPool) `
            "$fileName has the wrong vanilla style-specific start pool"

        $c2Properties = @($c2.PSObject.Properties.Name | Sort-Object)
        $vanillaProperties = @($vanilla.PSObject.Properties.Name | Sort-Object)
        Assert-SequenceEqual $c2Properties $vanillaProperties `
            "$fileName property set differs from pristine vanilla 26.2"
        foreach ($property in $vanillaProperties) {
            if ($property -ceq 'biomes') {
                continue
            }
            Assert-True ((ConvertTo-SemanticJson $c2.$property) -ceq (ConvertTo-SemanticJson $vanilla.$property)) `
                "$fileName field '$property' differs from pristine vanilla 26.2"
        }
    }

    [byte[]] $c1Meta = Get-ZipBytes $c1Zip 'pack.mcmeta'
    [byte[]] $c2Meta = Get-ZipBytes $artifactZip ($builtInPrefix + 'pack.mcmeta')
    Assert-True (Test-ByteArrayEqual $c1Meta $c2Meta) 'built-in pack metadata differs from C1'

    $fabricMetadata = Get-ZipText $artifactZip 'fabric.mod.json' | ConvertFrom-Json
    Assert-True ([string] $fabricMetadata.id -ceq 'matcha_vanilla_village_restoration') `
        'Fabric mod id changed'
    Assert-True ([string] $fabricMetadata.version -ceq '0.2.0-canary2') `
        'Fabric mod version changed'
    Assert-True ([string] $fabricMetadata.custom.'workbench:matcha_reference'.filename -ceq 'Matcha_Flavoured_1_12.zip') `
        'pinned Matcha filename changed'
    Assert-True ([string] $fabricMetadata.custom.'workbench:matcha_reference'.sha256 -ceq $expectedMatchaHash.ToLowerInvariant()) `
        'pinned Matcha SHA-256 changed'

    $mixinMetadata = Get-ZipText $artifactZip 'matcha_vanilla_village_restoration.mixins.json' | ConvertFrom-Json
    Assert-True ([bool] $mixinMetadata.required) 'precedence mixin must be required'
    Assert-SequenceEqual @($mixinMetadata.mixins) @('MultiPackResourceManagerMixin') `
        'precedence mixin inventory changed'
    Assert-True ([int] $mixinMetadata.injectors.defaultRequire -eq 1) `
        'precedence injection must fail closed'
}
finally {
    $vanillaZip.Dispose()
    $c1Zip.Dispose()
    $artifactZip.Dispose()
}

$artifactInfo = Get-Item -LiteralPath $artifactPath
$artifactHash = (Get-FileHash -LiteralPath $artifactPath -Algorithm SHA256).Hash
Write-Host "Artifact: $($artifactInfo.Name)"
Write-Host "Artifact size: $($artifactInfo.Length) bytes"
Write-Host "Artifact SHA-256: $artifactHash"
Write-Host "Pinned external Matcha 1.12 SHA-256: $expectedMatchaHash"
Write-Host 'PASS: C2 packages exactly five village definitions in one ALWAYS_ENABLED built-in pack.'
Write-Host 'PASS: every C2 village definition is byte-identical to runtime-passed C1.'
Write-Host 'PASS: all non-biome fields still match pristine Minecraft 26.2.'
Write-Host 'PASS: no structure-set or unrelated data/worldgen resource is packaged.'
Write-Host 'INFO: Gradle test is the executable proof that the required mixin promotes C2 above a later conflicting pack.'
Write-Host 'Classification: STATIC_PASS / NOT_DEPLOYED / RUNTIME_UNTESTED.'
