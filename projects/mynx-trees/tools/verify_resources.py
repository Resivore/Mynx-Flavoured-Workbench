"""Focused package-contract checks, not Minecraft runtime validation."""
import json, pathlib, zipfile, hashlib, re, unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]
OUT=ROOT/'build/generated/resources'
MC=pathlib.Path.home()/'.gradle/caches/fabric-loom/26.2/minecraft-merged.jar'
class Resources(unittest.TestCase):
    def read(self,p):return json.loads((OUT/p).read_text())
    def test_exact_private_pixels(self):
        entries=json.loads((ROOT/'build-inputs.json').read_text())['packaged_assets']
        self.assertEqual({x['target'] for x in entries},{p.relative_to(OUT).as_posix() for p in OUT.rglob('*.png')})
        for e in entries:self.assertEqual(e['sha256'],hashlib.sha256((OUT/e['target']).read_bytes()).hexdigest(),e['source'])
    def test_no_vanilla_assets_or_foreign_runtime(self):
        self.assertFalse((OUT/'assets/minecraft').exists())
        for p in (OUT/'data/minecraft').rglob('*.json'):self.assertIn('/tags/',p.as_posix());self.assertIs(self.read(p.relative_to(OUT))['replace'],False)
        meta=json.loads((ROOT/'src/main/resources/fabric.mod.json').read_text());self.assertNotIn('regions_unexplored',meta['depends']);self.assertNotIn('nemos-blooming-blossom',meta['depends'])
        for p in (ROOT/'src/main/java').rglob('*.java'):self.assertNotIn('net.minecraft.client',p.read_text())
    def test_nine_identities_and_resource_references(self):
        names=[t+'_'+part for t in ['silver_birch','wisteria'] for part in ['log','wood','leaves','sapling']]+['sweet_violets']
        self.assertEqual(set(names),{p.stem for p in (OUT/'assets/mynx_trees/items').glob('*.json')})
        with zipfile.ZipFile(MC) as mc:
            def exists(ref,kind):
                ns,path=ref.split(':',1);p=f'assets/{ns}/{kind}/{path}.json'
                self.assertTrue((OUT/p).exists() or p in mc.namelist(),p)
            for name in names:
                self.assertTrue((OUT/f'assets/mynx_trees/blockstates/{name}.json').exists())
                self.assertTrue((OUT/f'data/mynx_trees/loot_table/blocks/{name}.json').exists())
            def walk(obj):
                if isinstance(obj,dict):
                    for k,v in obj.items():
                        if k in ['parent','model'] and isinstance(v,str) and ':' in v:exists(v,'models')
                        walk(v)
                elif isinstance(obj,list):
                    for v in obj:walk(v)
            for p in (OUT/'assets/mynx_trees').rglob('*.json'):
                if '/lang/' not in p.as_posix():walk(json.loads(p.read_text()))
    def test_only_eight_unambiguous_direct_recipes(self):
        self.assertEqual(8,len(list((OUT/'data/mynx_trees/recipe').glob('*.json'))))
        for tree,wood in [('silver_birch','birch'),('wisteria','pale_oak')]:
            for part in ['log','wood']:
                r=self.read(f'data/mynx_trees/recipe/{tree}_{part}_planks.json');self.assertEqual(['mynx_trees:'+tree+'_'+part],r['ingredients']);self.assertEqual({'id':'minecraft:'+wood+'_planks','count':4},r['result'])
            r=self.read(f'data/mynx_trees/recipe/{tree}_wood.json');self.assertEqual(['##','##'],r['pattern']);self.assertEqual({'#':'mynx_trees:'+tree+'_log'},r['key']);self.assertEqual(3,r['result']['count'])
            r=self.read(f'data/mynx_trees/recipe/{tree}_sapling.json');self.assertEqual(['mynx_trees:'+tree+'_leaves'],r['ingredients']);self.assertEqual({'id':'mynx_trees:'+tree+'_sapling','count':1},r['result'])
        self.assertEqual(8,len(list((OUT/'data/mynx_trees/advancement/recipes').glob('*.json'))))
    def test_leaf_loot_has_only_tool_gated_leaf(self):
        with zipfile.ZipFile(MC) as mc:
            gate=json.loads(mc.read('data/minecraft/loot_table/blocks/birch_leaves.json'))['pools'][0]['entries'][0]['children'][0]['conditions']
        for t in ['silver_birch','wisteria']:
            loot=self.read(f'data/mynx_trees/loot_table/blocks/{t}_leaves.json');self.assertEqual(1,len(loot['pools']));entries=loot['pools'][0]['entries'];self.assertEqual(1,len(entries));self.assertEqual(gate,entries[0]['conditions']);self.assertEqual('mynx_trees:'+t+'_leaves',entries[0]['name']);self.assertEqual(1,loot['pools'][0]['rolls'])
            for unwanted in ['sapling','stick','fortune','table_bonus']:self.assertNotIn(unwanted,json.dumps(loot))
    def test_logs_support_leaves_before_and_after_stripping_without_plank_tag_conflicts(self):
        with zipfile.ZipFile(MC) as mc:
            def members(tag,seen=None):
                seen=set() if seen is None else seen
                if tag in seen:return set()
                seen.add(tag);path='data/minecraft/tags/block/'+tag+'.json';vals=[]
                if path in mc.namelist():vals+=json.loads(mc.read(path))['values']
                if (OUT/path).exists():vals+=self.read(path)['values']
                result=set()
                for v in vals:
                    if isinstance(v,dict):v=v['id']
                    if v.startswith('#minecraft:'):result|=members(v.split(':',1)[1],seen)
                    else:result.add(v)
                return result
            support=members('prevents_nearby_leaf_decay')
            for t in ['silver_birch','wisteria']:
                for part in ['log','wood']:self.assertIn('mynx_trees:'+t+'_'+part,support)
            for t in ['birch','pale_oak']:
                for part in ['log','wood']:self.assertIn('minecraft:stripped_'+t+'_'+part,support)
        for kind in ['block','item']:
            for t in ['birch','pale_oak','cherry']:self.assertFalse((OUT/f'data/minecraft/tags/{kind}/{t}_logs.json').exists())
    def test_birch_selection_and_density_exact(self):
        with zipfile.ZipFile(MC) as mc:
            original=json.loads(mc.read('data/minecraft/worldgen/configured_feature/birch_tall.json'));actual=self.read('data/mynx_trees/worldgen/configured_feature/silver_birch_selection.json')
            for n in ['birch_bees_0002','super_birch_bees_0002','fallen_birch_tree','fallen_super_birch_tree']:
                original=json.loads(json.dumps(original).replace('minecraft:'+n,'mynx_trees:silver_'+n))
                actualplacement=self.read('data/mynx_trees/worldgen/placed_feature/silver_'+n+'.json');vp=json.loads(mc.read('data/minecraft/worldgen/placed_feature/'+n+'.json'));self.assertEqual(vp['placement'],actualplacement['placement'])
            self.assertEqual(original,actual)
            orig=json.loads(mc.read('data/minecraft/worldgen/placed_feature/birch_tall.json'));self.assertEqual(orig['placement'],self.read('data/mynx_trees/worldgen/placed_feature/silver_birch_trees.json')['placement'])
    def test_grove_replaces_selection_at_original_density(self):
        with zipfile.ZipFile(MC) as mc:original=json.loads(mc.read('data/minecraft/worldgen/placed_feature/trees_cherry.json'))
        self.assertEqual(original['placement'],self.read('data/mynx_trees/worldgen/placed_feature/grove_trees.json')['placement'])
        config=self.read('data/mynx_trees/worldgen/configured_feature/grove_selection.json')['config'];self.assertEqual(1,len(config['features']));self.assertEqual(0.25,config['features'][0]['chance']);self.assertEqual('mynx_trees:cherry_cherry_bees_005',config['default']['feature']);self.assertEqual([],config['default']['placement'])
    def test_violet_states_loot_and_species_assets(self):
        states=self.read('assets/mynx_trees/blockstates/sweet_violets.json')['multipart'];self.assertEqual(16,len(states))
        loot=self.read('data/mynx_trees/loot_table/blocks/sweet_violets.json');functions=loot['pools'][0]['entries'][0]['functions'];self.assertEqual([1,2,3,4],[f['count'] for f in functions if 'count' in f]);self.assertNotIn('minecraft:pink_petals',json.dumps(loot))
        log=self.read('assets/mynx_trees/models/block/silver_birch_log.json');self.assertEqual('minecraft:block/birch_log_top',log['textures']['end'])
        log=self.read('assets/mynx_trees/models/block/wisteria_log.json');self.assertEqual('mynx_trees:block/wisteria_log_top',log['textures']['end'])
        inv=self.read('assets/mynx_trees/items/silver_birch_leaves.json');self.assertEqual('mynx_trees:block/silver_birch_leaves',inv['model']['model']);self.assertEqual([{'type':'minecraft:constant','value':-8034533}],inv['model']['tints']);self.assertFalse((OUT/'assets/mynx_trees/textures/item/silver_birch_leaves.png').exists());self.assertEqual('mynx_trees:block/silver_birch_leaves',self.read('assets/mynx_trees/models/item/silver_birch_leaves.json')['parent'])
    def test_bounded_deterministic_decoration_and_native_interactions(self):
        source=(ROOT/'src/main/java/dev/resivore/mynxtrees/GroveFlowers.java').read_text();self.assertIn('context.random()',source);self.assertIn('state.canSurvive',source);self.assertIn('!context.isAir(pos)',source);self.assertNotIn('new Random',source)
        common=(ROOT/'src/main/java/dev/resivore/mynxtrees/MynxTrees.java').read_text();self.assertIn('extends SaplingBlock',common);self.assertIn('extends FlowerBedBlock',common)
        self.assertEqual(4,common.count('StrippableBlockRegistry.register('));self.assertIn('CompostableRegistry.INSTANCE.add(SWEET_VIOLETS, 0.3F)',common)
        self.assertIn('BiomeSelectors.includeByKey(Biomes.OLD_GROWTH_BIRCH_FOREST)',common);self.assertIn('BiomeSelectors.includeByKey(Biomes.CHERRY_GROVE)',common)
        self.assertIn('vanillaPlaced("flower_cherry")',common)
    def test_standalone_wisteria_top_and_no_flat_leaf_icon(self):
        manifest=json.loads((ROOT/'build-inputs.json').read_text())
        entries=manifest['packaged_assets'];top=[e for e in entries if e['target']=='assets/mynx_trees/textures/block/wisteria_log_top.png']
        self.assertEqual(1,len(top));self.assertEqual('originals/assets/block_wisteria_log_top.png',top[0]['source']);self.assertEqual([16,16],top[0]['dimensions']);self.assertEqual('68f630d51c1c900286def61b213b343dcfa5bc166fda85d5c0ade622e87b3d0b',top[0]['sha256'])
        self.assertFalse(any('item_silver_birch_leaves.png' in e['source'] or '/cherry_log_top.png' in e['source'] for e in entries))
    def test_optional_iris_hook_is_client_only_and_two_species_only(self):
        meta=json.loads((ROOT/'src/main/resources/fabric.mod.json').read_text());self.assertNotIn('iris',meta['depends']);self.assertEqual([{'config':'mynx_trees.client.mixins.json','environment':'client'}],meta['mixins'])
        cfg=json.loads((ROOT/'src/main/resources/mynx_trees.client.mixins.json').read_text());self.assertNotIn('mixins',cfg);self.assertEqual(['IrisLeafMaterialMixin'],cfg['client'])
        source=(ROOT/'src/client/java/dev/resivore/mynxtrees/mixin/IrisLeafMaterialMixin.java').read_text();self.assertEqual(2,source.count('LeafShaderAliases.inheritUnmapped('));self.assertEqual(2,source.count('.withPropertiesOf(state)'));self.assertIn('@At("RETURN")',source);self.assertIn('Blocks.BIRCH_LEAVES',source);self.assertNotRegex(source,r'\b1\d{4}\b');self.assertIn('@Pseudo',source)
        for state in ['Blocks.SUNFLOWER','Blocks.LILAC','Blocks.ROSE_BUSH','Blocks.PEONY']:self.assertNotIn(state,source)
        aliases=(ROOT/'src/client/java/dev/resivore/mynxtrees/LeafShaderAliases.java').read_text();self.assertNotIn('inheritUnmappedFromFirstPresent',aliases);self.assertIn('if (ids.containsKey(vanilla))',aliases)
        plugin=(ROOT/'src/client/java/dev/resivore/mynxtrees/IrisLeafMixinPlugin.java').read_text();self.assertIn('isModLoaded("iris")',plugin)
        for name in ['silver_birch_leaves','wisteria_leaves']:self.assertIn('mynx_trees:'+name,self.read('data/minecraft/tags/block/leaves.json')['values'])
    def test_silver_birch_tint_and_texture_contracts_stay_exact(self):
        manifest=json.loads((ROOT/'build-inputs.json').read_text());color=manifest['color_reference']
        self.assertEqual('h = sin(x/10 + sin((z+x)/50)*3)/75 + 0.12; s = 0.8; v = 0.52; float arithmetic, Minecraft sine lookup, RGB rounding as Java HSBtoRGB; this is the current intentional render compensation toward the user-supplied visual target, not byte-for-byte RU raw provider output',color['leaf_hsv'])
        self.assertEqual('#85671B',color['inventory_rgb']);self.assertEqual('#B0C73A',color['grass'])
        leaf=next(asset for asset in manifest['packaged_assets'] if asset['target']=='assets/mynx_trees/textures/block/silver_birch_leaves.png')
        self.assertEqual('fdabcce828735f8435dca7884d63957d4b7271e43fa38cf7349df06eb928e8fc',leaf['sha256'])
        client=(ROOT/'src/client/java/dev/resivore/mynxtrees/MynxTreesClient.java').read_text()
        self.assertIn('public static final float SILVER_BIRCH_HUE_BASE = 0.12F;',client)
        self.assertIn('float hue=Mth.sin(x/10.0F+Mth.sin(((float)z+x)/50.0F)*3.0F)/75.0F+SILVER_BIRCH_HUE_BASE;',client)
        self.assertIn('public static final float SILVER_BIRCH_VALUE = 0.52F;',client)
        self.assertIn('return java.awt.Color.HSBtoRGB(hue,0.8F,SILVER_BIRCH_VALUE);',client)
    def test_base_bark_soils_are_exact_and_independent(self):
        tag=self.read('data/mynx_trees/tags/block/silver_birch_base_soils.json')
        self.assertEqual({'replace':False,'values':['minecraft:'+n for n in ['grass_block','dirt','coarse_dirt','rooted_dirt','podzol','mycelium']]},tag)
        for p in (ROOT/'src/main/java').rglob('*.java'):self.assertNotIn('silver_birch_base_soils',p.read_text())
        for p in (OUT/'data/mynx_trees/worldgen').rglob('*.json'):self.assertNotIn('silver_birch_base_soils',p.read_text())
    def test_base_bark_is_side_only_and_items_stay_ordinary(self):
        ordinary=self.read('assets/mynx_trees/models/block/silver_birch_log.json')
        base=self.read('assets/mynx_trees/models/block/silver_birch_log_base.json')
        expected=json.loads(json.dumps(ordinary));expected['textures']['side']='mynx_trees:block/silver_birch_log_base'
        self.assertEqual(expected,base);self.assertEqual('minecraft:block/birch_log_top',base['textures']['end'])
        self.assertEqual('minecraft:block/cube_column',base['parent'])
        item=self.read('assets/mynx_trees/items/silver_birch_log.json')
        self.assertEqual({'model':{'type':'minecraft:model','model':'mynx_trees:item/silver_birch_log'}},item)
        self.assertEqual({'parent':'mynx_trees:block/silver_birch_log'},self.read('assets/mynx_trees/models/item/silver_birch_log.json'))
        states=self.read('assets/mynx_trees/blockstates/silver_birch_log.json')
        self.assertEqual({'axis=x','axis=y','axis=z'},set(states['variants']))
        for state in states['variants'].values():self.assertEqual('mynx_trees:block/silver_birch_log',state['model'])
        with zipfile.ZipFile(MC) as mc:
            column=json.loads(mc.read('assets/minecraft/models/block/cube_column.json'))
            cube=json.loads(mc.read('assets/minecraft/models/block/cube.json'))
            faces=cube['elements'][0]['faces']
            self.assertEqual({'down','up','north','south','west','east'},set(faces))
            for face,data in faces.items():
                self.assertEqual('#'+face,data['texture'])
                self.assertEqual('#end' if face in ['up','down'] else '#side',column['textures'][face])
    def test_base_bark_client_scope_cache_and_reload_contract(self):
        folder=ROOT/'src/client/java/dev/resivore/mynxtrees'
        registration=(folder/'SilverBirchBaseModels.java').read_text();model=(folder/'SilverBirchBaseModel.java').read_text()
        self.assertIn('state.is(MynxTrees.SILVER_LOG)',registration);self.assertIn('state.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.Y',registration)
        self.assertIn('modifyBlockModelOnLoad()',registration);self.assertNotIn('modifyItemModel',registration)
        self.assertIn('BASE_MODEL.resolveDependencies(resolver)',registration);self.assertIn('BASE_MODEL.bake(baker)',registration)
        self.assertIn('TAGS_LOADED.register',registration);self.assertIn('if (client)',registration);self.assertIn('minecraft.execute(',registration);self.assertIn('minecraft.levelExtractor.allChanged()',registration)
        self.assertIn('level::getBlockState',model);self.assertIn('below.is(SilverBirchBaseModels.SOILS)',model)
        self.assertIn('GroundContact.geometryKey(this, grounded,',model);self.assertIn('delegate(grounded).createGeometryKey',model)
        self.assertEqual(1,model.count('.emitQuads('));self.assertIn('ordinary.materialFlags() | base.materialFlags()',model)
        for forbidden in ['getChunk','ServerLevel','ClientTickEvents','new Block','BlockEntity','pushTransform']:
            self.assertNotIn(forbidden,registration+model)
        meta=json.loads((ROOT/'src/main/resources/fabric.mod.json').read_text())
        for dependency in ['iris','continuity','sodium']:self.assertNotIn(dependency,meta['depends'])
        for p in (OUT/'assets/mynx_trees/models').rglob('*.json'):
            for ref in json.loads(p.read_text()).get('textures',{}).values():
                if ref.startswith('mynx_trees:'):self.assertTrue((OUT/('assets/mynx_trees/textures/'+ref.split(':',1)[1]+'.png')).exists(),ref)
    def test_final_jar_exact_assets_and_no_reference_binaries(self):
        jars=list((ROOT/'build/libs').glob('mynx-trees-private-*.jar'));self.assertEqual(1,len(jars))
        with zipfile.ZipFile(jars[0]) as z:
            for p in OUT.rglob('*'):
                if p.is_file():self.assertEqual(p.read_bytes(),z.read(p.relative_to(OUT).as_posix()))
            self.assertFalse(any(n.endswith('.jar') for n in z.namelist()));self.assertFalse(any(n.startswith(('assets/minecraft/','io/github/uhq_games/','com/nemonotfound/')) for n in z.namelist()))
if __name__=='__main__':unittest.main(verbosity=2)
