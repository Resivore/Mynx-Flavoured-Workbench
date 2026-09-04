"""Focused package-contract checks, not Minecraft runtime validation."""
import json, pathlib, zipfile, hashlib, unittest
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
        inv=self.read('assets/mynx_trees/items/silver_birch_leaves.json');self.assertNotIn('tints',str(inv));self.assertEqual('minecraft:item/generated',self.read('assets/mynx_trees/models/item/silver_birch_leaves.json')['parent'])
    def test_bounded_deterministic_decoration_and_native_interactions(self):
        source=(ROOT/'src/main/java/dev/resivore/mynxtrees/GroveFlowers.java').read_text();self.assertIn('context.random()',source);self.assertIn('state.canSurvive',source);self.assertIn('!context.isAir(pos)',source);self.assertNotIn('new Random',source)
        common=(ROOT/'src/main/java/dev/resivore/mynxtrees/MynxTrees.java').read_text();self.assertIn('extends SaplingBlock',common);self.assertIn('extends FlowerBedBlock',common)
        self.assertEqual(4,common.count('StrippableBlockRegistry.register('));self.assertIn('CompostableRegistry.INSTANCE.add(SWEET_VIOLETS, 0.3F)',common)
        self.assertIn('BiomeSelectors.includeByKey(Biomes.OLD_GROWTH_BIRCH_FOREST)',common);self.assertIn('BiomeSelectors.includeByKey(Biomes.CHERRY_GROVE)',common)
        self.assertIn('vanillaPlaced("flower_cherry")',common)
    def test_final_jar_exact_assets_and_no_reference_binaries(self):
        jars=list((ROOT/'build/libs').glob('mynx-trees-private-*.jar'));self.assertEqual(1,len(jars))
        with zipfile.ZipFile(jars[0]) as z:
            for p in OUT.rglob('*'):
                if p.is_file():self.assertEqual(p.read_bytes(),z.read(p.relative_to(OUT).as_posix()))
            self.assertFalse(any(n.endswith('.jar') for n in z.namelist()));self.assertFalse(any(n.startswith(('assets/minecraft/','io/github/uhq_games/','com/nemonotfound/')) for n in z.namelist()))
if __name__=='__main__':unittest.main(verbosity=2)
