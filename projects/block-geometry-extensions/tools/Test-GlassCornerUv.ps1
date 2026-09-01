param(
    [string]$ProjectRoot = (Split-Path -Parent $PSScriptRoot)
)

$ErrorActionPreference = 'Stop'

function Require([bool]$Condition, [string]$Message) {
    if (-not $Condition) { throw $Message }
}

function Sha256Text([string]$Text) {
    $bytes = [System.Text.UTF8Encoding]::new($false).GetBytes($Text)
    $sha = [System.Security.Cryptography.SHA256]::Create()
    try { return (($sha.ComputeHash($bytes) | ForEach-Object { $_.ToString('x2') }) -join '') }
    finally { $sha.Dispose() }
}

$fixturePath = Join-Path $ProjectRoot 'build-inputs\glass-corner-uv\glass_corner_north_east.sanitized.bbmodel'
$readmePath = Join-Path $ProjectRoot 'build-inputs\glass-corner-uv\README.md'
$fixtureHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $fixturePath).Hash.ToLowerInvariant()
$raw = Get-Content -Raw -LiteralPath $fixturePath
$model = $raw | ConvertFrom-Json
$readme = Get-Content -Raw -LiteralPath $readmePath

$checks = [ordered]@{}
$checks['sanitized fixture has the exact audited repository hash'] =
        $fixtureHash -eq '0de17962f9d569a95a1fe3004657af87ce9be7bcb3cc495f2b9f98fc6c6753b5'
$checks['embedded and sibling PNG bytes are absent'] =
        $raw -notmatch 'data:image|iVBORw0KGgo' -and
        @(Get-ChildItem -LiteralPath (Split-Path -Parent $fixturePath) -File -Filter '*.png').Count -eq 0
$checks['Blockbench and Java model metadata are exact'] =
        $model.meta.format_version -eq '5.0' -and $model.meta.model_format -eq 'java_block' -and
        $model.meta.box_uv -eq $false -and $model.java_block_version -eq '1.21.11' -and
        $model.name -eq 'glass_corner_north_east' -and $model.resolution.width -eq 16 -and
        $model.resolution.height -eq 16 -and $model.ambientocclusion -eq $true
$checks['texture descriptor is retained without source bytes'] =
        $model.textures.Count -eq 1 -and $model.textures[0].id -eq '0' -and
        $model.textures[0].name -eq 'BlockSprite_glass.png' -and
        $model.textures[0].width -eq 16 -and $model.textures[0].height -eq 16 -and
        $model.textures[0].uv_width -eq 16 -and $model.textures[0].uv_height -eq 16 -and
        -not ($model.textures[0].PSObject.Properties.Name -contains 'source')
$checks['original archive and member identities are pinned in provenance'] =
        $readme -match '259a87b3459b5ce6e797a93e117fa4ba4526ca6002baa6f58db15e9da79ba0e3' -and
        $readme -match 'ed92231ff056e0e2f7cd278381e80bf0131ba62ce27e74bd1874625834a48e54' -and
        $readme -match '0be697e533f7de0ddbb27eef37f13166d7da3f75a47d3a74839d4753b8b05c07' -and
        $readme -match 'be65b86e763dec985c900ebd23115fd5a07b78a0b882228589abdab1799d3743'

$expectedBounds = @(
    @(0, 0, 0, 16, 16, 7),
    @(0, 0, 7, 7, 16, 8),
    @(9, 0, 7, 16, 16, 8),
    @(7, 0, 7, 8, 16, 8),
    @(9, 0, 8, 16, 16, 16),
    @(8, 0, 7, 9, 16, 8),
    @(8, 0, 9, 9, 16, 16),
    @(8, 0, 8, 9, 16, 9)
)
$exactBounds = $model.elements.Count -eq $expectedBounds.Count
for ($index = 0; $exactBounds -and $index -lt $expectedBounds.Count; $index++) {
    $actual = @($model.elements[$index].from) + @($model.elements[$index].to)
    $exactBounds = [string]::Join(',', $actual) -eq [string]::Join(',', $expectedBounds[$index])
}
$checks['all eight authored element bounds and ordering are exact'] = $exactBounds

