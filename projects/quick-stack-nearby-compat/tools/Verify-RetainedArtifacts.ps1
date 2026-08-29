[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$expectedArtifacts = @(
    [pscustomobject]@{
        Filename = 'quick-stack-nearby-compat-0.1.0-canary6.jar'
        Version = '0.1.0-canary6'
        Size = 21805L
        Sha256 = 'C2F4AE3B02A5517AD184998C784C356D90132AEAEE91546C91D03A878BE6CE98'
        ClassCount = 14
    },
    [pscustomobject]@{
        Filename = 'quick-stack-nearby-compat-0.1.0-canary4.jar'
        Version = '0.1.0-canary4'
        Size = 17181L
        Sha256 = '93581DE741DAA22426A22E1C816B630B9E0FFB90224A2CD27260563530AFB441'
        ClassCount = 10
    }
)

$sharedClasses = @(
    'dev/resivore/quickstacknearbycompat/core/CnmShapeMapApi.class',
    'dev/resivore/quickstacknearbycompat/core/CnmShapeMapResolver.class',
    'dev/resivore/quickstacknearbycompat/core/PlayerStorageSlots$Window.class',
    'dev/resivore/quickstacknearbycompat/core/PlayerStorageSlots.class',
    'dev/resivore/quickstacknearbycompat/core/QsnDestinationExclusions.class',
    'dev/resivore/quickstacknearbycompat/core/ShapeMapTargetAffinity$SourceKey.class',
    'dev/resivore/quickstacknearbycompat/core/ShapeMapTargetAffinity.class',
    'dev/resivore/quickstacknearbycompat/mixin/QuickStackServiceMixin.class',
    'dev/resivore/quickstacknearbycompat/mixin/client/QuickStackRuleStoreMixin.class',
    'dev/resivore/quickstacknearbycompat/mixin/client/QuickStackRulesScreenMixin.class'
)

$c6OnlyClasses = @(
    'dev/resivore/quickstacknearbycompat/core/InventorySearchContainerClassification.class',
    'dev/resivore/quickstacknearbycompat/core/QsnInventorySearchButtonPlacement.class',
    'dev/resivore/quickstacknearbycompat/mixin/client/InventoryScreenButtonSlotsMixin.class',
    'dev/resivore/quickstacknearbycompat/mixin/client/QuickStackButtonSlotBridgeMixin.class'
)

function Get-EntrySha256 {
    param(
        [Parameter(Mandatory)] [IO.Compression.ZipArchive] $Archive,
        [Parameter(Mandatory)] [string] $EntryName
    )

    $entry = $Archive.GetEntry($EntryName)
    if (-not $entry) {
        throw "Archive entry is missing: $EntryName"
    }

    $stream = $entry.Open()
    $sha = [Security.Cryptography.SHA256]::Create()
    try {
        ([BitConverter]::ToString($sha.ComputeHash($stream))).Replace('-', '')
    }
    finally {
        $sha.Dispose()
        $stream.Dispose()
    }
}

Add-Type -AssemblyName System.IO.Compression.FileSystem

$archives = @{}
try {
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
        $archives[$expected.Version] = $archive

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

        if ($metadata.id -cne 'quick_stack_nearby_compat' -or
            $metadata.version -cne $expected.Version -or
            $metadata.depends.'quick-stack-nearby' -cne '=0.4.0') {
            throw "Retained artifact embedded identity/dependency mismatch for $($expected.Filename)."
        }

        foreach ($requiredEntry in @(
            'quick_stack_nearby_compat.mixins.json',
            'quick_stack_nearby_compat.client.mixins.json'
        )) {
            if (-not $archive.GetEntry($requiredEntry)) {
                throw "Retained artifact entry is missing from $($expected.Filename): $requiredEntry"
            }
        }

        $classes = @($archive.Entries | Where-Object { $_.FullName.EndsWith('.class', [StringComparison]::Ordinal) })
        if ($classes.Count -ne $expected.ClassCount) {
            throw "Retained artifact class count mismatch for $($expected.Filename): expected $($expected.ClassCount), found $($classes.Count)."
        }

        $foreignClasses = @($classes | Where-Object {
            -not $_.FullName.StartsWith('dev/resivore/quickstacknearbycompat/', [StringComparison]::Ordinal)
        })
        if ($foreignClasses.Count -ne 0) {
            throw "Retained artifact bundles foreign classes: $($expected.Filename)"
        }

        $nestedJars = @($archive.Entries | Where-Object {
            $_.FullName.EndsWith('.jar', [StringComparison]::OrdinalIgnoreCase)
        })
        if ($nestedJars.Count -ne 0) {
            throw "Retained artifact contains nested JARs: $($expected.Filename)"
        }

        Write-Output "RETAINED_ARTIFACT_OK: $($expected.Filename) $($expected.Size) bytes $actualHash"
    }

    $c4 = $archives['0.1.0-canary4']
    $c6 = $archives['0.1.0-canary6']
    foreach ($entryName in $sharedClasses + @('quick_stack_nearby_compat.mixins.json')) {
        $c4Hash = Get-EntrySha256 -Archive $c4 -EntryName $entryName
        $c6Hash = Get-EntrySha256 -Archive $c6 -EntryName $entryName
        if ($c4Hash -cne $c6Hash) {
            throw "C4/C6 retained regression entry differs: $entryName"
        }
    }

    foreach ($entryName in $c6OnlyClasses) {
        if (-not $c6.GetEntry($entryName) -or $c4.GetEntry($entryName)) {
            throw "C6-only class boundary mismatch: $entryName"
        }
    }

    Write-Output 'RETAINED_REGRESSION_OK: 10 C4 classes and the common mixin configuration are byte-identical in C6; C6 adds exactly 4 Inventory Search interoperability classes.'
}
finally {
    foreach ($archive in $archives.Values) {
        $archive.Dispose()
    }
}
