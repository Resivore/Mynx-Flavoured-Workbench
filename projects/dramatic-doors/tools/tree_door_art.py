"""Bind native short/tall models and retain the supplied Wisteria front panels."""
from io import BytesIO
import json
from pathlib import Path
import sys
from zipfile import ZipFile
from PIL import Image
from amc_macaw import ranked, palette, write


def build(dd, trees, assets, out):
    if sys.platform == "win32":
        out=Path("//?/"+out.resolve().as_posix())
    for wood in ("silver_birch","wisteria"):
        pack=out/"resourcepacks"/(wood+"_door")
        write(pack,"pack.mcmeta",{"pack":{"description":"Dramatic Doors / "+wood,"min_format":88,"max_format":[107,1]}})
        tags={}
        labels={loc:{} for loc in ("en_us","ru_ru","zh_cn")}
        def recolor(data):
            image=Image.open(BytesIO(data)).convert("RGBA")
            material=Image.open(BytesIO(trees.read("assets/mynx_trees/textures/block/"+wood+"_planks.png"))).convert("RGBA")
            colors={p[:3] for p in image.getdata() if p[3] and p[0]>p[1] and p[1]>p[2]+7}
            mapping=ranked(colors,palette(material))
            image.putdata([(*mapping.get(p[:3],p[:3]),p[3]) for p in image.getdata()])
            return image
        for height in ("short","tall"):
            old=height+"_birch_door";name=height+"_"+("mynx_wisteria" if wood=="wisteria" else wood)+"_door"
            for path in dd.namelist():
                if not path.startswith("assets/dramaticdoors/") or not Path(path).stem.startswith(old):
                    continue
                if Path(path).stem!=old and not Path(path).stem.startswith(old+"_"):continue
                dest=pack/path.replace(old,name);dest.parent.mkdir(parents=True,exist_ok=True)
                if path.endswith(".json"):
                    dest.write_bytes(dd.read(path).replace(old.encode(),name.encode()).replace(b"minecraft:block/birch_",("mynx_trees:block/"+wood+"_").encode()))
                elif path.endswith(".png"):
                    if wood=="wisteria" and "/block/" in path:
                        if height=="short":
                            dest.write_bytes((assets/"short wisteria door.png").read_bytes())
                        else:
                            offset={"bottom":32,"middle":16,"top":0}[Path(path).stem.split("_")[-1]]
                            with Image.open(assets/"tall wisteria door.png") as panel:panel.crop((0,offset,16,offset+16)).save(dest)
                    elif wood=="wisteria":
                        # Apply the owner's panel to DD's existing short/tall inventory silhouette.
                        with Image.open(assets/(height+" wisteria door.png")) as panel:
                            ref=Image.open(BytesIO(dd.read(path))).convert("RGBA")
                            box=ref.getbbox();icon=Image.new("RGBA",ref.size)
                            icon.paste(panel.convert("RGBA").resize((box[2]-box[0],box[3]-box[1]),Image.Resampling.NEAREST),box[:2]);icon.save(dest)
                    else:
                        recolor(dd.read(path)).save(dest)
            loot="data/dramaticdoors/loot_table/blocks/"+old+".json"
            write(pack,loot.replace(old,name),json.loads(dd.read(loot).replace(old.encode(),name.encode())))
            key="block.dramaticdoors."+name
            labels["en_us"][key]=(height+"_"+wood+"_door").replace("_"," ").title()
            ru="Низкая" if height=="short" else "Высокая"
            zh="矮" if height=="short" else "高"
            labels["ru_ru"][key]=ru+" дверь из "+("глицинии" if wood=="wisteria" else "серебристой берёзы")
            labels["zh_cn"][key]=zh+("紫藤木" if wood=="wisteria" else "银桦木")+"门"
            for path in dd.namelist():
                if path.startswith("data/") and "/tags/" in path and path.endswith(".json"):
                    if "dramaticdoors:"+old in json.loads(dd.read(path))["values"]:
                        tags.setdefault(path,[]).append("dramaticdoors:"+name)
        for path,values in tags.items():
            write(pack,path,{"replace":False,"values":values})
        for loc,strings in labels.items():
            write(pack,"assets/dramaticdoors/lang/"+loc+".json",strings)


if __name__=="__main__":
    with ZipFile(sys.argv[1]) as dd, ZipFile(sys.argv[2]) as trees:
        build(dd,trees,Path(sys.argv[3]),Path(sys.argv[4]))
