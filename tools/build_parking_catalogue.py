"""Build an original, solver-verified parking puzzle catalogue. No downloaded levels."""
import random,json
from collections import deque
from pathlib import Path
rng=random.Random(30062026)

def search(spec,start,limit=25000,all_states=False):
    masks=[]
    for axis,fixed,length in spec:
        masks.append([sum(1<<((fixed*6+p+j) if axis else ((p+j)*6+fixed)) for j in range(length)) for p in range(7-length)])
    q=deque([start]);seen={start:(None,None,0)};last=start
    while q and len(seen)<limit:
        s=q.popleft();last=s
        if not all_states and s[0]==4:
            route=[];n=s
            while seen[n][0] is not None:
                parent,move,_=seen[n];route.append(move);n=parent
            return route[::-1]
        occupied=0
        for i,p in enumerate(s): occupied|=masks[i][p]
        for i,p in enumerate(s):
            others=occupied^masks[i][p]
            for delta in (-1,1):
                n=p+delta
                if 0<=n<len(masks[i]) and not others&masks[i][n]:
                    nxt=s[:i]+(n,)+s[i+1:]
                    if nxt not in seen: seen[nxt]=(s,(i,delta),seen[s][2]+1);q.append(nxt)
    return seen if all_states else None

catalog=[];attempt=0
while len(catalog)<24 and attempt<500:
    attempt+=1;spec=[(1,2,2)];positions=[4];occupied=(1<<16)|(1<<17)
    for _ in range(rng.randint(9,11)):
        for trial in range(120):
            axis=rng.randrange(2);length=3 if rng.random()<.25 else 2;fixed=rng.randrange(6);p=rng.randrange(7-length)
            mask=sum(1<<((fixed*6+p+j) if axis else ((p+j)*6+fixed)) for j in range(length))
            if mask&occupied==0: spec.append((axis,fixed,length));positions.append(p);occupied|=mask;break
    if len(spec)<10:continue
    states=search(spec,tuple(positions),limit=18000,all_states=True)
    candidates=sorted(states,key=lambda s:states[s][2],reverse=True)[:30]
    for s in candidates:
        if s[0]>1:continue
        blockers=sum(1 for i,(axis,fixed,length) in enumerate(spec) if i and not axis and fixed>s[0]+1 and s[i]<=2<s[i]+length)
        if blockers<2:continue
        route=search(spec,s)
        if route and 10<=len(route)<=36 and len({i for i,_ in route if i})>=3:
            cars=[dict(id=i,x=p if axis else fixed,y=fixed if axis else p,length=length,horizontal=bool(axis)) for i,((axis,fixed,length),p) in enumerate(zip(spec,s))]
            catalog.append(dict(cars=cars,optimal=len(route),route=route));print('level',len(catalog),'optimal',len(route),'cars',len(cars),flush=True);break
catalog.sort(key=lambda c:c['optimal'])
Path('tools/parking-catalogue.json').write_text(json.dumps(catalog,indent=2))
lines=['package com.thirtyseconds.domain','','/** Original layouts generated offline and verified by shortest-path search. */','internal object ParkingCatalogue {','    val boards=listOf(']
for entry in catalog:
    cars=', '.join('Vehicle({id},{x},{y},{length},{horizontal})'.format(**{**v,'horizontal':str(v['horizontal']).lower()}) for v in entry['cars'])
    lines.append(f"        ParkingLayout({entry['optimal']},listOf({cars})),")
lines+=['    )','}','internal data class ParkingLayout(val optimal:Int,val cars:List<Vehicle>)']
if len(catalog)<12: raise RuntimeError('Insufficient verified layouts')
Path('core/domain/src/main/kotlin/com/thirtyseconds/domain/ParkingCatalogue.kt').write_text('\n'.join(lines)+'\n')
