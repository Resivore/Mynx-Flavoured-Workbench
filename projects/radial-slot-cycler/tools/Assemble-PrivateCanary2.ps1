[CmdletBinding()]
param(
    [string] $TravelerJarPath,
    [string] $GradlePath,
    [string] $JavaHome
)

$ErrorActionPreference = 'Stop'

if ($PSVersionTable.PSVersion.Major -lt 7) {
    throw 'Private Canary 2 assembly requires PowerShell 7 or newer; run this script with pwsh.'
}

$expectedTravelerName = 'travelertoolbelt-fabric-26.2-1.0.2-TRINKETS-CANARY1.jar'
$expectedTravelerSize = 308008L
$expectedTravelerHash = '43D3370135F42EF74129B904325006F91BA32074AB8DBD81A649170D1366FCD6'
$sourceEntryName = 'assets/travelertoolbelt/textures/gui/belt_overlay.png'
$privateEntryName = 'assets/radial_slot_cycler/textures/gui/belt_overlay.png'
$expectedResourceSize = 4580L
$expectedResourceHash = 'C73B3634EE3FE16FC7DC00CED67EF833DE11E8B650BEEF3095B2F5900DC81910'

$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..\..')).Path
$projectRoot = Join-Path $repositoryRoot 'projects\radial-slot-cycler'
$privateRoot = Join-Path $projectRoot 'build\private\canary2'
$privateResources = Join-Path $privateRoot 'resources'
$privateManifest = Join-Path $privateRoot 'PRIVATE_ASSEMBLY.json'
$privateOutputRoot = Join-Path $privateRoot 'artifact'
$privateArtifactName = 'radial-slot-cycler-0.1.0-canary2-private.jar'
$privateArtifactPath = Join-Path $privateOutputRoot $privateArtifactName
$baseArtifactPath = Join-Path $projectRoot 'build\libs\radial-slot-cycler-private-base-0.1.0-canary2-private.jar'

if (-not $TravelerJarPath) {
    throw 'Pass -TravelerJarPath for the exact user-owned Traveler Tool Belt input; it is not retained in this repository.'
}

