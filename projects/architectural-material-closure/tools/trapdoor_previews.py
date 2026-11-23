"""Local orthographic reference sheets rendered from packaged model faces and GUI transforms."""
from functools import lru_cache
import io
import json

import numpy as np
from PIL import Image, ImageDraw

# Corners in provider face UV order: upper left, upper right, lower left.
CORNERS = {"up": ("010","110","011"), "down": ("001","101","000"),
           "north": ("110","010","100"), "south": ("011","111","001"),
           "west": ("010","011","000"), "east": ("111","110","101")}


def rotation(axis, angle):
    a = np.radians(angle)
    c, s = np.cos(a), np.sin(a)
    return np.array({"x": [[1,0,0],[0,c,-s],[0,s,c]],
                     "y": [[c,0,s],[0,1,0],[-s,0,c]],
                     "z": [[c,-s,0],[s,c,0],[0,0,1]]}[axis])


class Graph:
    def __init__(self, archives):
        self.archives = archives

    @lru_cache(maxsize=None)
    def get(self, ref, folder, extension):
        ns, path = (ref if ":" in ref else "minecraft:"+ref).split(":")
        name = f"assets/{ns}/{folder}/{path}.{extension}"
        for jar, prefix in self.archives:
            if prefix+name in jar.NameToInfo:
                raw = jar.read(prefix+name)
                return json.loads(raw) if extension=="json" else Image.open(io.BytesIO(raw)).convert("RGBA")
        raise KeyError(name)

    @lru_cache(maxsize=None)
    def resolve(self, ref):
        model = self.get(ref,"models","json")
        base = self.resolve(model["parent"]) if "parent" in model else {}
        result = dict(base,**model)
        for k in ("textures","display"):
            result[k] = {**base.get(k,{}),**model.get(k,{})}
        return result

    def view(self, branch, gui=False):
        model = self.resolve(branch["model"])
        pose = model["display"]["gui"] if gui else {"rotation":[32,-30,0],"scale":[0.66]*3}
        rx,ry,rz = pose.get("rotation",[0,0,0])
        camera = rotation("x",rx) @ rotation("y",ry) @ rotation("z",rz) @ np.diag(pose["scale"])
        state = rotation("y",-branch.get("y",0)) @ rotation("x",-branch.get("x",0))
        canvas = np.zeros((76,76,4),dtype=np.uint8)
        depth = np.full((76,76),-np.inf)
        for element in model["elements"]:
            low,high = np.array(element["from"]),np.array(element["to"])
            rot = element.get("rotation",{"axis":"y","angle":0,"origin":[8]*3})
            pivot = np.array(rot["origin"])
            for side, face in element["faces"].items():
                points = np.array([np.where([k=="1" for k in code],high,low) for code in CORNERS[side]])
                points = (points-pivot) @ rotation(rot["axis"],rot["angle"]).T + pivot
                points = (points-8) @ state.T @ camera.T + np.array(pose.get("translation",[0,0,0]))
                points = points * [4,-4,1] + [38,38,0]
                p,q,r = points
                basis = np.stack([q[:2]-p[:2],r[:2]-p[:2]],axis=1)
                if np.linalg.det(basis) < 0.001:
                    continue
                bounds = np.vstack((points,q+r-p))
                x,y = np.maximum(0,np.floor(bounds[:,:2].min(axis=0)).astype(int))
                xx,yy = np.minimum(76,np.ceil(bounds[:,:2].max(axis=0)).astype(int))
                if x>=xx or y>=yy:
                    continue
                ys,xs = np.mgrid[y:yy,x:xx]
                u,v = np.linalg.solve(basis,np.vstack(((xs+0.5-p[0]).ravel(),(ys+0.5-p[1]).ravel())))
                z = p[2]+u*(q[2]-p[2])+v*(r[2]-p[2])
                select = (u>=0)&(v>=0)&(u<1)&(v<1)&(z>=depth[ys,xs].ravel())
                for _ in range(face.get("rotation",0)//90):
                    u,v = v,1-u
                ref = face["texture"]
                while ref.startswith("#"):
                    ref = model["textures"][ref[1:]]
                tile = np.asarray(self.get(ref,"textures","png"))
                u0,v0,u1,v1 = face["uv"]
                px = np.floor(u0+(u1-u0)*u).astype(int).clip(0,15)
                py = np.floor(v0+(v1-v0)*v).astype(int).clip(0,15)
                rgba = tile[py,px]
                select &= rgba[:,3]>0
                ix,iy = xs.ravel()[select],ys.ravel()[select]
                canvas[iy,ix] = rgba[select]
                depth[iy,ix] = z[select]
        assert np.any(canvas[:,:,3]), branch
        return Image.fromarray(canvas)


def sheet(graph, rows, wood, folder):
    sheet = Image.new("RGB",(560, len(rows)*81+47),(38,42,47))
    draw = ImageDraw.Draw(sheet)
    draw.text((8,5),wood.replace("_"," ").title()+"    |    Macaw Trapdoors",fill="white")
    for i,label in enumerate(("Reference","Closed","Open","Inventory")):
        draw.text((135+i*100,26),label,fill="white")
    for index,(design,source,target,item) in enumerate(rows):
        y = 46+81*index
        draw.text((8,y+28),design.replace("_"," ").title(),fill="white")
        key = "facing=north,half=bottom,open="
        branches = [source[key+"false"],target[key+"false"],target[key+"true"],item]
        for i,branch in enumerate(branches):
            view = graph.view(branch,gui=i==3)
            sheet.paste(view,(130+100*i,y),view)
    folder.mkdir(parents=True,exist_ok=True)
    sheet.save(folder/(wood+".png"))
