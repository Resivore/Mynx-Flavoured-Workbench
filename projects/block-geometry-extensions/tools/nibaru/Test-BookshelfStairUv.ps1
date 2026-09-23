param(
    [string]$ProjectRoot = (Join-Path $PSScriptRoot '..\..'),
    [string]$MinecraftClientJar,
    [string]$ResourceRoot
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem

$projectRoot = [System.IO.Path]::GetFullPath($ProjectRoot)
if (-not $MinecraftClientJar) {
    $gradleHome = if ($env:GRADLE_USER_HOME) { $env:GRADLE_USER_HOME } else { Join-Path $env:USERPROFILE '.gradle' }
    $MinecraftClientJar = Join-Path $gradleHome 'caches\fabric-loom\26.2\minecraft-client.jar'
}
if (-not $ResourceRoot) { $ResourceRoot = Join-Path $projectRoot 'build\resources\main' }
$resourceRoot = [System.IO.Path]::GetFullPath($ResourceRoot)

function Read-Json([string]$Path) {
    $full = [System.IO.Path]::GetFullPath($Path)
    return [System.IO.File]::ReadAllText("\\?\$full") | ConvertFrom-Json
}

function Read-ArchiveJson([System.IO.Compression.ZipArchive]$Archive, [string]$EntryName) {
    $entry = $Archive.GetEntry($EntryName)
    if ($null -eq $entry) { throw "Minecraft 26.2 client resource is absent: $EntryName" }
    $stream = $entry.Open()
    $reader = [System.IO.StreamReader]::new($stream)
    try { return $reader.ReadToEnd() | ConvertFrom-Json } finally { $reader.Dispose(); $stream.Dispose() }
}

function Read-ArchiveClassText([System.IO.Compression.ZipArchive]$Archive, [string]$EntryName) {
    $entry = $Archive.GetEntry($EntryName)
    if ($null -eq $entry) { throw "Minecraft 26.2 client class is absent: $EntryName" }
    $stream = $entry.Open()
    $buffer = [System.IO.MemoryStream]::new()
    try {
        $stream.CopyTo($buffer)
        return [System.Text.Encoding]::UTF8.GetString($buffer.ToArray())
    } finally { $buffer.Dispose(); $stream.Dispose() }
}

function Assert-SameProperty($Actual, $Expected, [string]$Property, [string]$State) {
    $actualValue = $Actual.PSObject.Properties[$Property]
    $expectedValue = $Expected.PSObject.Properties[$Property]
    if (($null -eq $actualValue) -ne ($null -eq $expectedValue) -or
        ($null -ne $actualValue -and $actualValue.Value -ne $expectedValue.Value)) {
        throw "Bookshelf Stair $State has a non-vanilla $Property transform or UV lock"
    }
}

$namespace = 'more_slabs_stairs_and_walls'
$assetRoot = Join-Path $resourceRoot "assets\$namespace"
$actual = Read-Json (Join-Path $assetRoot 'blockstates\bookshelf_stairs.json')
$archive = [System.IO.Compression.ZipFile]::OpenRead([System.IO.Path]::GetFullPath($MinecraftClientJar))
try {
    # In the actual 26.2 baker, UV-locked rotations supply the inverse face
    # transform, and FaceBakery applies it to each face's UV coordinates. This
    # distinguishes UV lock from rotating the physical Stair model alone.
    $faceBakery = Read-ArchiveClassText $archive 'net/minecraft/client/resources/model/cuboid/FaceBakery.class'
    $lockedRotation = Read-ArchiveClassText $archive 'net/minecraft/client/renderer/block/dispatch/BlockModelRotation$WithUvLock.class'
    $rotation = Read-ArchiveClassText $archive 'net/minecraft/client/renderer/block/dispatch/BlockModelRotation.class'
    if (-not $faceBakery.Contains('inverseFaceTransformation') -or
        -not $lockedRotation.Contains('inverseFaceMapping') -or
        -not $rotation.Contains('invertAffine') -or
        -not $rotation.Contains('getFaceTransformation')) {
        throw 'Minecraft 26.2 UV lock no longer uses the inverse face transformation in its model baker'
    }

    $reference = Read-ArchiveJson $archive 'assets/minecraft/blockstates/oak_stairs.json'
    $actualKeys = @($actual.variants.PSObject.Properties.Name)
    $referenceKeys = @($reference.variants.PSObject.Properties.Name)
    if ($actualKeys.Count -ne 40 -or $referenceKeys.Count -ne 40) {
        throw "Expected exactly 40 Bookshelf and vanilla Stair states; found $($actualKeys.Count) and $($referenceKeys.Count)"
    }

    $checked = 0
    $locked = 0
    foreach ($facing in @('north', 'east', 'south', 'west')) {
        foreach ($half in @('bottom', 'top')) {
            foreach ($shape in @('straight', 'inner_left', 'inner_right', 'outer_left', 'outer_right')) {
                $state = "facing=$facing,half=$half,shape=$shape"
                $actualVariant = $actual.variants.PSObject.Properties[$state]
                $referenceVariant = $reference.variants.PSObject.Properties[$state]
                if ($null -eq $actualVariant -or $null -eq $referenceVariant) {
                    throw "Missing Bookshelf or vanilla Stair state: $state"
                }
                $actualVariant = $actualVariant.Value
                $referenceVariant = $referenceVariant.Value
                $expectedModel = $referenceVariant.model.Replace('minecraft:block/oak_stairs',
                    "$namespace`:block/bookshelf_stairs")
                if ($actualVariant.model -cne $expectedModel) {
                    throw "Bookshelf Stair $state changed its straight/inner/outer physical model"
                }
                foreach ($property in @('x', 'y', 'uvlock')) {
                    Assert-SameProperty $actualVariant $referenceVariant $property $state
                }
                if ($actualVariant.PSObject.Properties.Name -contains 'uvlock') { $locked++ }
                $checked++
            }
        }
    }
    if ($checked -ne 40 -or $locked -ne 35) {
        throw "Bookshelf Stair UV lock matrix is incomplete: checked=$checked locked=$locked"
    }

    foreach ($shape in @('', '_inner', '_outer')) {
        $model = Read-Json (Join-Path $assetRoot "models\block\bookshelf_stairs$shape.json")
        $vanillaTemplate = switch ($shape) {
            '' { 'stairs' }
            '_inner' { 'inner_stairs' }
            '_outer' { 'outer_stairs' }
        }
        $referenceModel = Read-ArchiveJson $archive "assets/minecraft/models/block/$vanillaTemplate.json"
        if ($model.parent -cne "minecraft:block/$vanillaTemplate" -or
            $model.PSObject.Properties.Name -contains 'elements' -or
            $referenceModel.elements.Count -lt 2 -or
            $model.textures.side -cne 'minecraft:block/bookshelf' -or
            $model.textures.top -cne 'minecraft:block/oak_planks' -or
            $model.textures.bottom -cne 'minecraft:block/oak_planks') {
            throw "Bookshelf Stair $shape changed its vanilla geometry, Bookshelf side, or fallback wood textures"
        }
        $woodFaces = 0
        $sideFaces = 0
        foreach ($element in $referenceModel.elements) {
            foreach ($face in $element.faces.PSObject.Properties) {
                $expectedRole = switch ($face.Name) {
                    'up' { '#top' }
                    'down' { '#bottom' }
                    default { '#side' }
                }
                if ($face.Value.texture -cne $expectedRole -or
                    $face.Value.PSObject.Properties.Name -contains 'rotation') {
                    throw "Minecraft 26.2 $vanillaTemplate changed its horizontal/side UV face contract"
                }
                if ($face.Name -eq 'up' -or $face.Name -eq 'down') { $woodFaces++ }
                else { $sideFaces++ }
            }
        }
        if ($woodFaces -eq 0 -or $sideFaces -eq 0) {
            throw "Minecraft 26.2 $vanillaTemplate is missing wood or side faces"
        }
    }
} finally {
    $archive.Dispose()
}

'PASS: 40 Bookshelf Stair states match vanilla physical rotations and UV lock; 26.2 baker uses inverse face UV transforms; three models retain wood/side face roles'
