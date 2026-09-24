# ✧ ASTRO 3D — Next-Generation Astrology Space Experience

A complete 3D astrology space simulator written in **pure Java (Swing + Java2D)**.
No libraries, no build tools — just the JDK.

## ✨ Features

- 🌌 **360° free-look reality view** — drag anywhere to orbit the whole sky in every direction, wheel to zoom, Shift+drag to pan
- 🪐 **Full solar system** — Sun + 8 planets with real relative orbit periods, inclinations and ascending nodes
- 🌙 **Moon orbiting Earth** (27.3-day period), **comet** on an elliptical Kepler orbit with a sun-facing tail
- ☄️ **Asteroid belt** (900 particles), **meteors**, **twinkling 6000-star sky**, **milky-way band**, **colored nebulae**
- ♈ **Zodiac constellations** drawn on the sky sphere with sign glyphs
- 🔮 **Live astrology HUD** — geocentric zodiac wheel + planetary positions table with sign, degree, ruler and element
- 🎯 **Click-free focus**: keys 1–9/0 snap the camera to any planet, `F` follows it
- 📷 **P** saves a PNG screenshot with timestamp
- Time control: pause, speed up (sim days per second), reset

## 🚀 Run

Windows: double-click **`run.bat`**
Mac/Linux: `./run.sh`
Manual: `javac -encoding UTF-8 -d bin src/astro3d/**/*.java && java -cp bin astro3d.Main`

Requires **JDK 17+** (tested on JDK 26).

## 🎮 Controls

| Input | Action |
|---|---|
| Drag | Orbit camera (full 360° yaw, ±88° pitch) |
| Shift+drag | Pan view |
| Mouse wheel | Zoom in/out |
| `1`–`9`, `0` | Focus a planet (Mercury→Neptune) |
| `F` | Follow the focused body |
| `Esc` | Free view again |
| `O` | Toggle orbit lines |
| `B` | Toggle asteroid belt |
| `C` | Toggle zodiac constellations |
| `Space` | Pause time |
| `+` / `-` | Time speed up / down |
| `P` | Screenshot PNG |
| `R` | Reset view |

## 🧠 Architecture

```
src/astro3d/
 ├ Main.java          window, 60 FPS game loop, input
 ├ core/Vec3.java     3D vector math + perspective projection
 ├ core/Camera.java   orbit camera, 360° yaw/pitch, zoom/pan
 ├ model/Body.java    planet model incl. astrology attributes
 ├ model/Zodiac.java  zodiac signs, rulers, geocentric longitude math
 ├ model/SolarSystem.java  builds and animates all bodies
 ├ model/Comet.java   Kepler ellipse + tail physics
 ├ scene/Starfield.java    stars, milky way, nebulae, constellations, meteors
 ├ scene/Renderer.java     depth-sorted painter renderer with glow/rings/shading
 └ ui/Hud.java       zodiac wheel + planetary positions + help overlay
```

## 🔭 Astrology math

Geocentric ecliptic longitude is computed from heliocentric positions
(`atan2(z − z☉, x − x☉)`), mapped to one of 12 signs (30° each), with
degrees/minutes within the sign, traditional rulers and the four elements.
The HUD wheel shows every body at its live geocentric position — a
simplified but real ephemeris model, not random decoration.
