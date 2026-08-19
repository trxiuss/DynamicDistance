# **⚡ DynamicDistance**
The **next-generation, proactive View & Simulation Distance engine** for Minecraft servers. DynamicDistance continuously reads your server's real vitals — TPS, MSPT, CPU, RAM, loaded chunks, and entity count — and reacts *before* lag ever hits your players. No harsh on/off thresholds, no jarring distance jumps: just a smooth, mathematically smoothed curve that keeps your world looking great while your hardware breathes.

> Stop choosing between a beautiful server and a stable one. DynamicDistance gives you both, automatically.

## 🌐 Multi-Language Support
DynamicDistance ships with **28 professionally structured languages** out of the box, so every admin and every community feels right at home:

**🇺🇸 English (en) | 🇹🇷 Turkish (tr) | 🇩🇪 German (de) | 🇪🇸 Spanish (es) | 🇷🇺 Russian (ru) | 🇨🇳 Chinese (zh) | 🇯🇵 Japanese (ja) | 🇦🇿 Azerbaijani (az) | 🇫🇷 French (fr) | 🇸🇦 Arabic (ar) | 🇳🇱 Dutch (nl) | 🇮🇩 Indonesian (id) | 🇦🇲 Armenian (hy) | 🇮🇹 Italian (it) | 🏴󠁧󠁢󠁳󠁣󠁴󠁿 Scottish Gaelic (gd) | 🇸🇪 Swedish (sv) | 🇰🇬 Kyrgyz (ky) | 🇰🇷 Korean (ko) | 🇭🇺 Hungarian (hu) | 🇨🇿 Czech (cs) | 🇬🇷 Greek (el) | 🇮🇷 Persian (fa) | 🇵🇱 Polish (pl) | 🇷🇴 Romanian (ro) | 🇻🇳 Vietnamese (vi) | 🇵🇹 Portuguese (pt) | 🇹🇭 Thai (th) | 🇺🇦 Ukrainian (uk)**

Switch languages instantly with the `language` field in `config.yml` — every translation file is auto-generated into `/plugins/DynamicDistance/langs`.

## 🚀 Why DynamicDistance?

- ⚙️ **Predictive, not reactive.** Most plugins react to lag after it happens. DynamicDistance's smoothed load score anticipates degradation and scales down gracefully, then eases back up — never a visible "pop."
- 🧠 **Actually intelligent math**, not a simple if/else. A weighted multi-metric stress score, dual-speed smoothing, hysteresis cooldowns, and a smoothstep quality curve — engineered like a real performance system, not a toggle.
- 🧵 **Fully async and Folia-ready.** Zero main-thread blocking, zero TPS overhead from the plugin itself.
- 🎯 **Granular by design.** Per-world limits, per-player application, per-rank bonuses, per-player timed overrides — total control down to the individual player.

## 🔥 Core Features

### 🧠 Adaptive Engine — The Brain
- Combines **MSPT, TPS, CPU load, RAM usage, loaded chunk count, and entity count** into a single weighted **Stress Score (0–100)**.
- **Dual-speed smoothing**: reacts *fast* when performance degrades, recovers *slowly and steadily* to avoid flickering distances during unstable ticks.
- A **smoothstep-based Quality Factor** curve translates the stress score into distance values with a natural, non-linear easing — no sudden jumps.
- **Recovery Cooldown (hysteresis protection)**: after a critical spike, distances are held back and ramp up gradually instead of snapping straight back to max.
- Fully configurable target/critical thresholds for MSPT, TPS, CPU, RAM, chunks, and entities.

### 📏 True Dual-Distance Control
- Manages **View Distance** and **Simulation Distance** independently and simultaneously.
- **Per-world overrides** — give the Nether or The End tighter limits than your Overworld without touching global settings.
- **Per-player real-time application** — every player gets the distance that matches *their* exact situation, not a single server-wide value.

