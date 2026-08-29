[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$expectedArtifacts = @(
    [pscustomobject]@{
        Filename = 'carried-container-auto-routing-0.3.6-external-quick-move-priority-fix-canary.jar'
        Version = '0.3.6-external-quick-move-priority-fix-canary'
        Size = 34528L
        Sha256 = '01913B0455D961D20174C601B0544C178ED47868113E6F1173E132290FE4CA02'
    },
    [pscustomobject]@{
        Filename = 'carried-container-auto-routing-0.3.5-storage-partial-priority-fix-canary.jar'
        Version = '0.3.5-storage-partial-priority-fix-canary'
        Size = 34489L
        Sha256 = '610F6C65C36468C9B98CE8AA7892F8A57003C59A28CB989FE3C80742BEC4EAA2'
    }
)

Add-Type -AssemblyName System.IO.Compression.FileSystem

foreach ($expected in $expectedArtifacts) {
    $artifactPath = Join-Path $projectRoot "artifacts\$($expected.Filename)"
    if (-not (Test-Path -LiteralPath $artifactPath -PathType Leaf)) {
        throw "Retained artifact is missing: $artifactPath"
    }

    $artifact = Get-Item -LiteralPath $artifactPath
    if ($artifact.Length -ne $expected.Size) {
        throw "Retained artifact size mismatch for $($expected.Filename): expected $($expected.Size), found $($artifact.Length)."
    }

    $actualHash = (Get-FileHash -LiteralPath $artifactPath -Algorithm SHA256).Hash
    if ($actualHash -cne $expected.Sha256) {
        throw "Retained artifact SHA-256 mismatch for $($expected.Filename): expected $($expected.Sha256), found $actualHash."
    }

    $archive = [IO.Compression.ZipFile]::OpenRead($artifactPath)
    try {
        $metadataEntry = $archive.GetEntry('fabric.mod.json')
        if (-not $metadataEntry) {
            throw "Retained artifact lacks fabric.mod.json: $($expected.Filename)"
        }

        $reader = [IO.StreamReader]::new($metadataEntry.Open(), [Text.Encoding]::UTF8)
        try {
            $metadata = $reader.ReadToEnd() | ConvertFrom-Json
        }
        finally {
            $reader.Dispose()
        }

        if ($metadata.id -cne 'carried_container_auto_routing' -or
            $metadata.version -cne $expected.Version) {
            throw "Retained artifact embedded identity mismatch for $($expected.Filename): $($metadata.id) $($metadata.version)"
        }

        foreach ($requiredEntry in @(
            'dev/resivore/carriedrouting/CarriedContainerAutoRouting.class',
            'dev/resivore/carriedrouting/RoutingService.class',
            'dev/resivore/carriedrouting/api/OffhandCompatibilityHook.class',
            'dev/resivore/carriedrouting/mixin/ItemEntityMixin.class',
            'carried_container_auto_routing.mixins.json'
        )) {
            if (-not $archive.GetEntry($requiredEntry)) {
                throw "Retained artifact entry is missing from $($expected.Filename): $requiredEntry"
            }
        }

        $nestedJars = @($archive.Entries | Where-Object {
            $_.FullName.EndsWith('.jar', [StringComparison]::OrdinalIgnoreCase)
        })
        if ($nestedJars.Count -ne 0) {
            throw "Retained artifact contains nested JARs: $($expected.Filename)"
        }

        $bundledOffhandClasses = @($archive.Entries | Where-Object {
            $_.FullName.StartsWith('dev/resivore/offhand', [StringComparison]::Ordinal)
        })
        if ($bundledOffhandClasses.Count -ne 0) {
            throw "Retained artifact bundles Offhand classes: $($expected.Filename)"
        }
    }
    finally {
        $archive.Dispose()
    }

    Write-Output "RETAINED_ARTIFACT_OK: $($expected.Filename) $($expected.Size) bytes $actualHash"
}
