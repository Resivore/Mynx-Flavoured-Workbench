$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$asset = Join-Path $root 'common\src\main\resources\assets\more_slabs_stairs_and_walls'

function Read-Solid([string]$name) {
    $json = Get-Content (Join-Path $asset "models\block\$name.json") -Raw | ConvertFrom-Json
    $expectedParent = if ($name -like 'template_glass_stairs*') { 'minecraft:block/stairs' } elseif ($name -eq 'template_glass_wall_inventory') { 'minecraft:block/wall_inventory' } else { 'minecraft:block/block' }
    if ($json.parent -ne $expectedParent) { throw "$name item display parent is $($json.parent), expected $expectedParent" }
    $occupied = [Collections.Generic.HashSet[string]]::new()
    foreach ($e in $json.elements) {
        for ($x=[int]$e.from[0];$x-lt[int]$e.to[0];$x++) { for ($y=[int]$e.from[1];$y-lt[int]$e.to[1];$y++) { for ($z=[int]$e.from[2];$z-lt[int]$e.to[2];$z++) {
            [void]$occupied.Add("$x,$y,$z")
        }}}
    }
    $outside = [Collections.Generic.HashSet[string]]::new()
    $queue = [Collections.Generic.Queue[int[]]]::new()
    $queue.Enqueue(@(-1,-1,-1)); [void]$outside.Add('-1,-1,-1')
    while ($queue.Count) {
        $v=$queue.Dequeue()
        foreach($d in @(@(1,0,0),@(-1,0,0),@(0,1,0),@(0,-1,0),@(0,0,1),@(0,0,-1))) {
            $n=@(($v[0]+$d[0]),($v[1]+$d[1]),($v[2]+$d[2])); if($n|Where-Object{$_-lt-1-or$_-gt16}){continue}
            $k="$($n[0]),$($n[1]),$($n[2])"; if($outside.Contains($k)){continue}
            if($n[0]-ge0-and$n[0]-lt16-and$n[1]-ge0-and$n[1]-lt16-and$n[2]-ge0-and$n[2]-lt16-and$occupied.Contains($k)){continue}
            [void]$outside.Add($k);$queue.Enqueue($n)
        }
    }
    $solid=[Collections.Generic.HashSet[string]]::new()
    for($x=0;$x-lt16;$x++){for($y=0;$y-lt16;$y++){for($z=0;$z-lt16;$z++){if(-not$outside.Contains("$x,$y,$z")){[void]$solid.Add("$x,$y,$z")}}}}
    return ,$solid
}

function Assert-ExteriorFaces([string]$name) {
    $json = Get-Content (Join-Path $asset "models\block\$name.json") -Raw | ConvertFrom-Json
    $solid = Read-Solid $name
    $faces = [Collections.Generic.HashSet[string]]::new()
    foreach ($e in $json.elements) {
        foreach ($property in $e.faces.PSObject.Properties) {
            if ($property.Value.texture -ne '#all') { throw "$name has noncanonical exterior texture" }
            if (@($property.Value.uv | Where-Object { $_ -lt 0 -or $_ -gt 16 }).Count) { throw "$name has UV outside 0..16" }
            switch ($property.Name) {
                north { for($x=$e.from[0];$x-lt$e.to[0];$x++){for($y=$e.from[1];$y-lt$e.to[1];$y++){[void]$faces.Add("north,$x,$y,$($e.from[2])")}} }
                south { for($x=$e.from[0];$x-lt$e.to[0];$x++){for($y=$e.from[1];$y-lt$e.to[1];$y++){[void]$faces.Add("south,$x,$y,$($e.to[2])")}} }
                west  { for($z=$e.from[2];$z-lt$e.to[2];$z++){for($y=$e.from[1];$y-lt$e.to[1];$y++){[void]$faces.Add("west,$($e.from[0]),$y,$z")}} }
                east  { for($z=$e.from[2];$z-lt$e.to[2];$z++){for($y=$e.from[1];$y-lt$e.to[1];$y++){[void]$faces.Add("east,$($e.to[0]),$y,$z")}} }
                down  { for($x=$e.from[0];$x-lt$e.to[0];$x++){for($z=$e.from[2];$z-lt$e.to[2];$z++){[void]$faces.Add("down,$x,$($e.from[1]),$z")}} }
                up    { for($x=$e.from[0];$x-lt$e.to[0];$x++){for($z=$e.from[2];$z-lt$e.to[2];$z++){[void]$faces.Add("up,$x,$($e.to[1]),$z")}} }
            }
        }
    }
    $directions = @{north=@(0,0,-1);south=@(0,0,1);west=@(-1,0,0);east=@(1,0,0);down=@(0,-1,0);up=@(0,1,0)}
    foreach ($voxel in $solid) {
        $v=$voxel.Split(',')|ForEach-Object{[int]$_}
        foreach ($entry in $directions.GetEnumerator()) {
            $neighbor="$($v[0]+$entry.Value[0]),$($v[1]+$entry.Value[1]),$($v[2]+$entry.Value[2])"
            if ($solid.Contains($neighbor)) { continue }
            $required=switch($entry.Key){north{"north,$($v[0]),$($v[1]),$($v[2])"};south{"south,$($v[0]),$($v[1]),$($v[2]+1)"};west{"west,$($v[0]),$($v[1]),$($v[2])"};east{"east,$($v[0]+1),$($v[1]),$($v[2])"};down{"down,$($v[0]),$($v[1]),$($v[2])"};up{"up,$($v[0]),$($v[1]+1),$($v[2])"}}
            if (-not $faces.Contains($required)) { throw "$name lacks exterior face unit $required" }
        }
    }
}

