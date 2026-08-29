[CmdletBinding()]
param(
    [string]$WorkbenchRoot
)

$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$artifactPath = Join-Path $projectRoot 'artifacts\strippingtoggle-fallingtree-compat-0.1.0-canary1-authoritative-toggle.jar'
$expectedHash = 'B5960558C42F6A5597504640A19297670BA83EF7EF79834FA68B6358E4C1AD08'

if (-not (Test-Path -LiteralPath $artifactPath -PathType Leaf)) {
    throw "Retained Canary 1 is missing: $artifactPath"
}
if ((Get-Item -LiteralPath $artifactPath).Length -ne 13568) {
    throw 'Retained Canary 1 size mismatch.'
}
$hash = (Get-FileHash -LiteralPath $artifactPath -Algorithm SHA256).Hash
if ($hash -cne $expectedHash) { throw "Retained Canary 1 hash mismatch: $hash" }

Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive = [IO.Compression.ZipFile]::OpenRead($artifactPath)
try {
    $metadataEntry = $archive.GetEntry('fabric.mod.json')
    if (-not $metadataEntry) { throw 'Retained Canary 1 lacks fabric.mod.json.' }
    $reader = [IO.StreamReader]::new($metadataEntry.Open())
    try { $metadata = $reader.ReadToEnd() | ConvertFrom-Json } finally { $reader.Dispose() }
    if ($metadata.id -cne 'strippingtoggle_fallingtree_compat' -or
        $metadata.version -cne '0.1.0-canary1-authoritative-toggle' -or
        $metadata.environment -cne '*') {
        throw "Retained Canary 1 embedded identity mismatch: $($metadata.id) $($metadata.version) $($metadata.environment)"
    }
    if ($metadata.depends.fallingtree -cne '=25') {
        throw 'Retained Canary 1 FallingTree dependency is not exact.'
    }
    if ($metadata.depends.PSObject.Properties.Name -contains 'strippingtoggle') {
        throw 'Client-only StrippingToggle must not be a universal hard dependency.'
    }
    if ($metadata.suggests.strippingtoggle -cne '=1.2.6+26.2') {
        throw 'Retained Canary 1 StrippingToggle client relationship is not exact.'
    }

    foreach ($required in @(
        'dev/resivore/strippingtogglefallingtreecompat/StrippingToggleFallingTreeCompat.class',
        'dev/resivore/strippingtogglefallingtreecompat/ToggleAuthority.class',
        'dev/resivore/strippingtogglefallingtreecompat/ToggleStatePayload.class',
        'dev/resivore/strippingtogglefallingtreecompat/ToggleStateStore.class',
        'dev/resivore/strippingtogglefallingtreecompat/client/StrippingToggleFallingTreeCompatClient.class',
        'dev/resivore/strippingtogglefallingtreecompat/client/ToggleSyncTracker.class',
        'dev/resivore/strippingtogglefallingtreecompat/mixin/FallingTreeCommonMixin.class',
        'dev/resivore/strippingtogglefallingtreecompat/mixin/ToggleCommandMixin.class',
        'strippingtoggle_fallingtree_compat.mixins.json'
    )) {
        if (-not $archive.GetEntry($required)) { throw "Retained Canary 1 entry missing: $required" }
    }
    if (@($archive.Entries | Where-Object {
        $_.FullName.StartsWith('yungando/', [StringComparison]::Ordinal) -or
        $_.FullName.StartsWith('fr/rakambda/', [StringComparison]::Ordinal)
    }).Count -ne 0) {
        throw 'Retained Canary 1 improperly bundles upstream mod classes.'
    }

    $seamChecks = @(
        @('dev/resivore/strippingtogglefallingtreecompat/mixin/FallingTreeCommonMixin.class', @('playerHasToggledOff', 'isFallingTreeDisabled')),
        @('dev/resivore/strippingtogglefallingtreecompat/mixin/ToggleCommandMixin.class', @('reportMasterState', 'fallingtree-disabled', 'statusTranslationKey')),
        @('dev/resivore/strippingtogglefallingtreecompat/client/StrippingToggleFallingTreeCompatClient.class', @('strippingEnabled', 'canSend', 'getModContainer'))
    )
    foreach ($seamCheck in $seamChecks) {
        $entry = $archive.GetEntry([string]$seamCheck[0])
        $stream = $entry.Open()
        try {
            $memory = [IO.MemoryStream]::new()
            try {
                $stream.CopyTo($memory)
                $classText = [Text.Encoding]::GetEncoding(28591).GetString($memory.ToArray())
            } finally { $memory.Dispose() }
        } finally { $stream.Dispose() }
        foreach ($token in @($seamCheck[1])) {
            if (-not $classText.Contains([string]$token)) {
                throw "Retained Canary 1 seam token missing from $($seamCheck[0]): $token"
            }
        }
    }
} finally {
    $archive.Dispose()
}

& (Join-Path $PSScriptRoot 'Verify-UpstreamContract.ps1') -WorkbenchRoot $WorkbenchRoot
if (-not $?) { throw 'Exact upstream dependency verification failed.' }
Write-Output "RETAINED_STRIPPING_TOGGLE_FALLING_TREE_CANARY1_OK: $(Split-Path -Leaf $artifactPath) $hash"
