package com.example.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class MoodItem(
    val id: String,
    val name: String,
    val group: String,
    val description: String,
    val gradient: List<Color>,
    val icon: ImageVector,
    val keywords: List<String>
)

object MusicMoodsCatalog {

    val moodGroups = listOf(
        "All",
        "Chill & Unwind",
        "Focus & Flow",
        "Energy & Workout",
        "Dark & Moody",
        "Party & Groove",
        "Nostalgia & Retro",
        "Cinematic & Epic",
        "Romance & Soul",
        "Nature & Space",
        "Upbeat & Sunshine"
    )

    val allMoods: List<MoodItem> = listOf(
        // === 1. CHILL & UNWIND ===
        MoodItem("mood_lofi_chill", "Lo-Fi Chill", "Chill & Unwind", "Mellow dusty beats & vinyl crackles", listOf(Color(0xFF5C6BC0), Color(0xFF8E24AA)), Icons.Default.Nightlight, listOf("lo-fi", "chill", "ambient", "beat", "jazz", "mellow")),
        MoodItem("mood_rainy_day", "Rainy Day Cozy", "Chill & Unwind", "Warm tea, misty windows & soft chords", listOf(Color(0xFF455A64), Color(0xFF607D8B)), Icons.Default.Cloud, listOf("rain", "acoustic", "piano", "folk", "soft", "calm")),
        MoodItem("mood_coffeehouse", "Coffeehouse Melodies", "Chill & Unwind", "Warm acoustic guitars & indie vocals", listOf(Color(0xFF6D4C41), Color(0xFF8D6E63)), Icons.Default.Coffee, listOf("acoustic", "indie", "coffee", "folk", "guitar", "vocal")),
        MoodItem("mood_lazy_sunday", "Lazy Sunday", "Chill & Unwind", "Unhurried tempos and gentle breezes", listOf(Color(0xFF81C784), Color(0xFF4DB6AC)), Icons.Default.Weekend, listOf("easy", "sunday", "pop", "acoustic", "soft", "light")),
        MoodItem("mood_warm_blanket", "Warm Blanket", "Chill & Unwind", "Gentle ambient textures & soothing soul", listOf(Color(0xFFFF8A65), Color(0xFFFFB74D)), Icons.Default.Bedtime, listOf("ambient", "soul", "relax", "warm", "peaceful")),
        MoodItem("mood_mellow_waves", "Mellow Waves", "Chill & Unwind", "Smooth synths and laid-back rhythms", listOf(Color(0xFF26C6DA), Color(0xFF0097A7)), Icons.Default.Waves, listOf("waves", "surf", "smooth", "synth", "chillout")),
        MoodItem("mood_cloud_nine", "Cloud Nine", "Chill & Unwind", "Ethereal pads and weightless melodies", listOf(Color(0xFF90CAF9), Color(0xFFCE93D8)), Icons.Default.CloudQueue, listOf("ethereal", "dream", "cloud", "pad", "float")),
        MoodItem("mood_acoustic_hearth", "Acoustic Hearth", "Chill & Unwind", "Fireside fingerpicking & intimate ballads", listOf(Color(0xFFD84315), Color(0xFFEF6C00)), Icons.Default.Fireplace, listOf("acoustic", "fire", "ballad", "hearth", "guitar")),
        MoodItem("mood_zen_garden", "Zen Garden", "Chill & Unwind", "Meditation bells, koto & quiet rivers", listOf(Color(0xFF66BB6A), Color(0xFF43A047)), Icons.Default.Spa, listOf("zen", "meditation", "flute", "bamboo", "peace")),
        MoodItem("mood_sunset_serenade", "Sunset Serenade", "Chill & Unwind", "Golden hour jazz chords & soft sax", listOf(Color(0xFFFF7043), Color(0xFFAB47BC)), Icons.Default.WbTwilight, listOf("sunset", "jazz", "sax", "golden", "twilight")),

        // === 2. FOCUS & FLOW ===
        MoodItem("mood_deep_focus", "Deep Focus", "Focus & Flow", "Binaural hums & distraction-free rhythms", listOf(Color(0xFF1E88E5), Color(0xFF1565C0)), Icons.Default.Psychology, listOf("focus", "study", "instrumental", "binaural", "minimal")),
        MoodItem("mood_cyber_coding", "Cyberpunk Coding", "Focus & Flow", "Dark synth arpeggios for deep terminal work", listOf(Color(0xFF00E5FF), Color(0xFF2979FF)), Icons.Default.Code, listOf("cyber", "synth", "synthwave", "electronic", "code")),
        MoodItem("mood_alpha_waves", "Alpha Waves", "Focus & Flow", "Steady frequencies boosting mental clarity", listOf(Color(0xFF26A69A), Color(0xFF00897B)), Icons.Default.GraphicEq, listOf("alpha", "brain", "focus", "ambient", "clarity")),
        MoodItem("mood_flow_state", "Flow State", "Focus & Flow", "Hypnotic beats keeping you in the zone", listOf(Color(0xFF5E35B1), Color(0xFF3949AB)), Icons.Default.Stream, listOf("flow", "hypnotic", "techno", "minimal", "groove")),
        MoodItem("mood_study_sanctuary", "Study Sanctuary", "Focus & Flow", "Delicate classical piano & library silence", listOf(Color(0xFF7986CB), Color(0xFF3F51B5)), Icons.Default.MenuBook, listOf("classical", "piano", "study", "library", "quiet")),
        MoodItem("mood_minimal_mind", "Minimalist Mind", "Focus & Flow", "Clean polyrhythms and uncluttered soundscapes", listOf(Color(0xFF78909C), Color(0xFF546E7A)), Icons.Default.CenterFocusStrong, listOf("minimal", "modular", "ambient", "clean", "structure")),
        MoodItem("mood_clockwork", "Clockwork Precision", "Focus & Flow", "Metric mechanical ticks & mathematical pacing", listOf(Color(0xFF455A64), Color(0xFF263238)), Icons.Default.Timer, listOf("tempo", "math", "precision", "electronic", "idm")),
        MoodItem("mood_white_noise_oasis", "White Noise Oasis", "Focus & Flow", "Constant soothing noise profiles & drone chords", listOf(Color(0xFFB0BEC5), Color(0xFF78909C)), Icons.Default.BlurOn, listOf("drone", "noise", "sleep", "focus", "calm")),
        MoodItem("mood_midnight_hacks", "Midnight Hacks", "Focus & Flow", "Low-lit ambient basslines for 3 AM breakthroughs", listOf(Color(0xFF00ACC1), Color(0xFF1A237E)), Icons.Default.Terminal, listOf("tech", "night", "dark", "electronic", "synth")),
        MoodItem("mood_ambient_architecture", "Ambient Architecture", "Focus & Flow", "Sprawling reverbs and expansive spaces", listOf(Color(0xFF4DB6AC), Color(0xFF00796B)), Icons.Default.Domain, listOf("ambient", "reverb", "hall", "soundscape", "space")),

        // === 3. ENERGY & WORKOUT ===
        MoodItem("mood_high_voltage", "High Voltage", "Energy & Workout", "Electrifying bass drops and adrenaline pumps", listOf(Color(0xFFFF1744), Color(0xFFFF5252)), Icons.Default.ElectricBolt, listOf("rock", "metal", "bass", "workout", "gym", "hype")),
        MoodItem("mood_power_beast", "Beast Mode", "Energy & Workout", "Aggressive drops and heavy trap drums", listOf(Color(0xFFD50000), Color(0xFF880E4F)), Icons.Default.FitnessCenter, listOf("gym", "trap", "hardcore", "beast", "heavy")),
        MoodItem("mood_adrenaline_rush", "Adrenaline Rush", "Energy & Workout", "Fast bpm synth sprints and drum & bass", listOf(Color(0xFFFF6D00), Color(0xFFFF3D00)), Icons.Default.Speed, listOf("dnb", "breakbeat", "fast", "sprint", "rush")),
        MoodItem("mood_cardio_blitz", "Cardio Blitz", "Energy & Workout", "Unstoppable 130+ bpm pop & house anthems", listOf(Color(0xFFFF4081), Color(0xFFF50057)), Icons.Default.DirectionsRun, listOf("cardio", "house", "dance", "tempo", "running")),
        MoodItem("mood_iron_temple", "Iron Temple", "Energy & Workout", "Industrial metal guitars for heavy lifting", listOf(Color(0xFF424242), Color(0xFF212121)), Icons.Default.SportsMartialArts, listOf("metal", "heavy", "industrial", "rock", "weights")),
        MoodItem("mood_neon_runners", "Neon Runners", "Energy & Workout", "Night-run retrowave driving beats", listOf(Color(0xFF00E676), Color(0xFF00B0FF)), Icons.Default.DirectionsBike, listOf("synthwave", "run", "night", "neon", "drive")),
        MoodItem("mood_boxing_ring", "Fight Night", "Energy & Workout", "Punchy hip hop verses & hard hitting kicks", listOf(Color(0xFFFFAB00), Color(0xFFDD2C00)), Icons.Default.SportsMma, listOf("hip hop", "rap", "drill", "fight", "punch")),
        MoodItem("mood_peak_velocity", "Peak Velocity", "Energy & Workout", "Trance crescendos and relentless energy", listOf(Color(0xFF651FFF), Color(0xFF3D5AFE)), Icons.Default.FlightTakeoff, listOf("trance", "rave", "fast", "velocity", "climb")),
        MoodItem("mood_sweat_groove", "Sweat Groove", "Energy & Workout", "Funky basslines making cardio effortless", listOf(Color(0xFFFFAB40), Color(0xFFFF6E40)), Icons.Default.SelfImprovement, listOf("funk", "groove", "disco", "dance", "active")),
        MoodItem("mood_victorious", "Victorious Anthem", "Energy & Workout", "Triumphant horns and cinematic stadium cheers", listOf(Color(0xFFFFD600), Color(0xFFFF9100)), Icons.Default.EmojiEvents, listOf("anthem", "stadium", "rock", "win", "hero")),

        // === 4. DARK & MOODY ===
        MoodItem("mood_neon_noir", "Neon Noir", "Dark & Moody", "Wet pavement reflections & brooding saxophone", listOf(Color(0xFF311B92), Color(0xFF000000)), Icons.Default.Nightlife, listOf("noir", "dark", "jazz", "synth", "moody")),
        MoodItem("mood_gotham_shadow", "Gotham Shadow", "Dark & Moody", "Cinematic dark cellos and echoing strings", listOf(Color(0xFF212121), Color(0xFF37474F)), Icons.Default.Shield, listOf("orchestra", "strings", "dark", "gotham", "shadow")),
        MoodItem("mood_dark_wave", "Darkwave Pulse", "Dark & Moody", "Cold synths, post-punk guitars & gothic echo", listOf(Color(0xFF4A148C), Color(0xFF000000)), Icons.Default.WaterDrop, listOf("gothic", "post-punk", "darkwave", "cold", "synth")),
        MoodItem("mood_brooding_midnight", "Brooding Midnight", "Dark & Moody", "Minor key pianos in empty cavernous halls", listOf(Color(0xFF1A237E), Color(0xFF0D47A1)), Icons.Default.DarkMode, listOf("piano", "minor", "midnight", "echo", "melancholy")),
        MoodItem("mood_smoke_mirrors", "Smoke & Mirrors", "Dark & Moody", "Trip hop breaks and sultry cinematic whispers", listOf(Color(0xFF5D4037), Color(0xFF212121)), Icons.Default.SmokingRooms, listOf("trip-hop", "downtempo", "smoke", "sultry", "chill")),
        MoodItem("mood_industrial_pulse", "Industrial Pulse", "Dark & Moody", "Distorted kicks and rusty iron rhythms", listOf(Color(0xFF37474F), Color(0xFF263238)), Icons.Default.Hardware, listOf("industrial", "ebm", "distort", "heavy", "dark")),
        MoodItem("mood_cyber_dystopia", "Cyber Dystopia", "Dark & Moody", "Ominous drones over futuristic megacities", listOf(Color(0xFF004D40), Color(0xFF000000)), Icons.Default.LocationCity, listOf("dystopia", "ambient", "drone", "cyber", "synth")),
        MoodItem("mood_stormy_skies", "Stormy Skies", "Dark & Moody", "Thunderous sub-bass and tempestuous chords", listOf(Color(0xFF3E2723), Color(0xFF455A64)), Icons.Default.Thunderstorm, listOf("storm", "bass", "heavy", "thunder", "rain")),
        MoodItem("mood_haunting_echoes", "Haunting Echoes", "Dark & Moody", "Ghostly vocal chops and distant reverbs", listOf(Color(0xFF4527A0), Color(0xFF283593)), Icons.Default.RecordVoiceOver, listOf("vocal", "ghost", "echo", "reverb", "haunt")),
        MoodItem("mood_black_velvet", "Black Velvet", "Dark & Moody", "Deep late night blues & smoky guitar solos", listOf(Color(0xFF1B1B1B), Color(0xFF4E342E)), Icons.Default.NightlightRound, listOf("blues", "guitar", "solo", "velvet", "night")),

        // === 5. PARTY & GROOVE ===
        MoodItem("mood_club_ignition", "Club Ignition", "Party & Groove", "Thumping 4-on-the-floor floorfillers", listOf(Color(0xFFD500F9), Color(0xFF651FFF)), Icons.Default.LocalBar, listOf("club", "edm", "dance", "house", "party")),
        MoodItem("mood_funkadelic", "Funkadelic Groove", "Party & Groove", "Slap bass, wah-wah guitars & brass hits", listOf(Color(0xFFFF6D00), Color(0xFFFFD600)), Icons.Default.Celebration, listOf("funk", "slap", "groove", "brass", "disco")),
        MoodItem("mood_house_party", "House Party", "Party & Groove", "Irresistible bounce and crowd-favorite hooks", listOf(Color(0xFFFF1744), Color(0xFFFF9100)), Icons.Default.SpeakerGroup, listOf("party", "house", "pop", "hooks", "banger")),
        MoodItem("mood_latin_fiesta", "Latin Fiesta", "Party & Groove", "Salsa horns, reggaeton dembow & heat", listOf(Color(0xFFFF5252), Color(0xFFFFB74D)), Icons.Default.MusicNote, listOf("latin", "salsa", "reggaeton", "dembow", "fiesta")),
        MoodItem("mood_disco_fever", "Disco Fever", "Party & Groove", "Glittering mirrorballs, strings & bass grooves", listOf(Color(0xFFFF4081), Color(0xFF7C4DFF)), Icons.Default.Stars, listOf("disco", "70s", "dance", "strings", "funk")),
        MoodItem("mood_trap_banger", "Trap Banger", "Party & Groove", "Rattling 808s, rolling hi-hats & anthems", listOf(Color(0xFF00E5FF), Color(0xFFD500F9)), Icons.Default.VolumeUp, listOf("trap", "808", "hip hop", "rap", "banger")),
        MoodItem("mood_neon_dancefloor", "Neon Dancefloor", "Party & Groove", "Eurodance synth leads and pulsating lasers", listOf(Color(0xFF00E676), Color(0xFF00B0FF)), Icons.Default.Lightbulb, listOf("eurodance", "rave", "synth", "dance", "laser")),
        MoodItem("mood_carnival_vibes", "Carnival Vibes", "Party & Groove", "Samba drums and joyous whistle parades", listOf(Color(0xFFFFEA00), Color(0xFF00E676)), Icons.Default.Festival, listOf("samba", "carnival", "drums", "parade", "joy")),
        MoodItem("mood_tropical_beats", "Tropical Beats", "Party & Groove", "Steel drums, marimbas & beach breezes", listOf(Color(0xFF00B4D8), Color(0xFF90E0EF)), Icons.Default.BeachAccess, listOf("tropical", "marimba", "beach", "summer", "island")),
        MoodItem("mood_bass_drop", "Bass Heavyweight", "Party & Groove", "Subwoofer rumbling dubstep & bass music", listOf(Color(0xFF76FF03), Color(0xFF00E5FF)), Icons.Default.SurroundSound, listOf("bass", "dubstep", "sub", "wobble", "drop")),

        // === 6. NOSTALGIA & RETRO ===
        MoodItem("mood_80s_highway", "80s Synth Highway", "Nostalgia & Retro", "Outrun neon grids & analogue synthesizers", listOf(Color(0xFFFF007F), Color(0xFF7928CA)), Icons.Default.DirectionsCar, listOf("80s", "synthwave", "outrun", "retro", "synth")),
        MoodItem("mood_90s_grunge", "90s Grunge Garage", "Nostalgia & Retro", "Fuzzy distortion pedals & flannel angst", listOf(Color(0xFF8D6E63), Color(0xFF3E2723)), Icons.Default.Album, listOf("grunge", "90s", "rock", "alternative", "guitar")),
        MoodItem("mood_vinyl_70s", "Vinyl Crackle 70s", "Nostalgia & Retro", "Warm analog warmth, Rhodes & classic soul", listOf(Color(0xFFFFA000), Color(0xFFE65100)), Icons.Default.Radio, listOf("70s", "vinyl", "classic", "soul", "rock")),
        MoodItem("mood_cassette_memories", "Cassette Memories", "Nostalgia & Retro", "Lo-fi tape saturation & golden memories", listOf(Color(0xFF00ACC1), Color(0xFF00897B)), Icons.Default.Audiotrack, listOf("cassette", "tape", "retro", "memory", "indie")),
        MoodItem("mood_retro_arcade", "Retro Arcade", "Nostalgia & Retro", "8-bit chiptunes, joystick wins & neon coins", listOf(Color(0xFF00E676), Color(0xFFFFD600)), Icons.Default.SportsEsports, listOf("chiptune", "8bit", "arcade", "game", "retro")),
        MoodItem("mood_motown_soul", "Motown Soul Classic", "Nostalgia & Retro", "Tamla brass, tambourines & sweet harmonies", listOf(Color(0xFFE91E63), Color(0xFFFF80AB)), Icons.Default.Mic, listOf("motown", "soul", "60s", "r&b", "classic")),
        MoodItem("mood_vintage_hollywood", "Vintage Hollywood", "Nostalgia & Retro", "Grand sweep strings & Golden Age glamour", listOf(Color(0xFFFFD54F), Color(0xFFFFB300)), Icons.Default.Movie, listOf("vintage", "hollywood", "orchestra", "glamour", "classic")),
        MoodItem("mood_old_school_hiphop", "Boom Bap 90s", "Nostalgia & Retro", "SP-1200 drum chops & classic lyricism", listOf(Color(0xFFFF6F00), Color(0xFFBF360C)), Icons.Default.Headphones, listOf("boombap", "90s", "hip hop", "rap", "sample")),
        MoodItem("mood_polaroid_dreams", "Polaroid Dreams", "Nostalgia & Retro", "Sun-faded dream pop & chorus guitars", listOf(Color(0xFFBA68C8), Color(0xFFE1BEE7)), Icons.Default.PhotoCamera, listOf("dreampop", "indie", "polaroid", "nostalgia", "faded")),
        MoodItem("mood_classic_yacht_rock", "Yacht Rock Bliss", "Nostalgia & Retro", "Smooth West Coast harmonies & electric piano", listOf(Color(0xFF4FC3F7), Color(0xFF0288D1)), Icons.Default.Sailing, listOf("yacht", "smooth", "70s", "80s", "harmony")),

        // === 7. CINEMATIC & EPIC ===
        MoodItem("mood_interstellar", "Interstellar Odyssey", "Cinematic & Epic", "Massive pipe organs and space cosmic swells", listOf(Color(0xFF0D47A1), Color(0xFF311B92)), Icons.Default.RocketLaunch, listOf("space", "cinematic", "epic", "organ", "cosmos")),
        MoodItem("mood_orchestral_majesty", "Orchestral Majesty", "Cinematic & Epic", "Full symphony orchestra reaching grand heights", listOf(Color(0xFFC2185B), Color(0xFF7B1FA2)), Icons.Default.TheaterComedy, listOf("orchestra", "symphony", "classical", "epic", "grand")),
        MoodItem("mood_hero_journey", "The Hero's Journey", "Cinematic & Epic", "Triumphant battle brass and soaring violins", listOf(Color(0xFFF57C00), Color(0xFFD32F2F)), Icons.Default.MilitaryTech, listOf("hero", "soundtrack", "brass", "battle", "epic")),
        MoodItem("mood_hans_tension", "Cinematic Tension", "Cinematic & Epic", "Tick-tock urgency and subterranean sub-drops", listOf(Color(0xFF263238), Color(0xFF000000)), Icons.Default.HourglassEmpty, listOf("tension", "thriller", "clock", "score", "film")),
        MoodItem("mood_viking_valhalla", "Nordic Valhalla", "Cinematic & Epic", "War drums, tagelharpa & throat singing", listOf(Color(0xFF4E342E), Color(0xFF212121)), Icons.Default.ShieldMoon, listOf("nordic", "viking", "folk", "war", "drums")),
        MoodItem("mood_galactic_voyage", "Galactic Voyage", "Cinematic & Epic", "Futuristic synthesisers & planet-hopping themes", listOf(Color(0xFF00B4D8), Color(0xFF7209B7)), Icons.Default.Public, listOf("scifi", "space", "synth", "voyage", "galaxy")),
        MoodItem("mood_film_score_magic", "Film Score Magic", "Cinematic & Epic", "Heartfelt movie themes that evoke pure wonder", listOf(Color(0xFFAB47BC), Color(0xFF4A148C)), Icons.Default.AutoAwesome, listOf("score", "magic", "wonder", "theme", "cinema")),
        MoodItem("mood_cyber_chase", "Cybernetic Chase", "Cinematic & Epic", "Fast-paced synth thrills and racing pulse", listOf(Color(0xFF00E5FF), Color(0xFFFF0055)), Icons.Default.FlashOn, listOf("action", "chase", "fast", "synth", "thrill")),
        MoodItem("mood_medieval_fantasy", "Medieval Fantasy", "Cinematic & Epic", "Lutes, harps & magical tavern storytelling", listOf(Color(0xFF388E3C), Color(0xFF8D6E63)), Icons.Default.Castle, listOf("fantasy", "lute", "medieval", "folk", "tavern")),
        MoodItem("mood_submerged_depths", "Submerged Depths", "Cinematic & Epic", "Hydrophone soundscapes and abyss frequencies", listOf(Color(0xFF006064), Color(0xFF002171)), Icons.Default.Water, listOf("underwater", "deep", "ocean", "ambient", "sub")),

        // === 8. ROMANCE & SOUL ===
        MoodItem("mood_midnight_romance", "Midnight Romance", "Romance & Soul", "Dim lights, smooth jazz and intimate warmth", listOf(Color(0xFF880E4F), Color(0xFF4A148C)), Icons.Default.Favorite, listOf("romance", "love", "smooth", "jazz", "intimate")),
        MoodItem("mood_candlelight_rnb", "Candlelight R&B", "Romance & Soul", "Silky vocal runs and sweet 808 ballads", listOf(Color(0xFFC2185B), Color(0xFFAD1457)), Icons.Default.VolunteerActivism, listOf("rnb", "vocal", "slowjam", "ballad", "soul")),
        MoodItem("mood_dreamy_slowdance", "Dreamy Slow Dance", "Romance & Soul", "Swinging 6/8 ballads and retro sweet chords", listOf(Color(0xFFF06292), Color(0xFFBA68C8)), Icons.Default.AccessibilityNew, listOf("slowdance", "waltz", "ballad", "sweet", "dreamy")),
        MoodItem("mood_heartstrings", "Heartstrings", "Romance & Soul", "Intimate cello duets and tender piano touch", listOf(Color(0xFFE57373), Color(0xFFD32F2F)), Icons.Default.MusicNote, listOf("strings", "cello", "piano", "tender", "love")),
        MoodItem("mood_neo_soul_warmth", "Neo-Soul Warmth", "Romance & Soul", "Laid-back rimshots, Rhodes chords & deep bass", listOf(Color(0xFFFF8A65), Color(0xFFD84315)), Icons.Default.FavoriteBorder, listOf("neosoul", "soul", "rhodes", "groove", "smooth")),
        MoodItem("mood_stargazing_together", "Stargazing", "Romance & Soul", "Gentle acoustic guitar under a moonlit sky", listOf(Color(0xFF3949AB), Color(0xFF8E24AA)), Icons.Default.Bedtime, listOf("acoustic", "night", "stars", "romantic", "guitar")),
        MoodItem("mood_velvet_kiss", "Velveteen Kiss", "Romance & Soul", "Seductive downtempo and velvet textures", listOf(Color(0xFF6A1B9A), Color(0xFF283593)), Icons.Default.Loyalty, listOf("downtempo", "seductive", "velvet", "chill", "soul")),
        MoodItem("mood_soulmate_harmony", "Soulmate Harmony", "Romance & Soul", "Vocal duets and interlocking melodies", listOf(Color(0xFFFF4081), Color(0xFFFF80AB)), Icons.Default.People, listOf("duet", "harmony", "vocal", "love", "pop")),
        MoodItem("mood_love_letters", "Love Letters", "Romance & Soul", "Unplugged love songs straight from the heart", listOf(Color(0xFFFF5252), Color(0xFFFF7A90)), Icons.Default.Mail, listOf("acoustic", "love", "letter", "singer", "songwriter")),
        MoodItem("mood_french_cafe", "Parisian Romance", "Romance & Soul", "Accordion waltzes and warm sidewalk café charm", listOf(Color(0xFFFF8A80), Color(0xFFFF5252)), Icons.Default.Restaurant, listOf("paris", "accordion", "waltz", "romance", "french")),

        // === 9. NATURE & SPACE ===
        MoodItem("mood_forest_canopy", "Forest Canopy", "Nature & Space", "Bird songs, wooden flutes & rustling leaves", listOf(Color(0xFF2E7D32), Color(0xFF1B5E20)), Icons.Default.Forest, listOf("forest", "nature", "flute", "acoustic", "green")),
        MoodItem("mood_ocean_depths", "Ocean Depths", "Nature & Space", "Rolling tide waves and whale song echoes", listOf(Color(0xFF0288D1), Color(0xFF01579B)), Icons.Default.Sailing, listOf("ocean", "waves", "sea", "ambient", "water")),
        MoodItem("mood_aurora_borealis", "Aurora Borealis", "Nature & Space", "Glimmering arctic synth pads and cold beauty", listOf(Color(0xFF00E676), Color(0xFF00B0FF)), Icons.Default.Brightness7, listOf("aurora", "arctic", "synth", "light", "space")),
        MoodItem("mood_astral_projection", "Astral Projection", "Nature & Space", "Floating cosmic ambient and out-of-body drones", listOf(Color(0xFF7C4DFF), Color(0xFF304FFE)), Icons.Default.BlurCircular, listOf("astral", "drone", "meditation", "ambient", "space")),
        MoodItem("mood_mountain_mist", "Mountain Mist", "Nature & Space", "Echoing Tibetan singing bowls & crisp breezes", listOf(Color(0xFF546E7A), Color(0xFF37474F)), Icons.Default.Terrain, listOf("mountain", "tibetan", "bowls", "mist", "zen")),
        MoodItem("mood_desert_starlight", "Desert Starlight", "Nature & Space", "Campfire oud strums under the Milky Way", listOf(Color(0xFFE65100), Color(0xFFBF360C)), Icons.Default.Star, listOf("desert", "oud", "ambient", "night", "stars")),
        MoodItem("mood_cosmic_drift", "Cosmic Drift", "Nature & Space", "Zero gravity synthesizer floating through nebulae", listOf(Color(0xFF4A148C), Color(0xFF000051)), Icons.Default.Explore, listOf("space", "nebula", "synth", "drift", "ambient")),
        MoodItem("mood_rain_on_leaves", "Rain on Leaves", "Nature & Space", "Gentle forest precipitation and soothing drops", listOf(Color(0xFF00796B), Color(0xFF004D40)), Icons.Default.Grass, listOf("rain", "nature", "sleep", "calm", "relax")),
        MoodItem("mood_waterfall_calm", "Waterfall Sanctuary", "Nature & Space", "Cascading crystal water & peaceful harmony", listOf(Color(0xFF0097A7), Color(0xFF006064)), Icons.Default.Shower, listOf("waterfall", "stream", "nature", "peace", "harmony")),
        MoodItem("mood_solar_flare", "Solar Flare", "Nature & Space", "Radiant analog frequencies and warmth of the sun", listOf(Color(0xFFFF6D00), Color(0xFFFFD600)), Icons.Default.WbSunny, listOf("sun", "solar", "warm", "energy", "ambient")),

        // === 10. UPBEAT & SUNSHINE ===
        MoodItem("mood_pure_joy", "Pure Joy", "Upbeat & Sunshine", "Infectious smiles, brass hooks & feel-good pop", listOf(Color(0xFFFFD600), Color(0xFFFF6D00)), Icons.Default.SentimentVerySatisfied, listOf("happy", "joy", "pop", "smile", "fun")),
        MoodItem("mood_radiant_sunshine", "Radiant Sunshine", "Upbeat & Sunshine", "Ukulele strums, acoustic bounce & blue skies", listOf(Color(0xFFFFEB3B), Color(0xFFFBC02D)), Icons.Default.LightMode, listOf("sun", "ukulele", "acoustic", "happy", "bright")),
        MoodItem("mood_good_vibes_only", "Good Vibes Only", "Upbeat & Sunshine", "Indie pop singalongs and sunny handclaps", listOf(Color(0xFF69F0AE), Color(0xFF00E676)), Icons.Default.ThumbUp, listOf("indie", "pop", "vibes", "positive", "fun")),
        MoodItem("mood_festival_fireworks", "Festival Fireworks", "Upbeat & Sunshine", "Euphoric builds and summer stage memories", listOf(Color(0xFFFF4081), Color(0xFF7C4DFF)), Icons.Default.Celebration, listOf("festival", "summer", "edm", "euphoric", "party")),
        MoodItem("mood_weekend_anthem", "Weekend Anthems", "Upbeat & Sunshine", "Friday 5 PM freedom and window-down tunes", listOf(Color(0xFFFF5252), Color(0xFFFF7A59)), Icons.Default.CarRental, listOf("weekend", "anthem", "rock", "pop", "drive")),
        MoodItem("mood_whistle_while_work", "Whistle While You Work", "Upbeat & Sunshine", "Cheerful melodies making daily chores a breeze", listOf(Color(0xFFFFB300), Color(0xFFFFA000)), Icons.Default.CleaningServices, listOf("cheer", "acoustic", "light", "happy", "breeze")),
        MoodItem("mood_summer_poolside", "Summer Poolside", "Upbeat & Sunshine", "Refreshing drinks, tropical bounce & splash", listOf(Color(0xFF40C4FF), Color(0xFF00B0FF)), Icons.Default.Pool, listOf("pool", "summer", "tropical", "house", "sun")),
        MoodItem("mood_road_trip_singalong", "Road Trip Singalong", "Upbeat & Sunshine", "Classic highway choruses everyone knows", listOf(Color(0xFFFF8F00), Color(0xFFFF6F00)), Icons.Default.Map, listOf("roadtrip", "singalong", "classic", "rock", "highway")),
        MoodItem("mood_breakfast_groove", "Breakfast Groove", "Upbeat & Sunshine", "Upbeat jazz piano & morning coffee smiles", listOf(Color(0xFFFFD54F), Color(0xFFFF8A65)), Icons.Default.BakeryDining, listOf("morning", "jazz", "breakfast", "groove", "sun")),
        MoodItem("mood_celebrate_life", "Celebrate Life", "Upbeat & Sunshine", "High energy brass, drums & uplifting triumph", listOf(Color(0xFFFF1744), Color(0xFFFFD600)), Icons.Default.Celebration, listOf("celebrate", "anthem", "triumph", "joy", "brass"))
    )

    fun filterTracksForMood(mood: MoodItem, allTracks: List<MusicTrack>): List<MusicTrack> {
        if (allTracks.isEmpty()) return emptyList()

        val matching = allTracks.filter { track ->
            val trackText = "${track.title} ${track.artist} ${track.album} ${track.genre}".lowercase()
            mood.keywords.any { kw -> trackText.contains(kw) }
        }

        return if (matching.isNotEmpty()) {
            matching
        } else {
            // Deterministic hash-based subset so every mood gets a consistent, diverse slice of the library
            val hash = mood.id.hashCode()
            val sliceSize = (allTracks.size / 3).coerceIn(4, allTracks.size)
            val startIndex = Math.abs(hash) % allTracks.size
            val list = mutableListOf<MusicTrack>()
            for (i in 0 until sliceSize) {
                list.add(allTracks[(startIndex + i) % allTracks.size])
            }
            list.distinctBy { it.id }
        }
    }
}
