# HomeCast

**Version:** 4.7.0
**Description:** A premium, all-in-one local network audiobook, e-book, and music media player built directly into a unified interface with intelligent AI discovery and adaptive glassmorphic UI.

---

## 🚀 Recent Updates (v4.7.0)
- **Dedicated Server Connections Submenu**: Re-architected personal media server settings into a streamlined, dedicated sub-screen supporting Audiobookshelf, Plex, Booklore, Komga, Kavita, and Jellyfin with real-time status diagnostics and synchronization triggers.
- **Public Domain Media Sources Manager**: Added a dedicated sub-menu to change, add, or customize public domain repositories. Users can type or paste any catalog website URL (e.g. Project Gutenberg, LibriVox, Standard Ebooks, Internet Archive). Google Gemini inspects and validates the endpoint, repairs broken protocols or subpaths, detects supported media types (Audiobook, E-Book/Comic, Music), and presents an interactive confirmation dialog with detailed explanations before activating.
- **Local Device Storage Folders Submenu**: Added a dedicated sub-menu to import and manage storage directories for each of the three media types (Audiobooks, E-Books/Comics, Music). Supports both system directory browsing (SAF) and direct storage path entry. Discovered offline files automatically import into the user's Personal Library tabs.
- **AI Local Media Metadata & Biography Enrichment**: Gemini AI scans offline files across local device storage, correcting messy file tags, locating high-resolution public cover art, and finding comprehensive literary/musical biographies.

## 🚀 Previous Updates (v4.6.1)
- **Authentic Comic & Manga Page Engine**: Fully replaced AI summary placeholders with direct graphical page streaming from Komga, Kavita, Archive.org, and local/downloaded CBZ/ZIP archives. Comics now render their genuine original full-bleed visual pages with zoom, pan, and LTR/RTL reading modes.
- **Direct E-Book Text Stream Reader**: E-books now stream genuine unabridged text chapters directly from Booklore, Project Gutenberg, and EPUB files without AI summary fallbacks.
- **Dark Mode Typography Polish**: Upgraded all media titles, author names, and descriptions to ensure crisp contrast and legibility across both dark and light modes.
- **Audiobooks Switcher UI Alignment**: Replicated the polished segmented switcher buttons from Books and Music onto the Audiobooks screen for seamless visual and interaction consistency.
- **Clean Bookshelf Header**: Simplified the e-book header to 'Bookshelf' for a clean, distraction-free reading experience.

## 🚀 Previous Updates (v4.6.0)
- **Unified Switcher UI/UX Across Media Tabs**: Replicated the sleek personal library & public domain switcher button tab bar from Books and Music across the Audiobooks screen for seamless visual and interaction consistency.
- **Relabeled Bookshelf Header**: Renamed "Glass Bookshelf" to "Bookshelf" in the primary e-book reading screen with crisp typography and high contrast.
- **Resilient Cover Art Engine (MediaCoverArt)**: Replaced fragile image loaders across all shelves with high-contrast, gold-embossed fallback covers. Book covers now load reliably and never display empty or broken boxes.
- **Massive Public Domain Catalog Expansion**: Preloaded hundreds of curated public domain books, audiobooks, and music masterworks across Sci-Fi, Cyberpunk, Fantasy, Philosophy, Mystery, and Classics.
- **Instant Zero-Delay Fallbacks for Details & Creators**: Integrated `LocalMediaMetadataProvider` to guarantee instant rendering of media biographies, creator profiles, ratings, and recommendations even when offline or before AI responses arrive.
- **Comprehensive Dark Mode Contrast Fixes**: Fixed all media titles, album names, and author links across all screens to render in high-contrast white and vibrant accents.

## 🚀 Previous Updates (v4.5.3)
- **AI-Powered Local Database Sanitization**: Restructured library synchronization pipelines to execute client-side author and genre sanitizers prior to local SQLite database insertions. This ensures clean, folder-free categorization and catalog author names even if the local network is offline or the Gemini API is unavailable.
- **Contextual Media Details Screen**: Tapping any book, audiobook, or music item now launches a beautiful, information-rich detailed layout. Users can inspect the item's synopsis/biography, publisher details, rating, and launch actions (Reading/Stream Playback).
- **Personal & Public Domain Cross-Recommendations**: Developed smart horizontal carousels that dynamically query, format, and display matching titles by the same creator or items within the same curated category, cross-linking local personal server catalogs with public domain archives.
- **Enhanced Ebook Reader Resolution**: Patched the e-reader's routing and resolution mechanisms for personal e-books synced from Booklore, ensuring instant file rendering.

