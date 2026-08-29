[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$artifactName = 'inherent-3x3-inventory-crafting-0.1.0-canary3.jar'
$archivePath = Join-Path $projectRoot "artifacts\$artifactName"
$buildPath = Join-Path $projectRoot "build\libs\$artifactName"
$expectedHash = '776918C5E5A76A8F81E5F67A481D819A5586CE1CA45CCDC0EFFAF04622AAEF08'
$expectedSize = 23675L
$artworkEntry = 'assets/inherent_3x3_inventory_crafting/textures/gui/container/inventory.png'
$artworkHash = 'A52221738FE4205B4EE1BB0A5F524A236BCC032FAADC869AE4611F46349A4A7C'

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
    throw "Retained Canary 3 artifact is missing: $archivePath"
}

$archive = Get-Item -LiteralPath $archivePath
if ($archive.Length -ne $expectedSize) {
    throw "Retained Canary 3 size mismatch: expected $expectedSize, found $($archive.Length)."
}

$archiveHash = (Get-FileHash -LiteralPath $archivePath -Algorithm SHA256).Hash
if ($archiveHash -ne $expectedHash) {
    throw "Retained Canary 3 SHA-256 mismatch: expected $expectedHash, found $archiveHash."
}

if (Test-Path -LiteralPath $buildPath -PathType Leaf) {
    $buildHash = (Get-FileHash -LiteralPath $buildPath -Algorithm SHA256).Hash
    if ($buildHash -ne $archiveHash) {
        throw "Clean-build and retained Canary 3 hashes differ: $buildHash vs $archiveHash."
    }
}

Add-Type -AssemblyName System.IO.Compression.FileSystem
$canary3 = Get-EntryState -JarPath $archivePath

if ($canary3[$artworkEntry].length -ne 2040L -or $canary3[$artworkEntry].hash -cne $artworkHash) {
    throw 'Canary 3 does not contain the exact authoritative inventory PNG.'
}

$mixinBytes = $canary3['dev/resivore/inventorycrafting/mixin/client/InventoryScreenMixin.class'].bytes
$mixinText = [System.Text.Encoding]::GetEncoding(28591).GetString($mixinBytes)
if (-not $mixinText.Contains('net/minecraft/client/gui/screens/inventory/AbstractRecipeBookScreen')) {
    throw 'Canary 3 InventoryScreenMixin does not retain the exact target superclass.'
}
if ($mixinText.Contains('Lorg/spongepowered/asm/mixin/Shadow;')) {
    throw 'Canary 3 InventoryScreenMixin regressed to an inherited-field @Shadow.'
}
if (-not $mixinText.Contains('textures/gui/container/inventory.png') -or
        -not $mixinText.Contains('Lorg/spongepowered/asm/mixin/injection/ModifyArg;')) {
    throw 'Canary 3 InventoryScreenMixin is missing the narrow artwork hook.'
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
    if ($metadata.id -ne 'inherent_3x3_inventory_crafting') {
        throw "Unexpected embedded mod id: $($metadata.id)"
    }
    if ($metadata.version -ne '0.1.0-canary3') {
        throw "Unexpected embedded version: $($metadata.version)"
    }
    if ($metadata.custom.'workbench:classification' -ne 'GENERATED / UNTESTED') {
        throw 'Embedded Canary 3 classification is not GENERATED / UNTESTED.'
    }
    if ($metadata.custom.'workbench:deployment' -ne 'NOT DEPLOYED') {
        throw 'Embedded Canary 3 generation marker is not NOT DEPLOYED.'
    }
}
finally {
    $zip.Dispose()
}

Write-Output "INVENTORY_3X3_RETAINED_CANARY3_OK: $artifactName $expectedSize bytes $expectedHash"
