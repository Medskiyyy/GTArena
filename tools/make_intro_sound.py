import subprocess
import sys

import numpy as np
from scipy.io import wavfile
from scipy.signal import butter, sosfilt

SR = 44100
DUR = 2.4
LOCK = 1.02
t = np.arange(int(SR * DUR)) / SR
rng = np.random.default_rng(7)


def band(x, lo, hi):
    return sosfilt(butter(2, [lo, hi], btype="band", fs=SR, output="sos"), x)


def env(start, attack, decay):
    e = np.zeros_like(t)
    a = (t >= start) & (t < start + attack)
    d = t >= start + attack
    e[a] = (t[a] - start) / attack
    e[d] = np.exp(-(t[d] - start - attack) / decay)
    return e


rise = np.clip(t / LOCK, 0, 1)
gate = (t < LOCK).astype(float)

freq = 70 + 330 * rise ** 2.2
phase = 2 * np.pi * np.cumsum(freq) / SR
spin = (np.sin(phase) + 0.45 * np.sin(2 * phase) + 0.2 * np.sin(3 * phase)) * rise ** 1.6 * gate
blade = 0.5 + 0.5 * np.sin(2 * np.pi * np.cumsum(6 + 26 * rise) / SR)
spin *= 0.55 + 0.45 * blade

noise = rng.standard_normal(t.size)
whoosh = np.zeros_like(t)
for lo, hi, a, b in [(300, 900, 0.0, 0.5), (700, 2400, 0.3, 0.8), (1800, 6000, 0.6, 1.0)]:
    w = np.clip((rise - a) / (b - a), 0, 1) * np.clip((1.05 - rise) / 0.25, 0, 1)
    whoosh += band(noise, lo, hi) * w
whoosh *= rise ** 1.3 * gate

thump_f = 48 + 90 * np.exp(-np.clip(t - LOCK, 0, None) / 0.05)
thump = np.sin(2 * np.pi * np.cumsum(thump_f) / SR) * env(LOCK, 0.004, 0.22)
click = band(noise, 1500, 9000) * env(LOCK, 0.001, 0.018)
ping = sum(
    g * np.sin(2 * np.pi * f * (t - LOCK)) * env(LOCK, 0.003, d)
    for f, g, d in [(659.3, 0.5, 0.42), (987.8, 0.35, 0.34), (1975.5, 0.18, 0.2), (2637.0, 0.1, 0.14)]
)

sweep_pos = np.clip((t - 1.08) / 0.48, 0, 1)
swish = band(noise, 2500, 9000) * np.sin(np.pi * sweep_pos) ** 2 * 0.5

mono = 0.30 * spin + 0.20 * whoosh + 0.95 * thump + 0.35 * click + 0.34 * ping + 0.10 * swish
mono *= np.clip((DUR - t) / 0.3, 0, 1)
pan = 0.5 + 0.35 * np.sin(2 * np.pi * np.cumsum(1.5 + 5 * rise) / SR) * gate
stereo = np.stack([mono * np.sqrt(1 - pan), mono * np.sqrt(pan)], axis=1)
stereo = np.tanh(stereo * 1.4)
stereo /= np.abs(stereo).max() / 0.89

wav, out = sys.argv[1], sys.argv[2]
wavfile.write(wav, SR, (stereo * 32767).astype(np.int16))
subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-i", wav, "-c:a", "libvorbis", "-q:a", "5", out], check=True)
