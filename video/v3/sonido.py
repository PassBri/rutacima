import numpy as np, json, sys, wave
from scipy.signal import butter, sosfilt, resample_poly, fftconvolve
S = sys.argv[1]; DUR = 61.5; SR = 44100; N = int(DUR * SR); rng = np.random.default_rng(11); t = np.arange(N) / SR
def leer(p):
    w = wave.open(p); x = np.frombuffer(w.readframes(w.getnframes()), np.int16).astype(np.float32) / 32768; r = w.getframerate()
    if w.getnchannels() == 2: x = x.reshape(-1, 2).mean(1)
    return resample_poly(x, SR, r) if r != SR else x
def flt(x, f, k='low', o=2): return sosfilt(butter(o, f, k, fs=SR, output='sos'), x)
def norm(x, v): return x / (np.abs(x).max() + 1e-9) * v
# Reverb: respuesta impulsiva sintética (ruido con caída exponencial, oscurecida)
def ir(seg, caida):
    n = int(seg * SR); tt = np.arange(n) / SR; h = rng.standard_normal(n) * np.exp(-tt * caida); h = flt(h, 4500); h[0] += 1.5; return h / np.abs(h).sum() * 40
IR = ir(3.2, 2.0)
def rev(x, mezcla): return x * (1 - mezcla) + norm(fftconvolve(x, IR)[:len(x)], np.abs(x).max()) * mezcla
voces = json.load(open(S + '/v3/voz.json'))
voz = np.zeros(N)
for i, s0 in enumerate(voces):
    x = leer(f'{S}/voz2/v{i}.wav'); s = int(s0 * SR); e = min(N, s + len(x)); voz[s:e] += x[:e - s]
voz = norm(voz, 0.92)
voz = voz * 0.9 + norm(fftconvolve(voz, ir(0.6, 9))[:N], 0.92) * 0.1   # un poco de sala, voz cálida
act = flt(np.abs(voz), 4, o=1); act = np.clip(act / (act.max() + 1e-9) * 5, 0, 1); duck = 1 - 0.5 * act
# Música: pad cálido (acordes Re mayor · Si menor · Sol · La), voces desafinadas, filtro bajo, ataque lento
def nota(f, n, amp=1.0):
    tt = np.arange(n) / SR; x = np.zeros(n)
    for d in (-0.12, 0.0, 0.13):
        ph = 2 * np.pi * f * (1 + d / 100) * tt + rng.uniform(0, 6.28)
        x += np.sin(ph) + 0.35 * np.sin(2 * ph) + 0.12 * np.sin(3 * ph)
    return x * amp
acordes = [[146.83, 220.0, 293.66, 369.99], [123.47, 185.0, 246.94, 293.66], [98.0, 196.0, 246.94, 293.66], [110.0, 220.0, 277.18, 329.63]]
pad = np.zeros(N); L = 4.0
for k in range(int(DUR / L) + 1):
    s = int(k * L * SR); n = min(N - s, int((L + 2.5) * SR))
    if n <= 0: break
    tt = np.arange(n) / SR; e = np.minimum(1, tt / 1.6) ** 2 * np.exp(-np.maximum(0, tt - L) * 1.4)
    for f in acordes[k % 4]: pad[s:s + n] += e * nota(f, n, 0.25)
pad = flt(pad, 1400); pad = norm(rev(pad, 0.35), 0.11)
# Arpegio suave tipo celesta/piano, entra tras la intro, notas con caída natural
arp = np.zeros(N); paso = 0.5
for k in range(int(3.4 / paso), int(56.0 / paso)):
    ac = acordes[int(k * paso / L) % 4]; f = ac[[1, 2, 3, 2][k % 4]] * 2
    s = int(k * paso * SR); n = min(N - s, int(2.2 * SR)); tt = np.arange(n) / SR
    e = (1 - np.exp(-tt * 300)) * np.exp(-tt * 2.6) * (0.75 if k % 4 else 1)
    arp[s:s + n] += e * (np.sin(2 * np.pi * f * tt) + 0.25 * np.sin(4 * np.pi * f * tt) * np.exp(-tt * 6))
