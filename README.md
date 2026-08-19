# HomeCast 🎧✨

<p align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" width="100" alt="HomeCast Icon" />
</p>

<p align="center">
  <strong>A unified Android client for Audiobookshelf & Plex with an adaptive glassmorphism aesthetic and Gemini AI media discovery.</strong>
</p>

<p align="center">
  <a href="https://github.com/your-username/HomeCast/actions/workflows/build.yml"><img src="https://img.shields.io/github/actions/workflow/status/your-username/HomeCast/build.yml?branch=main&style=flat-square&logo=githubactions&logoColor=white&label=Build%20APK" alt="Build Status" /></a>
  <img src="https://img.shields.io/badge/Release-v1.9-brightgreen?style=flat-square" alt="Version 1.9" />
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=flat-square&logo=android&logoColor=white" alt="Platform: Android" />
  <img src="https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?style=flat-square&logo=kotlin&logoColor=white" alt="Kotlin" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose%20%7C%20M3-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose" />
  <img src="https://img.shields.io/badge/License-MIT-blue?style=flat-square" alt="License" />
</p>

---

### 🚀 What's New in v1.9
- **🔑 True 4-Character Plex Link Code**:
  - Configured `strong=false` on Plex API to generate standard 4-character linking codes formatted specifically for `plex.tv/link`.
  - Upgraded Link UI with individual digit/character tile displays, 1-tap "Copy Code", and automated browser link generation (`https://plex.tv/link?code=...`).
  - **Zero-Touch Background Token Claiming**: HomeCast now automatically polls and claims the auth token in real-time as soon as the PIN is approved on `plex.tv/link` in your browser, automatically filling the token and dismissing the dialog.
- **🎵 Bulletproof Plex Connection & HTTP 401 Resolution (v1.8 Integration)**:
  - Official Plex PIN sign-in, full standard client header negotiation, URL & port auto-sanitization, permissive SSL fallback, and comprehensive connection diagnostics.
- **🛡️ Bulletproof Audiobookshelf Connection & Diagnostics (v1.7 Integration)**:
  - Permissive SSL engine, deep URL sanitizing, multi-strategy token login, and live diagnostics dialog.
- **📖 Kindle-Grade E-Reader (v1.6 Integration)**:
  - Physics-based realistic page turns with 3D curl simulation.
  - 4 Selectable Font Families (Literata, Bookerly, Inter, JetBrains Mono) with granular sizing and 5 color themes (Warm Sepia, OLED Black, Paper White, Midnight Slate, Solar Mint).
- **💥 Dynamic Comic Reader with Guided Frame Zoom**:
  - Guided panel zoom with tap-right next frame, tap-left previous frame, double-tap zoom toggle, and Manga RTL mode.
- **👇 Drag & Slide-Down Player Gesture**:
  - Vertical drag-to-dismiss gesture on the full player screen to collapse smoothly into the mini-player.
- **🎯 Active Server-Grounded AI Discovery**:
  - Strict grounding on connected server inventory for personalized media discovery.
- **Unified Discovery Shelf-Like Architecture**:
  - **Multi-Media Horizontal Carousels**: Refactored the entire Discovery experience into fluid horizontal sliding shelves matching the Library and Music interfaces.
  - **Complete Media Triad**: Full discovery support across **Audiobooks**, **Music**, and **Books (E-Reader)**.
  - **Vertical Categorized Shelves**:
    - **Audiobooks**: "Trending Audiobooks" (bestsellers, top narrators), "Sci-Fi & Cyberpunk Sagas" (epic worldbuilding, space operas), "Mindset & Growth", and "Psychological Thrillers".
    - **Music**: "Featured Music Albums", "Acoustic & Lo-Fi Chill" (focus loops, rainy vibes, coffee shop melodies), "High-Voltage Rock & Synthwaves", and "Cinematic Orchestras & Scores".
    - **Books (E-Reader Ready)**: "Bestselling E-Books & Novels", "Classic Literature", and "Speculative Fiction".
