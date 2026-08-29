[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

& (Join-Path $PSScriptRoot 'Verify-RetainedCanary3.ps1')

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$canary3Path = Join-Path $projectRoot 'artifacts\inherent-3x3-inventory-crafting-0.1.0-canary3.jar'
$artifactName = 'inherent-3x3-inventory-crafting-0.1.0-canary4.jar'
$archivePath = Join-Path $projectRoot "artifacts\$artifactName"
$buildPath = Join-Path $projectRoot "build\libs\$artifactName"
$expectedHash = '6E797291DD6C68F5AFE65F91177AD2653F7C4A99E10BD20E078FAFD7AAF6CB33'
$expectedSize = 23788L
$artworkEntry = 'assets/inherent_3x3_inventory_crafting/textures/gui/container/inventory.png'
$artworkHash = '8E7BAD5FEC6571199D651D68C4F1114A5CC70A7D98F59B713345A0F7866614D8'

Add-Type -AssemblyName System.IO.Compression.FileSystem

function Get-EntryState {
    param([Parameter(Mandatory = $true)][string] $JarPath)

    $result = @{}
    $zip = [System.IO.Compression.ZipFile]::OpenRead($JarPath)
    try {
        foreach ($entry in $zip.Entries) {
            if ($entry.FullName.EndsWith('/')) { continue }
            $stream = $entry.Open()
            $memory = [System.IO.MemoryStream]::new()
            try {
                $stream.CopyTo($memory)
                $sha = [System.Security.Cryptography.SHA256]::Create()
                try {
                    $bytes = $memory.ToArray()
                    $hash = ([System.BitConverter]::ToString($sha.ComputeHash($bytes))).Replace('-', '')
                }
                finally {
                    $sha.Dispose()
                }
                $result[$entry.FullName] = [ordered]@{
                    length = $entry.Length
                    hash = $hash
                    bytes = $bytes
                }
            }
            finally {
                $memory.Dispose()
                $stream.Dispose()
            }
        }
    }
    finally {
        $zip.Dispose()
    }
    return $result
}

if (-not (Test-Path -LiteralPath $archivePath -PathType Leaf)) {
    throw "Retained Canary 4 artifact is missing: $archivePath"
}

$archive = Get-Item -LiteralPath $archivePath
if ($archive.Length -ne $expectedSize) {
    throw "Retained Canary 4 size mismatch: expected $expectedSize, found $($archive.Length)."
}

$archiveHash = (Get-FileHash -LiteralPath $archivePath -Algorithm SHA256).Hash
if ($archiveHash -cne $expectedHash) {
    throw "Retained Canary 4 SHA-256 mismatch: expected $expectedHash, found $archiveHash."
}

if (Test-Path -LiteralPath $buildPath -PathType Leaf) {
    $buildHash = (Get-FileHash -LiteralPath $buildPath -Algorithm SHA256).Hash
    if ($buildHash -cne $archiveHash) {
        throw "Clean-build and retained Canary 4 hashes differ: $buildHash vs $archiveHash."
    }
}

$canary3 = Get-EntryState -JarPath $canary3Path
$canary4 = Get-EntryState -JarPath $archivePath
$canary3Names = @($canary3.Keys | Sort-Object)
$canary4Names = @($canary4.Keys | Sort-Object)

$addedEntries = @($canary4Names | Where-Object { -not $canary3.ContainsKey($_) })
$removedEntries = @($canary3Names | Where-Object { -not $canary4.ContainsKey($_) })
if ($addedEntries.Count -ne 0 -or $removedEntries.Count -ne 0) {
    throw "Canary 4 entry-set drift: added [$($addedEntries -join ', ')], removed [$($removedEntries -join ', ')]."
}

$expectedChangedEntries = @(
    $artworkEntry,
    'dev/resivore/inventorycrafting/Inventory3x3Crafting.class',
    'dev/resivore/inventorycrafting/InventoryCraftingLayout.class',
    'dev/resivore/inventorycrafting/mixin/InventoryMenuMixin.class',
    'fabric.mod.json'
) | Sort-Object
$changedEntries = @($canary3Names | Where-Object {
    $canary3[$_].length -ne $canary4[$_].length -or $canary3[$_].hash -cne $canary4[$_].hash
}) | Sort-Object
if (($changedEntries | ConvertTo-Json -Compress) -cne ($expectedChangedEntries | ConvertTo-Json -Compress)) {
    throw "Canary 4 changed unexpected JAR entries: $($changedEntries -join ', ')"
}

if ($canary4[$artworkEntry].length -ne 2040L -or $canary4[$artworkEntry].hash -cne $artworkHash) {
    throw 'Canary 4 does not contain the exact replacement inventory PNG.'
}

$zip = [System.IO.Compression.ZipFile]::OpenRead($archivePath)
try {
    $nestedJars = @($zip.Entries | Where-Object { $_.FullName.EndsWith('.jar', [System.StringComparison]::OrdinalIgnoreCase) })
    if ($nestedJars.Count -ne 0) {
        throw "Unexpected bundled third-party JARs: $($nestedJars.FullName -join ', ')"
    }

    $metadataEntry = $zip.GetEntry('fabric.mod.json')
    $reader = [System.IO.StreamReader]::new($metadataEntry.Open())
    try {
        $metadata = $reader.ReadToEnd() | ConvertFrom-Json
    }
    finally {
        $reader.Dispose()
    }
    if ($metadata.id -ne 'inherent_3x3_inventory_crafting' -or $metadata.version -ne '0.1.0-canary4') {
        throw "Unexpected embedded identity: $($metadata.id) $($metadata.version)"
    }
    if ($metadata.custom.'workbench:classification' -ne 'GENERATED / UNTESTED' -or
            $metadata.custom.'workbench:deployment' -ne 'NOT DEPLOYED') {
        throw 'Embedded Canary 4 generation provenance is incorrect.'
    }
}
finally {
    $zip.Dispose()
}

Write-Output "INVENTORY_3X3_RETAINED_CANARY4_OK: $artifactName $expectedSize bytes $expectedHash"
