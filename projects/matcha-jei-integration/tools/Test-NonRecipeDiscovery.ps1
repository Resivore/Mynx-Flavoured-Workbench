#requires -Version 7.0

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

Add-Type -AssemblyName System.IO.Compression

function Assert-True {
    param(
        [Parameter(Mandatory)]
        [bool] $Condition,

        [Parameter(Mandatory)]
        [string] $Message
    )

    if (-not $Condition) {
        throw $Message
    }
}

function Assert-Equal {
    param(
        [Parameter(Mandatory)]
        [AllowNull()]
        [object] $Actual,

        [Parameter(Mandatory)]
        [AllowNull()]
        [object] $Expected,

        [Parameter(Mandatory)]
        [string] $Label
    )

    if ($Actual -cne $Expected) {
        throw "$Label changed: expected '$Expected', found '$Actual'."
    }
}

function Assert-Matches {
    param(
        [Parameter(Mandatory)]
        [string] $Text,

        [Parameter(Mandatory)]
        [string] $Pattern,

        [Parameter(Mandatory)]
        [string] $Message
    )

    if (-not [regex]::IsMatch(
            $Text,
            $Pattern,
            [Text.RegularExpressions.RegexOptions]::Singleline
        )) {
        throw $Message
    }
}

function New-OrdinalSet {
    Write-Output -NoEnumerate ([Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal))
}

function Copy-OrdinalSet {
    param(
        [Parameter(Mandatory)]
        [Collections.Generic.HashSet[string]] $Source
    )

    $copy = New-OrdinalSet
    $copy.UnionWith($Source)
    Write-Output -NoEnumerate $copy
}

function Format-SetDifference {
    param(
        [Parameter(Mandatory)]
        [Collections.Generic.HashSet[string]] $Actual,

        [Parameter(Mandatory)]
        [Collections.Generic.HashSet[string]] $Expected
    )

    $missing = Copy-OrdinalSet $Expected
    $missing.ExceptWith($Actual)
    $extra = Copy-OrdinalSet $Actual
    $extra.ExceptWith($Expected)
    return "missing=[$(@($missing | Sort-Object) -join ', ')]; extra=[$(@($extra | Sort-Object) -join ', ')]"
}

function Assert-SetEqual {
    param(
        [Parameter(Mandatory)]
        [Collections.Generic.HashSet[string]] $Actual,

        [Parameter(Mandatory)]
        [Collections.Generic.HashSet[string]] $Expected,

        [Parameter(Mandatory)]
        [string] $Label
    )

    if (-not $Actual.SetEquals($Expected)) {
        throw "$Label changed: $(Format-SetDifference -Actual $Actual -Expected $Expected)"
    }
}

function Normalize-Identifier {
    param(
        [Parameter(Mandatory)]
        [string] $Identifier
    )

    if ($Identifier.Contains(':')) {
        return $Identifier
    }
    return "minecraft:$Identifier"
}

function Read-ZipJson {
    param(
        [Parameter(Mandatory)]
        [IO.Compression.ZipArchiveEntry] $Entry
    )

    $reader = [IO.StreamReader]::new($Entry.Open(), [Text.Encoding]::UTF8)
    try {
        return $reader.ReadToEnd() | ConvertFrom-Json -AsHashtable -Depth 100
    } finally {
        $reader.Dispose()
    }
}

function Get-ItemModels {
    param(
        [Parameter(Mandatory)]
        [AllowNull()]
        [object] $Node
    )

    if ($null -eq $Node -or $Node -is [string] -or $Node -is [ValueType]) {
        return
    }

    if ($Node -is [Collections.IDictionary]) {
        foreach ($key in $Node.Keys) {
            if ([string] $key -ceq 'minecraft:item_model' -and $Node[$key] -is [string]) {
                $Node[$key]
            }
            Get-ItemModels $Node[$key]
        }
        return
    }

    if ($Node -is [Collections.IEnumerable]) {
        foreach ($value in $Node) {
            Get-ItemModels $value
        }
    }
}

function Test-ContainsComponentMap {
    param(
        [Parameter(Mandatory)]
        [AllowNull()]
        [object] $Node
    )

    if ($null -eq $Node -or $Node -is [string] -or $Node -is [ValueType]) {
        return $false
    }
    if ($Node -is [Collections.IDictionary]) {
        if ($Node['components'] -is [Collections.IDictionary] -and $Node['components'].Count -gt 0) {
            return $true
        }
        foreach ($value in $Node.Values) {
            if (Test-ContainsComponentMap $value) {
                return $true
            }
        }
        return $false
    }
    if ($Node -is [Collections.IEnumerable]) {
        foreach ($value in $Node) {
            if (Test-ContainsComponentMap $value) {
                return $true
            }
        }
    }
    return $false
}

function ConvertTo-FixtureStack {
    param(
        [Parameter(Mandatory)]
        [Collections.IDictionary] $Stack
    )

    if ($Stack['id'] -isnot [string]) {
        return $null
    }
    $components = [ordered] @{}
    if ($Stack['components'] -is [Collections.IDictionary]) {
        $componentKeys = [string[]] @($Stack['components'].Keys)
        [Array]::Sort($componentKeys, [StringComparer]::Ordinal)
        foreach ($componentKey in $componentKeys) {
            $value = $Stack['components'][$componentKey]
            if (
                $componentKey -ceq 'minecraft:item_model' -and
                $value -is [string]
            ) {
                $value = Normalize-Identifier $value
            }
            $components[$componentKey] = $value
        }
    }
    return [ordered] @{
        id = Normalize-Identifier $Stack['id']
        components = $components
    }
}

function Get-FixtureStackKey {
    param(
        [Parameter(Mandatory)]
        [Collections.IDictionary] $Stack
    )

    $normalized = ConvertTo-FixtureStack $Stack
    if ($null -eq $normalized) {
        return $null
    }
    return Get-SemanticDigest $normalized
}

function Get-JsonArrayValues {
    param(
        [Parameter(Mandatory)]
        [AllowNull()]
        [object] $Value
    )

    if ($null -eq $Value -or $Value -is [string] -or $Value -is [Collections.IDictionary]) {
        return @()
    }
    return @($Value | Where-Object { $_ -is [Collections.IDictionary] })
}

function Test-FixturePassThroughFunctions {
    param(
        [Parameter(Mandatory)]
        [AllowEmptyCollection()]
        [object[]] $Functions
    )

    $allowed = @(
        'minecraft:set_count',
        'minecraft:limit_count',
        'minecraft:explosion_decay',
        'minecraft:enchanted_count_increase',
        'minecraft:apply_bonus'
    )
    foreach ($function in $Functions) {
        if ($function['function'] -cnotin $allowed) {
            return $false
        }
    }
    return $true
}

function ConvertFrom-FixtureLootItem {
    param(
        [Parameter(Mandatory)]
        [Collections.IDictionary] $Entry,

        [Parameter(Mandatory)]
        [AllowEmptyCollection()]
        [object[]] $Functions
    )

    if ($Entry['name'] -isnot [string]) {
        return $null
    }
    $components = [ordered] @{}
    $durabilityVaries = $false
    foreach ($function in $Functions) {
        $functionId = [string] $function['function']
        if ($functionId -ceq 'minecraft:set_components') {
            if ($function['components'] -isnot [Collections.IDictionary]) {
                return $null
            }
            foreach ($componentKey in $function['components'].Keys) {
                $components[[string] $componentKey] = $function['components'][$componentKey]
            }
            continue
        }
        if ($functionId -ceq 'minecraft:set_name') {
            if (
                $function['name'] -isnot [string] -or
                ($null -ne $function['conditions'] -and @($function['conditions']).Count -gt 0)
            ) {
                return $null
            }
            $target = if ($function['target'] -is [string]) { $function['target'] } else { 'custom_name' }
            $componentKey = if ($target -ceq 'item_name') {
                'minecraft:item_name'
            } elseif ($target -ceq 'custom_name') {
                'minecraft:custom_name'
            } else {
                return $null
            }
            $components[$componentKey] = $function['name']
            continue
        }
        if ($functionId -ceq 'minecraft:set_damage') {
            $durabilityVaries = $true
            continue
        }
        if (-not (Test-FixturePassThroughFunctions @($function))) {
            return $null
        }
    }
    if ($components.Count -eq 0) {
        return $null
    }
    $stack = [ordered] @{
        id = $Entry['name']
        components = $components
    }
    $key = Get-FixtureStackKey $stack
    if ($null -eq $key) {
        return $null
    }
    return [pscustomobject] @{
        Key = $key
        Stack = ConvertTo-FixtureStack $stack
        DurabilityVaries = $durabilityVaries
    }
}

