"""Reproducible private assembler; never modifies originals or ships reference packs wholesale."""
import hashlib, json, pathlib, subprocess, zipfile, os
ROOT = pathlib.Path(__file__).resolve().parents[1]
OUT = ROOT / 'build/generated/resources'
COMMON = pathlib.Path(subprocess.check_output(['git','rev-parse','--path-format=absolute','--git-common-dir'], text=True).strip()).parent
INPUTS = json.loads((ROOT/'build-inputs.json').read_text())
MC = pathlib.Path(os.environ.get('MYNX_MINECRAFT_JAR', pathlib.Path.home()/'.gradle/caches/fabric-loom/26.2/minecraft-merged.jar'))

def write(path, value):
    target=OUT/path; target.parent.mkdir(parents=True,exist_ok=True)
    target.write_bytes(value if isinstance(value,bytes) else (json.dumps(value,indent=2)+'\n').encode())

def rename(value, replacements):
    if isinstance(value,str):return replacements.get(value,value)
    if isinstance(value,list):return [rename(x,replacements) for x in value]
    if isinstance(value,dict):return {k:rename(v,replacements) for k,v in value.items()}
    return value

def assemble():
    for retired in ['assets/mynx_trees/textures/item/silver_birch_leaves.png']:
        (OUT/retired).unlink(missing_ok=True)
    if hashlib.sha256(MC.read_bytes()).hexdigest()!=INPUTS['minecraft_baseline']['sha256']:raise ValueError('Changed Minecraft 26.2 build baseline')
    for entry in INPUTS['assets']:
        source=COMMON/entry['source']
        if hashlib.sha256(source.read_bytes()).hexdigest()!=entry['sha256']:raise ValueError('Changed immutable input: '+str(source))
    for name in ['log','leaves','sapling']:
        write('assets/mynx_trees/textures/block/silver_birch_'+name+'.png',(COMMON/('originals/assets/block_silver_birch_'+name+'.png')).read_bytes())
    write('assets/mynx_trees/textures/block/wisteria_log_top.png',(COMMON/'originals/assets/block_wisteria_log_top.png').read_bytes())
    with zipfile.ZipFile(COMMON/'originals/assets/cherry-to-wisteria.zip') as pack:
        for folder,old,new in [('block','cherry_leaves','wisteria_leaves'),('block','cherry_log','wisteria_log'),('block','cherry_sapling','wisteria_sapling'),('block','pink_petals','sweet_violets'),('item','pink_petals','sweet_violets')]+[('particle','cherry_'+str(i),'wisteria_'+str(i)) for i in range(12)]:
            write(f'assets/mynx_trees/textures/{folder}/{new}.png',pack.read(f'cherry-to-wisteria/assets/minecraft/textures/{folder}/{old}.png'))
    with zipfile.ZipFile(MC) as mc:
        def vanilla(path):return json.loads(mc.read(path))
        def data(kind,name,obj):write(f'data/mynx_trees/{kind}/{name}.json',obj)
        def asset(kind,name,obj):write(f'assets/mynx_trees/{kind}/{name}.json',obj)
        names=[]
        for tree,wood in [('silver_birch','birch'),('wisteria','pale_oak')]:
            for part in ['log','wood','leaves','sapling']:
                name=tree+'_'+part;names.append(name);ident='mynx_trees:'+name
                if part in ['log','wood']:
                    asset('blockstates',name,{'variants':{'axis=y':{'model':'mynx_trees:block/'+name},'axis=x':{'model':'mynx_trees:block/'+name,'x':90,'y':90},'axis=z':{'model':'mynx_trees:block/'+name,'x':90}}})
                    end=('minecraft:block/birch_log_top' if tree=='silver_birch' else 'mynx_trees:block/wisteria_log_top') if part=='log' else 'mynx_trees:block/'+tree+'_log'
                    asset('models/block',name,{'parent':'minecraft:block/cube_column','textures':{'side':'mynx_trees:block/'+tree+'_log','end':end}})
                    data('recipe',name+'_planks',{'type':'minecraft:crafting_shapeless','category':'building','ingredients':[ident],'result':{'id':'minecraft:'+wood+'_planks','count':4}})
                    data('loot_table/blocks',name,{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':ident}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
                elif part=='leaves':
                    asset('blockstates',name,{'variants':{'':{'model':'mynx_trees:block/'+name}}})
                    asset('models/block',name,{'parent':'minecraft:block/leaves' if tree=='silver_birch' else 'minecraft:block/cube_all','textures':{'all':'mynx_trees:block/'+name}})
                    leaf=vanilla('data/minecraft/loot_table/blocks/birch_leaves.json')['pools'][0]['entries'][0]['children'][0]
                    leaf['name']=ident
                    data('loot_table/blocks',name,{'type':'minecraft:block','pools':[{'rolls':1,'entries':[leaf]}]})
                    data('recipe',tree+'_sapling',{'type':'minecraft:crafting_shapeless','category':'misc','ingredients':[ident],'result':{'id':'mynx_trees:'+tree+'_sapling','count':1}})
                else:
                    asset('blockstates',name,{'variants':{'':{'model':'mynx_trees:block/'+name}}})
                    asset('models/block',name,{'parent':'minecraft:block/cross','textures':{'cross':'mynx_trees:block/'+name}})
                    data('loot_table/blocks',name,{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':ident}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
                sprite=part=='sapling'
                asset('models/item',name,{'parent':'minecraft:item/generated','textures':{'layer0':'mynx_trees:'+('item/' if part=='leaves' else 'block/')+name}} if sprite else {'parent':'mynx_trees:block/'+name})
                if tree=='silver_birch' and part=='leaves':
                    # Exactly one vanilla-style item tint; the block texture remains the source pixels.
                    rgb=int(INPUTS['color_reference']['inventory_rgb'][1:],16)
                    asset('items',name,{'model':{'type':'minecraft:model','model':'mynx_trees:block/'+name,'tints':[{'type':'minecraft:constant','value':rgb-0x1000000}]}})
                else:
                    asset('items',name,{'model':{'type':'minecraft:model','model':'mynx_trees:item/'+name}})
            data('recipe',tree+'_wood',{'type':'minecraft:crafting_shaped','category':'building','pattern':['##','##'],'key':{'#':'mynx_trees:'+tree+'_log'},'result':{'id':'mynx_trees:'+tree+'_wood','count':3}})
        names.append('sweet_violets')
        mapping={'minecraft:pink_petals':'mynx_trees:sweet_violets','minecraft:blocks/pink_petals':'mynx_trees:blocks/sweet_violets',**{'minecraft:block/pink_petals_'+str(i):'mynx_trees:block/sweet_violets_'+str(i) for i in range(1,5)}}
        asset('blockstates','sweet_violets',rename(vanilla('assets/minecraft/blockstates/pink_petals.json'),mapping))
        for i in range(1,5):asset('models/block','sweet_violets_'+str(i),{'parent':f'minecraft:block/flowerbed_{i}','textures':{'flowerbed':'mynx_trees:block/sweet_violets','stem':'minecraft:block/pink_petals_stem'}})
        asset('models/item','sweet_violets',{'parent':'minecraft:item/generated','textures':{'layer0':'mynx_trees:item/sweet_violets'}})
        asset('items','sweet_violets',{'model':{'type':'minecraft:model','model':'mynx_trees:item/sweet_violets'}})
        data('loot_table/blocks','sweet_violets',rename(vanilla('data/minecraft/loot_table/blocks/pink_petals.json'),mapping))
        asset('lang','en_us',{'block.mynx_trees.'+n:n.replace('_',' ').title() for n in names})
        asset('particles','wisteria',{'textures':['mynx_trees:wisteria_'+str(i) for i in range(12)]})
        # General logs integrate direct-log recipes and charcoal without entering a vanilla species plank tag.
        logs=['mynx_trees:'+t+'_'+s for t in ['silver_birch','wisteria'] for s in ['log','wood']]
        tags={'logs':logs,'logs_that_burn':logs,'mineable/axe':logs,'leaves':['mynx_trees:'+t+'_leaves' for t in ['silver_birch','wisteria']],'saplings':['mynx_trees:'+t+'_sapling' for t in ['silver_birch','wisteria']],'flowers':['mynx_trees:sweet_violets'],'small_flowers':['mynx_trees:sweet_violets']}
        # Pink Petals are not small_flowers (suspicious stew/dye semantics); match only actual counterpart tags.
        tags.pop('small_flowers')
        for kind in ['block','item']:
            for tag,values in tags.items():
                if kind=='item' and tag=='mineable/axe':continue
                write(f'data/minecraft/tags/{kind}/{tag}.json',{'replace':False,'values':values})
        for kind,tag,values in [('block','bee_attractive',['mynx_trees:sweet_violets','mynx_trees:wisteria_leaves']),('block','inside_step_sound_blocks',['mynx_trees:sweet_violets']),('item','bee_food',['mynx_trees:sweet_violets']),('block','overworld_natural_logs',['mynx_trees:silver_birch_log','mynx_trees:wisteria_log'])]:
            write(f'data/minecraft/tags/{kind}/{tag}.json',{'replace':False,'values':values})
        # Recipe-book unlocks retain direct vanilla plank outputs.
        for recipe in (OUT/'data/mynx_trees/recipe').glob('*.json'):
            obj=json.loads(recipe.read_text()); ingredient=obj.get('ingredients',[None])[0] or obj['key']['#']
            data('advancement/recipes',recipe.stem,{'parent':'minecraft:recipes/root','criteria':{'has_material':{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':ingredient}]}},'has_recipe':{'trigger':'minecraft:recipe_unlocked','conditions':{'recipe':'mynx_trees:'+recipe.stem}}},'requirements':[['has_material','has_recipe']],'rewards':{'recipes':['mynx_trees:'+recipe.stem]}})
        # Derive the entire birch selector graph, including both fallen forms and per-branch survival filters.
        variants=['birch_bees_0002','super_birch_bees_0002','fallen_birch_tree','fallen_super_birch_tree']
        for n in variants+['super_birch_bees']:
            data('worldgen/configured_feature','silver_'+n,{'type':'mynx_trees:material_tree','config':{'source':'minecraft:'+n,'species':'silver_birch'}})
        for n in variants:
            placed=vanilla('data/minecraft/worldgen/placed_feature/'+n+'.json');placed['feature']='mynx_trees:silver_'+n
            data('worldgen/placed_feature','silver_'+n,placed)
        selector=vanilla('data/minecraft/worldgen/configured_feature/birch_tall.json')
        data('worldgen/configured_feature','silver_birch_selection',rename(selector,{'minecraft:'+n:'mynx_trees:silver_'+n for n in variants}))
        placed=vanilla('data/minecraft/worldgen/placed_feature/birch_tall.json');placed['feature']='mynx_trees:silver_birch_selection'
        data('worldgen/placed_feature','silver_birch_trees',placed)
        for source in ['cherry','cherry_bees_005']:
            for species in ['wisteria','cherry']:
                data('worldgen/configured_feature',species+'_'+source,{'type':'mynx_trees:material_tree','config':{'source':'minecraft:'+source,'species':species}})
        data('worldgen/configured_feature','grove_selection',{'type':'minecraft:random_selector','config':{'features':[{'chance':0.25,'feature':{'feature':'mynx_trees:wisteria_cherry_bees_005','placement':[]}}],'default':{'feature':'mynx_trees:cherry_cherry_bees_005','placement':[]}}})
        placed=vanilla('data/minecraft/worldgen/placed_feature/trees_cherry.json');placed['feature']='mynx_trees:grove_selection'
        data('worldgen/placed_feature','grove_trees',placed)
    print('Generated private textures and focused 26.2 resources; all supplied input hashes verified.')
if __name__=='__main__':assemble()
