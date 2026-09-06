"""Read-only validation of the exact Iris/Complementary seam; no profile mutation or runtime launch."""
import argparse,hashlib,json,pathlib,re,struct,zipfile
ROOT=pathlib.Path(__file__).resolve().parents[1]

def methods(data):
    pos=8
    def u2():
        nonlocal pos
        value=struct.unpack_from('>H',data,pos)[0];pos+=2;return value
    assert data[:4]==b'\xca\xfe\xba\xbe'
    pool=[None]*u2();i=1
    while i<len(pool):
        tag=data[pos];pos+=1
        if tag==1:
            length=u2();pool[i]=data[pos:pos+length].decode('utf-8',errors='replace');pos+=length
        elif tag in [3,4]:pos+=4
        elif tag in [5,6]:pos+=8;i+=1
        elif tag in [7,8,16,19,20]:pos+=2
        elif tag in [9,10,11,12,17,18]:pos+=4
        elif tag==15:pos+=3
        else:raise ValueError('Unknown constant-pool tag '+str(tag))
        i+=1
    pos+=6;interfaces=u2();pos+=2*interfaces
    def attrs():
        nonlocal pos
        for _ in range(u2()):
            pos+=2;size=struct.unpack_from('>I',data,pos)[0];pos+=4+size
    for _ in range(u2()):pos+=6;attrs()
    result=[]
    for _ in range(u2()):
        access=u2();name=pool[u2()];desc=pool[u2()];attrs();result.append((name,desc,access))
    return result

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--iris',type=pathlib.Path,required=True);parser.add_argument('--shader',type=pathlib.Path,required=True);args=parser.parse_args()
    ref=json.loads((ROOT/'build-inputs.json').read_text())['shader_reference']
    for p,h in [(args.iris,ref['iris_sha256']),(args.shader,ref['shader_sha256'])]:assert hashlib.sha256(p.read_bytes()).hexdigest()==h,'Reference hash mismatch: '+str(p)
    with zipfile.ZipFile(args.iris) as z:
        entry=ref['target'].split('#')[0].replace('.','/')+'.class';data=z.read(entry)
        matching=[m for m in methods(data) if m[0]=='createBlockStateIdMap' and m[1]==ref['descriptor']]
        assert len(matching)==1 and matching[0][2]&0x0008,'Expected exact static Iris return hook'
        assert b'Object2IntLinkedOpenHashMap' in data,'Expected mutable map construction'
    hook=(ROOT/'src/client/java/dev/resivore/mynxtrees/mixin/IrisLeafMaterialMixin.java').read_text()
    assert 'createBlockStateIdMap'+ref['descriptor'] in hook,'Mixin and inspected target differ'
    assert 'inheritUnmappedFromFirstPresent' in hook and 'Blocks.BIRCH_LEAVES' not in hook,'Silver Birch must use dynamic upper foliage, not birch leaves'
    assert not re.search(r'\b1\d{4}\b',hook),'Shader material IDs must not be hardcoded in the mixin'
    with zipfile.ZipFile(args.shader) as z:
        mapping=z.read(ref['block_properties_entry']).decode()
        upper=next((line for line in mapping.splitlines() if line.startswith('block.') and 'sunflower:half=upper' in line),None)
        assert upper is not None,'Expected a mapped upper sunflower foliage representative'
        material=re.fullmatch(r'block\.(\d+)=(.*)',upper)
        assert material is not None and all(state in material.group(2) for state in ['sunflower:half=upper','lilac:half=upper','rose_bush:half=upper','peony:half=upper'])
        material_id=material.group(1)
        waving=z.read('shaders/lib/materials/materialMethods/wavingBlocks.glsl').decode()
        upper_waving=re.search(r'else if \(mat == '+re.escape(material_id)+r'\).*?DoWave_Foliage\(playerPos\.xyz, worldPos, 1\.0\)',waving,re.S)
        assert upper_waving is not None,'Upper foliage source must reach foliage waving'
        terrain=z.read('shaders/lib/materials/materialHandling/terrainMaterials.glsl').decode()
        upper_treatment=terrain.split('else if (mat == '+material_id+')',1)
        assert len(upper_treatment)==2,'Expected upper foliage material treatment'
        upper_treatment=upper_treatment[1].split('} else if',1)[0]
        assert 'DoFoliageColorTweaks' in upper_treatment and 'leaves.glsl' not in upper_treatment,'Upper foliage must avoid the normal leaves treatment'
    print('STATIC PASS: exact Iris target/descriptor, dynamic upper-foliage representative, foliage waving and non-leaves material treatment verified. No runtime waving observed.')
if __name__=='__main__':main()
