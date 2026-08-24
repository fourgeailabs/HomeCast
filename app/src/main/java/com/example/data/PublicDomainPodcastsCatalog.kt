package com.example.data

import com.example.ui.screens.PodcastChannel
import com.example.ui.screens.PodcastEpisode

object PublicDomainPodcastsCatalog {

    val categories = listOf(
        "All",
        "Video Podcasts",
        "Old Time Radio",
        "Science & Tech",
        "History & Culture",
        "Philosophy & Books",
        "News & Ideas",
        "Audio Serials",
        "Indie & Community"
    )

    val curatedPodcasts: List<PodcastChannel> = listOf(
        // Video Podcasts (High-Definition Video Enclosures)
        PodcastChannel(
            id = "pod_video_nasa",
            title = "NASA HD Video Telemetry & Missions",
            publisher = "NASA Public Domain Video Feed",
            coverUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=600&q=80",
            description = "High-definition video podcast broadcasts featuring rocket launches, spacewalks, Mars rover missions, and deep space cosmos rendering.",
            category = "Video Podcasts",
            feedUrl = "https://www.nasa.gov/rss/dyn/lg_image_of_the_day.rss",
            isPublic = true,
            episodes = listOf(
                PodcastEpisode(
                    id = "ep_vid_nasa_1",
                    title = "Artemis & Deep Space Exploration (HD Video)",
                    podcastTitle = "NASA HD Video Telemetry",
                    publisher = "NASA Video",
                    durationSeconds = 600L,
                    audioUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                    coverUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=600&q=80",
                    publishDate = "Today",
                    description = "Full HD video coverage of lunar orbit insertion, crew module telemetry, and space architecture tests.",
                    isVideo = true
                ),
                PodcastEpisode(
                    id = "ep_vid_nasa_2",
                    title = "James Webb Deep Field Universe Scan (Video)",
                    podcastTitle = "NASA HD Video Telemetry",
                    publisher = "NASA Video",
                    durationSeconds = 596L,
                    audioUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                    coverUrl = "https://images.unsplash.com/photo-1446776811953-b23d57bd21aa?w=600&q=80",
                    publishDate = "Yesterday",
                    description = "High resolution infrared imagery flythrough of distant galaxies rendered in 4K HDR.",
                    isVideo = true
                )
            )
        ),
        PodcastChannel(
            id = "pod_video_blender",
            title = "Open Culture HD Cinema & Video Serials",
            publisher = "Blender Foundation & Public Cinema",
            coverUrl = "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=600&q=80",
            description = "Open-access cinema, 3D animated film shorts, CGI breakthroughs, and public domain video serials in full 1080p video.",
            category = "Video Podcasts",
            feedUrl = "https://peach.blender.org/feed/",
            isPublic = true,
            episodes = listOf(
                PodcastEpisode(
                    id = "ep_vid_blender_1",
                    title = "Sintel: Open Video Cinema Short (1080p)",
                    podcastTitle = "Open Culture HD Cinema",
                    publisher = "Blender Open Video",
                    durationSeconds = 888L,
                    audioUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                    coverUrl = "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=600&q=80",
                    publishDate = "Public Domain",
                    description = "A lonely young warrior girl searches for a baby dragon in a visually captivating fantasy world.",
                    isVideo = true
                ),
                PodcastEpisode(
                    id = "ep_vid_blender_2",
                    title = "Tears of Steel: VFX Sci-Fi Short (1080p)",
                    podcastTitle = "Open Culture HD Cinema",
                    publisher = "Blender Open Video",
                    durationSeconds = 734L,
                    audioUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                    coverUrl = "https://images.unsplash.com/photo-1518770660439-4636190af475?w=600&q=80",
                    publishDate = "Public Domain",
                    description = "Set in a dystopian future in Amsterdam where a group of rebels attempt to save the world from robotics.",
                    isVideo = true
                )
            )
        ),
        // Old Time Radio & Classic Audio Drama
        PodcastChannel(
            id = "pod_otr_shadow",
            title = "The Shadow - Classic Radio Theatre",
            publisher = "Mutual Broadcasting Network (Public Domain)",
            coverUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&q=80",
            description = "Who knows what evil lurks in the hearts of men? The Shadow knows. Classic 1930s-1940s mystery audio serials starring Orson Welles.",
            category = "Old Time Radio",
            feedUrl = "https://itunes.apple.com/search?media=podcast&term=the+shadow+radio",
            isPublic = true,
            episodes = listOf(
                PodcastEpisode("ep_shadow_1", "The Death House Rescue", "The Shadow - Classic Radio Theatre", "Mutual Network", 1800L, "https://archive.org/download/TheShadow_682/The_Shadow_37-09-26_001_The_Death_House_Rescue.mp3", "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&q=80", "Sep 1937", "The Lamont Cranston origin episode where The Shadow thwarts an unjust execution."),
                PodcastEpisode("ep_shadow_2", "The Temple of Bells", "The Shadow - Classic Radio Theatre", "Mutual Network", 1750L, "https://archive.org/download/TheShadow_682/The_Shadow_37-10-24_005_The_Temple_of_Bells.mp3", "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&q=80", "Oct 1937", "A sinister ringing bell signals danger in San Francisco's Chinatown."),
                PodcastEpisode("ep_shadow_3", "The Phantom Voice", "The Shadow - Classic Radio Theatre", "Mutual Network", 1820L, "https://archive.org/download/TheShadow_682/The_Shadow_37-10-31_006_The_Phantom_Voice.mp3", "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&q=80", "Oct 1937", "An invisible adversary threatens city officials over broadcast airwaves.")
            )
        ),
        PodcastChannel(
            id = "pod_otr_dragnet",
            title = "Dragnet - Authentic Police Files",
            publisher = "NBC Radio Public Archives",
            coverUrl = "https://images.unsplash.com/photo-1453873531674-2751b149f0f3?w=600&q=80",
            description = "Ladies and gentlemen, the story you are about to hear is true. Jack Webb stars as Sergeant Joe Friday in ground-breaking police procedural radio.",
            category = "Old Time Radio",
            feedUrl = "https://itunes.apple.com/search?media=podcast&term=dragnet+old+time+radio",
            isPublic = true,
            episodes = listOf(
                PodcastEpisode("ep_dragnet_1", "The Original Police Pilot", "Dragnet", "NBC Radio", 1740L, "https://archive.org/download/OTRR_Dragnet_Singles/Dragnet_49-06-03_001_Helster_Color_Tattoo.mp3", "https://images.unsplash.com/photo-1453873531674-2751b149f0f3?w=600&q=80", "Jun 1949", "Joe Friday investigates a robbery with a unique color tattoo clue."),
                PodcastEpisode("ep_dragnet_2", "The Werewolf Case", "Dragnet", "NBC Radio", 1810L, "https://archive.org/download/OTRR_Dragnet_Singles/Dragnet_49-06-10_002_The_Werewolf.mp3", "https://images.unsplash.com/photo-1453873531674-2751b149f0f3?w=600&q=80", "Jun 1949", "A nighttime prowler leaves strange claw marks across downtown Los Angeles.")
            )
        ),
        PodcastChannel(
            id = "pod_otr_sherlock",
            title = "The New Adventures of Sherlock Holmes",
            publisher = "Classic Radio Theater",
            coverUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=600&q=80",
            description = "Basil Rathbone as Sherlock Holmes and Nigel Bruce as Dr. Watson solve Arthur Conan Doyle mysteries on 1940s radio.",
            category = "Old Time Radio",
            feedUrl = "https://itunes.apple.com/search?media=podcast&term=sherlock+holmes+radio",
            isPublic = true,
            episodes = listOf(
                PodcastEpisode("ep_sherlock_1", "The Speckled Band Radio Play", "Sherlock Holmes Radio", "Classic Radio Theater", 1800L, "https://archive.org/download/OTRR_Sherlock_Holmes_Rathbone_Bruce_Singles/Sherlock_Holmes_39-11-06_The_Speckled_Band.mp3", "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=600&q=80", "Nov 1939", "Holmes investigates a deadly whistling sound in an old English manor."),
                PodcastEpisode("ep_sherlock_2", "The Hound of the Baskervilles", "Sherlock Holmes Radio", "Classic Radio Theater", 2400L, "https://archive.org/download/OTRR_Sherlock_Holmes_Rathbone_Bruce_Singles/Sherlock_Holmes_39-09-25_The_Hound_of_the_Baskervilles.mp3", "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=600&q=80", "Sep 1939", "A glowing beast haunts the foggy Dartmoor marshes.")
            )
        ),
        PodcastChannel(
            id = "pod_otr_scifi",
            title = "X Minus One - Sci-Fi Theater",
            publisher = "NBC & Galaxy Science Fiction",
            coverUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=600&q=80",
            description = "Pioneering radio drama adapting stories by Isaac Asimov, Ray Bradbury, Philip K. Dick, and Robert A. Heinlein.",
            category = "Old Time Radio",
            feedUrl = "https://itunes.apple.com/search?media=podcast&term=x+minus+one",
            isPublic = true,
            episodes = listOf(
                PodcastEpisode("ep_xminus_1", "Mars Is Heaven! (Ray Bradbury)", "X Minus One", "NBC Radio", 1750L, "https://archive.org/download/XMinusOne1/55-05-08_Mars_Is_Heaven.mp3", "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=600&q=80", "May 1955", "Earth astronauts land on Mars only to find an exact replica of their 1920s hometowns."),
                PodcastEpisode("ep_xminus_2", "Nightfall (Isaac Asimov)", "X Minus One", "NBC Radio", 1820L, "https://archive.org/download/XMinusOne1/55-12-07_Nightfall.mp3", "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=600&q=80", "Dec 1955", "A planet bathed in six suns faces darkness for the first time in 2,000 years.")
            )
        ),

        // Science & Tech
        PodcastChannel(
            id = "pod_tech_daily",
            title = "Silicon Valley Tech & AI Daily",
            publisher = "FourgeAI Labs Audio",
            coverUrl = "https://images.unsplash.com/photo-1518770660439-4636190af475?w=600&q=80",
            description = "Daily executive briefings on artificial intelligence, mobile architecture, cloud infrastructure, and open source breakthroughs.",
            category = "Science & Tech",
            feedUrl = "https://rss.art19.com/tech-news-briefing",
            isPublic = true,
            episodes = listOf(
                PodcastEpisode("ep_tech_1", "On-Device Neural Accelerators & Mobile AI", "Tech & AI Daily", "FourgeAI Labs", 2100L, "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-__Fly_Paper.mp3", "https://images.unsplash.com/photo-1518770660439-4636190af475?w=600&q=80", "Today", "Optimizing quantized LLMs and Jetpack Compose state engines."),
                PodcastEpisode("ep_tech_2", "The Future of WebAssembly & Native Android Core", "Tech & AI Daily", "FourgeAI Labs", 1950L, "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-__Fly_Paper.mp3", "https://images.unsplash.com/photo-1518770660439-4636190af475?w=600&q=80", "Yesterday", "How C++ and Rust native libraries interface with Android NDK.")
            )
        ),
        PodcastChannel(
            id = "pod_nasa_casts",
            title = "NASA Science Casts",
            publisher = "NASA Goddard & JPL Public Feed",
            coverUrl = "https://images.unsplash.com/photo-1446776811953-b23d57bd21aa?w=600&q=80",
            description = "Official NASA audio telemetry, space exploration news, James Webb Space Telescope discoveries, and Artemis mission updates.",
            category = "Science & Tech",
            feedUrl = "https://www.nasa.gov/rss/dyn/breaking_news.rss",
            isPublic = true,
            episodes = listOf(
                PodcastEpisode("ep_nasa_1", "Deep Space Telemetry from Webb Telescope", "NASA Science Casts", "NASA", 1500L, "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-__Fly_Paper.mp3", "https://images.unsplash.com/photo-1446776811953-b23d57bd21aa?w=600&q=80", "This Week", "Analyzing atmospheric water vapor signatures on exoplanets 120 light years away."),
                PodcastEpisode("ep_nasa_2", "Artemis Lunar Base Architecture", "NASA Science Casts", "NASA", 1800L, "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-__Fly_Paper.mp3", "https://images.unsplash.com/photo-1446776811953-b23d57bd21aa?w=600&q=80", "Last Week", "Building sustainable human habitats at the moon's south pole.")
            )
        ),
        PodcastChannel(
            id = "pod_astronomy_cast",
            title = "Astronomy Cast - Cosmos Explored",
            publisher = "Astrosphere New Media",
            coverUrl = "https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?w=600&q=80",
            description = "Fraser Cain and Dr. Pamela Gay take a fact-based journey through the cosmos, astrophysics, and quantum physics.",
            category = "Science & Tech",
            feedUrl = "https://astronomycast.com/feed/",
            isPublic = true,
            episodes = listOf(
                PodcastEpisode("ep_astro_1", "Black Hole Event Horizons & Hawking Radiation", "Astronomy Cast", "Astrosphere", 2200L, "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-__Fly_Paper.mp3", "https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?w=600&q=80", "3 days ago", "What happens to information when matter crosses the event horizon?"),
                PodcastEpisode("ep_astro_2", "Dark Energy & The Expansion of the Universe", "Astronomy Cast", "Astrosphere", 2100L, "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-__Fly_Paper.mp3", "https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?w=600&q=80", "1 week ago", "Measuring cosmic acceleration through type Ia supernovae.")
            )
        ),

        // History & Culture
        PodcastChannel(
            id = "pod_hist_echoes",
            title = "Echoes of History - World Civilizations",
            publisher = "Archive Cultural Media",
            coverUrl = "https://images.unsplash.com/photo-1461360370896-922624d12aa1?w=600&q=80",
            description = "Exploring ancient empires, pivotal battles, industrial revolutions, and intellectual movements that shaped human civilization.",
            category = "History & Culture",
            feedUrl = "https://feeds.simplecast.com/history_echoes",
            isPublic = true,
            episodes = listOf(
                PodcastEpisode("ep_hist_1", "The Library of Alexandria & Ancient Wisdom", "Echoes of History", "Archive Cultural Media", 3100L, "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-__Fly_Paper.mp3", "https://images.unsplash.com/photo-1461360370896-922624d12aa1?w=600&q=80", "4 days ago", "How scholars gathered knowledge across the Hellenistic Mediterranean."),
                PodcastEpisode("ep_hist_2", "The Silk Road Exchange & Trade Empires", "Echoes of History", "Archive Cultural Media", 2900L, "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-__Fly_Paper.mp3", "https://images.unsplash.com/photo-1461360370896-922624d12aa1?w=600&q=80", "1 week ago", "Caravans, papermaking technology, and cultural diffusion between East and West.")
            )
        ),
        PodcastChannel(
            id = "pod_bbc_inourtime",
            title = "In Our Time - Culture & Archives",
            publisher = "BBC Radio 4 Public Archives",
            coverUrl = "https://images.unsplash.com/photo-1457369804613-52c61a468e7d?w=600&q=80",
            description = "Melvyn Bragg and leading academic experts discuss the history of ideas, philosophy, literature, and science.",
            category = "History & Culture",
            feedUrl = "https://podcasts.files.bbci.co.uk/p01dh5yg.rss",
            isPublic = true,
            episodes = listOf(
                PodcastEpisode("ep_bbc_1", "The Magna Carta & Constitutional History", "In Our Time", "BBC Radio 4", 2700L, "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-__Fly_Paper.mp3", "https://images.unsplash.com/photo-1457369804613-52c61a468e7d?w=600&q=80", "Recent", "The 1215 legal charter at Runnymede and its enduring global impact."),
                PodcastEpisode("ep_bbc_2", "The Stoic Philosophers of Rome", "In Our Time", "BBC Radio 4", 2650L, "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-__Fly_Paper.mp3", "https://images.unsplash.com/photo-1457369804613-52c61a468e7d?w=600&q=80", "Recent", "Seneca, Epictetus, and Marcus Aurelius on virtue and resilience.")
            )
        ),

        // Philosophy & Books
        PodcastChannel(
            id = "pod_phil_this",
            title = "Philosophize This! - Open Mind Podcast",
            publisher = "Stephen West Philosophy",
            coverUrl = "https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?w=600&q=80",
            description = "A beginner-friendly educational podcast that moves chronologically through human philosophical ideas from pre-Socratics to existentialism.",
            category = "Philosophy & Books",
            feedUrl = "https://philosophizethis.libsyn.com/rss",
            isPublic = true,
            episodes = listOf(
                PodcastEpisode("ep_phil_1", "Socrates & The Trial of Reason", "Philosophize This!", "Stephen West", 1950L, "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-__Fly_Paper.mp3", "https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?w=600&q=80", "Classic", "Examining Plato's Apology and the unexamined life in Athens."),
                PodcastEpisode("ep_phil_2", "Descartes & Methodological Doubt", "Philosophize This!", "Stephen West", 2100L, "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-__Fly_Paper.mp3", "https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?w=600&q=80", "Classic", "Cogito, ergo sum and the foundations of modern rationalism.")
            )
        ),
        PodcastChannel(
            id = "pod_librivox_serials",
            title = "LibriVox Classic Audio Serials",
            publisher = "LibriVox Public Domain Volunteers",
            coverUrl = "https://images.unsplash.com/photo-1476275466078-4007374efbbe?w=600&q=80",
            description = "Public domain full audiobooks serialized into daily podcast episodes narrated by global volunteers.",
            category = "Philosophy & Books",
            feedUrl = "https://librivox.org/rss/latest_audiobooks",
            isPublic = true,
            episodes = listOf(
                PodcastEpisode("ep_lv_1", "The Time Machine (H.G. Wells) - Episode 1", "LibriVox Serials", "LibriVox", 1800L, "https://archive.org/download/time_machine_0706_librivox/timemachine_01_wells_64kb.mp3", "https://images.unsplash.com/photo-1476275466078-4007374efbbe?w=600&q=80", "Public Domain", "The Time Traveller demonstrates his fourth dimension model to dinner guests."),
                PodcastEpisode("ep_lv_2", "Frankenstein (Mary Shelley) - Episode 1", "LibriVox Serials", "LibriVox", 2100L, "https://archive.org/download/frankenstein_1111_librivox/frankenstein_01_shelley_64kb.mp3", "https://images.unsplash.com/photo-1476275466078-4007374efbbe?w=600&q=80", "Public Domain", "Captain Robert Walton writes to his sister from his Arctic voyage.")
            )
        ),

        // News & Ideas
        PodcastChannel(
            id = "pod_playpodcast",
            title = "PlayPodcast Independent Directory",
            publisher = "PlayPodcast Network",
            coverUrl = "https://images.unsplash.com/photo-1590602847861-f357a9332bbc?w=600&q=80",
            description = "Discover trending independent podcasts, investigative reporting, daily news briefs, and acoustic podcasts.",
            category = "News & Ideas",
            feedUrl = "https://www.playpodcast.net/",
            isPublic = true,
            episodes = listOf(
                PodcastEpisode("ep_play1", "The Tech Tomorrow Show #142", "PlayPodcast Directory", "PlayPodcast Network", 2400L, "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-__Fly_Paper.mp3", "https://images.unsplash.com/photo-1590602847861-f357a9332bbc?w=600&q=80", "Today", "Daily breakthroughs in AI, consumer hardware, and software."),
                PodcastEpisode("ep_play2", "Mindset & Cognitive Performance Digest", "PlayPodcast Directory", "PlayPodcast Network", 1800L, "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-__Fly_Paper.mp3", "https://images.unsplash.com/photo-1590602847861-f357a9332bbc?w=600&q=80", "Yesterday", "Cognitive tools and habit design for creators and engineers.")
            )
        ),
        PodcastChannel(
            id = "pod_rss_community",
            title = "RSS.com Creator Showcase",
            publisher = "RSS.com Podcasting",
            coverUrl = "https://images.unsplash.com/photo-1478737270239-2f02b77fc618?w=600&q=80",
            description = "Global creator network featuring culture, indie music broadcasts, storytelling, and audio production.",
            category = "Indie & Community",
            feedUrl = "https://rss.com/community/",
            isPublic = true,
            episodes = listOf(
                PodcastEpisode("ep_rss1", "Indie Creator Stories Vol. 8", "RSS.com Showcase", "RSS.com Podcasting", 2100L, "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-__Fly_Paper.mp3", "https://images.unsplash.com/photo-1478737270239-2f02b77fc618?w=600&q=80", "3 days ago", "How independent podcasters build active listener communities."),
                PodcastEpisode("ep_rss2", "Acoustic & Ambient Field Recording", "RSS.com Showcase", "RSS.com Podcasting", 3600L, "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-__Fly_Paper.mp3", "https://images.unsplash.com/photo-1478737270239-2f02b77fc618?w=600&q=80", "5 days ago", "High-fidelity binaural recordings from remote forests and sea coasts.")
            )
        ),
        PodcastChannel(
            id = "pod_getpodcast",
            title = "GetPodcast Global Directory",
            publisher = "GetPodcast Platform",
            coverUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&q=80",
            description = "Top charting global audio broadcasts, investigative journalism, documentary series, and science.",
            category = "News & Ideas",
            feedUrl = "https://getpodcast.com/",
            isPublic = true,
            episodes = listOf(
                PodcastEpisode("ep_get1", "The Neuroscience of Deep Focus", "GetPodcast Global", "GetPodcast Platform", 2800L, "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-__Fly_Paper.mp3", "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&q=80", "This Week", "Tools and protocols for sustained focus and cognitive endurance."),
                PodcastEpisode("ep_get2", "Exoplanet Atmospheres & JWST Telemetry", "GetPodcast Global", "GetPodcast Platform", 3200L, "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-__Fly_Paper.mp3", "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&q=80", "Last Week", "Measuring methane and carbon dioxide in distant worlds.")
            )
        )
    )
}
