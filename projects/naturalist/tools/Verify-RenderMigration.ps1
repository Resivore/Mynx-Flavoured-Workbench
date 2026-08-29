[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$modelRoot = Join-Path $projectRoot 'common\src\main\java\com\crispytwig\naturalist\client\model'
$rendererRoot = Join-Path $projectRoot 'common\src\main\java\com\crispytwig\naturalist\client\renderer'

function Assert-Equal {
    param(
        [Parameter(Mandatory)]$Actual,
        [Parameter(Mandatory)]$Expected,
        [Parameter(Mandatory)][string]$Label
    )

    if ($Actual -ne $Expected) {
        throw "$Label mismatch: expected '$Expected', found '$Actual'."
    }
    Write-Host "PASS  $Label = $Expected"
}

function Assert-Matches {
    param(
        [Parameter(Mandatory)][string]$Source,
        [Parameter(Mandatory)][string]$Pattern,
        [Parameter(Mandatory)][string]$Label
    )

    if ($Source -notmatch $Pattern) {
        throw "$Label is missing the required source contract."
    }
    Write-Host "PASS  $Label"
}

function Assert-NoHits {
    param(
        [Parameter()][IO.FileInfo[]]$Files,
        [Parameter(Mandatory)][string]$Pattern,
        [Parameter(Mandatory)][string]$Label
    )

    $hits = @($Files | Select-String -Pattern $Pattern)
    if ($hits.Count -ne 0) {
        $locations = @($hits | ForEach-Object { "$($_.Path):$($_.LineNumber)" })
        throw "$Label must be empty, but found: $($locations -join ', ')"
    }
    Write-Host "PASS  $Label is empty"
}

function Read-ProjectSource {
    param([Parameter(Mandatory)][string]$RelativePath)

    return Get-Content -LiteralPath (Join-Path $projectRoot $RelativePath) -Raw
}

$rootModelFiles = @(Get-ChildItem -LiteralPath $modelRoot -File -Filter '*Model.java')
$concreteModels = @($rootModelFiles | Where-Object {
    $_.BaseName -ne 'IKEntityModel' -and
    (Get-Content -LiteralPath $_.FullName -Raw) -match 'extends\s+(?:NaturalistEntityModel|IKEntityModel)<'
})
Assert-Equal $rootModelFiles.Count 76 'top-level client model file count'
Assert-Equal $concreteModels.Count 73 'concrete Naturalist entity model count'

foreach ($file in $concreteModels) {
    $source = Get-Content -LiteralPath $file.FullName -Raw
    $constructorPattern = '(?s)\bpublic\s+' + [regex]::Escape($file.BaseName) + '\s*\([^)]*\)\s*\{\s*super\s*\('
    Assert-Matches $source $constructorPattern "$($file.BaseName) root-bearing EntityModel constructor"
}

$naturalistModel = Read-ProjectSource 'common\src\main\java\com\crispytwig\naturalist\client\model\NaturalistEntityModel.java'
Assert-Matches $naturalistModel 'extends\s+EntityModel\s*<\s*NaturalistRenderState\s*<\s*E\s*>\s*>' 'shared model uses a 26.2 render state'
Assert-Matches $naturalistModel 'void\s+setupAnim\s*\(\s*NaturalistRenderState' 'shared model state animation entrypoint'
Assert-Matches $naturalistModel 'boneAnimations\s*\(\s*\)[\s\S]*bakeChannelsForResolvedPart' 'animation definitions bake per resolved model part'
Assert-Matches $naturalistModel 'getAllParts\s*\(\s*\)[\s\S]*hasChild\s*\(\s*name\s*\)[\s\S]*getChild\s*\(\s*name\s*\)' 'animation bones retain upstream child-first lookup semantics'
Assert-Matches $naturalistModel 'orElseGet[\s\S]*name\.equals\s*\(\s*this\.getRootPartName\s*\(\s*\)\s*\)[\s\S]*this\.root\s*\(\s*\)' 'nonstandard animation roots remap to the selected model root'
Assert-Matches $naturalistModel 'Map\.of\s*\(\s*"root"\s*,\s*channels\s*\)[\s\S]*bake\s*\(\s*targetPart\s*\)' 'duplicate root-named children bake against their resolved target'

foreach ($rootAlias in @(
    @{ File = 'DirtTrailModel.java'; Alias = 'mound' },
    @{ File = 'SnakeModel.java'; Alias = 'main' },
    @{ File = 'VultureModel.java'; Alias = 'body' }
)) {
    $source = Get-Content -LiteralPath (Join-Path $modelRoot $rootAlias.File) -Raw
    Assert-Matches $source ('getRootPartName\s*\(\s*\)[\s\S]*return\s+"' + $rootAlias.Alias + '"') "$($rootAlias.File) animation root alias"
}

$smoothAnimation = Read-ProjectSource 'common\src\main\java\com\crispytwig\naturalist\server\entity\util\SmoothAnimationState.java'
Assert-Matches $smoothAnimation 'animationAccumulatedTime\s*\+=' 'pre-26.2 smooth animation time accumulation'
Assert-Matches $smoothAnimation '\(currentTime\s*-\s*this\.animationLastTime\)\s*\*\s*speed' 'smooth animation speed integrates without time jumps'
Assert-Matches $naturalistModel 'state\.updateTime\s*\(\s*ageInTicks\s*,\s*speed\s*\)' 'smooth animation helper updates accumulated time'
Assert-Matches $naturalistModel 'apply\s*\(\s*state\.getAccumulatedTime\s*\(\s*\)\s*,\s*factor\s*\)' 'smooth animation helper preserves blend weight'

$rendererFiles = @(Get-ChildItem -LiteralPath $rendererRoot -File -Filter '*.java')
$entityPipelineFiles = @(
    $rootModelFiles
    Get-ChildItem -LiteralPath (Join-Path $rendererRoot 'layers') -File -Filter '*.java'
    $rendererFiles | Where-Object Name -ne 'SnailShellRenderer.java'
)
Assert-Equal @($rendererFiles | Select-String -Pattern 'class\s+\w+Renderer\s+extends\s+NaturalistMobRenderer<').Count 48 'Naturalist mob renderer count'
Assert-Equal @($rendererFiles | Select-String -Pattern '\bthis\.addLayer\s*\(').Count 24 'preserved feature-layer attachment count'
Assert-Equal @(Get-ChildItem -LiteralPath (Join-Path $rendererRoot 'layers') -File -Filter '*.java').Count 12 'feature-layer support file count'

Assert-NoHits $entityPipelineFiles '\bHierarchicalModel\b' 'retired HierarchicalModel references in the entity pipeline'
Assert-NoHits $entityPipelineFiles '\bMultiBufferSource\b' 'retired immediate entity buffers in the entity pipeline'
Assert-NoHits $entityPipelineFiles '\bvoid\s+render\s*\(' 'retired immediate render overrides in the entity pipeline'
Assert-NoHits $entityPipelineFiles '\bRenderType\.entity' 'retired singular RenderType factories in the entity pipeline'
Assert-NoHits $concreteModels '\bModelPart\s+root\s*\(\s*\)' 'overrides of final 26.2 model root method'
Assert-NoHits $concreteModels '\bsetupAnim\s*\([^)]*,[^)]*,[^)]*,[^)]*,[^)]*,' 'legacy six-argument entity model setup methods'
Assert-NoHits ($rendererFiles | Where-Object Name -ne 'SnailShellRenderer.java') '\.visible\s*=' 'deferred renderer-side model visibility mutations'

$naturalistRenderer = Read-ProjectSource 'common\src\main\java\com\crispytwig\naturalist\client\renderer\NaturalistMobRenderer.java'
Assert-Matches $naturalistRenderer 'MobRenderer\s*<\s*T\s*,\s*NaturalistRenderState\s*<\s*T\s*>\s*,\s*NaturalistEntityModel\s*<\s*T\s*>\s*>' 'shared renderer uses 26.2 entity/state/model generics'
Assert-Matches $naturalistRenderer 'state\.texture\s*=\s*this\.getTextureLocation\s*\(\s*entity\s*\)' 'texture selection is captured during state extraction'
Assert-Matches $naturalistRenderer 'state\.isBaby\s*\?\s*this\.babyModel\s*:\s*this\.adultModel' 'adult/baby model selection remains explicit'
Assert-Matches $naturalistRenderer 'babyShadowRadius\s*:\s*this\.adultShadowRadius\)[^;]*state\.scale[^;]*state\.ageScale' 'adult/baby shadow scaling uses current state scales'

foreach ($preSubmitScale in @(
    @{ File = 'KomodoDragonRenderer.java'; Factor = '0\.45F' },
    @{ File = 'MoleRenderer.java'; Factor = '0\.6F' },
    @{ File = 'TurkeyRenderer.java'; Factor = '0\.5F' }
)) {
    $source = Get-Content -LiteralPath (Join-Path $rendererRoot $preSubmitScale.File) -Raw
    Assert-Matches $source ('submit[\s\S]*pushPose[\s\S]*state\.isBaby[\s\S]*scale\s*\(\s*' + $preSubmitScale.Factor + '[\s\S]*super\.submit[\s\S]*popPose') "$($preSubmitScale.File) preserves upstream pre-render baby scale order"
}

$clientSource = Read-ProjectSource 'common\src\main\java\com\crispytwig\naturalist\NaturalistClient.java'
$layerRegistration = [regex]::Match($clientSource, '(?s)public\s+static\s+void\s+registerLayerDefinitions\s*\([^)]*\)\s*\{(.*?)\n\s*\}')
$rendererRegistration = [regex]::Match($clientSource, '(?s)public\s+static\s+void\s+registerRenderers\s*\([^)]*\)\s*\{(.*?)\n\s*\}')
Assert-Equal ([regex]::Matches($layerRegistration.Groups[1].Value, '\br\.register\s*\(').Count) 73 'registered model-layer definition count'
Assert-Equal ([regex]::Matches($rendererRegistration.Groups[1].Value, '\br\.register\s*\(').Count) 51 'registered entity renderer count'

$fabricClient = Read-ProjectSource 'fabric\src\main\java\com\crispytwig\naturalist\fabric\client\NaturalistFabricClient.java'
Assert-Matches $fabricClient '\bModelLayerRegistry\.registerModelLayer\s*\(' 'Fabric 26.2 model-layer registration API'
Assert-Matches $fabricClient '\bEntityRendererRegistry::register\b' 'Fabric entity renderer registration API'

$mixinRoot = Join-Path $projectRoot 'common\src\main\java\com\crispytwig\naturalist\mixin'
$renderMixinFiles = @(
    Get-Item -LiteralPath (Join-Path $mixinRoot 'EntityRenderDispatcherMixin.java')
    Get-Item -LiteralPath (Join-Path $mixinRoot 'PlayerModelMixin.java')
    Get-Item -LiteralPath (Join-Path $mixinRoot 'ParrotModelMixin.java')
    Get-Item -LiteralPath (Join-Path $mixinRoot 'ParrotOnShoulderLayerMixin.java')
    Get-Item -LiteralPath (Join-Path $mixinRoot 'WolfModelMixin.java')
)
Assert-NoHits $renderMixinFiles '\bMultiBufferSource\b|setupAnim\(Lnet/minecraft/world/entity/' 'retired render APIs in state-era mixins'

$dispatcherMixin = Get-Content -LiteralPath (Join-Path $mixinRoot 'EntityRenderDispatcherMixin.java') -Raw
$playerMixin = Get-Content -LiteralPath (Join-Path $mixinRoot 'PlayerModelMixin.java') -Raw
$parrotMixin = Get-Content -LiteralPath (Join-Path $mixinRoot 'ParrotModelMixin.java') -Raw
$shoulderMixin = Get-Content -LiteralPath (Join-Path $mixinRoot 'ParrotOnShoulderLayerMixin.java') -Raw
$wolfMixin = Get-Content -LiteralPath (Join-Path $mixinRoot 'WolfModelMixin.java') -Raw
Assert-Matches $dispatcherMixin 'extractEntity\(Lnet/minecraft/world/entity/Entity;F\)[^"\r\n]*EntityRenderState' 'entity-to-render-state lookup injection'
Assert-Matches $dispatcherMixin 'skipBakedRider[\s\S]*entity\.getVehicle\s*\(\s*\)\s+instanceof\s+IKMount' 'IK mount duplicate-rider suppression'
Assert-Matches $dispatcherMixin 'EntityHitboxDebugRenderer[\s\S]*Gizmos\.cuboid' 'multipart gizmo hitboxes'
Assert-Matches $playerMixin 'AvatarRenderState[\s\S]*naturalist\$rotateWaist' 'player IK lean state animation'
Assert-Matches $shoulderMixin 'submitOnShoulder[\s\S]*NaturalistParrotRenderStateLookup\.markFlyingShoulder' 'queued shoulder-parrot state marker'
Assert-Matches $parrotMixin 'ParrotModel\.Pose\.FLYING[\s\S]*state\.flapAngle' 'shoulder-parrot flying pose animation'
Assert-Matches $wolfMixin 'WolfRenderState[\s\S]*naturalist\$isDiggingOutMole' 'wolf digging state animation'

$specializedContracts = @(
    @{ File = 'BassRenderer.java'; Pattern = 'isLargeVariant[\s\S]*isMediumVariant'; Label = 'Bass three-size model selection' },
    @{ File = 'BlobfishRenderer.java'; Pattern = 'isGray\s*\(\s*\)[\s\S]*(?:grayModel|GRAY)'; Label = 'Blobfish pressure appearance selection' },
    @{ File = 'AnglerfishRenderer.java'; Pattern = 'isGlowing\s*\(\s*\)[\s\S]*lightCoords'; Label = 'Anglerfish full-bright state extraction' },
    @{ File = 'HippoRenderer.java'; Pattern = 'BlockModelRenderState[\s\S]*blockModel\.submit'; Label = 'Hippo jaw-block feature submission' },
    @{ File = 'CarriedFoodRenderer.java'; Pattern = 'carrierOffsetX[\s\S]*carrierOffsetY[\s\S]*carrierOffsetZ'; Label = 'carried-food carrier-relative placement' },
    @{ File = 'DirtTrailRenderer.java'; Pattern = 'getId\s*\(\s*\)[\s\S]*rotationDegrees'; Label = 'dirt-trail deterministic placement and rotation' },
    @{ File = 'DirtTrailRenderer.java'; Pattern = 'state\.ageInTicks\s*=\s*entity\.tickCount\s*\+\s*partialTick'; Label = 'dirt-trail non-living animation age extraction' },
    @{ File = 'DirtTrailRenderer.java'; Pattern = 'OverlayTexture\.NO_OVERLAY\s*,\s*state\.outlineColor'; Label = 'dirt-trail outline state submission' }
)
foreach ($contract in $specializedContracts) {
    $source = Get-Content -LiteralPath (Join-Path $rendererRoot $contract.File) -Raw
    Assert-Matches $source $contract.Pattern $contract.Label
}

$moleModel = Get-Content -LiteralPath (Join-Path $modelRoot 'MoleModel.java') -Raw
Assert-Matches $moleModel 'setupAnimations\s*\([^)]*Mole[^)]*\)[\s\S]*mound\.visible\s*=\s*entity\.isRolledUp\s*\(\s*\)' 'per-state mole mound visibility'

$layerContracts = @(
    @{ File = 'DyeLayer.java'; Pattern = 'getDyeColor[\s\S]*RenderTypes\.entityCutout'; Label = 'dye texture layer' },
    @{ File = 'FireflyGlowLayer.java'; Pattern = '(?s)(?=.*submitCustomGeometry)(?=.*AnimatedUVVertexConsumer)(?=.*entityTranslucentEmissive)'; Label = 'animated emissive firefly layers' },
    @{ File = 'BannerLayer.java'; Pattern = '(?s)(?=.*submitModel\s*\(\s*sideFlag)(?=.*BannerRenderer\.submitPatterns)(?=.*mirroredFlag)(?=.*mirroredBar)(?=.*state\.outlineColor)'; Label = 'base-lit patterned mirrored elephant banners' },
    @{ File = 'SeatedRiderLayer.java'; Pattern = 'extractEntity[\s\S]*renderer\.submit'; Label = 'seated passenger renderer state' },
    @{ File = 'TortoiseMaskLayer.java'; Pattern = 'Donatello[\s\S]*Leonardo[\s\S]*Michelangelo[\s\S]*Raphael'; Label = 'named tortoise masks' },
    @{ File = 'HedgehogGlintLayer.java'; Pattern = 'RenderTypes\.entityGlint\s*\('; Label = 'hedgehog enchantment glint' }
)
foreach ($contract in $layerContracts) {
    $source = Get-Content -LiteralPath (Join-Path (Join-Path $rendererRoot 'layers') $contract.File) -Raw
    Assert-Matches $source $contract.Pattern $contract.Label
}

Write-Host 'Render/model migration static verification passed.'
