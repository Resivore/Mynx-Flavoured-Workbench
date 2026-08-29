[CmdletBinding()]
param(
    [string]$Artifact
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$shortPath = Join-Path $projectRoot 'common\src\main\java\com\fizzware\dramaticdoors\blocks\ShortWeatheringDoorBlock.java'
$tallPath = Join-Path $projectRoot 'common\src\main\java\com\fizzware\dramaticdoors\blocks\TallWeatheringDoorBlock.java'

function Assert-True {
    param([bool]$Condition, [string]$Message)
    if (-not $Condition) { throw $Message }
}

foreach ($path in @($shortPath, $tallPath)) {
    $source = [regex]::Replace((Get-Content -LiteralPath $path -Raw -Encoding UTF8), '\s+', ' ')
    $matches = [regex]::Matches($source, 'public boolean isRandomlyTicking\(BlockState state\) \{ (?<body>.*?) \}')
    Assert-True ($matches.Count -eq 1) "Expected one isRandomlyTicking implementation in $path."
    $body = $matches[0].Groups['body'].Value
    Assert-True ($body -ceq 'return this.weatherState != WeatheringCopper.WeatherState.OXIDIZED;') "Weathering state-cache initialization must not materialize the registry-backed next-stage map in $path."
    Assert-True (-not $body.Contains('getNext(')) "isRandomlyTicking still resolves pending registry IDs in $path."
}

$artifactLabel = 'source-only'
if (-not [string]::IsNullOrWhiteSpace($Artifact)) {
    $artifactPath = (Resolve-Path -LiteralPath $Artifact).Path
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [IO.Compression.ZipFile]::OpenRead($artifactPath)
    try {
        foreach ($entryName in @(
            'com/fizzware/dramaticdoors/blocks/ShortWeatheringDoorBlock.class',
            'com/fizzware/dramaticdoors/blocks/TallWeatheringDoorBlock.class'
        )) {
            Assert-True ($null -ne $archive.GetEntry($entryName)) "Packaged Canary lacks $entryName."
        }
    }
    finally {
        $archive.Dispose()
    }
    $artifactLabel = [IO.Path]::GetFileName($artifactPath)
}

Write-Output "DRAMATIC_DOORS_WEATHERING_INITIALIZATION_COMPAT_OK: short/tall state-cache checks avoid pending registry maps; artifact=$artifactLabel"
