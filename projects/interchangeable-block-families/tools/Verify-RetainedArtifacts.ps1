$ErrorActionPreference = 'Stop'

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$artifactRoot = Join-Path $projectRoot 'artifacts'
$expectedArtifacts = @(
    [ordered]@{
        Filename = 'interchangeable-block-families-0.1.0-canary4.jar'
        Size = 51171L
        Hash = '8F805D0CC6CF65599C8FAB190BE2AA023006A9BD990BCDD2BF0C45F62F1A2968'
        Version = '0.1.0-canary4'
    },
    [ordered]@{
        Filename = 'interchangeable-block-families-0.1.0-canary3.jar'
        Size = 47456L
        Hash = 'D0150AEA777DD7837D0C9B4F5CF41CE45F510C60EF416FB7001D06A36A6B0E87'
        Version = '0.1.0-canary3'
    }
)

Add-Type -AssemblyName System.IO.Compression.FileSystem

foreach ($expected in $expectedArtifacts) {
    $artifactPath = Join-Path $artifactRoot $expected.Filename
    if (-not (Test-Path -LiteralPath $artifactPath -PathType Leaf)) {
        throw "Retained artifact is missing: $artifactPath"
    }

    $artifact = Get-Item -LiteralPath $artifactPath
    if ($artifact.Length -ne $expected.Size) {
        throw "Retained artifact size mismatch for $($expected.Filename): $($artifact.Length)"
    }

    $actualHash = (Get-FileHash -LiteralPath $artifactPath -Algorithm SHA256).Hash
    if ($actualHash -cne $expected.Hash) {
        throw "Retained artifact SHA-256 mismatch for $($expected.Filename): $actualHash"
    }

    $archive = [IO.Compression.ZipFile]::OpenRead($artifactPath)
    try {
        $metadataEntry = $archive.Entries |
            Where-Object FullName -eq 'fabric.mod.json' |
            Select-Object -First 1
        if (-not $metadataEntry) {
            throw "Retained artifact has no fabric.mod.json: $($expected.Filename)"
        }

        $reader = [IO.StreamReader]::new($metadataEntry.Open(), [Text.Encoding]::UTF8)
        try {
            $metadata = $reader.ReadToEnd() | ConvertFrom-Json
        }
        finally {
            $reader.Dispose()
        }

        $forbidden = @($archive.Entries | Where-Object {
            $_.FullName -like 'dev/resivore/blockfamilies/gametest/*' -or
            $_.FullName -like 'com/mcwpaths/*' -or
            $_.FullName -like 'com/mcwdoors/*' -or
            $_.FullName -like 'com/mcwtrpdoors/*' -or
            $_.FullName -like 'com/mcwwindows/*' -or
            $_.FullName -like 'dev/tazer/clutternomore/*'
        })
        if ($forbidden.Count -ne 0) {
            throw "Retained artifact bundles forbidden GameTest/provider content: $($forbidden.FullName -join ', ')"
        }
    }
    finally {
        $archive.Dispose()
    }

    if (([string]$metadata.id) -cne 'interchangeable_block_families') {
        throw "Retained artifact mod ID mismatch for $($expected.Filename): $($metadata.id)"
    }
    if (([string]$metadata.version) -cne $expected.Version) {
        throw "Retained artifact version mismatch for $($expected.Filename): $($metadata.version)"
    }

    Write-Output "RETAINED_ARTIFACT_OK: $($expected.Filename) $($artifact.Length) $actualHash"
}
