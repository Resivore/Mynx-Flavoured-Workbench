[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string] $TravelerJarPath
)

$ErrorActionPreference = 'Stop'

if ($PSVersionTable.PSVersion.Major -lt 7) {
    throw 'Private Canary 2 verification requires PowerShell 7 or newer; run this script with pwsh.'
}

$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..\..')).Path
$projectRoot = Join-Path $repositoryRoot 'projects\radial-slot-cycler'
$statusPath = Join-Path $projectRoot 'WORKBENCH_STATUS.json'
$privateEntryName = 'assets/radial_slot_cycler/textures/gui/belt_overlay.png'
$travelerEntryName = 'assets/travelertoolbelt/textures/gui/belt_overlay.png'
$expectedTravelerName = 'travelertoolbelt-fabric-26.2-1.0.2-TRINKETS-CANARY1.jar'
$expectedTravelerSize = 308008L
$expectedTravelerHash = '43D3370135F42EF74129B904325006F91BA32074AB8DBD81A649170D1366FCD6'
$expectedResourceHash = 'C73B3634EE3FE16FC7DC00CED67EF833DE11E8B650BEEF3095B2F5900DC81910'
$expectedBaseName = 'radial-slot-cycler-private-base-0.1.0-canary2-private.jar'
$expectedBaseSize = 32223L
$expectedBaseHash = 'A39B1DA1E29168F2EE0ABE7267D7E84A20CB08669B069D7209C5518A2325F58A'
$expectedPrivateName = 'radial-slot-cycler-0.1.0-canary2-private.jar'
$expectedPrivateSize = 36436L
$expectedPrivateHash = '3FE7E539EAAFDA97AB5A5A301544B25563F896FEE354B729607074F404BDBC21'
$expectedCanary1Hash = '7393321A6AC7FF1D16B918075C47A26BBC90005B3A784F4D2B661F93CCA4F2E4'

if (-not (Test-Path -LiteralPath $statusPath -PathType Leaf)) {
    throw "Canonical Workbench status is missing: $statusPath"
}
$status = Get-Content -Raw -LiteralPath $statusPath | ConvertFrom-Json
$current = $status.state.releases.current
$accepted = $status.state.releases.accepted
$rollback = $status.state.releases.rollback
if (([string]$current.artifact.filename) -cne $expectedPrivateName -or
        ([string]$current.artifact.sha256).ToUpperInvariant() -cne $expectedPrivateHash -or
        ([string]$accepted.artifact.filename) -cne $expectedPrivateName -or
        ([string]$accepted.artifact.sha256).ToUpperInvariant() -cne $expectedPrivateHash) {
    throw 'Canonical current/accepted Private Canary 2 identity changed.'
}
if (([string]$rollback.artifact.filename) -cne 'radial-slot-cycler-0.1.0-canary1.jar' -or
        ([string]$rollback.artifact.sha256).ToUpperInvariant() -cne $expectedCanary1Hash) {
    throw 'Canonical clean Canary 1 rollback identity changed.'
}

$privateRoot = Join-Path $projectRoot 'build\private\canary2'
$privateArtifactPath = Join-Path (Join-Path $privateRoot 'artifact') $expectedPrivateName
$baseArtifactPath = Join-Path $projectRoot "build\libs\$expectedBaseName"
$canary1Path = Join-Path $projectRoot 'artifacts\radial-slot-cycler-0.1.0-canary1.jar'

if (-not (Test-Path -LiteralPath $TravelerJarPath -PathType Leaf)) {
    throw "Exact user-owned Traveler Tool Belt input is missing: $TravelerJarPath"
}
$TravelerJarPath = (Resolve-Path -LiteralPath $TravelerJarPath).Path
$traveler = Get-Item -LiteralPath $TravelerJarPath
if ($traveler.Name -cne $expectedTravelerName -or $traveler.Length -ne $expectedTravelerSize) {
    throw "Traveler input identity mismatch: $($traveler.Name) $($traveler.Length) bytes"
}
if ((Get-FileHash -LiteralPath $TravelerJarPath -Algorithm SHA256).Hash -cne $expectedTravelerHash) {
    throw 'Accepted Traveler Tool Belt input hash changed.'
}

