$ErrorActionPreference = 'Stop'

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$artifactPath = Join-Path $projectRoot 'artifacts\matcha-jei-integration-0.1.1-subtype-registration-fix-canary.jar'
$expectedHash = '4EF3DDDE44EC06E2EE852DD64F5F5D1CF25C1F4F908282F854E9F49B2D5FB60D'

if (-not (Test-Path -LiteralPath $artifactPath -PathType Leaf)) {
    throw "Retained Matcha JEI subtype-fix canary is missing: $artifactPath"
}
if ((Get-Item -LiteralPath $artifactPath).Length -ne 9165) {
    throw 'Retained Matcha JEI subtype-fix canary size mismatch.'
}
$hash = (Get-FileHash -LiteralPath $artifactPath -Algorithm SHA256).Hash
if ($hash -cne $expectedHash) {
    throw "Retained Matcha JEI subtype-fix canary hash mismatch: $hash"
}

Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive = [IO.Compression.ZipFile]::OpenRead($artifactPath)
try {
    foreach ($required in @(
        'fabric.mod.json',
        'dev/resivore/matchajei/MatchaJeiIntegration.class',
        'dev/resivore/matchajei/MatchaNamespaces.class',
        'dev/resivore/matchajei/client/MatchaJeiPlugin.class'
    )) {
        if (-not $archive.GetEntry($required)) {
            throw "Required subtype-fix canary entry is missing: $required"
        }
    }
    if (@($archive.Entries | Where-Object { $_.FullName -like '*.mixins.json' -or $_.FullName -like '*Mixin.class' }).Count) {
        throw 'Subtype-fix canary unexpectedly contains a Mixin configuration or class.'
    }

    $metadataEntry = $archive.GetEntry('fabric.mod.json')
    $metadataReader = [IO.StreamReader]::new($metadataEntry.Open())
    try { $metadata = $metadataReader.ReadToEnd() | ConvertFrom-Json }
    finally { $metadataReader.Dispose() }

    if ([string]$metadata.id -cne 'matcha_jei_integration' -or [string]$metadata.version -cne '0.1.1-subtype-registration-fix-canary') {
        throw 'Subtype-fix canary embedded mod ID/version mismatch.'
    }
    if ([string]$metadata.environment -cne '*') {
        throw 'Subtype-fix canary must retain common/server initialization support.'
    }
    if ('dev.resivore.matchajei.MatchaJeiIntegration' -notin @($metadata.entrypoints.main)) {
        throw 'Subtype-fix canary common entrypoint is missing.'
    }
    if ('dev.resivore.matchajei.client.MatchaJeiPlugin' -notin @($metadata.entrypoints.jei_mod_plugin)) {
        throw 'Subtype-fix canary JEI plugin entrypoint is missing.'
    }

    $commonEntry = $archive.GetEntry('dev/resivore/matchajei/MatchaJeiIntegration.class')
    $commonStream = $commonEntry.Open()
    try {
        $memory = [IO.MemoryStream]::new()
        try {
            $commonStream.CopyTo($memory)
            $commonText = [Text.Encoding]::GetEncoding(28591).GetString($memory.ToArray())
        }
        finally { $memory.Dispose() }
    }
    finally { $commonStream.Dispose() }
    if ($commonText.Contains('mezz/jei') -or $commonText.Contains('mezz.jei')) {
        throw 'Common subtype-fix canary initializer directly references JEI classes.'
    }
}
finally {
    $archive.Dispose()
}

& (Join-Path $PSScriptRoot 'Test-SubtypeRegistrationOwnership.ps1')
Write-Output "RETAINED_MATCHA_JEI_SUBTYPE_FIX_CANARY_OK: matcha-jei-integration-0.1.1-subtype-registration-fix-canary.jar $hash"
