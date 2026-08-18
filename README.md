# HomeCast 🎧✨

<p align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" width="100" alt="HomeCast Icon" />
</p>

<p align="center">
  <strong>A unified Android client for Audiobookshelf & Plex with an adaptive glassmorphism aesthetic and Gemini AI media discovery.</strong>
</p>

<p align="center">
  <a href="https://github.com/your-username/HomeCast/actions/workflows/build.yml"><img src="https://img.shields.io/github/actions/workflow/status/your-username/HomeCast/build.yml?branch=main&style=flat-square&logo=githubactions&logoColor=white&label=Build%20APK" alt="Build Status" /></a>
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=flat-square&logo=android&logoColor=white" alt="Platform: Android" />
  <img src="https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?style=flat-square&logo=kotlin&logoColor=white" alt="Kotlin" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose%20%7C%20M3-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose" />
  <img src="https://img.shields.io/badge/License-MIT-blue?style=flat-square" alt="License" />
</p>

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
