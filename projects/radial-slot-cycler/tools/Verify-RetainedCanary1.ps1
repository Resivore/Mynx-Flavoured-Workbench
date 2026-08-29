[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..\..')).Path
$projectRoot = Join-Path $repositoryRoot 'projects\radial-slot-cycler'
$artifactName = 'radial-slot-cycler-0.1.0-canary1.jar'
$artifactPath = Join-Path $projectRoot "artifacts\$artifactName"
$buildPath = Join-Path $projectRoot "build\libs\$artifactName"
$expectedSize = 28784L
$expectedHash = '7393321A6AC7FF1D16B918075C47A26BBC90005B3A784F4D2B661F93CCA4F2E4'

if (-not (Test-Path -LiteralPath $artifactPath -PathType Leaf)) {
    throw "Retained Canary 1 artifact is missing: $artifactPath"
}

$artifact = Get-Item -LiteralPath $artifactPath
if ($artifact.Length -ne $expectedSize) {
    throw "Retained Canary 1 size mismatch: expected $expectedSize, found $($artifact.Length)."
}

$artifactHash = (Get-FileHash -LiteralPath $artifactPath -Algorithm SHA256).Hash
if ($artifactHash -cne $expectedHash) {
    throw "Retained Canary 1 SHA-256 mismatch: expected $expectedHash, found $artifactHash."
}

if (Test-Path -LiteralPath $buildPath -PathType Leaf) {
    $buildHash = (Get-FileHash -LiteralPath $buildPath -Algorithm SHA256).Hash
    if ($buildHash -cne $artifactHash) {
        throw "Clean-build and retained Canary 1 hashes differ: $buildHash vs $artifactHash."
    }
}

Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive = [System.IO.Compression.ZipFile]::OpenRead($artifactPath)
try {
    $entryNames = @($archive.Entries | Select-Object -ExpandProperty FullName)
    $requiredEntries = @(
        'fabric.mod.json',
        'LICENSE_radial-slot-cycler',
        'assets/radial_slot_cycler/lang/en_us.json',
        'dev/resivore/radialslotcycler/RadialSlotCycler.class',
        'dev/resivore/radialslotcycler/client/RadialSlotCyclerClient.class',
        'dev/resivore/radialslotcycler/core/ColumnLayout.class',
        'dev/resivore/radialslotcycler/core/ExactPairwiseSwap.class',
        'dev/resivore/radialslotcycler/core/SwapRequestValidator.class',
        'dev/resivore/radialslotcycler/network/SwapSlotPayload.class'
    )
    foreach ($requiredEntry in $requiredEntries) {
        if ($entryNames -cnotcontains $requiredEntry) {
            throw "Retained Canary 1 is missing required entry: $requiredEntry"
        }
    }

    $nestedJars = @($entryNames | Where-Object { $_.EndsWith('.jar', [StringComparison]::OrdinalIgnoreCase) })
    if ($nestedJars.Count -ne 0) {
        throw "Unexpected nested JARs: $($nestedJars -join ', ')"
    }

    $foreignClasses = @($entryNames | Where-Object {
        $_.EndsWith('.class', [StringComparison]::OrdinalIgnoreCase) -and
        -not $_.StartsWith('dev/resivore/radialslotcycler/', [StringComparison]::Ordinal)
    })
    if ($foreignClasses.Count -ne 0) {
        throw "Unexpected foreign classes: $($foreignClasses -join ', ')"
    }

    $foreignAssets = @($entryNames | Where-Object {
        $_.StartsWith('assets/', [StringComparison]::Ordinal) -and
        -not $_.EndsWith('/', [StringComparison]::Ordinal) -and
        -not $_.StartsWith('assets/radial_slot_cycler/', [StringComparison]::Ordinal)
    })
    if ($foreignAssets.Count -ne 0) {
        throw "Unexpected foreign assets: $($foreignAssets -join ', ')"
    }

    $metadataEntry = $archive.GetEntry('fabric.mod.json')
    $reader = [System.IO.StreamReader]::new($metadataEntry.Open(), [Text.Encoding]::UTF8)
    try {
        $metadata = $reader.ReadToEnd() | ConvertFrom-Json
    }
    finally {
        $reader.Dispose()
    }

    if (([string]$metadata.id) -cne 'radial_slot_cycler' -or
            ([string]$metadata.version) -cne '0.1.0-canary1') {
        throw "Unexpected embedded identity: $($metadata.id) $($metadata.version)"
    }
    if (([string]$metadata.license) -cne 'MIT') {
        throw "Unexpected embedded license: $($metadata.license)"
    }
    if (([string]$metadata.custom.'workbench:classification') -cne
            'GENERATED / STATICALLY VALIDATED / RUNTIME UNTESTED' -or
            ([string]$metadata.custom.'workbench:deployment') -cne 'NOT DEPLOYED' -or
            ([string]$metadata.custom.'workbench:runtime-slot-owner') -cne
            "Treasure Chest X on Xaero's Map") {
        throw 'Embedded Canary 1 generation/deployment provenance is incorrect.'
    }
}
finally {
    $archive.Dispose()
}

Write-Output "RADIAL_SLOT_CYCLER_RETAINED_CANARY1_OK: $artifactName $expectedSize bytes $expectedHash"
