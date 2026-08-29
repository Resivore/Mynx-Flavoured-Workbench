param(
    [switch]$ValidateDefinitions
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$javaRoot = Join-Path $projectRoot 'common/src/main/java'
$resourcesRoot = Join-Path $projectRoot 'common/src/main/resources/assets/dramaticdoors'
$namesSource = Get-Content -Raw -LiteralPath (Join-Path $javaRoot 'com/fizzware/dramaticdoors/registry/DDNames.java')
$names = @{}

[regex]::Matches($namesSource, 'public static final String\s+(\w+)\s*=\s*"([^"]+)"') | ForEach-Object {
    $names[$_.Groups[1].Value] = $_.Groups[2].Value
}

$keys = [System.Collections.Generic.HashSet[string]]::new()
$unparsed = [System.Collections.Generic.List[string]]::new()

Get-ChildItem -LiteralPath $javaRoot -Recurse -Filter '*.java' | ForEach-Object {
    $content = [regex]::Replace((Get-Content -Raw -LiteralPath $_.FullName), '(?s)/\*.*?\*/', '')
    foreach ($line in ($content -split "`r?`n")) {
        $code = [regex]::Replace($line, '//.*$', '')
        if ($code -match 'DDRegistry\.register(?:Weathering|Sliding|Stable)?DoorBlockAndItem\(') {
            $match = [regex]::Match($code, 'register(?:Weathering|Sliding|Stable)?DoorBlockAndItem\(DDNames\.(\w+),\s*(?:DDNames\.(\w+)|null),.*?,\s*(true|false)(?:,\s*WeatherState\.\w+)?\);\s*$')
            if (-not $match.Success) {
                $unparsed.Add("$($_.FullName): $line")
                continue
            }
            [void]$keys.Add($match.Groups[1].Value)
            if ($match.Groups[3].Value -eq 'true' -and $match.Groups[2].Success) {
                [void]$keys.Add($match.Groups[2].Value)
            }
        }

        if ($code -match 'DOOR_ITEMS(?:_TO_REGISTER)?\.add' -and $code -match 'DDNames\.') {
            $match = [regex]::Match($code, 'Pair<String, Item>\(DDNames\.(\w+)')
            if (-not $match.Success) {
                $unparsed.Add("$($_.FullName): $line")
                continue
            }
            [void]$keys.Add($match.Groups[1].Value)
        }
    }
}

if ($unparsed.Count -gt 0) {
    throw "Unparsed registration statements:`n$($unparsed -join "`n")"
}

$ids = @($keys | ForEach-Object {
    if (-not $names.ContainsKey($_)) {
        throw "Registration references unknown DDNames constant: $_"
    }
    $names[$_]
} | Sort-Object -Unique)

if (-not $ValidateDefinitions) {
    $ids
    return
}

$modelRoot = Join-Path $resourcesRoot 'models/item'
$definitionRoot = Join-Path $resourcesRoot 'items'
$textureRoot = Join-Path $resourcesRoot 'textures'
$languagePath = Join-Path $resourcesRoot 'lang/en_us.json'
$language = Get-Content -Raw -LiteralPath $languagePath | ConvertFrom-Json
$languageKeys = @($language.PSObject.Properties.Name)
$errors = [System.Collections.Generic.List[string]]::new()

$definitionIds = if (Test-Path -LiteralPath $definitionRoot) {
    @(Get-ChildItem -LiteralPath $definitionRoot -Filter '*.json' | ForEach-Object BaseName | Sort-Object -Unique)
} else { @() }

foreach ($id in $ids) {
    $modelPath = Join-Path $modelRoot "$id.json"
    $definitionPath = Join-Path $definitionRoot "$id.json"
    if (-not (Test-Path -LiteralPath $modelPath)) {
        $errors.Add("Missing legacy item model: $id")
        continue
    }
    if (-not (Test-Path -LiteralPath $definitionPath)) {
        $errors.Add("Missing 26.2 item definition: $id")
        continue
    }

    try { $model = Get-Content -Raw -LiteralPath $modelPath | ConvertFrom-Json } catch { $errors.Add("Malformed model JSON: $id") ; continue }
    try { $definition = Get-Content -Raw -LiteralPath $definitionPath | ConvertFrom-Json } catch { $errors.Add("Malformed definition JSON: $id") ; continue }

    if ($definition.model.type -ne 'minecraft:model' -or $definition.model.model -ne "dramaticdoors:item/$id") {
        $errors.Add("Unexpected item definition target: $id")
    }
    if ($model.overrides -or $model.elements -or $model.display -or -not $model.textures.layer0) {
        $errors.Add("Nontrivial legacy item model requires manual audit: $id")
    }
    if ($model.parent -notin @('item/generated', 'minecraft:item/generated')) {
        $errors.Add("Unexpected item model parent for ${id}: $($model.parent)")
    }
    if ($model.textures.layer0 -match '^dramaticdoors:item/(.+)$') {
        $texturePath = Join-Path $textureRoot "item/$($Matches[1]).png"
        if (-not (Test-Path -LiteralPath $texturePath)) {
            $errors.Add("Missing item texture for ${id}: $($model.textures.layer0)")
        }
    } else {
        $errors.Add("Unexpected item texture namespace for ${id}: $($model.textures.layer0)")
    }
    if ("block.dramaticdoors.$id" -notin $languageKeys) {
        $errors.Add("Missing en_us block language key: $id")
    }
}

foreach ($id in $definitionIds) {
    if ($id -notin $ids) {
        $errors.Add("Definition exists for non-registered item: $id")
    }
}

Write-Output "Registered items: $($ids.Count)"
Write-Output "Item definitions: $($definitionIds.Count)"
Write-Output "Validation errors: $($errors.Count)"
if ($errors.Count -gt 0) {
    $errors | ForEach-Object { Write-Error $_ }
    exit 1
}
