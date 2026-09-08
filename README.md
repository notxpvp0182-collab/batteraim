# Better AIM

**Advanced crosshair, highlight, hitbox, and target HUD customisation for Minecraft Java PvP.**

| Attribute | Value |
|---|---|
| Minecraft | 1.21.11 |
| Loader | Fabric |
| Java | 21 |
| Side | Client-only |
| Author | Ahmad |

---

## Features

### Crosshair
- 11 built-in styles: Cross, Dot, Circle, Hollow Circle, Small Box, Large Box, Hollow Box, Four Corners, Diagonal X, Minimal, Arrow
- Fully configurable: size, thickness, gap, opacity, colour
- **Normal crosshair** and a separate **Hitable crosshair** (shown when a valid target is in reach)
- Per-perspective toggles: first-person, third-person, other cameras

### Target Highlight
- Independent colour system for **players** and **mobs**
- Configurable outline width and opacity
- **Hit feedback**: temporary colour flash when you land a hit

### Hitbox Visualisation
- Wireframe bounding boxes with independent colour, opacity, and line width
- Optional transparent fill
- Separate toggles for players and mobs

### Target HUD
- Panel showing the currently targeted entity's name
- Optional 3D player model preview
- Fully configurable: background colour, border, text colour, scale, position
- Disappears automatically when no target is in range

### All systems are fully independent
Changing the hitable-crosshair colour has no effect on highlight colours, hit-feedback colours, hitbox colours, or HUD colours.

---

## Building

### GitHub Actions (recommended)
Push to any branch — the workflow in `.github/workflows/build.yml` will:
1. Set up Temurin Java 21
2. Install Gradle 8.14 and generate the wrapper
3. Run `./gradlew build`
4. Upload the JAR as a build artifact

```
git add .
git commit -m "Initial Better AIM release"
git push
```

The JAR will appear in the **Actions → your run → Artifacts** section.

### Local build
Requires Java 21 and an internet connection (Loom downloads MC mappings).

```bash
# Unix / macOS
./gradlew build

# Windows
gradlew.bat build
```

Output: `build/libs/better-aim-<version>.jar`

> **Note:** `gradle-wrapper.jar` is generated automatically by the GitHub Actions workflow.  
> For a local first-time build without the JAR, run `gradle wrapper --gradle-version 8.14` once
> (requires Gradle installed on your PATH), then use `./gradlew` normally.

---

## Installation

Place the output JAR into your Minecraft `mods/` folder alongside:
- [Fabric Loader](https://fabricmc.net/use/) ≥ 0.18.1
- [Fabric API](https://modrinth.com/mod/fabric-api) for 1.21.11
- [Mod Menu](https://modrinth.com/mod/modmenu) (optional, for in-game config access)

---

## Configuration

**In-game:** Press **M** (default) or click "Config" in Mod Menu.

**Config file:** `.minecraft/config/better-aim.json`  
Edited automatically — manual editing is supported; corrupted files are backed up to `better-aim.json.bak`.

**Reset:** Use the "Reset Tab" or "Reset All Settings" buttons in the GUI.

---

## Keybindings

| Action | Default |
|---|---|
| Open Better AIM Config | M |
| Toggle Crosshair | (unbound) |
| Toggle Target HUD | (unbound) |
| Toggle Highlight | (unbound) |
| Toggle Hitbox | (unbound) |

Change keybindings in **Options → Controls → Keybinds → Better AIM**.

---

## What this mod does NOT do

- Auto-aim / aim assist
- Auto-attack / kill aura
- Reach modification
- Server-side hitbox manipulation
- X-ray or wallhack

All features are purely visual and client-side.

---

## License

MIT — see [LICENSE](LICENSE).
