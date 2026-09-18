[CmdletBinding()]
param(
    [string]$Artifact
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$compatPath = Join-Path $projectRoot 'common\src\main\java\com\fizzware\dramaticdoors\compat\registries\EnderscapeCompat.java'

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

Assert-True (-not $normalizedSource.Contains('enderscape", "murushroom_door')) 'Enderscape compatibility still references the obsolete enderscape:murushroom_door source ID.'
Assert-True (([regex]::Matches($normalizedSource, [regex]::Escape('Identifier.fromNamespaceAndPath("enderscape", "murublight_door")'))).Count -eq 5) 'Enderscape compatibility must use enderscape:murublight_door for its block registration and four generated recipe/advancement paths.'
Assert-True ($normalizedSource.Contains('DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_MURUSHROOM, Identifier.fromNamespaceAndPath("enderscape", "murublight_door"), true);')) 'The short Murushroom-ID recipe advancement must be generated from enderscape:murublight_door.'
Assert-True ($normalizedSource.Contains('DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MURUSHROOM, Identifier.fromNamespaceAndPath("enderscape", "murublight_door"));')) 'The tall Murushroom-ID recipe advancement must be generated from enderscape:murublight_door.'
Assert-True ($normalizedSource.Contains('DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_MURUSHROOM, Identifier.fromNamespaceAndPath("enderscape", "murublight_door"), true);')) 'The short Murushroom-ID recipe must be generated from enderscape:murublight_door.'
Assert-True ($normalizedSource.Contains('DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MURUSHROOM, Identifier.fromNamespaceAndPath("enderscape", "murublight_door"), "tall_wooden_door");')) 'The tall Murushroom-ID recipe must be generated from enderscape:murublight_door.'
Assert-True ($normalizedSource.Contains('DDNames.SHORT_MURUSHROOM')) 'The existing Dramatic Doors short_murushroom_door identity must remain intact.'
Assert-True ($normalizedSource.Contains('DDNames.TALL_MURUSHROOM')) 'The existing Dramatic Doors tall_murushroom_door identity must remain intact.'
Assert-True (([regex]::Matches($normalizedSource, [regex]::Escape('Identifier.fromNamespaceAndPath("enderscape", "celestial_door")'))).Count -eq 5) 'The existing valid enderscape:celestial_door compatibility family must remain unchanged.'

$artifactLabel = 'source-only'
if (-not [string]::IsNullOrWhiteSpace($Artifact)) {
    $artifactPath = (Resolve-Path -LiteralPath $Artifact).Path
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [IO.Compression.ZipFile]::OpenRead($artifactPath)
    try {
        $classEntry = $archive.GetEntry('com/fizzware/dramaticdoors/compat/registries/EnderscapeCompat.class')
        Assert-True ($null -ne $classEntry) 'Packaged artifact lacks EnderscapeCompat.class.'
        $reader = [IO.BinaryReader]::new($classEntry.Open())
        try {
            $classText = [Text.Encoding]::UTF8.GetString($reader.ReadBytes([int]$classEntry.Length))
        }
        finally {
            $reader.Dispose()
        }
        $murublightConstant = "$([char]0)$([char]15)murublight_door"
        $obsoleteConstant = "$([char]0)$([char]15)murushroom_door"
        Assert-True ($classText.Contains($murublightConstant)) 'Packaged Enderscape compatibility does not contain the current Murublight source-ID class constant.'
        Assert-True (-not $classText.Contains($obsoleteConstant)) 'Packaged Enderscape compatibility still contains the obsolete Murushroom source-ID class constant.'
    }
    finally {
        $archive.Dispose()
    }
    $artifactLabel = [IO.Path]::GetFileName($artifactPath)
}

Write-Output "DRAMATIC_DOORS_ENDERSCAPE_MURUBLIGHT_COMPAT_OK: Murushroom Dramatic Doors IDs retained; 4 generated recipe/advancement paths and block registration use enderscape:murublight_door; artifact=$artifactLabel"