foreach ($requiredPath in @($privateArtifactPath, $baseArtifactPath, $canary1Path)) {
    if (-not (Test-Path -LiteralPath $requiredPath -PathType Leaf)) {
        throw "Required private verification input is missing: $requiredPath"
    }
}

$baseArtifact = Get-Item -LiteralPath $baseArtifactPath
if ($baseArtifact.Name -cne $expectedBaseName -or $baseArtifact.Length -ne $expectedBaseSize -or
        (Get-FileHash -LiteralPath $baseArtifactPath -Algorithm SHA256).Hash -cne $expectedBaseHash) {
    throw 'Resource-clean Private Canary 2 base identity changed.'
}

$privateArtifact = Get-Item -LiteralPath $privateArtifactPath
if ($privateArtifact.Name -cne $expectedPrivateName -or $privateArtifact.Length -ne $expectedPrivateSize) {
    throw "Private Canary 2 identity mismatch: $($privateArtifact.Name) $($privateArtifact.Length) bytes"
}
$privateHash = (Get-FileHash -LiteralPath $privateArtifactPath -Algorithm SHA256).Hash
if ($privateHash -cne $expectedPrivateHash) {
    throw "Private Canary 2 SHA-256 mismatch: $privateHash"
}

Add-Type -AssemblyName System.IO.Compression.FileSystem
function Get-ArchiveEntries {
    param([Parameter(Mandatory = $true)][string] $Path)

    $result = @{}
    $archive = [IO.Compression.ZipFile]::OpenRead($Path)
    try {
        foreach ($entry in $archive.Entries) {
            if ($entry.FullName.EndsWith('/')) { continue }
            if ($result.ContainsKey($entry.FullName)) {
                throw "Duplicate JAR entry: $($entry.FullName)"
            }
            $stream = $entry.Open()
            $memory = [IO.MemoryStream]::new()
            try {
                $stream.CopyTo($memory)
                $bytes = $memory.ToArray()
            }
            finally {
                $memory.Dispose()
                $stream.Dispose()
            }
            $digest = [Security.Cryptography.SHA256]::Create()
            try {
                $hash = ([BitConverter]::ToString($digest.ComputeHash($bytes))).Replace('-', '')
            }
            finally {
                $digest.Dispose()
            }
            $result[$entry.FullName] = [ordered]@{
                length = $entry.Length
                hash = $hash
                bytes = $bytes
            }
        }
    }
    finally {
        $archive.Dispose()
    }
    return $result
}

$travelerEntries = Get-ArchiveEntries -Path $TravelerJarPath
if (-not $travelerEntries.ContainsKey($travelerEntryName) -or
        $travelerEntries[$travelerEntryName].length -ne 4580L -or
        $travelerEntries[$travelerEntryName].hash -cne $expectedResourceHash) {
    throw 'Traveler Tool Belt radial resource identity changed.'
}
if (-not $travelerEntries.ContainsKey('fabric.mod.json')) {
    throw 'Traveler Tool Belt metadata is missing.'
}
$travelerMetadata = [Text.Encoding]::UTF8.GetString($travelerEntries['fabric.mod.json'].bytes) |
        ConvertFrom-Json
if (([string]$travelerMetadata.id) -cne 'travelertoolbelt' -or
        ([string]$travelerMetadata.version) -cne '1.0.2+26.2-trinkets-canary1') {
    throw "Traveler embedded identity mismatch: $($travelerMetadata.id) $($travelerMetadata.version)"
}

$baseEntries = Get-ArchiveEntries -Path $baseArtifactPath
$privateEntries = Get-ArchiveEntries -Path $privateArtifactPath
$addedEntries = @($privateEntries.Keys | Where-Object { -not $baseEntries.ContainsKey($_) })
$removedEntries = @($baseEntries.Keys | Where-Object { -not $privateEntries.ContainsKey($_) })
if ($addedEntries.Count -ne 1 -or $addedEntries[0] -cne $privateEntryName -or
        $removedEntries.Count -ne 0) {
    throw "Private entry-set delta is not the one allowed resource: added=[$($addedEntries -join ',')] removed=[$($removedEntries -join ',')]"
}
foreach ($name in $baseEntries.Keys) {
    if ($baseEntries[$name].length -ne $privateEntries[$name].length -or
            $baseEntries[$name].hash -cne $privateEntries[$name].hash) {
        throw "Private assembly changed base entry: $name"
    }
}
if ($privateEntries[$privateEntryName].length -ne 4580L -or
        $privateEntries[$privateEntryName].hash -cne $expectedResourceHash) {
    throw 'Private radial resource identity mismatch.'
}