function Add-FixtureLootEntry {
    param(
        [Parameter(Mandatory)]
        [Collections.IDictionary] $Entry,

        [Parameter(Mandatory)]
        [AllowEmptyCollection()]
        [object[]] $InheritedFunctions,

        [Parameter(Mandatory)]
        [AllowEmptyCollection()]
        [Collections.Generic.Dictionary[string, object]] $Items,

        [Parameter(Mandatory)]
        [AllowEmptyCollection()]
        [Collections.Generic.HashSet[string]] $Children
    )

    $functions = @($InheritedFunctions) + @(Get-JsonArrayValues $Entry['functions'])
    $type = [string] $Entry['type']
    if ($type -ceq 'minecraft:item') {
        $item = ConvertFrom-FixtureLootItem -Entry $Entry -Functions $functions
        if ($null -ne $item) {
            if (-not $Items.ContainsKey($item.Key)) {
                $Items.Add($item.Key, $item)
            } elseif ($item.DurabilityVaries) {
                $Items[$item.Key].DurabilityVaries = $true
            }
        }
    } elseif (
        $type -ceq 'minecraft:loot_table' -and
        $Entry['value'] -is [string] -and
        (Test-FixturePassThroughFunctions $functions)
    ) {
        [void] $Children.Add((Normalize-Identifier $Entry['value']))
    }

    foreach ($childField in 'children', 'entries') {
        foreach ($child in @(Get-JsonArrayValues $Entry[$childField])) {
            Add-FixtureLootEntry -Entry $child -InheritedFunctions $functions -Items $Items -Children $Children
        }
    }
}

function New-FixtureLootNode {
    param(
        [Parameter(Mandatory)]
        [Collections.IDictionary] $Table
    )

    $items = [Collections.Generic.Dictionary[string, object]]::new([StringComparer]::Ordinal)
    $children = New-OrdinalSet
    $tableFunctions = @(Get-JsonArrayValues $Table['functions'])
    foreach ($pool in @(Get-JsonArrayValues $Table['pools'])) {
        $inherited = $tableFunctions + @(Get-JsonArrayValues $pool['functions'])
        foreach ($entry in @(Get-JsonArrayValues $pool['entries'])) {
            Add-FixtureLootEntry -Entry $entry -InheritedFunctions $inherited -Items $items -Children $children
        }
    }
    return [pscustomobject] @{
        DirectItems = $items
        ChildIds = $children
    }
}

function Resolve-FixtureLoot {
    param(
        [Parameter(Mandatory)]
        [string] $TableId,

        [Parameter(Mandatory)]
        [Collections.Generic.Dictionary[string, object]] $Nodes,

        [Parameter(Mandatory)]
        [AllowEmptyCollection()]
        [Collections.Generic.HashSet[string]] $Visiting
    )

    $resolved = [Collections.Generic.Dictionary[string, object]]::new([StringComparer]::Ordinal)
    if (-not $Nodes.ContainsKey($TableId) -or -not $Visiting.Add($TableId)) {
        return [pscustomobject] @{ Map = $resolved }
    }
    $node = $Nodes[$TableId]
    if ($node -is [array] -or $node.DirectItems -isnot [Collections.Generic.Dictionary[string, object]]) {
        throw "Unexpected fixture loot node shape for ${TableId}: node=$($node.GetType().FullName) direct=$($node.DirectItems.GetType().FullName)"
    }
    [Collections.Generic.Dictionary[string, object]] $directItems = $node.DirectItems
    foreach ($itemKey in $directItems.Keys) {
        $resolved.Add($itemKey, $directItems[$itemKey])
    }
    foreach ($childId in @($node.ChildIds | Sort-Object)) {
        $childResult = Resolve-FixtureLoot -TableId $childId -Nodes $Nodes -Visiting $Visiting
        [Collections.Generic.Dictionary[string, object]] $childItems = $childResult.Map
        foreach ($itemKey in $childItems.Keys) {
            if (-not $resolved.ContainsKey($itemKey)) {
                $resolved.Add($itemKey, $childItems[$itemKey])
            } elseif ($childItems[$itemKey].DurabilityVaries) {
                $resolved[$itemKey].DurabilityVaries = $true
            }
        }
    }
    [void] $Visiting.Remove($TableId)
    return [pscustomobject] @{ Map = $resolved }
}

function Test-FixtureAcquisitionSource {
    param(
        [Parameter(Mandatory)]
        [string] $TableId,

        [Parameter(Mandatory)]
        [Collections.Generic.HashSet[string]] $Referenced
    )

    $path = $TableId.Substring($TableId.IndexOf(':') + 1)
    if ($path -cin @(
            'gameplay/chicken_lay',
            'gameplay/fishing',
            'gameplay/piglin_bartering',
            'gameplay/turtle_grow'
        )) {
        return $true
    }
    if ($Referenced.Contains($TableId)) {
        return $false
    }
    if ($path.StartsWith('chests/equipment/', [StringComparison]::Ordinal) -or
        $path -ceq 'chests/fishing_buried_treasure') {
        return $false
    }
    foreach ($prefix in @(
            'chests/',
            'archaeology/',
            'entities/',
            'blocks/',
            'harvest/',
            'pots/',
            'spawners/',
            'shearing/'
        )) {
        if ($path.StartsWith($prefix, [StringComparison]::Ordinal)) {
            return $true
        }
    }
    return $false
}

function Get-NestedLootReferences {
    param(
        [Parameter(Mandatory)]
        [AllowNull()]
        [object] $Node
    )

    if ($null -eq $Node -or $Node -is [string] -or $Node -is [ValueType]) {
        return
    }

    if ($Node -is [Collections.IDictionary]) {
        if ($Node['type'] -ceq 'minecraft:loot_table' -and $Node['value'] -is [string]) {
            $Node['value']
        }
        foreach ($value in $Node.Values) {
            Get-NestedLootReferences $value
        }
        return
    }

    if ($Node -is [Collections.IEnumerable]) {
        foreach ($value in $Node) {
            Get-NestedLootReferences $value
        }
    }
}

function Get-SetComponentMaps {
    param(
        [Parameter(Mandatory)]
        [AllowNull()]
        [object] $Node
    )

    if ($null -eq $Node -or $Node -is [string] -or $Node -is [ValueType]) {
        return
    }

    if ($Node -is [Collections.IDictionary]) {
        if (
            $Node['function'] -ceq 'minecraft:set_components' -and
            $Node['components'] -is [Collections.IDictionary] -and
            $Node['components'].Count -gt 0
        ) {
            $Node['components']
        }
        foreach ($value in $Node.Values) {
            Get-SetComponentMaps $value
        }
        return
    }

    if ($Node -is [Collections.IEnumerable]) {
        foreach ($value in $Node) {
            Get-SetComponentMaps $value
        }
    }
}

function Get-LootComponentStacks {
    param(
        [Parameter(Mandatory)]
        [AllowNull()]
        [object] $Node
    )

    if ($null -eq $Node -or $Node -is [string] -or $Node -is [ValueType]) {
        return
    }

    if ($Node -is [Collections.IDictionary]) {
        if ($Node['type'] -ceq 'minecraft:item' -and $Node['name'] -is [string]) {
            foreach ($components in @(Get-SetComponentMaps $Node['functions'])) {
                [pscustomobject]@{
                    Id = Normalize-Identifier $Node['name']
                    Components = $components
                }
            }
        }
        foreach ($value in $Node.Values) {
            Get-LootComponentStacks $value
        }
        return
    }

    if ($Node -is [Collections.IEnumerable]) {
        foreach ($value in $Node) {
            Get-LootComponentStacks $value
        }
    }
}

function Add-ComponentStackToInventory {
    param(
        [Parameter(Mandatory)]
        [Collections.IDictionary] $Stack,

        [Parameter(Mandatory)]
        [AllowEmptyCollection()]
        [Collections.Generic.HashSet[string]] $ItemIds,

        [Parameter(Mandatory)]
        [AllowEmptyCollection()]
        [Collections.Generic.HashSet[string]] $ComponentIds
    )

    if (
        $Stack['id'] -isnot [string] -or
        $Stack['components'] -isnot [Collections.IDictionary] -or
        $Stack['components'].Count -eq 0
    ) {
        return
    }

    [void] $ItemIds.Add((Normalize-Identifier $Stack['id']))
    foreach ($componentId in $Stack['components'].Keys) {
        [void] $ComponentIds.Add(([string] $componentId -replace '^!', ''))
    }
}

function Add-LootStackToInventory {
    param(
        [Parameter(Mandatory)]
        [pscustomobject] $Stack,

        [Parameter(Mandatory)]
        [AllowEmptyCollection()]
        [Collections.Generic.HashSet[string]] $ItemIds,

        [Parameter(Mandatory)]
        [AllowEmptyCollection()]
        [Collections.Generic.HashSet[string]] $ComponentIds
    )

    [void] $ItemIds.Add($Stack.Id)
    foreach ($componentId in $Stack.Components.Keys) {
        [void] $ComponentIds.Add(([string] $componentId -replace '^!', ''))
    }
}

