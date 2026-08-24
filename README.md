# HomeCast

**Version:** 5.14.00
**Description:** A premium, all-in-one local network audiobook, e-book, music, comic, podcast, and video media player built directly into a unified interface with intelligent AI discovery and adaptive glassmorphic UI.

---

## 🚀 Recent Updates (v5.14.00)
- **Enhanced Plex Authentication & Dual Sign-In Options**:
  - **1-Tap Browser Auth & Direct Link**: Integrated official 1-tap browser auth link (`app.plex.tv/auth#...`) allowing instant authorization via Google, Apple, or Plex web login with auto-claimed PIN codes.
  - **Direct Plex Credentials Sign-In**: Added dedicated tab for direct Plex Username/Email & Password sign-in, obtaining `authToken` directly without leaving the app.
  - **Fail-Proof Dual JSON Token Parsing**: Added fallback snake_case `auth_token` and `authToken` parsing to guarantee zero-stall background polling when linking Plex accounts.

## 🚀 Previous Updates (v5.13.00)
- **Full Plex Server Video Library Support (Movies, Shows, Music)**:
  - Added full support for personal Plex Movies (type 1) and TV Show episodes (type 4) alongside music tracks (type 10).
  - Built dedicated `Movies & Shows` media view tab featuring poster artwork, year/season metadata, and 1-tap playback in embedded HD `VideoPlayerDialog`.
- **Remote Access Plex Pass Disclaimer**:
  - Added explicit notice in the About section of Settings stating that a Plex Pass subscription is required for remote access outside your home network.
- **Concurrent Server Discovery & Parallel URL Probing**:
  - Accelerated Plex server discovery and authentication using concurrent multi-URL probing, avoiding hangs on unreachable candidate URLs.

## 🚀 Previous Updates (v5.12.00)
- **Resolved Audio & Video Media Playback Stream Engine**:
  - Fixed ExoPlayer network request headers by removing conflicting authentication interceptors that triggered `400 Bad Request` or `403 Forbidden` errors on signed URLs (Google Cloud Storage, Archive.org, LibriVox, RSS feeds, and CDN endpoints).
  - All public domain audiobooks, music tracks, podcasts, video podcasts, and self-hosted server streams play reliably and instantly.
- **Strict Single-Server Labeling (Plex or Jellyfin)**:
  - Dynamically evaluates connected servers and strictly displays EITHER `Plex Library` or `Jellyfin Library` (never joined with slashes) based on connected server instances.
- **Clean Space-Constrained Bottom Navigation Labeling**:
  - Updated navigation bar label to **Media** — a concise, clean 5-letter label that fits within screen constraints without wrapping or truncation.

## 🚀 Previous Updates (v5.11.00)
- **High-Performance Embedded Video Player for Podcasts & Video Media**:
  - Integrated Media3 `PlayerView` and `ExoPlayer` powered by `OptimizedNetworkEngine` for high-throughput video streaming.
  - Features aspect-ratio switching (Fit, Zoom, Fill), timeline scrubbing slider with exact timestamps, fast-forward/rewind 10s controls, auto-hiding HUD controls, and backdrop blur dismissals.
  - Automatically identifies video podcast feeds and episode media types (`.mp4`, `.m4v`, `.webm`, `.mov`), displaying high-visibility `[VIDEO]` badges and instant video playback.
- **Dynamic Plex & Jellyfin Server Tab Labeling**:
  - Automatically detects connected personal servers (Plex, Jellyfin, Audiobookshelf) and dynamically updates section labels (e.g. `Plex Library`, `Jellyfin Library`, `Plex / Jellyfin Library`).
  - Renamed music navigation section to **Music & Video** to prepare for unified media, movies, and TV show collections from self-hosted servers.
- **AI Capabilities & Notice Accordion Menu in About**:
  - Full-featured dropdown menu inside the About section of Settings detailing all 9 intelligent AI features built by FourgeAI LABS.
  - Every update notice and AI capability starts closed and expands on user interaction, automatically closing any previously opened card.
- **High-Speed Network & SSL Engine Optimization**:
  - Centralized network clients across `PlexClient`, `AudiobookshelfClient`, `ArchiveOrgClient`, `BookloreClient`, and `ComicContentFetcher` under `OptimizedNetworkEngine`.
  - Configured 32-connection HTTP/2 pooling, 120MB disk caching, and permissive SSL handling for low-latency streaming from local self-hosted instances.

