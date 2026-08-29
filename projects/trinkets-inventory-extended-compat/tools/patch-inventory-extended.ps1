param(
    [string]$InputJar = "../../originals/mods/inventoryextended-1.1.2-mc26.2.jar",
    [string]$OutputJar = "../../test-builds/inventoryextended-1.1.2-mc26.2-trinkets-compat-canary1.jar"
)

$ErrorActionPreference = 'Stop'
$projectDirectory = Split-Path -Parent $PSScriptRoot

function Resolve-ProjectPath([string]$Path) {
    if ([System.IO.Path]::IsPathRooted($Path)) {
        return [System.IO.Path]::GetFullPath($Path)
    }
    return [System.IO.Path]::GetFullPath((Join-Path $projectDirectory $Path))
}

function Get-Sha256([string]$Path) {
    $stream = [System.IO.File]::OpenRead($Path)
    try {
        $sha256 = [System.Security.Cryptography.SHA256]::Create()
        try {
            return [System.BitConverter]::ToString($sha256.ComputeHash($stream)).Replace('-', '')
        } finally {
            $sha256.Dispose()
        }
    } finally {
        $stream.Dispose()
    }
}

$inputPath = Resolve-ProjectPath $InputJar
$outputPath = Resolve-ProjectPath $OutputJar

if (-not (Test-Path -LiteralPath $inputPath)) {
    throw "Pristine Inventory Extended JAR not found: $inputPath"
}

$expectedHash = '7CDBE2079D5E8BE9C5FABA8B03DBCCC1DDCCB7F99EC48987079CC9BD8E235BC6'
$actualHash = Get-Sha256 $inputPath
if ($actualHash -ne $expectedHash) {
    throw "Refusing to patch unexpected input. Expected $expectedHash, found $actualHash"
}

$outputDirectory = Split-Path -Parent $outputPath
New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null
Copy-Item -LiteralPath $inputPath -Destination $outputPath -Force

Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive = [System.IO.Compression.ZipFile]::Open($outputPath, [System.IO.Compression.ZipArchiveMode]::Update)
try {
    $entry = $archive.GetEntry('inventoryextended.mixins.json')
    if ($null -eq $entry) {
        throw 'inventoryextended.mixins.json is missing from the copied JAR'
    }
    $entryTimestamp = $entry.LastWriteTime

    $reader = [System.IO.StreamReader]::new($entry.Open())
    try {
        $config = $reader.ReadToEnd() | ConvertFrom-Json
    } finally {
        $reader.Dispose()
    }

    $matches = @($config.mixins | Where-Object { $_ -eq 'FixCreativeSlotRangeCheck' })
    if ($matches.Count -ne 1) {
        throw "Expected exactly one FixCreativeSlotRangeCheck entry, found $($matches.Count)"
    }

    $config.mixins = @($config.mixins | Where-Object { $_ -ne 'FixCreativeSlotRangeCheck' })
    $replacement = $config | ConvertTo-Json -Depth 10
    $entry.Delete()
    $newEntry = $archive.CreateEntry('inventoryextended.mixins.json', [System.IO.Compression.CompressionLevel]::Optimal)
    $newEntry.LastWriteTime = $entryTimestamp
    $writer = [System.IO.StreamWriter]::new($newEntry.Open(), [System.Text.UTF8Encoding]::new($false))
    try {
        $writer.Write($replacement)
    } finally {
        $writer.Dispose()
    }
} finally {
    $archive.Dispose()
}

$outputHash = Get-Sha256 $outputPath
Write-Output "Created $outputPath"
Write-Output "SHA-256 $outputHash"