function ConvertTo-CanonicalValue {
    param(
        [Parameter(Mandatory)]
        [AllowNull()]
        [object] $Value
    )

    if ($null -eq $Value -or $Value -is [string] -or $Value -is [ValueType]) {
        return $Value
    }

    if ($Value -is [Collections.IDictionary]) {
        $ordered = [ordered] @{}
        $keys = [string[]] @($Value.Keys)
        [Array]::Sort($keys, [StringComparer]::Ordinal)
        foreach ($key in $keys) {
            $ordered[$key] = ConvertTo-CanonicalValue $Value[$key]
        }
        return $ordered
    }

    if ($Value -is [Collections.IEnumerable]) {
        $items = [Collections.Generic.List[object]]::new()
        foreach ($item in $Value) {
            $items.Add((ConvertTo-CanonicalValue $item))
        }
        return ,$items.ToArray()
    }

    return $Value
}

function Get-SemanticDigest {
    param(
        [Parameter(Mandatory)]
        [object] $Value
    )

    $json = ConvertTo-Json -InputObject (ConvertTo-CanonicalValue $Value) -Depth 100 -Compress
    $bytes = [Text.Encoding]::UTF8.GetBytes($json)
    return [Convert]::ToHexString([Security.Cryptography.SHA256]::HashData($bytes))
}

function Assert-StackDigest {
    param(
        [Parameter(Mandatory)]
        [Collections.IDictionary] $Stack,

        [Parameter(Mandatory)]
        [string] $ExpectedDigest,

        [Parameter(Mandatory)]
        [string] $Label
    )

    Assert-Equal (Get-SemanticDigest $Stack) $ExpectedDigest "$Label exact stack"
}

function Get-StackCount {
    param(
        [Parameter(Mandatory)]
        [Collections.IDictionary] $Stack
    )

    if ($Stack.Contains('count')) {
        return [int] $Stack['count']
    }
    return 1
}

function Read-ManifestSet {
    param(
        [Parameter(Mandatory)]
        [string] $Path
    )

    Assert-True (Test-Path -LiteralPath $Path -PathType Leaf) "Missing component inventory manifest: $Path"
    $set = New-OrdinalSet
    foreach ($line in Get-Content -LiteralPath $Path -Encoding UTF8) {
        $value = $line.Trim()
        if ($value.Length -eq 0 -or $value.StartsWith('#')) {
            continue
        }
        Assert-True ($value -cmatch '^[a-z0-9_.-]+:[a-z0-9_./-]+$') "Invalid identifier '$value' in $Path."
        Assert-True ($set.Add($value)) "Duplicate identifier '$value' in $Path."
    }
    Write-Output -NoEnumerate $set
}

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..\..')).Path
$zipPath = Join-Path $repoRoot 'originals\datapacks\Matcha_Flavoured_1_12.zip'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path

Assert-True (Test-Path -LiteralPath $zipPath -PathType Leaf) "Missing authoritative Matcha reference: $zipPath"

