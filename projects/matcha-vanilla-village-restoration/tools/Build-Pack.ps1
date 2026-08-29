[CmdletBinding()]
param(
    [string] $OutputPath
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

Add-Type -AssemblyName System.IO.Compression.FileSystem

$projectRoot = Split-Path -Parent $PSScriptRoot
$repoRoot = (Resolve-Path (Join-Path $projectRoot '..\..')).Path

if ([string]::IsNullOrWhiteSpace($OutputPath)) {
    $OutputPath = Join-Path $repoRoot 'test-builds\matcha-vanilla-village-restoration-0.1.0-canary1.zip'
}

$payload = @(
    'data/minecraft/worldgen/structure/village_desert.json',
    'data/minecraft/worldgen/structure/village_plains.json',
    'data/minecraft/worldgen/structure/village_savanna.json',
    'data/minecraft/worldgen/structure/village_snowy.json',
    'data/minecraft/worldgen/structure/village_taiga.json',
    'pack.mcmeta'
) | Sort-Object

$resolvedOutput = [System.IO.Path]::GetFullPath($OutputPath)
if (Test-Path -LiteralPath $resolvedOutput) {
    throw "Refusing to overwrite existing artifact: $resolvedOutput"
}

$outputDirectory = Split-Path -Parent $resolvedOutput
$null = [System.IO.Directory]::CreateDirectory($outputDirectory)

$fixedTimestamp = [System.DateTimeOffset]::new(2000, 1, 1, 0, 0, 0, [System.TimeSpan]::Zero)
$fileStream = [System.IO.File]::Open($resolvedOutput, [System.IO.FileMode]::CreateNew)
$archive = [System.IO.Compression.ZipArchive]::new(
    $fileStream,
    [System.IO.Compression.ZipArchiveMode]::Create,
    $false
)

try {
    foreach ($relativePath in $payload) {
        $sourcePath = Join-Path $projectRoot $relativePath.Replace('/', '\')
        if (-not (Test-Path -LiteralPath $sourcePath -PathType Leaf)) {
            throw "Missing payload source: $relativePath"
        }

        $entry = $archive.CreateEntry($relativePath, [System.IO.Compression.CompressionLevel]::Optimal)
        $entry.LastWriteTime = $fixedTimestamp
        $inputStream = [System.IO.File]::OpenRead($sourcePath)
        $outputStream = $entry.Open()
        try {
            $inputStream.CopyTo($outputStream)
        }
        finally {
            $outputStream.Dispose()
            $inputStream.Dispose()
        }
    }
}
finally {
    $archive.Dispose()
    $fileStream.Dispose()
}

$artifact = Get-Item -LiteralPath $resolvedOutput
$hash = (Get-FileHash -LiteralPath $resolvedOutput -Algorithm SHA256).Hash
Write-Host "Built: $($artifact.FullName)"
Write-Host "Size: $($artifact.Length) bytes"
Write-Host "SHA-256: $hash"
