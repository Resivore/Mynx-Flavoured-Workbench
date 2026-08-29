[CmdletBinding()]
param(
    [string]$WorkbenchRoot
)

$ErrorActionPreference = 'Stop'
if ([string]::IsNullOrWhiteSpace($WorkbenchRoot)) {
    $WorkbenchRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..\..')).Path
}
$modsRoot = Join-Path $WorkbenchRoot 'originals\mods'

$expected = [ordered]@{
    'inventoryextended-1.1.2-mc26.2.jar' = '7CDBE2079D5E8BE9C5FABA8B03DBCCC1DDCCB7F99EC48987079CC9BD8E235BC6'
    'simple_trash_slot-1.0.4+26.1.2-fabric.jar' = '81790940DA606F732A565ADCA2B4EC9FA0E8E0015CB1CDC1C4E009506244739B'
    'trinkets-4.1.0-beta.3+26.2.jar.disabled' = '958D064DA8DFA62C782D7CAA00D17F9BA5630301096A0E637788FAA2D982A564'
    'jei-26.2-fabric-30.18.0.144.jar' = '20BC7F0EBE5F36F84C8C4D571469968BE54A6E1989B4E85A736136DC41EA8FE2'
    'Simple-Portable-Crafting-1.0.0+mc26.2.jar' = '34C7B4AC4FF2EDC46EEFC21429416603E81B6D3944EA508E4D7CAFBE41C17DA4'
}

foreach ($entry in $expected.GetEnumerator()) {
    $path = Join-Path $modsRoot $entry.Key
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        throw "Required audited dependency is missing: $path"
    }
    $actual = (Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash
    if ($actual -cne $entry.Value) {
        throw "Audited dependency hash mismatch for $($entry.Key): $actual"
    }
}

Add-Type -AssemblyName System.IO.Compression.FileSystem
function Get-FabricMetadata([string]$path) {
    $archive = [IO.Compression.ZipFile]::OpenRead($path)
    try {
        $metadataEntry = $archive.GetEntry('fabric.mod.json')
        if (-not $metadataEntry) { throw "fabric.mod.json is missing from $path" }
        $reader = [IO.StreamReader]::new($metadataEntry.Open())
        try { return $reader.ReadToEnd() | ConvertFrom-Json } finally { $reader.Dispose() }
    } finally {
        $archive.Dispose()
    }
}

$ie = Get-FabricMetadata (Join-Path $modsRoot 'inventoryextended-1.1.2-mc26.2.jar')
$trash = Get-FabricMetadata (Join-Path $modsRoot 'simple_trash_slot-1.0.4+26.1.2-fabric.jar')
$trinkets = Get-FabricMetadata (Join-Path $modsRoot 'trinkets-4.1.0-beta.3+26.2.jar.disabled')
$jei = Get-FabricMetadata (Join-Path $modsRoot 'jei-26.2-fabric-30.18.0.144.jar')
if ($ie.id -cne 'inventoryextended' -or $ie.version -cne '1.1.2') { throw 'Inventory Extended metadata drifted.' }
if ($trash.id -cne 'simple_trash_slot' -or $trash.version -cne '1.0.4') { throw 'Simple Trash metadata drifted.' }
if ($trinkets.id -cne 'trinkets_updated' -or $trinkets.version -cne '4.1.0-beta.3+26.2') { throw 'Trinkets metadata drifted.' }
if ($jei.id -cne 'jei' -or $jei.version -cne '30.18.0.144') { throw 'JEI metadata drifted.' }

Write-Output 'INVENTORY_3X3_UPSTREAM_CONTRACT_OK'
