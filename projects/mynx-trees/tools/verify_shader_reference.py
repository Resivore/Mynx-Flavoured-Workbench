"""Read-only validation of the exact Iris/Complementary seam; no profile mutation or runtime launch."""
import argparse,hashlib,json,pathlib,struct,zipfile
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
    with zipfile.ZipFile(args.shader) as z:
        mapping=z.read(ref['block_properties_entry']).decode();leaf=next(l for l in mapping.splitlines() if l.startswith('block.10009='))
        assert 'birch_leaves' in leaf and 'cherry_leaves' in leaf
        waving=z.read('shaders/lib/materials/materialMethods/wavingBlocks.glsl').decode()
        assert '#ifdef WAVING_LEAVES' in waving and 'if (mat == 10009)' in waving and 'DoWave_Leaves(playerPos.xyz, worldPos, 1.0)' in waving
    print('STATIC PASS: exact Iris static target/descriptor and mutable map, configured mixin selector, Complementary leaf ID and leaf-wind dispatch. No runtime waving observed.')
if __name__=='__main__':main()