- **Interactive E-Reader Engine & Preview**:
  - Prepared the app for the upcoming full E-Reader aspect with an interactive **E-Reader Reading Canvas**.
  - Includes typography customization (font size adjustments A- / A+), multiple reading themes (**Sepia**, **Night Dark**, **Clean Light**), formatted chapter excerpts, page counters, EPUB/PDF format badges, and one-tap "Bookmark to Library".
- **Real-Time Gemini AI Generative Discovery**:
  - Filter chips for fast context switching (`All Media`, `Audiobooks`, `Music`, `Books (E-Reader)`, `Nearby Culture`).
  - Generative AI prompt bar for requesting customized recommendations across narrators, soundscapes, or book authors with direct playback and excerpt preview actions.
- **Fluid Playback Integration**:
  - Selecting any discovered audiobook or music track immediately launches audio playback and slides the full media player up into view.

### 🚀 What's New in v1.4
- **Intuitive Shelf-Like UI Architecture**:
  - **Horizontal Sliding Shelves**: Audiobooks and music are now organized into horizontal sliding carousels ("slide to the side") for quick visual discovery.
  - **Vertical Section Hierarchy**: Scroll up and down through curated categories including **"Continue Listening" / "Recent Grooves"**, **"New Releases & Additions"**, **"Popular & Regional Hits"**, **"Noteworthy Masterpieces"**, **"Series & Sagas"**, **"Curated Mixes"**, and **"Favorites"**.
- **Hierarchical Music File Structure (Genre -> Artist -> Album -> Songs)**:
  - Keep the persistent top search bar while unlocking a structured media file hierarchy.
  - **Genre Exploration**: Interactive visual grid featuring dedicated artwork, vibrant gradient cards, and genre metadata (Rock & Alternative, Electronic, Pop, Hip Hop, Jazz, Classical, Acoustic, Ambient).
  - **Artist Discographies**: Artist avatar hubs with album counts and direct drilldowns.
  - **Album Song Hub with Centered Artwork**: Clicking any album reveals the dedicated song list featuring a **large square album cover with rounded corners at the top center**, full artist credits, track durations, and one-tap "Play Album" / "Shuffle" actions.
- **Fluid Slide-Up Media Player**:
  - Tapping any audiobook or song immediately starts audio playback and smoothly slides the full media player up into view.
  - Includes a top collapse bar/chevron to slide the player down without interrupting audio, keeping a persistent mini-player pinned above the navigation bar for continuous browsing.
- **Database & Model Upgrade**: Bumped Room database to version 3 with indexed genre and track sequence mappings for Plex music collections.