## 🚀 Previous Updates (v5.10.00)
- **Expanded Public Domain & Global Podcast Engine**:
  - **30+ Curated Public Domain Audio Series**: Added a comprehensive curated catalog featuring classic Old Time Radio dramas (*The Shadow*, *Dragnet*, *Sherlock Holmes*, *X Minus One*), science & space exploration (*NASA Science Casts*, *Astronomy Cast*), history & culture (*In Our Time*, *Echoes of History*), philosophy (*Philosophize This!*, *LibriVox Serials*), news, and independent creator feeds (*PlayPodcast*, *RSS.com*, *GetPodcast*).
  - **Live iTunes Podcast Search API**: Enabled real-time search and directory browsing across thousands of global public podcasts directly from the Podcasts screen.
  - **Live RSS & iTunes Episode Extractor**: Automatically parses XML feeds and iTunes metadata to fetch live episode audio streams, full descriptions, publication dates, and artwork.
  - **Interactive Category Filtering**: Fast navigation across 8 categories (Old Time Radio, Science & Tech, History & Culture, Philosophy & Books, News & Ideas, Audio Serials, Indie & Community).
  - **Personal Podcast Subscriptions**: Easily bookmark and save public channels into your Personal Podcasts collection.

## 🚀 Previous Updates (v5.09.00)
- **AI Capabilities & Features Dropdown Notice**:
  - Added an interactive accordion dialog in the About section of Settings detailing all 9 intelligent AI models and smart features built into HomeCast by FourgeAI LABS.
  - Features expandable cards with category tags, detailed descriptions, and feature bullet points that start closed and expand on tap.
- **Restored Comic Archive & Page Streaming Engine**:
  - Resolved comic file and identifier loading issues across Archive.org public domain comics, CBZ/ZIP local archives, and self-hosted servers (Komga/Kavita/Booklore).
  - Enhanced `ComicContentFetcher` with multi-tier extraction: checks for direct archive files, parses page metadata, falls back to Archive.org page stream rendering (`page/n$i.jpg`), and processes local CBZ/ZIP directories.
  - Injected custom `User-Agent` headers into Coil's `ImageRequest` builders to ensure remote comic pages and Archive.org images load reliably without server blockages.
  - Delivered seamless Western LTR, Manga RTL, and Webtoon vertical continuous scroll reading modes with multi-touch zoom and guided panel transitions.

## 🚀 Previous Updates (v5.08.00)
- **Comprehensive AI Media Intelligence Suite**:
  - **Story So Far AI Summarizer**: Provides instant chapter-level context recaps for long audiobooks, e-books, and podcasts without spoilers.
  - **24/7 AI Companion Assistant**: In-context conversational assistant on player and e-reader screens that answers questions tailored strictly to your current listening or reading position.
  - **AI Media Concierge**: Curates custom media blends and delivers narrative recommendations in the Discovery feed.
  - **Smart Sleep Timer & Sleep Assistant**: Intelligent sleep timer with custom audio fade-out and AI-generated bedtime prompts.
  - **Dynamic Ambient Soundscape Synthesizer**: Generates real-time ambient background audio (Rainfall, Fireplace, Ocean Waves, Cafe Ambient, Forest Birds, Cosmic Drone) synthesized directly on-device using PCM AudioTracks for immersive reading or listening.
  - **Stylized Quote Card Generator**: Transforms book excerpts and bookmarks into beautifully designed quote cards with customizable color palettes, typography, and background patterns.

## 🚀 Previous Updates (v5.07.00)
- **High-Performance Responsiveness & Zero App Lag**:
  - Eliminated app sluggishness on launch by throttling progress state polling to 1000ms, checking position delta before StateFlow emission, and enforcing daily checks on AI menu/category cleanup instead of blocking startup loops.
- **Restored Music & Podcast Audio Streaming**:
  - Fixed stream resolution logic for public domain and server audio items.
  - Implemented an ExoPlayer initialization action queue in `PlaybackManager` to prevent playback calls made prior to service binding from failing silently.
- **Instant Server Reconnection on Backup Restore**:
  - Updated `SettingsBackupManager` and `SecureConfigManager` to reload server lists immediately upon backup import and trigger automatic background server syncs (`syncServer`).

## 🚀 Previous Updates (v5.06.00)
- **Mini-Player Forehead Space Optimization**:
  - Removed top outer margins and reduced inner top padding on the floating Mini-Player card (`top = 0.dp`), eliminating unwanted whitespace above the player bar across all screens.