$forbiddenEntries = @($privateEntries.Keys | Where-Object {
    $_.StartsWith('com/tiviacz/', [StringComparison]::Ordinal) -or
    $_.StartsWith('assets/travelertoolbelt/', [StringComparison]::Ordinal) -or
    $_.EndsWith('.jar', [StringComparison]::OrdinalIgnoreCase)
})
if ($forbiddenEntries.Count -ne 0) {
    throw "Private JAR contains forbidden Traveler code/namespace/nested JAR entries: $($forbiddenEntries -join ', ')"
}
$foreignClasses = @($privateEntries.Keys | Where-Object {
    $_.EndsWith('.class', [StringComparison]::OrdinalIgnoreCase) -and
    -not $_.StartsWith('dev/resivore/radialslotcycler/', [StringComparison]::Ordinal)
})
if ($foreignClasses.Count -ne 0) {
    throw "Private JAR contains foreign classes: $($foreignClasses -join ', ')"
}

$canary1Entries = Get-ArchiveEntries -Path $canary1Path
$behaviorEntries = @(
    'dev/resivore/radialslotcycler/RadialSlotCycler.class',
    'dev/resivore/radialslotcycler/client/RadialClientConfig.class',
    'dev/resivore/radialslotcycler/client/RadialInteractionController.class',
    'dev/resivore/radialslotcycler/client/RadialInteractionController$Action.class',
    'dev/resivore/radialslotcycler/client/RadialInteractionController$Mode.class',
    'dev/resivore/radialslotcycler/client/RadialSelection.class',
    'dev/resivore/radialslotcycler/core/ColumnLayout.class',
    'dev/resivore/radialslotcycler/core/ExactPairwiseSwap.class',
    'dev/resivore/radialslotcycler/core/OrdinaryInventorySnapshot.class',
    'dev/resivore/radialslotcycler/core/SwapRequestValidator.class',
    'dev/resivore/radialslotcycler/core/SwapRequestValidator$Result.class',
    'dev/resivore/radialslotcycler/network/SwapSlotPayload.class'
)
foreach ($entry in $behaviorEntries) {
    if (-not $canary1Entries.ContainsKey($entry) -or -not $privateEntries.ContainsKey($entry) -or
            $canary1Entries[$entry].hash -cne $privateEntries[$entry].hash) {
        throw "Canary 1 behavior class changed in private Canary 2: $entry"
    }
}

if (-not $privateEntries.ContainsKey('fabric.mod.json')) {
    throw 'Private Canary 2 metadata is missing.'
}
$metadata = [Text.Encoding]::UTF8.GetString($privateEntries['fabric.mod.json'].bytes) |
        ConvertFrom-Json
if (([string]$metadata.id) -cne 'radial_slot_cycler' -or
        ([string]$metadata.version) -cne '0.1.0-canary2-private') {
    throw "Private Canary 2 embedded identity mismatch: $($metadata.id) $($metadata.version)"
}
if (([string]$metadata.custom.'workbench:classification') -cne
        'GENERATED / STATICALLY VALIDATED / RUNTIME UNTESTED / PRIVATE / NOT REDISTRIBUTABLE') {
    throw 'Private Canary 2 embedded classification mismatch.'
}
if (([string]$metadata.custom.'workbench:deployment') -cne 'NOT DEPLOYED') {
    throw 'Private Canary 2 embedded deployment provenance must remain NOT DEPLOYED.'
}

$relativePrivate = [IO.Path]::GetRelativePath($repositoryRoot, $privateArtifactPath).Replace('\', '/')
& git -C $repositoryRoot check-ignore -q -- $relativePrivate
if ($LASTEXITCODE -ne 0) {
    throw "Private Canary 2 path is not ignored: $relativePrivate"
}
$trackedPrivate = @(& git -C $repositoryRoot ls-files -- $relativePrivate)
if ($trackedPrivate.Count -ne 0) {
    throw "Private Canary 2 is tracked by Git: $relativePrivate"
}

Write-Output "RADIAL_PRIVATE_CANARY2_OK: $($privateArtifact.Name) $($privateArtifact.Length) bytes $privateHash"
