[CmdletBinding()]
param(
    [string]$WorkbenchRoot
)

$ErrorActionPreference = 'Stop'
if ([string]::IsNullOrWhiteSpace($WorkbenchRoot)) {
    $WorkbenchRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..\..')).Path
}
$WorkbenchRoot = (Resolve-Path -LiteralPath $WorkbenchRoot).Path

$contracts = @(
    @{
        Name = 'StrippingToggle'
        Path = Join-Path $WorkbenchRoot 'originals\mods\strippingtoggle-1.2.6+26.2.jar'
        Hash = '0A44DB1BB183B3296461AD10C4FB8179C7A345F5CCE54B30E291F5355282857D'
        ModId = 'strippingtoggle'
        Version = '1.2.6+26.2'
        Environment = 'client'
        Entries = @(
            'yungando/strippingtoggle/StrippingToggle.class',
            'yungando/strippingtoggle/mixin/MultiPlayerGameModeMixin.class',
            'strippingtoggle.mixins.json'
        )
        ClassEntry = 'yungando/strippingtoggle/StrippingToggle.class'
        ClassTokens = @('strippingEnabled', 'toggleStripping')
    },
    @{
        Name = 'FallingTree'
        Path = Join-Path $WorkbenchRoot 'originals\mods\FallingTree-26.2-25.jar'
        Hash = 'ABE61A2708F9BA00B063D918BE63292E69C62B7743E2FE64FC63CE8877F1C5F2'
        ModId = 'fallingtree'
        Version = '25'
        Environment = '*'
        Entries = @(
            'fr/rakambda/fallingtree/common/FallingTreeCommon.class',
            'fr/rakambda/fallingtree/common/command/ToggleCommand.class',
            'fr/rakambda/fallingtree/common/config/enums/SneakMode.class',
            'fr/rakambda/fallingtree/common/wrapper/IPlayer.class'
        )
        ClassEntry = 'fr/rakambda/fallingtree/common/FallingTreeCommon.class'
        ClassTokens = @('isPlayerInRightState', 'playerHasToggledOff', 'fallingtree-disabled')
    }
)

Add-Type -AssemblyName System.IO.Compression.FileSystem
foreach ($contract in $contracts) {
    if (-not (Test-Path -LiteralPath $contract.Path -PathType Leaf)) {
        throw "$($contract.Name) reference is missing: $($contract.Path)"
    }
    $hash = (Get-FileHash -LiteralPath $contract.Path -Algorithm SHA256).Hash
    if ($hash -cne $contract.Hash) { throw "$($contract.Name) SHA-256 mismatch: $hash" }

    $archive = [IO.Compression.ZipFile]::OpenRead($contract.Path)
    try {
        $metadataEntry = $archive.GetEntry('fabric.mod.json')
        if (-not $metadataEntry) { throw "$($contract.Name) lacks fabric.mod.json." }
        $reader = [IO.StreamReader]::new($metadataEntry.Open())
        try { $metadata = $reader.ReadToEnd() | ConvertFrom-Json } finally { $reader.Dispose() }
        if ($metadata.id -cne $contract.ModId -or $metadata.version -cne $contract.Version) {
            throw "$($contract.Name) identity mismatch: $($metadata.id) $($metadata.version)"
        }
        if ($metadata.environment -cne $contract.Environment) {
            throw "$($contract.Name) environment mismatch: $($metadata.environment)"
        }
        foreach ($required in $contract.Entries) {
            if (-not $archive.GetEntry($required)) { throw "$($contract.Name) entry missing: $required" }
        }

        $classEntry = $archive.GetEntry($contract.ClassEntry)
        $stream = $classEntry.Open()
        try {
            $memory = [IO.MemoryStream]::new()
            try {
                $stream.CopyTo($memory)
                $classText = [Text.Encoding]::GetEncoding(28591).GetString($memory.ToArray())
            } finally { $memory.Dispose() }
        } finally { $stream.Dispose() }
        foreach ($token in $contract.ClassTokens) {
            if (-not $classText.Contains($token)) { throw "$($contract.Name) seam token missing: $token" }
        }
    } finally {
        $archive.Dispose()
    }

    Write-Output "UPSTREAM_CONTRACT_OK: $($contract.Name) $($contract.ModId) $($contract.Version) $hash"
}