### 🧩 Universal Platform Architecture
- **Folia Ready** — native region-threaded adapter, built for the future of Minecraft server performance.
- **Modern Paper (1.18+)** — automatically detects and uses native Simulation Distance APIs.
- **Legacy Spigot / Bukkit** — gracefully falls back to View Distance–only optimization with zero errors or missing features.
- The correct adapter is **auto-detected on startup** — no manual configuration, no guesswork.

### 😴 AFK Limiter
- Automatically detects inactive players (movement, chat, commands, interactions) and shrinks their distance to save resources they're not even using.
- Instantly restores their normal distance the moment they become active again.

### 💨 Speed Limiter
- Detects high-speed movement (Elytra flight, boosted travel, etc.) via real-time blocks/second tracking.
- Temporarily trims Simulation Distance for fast-moving players to prevent chunk-loading spikes — completely invisible to the player.

### 👑 Rank & Permission-Based Bonus System
- Full **LuckPerms** integration, plus standard `group.<name>` permission support — use whichever your server already runs.
- Every rank can have its own **bonus View/Simulation Distance**, priority weighting, and independent **bypass flags** for Dynamic scaling, AFK limiting, and Speed limiting.
- Ships with ready-to-use `default`, `vip`, `vip_plus`, `mvp`, and `admin` templates you can rename and tune freely.

### 🛠️ Admin Override System
- `/dd setoverride <player|all> <view> [sim] [duration]` — instantly pin a **permanent or timed** custom distance for one player or your **entire server**.
- Supports intuitive durations like `30s`, `10m`, `2h`, `1d`.
- Expired overrides are **cleaned up automatically** and reapplied in real time — no manual cleanup, ever.

### 📊 Live Performance HUD (BossBar)
- `/dd statusbar` toggles a sleek, **color-coded real-time BossBar** showing TPS, MSPT, CPU, RAM, View Distance, and Simulation Distance at a glance.
- Bar color shifts automatically between green, yellow, and red based on live server health.

### 🧩 PlaceholderAPI Integration
Bring live server performance data straight into your Scoreboard, Tab list, or chat:
`%dynamicdistance_tps%` · `%dynamicdistance_mspt%` · `%dynamicdistance_cpu%` · `%dynamicdistance_ram%` · `%dynamicdistance_ram_mb%` · `%dynamicdistance_chunks%` · `%dynamicdistance_entities%` · `%dynamicdistance_load_score%` · `%dynamicdistance_state%` · `%dynamicdistance_view_distance%` · `%dynamicdistance_sim_distance%` · `%dynamicdistance_target_view%` · `%dynamicdistance_target_sim%` · `%dynamicdistance_reason%`

### 📋 Deep Diagnostics Commands
- `/dd status` — a full live system report: platform, TPS/MSPT, CPU (with core count), RAM (used/max in MB), world chunk & entity load, current View/Simulation distances, and the live stress score with health state.
- `/dd info <player>` — inspect exactly what a specific player is getting and **why**: active View/Simulation distance, AFK status, rank group, override status, and the precise reason it's being applied (dynamic scaling, group bonus, AFK restriction, speed restriction, admin bypass, or override).

### 🏢 Built Like Enterprise Software
- **Self-healing configuration** — on every startup and reload, invalid or conflicting config ranges are automatically detected and corrected, with clear console warnings.
- **Built-in Modrinth update checker** — automatically notifies admins in-game the moment a new version is available.
- Lightweight **JSON-based storage** with periodic auto-save for custom player overrides.
- Optional **bStats metrics** for transparent, anonymous usage insights.

## 🛠️ Configuration
`config.yml` puts every layer of the engine in your hands:

### ⚙️ Engine
- **check_interval** — how often (in seconds) the engine evaluates performance.
- **smoothing_factor_degrade / recover** — how aggressively distances shrink under load vs. how gently they recover.
- **recovery_cooldown** — hysteresis delay before distances are allowed to climb back up after a critical event.
- **permission_cache_seconds** — how long rank/group lookups are cached for performance.

