$ErrorActionPreference = 'Stop'

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$artifactPath = Join-Path $projectRoot 'artifacts\matcha-jei-integration-0.2.0-nonrecipe-discovery-canary1.jar'
$expectedHash = '1E4941E2353C1696505F243E86EB58F0239DD1144D544897370B021F6748A455'

if (-not (Test-Path -LiteralPath $artifactPath -PathType Leaf)) {
    throw "Retained Matcha JEI non-recipe canary is missing: $artifactPath"
}
if ((Get-Item -LiteralPath $artifactPath).Length -ne 70624) {
    throw 'Retained Matcha JEI non-recipe canary size mismatch.'
}
$hash = (Get-FileHash -LiteralPath $artifactPath -Algorithm SHA256).Hash
if ($hash -cne $expectedHash) {
    throw "Retained Matcha JEI non-recipe canary hash mismatch: $hash"
}

Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive = [IO.Compression.ZipFile]::OpenRead($artifactPath)
try {
    foreach ($required in @(
        'fabric.mod.json',
        'dev/resivore/matchajei/MatchaJeiIntegration.class',
        'dev/resivore/matchajei/MatchaNamespaces.class',
        'dev/resivore/matchajei/client/MatchaExactIngredient.class',
        'dev/resivore/matchajei/client/MatchaJeiClient.class',
        'dev/resivore/matchajei/client/MatchaJeiPlugin.class',
        'dev/resivore/matchajei/client/MatchaJeiRuntimeData.class',
        'dev/resivore/matchajei/client/category/MatchaAcquisitionCategory.class',
        'dev/resivore/matchajei/client/category/MatchaVillagerTradeCategory.class',
        'dev/resivore/matchajei/network/MatchaJeiDataPayload.class',
        'dev/resivore/matchajei/server/MatchaDataScanner.class',
        'matcha_jei_integration/component-item-ids.txt',
        'matcha_jei_integration/identity-component-ids.txt'
    )) {
        if (-not $archive.GetEntry($required)) {
            throw "Required non-recipe canary entry is missing: $required"
        }
    }
    if (@($archive.Entries | Where-Object { $_.FullName -like '*.mixins.json' -or $_.FullName -like '*Mixin.class' }).Count) {
        throw 'Non-recipe canary unexpectedly contains a Mixin configuration or class.'
    }

    $metadataEntry = $archive.GetEntry('fabric.mod.json')
    $metadataReader = [IO.StreamReader]::new($metadataEntry.Open())
    try { $metadata = $metadataReader.ReadToEnd() | ConvertFrom-Json }
    finally { $metadataReader.Dispose() }

    if ([string]$metadata.id -cne 'matcha_jei_integration' -or [string]$metadata.version -cne '0.2.0-nonrecipe-discovery-canary1') {
        throw 'Non-recipe canary embedded mod ID/version mismatch.'
    }
    if ([string]$metadata.environment -cne '*') {
        throw 'Non-recipe canary must retain common/server initialization support.'
    }
    if ('dev.resivore.matchajei.MatchaJeiIntegration' -notin @($metadata.entrypoints.main)) {
        throw 'Non-recipe canary common entrypoint is missing.'
    }
    if ('dev.resivore.matchajei.client.MatchaJeiClient' -notin @($metadata.entrypoints.client)) {
        throw 'Non-recipe canary client networking entrypoint is missing.'
    }
    if ('dev.resivore.matchajei.client.MatchaJeiPlugin' -notin @($metadata.entrypoints.jei_mod_plugin)) {
        throw 'Non-recipe canary JEI plugin entrypoint is missing.'
    }

    $commonClasses = @($archive.Entries | Where-Object {
            $_.FullName -match '^dev/resivore/matchajei/(?:[^/]+|data/[^/]+|network/[^/]+|server/[^/]+)\.class$'
        })
    foreach ($entry in $commonClasses) {
        $stream = $entry.Open()
        try {
            $memory = [IO.MemoryStream]::new()
            try {
                $stream.CopyTo($memory)
                $classText = [Text.Encoding]::GetEncoding(28591).GetString($memory.ToArray())
            }
            finally { $memory.Dispose() }
        }
        finally { $stream.Dispose() }
        if ($classText.Contains('mezz/jei') -or $classText.Contains('mezz.jei')) {
            throw "Common/server class directly references JEI: $($entry.FullName)"
        }
        if ($classText.Contains('dev/resivore/matchajei/client/')) {
            throw "Common/server class directly references client integration code: $($entry.FullName)"
        }
    }
}
finally {
    $archive.Dispose()
}

& (Join-Path $PSScriptRoot 'Test-SubtypeRegistrationOwnership.ps1')
& (Join-Path $PSScriptRoot 'Test-NonRecipeDiscovery.ps1')
Write-Output "RETAINED_MATCHA_JEI_NONRECIPE_CANARY1_OK: matcha-jei-integration-0.2.0-nonrecipe-discovery-canary1.jar $hash"