function Rotate([Collections.Generic.HashSet[string]]$solid,[int]$angle) {
    $result=[Collections.Generic.HashSet[string]]::new();$turns=(($angle%360)+360)%360/90
    foreach($key in $solid){$v=$key.Split(',')|ForEach-Object{[int]$_};$x=$v[0];$z=$v[2];for($i=0;$i-lt$turns;$i++){$nx=15-$z;$z=$x;$x=$nx};[void]$result.Add("$x,$($v[1]),$z")};return ,$result
}
function Half([string]$direction,[int]$x,[int]$z){switch($direction){north{return $z-lt8};south{return $z-ge8};east{return $x-ge8};west{return $x-lt8}}}
function Right([string]$f){switch($f){north{'east'};east{'south'};south{'west'};west{'north'}}}
function Left([string]$f){switch($f){north{'west'};west{'south'};south{'east'};east{'north'}}}
function Expected([string]$f,[string]$half,[string]$shape){$s=[Collections.Generic.HashSet[string]]::new();$turn=if($shape.EndsWith('left')){Left $f}else{Right $f};for($x=0;$x-lt16;$x++){for($y=0;$y-lt16;$y++){for($z=0;$z-lt16;$z++){$full=if($half-eq'bottom'){$y-lt8}else{$y-ge8};$band=if($shape-eq'straight'){Half $f $x $z}elseif($shape.StartsWith('outer')){(Half $f $x $z)-and(Half $turn $x $z)}else{(Half $f $x $z)-or(Half $turn $x $z)};$extra=if($half-eq'bottom'){$y-ge8-and$band}else{$y-lt8-and$band};if($full-or$extra){[void]$s.Add("$x,$y,$z")}}}};return ,$s}

$families=@('glass','white_stained_glass','orange_stained_glass','magenta_stained_glass','light_blue_stained_glass','yellow_stained_glass','lime_stained_glass','pink_stained_glass','gray_stained_glass','light_gray_stained_glass','cyan_stained_glass','purple_stained_glass','blue_stained_glass','brown_stained_glass','green_stained_glass','red_stained_glass','black_stained_glass')
$states=Get-Content (Join-Path $asset 'blockstates\glass_stairs.json') -Raw|ConvertFrom-Json -AsHashtable
$checked=0
foreach($entry in $states.variants.GetEnumerator()){$properties=@{};$entry.Key.Split(',')|ForEach-Object{$v=$_.Split('=');$properties[$v[0]]=$v[1]};$model=($entry.Value.model.Split('/')[-1]-replace '^glass_','template_glass_');$actual=Rotate (Read-Solid $model) ($(if($entry.Value.ContainsKey('y')){$entry.Value.y}else{0}));$expected=Expected $properties.facing $properties.half $properties.shape;if(-not$actual.SetEquals($expected)){throw "Stair silhouette mismatch: $($entry.Key) -> $model/$($entry.Value.y)"};$checked++}
if($checked-ne40){throw "Expected 40 stair states; found $checked"}
$innerStates=@($states.variants.GetEnumerator()|Where-Object{$_.Key-like'*shape=inner_*'})
if($innerStates.Count-ne16){throw "Expected 16 inner stair states; found $($innerStates.Count)"}
Assert-ExteriorFaces 'template_glass_stairs_inner'
Assert-ExteriorFaces 'template_glass_stairs_inner_up'
foreach($family in $families){$j=Get-Content (Join-Path $asset "blockstates\${family}_stairs.json") -Raw|ConvertFrom-Json -AsHashtable;if($j.variants.Count-ne40){throw "$family does not share the 40-state architecture"}}

