# HomeCast

**Version:** 4.0.0
**Description:** A premium, all-in-one local network audiobook, e-book, and music media player built directly into a unified interface with intelligent AI discovery and adaptive glassmorphic UI.

---

## 🚀 Recent Updates (v4.0.0)
- **AI-Powered Cover Art Locator**: Built a robust system utilizing Google Gemini that scans your media files (audiobooks, music tracks, e-books) for missing or placeholder artwork and contextually sources high-quality, beautiful thematic image covers from public curators.
- **Daily Dynamic Menu & Category Curation**: Engineered an automated scheduling system. On launch or daily date changes, the app utilizes Gemini to generate fresh, unique daily thematic category sets and automatically groups your media assets contextually based on their titles and contents (avoiding unhelpful or directory-like names).
- **Intelligent Author & Artist Cleaning**: Integrated AI-powered catalog normalization that processes messy listing structures (e.g. `Smith, Jeff, 1960 Feb...` or `Wells, H. G. (Herbert George), 1866-1946`) into clean, reader-friendly, beautiful human-readable names.
- **Cohesive Frosted Glass UI Overhaul**: Elevated the application's visual presence by applying frosted glass translucency styling (`SurfaceGlass` / `SurfaceGlassBorder` with dark slate alpha overlays) across all bottom navigation controls, lists, and main layers.
- **AI Magic Optimizer Controls**: Added a dedicated control section in Settings to trigger live optimization cycles for authors and cover locator routines with interactive loading indicators.

### 🚀 Previous Updates (v3.4.0)
- **AI-Powered E-Book Genre Categorization**: Added an automated categorization system to the E-Books interface. Clicking the ✨ button in the E-Books screen header sends a batch of books/comics metadata (IDs, titles, and authors) to Google Gemini. Gemini classifies each book into a single high-level precise genre (e.g., `Sci-Fi`, `Fantasy`, `Manga`, `Non-Fiction`), which is then saved to the local Room database, immediately refreshing your dynamic genre tabs!
- **Fixed Media3 & Playback Formats**: Resolved formatting mismatches for audiobook durations by using reliable formatting helpers.
- **Fixed WhiteVariant Color Reference**: Fixed compilation issues with `WhiteVariant` color references in the Music Player controls, replacing them with standard Material theme and custom translucent values.

### 🚀 Previous Updates (v2.6)
- **Booklore API Authentication Fix**: Fixed the 403 Forbidden error when connecting to Booklore. The app now correctly executes a pre-flight authentication sequence against Booklore's `/api/v1/auth/login` endpoint to exchange credentials for a secure JWT Bearer token before requesting your library data.

### 🚀 What's New in v2.5
- **Booklore Data Parsing Fix**: Re-architected the E-Book data parser to natively support the Komga/Booklore paginated `content` structures. Books are now properly decoded with their correct titles, authors, cover thumbnails, and total page counts.
- **Dynamic Personal Shelves Fix**: Rebuilt the Personal Library shelf system. The engine now dynamically groups and displays all synced ebooks by their actual native genre, including a fallback for uncategorized media.

*(See commit history for older v1.x feature sets).*

---

## 📖 Overview

**HomeCast** brings together your self-hosted media into a single app:
- **Audiobookshelf** for listening to audiobooks with chapter progress and resume support.
- **Plex** for streaming your personal music library, artists, albums, and playlists.
- **Booklore / E-Books** for fetching and reading personal EPUB/TXT collections or Project Gutenberg classics natively.
- **Adaptive Glassmorphic Player** that extracts dominant color palettes dynamically from album art and covers to tint background blurs and interactive controls.
- **Contextual AI Discovery** powered by Google Gemini to suggest what to listen to next based on context and preferences.

---

## ✨ Features
- **📚 Audiobookshelf Integration**: Browse your audiobook collection, track listening progress, and pick up right where you left off.
- **🎵 Plex Music Client**: Stream tracks, navigate albums and artists with fast, cached image loading.
- **📖 Native E-Reader**: Real paginated layout with physical page-turn animations, theme options, and adjustable fonts.
- **🎨 Adaptive Glassmorphism UI**: Dynamic palette extraction (`androidx.palette`) from cover art to create matching ambient background glows.
- **🤖 Smart AI Discovery & Details**: Ask Gemini for personalized listening recommendations or tap an author/artist to instantly generate a full biography and detail sheet.
- **🛠️ Custom Server Management**: Configure both local (LAN) and remote (WAN) endpoints for Audiobookshelf, Plex, and Booklore instances.
- **⚡ Automated APK CI/CD**: Pre-configured GitHub Actions automatically compiles and provides direct `.apk` downloads on every push.

---

## 📲 Download & Installation

### Option 1: Download from GitHub Actions (Recommended)
1. Go to the **Actions** tab in this repository.
2. Click on the latest workflow run on the `main` branch.
3. Scroll down to the **Artifacts** section and download **`HomeCast-debug`**.
4. Unzip and install the APK on your Android device.

*Note: If you have an older version of the app installed from a different signing key or run, you may need to completely uninstall the old version from your Android Settings before installing the new APK.*

### Option 2: Build with Android Studio
1. Clone this repository.
2. Open the project in **Android Studio**.
3. Let Gradle sync and click **Run ▶** to install on your connected device or emulator.

---

## 🛠️ Tech Stack
| Component | Library / Framework |
| :--- | :--- |
| **Language** | Kotlin |
| **UI Framework** | Jetpack Compose (Material 3) |
| **Color Extraction** | AndroidX Palette KTX |
| **Audio Engine** | Media3 ExoPlayer & MediaSession |
| **Image Loading** | Coil Compose |
| **Networking** | Retrofit 2 + OkHttp 4 + Moshi |
| **AI Recommendations** | Google Gemini API (REST) |

---

## ⚙️ Configuration
1. Launch **HomeCast** on your Android device.
2. Navigate to the **Settings** tab to enter your host addresses and API tokens for Audiobookshelf, Plex, and Booklore.
