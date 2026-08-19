# HomeCast

**Version:** 2.4
**Description:** A premium, all-in-one local network audiobook, e-book, and music media player built directly into a unified interface with intelligent AI discovery and adaptive glassmorphic UI.

---

## 🚀 Recent Updates (v2.4)
- **Booklore API Authentication Fix**: Corrected the network client for Booklore instances. The E-Book synchronization engine now natively uses standard OPDS/Basic Authentication endpoints rather than generic Bearer tokens. It will correctly scan OPDS catalogs, `/api/v1/library/books`, and various standard ports (like 6060) to parse your epub/manga collection successfully.
- **Dynamic Personal Shelves Fix**: Rebuilt the Personal Library shelf system. Instead of arbitrarily hiding books that don't match specific static genres (like "Sci-Fi"), the engine now dynamically groups and displays all synced ebooks by their actual native genre, including a fallback for uncategorized media.

### 🚀 What's New in v2.3
- **Background Playback & Lock Screen Controls**: Upgraded the internal audio engine to `androidx.media3` ExoPlayer and `MediaSessionService`. Audiobooks and music tracks now continue playing flawlessly in the background. A media notification is pinned to your lock screen and notification panel with Play/Pause, Skip Forward, and Skip Backward controls.
- **Real Public Domain Content (Project Gutenberg)**: Simulated texts have been replaced! The E-Reader now dynamically fetches real, high-quality public domain books directly from Project Gutenberg (e.g., *The Great Gatsby*, *The Time Machine*) and parses them into chronological chapters for a full reading experience.
- **Dynamic Media & Creator Details**: Clicking on an author, artist, album, or book cover throughout the app now instantly generates a dynamic details page. Gemini synthesizes biographies, synopses, ratings, Wikipedia links, and publishers in real-time.
- **Strict Library Isolation**: E-Books and Audiobooks are strictly isolated by source tabs. Public domain items no longer bleed into your Personal Library sections, maintaining an organized collection.

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