- **Seamless Content-to-Player Alignment**:
  - Streamlined bottom scroll padding across Audiobooks, Music, Bookshelf, Podcasts, and Discover feeds (`contentPadding = PaddingValues(bottom = 12.dp)`), allowing scroll content to align cleanly and directly against the mini-player.

## 🚀 Previous Updates (v5.05.00)
- **Unified Visual Styling Across All Main Media Screens**:
  - Replicated the polished design system of the `PodcastsScreen` across all main media sections: `Audiobooks` (`LibraryScreen`), `Music` (`MusicScreen`), `Bookshelf` (`EBooksScreen`), and `Discover & Blends` (`DiscoveryScreen`).
- **Sleek Pill Segmented Switchers**:
  - Modernized the `Personal Library` vs `Public Domain` tab rows with rounded, pill-shaped segmented button containers, subtle dark background fills, bold white active typography, and vibrant accent highlights.
- **Header & Search Bar Consistency**:
  - Streamlined top header bars with clean section icons (`Headphones`, `MusicNote`, `MenuBook`, `Explore`), bold display titles, compact settings buttons, and translucent dark glass search bars with accent focus borders.

## 🚀 Previous Updates (v5.04.00)
- **New Podcast Navigation Hub & Directory**:
  - Integrated a dedicated "Podcasts" bottom navigation section with a personal/public toggle.
  - Personal section supports local device media vs personal server filtering (`All Personal`, `Local Device`, `Personal Server`).
  - Public directory features direct curated feeds from requested directories (*PlayPodcast.net*, *RSS.com Community Showcase*, and *GetPodcast Global Charts*) with AI Daily Briefing blends.
- **Smart System Back Button Navigation Stack**:
  - Refactored `BackHandler` so system back button presses smoothly pop the navigation stack back to the previous screen rather than resetting to the personal audiobooks library.
- **Overhauled Artist Bio & Discography Experience**:
  - **Popular Tracks Dropdown**: Features a persistent 1-tap dropdown for top tracks (stuck open by default) with instant play buttons on the right of each song.
  - **Chronological Albums Shelf**: Displays artist discography albums sorted newest to oldest from left to right below top tracks.
  - **AI Sanity Check Indicator**: Background verification loading state ensuring images, metadata, and tracks are fully validated.
- **AI-Guided Cinematic Comic & Manga Panel Zoom**:
  - Integrated "Cinematic" mode into `ComicReaderScreen` with energetic spring transitions and panel-by-panel guided zoom (Top-Left, Top-Right, Bottom-Left, Bottom-Right) across pages.
  - Added smart comic detection when opening books so CBZ/CBR/Manga archives automatically trigger the high-performance comic viewer instead of text reformatting.
- **Solid Full-Screen Player Background**:
  - Reinforced `PlayerScreen` root container with an opaque dark base canvas to prevent lower screens or controls from bleeding through during sheet transitions.

## 🚀 Previous Updates (v5.03.00)
- **Authentic Internet Creator Biographies**:
  - Replaced repetitive generic AI template text with authentic, verified biographies fetched live from Wikipedia's official REST API, Wikidata, and curated encyclopedic archives.
  - Automatically queries page summaries, life eras, roles, and multi-paragraph biographical essays with smooth background fallback.
- **Genuine Internet Portrait Photos**:
  - High-resolution creator portraits sourced directly from Wikimedia Commons and official web archives displayed inside an elevated avatar container with subtle glowing gradients.
- **Direct Wikipedia & IMDb Action Hub**:
  - Integrated one-tap interactive action buttons linking directly to the creator's live Wikipedia article, IMDb filmography/credits profile, and Internet Archive media repository.
  - Added a one-tap "Copy Bio" button with instant clipboard confirmation.
- **Curated Historical Mastermind Encyclopedia**:
  - Built-in instant, zero-latency database of authentic biographies and Wikimedia portraits for classic public domain creators, novelists, spoken-word authors, and classical composers (*H. G. Wells, Mary Shelley, Jane Austen, Arthur Conan Doyle, Edgar Allan Poe, Jules Verne, Bram Stoker, Mark Twain, Charles Dickens, Beethoven, Mozart, Bach, Tchaikovsky, Chopin, Kafka, Lovecraft, Wilde, Woolf, London, Carroll, Stevenson, Melville, Shakespeare*).
