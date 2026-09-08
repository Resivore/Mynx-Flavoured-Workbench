param(
    [string]$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path,
    [string]$PristineJar = (Join-Path $ProjectRoot 'build-inputs\nibaru\moreslabsstairsandwalls-4.2.0.jar'),
    [string]$OutputRoot = (Join-Path $ProjectRoot 'build\generated\nibaru-resources')
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$namespace = 'more_slabs_stairs_and_walls'
$projectRoot = [System.IO.Path]::GetFullPath($ProjectRoot)
$outputRoot = [System.IO.Path]::GetFullPath($OutputRoot)
$staticRoots = @(
    (Join-Path $projectRoot 'src\nibaru\resources'),
    (Join-Path $projectRoot 'src\main\resources')
)
$resolvedOutput = [System.IO.Path]::GetFullPath($outputRoot)
$expectedOutput = [System.IO.Path]::GetFullPath((Join-Path $projectRoot 'build\generated\nibaru-resources'))

if ($resolvedOutput -ne $expectedOutput) {
    throw "Refusing to replace unexpected generated-resource path: $resolvedOutput"
}
if (-not (Test-Path -LiteralPath $PristineJar -PathType Leaf)) {
    throw "Pristine upstream JAR not found: $PristineJar"
}
$expectedPristineSha256 = '50CEB45DCE67C528ED65E12810DF95DF2F346743EA07C3498FE02F7E49FF7467'
$sha256 = [System.Security.Cryptography.SHA256]::Create()
$pristineStream = [System.IO.File]::OpenRead([System.IO.Path]::GetFullPath($PristineJar))
try {
    $actualPristineSha256 = [System.BitConverter]::ToString($sha256.ComputeHash($pristineStream)).Replace('-', '')
} finally {
    $pristineStream.Dispose()
    $sha256.Dispose()
}
if ($actualPristineSha256 -ne $expectedPristineSha256) {
    throw "Pristine upstream JAR hash mismatch: expected $expectedPristineSha256; found $actualPristineSha256"
}

if (Test-Path -LiteralPath $resolvedOutput) {
    [System.IO.Directory]::Delete("\\?\$resolvedOutput", $true)
}
New-Item -ItemType Directory -Path $resolvedOutput | Out-Null

function Test-StaticOverride([string]$RelativePath) {
    foreach ($root in $staticRoots) {
        $candidate = [System.IO.Path]::GetFullPath((Join-Path $root $RelativePath))
        if ([System.IO.File]::Exists("\\?\$candidate")) { return $true }
    }
    return $false
}

$archive = [System.IO.Compression.ZipFile]::OpenRead((Resolve-Path -LiteralPath $PristineJar))
try {
    foreach ($entry in $archive.Entries) {
        $name = $entry.FullName.Replace('/', '\')
        if ($entry.Length -eq 0) { continue }
        $isModResource = $name -like "assets\$namespace\*" -or $name -like "data\$namespace\*"
        $isMinecraftBlockTag = $name -like 'data\minecraft\tags\block\*'
        if (-not $isModResource -and -not $isMinecraftBlockTag) { continue }
        # The Workbench permanently pairs Nibaru with CNM. CNM owns geometry
        # selection and geometry-aware recipe equivalence, so Nibaru must not
        # import either the current or inert legacy recipe layers.
        $isProviderRecipe = $name -like "data\$namespace\recipe\*" -or
            $name -like "data\$namespace\recipes\*"
        $isProviderRecipeAdvancement = $name -like "data\$namespace\advancement\recipes\*" -or
            $name -like "data\$namespace\advancements\recipes*"
        if ($isProviderRecipe -or $isProviderRecipeAdvancement) { continue }
        if (Test-StaticOverride $name) { continue }

        $target = Join-Path $resolvedOutput $name
        [System.IO.Directory]::CreateDirectory("\\?\$(Split-Path -Parent $target)") | Out-Null
        $entryStream = $entry.Open()
        $fileStream = [System.IO.File]::Create("\\?\$target")
        try { $entryStream.CopyTo($fileStream) } finally { $fileStream.Dispose(); $entryStream.Dispose() }
    }
} finally {
    $archive.Dispose()
}

# Minecraft 26.2 added these canonical materials after the pristine upstream catalog.
# Clone only the proven counterpart resource structures; retain the target materials'
# own identifiers and canonical vanilla texture names.
function Copy-CounterpartFiles {
    param(
        [string]$Source,
        [string]$Target,
        [string]$ContentSource = $Source,
        [string]$ContentTarget = $Target,
        [string]$AdditionalContentSource,
        [string]$AdditionalContentTarget
    )

    if ([string]::IsNullOrEmpty($AdditionalContentSource) -ne [string]::IsNullOrEmpty($AdditionalContentTarget)) {
        throw 'Additional counterpart content replacements require both source and target identifiers.'
    }

    $sourceFiles = @(Get-ChildItem -LiteralPath $resolvedOutput -Recurse -File | Where-Object {
        $_.BaseName -eq $Source -or $_.BaseName.StartsWith("${Source}_")
    })
    foreach ($sourceFile in $sourceFiles) {
        $relative = $sourceFile.FullName.Substring($resolvedOutput.Length).TrimStart('\')
        $targetRelative = $relative.Replace($Source, $Target)
        $targetPath = Join-Path $resolvedOutput $targetRelative
        [System.IO.Directory]::CreateDirectory("\\?\$(Split-Path -Parent $targetPath)") | Out-Null
        $raw = [System.IO.File]::ReadAllText("\\?\$($sourceFile.FullName)")
        $updated = $raw.Replace($Source, $Target)
        if ($ContentSource -ne $Source -or $ContentTarget -ne $Target) {
            $updated = $updated.Replace($ContentSource, $ContentTarget)
        }
        if (-not [string]::IsNullOrEmpty($AdditionalContentSource)) {
            $updated = $updated.Replace($AdditionalContentSource, $AdditionalContentTarget)
        }
        [System.IO.File]::WriteAllText(
            "\\?\$targetPath",
            $updated,
            [System.Text.UTF8Encoding]::new($false))
    }
}

$paleCounterparts = @(
    @{ Source = 'stripped_dark_oak_log'; Target = 'stripped_pale_oak_log'; AdditionalContentSource = 'minecraft:dark_oak_planks'; AdditionalContentTarget = 'minecraft:pale_oak_planks' },
    @{ Source = 'stripped_dark_oak_wood'; Target = 'stripped_pale_oak_wood'; ContentSource = 'stripped_dark_oak_log'; ContentTarget = 'stripped_pale_oak_log'; AdditionalContentSource = 'minecraft:dark_oak_planks'; AdditionalContentTarget = 'minecraft:pale_oak_planks' },
    @{ Source = 'dark_oak_log'; Target = 'pale_oak_log'; AdditionalContentSource = 'minecraft:dark_oak_planks'; AdditionalContentTarget = 'minecraft:pale_oak_planks' },
    @{ Source = 'dark_oak_wood'; Target = 'pale_oak_wood'; ContentSource = 'dark_oak_log'; ContentTarget = 'pale_oak_log'; AdditionalContentSource = 'minecraft:dark_oak_planks'; AdditionalContentTarget = 'minecraft:pale_oak_planks' },
    @{ Source = 'dark_oak_leaves'; Target = 'pale_oak_leaves' },
    @{ Source = 'dark_oak_planks'; Target = 'pale_oak_planks' },
    @{ Source = 'moss_block'; Target = 'pale_moss_block' }
)
foreach ($mapping in $paleCounterparts) {
    $contentSource = if ($mapping.ContainsKey('ContentSource')) { $mapping.ContentSource } else { $mapping.Source }
    $contentTarget = if ($mapping.ContainsKey('ContentTarget')) { $mapping.ContentTarget } else { $mapping.Target }
    $additionalContentSource = if ($mapping.ContainsKey('AdditionalContentSource')) { $mapping.AdditionalContentSource } else { $null }
    $additionalContentTarget = if ($mapping.ContainsKey('AdditionalContentTarget')) { $mapping.AdditionalContentTarget } else { $null }
    Copy-CounterpartFiles -Source $mapping.Source -Target $mapping.Target `
            -ContentSource $contentSource -ContentTarget $contentTarget `
            -AdditionalContentSource $additionalContentSource -AdditionalContentTarget $additionalContentTarget
}

# Per-wood item groups are named for the wood type rather than an individual block.
foreach ($shape in @('slabs', 'stairs', 'walls')) {
    Copy-CounterpartFiles -Source "dark_oak_$shape" -Target "pale_oak_$shape" `
            -ContentSource 'dark_oak' -ContentTarget 'pale_oak'
}

# Mirror every Dark Oak/Moss derived entry already admitted to the generic Minecraft
# block tags. This preserves the counterpart's mining, shape, leaf, and wall tags.
$minecraftTagRoot = Join-Path $resolvedOutput 'data\minecraft\tags\block'
Get-ChildItem -LiteralPath $minecraftTagRoot -Recurse -Filter '*.json' -File | ForEach-Object {
    $json = Get-Content -LiteralPath $_.FullName -Raw | ConvertFrom-Json
    if ($null -eq $json.values) { return }
    $values = [System.Collections.Generic.List[object]]::new()
    foreach ($value in @($json.values)) { $values.Add($value) }
    foreach ($value in @($json.values)) {
        if ($value -isnot [string]) { continue }
        foreach ($mapping in $paleCounterparts) {
            if (-not $value.Contains($mapping.Source)) { continue }
            $candidate = $value.Replace($mapping.Source, $mapping.Target)
            if (-not $values.Contains($candidate)) { $values.Add($candidate) }
        }
    }
    $json.values = @($values)
    [System.IO.File]::WriteAllText(
        $_.FullName,
        ($json | ConvertTo-Json -Depth 100),
        [System.Text.UTF8Encoding]::new($false))
}

# Clone localized names while retaining the exact new registry identifiers.
$langPath = Join-Path $resolvedOutput "assets\$namespace\lang\en_us.json"
$lang = Get-Content -LiteralPath $langPath -Raw | ConvertFrom-Json
foreach ($mapping in $paleCounterparts) {
    foreach ($property in @($lang.PSObject.Properties | Where-Object { $_.Name.Contains($mapping.Source) })) {
        $targetName = $property.Name.Replace($mapping.Source, $mapping.Target)
        if ($lang.PSObject.Properties.Name -contains $targetName) { continue }
        $targetValue = if ($mapping.Source -eq 'moss_block') {
            ([string]$property.Value).Replace('Moss', 'Pale Moss')
        } else {
            ([string]$property.Value).Replace('Dark Oak', 'Pale Oak')
        }
        $lang | Add-Member -NotePropertyName $targetName -NotePropertyValue $targetValue
    }
}
[System.IO.File]::WriteAllText(
    $langPath,
    ($lang | ConvertTo-Json -Depth 20),
    [System.Text.UTF8Encoding]::new($false))

$legacyItemModelRoot = Join-Path $resolvedOutput "assets\$namespace\models\item"
$itemDefinitionRoot = Join-Path $resolvedOutput "assets\$namespace\items"
New-Item -ItemType Directory -Path $itemDefinitionRoot -Force | Out-Null
$allItemModels = @(
    Get-ChildItem -LiteralPath $legacyItemModelRoot -Filter '*.json' -File
    foreach ($root in $staticRoots) {
        $staticModels = Join-Path $root "assets\$namespace\models\item"
        if (Test-Path -LiteralPath $staticModels) { Get-ChildItem -LiteralPath $staticModels -Filter '*.json' -File }
    }
)
$allItemModels | Sort-Object BaseName -Unique | ForEach-Object {
    $id = $_.BaseName
    $family = $id -replace '_(slab|stairs|wall)$', ''
    $definition = [ordered]@{
        model = [ordered]@{
            type = 'minecraft:model'
            model = "$namespace`:item/$id"
        }
    }
    $itemTint = switch ($family) {
        'grass_block' { [ordered]@{ type = 'minecraft:grass'; downfall = 1.0; temperature = 0.5 } }
        { $_ -in @('oak_leaves', 'jungle_leaves', 'acacia_leaves', 'dark_oak_leaves') } {
            [ordered]@{ type = 'minecraft:constant'; value = -12012264 }
        }
        'mangrove_leaves' { [ordered]@{ type = 'minecraft:constant'; value = -7158200 } }
        'spruce_leaves' { [ordered]@{ type = 'minecraft:constant'; value = -10380959 } }
        'birch_leaves' { [ordered]@{ type = 'minecraft:constant'; value = -8345771 } }
        default { $null }
    }
    if ($null -ne $itemTint) {
        $definition.model.tints = @($itemTint)
    }
    # `Set-Content` can lose this OneDrive worktree's just-created directory between
    # provider entries. Use the same long-path .NET writer as the archive staging path
    # and re-establish the narrowly scoped parent immediately before every definition.
    [System.IO.Directory]::CreateDirectory("\\?\$itemDefinitionRoot") | Out-Null
    [System.IO.File]::WriteAllText(
        "\\?\$(Join-Path $itemDefinitionRoot "$id.json")",
        (($definition | ConvertTo-Json -Depth 10) + [Environment]::NewLine),
        [System.Text.UTF8Encoding]::new($true))
}

# Minecraft 26.2 renamed the pillar side texture; keep generated native models aligned
# with the provider's PILLAR role so CNM and Nibaru consume one canonical source.
$blockModelRoot = Join-Path $resolvedOutput "assets\$namespace\models\block"
Get-ChildItem -LiteralPath $blockModelRoot -Filter 'quartz_pillar_*.json' -File | ForEach-Object {
    $raw = Get-Content -LiteralPath $_.FullName -Raw
    $corrected = $raw.Replace('minecraft:block/quartz_pillar"', 'minecraft:block/quartz_pillar_side"')
    if ($corrected -ne $raw) { Set-Content -LiteralPath $_.FullName -Value $corrected -Encoding utf8 }
}

$providerRecipeFiles = @(
    foreach ($relativeRoot in @("data\$namespace\recipe", "data\$namespace\recipes")) {
        $path = Join-Path $resolvedOutput $relativeRoot
        if (Test-Path -LiteralPath $path -PathType Container) {
            Get-ChildItem -LiteralPath $path -Filter '*.json' -File -Recurse
        }
    }
)
$providerRecipeAdvancements = @(
    foreach ($relativeRoot in @("data\$namespace\advancement", "data\$namespace\advancements")) {
        $path = Join-Path $resolvedOutput $relativeRoot
        if (Test-Path -LiteralPath $path -PathType Container) {
            Get-ChildItem -LiteralPath $path -Filter '*.json' -File -Recurse | Where-Object {
                $_.FullName.Substring($path.Length).TrimStart('\') -like 'recipes*'
            }
        }
    }
)

$counts = [ordered]@{
    blockstates = (Get-ChildItem (Join-Path $resolvedOutput "assets\$namespace\blockstates") -Filter '*.json' -File).Count
    item_definitions = (Get-ChildItem $itemDefinitionRoot -Filter '*.json' -File).Count
    item_models = (Get-ChildItem $legacyItemModelRoot -Filter '*.json' -File).Count
    loot_tables = (Get-ChildItem (Join-Path $resolvedOutput "data\$namespace\loot_table\blocks") -Filter '*.json' -File).Count
    recipes = $providerRecipeFiles.Count
    recipe_advancements = $providerRecipeAdvancements.Count
    minecraft_block_tags = (Get-ChildItem (Join-Path $resolvedOutput 'data\minecraft\tags\block') -Filter '*.json' -File -Recurse).Count
}

$counts | ConvertTo-Json
