[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$artifactName = 'dramaticdoors-1.20.1-3.3.3+26.2-workbench-canary6.jar'
$artifactPath = Join-Path $projectRoot "artifacts\$artifactName"
$expectedHash = '1D6B8286C3CC4E657F5326A2E5807081E35F66D13A41E77B3931B647FCDDA4F6'
$expectedSize = 13245067L

if (-not (Test-Path -LiteralPath $artifactPath -PathType Leaf)) {
    throw "Retained Canary 6 is missing: $artifactPath"
}
if ((Get-Item -LiteralPath $artifactPath).Length -ne $expectedSize) {
    throw 'Retained Canary 6 size mismatch.'
}
$actualHash = (Get-FileHash -LiteralPath $artifactPath -Algorithm SHA256).Hash
if ($actualHash -cne $expectedHash) {
    throw "Retained Canary 6 SHA-256 mismatch: $actualHash"
}

Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive = [IO.Compression.ZipFile]::OpenRead($artifactPath)
try {
    $metadataEntry = $archive.GetEntry('fabric.mod.json')
    if ($null -eq $metadataEntry) { throw 'Retained Canary 6 lacks fabric.mod.json.' }
    $reader = [IO.StreamReader]::new($metadataEntry.Open())
    try { $metadata = $reader.ReadToEnd() | ConvertFrom-Json }
    finally { $reader.Dispose() }
    if ($metadata.id -cne 'dramaticdoors' -or $metadata.version -cne '1.20.1-3.3.3+26.2-workbench-canary6') {
        throw "Unexpected embedded identity: $($metadata.id) $($metadata.version)"
    }

    $itemDefinitions = @($archive.Entries | Where-Object {
        $_.FullName -match '^assets/[^/]+/items/[^/]+\.json$'
    })
    $uniqueDefinitions = @($itemDefinitions.FullName | Sort-Object -Unique)
    if ($itemDefinitions.Count -ne 1692 -or $uniqueDefinitions.Count -ne 1692) {
        throw "Unexpected item-definition inventory: $($itemDefinitions.Count) total / $($uniqueDefinitions.Count) unique."
    }

    $manifestEntry = $archive.GetEntry('META-INF/MANIFEST.MF')
    if ($null -eq $manifestEntry) { throw 'Retained Canary 6 lacks META-INF/MANIFEST.MF.' }
    $reader = [IO.StreamReader]::new($manifestEntry.Open())
    try { $manifest = $reader.ReadToEnd() }
    finally { $reader.Dispose() }
    foreach ($line in @(
        'Implementation-Timestamp: 1970-01-01T00:00:00+0000',
        'Timestamp: 0',
        'Built-On-Minecraft: 26.2',
        'Fabric-Loader-Version: 0.19.3'
    )) {
        if (-not $manifest.Contains($line)) { throw "Retained Canary 6 manifest lacks: $line" }
    }
}
finally {
    $archive.Dispose()
}

& (Join-Path $PSScriptRoot 'Verify-RecipeResources26_2.ps1') -Artifact $artifactPath
Write-Output "DRAMATIC_DOORS_RETAINED_CANARY6_OK: $artifactName $expectedSize bytes $actualHash; 1692 item definitions"
