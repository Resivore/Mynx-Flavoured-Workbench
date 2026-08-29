$ErrorActionPreference = 'Stop'

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$artifactPath = Join-Path $projectRoot 'artifacts\wool-soundproof-chests-0.1.0-alpha1.jar'
$expectedHash = 'E8F0D10DFF823E97A4F4AB39029BF7A71B13501F2294544C55E38F7E1CA9D020'

if (-not (Test-Path -LiteralPath $artifactPath -PathType Leaf)) {
    throw "Retained Alpha 1 artifact is missing: $artifactPath"
}

$actualHash = (Get-FileHash -LiteralPath $artifactPath -Algorithm SHA256).Hash
if ($actualHash -cne $expectedHash) {
    throw "Retained Alpha 1 SHA-256 mismatch: $actualHash"
}

Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive = [IO.Compression.ZipFile]::OpenRead($artifactPath)
try {
    $metadataEntry = $archive.Entries |
        Where-Object FullName -eq 'fabric.mod.json' |
        Select-Object -First 1
    if (-not $metadataEntry) {
        throw 'Retained Alpha 1 has no fabric.mod.json.'
    }

    $reader = [IO.StreamReader]::new($metadataEntry.Open(), [Text.Encoding]::UTF8)
    try {
        $metadata = $reader.ReadToEnd() | ConvertFrom-Json
    }
    finally {
        $reader.Dispose()
    }
}
finally {
    $archive.Dispose()
}

if (([string]$metadata.id) -cne 'wool_soundproof_chests') {
    throw "Retained Alpha 1 mod ID mismatch: $($metadata.id)"
}
if (([string]$metadata.version) -cne '0.1.0-alpha1') {
    throw "Retained Alpha 1 version mismatch: $($metadata.version)"
}

Write-Output "RETAINED_CANARY_OK: $([IO.Path]::GetFileName($artifactPath)) $actualHash"
