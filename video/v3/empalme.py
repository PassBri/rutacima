import subprocess, numpy as np, wave, sys
SR=44100; D=sys.argv[1]
def load(p): return np.frombuffer(subprocess.run(['ffmpeg','-v','error','-i',p,'-f','s16le','-ac','1','-ar',str(SR),'-af','highpass=f=70','-'],capture_output=True).stdout,np.int16).astype(np.float32)/32768
x=load(D+'/voz2/voz.mp3'); n=load(D+'/voz3/tono.wav')
def cut(src,a,b,f=0.012):
    s=src[int(a*SR):int(b*SR)].copy(); k=int(f*SR); s[:k]*=np.linspace(0,1,k); s[-k:]*=np.linspace(1,0,k); return s
rms=lambda s: np.sqrt((s[np.abs(s)>0.02]**2).mean())
nuevo=cut(n,0.25,0.90,0.008); nuevo*=rms(cut(x,5.40,5.83))/rms(nuevo)
O=lambda a,b: cut(x,a,b); Z=lambda d: np.zeros(int(d*SR)); P=0.07
s=lambda a,b: O(a-P,b+P); une=lambda *p: np.concatenate(p)
frases=[une(O(0.17,0.69),Z(0.03),nuevo), une(s(2.23,4.44),Z(0.45),nuevo,Z(0.02),O(5.94,8.16+P)),
 une(s(9.10,10.49),Z(0.45),s(11.48,14.80)), une(s(15.80,18.04),Z(0.45),s(19.03,22.80)), une(s(23.74,25.10),Z(0.45),s(26.11,29.32)),
 une(s(30.31,32.19),Z(0.45),s(33.18,36.68)), une(s(38.11,40.07),Z(0.45),s(41.09,44.14)), une(nuevo,Z(0.6),s(46.73,48.05))]
for i,y in enumerate(frases):
    o=wave.open(f'{D}/voz2/v{i}.wav','w'); o.setnchannels(1); o.setsampwidth(2); o.setframerate(SR); o.writeframes((np.clip(y,-1,1)*32767).astype(np.int16).tobytes()); o.close()
