import sys, zlib, base64
def compact(path, nd=4):
    V={};T={};N={};vl=[];tl=[];nl=[];out=[];raw_v=[];raw_t=[];raw_n=[]
    for line in open(path):
        p=line.split()
        if not p: continue
        if p[0]=='v': raw_v.append(tuple(round(float(x),nd) for x in p[1:4]))
        elif p[0]=='vt': raw_t.append(tuple(round(float(x),nd) for x in p[1:3]))
        elif p[0]=='vn': raw_n.append(tuple(round(float(x),3) for x in p[1:4]))
    def idx(d,l,k):
        if k not in d: d[k]=len(l)+1; l.append(k)
        return d[k]
    for line in open(path):
        p=line.split()
        if not p: continue
        if p[0]=='o': out.append(('o',p[1]))
        elif p[0]=='f':
            f=[]
            for t in p[1:]:
                a,b,c=[int(x)-1 for x in t.split('/')]
                f.append((idx(V,vl,raw_v[a]),idx(T,tl,raw_t[b]),idx(N,nl,raw_n[c])))
            out.append(('f',f))
    s=[]
    s+=['v %g %g %g'%v for v in vl]+['vt %g %g'%t for t in tl]+['vn %g %g %g'%n for n in nl]
    for k,v in out:
        s.append('o '+v if k=='o' else 'f '+' '.join('%d/%d/%d'%t for t in v))
    return '\n'.join(s)+'\n'
if __name__=='__main__':
    t=compact(sys.argv[1]);b=base64.b64encode(zlib.compress(t.encode(),9)).decode()
    print(len(t),len(b));open(sys.argv[2],'w').write(b)