## 🚀 Previous Updates (v4.5.2)
- **Signature Bypass Settings Migration (Dual-Path)**: Solved the Android package installation/signature collision issue completely! If you are migrating from a previous GitHub version or are forced to do a clean uninstall/reinstall due to conflicting debug keystores, HomeCast now supports an absolute, signature-free, storage-independent backup mechanism.
- **Offline Storage Access Framework (SAF) Export/Import**: Users can now click "Export Backup" inside the settings menu to save their encrypted server configs, passwords, and playback state into a portable `.json` backup file anywhere (local downloads, Google Drive, SD card). Selecting "Import Backup" restores everything instantly in 1-click.
- **Automatic Auto-Backup Detection**: On any modification, connection profiles are auto-saved to public Downloads (/sdcard/Download/homecast_backup.json). On fresh reinstalls, if the database is unconfigured, Settings presents a prominent 1-click prompt to auto-restore all connections immediately.
- **Interactive "What's New" Accordion Log**: Added a premium "What's New" dialog available directly from the settings menu. All release notes and updates starting from v4.3.0 up to the current release (v4.5.2) are loaded into a self-collapsing dropdown accordion list. The dropdown items start fully closed, and clicking on any update automatically closes the previously opened one to maintain clean visual structure.
- **Official Creator Branding & Repository Links**: Enhanced the "About" section in Settings to display the official publisher branding: **Created by FourgeAI LABS**. Clicking on the publisher name automatically links directly to the global publisher GitHub organization profile (`https://github.com/fourgeailabs`), alongside a brand-new official button linking directly to the HomeCast repository (`https://github.com/fourgeailabs/HomeCast`).

## 🚀 Previous Updates (v4.5.1)
- **Automatic Settings Preservation & Cloud Backup**: Explicitly configured native Android Auto Backup and modern Cloud Data Extraction rules (`backup_rules.xml` and `data_extraction_rules.xml`). This guarantees that all configuration files (including EncryptedSharedPreferences for servers and general playback preferences) and Room SQLite database assets are preserved during updates, reinstalls, or device-to-device transfers.
- **Fragile Data Retention Support**: Fully integrated `android:hasFragileUserData="true"`, ensuring that if a user manually uninstalls the app on modern Android versions, they are offered an OS-level checkbox to seamlessly preserve their settings, configurations, and reading history for subsequent reinstalls.

## 🚀 Previous Updates (v4.5.0)
- **Unified Mini-Player and Custom Stop Controls**: Added a direct "Stop" button to the sliding player controls. This halts ExoPlayer playback, collapses the player screen, and dismisses the mini-player completely. The mini-player now incorporates a sleek, non-interactive visual seek bar overlay that utilizes theme-matching gradients to reflect real-time playback progress.
- **Dynamic Startup Navigation & Tab Presets**: The app now dynamically saves and restores the last played media item. On launch, HomeCast loads the exact media section (Library, Music, or Ebooks) and initializes the view's data source filter (Personal Server vs. Public Domain) according to the media's origin.
- **Premium Adaptive Icon Compatibility**: Re-architected launcher icon vector drawables to move the multi-stop gradient (Cyan to Magenta) into a full-bleed background layer. This ensures the launcher icon is 100% compliant with native Android adaptive masking, delivering a perfect crop on circular devices (like Google Pixel) and squircles alike without double-outline clipping or shape clashing.
- **AI-Powered Personal Server Categorization**: Expanded the Gemini-backed automated dynamic categorization and curation engines to process personal server files (Plex music, Audiobookshelf titles, Booklore books), organizing them into gorgeous dynamic shelves alongside public domain media.
- **Keyboard Password Input Auto-Spacing Fix**: Integrated dedicated `KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrect = false)` to prevent mobile keyboards (like Gboard) from inserting automatic spaces when typing special characters in Server Settings password fields.
- **Enhanced E-Book Loading Resilience**: Resolved text parsing and file loading crashes across public domain Project Gutenberg collections, ensuring stable page rendering.
- **Seamless Update Integration**: Incremented the platform build configuration to `versionCode 37` and `versionName "4.5.0"` to eliminate installer downgrade conflicts during uninstalls where users preserve existing app databases and shared preferences.

## 🚀 Previous Updates (v4.3.2)
- **Resolved Audiobook Duration Display**: Solved the pervasive 1-hour default duration display bug on public domain audiobook cards. Fallbacks are now set to `0L` (hiding the duration badge until resolved) and the reactive background metadata worker is throttled using a Coroutine Semaphore (`Semaphore(3)`). This prevents network flooding/rate-limiting and ensures actual audio durations are resolved successfully and displayed beautifully.
- **Fixed Password Input Auto-Spacing**: Discovered that standard input fields without explicit password configurations trigger predictive text, causing mobile keyboards (like Gboard/SwiftKey) to automatically insert spaces when typing special characters (like `.`, `@`, `#`). Adding dedicated `KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrect = false)` to the Server Settings fields disables auto-spacing and predictive suggestions entirely.
- **Embedded Keystore Restoration**: Resolved installation failures that occurred when users uninstalled previous app versions while choosing to "Keep app data" (which retains signature records). By configuring `app/build.gradle.kts` to dynamically decode and restore the identical, secure `debug.keystore` from `debug.keystore.base64` prior to compilation, we guarantee perfectly consistent signing certificates across all environments (browser builds, local development, and GitHub Actions CI/CD workflows).

