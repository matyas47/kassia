# Kassia

**Kassia** is a SuperCollider-based drone instrument built around FM-derived partials, additive synthesis, 
and formant filtering. The instrument generates evolving harmonic structures using a bank of oscillators 
whose frequencies and amplitudes are derived from FM partial analysis. These partials can then be shaped 
with amplitude, panning, modulation, and filtering controls. The result is a flexible system for creating 
slowly evolving drones, installation soundscapes, and spectral textures.Kassia is named after
 **Kassia (Kassiani) of Byzantium**, a 9th-century composer and poet.

---

## Features

- 8-voice additive drone engine
- FM-derived partial generation
- Per-partial amplitude, panning, and modulation controls
- Moog-style master filter
- vowel/formant filtering stage
- very slow modulation support (minutes-long cycles)
- real-time spectrum display
- smooth parameter interpolation for gradual transitions

---

## Requirements

- **SuperCollider 3.13+**
- Tested on Linux (Qt GUI)

No external plugins are required.

---

## Files
KassiaFormant.sc # formant filter implementation
KassiaSynth.sc # drone synthesis engine
Kassia.scd # GUI and instrument launcher


---

## Installation

Clone the repository:

```bash
git clone https://github.com/YOURNAME/kassia.git```

Copy the class files to your SuperCollider Extensions directory:
```
~/.local/share/SuperCollider/Extensions/
```
or
```
/usr/local/share/SuperCollider/Extensions/
```
##Running Kassia
After booting the Supercollider server (s.boot; in most cases),
Open and run:
Kassia.scd

###Use Cases

Use Cases

Kassia was designed primarily for long-form drone composition,
ambient and electroacoustic textures, installation sound 
environments, and spectral exploration of FM partial structures,

###License
GPL v3
This license does not apply to audio generated using this software.