$workingTreeStatus = @(& git -C $repositoryRoot status --porcelain=v1 --untracked-files=normal)
if ($LASTEXITCODE -ne 0) {
    throw 'Unable to verify the Git working tree before private assembly.'
}
if ($workingTreeStatus.Count -ne 0) {
    throw "Private assembly requires a clean Git working tree so its source commit is exact:`n$($workingTreeStatus -join "`n")"
}

function Assert-ContainedPath {
    param(
        [Parameter(Mandatory = $true)][string] $Candidate,
        [Parameter(Mandatory = $true)][string] $Parent,
        [Parameter(Mandatory = $true)][string] $Label
    )

    $candidateFull = [IO.Path]::GetFullPath($Candidate).TrimEnd('\', '/')
    $parentFull = [IO.Path]::GetFullPath($Parent).TrimEnd('\', '/')
    $prefix = $parentFull + [IO.Path]::DirectorySeparatorChar
    if (-not $candidateFull.StartsWith($prefix, [StringComparison]::OrdinalIgnoreCase)) {
        throw "$Label escaped its required parent: $candidateFull"
    }
}

function Assert-IgnoredAndUntracked {
    param([Parameter(Mandatory = $true)][string] $FullPath)

    $relative = [IO.Path]::GetRelativePath($repositoryRoot, $FullPath).Replace('\', '/')
    & git -C $repositoryRoot check-ignore -q -- $relative
    if ($LASTEXITCODE -ne 0) {
        throw "Private output is not ignored by Git: $relative"
    }
    $tracked = @(& git -C $repositoryRoot ls-files -- $relative)
    if ($LASTEXITCODE -ne 0 -or $tracked.Count -ne 0) {
        throw "Private output is already tracked by Git: $relative"
    }
}

function Get-BytesHash {
    param([Parameter(Mandatory = $true)][byte[]] $Bytes)

    $digest = [Security.Cryptography.SHA256]::Create()
    try {
        return ([BitConverter]::ToString($digest.ComputeHash($Bytes))).Replace('-', '')
    }
    finally {
        $digest.Dispose()
    }
}

Assert-ContainedPath -Candidate $privateResources -Parent $privateRoot -Label 'Private resource staging'
Assert-ContainedPath -Candidate $privateArtifactPath -Parent $privateOutputRoot -Label 'Private artifact'
Assert-IgnoredAndUntracked -FullPath $privateResources
Assert-IgnoredAndUntracked -FullPath $privateArtifactPath

if (-not (Test-Path -LiteralPath $TravelerJarPath -PathType Leaf)) {
    throw "Exact accepted Traveler Tool Belt JAR is required: $TravelerJarPath"
}
$TravelerJarPath = (Resolve-Path -LiteralPath $TravelerJarPath).Path
$traveler = Get-Item -LiteralPath $TravelerJarPath
if ($traveler.Name -cne $expectedTravelerName) {
    throw "Traveler filename mismatch: expected $expectedTravelerName, found $($traveler.Name)"
}
if ($traveler.Length -ne $expectedTravelerSize) {
    throw "Traveler size mismatch: expected $expectedTravelerSize, found $($traveler.Length)"
}
$travelerHash = (Get-FileHash -LiteralPath $TravelerJarPath -Algorithm SHA256).Hash
if ($travelerHash -cne $expectedTravelerHash) {
    throw "Traveler SHA-256 mismatch: expected $expectedTravelerHash, found $travelerHash"
}

Add-Type -AssemblyName System.IO.Compression.FileSystem
$travelerArchive = [IO.Compression.ZipFile]::OpenRead($TravelerJarPath)
try {
    $metadataEntries = @($travelerArchive.Entries | Where-Object FullName -ceq 'fabric.mod.json')
    $sourceEntries = @($travelerArchive.Entries | Where-Object FullName -ceq $sourceEntryName)
    if ($metadataEntries.Count -ne 1 -or $sourceEntries.Count -ne 1) {
        throw 'Traveler JAR does not contain one exact metadata entry and one exact radial overlay entry.'
    }

    $metadataReader = [IO.StreamReader]::new($metadataEntries[0].Open(), [Text.Encoding]::UTF8)
    try {
        $travelerMetadata = $metadataReader.ReadToEnd() | ConvertFrom-Json
    }
    finally {
        $metadataReader.Dispose()
    }
    if (([string]$travelerMetadata.id) -cne 'travelertoolbelt' -or
            ([string]$travelerMetadata.version) -cne '1.0.2+26.2-trinkets-canary1') {
        throw "Traveler embedded identity mismatch: $($travelerMetadata.id) $($travelerMetadata.version)"
    }

    $sourceStream = $sourceEntries[0].Open()
    $sourceMemory = [IO.MemoryStream]::new()
    try {
        $sourceStream.CopyTo($sourceMemory)
        $resourceBytes = $sourceMemory.ToArray()
    }
    finally {
        $sourceMemory.Dispose()
        $sourceStream.Dispose()
    }
    $sourceTimestamp = $sourceEntries[0].LastWriteTime
}
finally {
    $travelerArchive.Dispose()
}

$resourceHash = Get-BytesHash -Bytes $resourceBytes
if ($resourceBytes.LongLength -ne $expectedResourceSize -or $resourceHash -cne $expectedResourceHash) {
    throw "Traveler radial resource identity mismatch: $($resourceBytes.LongLength) bytes $resourceHash"
}
$pngSignature = [byte[]](137, 80, 78, 71, 13, 10, 26, 10)
for ($index = 0; $index -lt $pngSignature.Length; $index++) {
    if ($resourceBytes[$index] -ne $pngSignature[$index]) {
        throw 'Traveler radial resource is not the expected PNG.'
    }
}
$width = ([int]$resourceBytes[16] -shl 24) -bor ([int]$resourceBytes[17] -shl 16) -bor
        ([int]$resourceBytes[18] -shl 8) -bor [int]$resourceBytes[19]
$height = ([int]$resourceBytes[20] -shl 24) -bor ([int]$resourceBytes[21] -shl 16) -bor
        ([int]$resourceBytes[22] -shl 8) -bor [int]$resourceBytes[23]
if ($width -ne 256 -or $height -ne 256) {
    throw "Traveler radial resource dimensions changed: ${width}x${height}"
}

if (Test-Path -LiteralPath $privateRoot) {
    Assert-ContainedPath -Candidate $privateRoot -Parent (Join-Path $projectRoot 'build\private') -Label 'Private cleanup target'
    Remove-Item -LiteralPath $privateRoot -Recurse -Force
}
New-Item -ItemType Directory -Path $privateResources -Force | Out-Null
New-Item -ItemType Directory -Path $privateOutputRoot -Force | Out-Null
$stagedResourcePath = Join-Path $privateResources ($privateEntryName.Replace('/', '\'))
New-Item -ItemType Directory -Path (Split-Path -Parent $stagedResourcePath) -Force | Out-Null
[IO.File]::WriteAllBytes($stagedResourcePath, $resourceBytes)

if (-not $GradlePath) {
    $gradleCommand = Get-Command gradle.bat -ErrorAction SilentlyContinue
    if ($gradleCommand) {
        $GradlePath = $gradleCommand.Source
    } else {
        $gradleRoot = Join-Path $env:USERPROFILE '.gradle\wrapper\dists\gradle-9.5.1-bin'
        $GradlePath = Get-ChildItem -LiteralPath $gradleRoot -Recurse -Filter gradle.bat |
                Select-Object -First 1 -ExpandProperty FullName
    }
}
if (-not $GradlePath -or -not (Test-Path -LiteralPath $GradlePath -PathType Leaf)) {
    throw 'Gradle 9.5.1 was not found; pass -GradlePath explicitly.'
}
if (-not $JavaHome) {
    $JavaHome = $env:JAVA_HOME
}
if (-not $JavaHome -or -not (Test-Path -LiteralPath (Join-Path $JavaHome 'bin\java.exe') -PathType Leaf)) {
    throw 'Java 25 was not found; pass -JavaHome explicitly.'
}

$previousJavaHome = $env:JAVA_HOME
$env:JAVA_HOME = $JavaHome
Push-Location $projectRoot
try {
    & $GradlePath clean test build --no-daemon
    if ($LASTEXITCODE -ne 0) {
        throw "Private Canary 2 source build failed with exit code $LASTEXITCODE."
    }
}
finally {
    Pop-Location
    $env:JAVA_HOME = $previousJavaHome
}

if (-not (Test-Path -LiteralPath $baseArtifactPath -PathType Leaf)) {
    throw "Private assembly base JAR was not produced: $baseArtifactPath"
}
Copy-Item -LiteralPath $baseArtifactPath -Destination $privateArtifactPath -Force

$privateArchive = [IO.Compression.ZipFile]::Open(
        $privateArtifactPath, [IO.Compression.ZipArchiveMode]::Update)
try {
    if ($privateArchive.GetEntry($privateEntryName)) {
        throw "Assembly base unexpectedly already contains protected resource: $privateEntryName"
    }
    $privateEntry = $privateArchive.CreateEntry(
            $privateEntryName, [IO.Compression.CompressionLevel]::Optimal)
    $privateEntry.LastWriteTime = $sourceTimestamp
    $privateStream = $privateEntry.Open()
    try {
        $privateStream.Write($resourceBytes, 0, $resourceBytes.Length)
    }
    finally {
        $privateStream.Dispose()
    }
}
finally {
    $privateArchive.Dispose()
}

$sourceCommit = (& git -C $repositoryRoot rev-parse HEAD).Trim()
$baseArtifact = Get-Item -LiteralPath $baseArtifactPath
$privateArtifact = Get-Item -LiteralPath $privateArtifactPath
$assembly = [ordered]@{
    schemaVersion = 1
    classification = 'GENERATED / STATICALLY VALIDATED / RUNTIME UNTESTED / PRIVATE / NOT REDISTRIBUTABLE'
    sourceCommit = $sourceCommit
    travelerInput = [ordered]@{
        path = $TravelerJarPath
        filename = $traveler.Name
        size = $traveler.Length
        sha256 = $travelerHash
        modId = 'travelertoolbelt'
        version = '1.0.2+26.2-trinkets-canary1'
    }
    resource = [ordered]@{
        sourceEntry = $sourceEntryName
        destinationEntry = $privateEntryName
        size = $resourceBytes.LongLength
        sha256 = $resourceHash
        dimensions = '256x256'
    }
    assemblyBase = [ordered]@{
        path = $baseArtifactPath
        filename = $baseArtifact.Name
        size = $baseArtifact.Length
        sha256 = (Get-FileHash -LiteralPath $baseArtifactPath -Algorithm SHA256).Hash
    }
    privateArtifact = [ordered]@{
        path = $privateArtifactPath
        filename = $privateArtifact.Name
        size = $privateArtifact.Length
        sha256 = (Get-FileHash -LiteralPath $privateArtifactPath -Algorithm SHA256).Hash
    }
}
$assembly | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $privateManifest -Encoding utf8

Write-Output "PRIVATE_CANARY2_ASSEMBLED: $($privateArtifact.Name) $($privateArtifact.Length) bytes $($assembly.privateArtifact.sha256)"
Write-Output "PRIVATE_ASSEMBLY_MANIFEST: $privateManifest"