### 📏 Distance Limits
- Global **min / max / default_start** values for both View and Simulation Distance.
- **worlds:** section for world-specific min/max overrides (e.g. tighter limits for the Nether and The End).

### 📊 Thresholds
- Target & critical **MSPT** and **TPS** values.
- Maximum **RAM** and **CPU** usage percentages.
- Target **chunk** and **entity** counts.
- Optimal and critical **stress score** boundaries.

### 🧩 Modules & Groups
- Every parameter of the **AFK Limiter** and **Speed Limiter** modules is independently configurable.
- The **groups:** section defines priority, bonus distances, and bypass behavior per rank.

## ⚡ Commands

| Command | Permission | Description | Aliases |
|---------|------------|-------------|---------------|
| `/dynamicdistance status` | dynamicdistance.admin | Displays a detailed live performance report | `/dd status` |
| `/dynamicdistance statusbar [player]` | dynamicdistance.admin | Toggles the real-time HUD BossBar | `/dd statusbar`, `/dd bar`, `/dd hud` |
| `/dynamicdistance reload` | dynamicdistance.admin | Reloads config and language files | `/dd reload` |
| `/dynamicdistance setoverride <player\|all> <view> [sim] [duration]` | dynamicdistance.admin | Sets a permanent or timed custom distance limit | `/dd setoverride` |
| `/dynamicdistance clearoverride <player\|all>` | dynamicdistance.admin | Removes a custom distance limit | `/dd clearoverride` |
| `/dynamicdistance info <player>` | dynamicdistance.admin | Inspects a player's live profile and applied-distance reason | `/dd info` |

> Base command aliases: `/dd`, `/dynview`, `/vdist`

## 🛡️ Permissions

| Permission | Description |
|------------------------------|------------------------------------------------------|
| `dynamicdistance.admin` | Grants access to all administrative commands (default: OP). |
| `dynamicdistance.bypass` | Exempts a player from all dynamic distance adjustments. |

## ⚙️ Supported Server Software

| Core | Support Status | Simulation Distance |
|--------------|----------------|----------------|
| ✅ Folia | Fully Supported | ✅ |
| ✅ Paper (1.18+) | Fully Supported | ✅ |
| ✅ Purpur | Fully Supported | ✅ |
| ✅ Spigot | Fully Supported | View Distance only |
| ✅ Bukkit | Fully Supported | View Distance only |

- The correct engine adapter (**Folia / Modern Paper / Legacy**) is detected automatically the moment the plugin loads — deploy it anywhere and it just works.

## 🧩 Placeholders
Requires **PlaceholderAPI** to be installed.
- `%dynamicdistance_tps%` / `%dynamicdistance_mspt%` — live tick health.
- `%dynamicdistance_cpu%` / `%dynamicdistance_ram%` / `%dynamicdistance_ram_mb%` — hardware usage.
- `%dynamicdistance_chunks%` / `%dynamicdistance_entities%` — world load counters.
- `%dynamicdistance_load%` / `%dynamicdistance_load_score%` / `%dynamicdistance_state%` — stress score & health state.
- `%dynamicdistance_view_distance%` / `%dynamicdistance_sim_distance%` — currently applied distances.
- `%dynamicdistance_target_view%` / `%dynamicdistance_target_sim%` — the engine's current global targets.
- `%dynamicdistance_reason%` — why the current distance is being applied.

## ⚖️ License
Released under **CC BY-NC-SA 4.0** (Attribution-NonCommercial-ShareAlike 4.0 International).
Commercial use and resale are strictly prohibited.

## Statistics

[![bStats](https://bstats.org/signatures/bukkit/Dynamic%20Distance.svg)](https://bstats.org/plugin/bukkit/Dynamic%20Diatance)

## Discord

[![Discord](https://cdn.modrinth.com/data/cached_images/4de86371cc7bcf3818924b198f31baacc304700f.png)
](https://discord.gg/62TzJBpm6C)