$itemTemplate=Get-Content (Join-Path $asset 'models\item\template_glass_stairs.json') -Raw|ConvertFrom-Json -AsHashtable
if($itemTemplate.parent-ne'minecraft:block/block'){throw 'Glass Stair item template does not restore the Provider Canary 31 presentation parent'}
if(!$itemTemplate.ContainsKey('display')){throw 'Glass Stair item template is missing the focused main-hand correction'}
if(@($itemTemplate.display.Keys).Count-ne1-or!$itemTemplate.display.ContainsKey('firstperson_righthand')){throw 'Glass Stair item template changes a display context other than firstperson_righthand'}
$expectedMainHand=[ordered]@{rotation=@(0,135,0);translation=@(0,0,0);scale=@(0.4,0.4,0.4)}|ConvertTo-Json -Depth 5 -Compress
$actualMainHand=$itemTemplate.display.firstperson_righthand|ConvertTo-Json -Depth 5 -Compress
if($actualMainHand-ne$expectedMainHand){throw "Glass Stair main-hand transform is $actualMainHand, expected $expectedMainHand"}
$worldTemplate=Get-Content (Join-Path $asset 'models\block\template_glass_stairs.json') -Raw|ConvertFrom-Json -AsHashtable
$itemElements=$itemTemplate.elements|ConvertTo-Json -Depth 30 -Compress
$worldElements=$worldTemplate.elements|ConvertTo-Json -Depth 30 -Compress
if($itemElements-ne$worldElements){throw 'Glass Stair item-only template does not reuse the exact authored world elements'}
foreach($family in $families){
    $item=Get-Content (Join-Path $asset "models\item\${family}_stairs.json") -Raw|ConvertFrom-Json -AsHashtable
    if($item.parent-ne'more_slabs_stairs_and_walls:item/template_glass_stairs'){throw "$family does not use the shared Provider Canary 31 item presentation contract"}
    if($item.ContainsKey('display')){throw "$family retains a compensating display table"}
    $texture=if($family-eq'glass'){'minecraft:block/glass'}else{"minecraft:block/$family"}
    if($item.textures.all-ne$texture){throw "$family item lost canonical texture binding"}
}

$wall=Get-Content (Join-Path $asset 'blockstates\glass_wall.json') -Raw|ConvertFrom-Json -AsHashtable
$fullRuns=@($wall.multipart|Where-Object{$_.apply.model-like'*wall_full*'})
if($fullRuns.Count-ne8){throw "Expected 8 exact no-post full-run states; found $($fullRuns.Count)"}
foreach($entry in $fullRuns){if($entry.when.up-ne'false'){throw 'A full run can create a fake post-bearing state'}}
foreach($name in @('template_glass_wall_full','template_glass_wall_full_tall')){$model=Get-Content (Join-Path $asset "models\block\$name.json") -Raw|ConvertFrom-Json;if(@($model.elements).Count-ne19){throw "$name lost its 19 authored elements"};if(($model|ConvertTo-Json -Depth 30)-notmatch '"#all"'){throw "$name lost canonical texture binding"}}

[pscustomobject]@{stairStates=$checked;innerExteriorStates=$innerStates.Count;families=$families.Count;wallFullRunStates=$fullRuns.Count;stairItemPresentation='Canary 36 right-main-hand-only yaw';stairItemDisplayOverrides=1;wallItemDisplayParent='minecraft:block/wall_inventory';result='PASS'}|ConvertTo-Json