### 🚀 What's New in v1.3
- **Audiobookshelf 401 & Authentication Fixes**:
  - Replaced manual JSON string construction with Moshi serialization so special characters in passwords (`"`, `\`, symbols) are never corrupted.
  - Added dual endpoint fallback (`/login` and `/api/login`) to support reverse proxy and custom path setups.
  - Automatic whitespace trimming on usernames, URLs, and tokens to prevent mobile keyboard input errors.
  - Added password visibility toggle (show/hide eye icon) to verify password entries before connecting.
  - Added an inline Help & Troubleshooting guide explaining how to retrieve and use an API Key/Token directly if username/password auth is disabled or uses SSO/OIDC.
  - Improved error diagnostics with detailed server-reported reason messages.

### 🚀 What's New in v1.2
- **Refreshed Custom Adaptive App Icon**: Brand-new custom vector adaptive launcher icon featuring a gradient badge (vibrant cyan, electric indigo, violet), headphones arch, open audiobook pages, cast waves, and glowing play beacon on a midnight gradient canvas with ambient lighting.
- **Visual Polish**: Upgraded Material You adaptive icon layers and dark/light system icon integration.

### 🚀 What's New in v1.1
- **Working Server Connections**: Functional **Audiobookshelf** and **Plex** server configuration with real test connection, token/credential validation, and one-tap database sync.
- **Plex Music Integration**: Added real Plex server connection and live music streaming (`X-Plex-Token` / direct URL).
- **Real Local Room Database**: Replaced placeholder data with live Room database caching for audiobooks, music tracks, and server configurations.
- **Gemini 2.5 AI Discovery**: Enhanced AI media discovery with quick category chips, custom search prompt queries, and optional location-aware recommendations.
- **Persistent Mini-Player**: Bottom mini-player bar for background playback and instant navigation between views.

---

## 📖 Overview

**HomeCast** brings together your self-hosted media into a single app:
- **Audiobookshelf** for listening to audiobooks with chapter progress and resume support.
- **Plex** for streaming your personal music library, artists, albums, and playlists.
- **Adaptive Glassmorphic Player** that extracts dominant color palettes dynamically from album art and covers to tint background blurs and interactive controls.
- **Contextual AI Discovery** powered by Google Gemini to suggest what to listen to next based on context and preferences.

---

## ✨ Features

- **📚 Audiobookshelf Integration**: Browse your audiobook collection, track listening progress, and pick up right where you left off.
- **🎵 Plex Music Client**: Stream tracks, navigate albums and artists with fast, cached image loading.
- **🎨 Adaptive Glassmorphism UI**:
  - Dynamic palette extraction (`androidx.palette`) from cover art to create matching ambient background glows.
  - Interactive scrubbable progress bar with instant seek dispatch.
  - Native support for both **Dark** and **Light** themes.
- **🤖 Smart AI Discovery**: Ask Gemini for personalized listening recommendations matching your mood, time of day, or activity.
- **🛠️ Custom Server Management**: Configure both local (LAN) and remote (WAN) endpoints with custom server names for your Audiobookshelf and Plex instances.
- **⚡ Automated APK CI/CD**: Pre-configured GitHub Actions automatically compiles and provides direct `.apk` downloads on every push.

---

## 📲 Download & Installation

### Option 1: Download from GitHub Actions (Recommended)
1. Go to the **[Actions](../../actions/workflows/build.yml)** tab in this repository.
2. Click on the latest workflow run on the `main` branch.
3. Scroll down to the **Artifacts** section and download **`HomeCast-debug`**.
4. Unzip and install the APK on your Android device.

### Option 2: Build with Android Studio
1. Clone this repository:
   ```bash
   git clone https://github.com/your-username/HomeCast.git
   ```
2. Open the project in **Android Studio**.
3. Let Gradle sync and click **Run ▶** to install on your connected device or emulator.

---

## 🛠️ Tech Stack

| Component | Library / Framework |
| :--- | :--- |
| **Language** | Kotlin 2.2 |
| **UI Framework** | Jetpack Compose (Material 3) |
| **Color Extraction** | AndroidX Palette KTX |
| **Image Loading** | Coil Compose |
| **Networking** | Retrofit 2 + OkHttp 4 + Moshi |
| **AI Recommendations** | Google Gemini API (REST / Firebase AI) |
| **Navigation** | Navigation Compose |
| **CI / Build Pipeline** | GitHub Actions + Gradle 9.3 |

---

## ⚙️ Configuration

1. Launch **HomeCast** on your Android device.
2. Navigate to the **Settings** tab:
   - **Audiobookshelf**: Enter your custom server name, host address (e.g., `http://192.168.1.100:13378`), and your API token.
   - **Plex**: Enter your custom server name, Plex URL, and `X-Plex-Token`.
   - **Theme**: Switch between Dark and Light mode.

---

## 🤝 Contributing

Contributions, issues, and feature requests are welcome! Feel free to check the [issues page](../../issues).

---

## 📄 License

Distributed under the MIT License. See `LICENSE` for more information.
