Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$projectRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path
$artifactPath = [IO.Path]::GetFullPath((Join-Path $projectRoot 'artifacts\custom-portals-26.2-4.0.0+26.2-port-canary2.jar'))
$expectedHash = '13FD0E76748FCC3EF963BCC2F4A820D5FC2D473A90AD5D5988CBE0129C2BE148'

if (-not (Test-Path -LiteralPath $artifactPath -PathType Leaf)) { throw "Retained Canary 2 is missing: $artifactPath" }
$artifact = Get-Item -LiteralPath $artifactPath -Force
if (($artifact.Attributes -band [IO.FileAttributes]::ReparsePoint) -ne 0) { throw 'Retained Canary 2 must not be a reparse point.' }
if ($artifact.Length -ne 910246) { throw "Retained Canary 2 size mismatch: $($artifact.Length)" }
$actualHash = (Get-FileHash -LiteralPath $artifactPath -Algorithm SHA256).Hash
if ($actualHash -cne $expectedHash) { throw "Retained Canary 2 hash mismatch: $actualHash" }

Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive = [IO.Compression.ZipFile]::OpenRead($artifactPath)
try {
    $metadataEntry = $archive.GetEntry('fabric.mod.json')
    if ($null -eq $metadataEntry) { throw 'Retained Canary 2 has no fabric.mod.json.' }
    $reader = [IO.StreamReader]::new($metadataEntry.Open(), [Text.Encoding]::UTF8)
    try { $metadata = $reader.ReadToEnd() | ConvertFrom-Json }
    finally { $reader.Dispose() }

    if ([string]$metadata.id -cne 'customportals') { throw "Unexpected mod ID: $($metadata.id)" }
    if ([string]$metadata.version -cne '4.0.0+26.2-port-canary2') { throw "Unexpected mod version: $($metadata.version)" }
    if ([string]$metadata.depends.minecraft -cne '~26.2') { throw "Unexpected Minecraft dependency: $($metadata.depends.minecraft)" }
    if ($null -eq $metadata.depends.PSObject.Properties['fabric-api']) { throw 'Fabric API dependency is missing.' }
    if ($null -eq $metadata.depends.PSObject.Properties['yet_another_config_lib_v3']) { throw 'YACL v3 dependency is missing.' }
    if ($null -eq $archive.GetEntry('META-INF/jars/cardinal-components-base-8.0.0.jar')) { throw 'Embedded Cardinal Components base is missing.' }
    if ($null -eq $archive.GetEntry('META-INF/jars/cardinal-components-level-8.0.0.jar')) { throw 'Embedded Cardinal Components level module is missing.' }
}
finally {
    $archive.Dispose()
}

Write-Output 'CUSTOM_PORTALS_CANARY2_VERIFIED'
Write-Output "JAR=$artifactPath"
Write-Output "SHA256=$actualHash"