$zipStream = [IO.File]::OpenRead($zipPath)
$zip = $null
try {
    $zip = [IO.Compression.ZipArchive]::new($zipStream, [IO.Compression.ZipArchiveMode]::Read)

    $recipeEntries = @(
        $zip.Entries | Where-Object FullName -CMatch '^data/[^/]+/recipe/.+\.json$'
    )
    $tradeEntries = @(
        $zip.Entries | Where-Object FullName -CMatch '^data/minecraft/villager_trade/.+\.json$'
    )
    $lootEntries = @(
        $zip.Entries | Where-Object FullName -CMatch '^data/[^/]+/loot_table/.+\.json$'
    )

    Assert-Equal $recipeEntries.Count 1076 'Recipe JSON count'
    Assert-Equal $tradeEntries.Count 290 'Villager-trade JSON count'

    $recipeRecords = @(
        foreach ($entry in $recipeEntries) {
            [pscustomobject]@{
                Path = $entry.FullName
                Json = Read-ZipJson $entry
            }
        }
    )
    $tradeRecords = @(
        foreach ($entry in $tradeEntries) {
            [pscustomobject]@{
                Path = $entry.FullName
                Json = Read-ZipJson $entry
            }
        }
    )
    $lootRecords = @(
        foreach ($entry in $lootEntries) {
            $pathMatch = [regex]::Match(
                $entry.FullName,
                '^data/(?<namespace>[^/]+)/loot_table/(?<path>.+)\.json$'
            )
            Assert-True $pathMatch.Success "Could not convert loot-table path to an identifier: $($entry.FullName)"
            $json = Read-ZipJson $entry
            [pscustomobject]@{
                Path = $entry.FullName
                Id = "$($pathMatch.Groups['namespace'].Value):$($pathMatch.Groups['path'].Value)"
                Json = $json
                Models = @(Get-ItemModels $json)
                References = @(Get-NestedLootReferences $json)
                ComponentStacks = @(Get-LootComponentStacks $json)
            }
        }
    )

    $tradeByPath = [Collections.Generic.Dictionary[string, object]]::new([StringComparer]::Ordinal)
    foreach ($record in $tradeRecords) {
        $tradeByPath.Add($record.Path, $record.Json)
    }
    $lootByPath = [Collections.Generic.Dictionary[string, object]]::new([StringComparer]::Ordinal)
    $lootById = [Collections.Generic.Dictionary[string, object]]::new([StringComparer]::Ordinal)
    foreach ($record in $lootRecords) {
        $lootByPath.Add($record.Path, $record)
        $lootById.Add($record.Id, $record)
    }

    $componentRecipeOutputs = @(
        $recipeRecords | Where-Object {
            $_.Json['result'] -is [Collections.IDictionary] -and
            $_.Json['result']['components'] -is [Collections.IDictionary] -and
            $_.Json['result']['components'].Count -gt 0
        }
    )
    Assert-Equal $componentRecipeOutputs.Count 315 'Non-empty component-bearing recipe output count'

    $recipeModels = New-OrdinalSet
    foreach ($record in $recipeRecords) {
        foreach ($model in @(Get-ItemModels $record.Json)) {
            [void] $recipeModels.Add($model)
        }
    }
    $tradeModels = New-OrdinalSet
    foreach ($record in $tradeRecords) {
        foreach ($model in @(Get-ItemModels $record.Json)) {
            [void] $tradeModels.Add($model)
        }
    }
    $lootModels = New-OrdinalSet
    foreach ($record in $lootRecords) {
        foreach ($model in $record.Models) {
            [void] $lootModels.Add($model)
        }
    }

    Assert-Equal $tradeModels.Count 145 'Distinct villager-trade item_model identity count'

    $componentTradeRecords = @(
        $tradeRecords | Where-Object { Test-ContainsComponentMap $_.Json }
    )
    Assert-Equal $componentTradeRecords.Count 177 'Component-bearing trade definition count'
    $dynamicComponentTradePaths = @(
        $componentTradeRecords |
            Where-Object {
                $null -ne $_.Json['given_item_modifiers'] -and
                @($_.Json['given_item_modifiers']).Count -gt 0
            } |
            ForEach-Object Path |
            Sort-Object
    )
    $expectedDynamicTradePaths = @(
        'data/minecraft/villager_trade/cartographer/1/papal_outpost.json',
        'data/minecraft/villager_trade/cartographer/3/abbey.json'
    )
    Assert-Equal ($dynamicComponentTradePaths | ConvertTo-Json -Compress) ($expectedDynamicTradePaths | ConvertTo-Json -Compress) 'Contextual exploration-map trade exclusions'
    Assert-Equal ($componentTradeRecords.Count - $dynamicComponentTradePaths.Count) 175 'Exact supported component-bearing trade count'

    $nonRecipeModels = Copy-OrdinalSet $tradeModels
    $nonRecipeModels.UnionWith($lootModels)
    $nonRecipeModels.ExceptWith($recipeModels)

    $lootAndTradeModels = Copy-OrdinalSet $tradeModels
    $lootAndTradeModels.IntersectWith($lootModels)
    $lootAndTradeModels.ExceptWith($recipeModels)

    $tradeOnlyModels = Copy-OrdinalSet $tradeModels
    $tradeOnlyModels.ExceptWith($lootModels)
    $tradeOnlyModels.ExceptWith($recipeModels)

    $lootOnlyModels = Copy-OrdinalSet $lootModels
    $lootOnlyModels.ExceptWith($tradeModels)
    $lootOnlyModels.ExceptWith($recipeModels)

    Assert-Equal $nonRecipeModels.Count 112 'Non-recipe item_model identity count'
    Assert-Equal $lootAndTradeModels.Count 57 'Loot-and-trade-only item_model identity count'
    Assert-Equal $tradeOnlyModels.Count 45 'Trade-only item_model identity count'
    Assert-Equal $lootOnlyModels.Count 10 'Loot-only item_model identity count'

    $topazExpertPath = 'data/minecraft/villager_trade/toolsmith/4/topaz_earrings.json'
    $topazMasterPath = 'data/minecraft/villager_trade/toolsmith/5/sell_topaz.json'
    Assert-True $tradeByPath.ContainsKey($topazExpertPath) "Missing Expert Toolsmith Topaz Earrings trade."
    Assert-True $tradeByPath.ContainsKey($topazMasterPath) "Missing Master Toolsmith Topaz sale trade."

    $topazExpert = [Collections.IDictionary] $tradeByPath[$topazExpertPath]
    $topazMaster = [Collections.IDictionary] $tradeByPath[$topazMasterPath]
    Assert-StackDigest $topazExpert['wants'] '433B8B7468F7CBE245D1A600793528D01151464159963943F059888BB7987132' 'Expert Toolsmith first input'
    Assert-StackDigest $topazExpert['additional_wants'] '805638FDAAACF1967785C6B270A9EB4EB524EC6191D95C7C5E43068D4A3231B2' 'Expert Toolsmith Topaz input'
    Assert-StackDigest $topazExpert['gives'] '940D10BA36D6FB02B4330DF4BA98D8E9377A6D585CB63CD42E2953FD58EE9DE7' 'Expert Toolsmith Topaz Earrings output'
    Assert-Equal (Get-StackCount $topazExpert['wants']) 14 'Expert Toolsmith emerald input count'
    Assert-Equal (Get-StackCount $topazExpert['additional_wants']) 1 'Expert Toolsmith Topaz input count'
    Assert-Equal (Get-StackCount $topazExpert['gives']) 1 'Expert Toolsmith Topaz Earrings output count'
    Assert-Equal $topazExpert['max_uses'] 999 'Expert Toolsmith max uses'
    Assert-Equal $topazExpert['xp'] 200 'Expert Toolsmith XP'

    Assert-StackDigest $topazMaster['wants'] '805638FDAAACF1967785C6B270A9EB4EB524EC6191D95C7C5E43068D4A3231B2' 'Master Toolsmith Topaz input'
    Assert-StackDigest $topazMaster['gives'] 'B3A136D3CB7773B1F7123C1BF32AD39968791E20DB5F1F27BC1CB83621A0CFA5' 'Master Toolsmith emerald output'
    Assert-Equal (Get-StackCount $topazMaster['wants']) 1 'Master Toolsmith Topaz input count'
    Assert-Equal (Get-StackCount $topazMaster['gives']) 10 'Master Toolsmith emerald output count'
    Assert-Equal $topazMaster['max_uses'] 999 'Master Toolsmith max uses'
    Assert-Equal $topazMaster['xp'] 0 'Master Toolsmith XP'

    Assert-True $lootAndTradeModels.Contains('minecraft:topaz') 'Topaz was not classified as a loot-and-trade non-recipe identity.'
    Assert-True $tradeOnlyModels.Contains('minecraft:topaz_earrings') 'Topaz Earrings were not classified as trade-only.'

    $topazLootId = 'minecraft:kleis_items/topaz'
    Assert-True $lootById.ContainsKey($topazLootId) 'Missing nested Topaz loot table.'
    $topazLoot = $lootById[$topazLootId]
    Assert-True ($topazLoot.Models -ccontains 'minecraft:topaz') 'Nested Topaz loot table does not produce the Topaz identity.'
    $topazLootStacks = @(
        $topazLoot.ComponentStacks | Where-Object {
            @(Get-ItemModels $_.Components) -ccontains 'minecraft:topaz'
        }
    )
    Assert-Equal $topazLootStacks.Count 1 'Nested Topaz loot stack count'
    $topazLootStack = [ordered] @{
        id = $topazLootStacks[0].Id
        components = $topazLootStacks[0].Components
    }
    Assert-StackDigest $topazLootStack '805638FDAAACF1967785C6B270A9EB4EB524EC6191D95C7C5E43068D4A3231B2' 'Nested Topaz loot output'

    $topazAncestors = New-OrdinalSet
    [void] $topazAncestors.Add($topazLootId)
    $pendingLootIds = [Collections.Generic.Queue[string]]::new()
    $pendingLootIds.Enqueue($topazLootId)
    while ($pendingLootIds.Count -gt 0) {
        $childId = $pendingLootIds.Dequeue()
        foreach ($record in $lootRecords) {
            if ($record.References -ccontains $childId -and $topazAncestors.Add($record.Id)) {
                $pendingLootIds.Enqueue($record.Id)
            }
        }
    }
    Assert-True $topazAncestors.Contains('minecraft:chests/ancient_city') 'Topaz nested loot did not trace to the Ancient City chest loot table.'

    $gemCases = @(
        [pscustomobject]@{
            Gem = 'opal'
            Equipment = 'opal_earrings'
            Level = 1
            ParentLoot = 'minecraft:entities/elder_guardian'
        },
        [pscustomobject]@{
            Gem = 'ruby'
            Equipment = 'ruby_circlet'
            Level = 2
            ParentLoot = 'minecraft:chests/bastion_treasure'
        },
        [pscustomobject]@{
            Gem = 'amber'
            Equipment = 'amber_earrings'
            Level = 3
            ParentLoot = 'minecraft:archaeology/trail_ruins_rare'
        }
    )
    foreach ($case in $gemCases) {
        $gemModel = "minecraft:$($case.Gem)"
        $equipmentModel = "minecraft:$($case.Equipment)"
        Assert-True $lootAndTradeModels.Contains($gemModel) "$gemModel was not independently classified as loot-and-trade non-recipe data."
        Assert-True $tradeOnlyModels.Contains($equipmentModel) "$equipmentModel was not independently classified as trade-only."

        $equipmentTradePath = "data/minecraft/villager_trade/toolsmith/$($case.Level)/$($case.Equipment).json"
        $saleTradePath = "data/minecraft/villager_trade/toolsmith/5/sell_$($case.Gem).json"
        Assert-True $tradeByPath.ContainsKey($equipmentTradePath) "Missing $($case.Equipment) Toolsmith trade."
        Assert-True $tradeByPath.ContainsKey($saleTradePath) "Missing $($case.Gem) Master Toolsmith sale trade."
        $equipmentTrade = [Collections.IDictionary] $tradeByPath[$equipmentTradePath]
        $saleTrade = [Collections.IDictionary] $tradeByPath[$saleTradePath]
        Assert-True (@(Get-ItemModels $equipmentTrade) -ccontains $gemModel) "$equipmentTradePath does not consume the exact $gemModel identity."
        Assert-True (@(Get-ItemModels $equipmentTrade['gives']) -ccontains $equipmentModel) "$equipmentTradePath does not output the exact $equipmentModel identity."
        Assert-True (@(Get-ItemModels $saleTrade['wants']) -ccontains $gemModel) "$saleTradePath does not consume the exact $gemModel identity."
        Assert-Equal $saleTrade['gives']['id'] 'minecraft:emerald' "$($case.Gem) sale output item"

        $gemLootId = "minecraft:kleis_items/$($case.Gem)"
        Assert-True $lootById.ContainsKey($gemLootId) "Missing nested $($case.Gem) loot table."
        Assert-True ($lootById[$gemLootId].Models -ccontains $gemModel) "Nested $($case.Gem) loot table does not produce $gemModel."
        $parents = @($lootRecords | Where-Object { $_.References -ccontains $gemLootId } | ForEach-Object Id)
        Assert-True ($parents -ccontains $case.ParentLoot) "$gemModel did not independently trace to $($case.ParentLoot)."
    }

    $fishTradePath = 'data/minecraft/villager_trade/fisherman/1/alaska_blackfish.json'
    Assert-True $tradeByPath.ContainsKey($fishTradePath) 'Missing representative custom-fish trade.'
    $fishTrade = [Collections.IDictionary] $tradeByPath[$fishTradePath]
    Assert-True $lootAndTradeModels.Contains('alaska_blackfish') 'Representative custom fish was not classified as loot-and-trade non-recipe data.'
    Assert-Equal $fishTrade['wants']['id'] 'minecraft:cod' 'Custom-fish trade input base item'
    Assert-Equal (Get-StackCount $fishTrade['wants']) 12 'Custom-fish trade input count'
    Assert-Equal $fishTrade['wants']['components']['minecraft:item_model'] 'alaska_blackfish' 'Custom-fish trade input identity'
    Assert-Equal $fishTrade['gives']['id'] 'minecraft:emerald' 'Custom-fish trade output base item'
    Assert-Equal (Get-StackCount $fishTrade['gives']) 1 'Custom-fish trade output count'

    $tradeOnlyPath = 'data/minecraft/villager_trade/toolsmith/4/bronze_laurel.json'
    Assert-True $tradeByPath.ContainsKey($tradeOnlyPath) 'Missing representative trade-only Bronze Laurel trade.'
    Assert-True $tradeOnlyModels.Contains('minecraft:bronze_laurel') 'Representative Bronze Laurel was not classified as trade-only.'
    Assert-True (@(Get-ItemModels $tradeByPath[$tradeOnlyPath]['gives']) -ccontains 'minecraft:bronze_laurel') 'Bronze Laurel trade does not output its exact component identity.'

    $lootOnlyPath = 'data/minecraft/loot_table/chests/equipment/special_compass.json'
    Assert-True $lootByPath.ContainsKey($lootOnlyPath) 'Missing representative loot-only special-compass table.'
    Assert-True $lootOnlyModels.Contains('minecraft:titanium_compass') 'Representative Titanium Compass was not classified as loot-only.'
    Assert-True ($lootByPath[$lootOnlyPath].Models -ccontains 'minecraft:titanium_compass') 'Special-compass loot table does not output the exact Titanium Compass identity.'

    # Pure fixture transform mirroring the server scanner's supported exact subset.
    $fixtureRecipeKeys = New-OrdinalSet
    $fixtureStacksByKey = [Collections.Generic.Dictionary[string, object]]::new([StringComparer]::Ordinal)
    foreach ($record in $recipeRecords) {
        if ($record.Json['result'] -is [Collections.IDictionary]) {
            $key = Get-FixtureStackKey $record.Json['result']
            if ($null -ne $key) {
                [void] $fixtureRecipeKeys.Add($key)
                $fixtureStacksByKey[$key] = ConvertTo-FixtureStack $record.Json['result']
            }
        }
    }

    $fixtureTradeKeys = New-OrdinalSet
    $fixtureTradePaths = New-OrdinalSet
    $fixtureConditionalTradePaths = New-OrdinalSet
    foreach ($record in $tradeRecords) {
        $json = [Collections.IDictionary] $record.Json
        if (@(Get-JsonArrayValues $json['given_item_modifiers']).Count -gt 0) {
            continue
        }
        if ($json['wants'] -isnot [Collections.IDictionary] -or
            $json['gives'] -isnot [Collections.IDictionary]) {
            continue
        }
        $hasSecond = $json.Contains('additional_wants')
        if ($hasSecond -and $json['additional_wants'] -isnot [Collections.IDictionary]) {
            continue
        }
        $stacks = @($json['wants'], $json['gives'])
        if ($hasSecond) {
            $stacks += $json['additional_wants']
        }
        if (-not ($stacks | Where-Object {
                    $_['components'] -is [Collections.IDictionary] -and
                    $_['components'].Count -gt 0
                })) {
            continue
        }
        [void] $fixtureTradePaths.Add($record.Path)
        if ($json.Contains('merchant_predicate')) {
            [void] $fixtureConditionalTradePaths.Add($record.Path)
        }
        foreach ($stack in $stacks) {
            if ($stack['components'] -is [Collections.IDictionary] -and $stack['components'].Count -gt 0) {
                $key = Get-FixtureStackKey $stack
                [void] $fixtureTradeKeys.Add($key)
                $fixtureStacksByKey[$key] = ConvertTo-FixtureStack $stack
            }
        }
    }
    Assert-Equal $fixtureTradePaths.Count 175 'Transformed exact trade display count'
    Assert-Equal $fixtureConditionalTradePaths.Count 12 'Transformed merchant-conditional trade count'
    Assert-True $fixtureTradePaths.Contains($topazExpertPath) 'Transformed trades lost the Expert Toolsmith Topaz page.'
    Assert-True $fixtureTradePaths.Contains($topazMasterPath) 'Transformed trades lost the Master Toolsmith Topaz page.'
    Assert-True $fixtureConditionalTradePaths.Contains('data/minecraft/villager_trade/farmer/3/baby_cold_cow.json') 'Cold-cow crate no longer carries its merchant-condition marker.'

    $fixtureTradeNonRecipeKeys = Copy-OrdinalSet $fixtureTradeKeys
    $fixtureTradeNonRecipeKeys.ExceptWith($fixtureRecipeKeys)
    Assert-Equal $fixtureTradeKeys.Count 211 'Transformed supported trade identity count'
    Assert-Equal $fixtureTradeNonRecipeKeys.Count 210 'Transformed non-recipe trade identity count'

    $fixtureLootNodes = [Collections.Generic.Dictionary[string, object]]::new([StringComparer]::Ordinal)
    $fixtureReferencedLootIds = New-OrdinalSet
    $fixtureDirectLootKeys = New-OrdinalSet
    $fixtureLootKeysByModel = [Collections.Generic.Dictionary[string, string]]::new([StringComparer]::Ordinal)
    foreach ($record in $lootRecords) {
        if ($record.Json -isnot [Collections.IDictionary]) {
            continue
        }
        $node = New-FixtureLootNode $record.Json
        $fixtureLootNodes.Add($record.Id, $node)
        foreach ($reference in $record.References) {
            [void] $fixtureReferencedLootIds.Add((Normalize-Identifier $reference))
        }
        foreach ($item in $node.DirectItems.Values) {
            [void] $fixtureDirectLootKeys.Add($item.Key)
            $fixtureStacksByKey[$item.Key] = $item.Stack
            $model = $item.Stack['components']['minecraft:item_model']
            if ($model -is [string] -and -not $fixtureLootKeysByModel.ContainsKey($model)) {
                $fixtureLootKeysByModel.Add($model, $item.Key)
            }
        }
    }
    Assert-Equal $fixtureDirectLootKeys.Count 108 'Direct parseable stable loot identity count'

    $fixtureAcceptedSources = New-OrdinalSet
    $fixtureSourcesWithAcquisitions = New-OrdinalSet
    $fixtureLootNonRecipeKeys = New-OrdinalSet
    $fixtureTopazSources = New-OrdinalSet
    $fixtureVariableDurabilityKeys = New-OrdinalSet
    $fixtureAcquisitionCount = 0
    $topazFixtureKey = Get-FixtureStackKey $topazExpert['additional_wants']
    foreach ($sourceId in @($fixtureLootNodes.Keys | Sort-Object)) {
        if (-not (Test-FixtureAcquisitionSource -TableId $sourceId -Referenced $fixtureReferencedLootIds)) {
            continue
        }
        [void] $fixtureAcceptedSources.Add($sourceId)
        $resolvedResult = Resolve-FixtureLoot -TableId $sourceId -Nodes $fixtureLootNodes -Visiting (New-OrdinalSet)
        [Collections.Generic.Dictionary[string, object]] $resolved = $resolvedResult.Map
        $sourceCount = 0
        foreach ($key in $resolved.Keys) {
            if ($fixtureRecipeKeys.Contains($key)) {
                continue
            }
            $sourceCount++
            $fixtureAcquisitionCount++
            [void] $fixtureLootNonRecipeKeys.Add($key)
            if ($resolved[$key].DurabilityVaries) {
                [void] $fixtureVariableDurabilityKeys.Add($key)
            }
            if ($key -ceq $topazFixtureKey) {
                [void] $fixtureTopazSources.Add($sourceId)
            }
        }
        if ($sourceCount -gt 0) {
            [void] $fixtureSourcesWithAcquisitions.Add($sourceId)
        }
    }
    Assert-Equal $fixtureAcceptedSources.Count 179 'Accepted semantic loot-root count'
    Assert-Equal $fixtureSourcesWithAcquisitions.Count 58 'Loot sources with non-recipe acquisitions'
    Assert-Equal $fixtureAcquisitionCount 249 'Transformed acquisition descriptor count'
    Assert-Equal $fixtureLootNonRecipeKeys.Count 91 'Reachable non-recipe loot identity count'
    Assert-Equal ($fixtureTopazSources | ConvertTo-Json -Compress) '"minecraft:chests/ancient_city"' 'Transformed Topaz acquisition source'

    foreach ($suppressedSource in @(
            'minecraft:chests/equipment/iron_tools',
            'minecraft:chests/fishing_buried_treasure',
            'minecraft:gameplay/fishing/fish/axolotl',
            'minecraft:gameplay/fishing/fish/blind_cave_fish',
            'minecraft:gameplay/fishing/fish/blind_minnow'
        )) {
        Assert-True (-not $fixtureAcceptedSources.Contains($suppressedSource)) "Internal/orphan loot root leaked: $suppressedSource"
    }
    foreach ($model in 'minecraft:fox_pelt', 'minecraft:compound_bow', 'minecraft:titanium_compass') {
        Assert-True $fixtureLootKeysByModel.ContainsKey($model) "Supported loot transform lost $model."
        Assert-True $fixtureLootNonRecipeKeys.Contains($fixtureLootKeysByModel[$model]) "$model is not reachable as a non-recipe acquisition."
    }
    Assert-True $fixtureVariableDurabilityKeys.Contains($fixtureLootKeysByModel['minecraft:compound_bow']) 'Compound Bow variable durability qualifier was lost.'

    $fixtureLootAndTradeKeys = Copy-OrdinalSet $fixtureTradeNonRecipeKeys
    $fixtureLootAndTradeKeys.IntersectWith($fixtureLootNonRecipeKeys)
    $fixtureTradeOnlyKeys = Copy-OrdinalSet $fixtureTradeNonRecipeKeys
    $fixtureTradeOnlyKeys.ExceptWith($fixtureLootNonRecipeKeys)
    $fixtureLootOnlyKeys = Copy-OrdinalSet $fixtureLootNonRecipeKeys
    $fixtureLootOnlyKeys.ExceptWith($fixtureTradeNonRecipeKeys)
    $fixtureIngredientUnion = Copy-OrdinalSet $fixtureTradeNonRecipeKeys
    $fixtureIngredientUnion.UnionWith($fixtureLootNonRecipeKeys)
    Assert-Equal $fixtureLootAndTradeKeys.Count 61 'Exact loot-and-trade ingredient identity count'
    Assert-Equal $fixtureTradeOnlyKeys.Count 149 'Exact trade-only ingredient identity count'
    Assert-Equal $fixtureLootOnlyKeys.Count 30 'Exact loot-only ingredient identity count'
    Assert-Equal $fixtureIngredientUnion.Count 240 'Exact non-recipe ingredient union count'

    $jeiOwnedBaseIds = New-OrdinalSet
    foreach ($base in @(
            'minecraft:tipped_arrow',
            'minecraft:potion',
            'minecraft:splash_potion',
            'minecraft:lingering_potion',
            'minecraft:enchanted_book',
            'minecraft:light',
            'minecraft:painting',
            'minecraft:goat_horn',
            'minecraft:firework_rocket',
            'minecraft:firework_star',
            'minecraft:suspicious_stew',
            'minecraft:ominous_bottle',
            'minecraft:shield',
            'minecraft:decorated_pot'
        )) {
        [void] $jeiOwnedBaseIds.Add($base)
    }
    $fixtureOwnedKeys = New-OrdinalSet
    $fixtureOwnedCounts = [Collections.Generic.Dictionary[string, int]]::new([StringComparer]::Ordinal)
    foreach ($key in $fixtureIngredientUnion) {
        Assert-True $fixtureStacksByKey.ContainsKey($key) "Missing fixture stack for exact identity $key."
        $baseId = [string] $fixtureStacksByKey[$key]['id']
        if ($jeiOwnedBaseIds.Contains($baseId)) {
            [void] $fixtureOwnedKeys.Add($key)
            $fixtureOwnedCounts[$baseId] = if ($fixtureOwnedCounts.ContainsKey($baseId)) {
                $fixtureOwnedCounts[$baseId] + 1
            } else {
                1
            }
        }
    }
    Assert-Equal $fixtureOwnedKeys.Count 19 'Exact ingredients requiring the JEI-owned-base bridge'
    Assert-Equal ($fixtureIngredientUnion.Count - $fixtureOwnedKeys.Count) 221 'Exact ingredients safe on companion-owned vanilla ItemStack subtypes'
    foreach ($expected in @(
            @{ Id = 'minecraft:enchanted_book'; Count = 5 },
            @{ Id = 'minecraft:goat_horn'; Count = 2 },
            @{ Id = 'minecraft:splash_potion'; Count = 10 },
            @{ Id = 'minecraft:tipped_arrow'; Count = 2 }
        )) {
        Assert-True $fixtureOwnedCounts.ContainsKey($expected.Id) "Missing JEI-owned-base identity group $($expected.Id)."
        Assert-Equal $fixtureOwnedCounts[$expected.Id] $expected.Count "JEI-owned-base identity count for $($expected.Id)"
    }
    Assert-Equal $fixtureOwnedCounts.Count 4 'Unexpected JEI-owned base in exact non-recipe ingredient union'

    $componentItemIds = New-OrdinalSet
    $componentTypeIds = New-OrdinalSet
    foreach ($record in $recipeRecords) {
        Add-ComponentStackToInventory $record.Json['result'] $componentItemIds $componentTypeIds
    }
    foreach ($record in $tradeRecords) {
        foreach ($field in 'wants', 'additional_wants', 'gives') {
            if ($record.Json[$field] -is [Collections.IDictionary]) {
                Add-ComponentStackToInventory $record.Json[$field] $componentItemIds $componentTypeIds
            }
        }
    }
    foreach ($record in $lootRecords) {
        foreach ($stack in $record.ComponentStacks) {
            Add-LootStackToInventory $stack $componentItemIds $componentTypeIds
        }
    }

    Assert-Equal $componentItemIds.Count 157 'Normalized component-bearing base item count'
    Assert-Equal $componentTypeIds.Count 30 'Authored logical component type count'
    Assert-True $componentTypeIds.Contains('minecraft:damage') 'Expected authored transient damage component was not observed in the reference.'

    $expectedIdentityComponentIds = Copy-OrdinalSet $componentTypeIds
    [void] $expectedIdentityComponentIds.Remove('minecraft:damage')
    $componentItemManifestPath = Join-Path $projectRoot 'src\main\resources\matcha_jei_integration\component-item-ids.txt'
    $identityComponentManifestPath = Join-Path $projectRoot 'src\main\resources\matcha_jei_integration\identity-component-ids.txt'
    $componentItemManifest = Read-ManifestSet $componentItemManifestPath
    $identityComponentManifest = Read-ManifestSet $identityComponentManifestPath
    Assert-SetEqual $componentItemManifest $componentItemIds 'Component item manifest'
    Assert-SetEqual $identityComponentManifest $expectedIdentityComponentIds 'Identity component manifest'
    Assert-Equal $componentItemManifest.Count 157 'Component item manifest count'
    Assert-Equal $identityComponentManifest.Count 29 'Identity component manifest count'
    Assert-True (-not $identityComponentManifest.Contains('minecraft:damage')) 'Transient minecraft:damage must not participate in JEI subtype identity.'

    $javaFiles = @(Get-ChildItem -LiteralPath (Join-Path $projectRoot 'src\main\java') -Recurse -File -Filter '*.java')
    $javaSources = @(
        foreach ($file in $javaFiles) {
            "`n// FILE: $($file.FullName)`n"
            Get-Content -LiteralPath $file.FullName -Raw -Encoding UTF8
        }
    ) -join "`n"
    $pluginPath = Join-Path $projectRoot 'src\main\java\dev\resivore\matchajei\client\MatchaJeiPlugin.java'
    $scannerPath = Join-Path $projectRoot 'src\main\java\dev\resivore\matchajei\server\MatchaDataScanner.java'
    $runtimeDataPath = Join-Path $projectRoot 'src\main\java\dev\resivore\matchajei\client\MatchaJeiRuntimeData.java'
    $exactIngredientPath = Join-Path $projectRoot 'src\main\java\dev\resivore\matchajei\client\MatchaExactIngredient.java'
    $exactRecipeBridgePath = Join-Path $projectRoot 'src\main\java\dev\resivore\matchajei\client\MatchaExactRecipeBridge.java'
    $exactCatalogPath = Join-Path $projectRoot 'src\main\java\dev\resivore\matchajei\data\MatchaExactCatalog.java'
    $creativeCatalogPath = Join-Path $projectRoot 'src\main\java\dev\resivore\matchajei\client\MatchaCreativeCatalog.java'
    $creativeSearchEntriesPath = Join-Path $projectRoot 'src\main\java\dev\resivore\matchajei\client\MatchaCreativeSearchEntries.java'
    $clientDataPath = Join-Path $projectRoot 'src\main\java\dev\resivore\matchajei\client\MatchaClientData.java'
    $payloadPath = Join-Path $projectRoot 'src\main\java\dev\resivore\matchajei\network\MatchaJeiDataPayload.java'
    $initializerPath = Join-Path $projectRoot 'src\main\java\dev\resivore\matchajei\MatchaJeiIntegration.java'
    $acquisitionCategoryPath = Join-Path $projectRoot 'src\main\java\dev\resivore\matchajei\client\category\MatchaAcquisitionCategory.java'
    $tradeCategoryPath = Join-Path $projectRoot 'src\main\java\dev\resivore\matchajei\client\category\MatchaVillagerTradeCategory.java'
    Assert-True (Test-Path -LiteralPath $pluginPath -PathType Leaf) 'Missing accepted Matcha JEI plugin source.'
    $pluginSource = Get-Content -LiteralPath $pluginPath -Raw -Encoding UTF8
    $scannerSource = Get-Content -LiteralPath $scannerPath -Raw -Encoding UTF8
    $runtimeDataSource = Get-Content -LiteralPath $runtimeDataPath -Raw -Encoding UTF8
    $exactIngredientSource = Get-Content -LiteralPath $exactIngredientPath -Raw -Encoding UTF8
    $exactRecipeBridgeSource = Get-Content -LiteralPath $exactRecipeBridgePath -Raw -Encoding UTF8
    $exactCatalogSource = Get-Content -LiteralPath $exactCatalogPath -Raw -Encoding UTF8
    $creativeCatalogSource = Get-Content -LiteralPath $creativeCatalogPath -Raw -Encoding UTF8
    $creativeSearchEntriesSource = Get-Content -LiteralPath $creativeSearchEntriesPath -Raw -Encoding UTF8
    $clientDataSource = Get-Content -LiteralPath $clientDataPath -Raw -Encoding UTF8
    $payloadSource = Get-Content -LiteralPath $payloadPath -Raw -Encoding UTF8
    $initializerSource = Get-Content -LiteralPath $initializerPath -Raw -Encoding UTF8
    $acquisitionCategorySource = Get-Content -LiteralPath $acquisitionCategoryPath -Raw -Encoding UTF8
    $tradeCategorySource = Get-Content -LiteralPath $tradeCategoryPath -Raw -Encoding UTF8

    Assert-Matches $scannerSource 'server\.getRecipeManager\(\)\.getRecipes\(\)' 'Effective recipe discovery is not reading the resolved server recipe manager.'
    Assert-Matches $scannerSource 'SlotDisplayContext\.REGISTRIES' 'Effective recipe discovery is not using the supported 26.2 recipe-display context.'
    Assert-Matches $scannerSource 'display\.result\(\)\.resolveForStacks\(displayContext\)' 'Effective recipe discovery is not resolving display outputs.'
    Assert-Matches $scannerSource 'MatchaExactCatalog\.selectRecipeOutputs\(rawFallbacks, resolvedOutputs\)' 'Resolved recipe outputs do not supersede raw Matcha JSON fallbacks.'
    Assert-Matches $scannerSource 'filter\(identity -> isComponentStack\(identity\.stack\(\)\)\).*?addCatalogEntry\(catalog, identity\)' 'Component-bearing effective recipe outputs are not added to the shared catalog.'
    Assert-Matches $scannerSource 'revisionOf\(recipes, state\.trades, lootTables, effectiveRecipeOutputs\)' 'The synchronized catalog revision ignores effective outputs.'
    Assert-Matches $scannerSource 'effectiveRecipeOutputs\.entrySet\(\)' 'The synchronized catalog revision does not fingerprint effective full-stack outputs.'
    Assert-Matches $exactCatalogSource 'known but non-representable recipe deliberately' 'A known unrepresentable resolved recipe could incorrectly revive its stale raw output.'
    Assert-Matches $exactCatalogSource 'ItemStack\.isSameItemSameComponents' 'Shared catalog deduplication no longer uses full component identity.'
    Assert-Matches $payloadSource 'List<ItemStack> catalog' 'The synchronized payload no longer carries the shared exact catalog.'
    Assert-Matches $runtimeDataSource 'payload\.catalog\(\)' 'JEI runtime data is not consuming the shared catalog.'
    Assert-True ($pluginSource -cnotmatch 'addIngredientsAtRuntime\s*\(\s*VanillaTypes\.ITEM_STACK\s*,\s*outputs\s*\)') 'C3 client-side raw recipe-output injection must not coexist with the resolved catalog.'

    Assert-True (-not [regex]::IsMatch(
            $javaSources,
            'addRecipes\s*\(\s*RecipeTypes\.(?:CRAFTING|STONECUTTING|SMELTING|SMOKING|BLASTING|CAMPFIRE_COOKING|SMITHING)',
            [Text.RegularExpressions.RegexOptions]::Singleline
        )) 'Matcha recipes must remain owned by JEI vanilla categories; fake vanilla recipe registration was found.'
    Assert-True ($javaSources -cnotmatch 'new\s+RecipeHolder\s*(?:<[^>]+>)?\s*\(') 'Fabricated RecipeHolder construction was found; non-recipe relationships must not be fake recipes.'

    Assert-Matches $pluginSource 'MatchaComponentInventory\.itemIds\s*\(\s*\)' 'Subtype registration is not using the audited component-item manifest.'
    Assert-Matches $pluginSource 'MatchaComponentInventory\.identityComponentTypes\s*\(\s*\)' 'Subtype registration is not using the audited identity-component manifest.'
    $ownedMatch = [regex]::Match(
        $pluginSource,
        'JEI_OWNED_SUBTYPE_ITEMS\s*=\s*Set\.of\s*\((?<items>.*?)\);',
        [Text.RegularExpressions.RegexOptions]::Singleline
    )
    Assert-True $ownedMatch.Success 'JEI-owned subtype exclusion set is missing.'
    $ownedPaths = New-OrdinalSet
    foreach ($itemMatch in [regex]::Matches($ownedMatch.Groups['items'].Value, '"(?<item>[^"]+)"')) {
        $item = $itemMatch.Groups['item'].Value
        if ($item.Contains(':')) {
            $item = $item.Substring($item.IndexOf(':') + 1)
        }
        [void] $ownedPaths.Add($item)
    }
    foreach ($ownedPath in 'enchanted_book', 'goat_horn', 'potion', 'shield', 'splash_potion', 'tipped_arrow') {
        Assert-True $ownedPaths.Contains($ownedPath) "JEI-owned subtype safeguard is missing $ownedPath."
    }
    $skipMatch = [regex]::Match(
        $pluginSource,
        'if\s*\(\s*JEI_OWNED_SUBTYPE_ITEMS\.contains\s*\([^)]*\)\s*\)\s*\{\s*continue\s*;\s*\}',
        [Text.RegularExpressions.RegexOptions]::Singleline
    )
    Assert-True $skipMatch.Success 'JEI-owned subtype items are not skipped before companion registration.'
    $registrationIndex = $pluginSource.IndexOf('registration.registerFromDataComponentTypes', [StringComparison]::Ordinal)
    Assert-True ($registrationIndex -gt $skipMatch.Index) 'JEI-owned subtype exclusion must execute before registration.'

    Assert-Matches $javaSources 'FileToIdConverter\.registry\s*\(\s*Registries\.VILLAGER_TRADE\s*\)' 'Villager-trade discovery is not based on the authoritative loaded registry resource path.'
    Assert-Matches $javaSources 'FileToIdConverter\.registry\s*\(\s*Registries\.LOOT_TABLE\s*\)' 'Loot discovery is not based on the authoritative loaded registry resource path.'
    Assert-Matches $javaSources '\.sourcePackId\s*\(\s*\)' 'Discovery does not retain source-pack ownership, so unrelated vanilla minecraft resources cannot be excluded safely.'
    Assert-Matches $javaSources 'MatchaNamespaces\.contains\s*\(\s*[A-Z_]+\.fileToId\s*\(.*?\)\.getNamespace\s*\(\s*\)' 'Discovery does not establish Matcha source-pack ownership from accepted loaded recipe namespaces.'
    Assert-Matches $javaSources 'matchaPackIds\.contains\s*\(.*?sourcePackId\s*\(\s*\)\s*\)' 'Trade and loot resources are not constrained to the winning Matcha source pack.'
    Assert-Matches $scannerSource 'PackMetadataSection\.SERVER_TYPE' 'Matcha pack ownership is not authenticated through loaded server-pack metadata.'
    Assert-Matches $scannerSource 'klei''s matcha flavoured' 'The exact Matcha metadata description marker is missing.'
    Assert-Matches $scannerSource 'metadataMatches\.retainAll\s*\(\s*recipeOwners\s*\)' 'Matcha metadata ownership is not cross-checked against the accepted recipe namespace fingerprint.'
    Assert-Matches $scannerSource 'registries\.lookup\s*\(\s*Registries\.TRADE_SET\s*\)' 'Startup trade resources are not resolved through authoritative loaded trade sets.'
    Assert-Matches $scannerSource 'registries\.lookup\s*\(\s*Registries\.VILLAGER_PROFESSION\s*\)' 'Startup trade levels are not resolved through authoritative loaded villager professions.'
    Assert-Matches $scannerSource 'getTrades\s*\(\s*\)\.stream\s*\(\s*\).*?unwrapKey\s*\(\s*\)' 'Trade resources are not constrained to actual holder assignments from loaded trade sets.'
    Assert-Matches $initializerSource 'SERVER_STARTED\.register\s*\(\s*MatchaDataScanner::initialize\s*\)' 'The immutable server-lifetime trade snapshot is not captured at startup.'
    Assert-Matches $initializerSource 'START_DATA_PACK_RELOAD\.register.*?invalidateReloadable' 'Reloadable recipe/loot data is not invalidated independently of the startup trade snapshot.'

    Assert-Matches $scannerSource 'document\.json\(\)\.get\s*\(\s*"result"\s*\)' 'Recipe subtraction is not limited to actual recipe result stacks.'
    Assert-Matches $exactCatalogSource 'stack\.copyWithCount\s*\(\s*1\s*\)' 'Canonical exact identities are not count-normalized.'
    Assert-Matches $scannerSource 'return\s+!recipeIdentities\.contains\s*\(\s*identity\.key\(\)\s*\)' 'Non-recipe subtraction is not comparing canonical full-stack identities.'
    Assert-True ($scannerSource -cnotmatch 'DataComponents\.ITEM_MODEL') 'Runtime non-recipe subtraction must not collapse full stacks to item_model.'
    Assert-Matches $scannerSource 'hasNonEmptyArray\s*\(\s*json\s*,\s*"given_item_modifiers"\s*\).*?return\s*;.*?parseTemplate\s*\(\s*json\.get\s*\(\s*"wants"' 'Contextual map modifiers are not rejected before any inexact ingredient can leak.'
    Assert-Matches $scannerSource 'collectLootTableReferences\s*\(\s*table\s*,\s*allReferences\s*\)' 'Function-owned loot references are not collected for internal-root suppression.'
    foreach ($sourceFamily in 'harvest/', 'pots/', 'gameplay/piglin_bartering', 'gameplay/fishing') {
        Assert-True $scannerSource.Contains("`"$sourceFamily`"") "Acquisition source boundary is missing $sourceFamily."
    }
    Assert-Matches $scannerSource 'chance and conditions vary' 'Loot acquisition text does not qualify predicates, luck, or nested chance.'

    Assert-Matches $runtimeDataSource 'UidContext\.Ingredient' 'JEI runtime ownership does not compare exact ingredient UIDs.'
    Assert-Matches $runtimeDataSource 'knownUids\.add\s*\(' 'JEI runtime ownership does not isolate newly introduced ingredient UIDs.'
    Assert-Matches $runtimeDataSource 'active\.introducedIngredients\s*\(\s*\)' 'Reload removal is not limited to ingredients introduced by this bridge.'
    Assert-Matches $pluginSource 'registerIngredients\s*\(\s*IModIngredientRegistration' 'The JEI-owned-base exact ingredient registration phase is missing.'
    Assert-Matches $pluginSource 'registration\.register\s*\(\s*MatchaExactIngredient\.TYPE\s*,\s*List\.of\s*\(\s*\)' 'The JEI-owned-base exact ingredient type is not actually registered.'
    Assert-Matches $pluginSource 'visibleSlot\.add\s*\(\s*stack\s*\).*?if\s*\(\s*!usesJeiOwnedSubtype\s*\(\s*stack\s*\)\s*\)' 'Category slots no longer preserve the visible vanilla ItemStack path before exact bridging.'
    Assert-True ($pluginSource -cnotmatch 'visibleSlot\.add\s*\(\s*MatchaExactIngredient\.TYPE') 'An exact wrapper became the visible slot ingredient and would break ordinary vanilla recipe navigation.'
    Assert-Matches $pluginSource 'addInvisibleIngredients\s*\(\s*role\s*\).*?MatchaExactIngredient\.TYPE' 'Visible vanilla slots are not linked to exact JEI-owned-base identities.'
    Assert-Matches $pluginSource 'createFocusLink\s*\(\s*visibleSlot\s*,\s*exactIngredient\s*\)' 'Exact and visible ingredient focus navigation is not linked.'
    Assert-Matches $pluginSource 'registration\.addRecipeManagerPlugin\s*\(\s*new\s+MatchaExactRecipeBridge' 'Exact Matcha recipe outputs are not bridged back to JEI vanilla recipe categories.'
    Assert-Matches $exactRecipeBridgeSource 'MatchaJeiRuntimeData\.findVanillaRecipes\(' 'Exact recipe output navigation does not delegate to existing vanilla recipes.'
    Assert-Matches $runtimeDataSource 'VanillaTypes\.ITEM_STACK.*?createRecipeLookup\(recipeType\)' "Exact recipe output navigation does not query JEI's real vanilla recipe category."
    Assert-Matches $runtimeDataSource '!\s*MatchaJeiPlugin\.usesJeiOwnedSubtype\s*\(' 'Vanilla runtime ingredients are not partitioned away from JEI-owned subtype bases.'
    Assert-Matches $runtimeDataSource '\.filter\s*\(\s*MatchaJeiPlugin::usesJeiOwnedSubtype\s*\)' 'JEI-owned-base candidates are not positively selected for the exact bridge.'
    Assert-Matches $runtimeDataSource 'MatchaExactIngredient\.TYPE\s*,\s*exactCandidates' 'JEI-owned-base exact ingredients are not added through the dedicated type.'
    Assert-Matches $runtimeDataSource 'removeIngredientsAtRuntime\s*\(\s*MatchaExactIngredient\.TYPE\s*,\s*introduced\.exact\s*\(\s*\)\s*\)' 'Reload replacement does not remove only the exact wrapper ingredients it introduced.'
    Assert-Matches $exactIngredientSource 'getUid\s*\(\s*MatchaExactIngredient\s+ingredient\s*,\s*UidContext\s+context\s*\).*?return\s+ingredient\s*;' 'Exact wrapper UID no longer returns the full-component wrapper identity.'
    Assert-Matches $exactIngredientSource 'ItemStack\.isSameItemSameComponents\s*\(' 'Exact wrapper UID equality no longer compares the full ItemStack component identity.'
    Assert-Matches $exactIngredientSource 'ItemStack\.hashItemAndComponents\s*\(' 'Exact wrapper UID hashing no longer covers the full ItemStack component identity.'
    Assert-Equal ([regex]::Matches($tradeCategorySource, 'MatchaJeiPlugin\.addExactAwareIngredient\s*\(').Count) 3 'Trade category exact-aware slot call count'
    Assert-Equal ([regex]::Matches($acquisitionCategorySource, 'MatchaJeiPlugin\.addExactAwareIngredient\s*\(').Count) 1 'Acquisition category exact-aware slot call count'
    Assert-Matches $scannerSource 'warnIfTradeResourcesChanged\s*\(\s*retainAssignedTrades\s*\(\s*currentTrades\s*,\s*state\.assignments\s*,\s*false\s*\)\s*\)' 'Trade reload revision compares a different resource domain from the startup snapshot.'
    Assert-Matches $scannerSource 'displayed at full durability, but acquired durability, chance, and conditions vary' 'Variable-durability loot is not qualified accurately.'
    Assert-Matches $acquisitionCategorySource '160\s*,\s*76' 'Acquisition category height no longer accommodates the longest current wrapped provenance label.'
    Assert-Matches $acquisitionCategorySource 'textWithWordWrap\s*\(' 'Acquisition provenance is not rendered with bounded wrapping.'
    Assert-Matches $creativeCatalogSource 'CreativeModeTabEvents\.modifyOutputEvent\(CreativeModeTabs\.INGREDIENTS\)' 'Creative Search does not expose the shared Matcha catalog through an ordinary tab rebuild.'
    Assert-Matches $creativeCatalogSource 'CreativeModeTab\.TabVisibility\.SEARCH_TAB_ONLY' 'Matcha entries are not scoped to Creative Search.'
    Assert-Matches $creativeCatalogSource 'MatchaClientData\.current\(\)\.catalog\(\)' 'Creative Search is not sourcing exact stacks from the shared catalog.'
    Assert-Matches $creativeCatalogSource 'MatchaCreativeSearchEntries\.replaceOwned\(' 'Payload synchronization does not replace only Matcha-owned Search entries.'
    Assert-Matches $creativeSearchEntriesSource 'searchContents\.removeIf\(ownedEntries::contains\)' 'Payload synchronization does not remove prior Matcha-owned entries by object identity.'
    Assert-Matches $creativeSearchEntriesSource 'ItemStack\.isSameItemSameComponents\(existing, contribution\)' 'Creative Search contribution no longer deduplicates exact component identity.'
    Assert-True (-not $creativeCatalogSource.Contains('CreativeModeTabs.tryRebuildTabContents(')) 'Matcha still requests a global Creative tab rebuild.'
    Assert-True (-not $creativeCatalogSource.Contains('CreativeModeTabs.searchTab().buildContents(')) 'Matcha still directly rebuilds global Creative Search.'
    Assert-Matches $clientDataSource 'addListener\(' 'Creative and JEI cannot independently receive synchronized catalog changes.'
    Assert-True ($creativeCatalogSource -cnotmatch 'mezz\.jei') 'Creative catalog support must not classload JEI API types.'
    Assert-True (-not [regex]::IsMatch(
            $javaSources,
            '(?:getNamespace\s*\(\s*\)\s*\.equals\s*\(\s*"(?:minecraft|main)"|"(?:minecraft|main)"\s*\.equals\s*\([^)]*getNamespace\s*\()',
            [Text.RegularExpressions.RegexOptions]::Singleline
        )) 'A minecraft/main namespace shortcut was found; those namespaces also contain unrelated vanilla resources.'

    Write-Output 'MATCHA_C5_CATALOG_OK: recipes=1076 raw_component_outputs=315 trades=290 trade_models=145 nonrecipe_models=112 (57/45/10) exact_trades=175 acquisitions=249 preserved_nonrecipe_identities=240 (61/149/30; 19 exact-bridge/221 vanilla); resolved outputs and additive Creative Search are verified structurally.'
} finally {
    if ($null -ne $zip) {
        $zip.Dispose()
    }
    $zipStream.Dispose()
}
