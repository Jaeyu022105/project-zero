import math
SIZES={'SMALL':(2,2),'MEDIUM':(2,3),'LARGE':(3,4)}
# Stones cling to the rift's glowing line: the band straddles it (rift_ring.png is brightest at
# about 0.9 R), and its thickness wobbles with the angle so it reads as rough rock, not a hoop.
def band(theta, R):
    wob = 0.22 * math.sin(3 * theta + 0.7) + 0.14 * math.sin(7 * theta + 2.1)
    return R * 0.9 + 0.06 * math.sin(5 * theta), R + 0.5 + wob
def ring(pw,ph):
    R=max(pw,ph)*0.62+0.35; uc,hc=pw/2,ph/2
    cells={}
    for h in range(0,ph+6):
        for u in range(-6,pw+6):
            if 0<=u<pw and h<ph: continue
            m=0
            for j in range(4):
                for i in range(4):
                    x=u+(i+.5)/4-uc; y=h+(j+.5)/4-hc
                    d=math.hypot(x,y)
                    lo,hi=band(math.atan2(y,x),R)
                    if lo<=d<hi: m|=1<<(j*4+i)
            if bin(m).count('1')>=4: cells[(u,h)]=m   # no slivers
    # keystones: the fullest upper cell of the centre column(s)
    centre=[u for u in range(-6,pw+6) if abs(u+.5-uc)<=0.5]
    keys=set()
    for u in centre:
        hs=[h for (uu,h) in cells if uu==u and h>=ph]
        if hs: keys.add((u,max(hs,key=lambda h:(bin(cells[(u,h)]).count('1'),h))))
    return [(u,h,m,(u,h) in keys) for (u,h),m in sorted(cells.items(),key=lambda k:(k[0][1],k[0][0]))]
def show():
    for n,(pw,ph) in SIZES.items():
        cs=ring(pw,ph); print(n,len(cs))
        g={}
        for u,h,m,k in cs:
            for j in range(4):
                for i in range(4):
                    if m>>(j*4+i)&1: g[(u*4+i,h*4+j)]='K' if k else '#'
        for y in range(max(v[1] for v in g),-1,-1):
            print(''.join(g.get((x,y),'.') for x in range(min(v[0] for v in g),max(v[0] for v in g)+1)))
if __name__=='__main__': show()
