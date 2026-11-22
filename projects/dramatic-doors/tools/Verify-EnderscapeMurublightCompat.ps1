[CmdletBinding()]
param(
    [string]$Artifact
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$compatPath = Join-Path $projectRoot 'fabric\src\main\java\com\fizzware\dramaticdoors\fabric\NativeDoorCompat.java'

function Assert-True {
    param(
        [bool]$Condition,
        [string]$Message
    )

    if (-not $Condition) {
        throw $Message
    }
}

$source = Get-Content -LiteralPath $compatPath -Raw -Encoding UTF8
$normalizedSource = [regex]::Replace($source, '\s+', ' ')

Assert-True ($normalizedSource.Contains('watch("enderscape:murublight_door", DDNames.SHORT_MURUSHROOM, DDNames.TALL_MURUSHROOM)')) 'Retained Murushroom IDs must resolve the native Murublight door.'
Assert-True ($normalizedSource.Contains('watch("enderscape:celestial_door", DDNames.SHORT_CELESTIAL, DDNames.TALL_CELESTIAL)')) 'Celestial must use its native door.'
Assert-True (-not $normalizedSource.Contains('"enderscape:murushroom_door"')) 'Obsolete Enderscape source ID.'

$artifactLabel = 'source-only'
if (-not [string]::IsNullOrWhiteSpace($Artifact)) {
    $artifactPath = (Resolve-Path -LiteralPath $Artifact).Path
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [IO.Compression.ZipFile]::OpenRead($artifactPath)
    try {
        $classEntry = $archive.GetEntry('com/fizzware/dramaticdoors/fabric/NativeDoorCompat.class')
        Assert-True ($null -ne $classEntry) 'Packaged artifact lacks NativeDoorCompat.class.'
        $reader = [IO.BinaryReader]::new($classEntry.Open())
        try {
            $classText = [Text.Encoding]::UTF8.GetString($reader.ReadBytes([int]$classEntry.Length))
        }
        finally {
            $reader.Dispose()
        }
        $murublightConstant = 'enderscape:murublight_door'
        $obsoleteConstant = 'enderscape:murushroom_door'
        Assert-True ($classText.Contains($murublightConstant)) 'Packaged Enderscape compatibility does not contain the current Murublight source-ID class constant.'
        Assert-True (-not $classText.Contains($obsoleteConstant)) 'Packaged Enderscape compatibility still contains the obsolete Murushroom source-ID class constant.'
    }
    finally {
        $archive.Dispose()
    }
    $artifactLabel = [IO.Path]::GetFileName($artifactPath)
}

Write-Output "DRAMATIC_DOORS_ENDERSCAPE_MURUBLIGHT_COMPAT_OK: Murushroom Dramatic Doors IDs retained; native registration and generated data use enderscape:murublight_door; artifact=$artifactLabel"
