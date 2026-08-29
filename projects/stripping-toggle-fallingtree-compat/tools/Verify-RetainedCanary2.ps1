[CmdletBinding()]
param(
    [string]$WorkbenchRoot
)

$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$artifactPath = Join-Path $projectRoot 'artifacts\strippingtoggle-fallingtree-compat-0.1.1-canary2.jar'
$expectedHash = 'D948C23DB8B6C73E764F96422D8359AFB85BA39689352E512FD7B4AA91644846'

& (Join-Path $PSScriptRoot 'Verify-RetainedCanary1.ps1') -WorkbenchRoot $WorkbenchRoot
if (-not $?) { throw 'Retained Canary 1 predecessor verification failed.' }

if (-not (Test-Path -LiteralPath $artifactPath -PathType Leaf)) {
    throw "Retained Canary 2 is missing: $artifactPath"
}
if ((Get-Item -LiteralPath $artifactPath).Length -ne 11987) {
    throw 'Retained Canary 2 size mismatch.'
}
$hash = (Get-FileHash -LiteralPath $artifactPath -Algorithm SHA256).Hash
if ($hash -cne $expectedHash) { throw "Retained Canary 2 hash mismatch: $hash" }

Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive = [IO.Compression.ZipFile]::OpenRead($artifactPath)
try {
    if ($archive.Entries.Count -ne 15) {
        throw "Retained Canary 2 JAR entry-count mismatch: $($archive.Entries.Count)"
    }

    $metadataEntry = $archive.GetEntry('fabric.mod.json')
    if (-not $metadataEntry) { throw 'Retained Canary 2 lacks fabric.mod.json.' }
    $reader = [IO.StreamReader]::new($metadataEntry.Open())
    try { $metadata = $reader.ReadToEnd() | ConvertFrom-Json } finally { $reader.Dispose() }
    if ($metadata.id -cne 'strippingtoggle_fallingtree_compat' -or
        $metadata.version -cne '0.1.1-canary2' -or
        $metadata.environment -cne '*') {
        throw "Retained Canary 2 embedded identity mismatch: $($metadata.id) $($metadata.version) $($metadata.environment)"
    }
    if ($metadata.depends.fallingtree -cne '=25') {
        throw 'Retained Canary 2 FallingTree dependency is not exact.'
    }
    if ($metadata.depends.PSObject.Properties.Name -contains 'strippingtoggle') {
        throw 'Client-only StrippingToggle must not be a universal hard dependency.'
    }
    if ($metadata.suggests.strippingtoggle -cne '=1.2.6+26.2') {
        throw 'Retained Canary 2 StrippingToggle client relationship is not exact.'
    }

    foreach ($required in @(
        'dev/resivore/strippingtogglefallingtreecompat/StrippingToggleFallingTreeCompat.class',
        'dev/resivore/strippingtogglefallingtreecompat/ToggleAuthority.class',
        'dev/resivore/strippingtogglefallingtreecompat/ToggleStatePayload.class',
        'dev/resivore/strippingtogglefallingtreecompat/client/StrippingToggleFallingTreeCompatClient.class',
        'dev/resivore/strippingtogglefallingtreecompat/client/ToggleSyncTracker.class',
        'dev/resivore/strippingtogglefallingtreecompat/mixin/ToggleCommandMixin.class',
        'strippingtoggle_fallingtree_compat.mixins.json'
    )) {
        if (-not $archive.GetEntry($required)) { throw "Retained Canary 2 entry missing: $required" }
    }
    foreach ($removed in @(
        'dev/resivore/strippingtogglefallingtreecompat/ToggleStateStore.class',
        'dev/resivore/strippingtogglefallingtreecompat/client/ToggleSyncTracker$SyncDecision.class',
        'dev/resivore/strippingtogglefallingtreecompat/mixin/FallingTreeCommonMixin.class'
    )) {
        if ($archive.GetEntry($removed)) { throw "Retained Canary 2 still contains removed hot-path entry: $removed" }
    }
    if (@($archive.Entries | Where-Object {
        $_.FullName.StartsWith('yungando/', [StringComparison]::Ordinal) -or
        $_.FullName.StartsWith('fr/rakambda/', [StringComparison]::Ordinal)
    }).Count -ne 0) {
        throw 'Retained Canary 2 improperly bundles upstream mod classes.'
    }

    $mixinsEntry = $archive.GetEntry('strippingtoggle_fallingtree_compat.mixins.json')
    $reader = [IO.StreamReader]::new($mixinsEntry.Open())
    try { $mixins = $reader.ReadToEnd() | ConvertFrom-Json } finally { $reader.Dispose() }
    if (-not $mixins.required -or $mixins.injectors.defaultRequire -ne 1 -or
        @($mixins.mixins).Count -ne 1 -or $mixins.mixins[0] -cne 'ToggleCommandMixin') {
        throw 'Retained Canary 2 mixin set is not the one required cold command seam.'
    }

    $seamChecks = @(
        @('dev/resivore/strippingtogglefallingtreecompat/StrippingToggleFallingTreeCompat.class', @('ServerPlayerEvents', 'ServerLifecycleEvents', 'clearNativeState')),
        @('dev/resivore/strippingtogglefallingtreecompat/ToggleAuthority.class', @('fallingtree-disabled', 'addTag', 'removeTag', 'entity-tag limit')),
        @('dev/resivore/strippingtogglefallingtreecompat/client/StrippingToggleFallingTreeCompatClient.class', @('strippingEnabled', 'START_CLIENT_TICK', 'canSend', 'markSent')),
        @('dev/resivore/strippingtogglefallingtreecompat/client/ToggleSyncTracker.class', @('APPLY_NATIVE_STATE', 'SEND_PENDING', 'markSent')),
        @('dev/resivore/strippingtogglefallingtreecompat/mixin/ToggleCommandMixin.class', @('reportMasterState', 'getTags', 'statusTranslationKey'))
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
                throw "Retained Canary 2 seam token missing from $($seamCheck[0]): $token"
            }
        }
        if ($seamCheck[0] -like '*ToggleCommandMixin.class' -and
            ($classText.Contains('addTag') -or $classText.Contains('removeTag'))) {
            throw 'Retained Canary 2 command seam still mutates FallingTree toggle state.'
        }
    }
} finally {
    $archive.Dispose()
}

& (Join-Path $PSScriptRoot 'Verify-UpstreamContract.ps1') -WorkbenchRoot $WorkbenchRoot
if (-not $?) { throw 'Exact upstream dependency verification failed.' }
Write-Output "RETAINED_STRIPPING_TOGGLE_FALLING_TREE_CANARY2_OK: $(Split-Path -Leaf $artifactPath) $hash"
