# FrogPixel Skyblock
**You can test the latest builds of the fabric plugin on our live server, <br>⚠️ Warning all progress will be reset often because this is a test server.** <br>
* *play.frogpixel.com*
* *default port 25565*

 For the discord and other information goto: `https://frogpixel.com/`
##

A server-side only Fabric mod that brings a complete, classic SkyBlock experience—inspired by Noobcrew's original map layout—directly to vanilla clients, WIP a fully integrated resource progression system inspired by *Ex Nihilo*.

Because this mod operates entirely as a server-side framework, **players can connect using a 100% unmodded vanilla Minecraft client**. No client-side mod loaders, or special configurations are required.

---

## 🛠️ Technical Specifications
* **Minecraft Version Support:** `26.1.2`
* **Fabric API/Server Layer:** `api: 0.0.146.1 - loader: 0.0.19.2 - launcher: 0.1.1.1`
* **Fabric Server JAR:** `fabric-server-mc.26.1.2-loader.0.19.2-launcher.1.1.1.jar` and `fabric-api-0.146.1+26.1.2.jar`
* **Mod Environment:** Server-Side Only (Fabric Loader)
* **Client Compatibility:** 100% Vanilla Client Compatible (Zero modifications required by players)

---

## 🌟 Features & Core Mechanics

### 🏝️ 1. Classic Island Generation (by Noobcrew)
Automatically generates isolated, classic-style SkyBlock islands suspended over a completely void world for connecting players. 
* Structured layers containing the classic dirt, bedrock anchor, single grass block, and lone oak tree.
* Completely handles independent coordinate allocation to prevent island overlaps.
* Implements background world-generation interceptors to completely eliminate standard vanilla terrain generation.

### 🔨 2. Hammer Tier Progression (Custom server-side component layer)
Replicates the foundational *Ex Nihilo* material degradation loops using low-level block-break event overrides. Breaking specific blocks with a valid Hammer transforms them into their broken-down variants:
* **Cobblestone** → **Gravel**
* **Gravel** → **Sand**
* **Sand** → **Dust**

### 🧺 3. Sieve Sifting Framework
Allows players to extract essential ores, minerals, and resources out of their processed blocks.
* Players right-click the Sieve while holding material types to roll internal loot tables.
* Drops critical progression items such as Iron Nuggets, Gold Nuggets, Flint, Coal, Redstone, Glowstone Dust, and Nether Quartz without requiring access to a standard mining dimension.

---

## 🤝 Credits & Acknowledgments
This project stands on the shoulders of the incredible map makers and mod developers who pioneered the skyblock genre:

* **Noobcrew:** Original creator of the definitive *SkyBlock* survival map. This project is dedicated to replicating that authentic, minimalistic isolation and resource management gameplay strategy.
* **Erasmus_Crowley & The Ex Nihilo Team:** Creators of the *Ex Nihilo* mod. This project reimplements their brilliant concept of hammers, sieves, and material cascading loops entirely within an engine wrapper compatible with unmodded vanilla client connections.

---

## 💻 Installation (Server-Side Only)
1. Ensure your dedicated server setup runs the **Fabric Loader** matching the target version block.
2. Drop the `fabric-api-x.x.x+26.1.2.jar` file directly into your server's `mods/` directory. (Required)
3. Drop the `FrogPixelSkyblock.jar` into the mods directory.
4. Drop the `FrogPixelGuard.jar` into the mods directory. (This is an optional but a recommended mod)
5. Boot up your server and connect with any unmodded vanilla Minecraft client!