$directions = @('north', 'east', 'south', 'west', 'up', 'down')
$allFaces = [System.Collections.Generic.List[string]]::new()
$visibleFaces = [System.Collections.Generic.List[string]]::new()
for ($index = 0; $index -lt $model.elements.Count; $index++) {
    foreach ($direction in $directions) {
        $face = $model.elements[$index].faces.$direction
        Require ($null -ne $face) "Element $($index + 1) is missing source face slot $direction"
        $uv = [string]::Join(',', @($face.uv))
        $rotation = if ($face.PSObject.Properties.Name -contains 'rotation') { [int]$face.rotation } else { 0 }
        $texture = if ($null -eq $face.texture) { 'null' } else { [string]$face.texture }
        $cullface = if ($face.PSObject.Properties.Name -contains 'cullface') { [string]$face.cullface } else { '-' }
        $allFaces.Add(('{0}|{1}|{2}|{3}|{4}|{5}' -f ($index + 1), $direction,
                    $uv, $rotation, $texture, $cullface))
        if ($null -ne $face.texture) {
            $visibleFaces.Add(('{0}|{1}|{2}|{3}|{4}' -f ($index + 1), $direction,
                        $uv, $rotation, $texture))
        }
    }
}
$checks['all 48 source face slots including raw cullfaces are exact'] =
        $allFaces.Count -eq 48 -and
        (Sha256Text ([string]::Join("`n", $allFaces))) -eq
            'c3a2a9106675a93fe3ee119d66608f17f0197e8572ba6bc919b24850e5865512'
$checks['all 28 visible faces, UV rectangles, texture refs, and rotations are exact'] =
        $visibleFaces.Count -eq 28 -and
        (Sha256Text ([string]::Join("`n", $visibleFaces))) -eq
            '88e48cd54304eabfe09f78ccf6146bfc8d163bb4c302ae547c2698315f3503fa'

$occupied = [System.Collections.Generic.HashSet[string]]::new()
$overlap = $false
foreach ($element in $model.elements) {
    $from = @($element.from)
    $to = @($element.to)
    for ($x = $from[0]; $x -lt $to[0]; $x++) {
        for ($y = $from[1]; $y -lt $to[1]; $y++) {
            for ($z = $from[2]; $z -lt $to[2]; $z++) {
                if (-not $occupied.Add("$x,$y,$z")) { $overlap = $true }
            }
        }
    }
}
$expectedOccupied = [System.Collections.Generic.HashSet[string]]::new()
for ($x = 0; $x -lt 16; $x++) {
    for ($y = 0; $y -lt 16; $y++) {
        for ($z = 0; $z -lt 16; $z++) {
            if ($z -lt 8 -or $x -ge 8) { [void]$expectedOccupied.Add("$x,$y,$z") }
        }
    }
}
$checks['occupied union is the exact nonoverlapping NORTH_EAST full-height L'] =
        -not $overlap -and $occupied.Count -eq 3072 -and
        $occupied.SetEquals($expectedOccupied)
$checks['all authored geometry and UV coordinates stay inside the 0-16 frame'] =
        @($model.elements | Where-Object {
            @($_.from + $_.to) | Where-Object { $_ -lt 0 -or $_ -gt 16 }
        }).Count -eq 0 -and
        @($model.elements | ForEach-Object { $_.faces.PSObject.Properties.Value } |
            ForEach-Object { $_.uv } | Where-Object { $_ -lt 0 -or $_ -gt 16 }).Count -eq 0
$checks['raw cullface discrepancy remains explicit and preserved only as provenance'] =
        $model.elements[0].faces.north.cullface -eq 'west' -and
        $model.elements[0].faces.east.cullface -eq 'west' -and
        $model.elements[0].faces.up.cullface -eq 'west' -and
        $model.elements[0].faces.down.cullface -eq 'west' -and
        $model.elements[3].faces.south.cullface -eq 'south' -and
        $readme -match 'not valid as production boundary metadata'

$failed = @($checks.GetEnumerator() | Where-Object { -not $_.Value })
$checks.GetEnumerator() | ForEach-Object {
    '{0}: {1}' -f ($(if ($_.Value) { 'PASS' } else { 'FAIL' })), $_.Key
}
if ($failed.Count -gt 0) {
    throw "Glass Corner UV source contract failed $($failed.Count) check(s)"
}

[pscustomobject]@{
    checks = $checks.Count
    elements = $model.elements.Count
    face_slots = $allFaces.Count
    visible_faces = $visibleFaces.Count
    occupied_volume = $occupied.Count
    fixture_sha256 = $fixtureHash
    result = 'PASS'
} | ConvertTo-Json -Compress