- **Author Jump-Cards on Media Detail Views**:
  - Added an interactive creator spotlight card to all media detail screens, allowing effortless jumping to the creator's full profile and catalog of works.

## 🚀 Previous Updates (v5.02.00)
- **Personal Server E-Book Loading Resolution**:
  - Implemented an intelligent multi-endpoint fallback engine for personal servers (Booklore, Komga, Audiobookshelf, Calibre-Web).
  - Automatically extracts tokens from query parameters or settings, providing dual-header authentication (`Authorization: Bearer <token>` and `x-auth-token`) along with URL query fallback.
  - Added permissive SSL handling and cross-protocol redirect support for self-signed certificates on personal server instances.
  - Upgraded EPUB and text stream parsing with natural chapter ordering, HTML entity decoding, and directory-agnostic unpacking.
- **Zero-Crash Audiobook Playback Transition**:
  - Solved playback crashes when switching between audiobooks on personal servers.
  - Thread-isolated all ExoPlayer operations (`setMediaItem`, `prepare`, `seekTo`, `play`, `stop`) strictly on the main application looper.
  - Integrated `androidx.media3:media3-datasource-okhttp` with permissive SSL and custom User-Agent headers in `PlaybackService` to stream flawlessly from personal servers.
  - Hardened player state listeners and progress updates to eliminate race conditions during track transitions.
- **Optimized Media Progress Synchronization**:
  - Throttled progress persistence to prevent background storage contention and UI thread lockups during active audio playback.

## 🚀 Previous Updates (v5.01.00)
- **Fully Populated Dual-Side Discovery Feeds**: Completely overhauled the Discovery screen so both **Private Library** and **Public Domain** tabs are vibrant, rich, interactive, and never empty or dry.
- **Public Domain Masterpiece Showcase & Sliding Shelves**:
  - **Featured Masterpiece Hero Spotlight**: Dynamic spotlight banner for premier classic literature and LibriVox recordings with direct 1-tap playback or reading.
  - **Immortal Classic E-Books Shelf**: Curated Project Gutenberg & Smithsonian masterworks (*Frankenstein*, *The Great Gatsby*, *Dracula*, *The Picture of Dorian Gray*, *The Art of War*, *Meditations*, *Twenty Thousand Leagues Under the Sea*, *The Metamorphosis*, *Moby Dick*, *The Prince*, *Jane Eyre*, *Wuthering Heights*) with 1-tap reading.
  - **Dramatic Audiobooks Shelf**: Full LibriVox unabridged voice recordings (*Sherlock Holmes*, *Dracula*, *Frankenstein*, *Art of War*, *The Time Machine*, *Alice in Wonderland*, *Pride and Prejudice*, *The Great Gatsby*) with duration badges and 1-tap audio playback.
  - **Golden Age Comics & Illustrated Stories Shelf**: Historic vintage comics (*Little Nemo in Slumberland*, *Planet Comics: Cosmic Patrol*, *Krazy Kat*, *Whiz Comics / Captain Marvel*) with 1-tap comic viewing.
  - **Masterpiece Classical & Archive Recordings Shelf**: Orchestral and piano masterworks (*Beethoven*, *Debussy*, *Vivaldi*, *Chopin*, *Scott Joplin*, *Mozart*, *Bach*) with instant streaming.
  - **Curated Historical Theme Clusters**: Interactive deep dives into Sci-Fi Pioneers, Victorian Mystery, Ancient Philosophy, Roaring 20s Jazz, and High Seas Adventures.
- **Private Library Multi-Media Showcase**:
  - **"Made For You" Daily Media Blend Hero**: Dynamic hero card tailored to the user's listening and reading habits.
  - **"Continue Your Journey" Resume Shelf**: 1-tap pickup for in-progress audiobooks, e-books, and recent tracks.
  - **AI Dynamic Time-of-Day Mixes**: Evolving music mixes (Morning Awakening, Midday Focus, Golden Hour, Midnight Low-End, Deep Cuts).
  - **Top E-Books & Featured Audiobooks**: Direct carousels of synced media from Audiobookshelf, Booklore, and Plex.
  - **100+ Moods & Vibe Explorer**: Interactive carousel from the 100-mood catalog.
- **Quick Media Type Filter Pills**: Rapidly toggle feeds between All Media, Audiobooks, E-Books & Comics, and Music & Mixes.
- **Direct Action Discovery Cards**: AI recommendations now feature immediate 1-tap "Read Now", "Listen", "Play", and "Details" buttons for seamless exploration.

