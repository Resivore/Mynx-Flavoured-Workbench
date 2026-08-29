[CmdletBinding()]
param(
    [string]$Artifact
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$recipeRoot = Join-Path $projectRoot 'common\src\main\resources\data\dramaticdoors\recipe'
$compatRecipePath = Join-Path $projectRoot 'common\src\main\java\com\fizzware\dramaticdoors\compat\DDCompatRecipe.java'

function Assert-True {
    param(
        [bool]$Condition,
        [string]$Message
    )

    if (-not $Condition) {
        throw $Message
    }
}

function Has-Property {
    param(
        [object]$Object,
        [string]$Name
    )

    return $null -ne $Object -and $Object.PSObject.Properties.Name -contains $Name
}

function Assert-IngredientString {
    param(
        [object]$Value,
        [string]$Context
    )

    Assert-True ($Value -is [string] -and -not [string]::IsNullOrWhiteSpace($Value)) "$Context must use a direct 26.2 item/tag string."
    Assert-True (-not $Value.StartsWith('##')) "$Context contains an invalid double tag prefix."
}

function Assert-FabricWoodworksCondition {
    param(
        [object]$Json,
        [bool]$Negated,
        [string]$Context
    )

    Assert-True (Has-Property $Json 'fabric:load_conditions') "$Context lacks fabric:load_conditions."
    $condition = $Json.'fabric:load_conditions'
    if ($Negated) {
        Assert-True ($condition.condition -ceq 'fabric:not') "$Context must disable the fallback stonecutting recipe when Woodworks is loaded."
        Assert-True (Has-Property $condition 'value') "$Context lacks the nested Fabric condition."
        $condition = $condition.value
    }

    Assert-True ($condition.condition -ceq 'fabric:all_mods_loaded') "$Context must test whether Woodworks is loaded."
    $values = @($condition.values)
    Assert-True ($values.Count -eq 1 -and $values[0] -ceq 'woodworks') "$Context must target only the woodworks mod ID."
}

function Assert-RecipeSet {
    param(
        [object[]]$Recipes,
        [string]$Origin
    )

    Assert-True ($Recipes.Count -eq 59) "$Origin must contain exactly 59 Dramatic Doors recipes; found $($Recipes.Count)."

    $typeCounts = @{}
    $fabricConditionCount = 0
    $neoForgeConditionCount = 0
    $recipesByName = @{}

    foreach ($recipe in $Recipes) {
        $name = $recipe.Name
        $json = $recipe.Json
        $recipesByName[$name] = $json

        Assert-True (Has-Property $json 'type') "$Origin/$name lacks a recipe type."
        $type = [string]$json.type
        if (-not $typeCounts.ContainsKey($type)) { $typeCounts[$type] = 0 }
        $typeCounts[$type]++

        Assert-True (Has-Property $json 'result') "$Origin/$name lacks a result."
        if ($json.result -is [string]) {
            $resultId = [string]$json.result
            $resultCount = if (Has-Property $json 'count') { [int]$json.count } else { 1 }
        }
        else {
            Assert-True (Has-Property $json.result 'id') "$Origin/$name result object lacks its 26.2 id field."
            $resultId = [string]$json.result.id
            $resultCount = if (Has-Property $json.result 'count') { [int]$json.result.count } else { 1 }
        }
        $expectedResultName = $name -replace '_sawmill$', '' -replace '_waxing$', ''
        Assert-True ($resultId -ceq "dramaticdoors:$expectedResultName") "$Origin/$name changed its result ID: $resultId."

        switch ($type) {
            'minecraft:crafting_shaped' {
                Assert-True (Has-Property $json 'pattern' -and @($json.pattern).Count -gt 0) "$Origin/$name lacks its shaped pattern."
                Assert-True (Has-Property $json 'key') "$Origin/$name lacks its shaped key."
                foreach ($key in $json.key.PSObject.Properties) {
                    Assert-IngredientString $key.Value "$Origin/$name key '$($key.Name)'"
                }
                Assert-True ($resultCount -eq 2) "$Origin/$name must still produce two doors."
            }
            'minecraft:crafting_shapeless' {
                Assert-True (Has-Property $json 'ingredients') "$Origin/$name lacks shapeless ingredients."
                $ingredients = @($json.ingredients)
                Assert-True ($ingredients.Count -ge 1 -and $ingredients.Count -le 9) "$Origin/$name must contain 1-9 shapeless ingredients."
                foreach ($ingredient in $ingredients) {
                    Assert-IngredientString $ingredient "$Origin/$name ingredient"
                }
                Assert-True ($resultCount -eq 1) "$Origin/$name changed its single-door waxing result count."
            }
            'minecraft:stonecutting' {
                Assert-True (Has-Property $json 'ingredient') "$Origin/$name lacks its stonecutting ingredient."
                Assert-IngredientString $json.ingredient "$Origin/$name ingredient"
                Assert-True ($resultCount -eq 2) "$Origin/$name must still produce two doors."
            }
            'woodworks:sawmill' {
                Assert-True (Has-Property $json 'ingredient') "$Origin/$name lacks its sawmill ingredient."
                Assert-IngredientString $json.ingredient "$Origin/$name ingredient"
                Assert-True ($resultCount -eq 2) "$Origin/$name must still produce two doors."
                Assert-True (Has-Property $json 'neoforge:conditions') "$Origin/$name lost its existing NeoForge Woodworks condition."
                Assert-FabricWoodworksCondition $json $false "$Origin/$name"
            }
            default {
                throw "$Origin/$name has an unexpected recipe type: $type"
            }
        }

        if (Has-Property $json 'fabric:load_conditions') { $fabricConditionCount++ }
        if (Has-Property $json 'neoforge:conditions') { $neoForgeConditionCount++ }
    }

    foreach ($expected in @{
        'minecraft:crafting_shaped' = 20
        'minecraft:crafting_shapeless' = 8
        'minecraft:stonecutting' = 20
        'woodworks:sawmill' = 11
    }.GetEnumerator()) {
        Assert-True ($typeCounts.ContainsKey($expected.Key) -and $typeCounts[$expected.Key] -eq $expected.Value) "$Origin has an unexpected $($expected.Key) count."
    }

    $sawmillNames = @($Recipes | Where-Object { $_.Json.type -ceq 'woodworks:sawmill' } | ForEach-Object { $_.Name })
    foreach ($sawmillName in $sawmillNames) {
        $fallbackName = $sawmillName -replace '_sawmill$', ''
        Assert-True ($recipesByName.ContainsKey($fallbackName)) "$Origin lacks the fallback recipe for $sawmillName."
        $fallback = $recipesByName[$fallbackName]
        Assert-True ($fallback.type -ceq 'minecraft:stonecutting') "$Origin/$fallbackName is not the expected stonecutting fallback."
        Assert-True (Has-Property $fallback 'neoforge:conditions') "$Origin/$fallbackName lost its existing NeoForge exclusion."
        Assert-FabricWoodworksCondition $fallback $true "$Origin/$fallbackName"
    }

    Assert-True ($fabricConditionCount -eq 22) "$Origin must contain exactly 22 Fabric Woodworks conditions; found $fabricConditionCount."
    Assert-True ($neoForgeConditionCount -eq 22) "$Origin must retain exactly 22 NeoForge Woodworks conditions; found $neoForgeConditionCount."
}

$sourceRecipes = @(Get-ChildItem -LiteralPath $recipeRoot -Filter '*.json' -File | Sort-Object Name | ForEach-Object {
    [pscustomobject]@{
        Name = $_.BaseName
        Json = Get-Content -LiteralPath $_.FullName -Raw -Encoding UTF8 | ConvertFrom-Json
    }
})
Assert-RecipeSet $sourceRecipes 'source'

$compatSource = Get-Content -LiteralPath $compatRecipePath -Raw -Encoding UTF8
$normalizedCompatSource = [regex]::Replace($compatSource, '\s+', ' ')
Assert-True ($normalizedCompatSource.Contains('String ingredient = ("tag".equals(type.get(i)) ? "#" : "") + items.get(i);')) 'Dynamic shaped recipes must encode item/tag ingredients as 26.2 strings.'
Assert-True ($normalizedCompatSource.Contains('keyList.addProperty(keys.get(i) + "", ingredient);')) 'Dynamic shaped recipes must write the string ingredient into the key map.'
Assert-True (([regex]::Matches($normalizedCompatSource, [regex]::Escape('json.addProperty("ingredient", input.toString());'))).Count -eq 2) 'Dynamic stonecutting and sawmill recipes must both encode direct ingredient strings.'
Assert-True (-not $normalizedCompatSource.Contains('ingredient.addProperty("item"')) 'Dynamic recipe generation still contains the obsolete item object schema.'

$artifactLabel = 'source-only'
if (-not [string]::IsNullOrWhiteSpace($Artifact)) {
    $artifactPath = (Resolve-Path -LiteralPath $Artifact).Path
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [IO.Compression.ZipFile]::OpenRead($artifactPath)
    try {
        $entries = @($archive.Entries | Where-Object { $_.FullName -match '^data/dramaticdoors/recipe/[^/]+\.json$' } | Sort-Object FullName)
        $packagedRecipes = @($entries | ForEach-Object {
            $reader = [IO.StreamReader]::new($_.Open())
            try { $json = $reader.ReadToEnd() | ConvertFrom-Json }
            finally { $reader.Dispose() }
            [pscustomobject]@{
                Name = [IO.Path]::GetFileNameWithoutExtension($_.FullName)
                Json = $json
            }
        })
        Assert-RecipeSet $packagedRecipes 'artifact'

        Assert-True ($null -ne $archive.GetEntry('com/fizzware/dramaticdoors/compat/DDCompatRecipe.class')) 'Packaged Canary lacks DDCompatRecipe.class.'
        $metadataEntry = $archive.GetEntry('fabric.mod.json')
        Assert-True ($null -ne $metadataEntry) 'Packaged Canary lacks fabric.mod.json.'
        $reader = [IO.StreamReader]::new($metadataEntry.Open())
        try { $metadata = $reader.ReadToEnd() | ConvertFrom-Json }
        finally { $reader.Dispose() }
        Assert-True ($metadata.id -ceq 'dramaticdoors' -and $metadata.version -ceq '1.20.1-3.3.3+26.2-workbench-canary6') "Unexpected packaged identity: $($metadata.id) $($metadata.version)."
    }
    finally {
        $archive.Dispose()
    }
    $artifactLabel = [IO.Path]::GetFileName($artifactPath)
}

Write-Output "DRAMATIC_DOORS_RECIPE_RESOURCES_26_2_OK: 59 recipes; 26.2 ingredient strings; 22 Fabric/NeoForge Woodworks conditions; dynamic builders; artifact=$artifactLabel"
