# Sink A Startup

A retro sci-fi desktop strategy game built with Java Swing, inspired by classic grid-based naval combat (Battleship). Hunt down and sink startup ventures hidden across a 7x7 tactical sector using visual feedback, particle explosions, sound effects, and camera shake.

---

## Features

- **Self-Contained Architecture:** Entire game logic, rendering loops, and UI are packaged in a single source file (`SinkAStartupSingle.java`).
- **Dynamic Visuals:**
  - Layered rendering for animated explosion particles.
  - Multi-frame button explosions.
  - Screen shake on successful hits and kills.
- **Adaptive Audio:** Looping background audio and dedicated sound effects for hits and kills (falls back to system beeps if audio files are missing).
- **Graceful Fallbacks:** Renders custom UI buttons and color-coded feedback even without image assets.
- **Difficulty Modes:**
  - **Easy:** 3 startups (Size: 3 cells each)
  - **Medium:** 4 startups (Size: 3 cells each)
  - **Hard:** 3 startups (Size: 4 cells each)
- **Developer Debug Mode:** Reveal hidden startup locations in real time to verify placement logic.

---

## How to Play

- **Objective:** Locate and sink all hidden startups across the 7×7 grid (`a0` to `g6`) in as few turns as possible.
- **Controls:** Click any tile to strike. Use the side panel to toggle difficulty, sound, or debug mode.
- **Tile Signals:**
  - `Gray`: Miss
  - `Orange`: Hit
  - `Red`: Sunk (Entire startup destroyed)
    
---

## Note

- **If the assets/ directory or individual files are missing, the game still launches and plays smoothly using procedural rendering and fallback UI themes.**

---

## Directory Structure

To run the game with full visual and sound effects, maintain the following directory structure:

```text
sink-a-startup/
├── SinkAStartupSingle.java
└── assets/
    ├── ships/
    │   ├── ship_poniez.png
    │   ├── ship_hacqi.png
    │   └── ship_cabista.png
    ├── explosions/
    │   ├── explosion1.png
    │   ├── explosion2.png
    │   └── explosion3.png
    └── sound/
        ├── background_music.wav
        ├── hit.wav
        └── kill.wav