## 🚀 Previous Updates (v5.0.0)
- **Clickable Music Category Navigation & Full Shelf Views**: Replaced single-row horizontal scrolling restrictions across all music shelves. Tapping any category header (Recent Grooves, New Releases, Featured Artists, AI Dynamic Mixes, All Songs) opens a dedicated, full-screen category drill-down view with full grid/list layouts, Play All, and Shuffle controls.
- **100+ Distinct Stylized Moods**: Added a brand new "Moods" tab to the music hierarchy matching the visual design and polish of the Genres screen. Browse and play tracks from over 100 moods categorized into 10 vibe clusters (Chillout, Lo-Fi, Deep Focus, Cyberpunk, 80s Synth, High Energy, Cinematic, Coffeehouse, Cosmic, and Party) with custom gradients and vector iconography.
- **AI Listening History "For You" Mix**: The music engine analyzes playback patterns, frequent artists, and top genres from listening history to create a personalized, dynamically generated "For You" mix with one-tap playback.
- **Dynamic Time-of-Day & Style Mixes**: Automatic dynamic mix generation that adapts multiple times a day (Morning Awakening, Midday Focus, Golden Hour, Midnight Low-End, Heavy Rotation, Deep Cuts, and Genre Fusion).
- **Dynamic AI Category Shuffling**: Mix up and remix your category shelves on demand or throughout the day with the one-tap remix button.
- **Revamped Discovery & Home Page**: Transformed the home explore experience with interactive prompt suggestion chips, live 'For You' music showcase cards, and instant mood exploration carousels.

## 🚀 Previous Updates (v4.9.1)
- **Resilient Personal Media Loading & Unified Fallback**: Completely fixed library view filtering across Audiobooks, E-Books, and Music screens. Personal media now loads seamlessly from connected servers (Audiobookshelf, Plex, Booklore), configured local device storage folders, and starter collections without getting masked by empty server filters.
- **Automated Background Server Sync & Local Media Scanning**: HomeCast now automatically executes background synchronization for all configured servers and scans enabled local storage folders upon application launch and configuration restoration.
- **One-Tap Header Sync & Refresh**: Added dedicated quick-sync buttons to the top header bars on Audiobooks, E-Books, and Music screens with real-time animated loading state indicators.
- **Full Backup Payload Preservation**: Hardened silent and exported JSON backup routines (`homecast_backup.json`) to guarantee that local folder profiles, public domain source configurations, reading bookmarks, and exact playback timestamps are preserved across sessions and reinstalls.

## 🚀 Previous Updates (v4.9.0)
- **True Screen-Budget Dynamic Pagination Engine**: Completely solved the multi-page chapter pagination engine in the E-Reader. Long text and chapters are cleanly divided into discrete screen-fitting pages with dynamic word capacity calculation based on typography (font size and line spacing). Swiping or tapping turns one discrete page at a time with smooth 3D page curl physics, eliminating single-page chapter bugs.
- **Universal Bookmarks for Books, Audiobooks & Music**: Added a comprehensive bookmarking system across all media types. Save multiple custom bookmarks with timestamp/excerpt previews and optional notes.
- **Interactive Bookmarks & Last Spot Drawer**: Browse all saved bookmarks and instantly resume reading or listening from your last spot with a single tap.
- **Instant JSON & Room Synchronization**: Every page turn, scrubber seek, and bookmark is immediately written to local Room storage and exported into the portable `homecast_backup.json` backup file.

## 🚀 Previous Updates (v4.8.0)
- **Granular Page-by-Page E-Reader Navigation**: Transformed the e-reader experience so every swipe or tap turns one individual page rather than advancing entire chapters. Chapters seamlessly transition only when the reader reaches the final page of a chapter.
- **Automatic Reading Progress Preservation**: The e-reader automatically records and saves the exact chapter and page upon every page turn, screen navigation, or app exit, instantly restoring your position when reopened.
- **Universal Media Progress in JSON Backup**: All reading progress (E-Books and Comics) and playback progress (Audiobooks and Music) are persisted directly into the portable `homecast_backup.json` offline file alongside server configurations and settings.
- **Cross-Session Comic Page Memory**: Comic and Manga reader now saves and restores the exact page index across app launches.

## 🚀 Previous Updates (v4.7.0)
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