## 🚀 Previous Updates (v4.3.1)
- **Dual Adaptive Icon Compatibility**: Introduced native adaptive icon layouts with distinct foreground configurations. Devices that prefer round/circular icons (like Google Pixel) load a mathematically perfect circular version of the gradient badge, preventing any forced letterboxing, cutting off of square corners, or double-outline stretching, while maintaining a classic squircle badge on square-preferring launchers.

## 🚀 Previous Updates (v4.3.0)
- **Clickable Artist/Creator Bio Detail Navigation**: Clicking on the artist/author name inside the sliding audio player now transitions the user directly to the Google Gemini-powered Creator Detail screen.
- **Dynamic Public Domain Library Matching in Bio**: The Creator Bio details page now cross-references both personal server files and online public domain records, presenting organized shelves of matching E-books, Audiobooks, and Music tracks that can be read or played directly from their detail profiles.
- **On-Demand Full Public Domain Album Resolution**: Solved the issue where public domain music albums only showed/played a single song. Clicking a public domain album now fetches its entire tracklist from Archive.org's files API, loading all constituent songs with exact titles, correct track numbers, and real-time durations.
- **Reactive Background Audiobook Durations**: Solved the inaccurate duration calculations for public domain audiobooks. On discovery, the app triggers a reactive background worker that queries the Archive.org files catalog, sums all chapter/track durations in seconds, and updates the UI shelf cards instantly.
- **User-Agent Gutenberg Request Headers**: Resolved E-book loading failures by appending a standard browser `User-Agent` header to all Project Gutenberg and Archive.org queries, bypassing security scrapers and filters to ensure texts load successfully.

## 🚀 Previous Updates (v4.2.1)
- **Resolved Audiobook Playback & Stream Resolution**: Solved the issue where public domain audiobooks and music tracks failed to play. The app now queries the Archive.org files metadata API in real-time, dynamically resolving actual `.mp3` and `.txt` file paths rather than relying on standard filename assumptions.
- **Fixed Audiobook Duration Calculations**: Corrected the duration scaling bug by normalizing data units. Seeded audiobooks and dynamic metadata items are now consistently declared in seconds, allowing the media player's formatting engine to calculate and display exact durations perfectly (fixing the 1,000+ hour display bug).
- **Expanded Public Domain Collections & Shelves**: Dramatically enriched the public domain discovery experience. By fetching multiple popular Archive.org collections in parallel (such as 78rpm Golden Era recordings, netlabels electronic/ambient, live concert archives, LibriVox, Gutenberg texts, and Smithsonian catalogs) and automatically mapping them into distinct, beautifully structured genre shelves, the app now provides endless hours of free media.
- **Unified Media Detail Action Pipeline**: Connected the contextual "Read" and "Play" buttons on the Media Detail screen. Clicking these buttons now correctly launches the native E-Reader or sliding ambient music/audiobook player with resolved streaming files directly from any view.
- **Intelligent OCR Fallback E-Reader**: Implemented automatic fallback routing within the custom E-Reader. If a book's primary OCR file (`_djvu.txt`) is missing on Archive.org, the fetcher automatically recovers by requesting the clean `.txt` variation, completely preventing empty page loads.

### 🚀 Previous Updates (v4.1.2)
- **Source-Level Library Separation**: Segregated public domain classics from the "Personal Library" section. The local pre-seeded/downloaded public domain files (like *The Time Machine* or *Dracula*) now appear correctly within the **Public Domain** section alongside fetched online items, keeping the **Personal** section completely clean for the user's synced Audiobookshelf or Plex servers.

### 🚀 Previous Updates (v4.1.1)
- **Universal Public Domain Access**: Decoupled public domain audiobook and music tabs from personal server connection empty states. Users can now immediately explore and play classic public domain audiobooks and ambient tracks without needing to connect an Audiobookshelf or Plex server first.
- **First-Launch Library Auto-Seeding**: Solves empty state issues by preloading highly realistic, slightly messy catalog entities on the very first startup (including classic books, popular audiobooks, and synthwave music tracks). This allows users to experience the AI-powered organizers and cover art locators immediately upon installation.
- **Dynamic Adaptive Genres**: Rewrote music and eBook category lists to be extracted reactively from active database records. When Google Gemini optimizes categories or genres, tabs and shelves update instantly in real-time.
- **Frosted Glass Borders & Outlines**: Enhanced the glassmorphic aesthetics by extending `SurfaceGlassBorder` highlights to the persistent mini-player, music album covers, song list rows, and track shelf cards.
- **Sequential Optimization Pipelines**: Chain-linked category optimizations with the missing cover-art locator, enabling both optimizations to run sequentially during seeding and manual settings refreshes.

### 🚀 Previous Updates (v4.0.0)
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