arp = norm(rev(flt(arp, 3500), 0.45), 0.07)
mus = (pad + arp)
# Entrada y salida de la música
fade = np.minimum(1, t / 1.5) * np.minimum(1, (DUR - t) / 2.5)
# Ambiente: viento muy suave y algunos pájaros lejanos (con reverb y filtrados)
viento = flt(flt(rng.standard_normal(N), 90, 'high'), 420); viento *= 0.6 + 0.4 * np.sin(2 * np.pi * 0.05 * t) ** 2; viento = norm(viento, 0.03)
paj = np.zeros(N)
for s0 in rng.uniform(4, DUR - 4, 9):
    for j in range(rng.integers(2, 4)):
        d = rng.uniform(0.06, 0.1); n = int(d * SR); tt = np.arange(n) / SR; f0 = rng.uniform(2300, 3300); f1 = f0 * rng.uniform(1.1, 1.3)
        x = np.sin(2 * np.pi * (f0 * tt + (f1 - f0) * tt ** 2 / (2 * d))) * np.sin(np.pi * tt / d) ** 2
        s = int((s0 + j * 0.14) * SR); paj[s:s + n] += x
paj = norm(rev(flt(paj, 5000), 0.6), 0.018)
# Transiciones: un "whoosh" suave + dos pasos en grava; taps del guía: un clic suave
fx = np.zeros(N)
def paso_grava(s):
    n = int(0.16 * SR); tt = np.arange(n) / SR; x = flt(rng.standard_normal(n), [300, 2400], 'band') * np.exp(-tt * 28) * (1 - np.exp(-tt * 400))
    x += 0.6 * np.sin(2 * np.pi * 85 * tt) * np.exp(-tt * 40); fx[s:s + n] += x * 0.5
for c in [13.5, 21.4, 31.8, 39.4, 47.4]:
    n = int(1.0 * SR); s = int((c - 0.55) * SR); tt = np.arange(n) / SR
    w = flt(rng.standard_normal(n), [250 + 900 * tt.mean(), 1800], 'band') * np.sin(np.pi * tt / 1.0) ** 2; fx[s:s + n] += norm(w, 0.08)
    paso_grava(int((c - 0.1) * SR)); paso_grava(int((c + 0.32) * SR))
taps = [g for g in json.load(open(S + '/v3/guia.json')) if g['tap']]
for g in taps:
    s = int(g['b'] * SR); n = int(0.09 * SR); tt = np.arange(n) / SR
    fx[s:s + n] += 0.09 * np.sin(2 * np.pi * 1320 * tt) * np.exp(-tt * 60) + 0.05 * np.sin(2 * np.pi * 660 * tt) * np.exp(-tt * 45)
fx = rev(fx, 0.25)
# Logo: campana suave al dibujarse (intro) y al final
for c in [0.4, 57.2]:
    s = int(c * SR); n = int(3 * SR); tt = np.arange(n) / SR
    fx[s:s + n] += 0.08 * sum(a * np.sin(2 * np.pi * f * tt) * np.exp(-tt * dd) for f, a, dd in [(587.3, 1, 1.4), (880, .5, 2), (1174.7, .3, 2.6)])
import os
MU = S + '/v3/musica_larga.wav'
if os.path.exists(MU):
    w = wave.open(MU); m = np.frombuffer(w.readframes(w.getnframes()), np.int16).astype(np.float32).reshape(-1, 2) / 32768
    m = m[:N] if len(m) >= N else np.vstack([m, np.zeros((N - len(m), 2))])
    musL, musR = m[:, 0], m[:, 1]; pk = np.abs(m).max() + 1e-9; musL, musR = musL / pk * 0.30, musR / pk * 0.30
    fade = np.minimum(1, t / 0.8) * np.minimum(1, (DUR - t) / 2.5)
    amb = (viento + paj) * 0.6
    base = voz + fx + amb * duck
    L = base + musL * fade * duck; R = base + musR * fade * duck
    st = np.stack([L, R], 1); st = np.tanh(st * 1.05) / 1.05; st = np.clip(st / (np.abs(st).max() + 1e-9) * 0.95, -1, 1)
    w = wave.open(S + '/v3/mezcla.wav', 'w'); w.setnchannels(2); w.setsampwidth(2); w.setframerate(SR); w.writeframes((st * 32767).astype(np.int16).tobytes()); w.close()
    print('ok con su música'); sys.exit()
fondo = (mus * fade + viento + paj) * duck
mix = voz + fondo + fx
mix = np.tanh(mix * 1.1) / 1.1
izq = mix + 0.0 * mus; der = mix
# Pequeño ancho estéreo para la música
sep = int(0.012 * SR); der = mix.copy(); der[sep:] = mix[sep:] - 0.5 * (mus * fade * duck)[:-sep] + 0.5 * (mus * fade * duck)[sep:]
st = np.stack([izq, der], 1); st = np.clip(st / (np.abs(st).max() + 1e-9) * 0.95, -1, 1)
w = wave.open(S + '/v3/mezcla.wav', 'w'); w.setnchannels(2); w.setsampwidth(2); w.setframerate(SR); w.writeframes((st * 32767).astype(np.int16).tobytes()); w.close()
print('ok')
