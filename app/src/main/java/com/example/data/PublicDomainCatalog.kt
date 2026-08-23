package com.example.data

import com.example.ui.screens.BookshelfItem

data class PublicDomainMediaItem(
    val id: String,
    val title: String,
    val authorOrCreator: String,
    val type: String, // "BOOK", "AUDIOBOOK", "MUSIC", "COMIC"
    val genre: String,
    val description: String,
    val coverUrl: String,
    val streamOrReadUrl: String,
    val durationSeconds: Long = 0L,
    val totalPages: Int = 150
)

object PublicDomainCatalog {

    val curatedEBooks: List<PublicDomainMediaItem> = listOf(
        // Sci-Fi & Speculative Fiction
        PublicDomainMediaItem(
            id = "gutenberg_35",
            title = "The Time Machine",
            authorOrCreator = "H. G. Wells",
            type = "BOOK",
            genre = "Sci-Fi",
            description = "A Victorian scientist invents a device enabling four-dimensional journeying to 802,701 AD, encountering the gentle Eloi and subterranean Morlocks.",
            coverUrl = "https://www.gutenberg.org/cache/epub/35/pg35.cover.medium.jpg",
            streamOrReadUrl = "https://www.gutenberg.org/cache/epub/35/pg35.txt",
            totalPages = 184
        ),
        PublicDomainMediaItem(
            id = "gutenberg_84",
            title = "Frankenstein",
            authorOrCreator = "Mary Wollstonecraft Shelley",
            type = "BOOK",
            genre = "Sci-Fi",
            description = "Obsessed with creating life, Victor Frankenstein brings an unnatural being to life, with catastrophic consequences exploring science and morality.",
            coverUrl = "https://www.gutenberg.org/cache/epub/84/pg84.cover.medium.jpg",
            streamOrReadUrl = "https://www.gutenberg.org/cache/epub/84/pg84.txt",
            totalPages = 280
        ),
        PublicDomainMediaItem(
            id = "gutenberg_36",
            title = "The War of the Worlds",
            authorOrCreator = "H. G. Wells",
            type = "BOOK",
            genre = "Sci-Fi",
            description = "Martian invaders armed with heat rays and towering tripod fighting machines devastate England in the foundational alien invasion classic.",
            coverUrl = "https://www.gutenberg.org/cache/epub/36/pg36.cover.medium.jpg",
            streamOrReadUrl = "https://www.gutenberg.org/cache/epub/36/pg36.txt",
            totalPages = 287
        ),
        PublicDomainMediaItem(
            id = "gutenberg_164",
            title = "Twenty Thousand Leagues Under the Sea",
            authorOrCreator = "Jules Verne",
            type = "BOOK",
            genre = "Adventure",
            description = "Captain Nemo commands the futuristic submarine Nautilus through uncharted ocean depths in a visionary masterpiece of science and adventure.",
            coverUrl = "https://www.gutenberg.org/cache/epub/164/pg164.cover.medium.jpg",
            streamOrReadUrl = "https://www.gutenberg.org/cache/epub/164/pg164.txt",
            totalPages = 440
        ),
        PublicDomainMediaItem(
            id = "gutenberg_5200",
            title = "The Metamorphosis",
            authorOrCreator = "Franz Kafka",
            type = "BOOK",
            genre = "Philosophy",
            description = "Gregor Samsa awakens one morning to find himself inexplicably transformed into a monstrous insect, examining alienation and family duty.",
            coverUrl = "https://www.gutenberg.org/cache/epub/5200/pg5200.cover.medium.jpg",
            streamOrReadUrl = "https://www.gutenberg.org/cache/epub/5200/pg5200.txt",
            totalPages = 120
        ),
        PublicDomainMediaItem(
            id = "gutenberg_132",
            title = "The Art of War",
            authorOrCreator = "Sun Tzu",
            type = "BOOK",
            genre = "Philosophy",
            description = "Ancient Chinese strategic military text focusing on strategy, diplomacy, psychology, and achieving victory with minimal conflict.",
            coverUrl = "https://www.gutenberg.org/cache/epub/132/pg132.cover.medium.jpg",
            streamOrReadUrl = "https://www.gutenberg.org/cache/epub/132/pg132.txt",
            totalPages = 112
        ),
        PublicDomainMediaItem(
            id = "gutenberg_64317",
            title = "The Great Gatsby",
            authorOrCreator = "F. Scott Fitzgerald",
            type = "BOOK",
            genre = "Classics",
            description = "Set in the Roaring Twenties on Long Island, exploring themes of decadent wealth, unrequited love, and the elusive American Dream.",
            coverUrl = "https://www.gutenberg.org/cache/epub/64317/pg64317.cover.medium.jpg",
            streamOrReadUrl = "https://www.gutenberg.org/cache/epub/64317/pg64317.txt",
            totalPages = 192
        ),
        PublicDomainMediaItem(
            id = "gutenberg_1342",
            title = "Pride and Prejudice",
            authorOrCreator = "Jane Austen",
            type = "BOOK",
            genre = "Classics",
            description = "Elizabeth Bennet navigates manners, upbringing, morality, and marriage in the society of the landed gentry of Regency-era Britain.",
            coverUrl = "https://www.gutenberg.org/cache/epub/1342/pg1342.cover.medium.jpg",
            streamOrReadUrl = "https://www.gutenberg.org/cache/epub/1342/pg1342.txt",
            totalPages = 432
        ),
        PublicDomainMediaItem(
            id = "gutenberg_11",
            title = "Alice's Adventures in Wonderland",
            authorOrCreator = "Lewis Carroll",
            type = "BOOK",
            genre = "Fantasy",
            description = "Alice falls down a rabbit hole into a whimsical, surreal world populated by peculiar anthropomorphic creatures and logic puzzles.",
            coverUrl = "https://www.gutenberg.org/cache/epub/11/pg11.cover.medium.jpg",
            streamOrReadUrl = "https://www.gutenberg.org/cache/epub/11/pg11.txt",
            totalPages = 176
        ),
        PublicDomainMediaItem(
            id = "gutenberg_1661",
            title = "The Adventures of Sherlock Holmes",
            authorOrCreator = "Sir Arthur Conan Doyle",
            type = "BOOK",
            genre = "Mystery",
            description = "Twelve ingenious detective stories starring Baker Street consulting detective Sherlock Holmes and Dr. John Watson.",
            coverUrl = "https://www.gutenberg.org/cache/epub/1661/pg1661.cover.medium.jpg",
            streamOrReadUrl = "https://www.gutenberg.org/cache/epub/1661/pg1661.txt",
            totalPages = 307
        ),
        PublicDomainMediaItem(
            id = "gutenberg_345",
            title = "Dracula",
            authorOrCreator = "Bram Stoker",
            type = "BOOK",
            genre = "Horror",
            description = "Count Dracula's dark attempt to move from Transylvania to Victorian England, battling Professor Abraham Van Helsing and Jonathan Harker.",
            coverUrl = "https://www.gutenberg.org/cache/epub/345/pg345.cover.medium.jpg",
            streamOrReadUrl = "https://www.gutenberg.org/cache/epub/345/pg345.txt",
            totalPages = 488
        ),
        PublicDomainMediaItem(
            id = "gutenberg_2701",
            title = "Moby Dick; or The Whale",
            authorOrCreator = "Herman Melville",
            type = "BOOK",
            genre = "Adventure",
            description = "Sailor Ishmael narrates the obsessive quest of Captain Ahab for revenge against the giant albino sperm whale, Moby Dick.",
            coverUrl = "https://www.gutenberg.org/cache/epub/2701/pg2701.cover.medium.jpg",
            streamOrReadUrl = "https://www.gutenberg.org/cache/epub/2701/pg2701.txt",
            totalPages = 635
        ),
        PublicDomainMediaItem(
            id = "gutenberg_174",
            title = "The Picture of Dorian Gray",
            authorOrCreator = "Oscar Wilde",
            type = "BOOK",
            genre = "Classics",
            description = "A decadent young man trades his soul for eternal youth while an oil portrait ages and captures the corruption of his sins.",
            coverUrl = "https://www.gutenberg.org/cache/epub/174/pg174.cover.medium.jpg",
            streamOrReadUrl = "https://www.gutenberg.org/cache/epub/174/pg174.txt",
            totalPages = 254
        ),
        PublicDomainMediaItem(
            id = "gutenberg_2680",
            title = "Meditations",
            authorOrCreator = "Marcus Aurelius",
            type = "BOOK",
            genre = "Philosophy",
            description = "Personal Stoic reflections written by Roman Emperor Marcus Aurelius on discipline, duty, self-restraint, and tranquility.",
            coverUrl = "https://www.gutenberg.org/cache/epub/2680/pg2680.cover.medium.jpg",
            streamOrReadUrl = "https://www.gutenberg.org/cache/epub/2680/pg2680.txt",
            totalPages = 160
        ),
        PublicDomainMediaItem(
            id = "gutenberg_1232",
            title = "The Prince",
            authorOrCreator = "Niccolò Machiavelli",
            type = "BOOK",
            genre = "Philosophy",
            description = "16th-century political treatise on statecraft, political power, pragmatism, and ruling strategy in Renaissance Florence.",
            coverUrl = "https://www.gutenberg.org/cache/epub/1232/pg1232.cover.medium.jpg",
            streamOrReadUrl = "https://www.gutenberg.org/cache/epub/1232/pg1232.txt",
            totalPages = 140
        ),
        PublicDomainMediaItem(
            id = "gutenberg_2554",
            title = "Crime and Punishment",
            authorOrCreator = "Fyodor Dostoevsky",
            type = "BOOK",
            genre = "Classics",
            description = "Impoverished student Rodion Raskolnikov executes a calculated murder in Saint Petersburg, followed by intense psychological turmoil and redemption.",
            coverUrl = "https://www.gutenberg.org/cache/epub/2554/pg2554.cover.medium.jpg",
            streamOrReadUrl = "https://www.gutenberg.org/cache/epub/2554/pg2554.txt",
            totalPages = 560
        ),
        PublicDomainMediaItem(
            id = "gutenberg_1260",
            title = "Jane Eyre",
            authorOrCreator = "Charlotte Brontë",
            type = "BOOK",
            genre = "Classics",
            description = "An orphaned governess discovers deep secrets at Thornfield Hall and falls in love with the brooding Mr. Rochester.",
            coverUrl = "https://www.gutenberg.org/cache/epub/1260/pg1260.cover.medium.jpg",
            streamOrReadUrl = "https://www.gutenberg.org/cache/epub/1260/pg1260.txt",
            totalPages = 500
        ),
        PublicDomainMediaItem(
            id = "gutenberg_768",
            title = "Wuthering Heights",
            authorOrCreator = "Emily Brontë",
            type = "BOOK",
            genre = "Classics",
            description = "A tempestuous tale of obsessive, destructive love between Heathcliff and Catherine Earnshaw on the windswept Yorkshire moors.",
            coverUrl = "https://www.gutenberg.org/cache/epub/768/pg768.cover.medium.jpg",
            streamOrReadUrl = "https://www.gutenberg.org/cache/epub/768/pg768.txt",
            totalPages = 416
        ),
        PublicDomainMediaItem(
            id = "gutenberg_43",
            title = "The Strange Case of Dr. Jekyll and Mr. Hyde",
            authorOrCreator = "Robert Louis Stevenson",
            type = "BOOK",
            genre = "Horror",
            description = "A respected London doctor invents a potion that unleashes his evil, hedonistic alter ego, Edward Hyde.",
            coverUrl = "https://www.gutenberg.org/cache/epub/43/pg43.cover.medium.jpg",
            streamOrReadUrl = "https://www.gutenberg.org/cache/epub/43/pg43.txt",
            totalPages = 144
        ),
        PublicDomainMediaItem(
            id = "gutenberg_120",
            title = "Treasure Island",
            authorOrCreator = "Robert Louis Stevenson",
            type = "BOOK",
            genre = "Adventure",
            description = "Young Jim Hawkins embarks on a high-seas expedition in search of buried pirate gold, facing the cunning Long John Silver.",
            coverUrl = "https://www.gutenberg.org/cache/epub/120/pg120.cover.medium.jpg",
            streamOrReadUrl = "https://www.gutenberg.org/cache/epub/120/pg120.txt",
            totalPages = 292
        ),
        PublicDomainMediaItem(
            id = "gutenberg_215",
            title = "The Call of the Wild",
            authorOrCreator = "Jack London",
            type = "BOOK",
            genre = "Adventure",
            description = "Buck, a domesticated St. Bernard mix, is stolen and sold into the harsh Yukon gold rush wilderness, reclaiming his primitive instincts.",
            coverUrl = "https://www.gutenberg.org/cache/epub/215/pg215.cover.medium.jpg",
            streamOrReadUrl = "https://www.gutenberg.org/cache/epub/215/pg215.txt",
            totalPages = 172
        ),
        PublicDomainMediaItem(
            id = "gutenberg_1952",
            title = "The Yellow Wallpaper",
            authorOrCreator = "Charlotte Perkins Gilman",
            type = "BOOK",
            genre = "Horror",
            description = "A haunting semi-autobiographical novella detailing a woman's psychological descent during a mandatory 'rest cure' in an isolated estate.",
            coverUrl = "https://www.gutenberg.org/cache/epub/1952/pg1952.cover.medium.jpg",
            streamOrReadUrl = "https://www.gutenberg.org/cache/epub/1952/pg1952.txt",
            totalPages = 64
        ),
        PublicDomainMediaItem(
            id = "gutenberg_1727",
            title = "The Odyssey",
            authorOrCreator = "Homer",
            type = "BOOK",
            genre = "Epics",
            description = "The epic ten-year journey of Greek hero Odysseus striving to return home to Ithaca after the fall of Troy, battling monsters and gods.",
            coverUrl = "https://www.gutenberg.org/cache/epub/1727/pg1727.cover.medium.jpg",
            streamOrReadUrl = "https://www.gutenberg.org/cache/epub/1727/pg1727.txt",
            totalPages = 384
        ),
        PublicDomainMediaItem(
            id = "gutenberg_6130",
            title = "The Iliad",
            authorOrCreator = "Homer",
            type = "BOOK",
            genre = "Epics",
            description = "The wrath of Achilles and the fateful final weeks of the Trojan War, highlighting heroism, honor, and destiny.",
            coverUrl = "https://www.gutenberg.org/cache/epub/6130/pg6130.cover.medium.jpg",
            streamOrReadUrl = "https://www.gutenberg.org/cache/epub/6130/pg6130.txt",
            totalPages = 460
        ),
        PublicDomainMediaItem(
            id = "gutenberg_98",
            title = "A Tale of Two Cities",
            authorOrCreator = "Charles Dickens",
            type = "BOOK",
            genre = "Classics",
            description = "Set in London and Paris during the Reign of Terror of the French Revolution, exploring love, tyranny, and ultimate sacrifice.",
            coverUrl = "https://www.gutenberg.org/cache/epub/98/pg98.cover.medium.jpg",
            streamOrReadUrl = "https://www.gutenberg.org/cache/epub/98/pg98.txt",
            totalPages = 448
        ),
        PublicDomainMediaItem(
            id = "gutenberg_74",
            title = "The Adventures of Tom Sawyer",
            authorOrCreator = "Mark Twain",
            type = "BOOK",
            genre = "Classics",
            description = "The mischievous escapades of young Tom Sawyer growing up along the Mississippi River in 1840s St. Petersburg, Missouri.",
            coverUrl = "https://www.gutenberg.org/cache/epub/74/pg74.cover.medium.jpg",
            streamOrReadUrl = "https://www.gutenberg.org/cache/epub/74/pg74.txt",
            totalPages = 274
        ),
        PublicDomainMediaItem(
            id = "gutenberg_76",
            title = "Adventures of Huckleberry Finn",
            authorOrCreator = "Mark Twain",
            type = "BOOK",
            genre = "Classics",
            description = "Huck Finn and runaway slave Jim raft down the mighty Mississippi River in search of freedom and moral integrity.",
            coverUrl = "https://www.gutenberg.org/cache/epub/76/pg76.cover.medium.jpg",
            streamOrReadUrl = "https://www.gutenberg.org/cache/epub/76/pg76.txt",
            totalPages = 366
        ),
        PublicDomainMediaItem(
            id = "gutenberg_28054",
            title = "The Brothers Karamazov",
            authorOrCreator = "Fyodor Dostoevsky",
            type = "BOOK",
            genre = "Philosophy",
            description = "A profound philosophical drama exploring faith, free will, morality, and family conflict following the murder of patriarch Fyodor Karamazov.",
            coverUrl = "https://www.gutenberg.org/cache/epub/28054/pg28054.cover.medium.jpg",
            streamOrReadUrl = "https://www.gutenberg.org/cache/epub/28054/pg28054.txt",
            totalPages = 824
        ),
        PublicDomainMediaItem(
            id = "gutenberg_514",
            title = "Little Women",
            authorOrCreator = "Louisa May Alcott",
            type = "BOOK",
            genre = "Classics",
            description = "The heartwarming lives, artistic aspirations, and trials of the four March sisters—Meg, Jo, Beth, and Amy—in 19th-century New England.",
            coverUrl = "https://www.gutenberg.org/cache/epub/514/pg514.cover.medium.jpg",
            streamOrReadUrl = "https://www.gutenberg.org/cache/epub/514/pg514.txt",
            totalPages = 504
        ),
        PublicDomainMediaItem(
            id = "gutenberg_236",
            title = "The Jungle Book",
            authorOrCreator = "Rudyard Kipling",
            type = "BOOK",
            genre = "Adventure",
            description = "The classic fables of feral child Mowgli raised by wolves in the Seoni jungle, guided by Baloo the bear and Bagheera the black panther.",
            coverUrl = "https://www.gutenberg.org/cache/epub/236/pg236.cover.medium.jpg",
            streamOrReadUrl = "https://www.gutenberg.org/cache/epub/236/pg236.txt",
            totalPages = 277
        )
    )

    val curatedComics: List<PublicDomainMediaItem> = listOf(
        PublicDomainMediaItem(
            id = "comic_nemo_1",
            title = "Little Nemo in Slumberland",
            authorOrCreator = "Winsor McCay",
            type = "COMIC",
            genre = "Comics",
            description = "Landmark weekly comic strip following young Nemo's dazzling, surreal dream adventures in the fantastical kingdom of King Morpheus.",
            coverUrl = "https://archive.org/services/img/LittleNemoInSlumberland1905",
            streamOrReadUrl = "https://archive.org/download/LittleNemoInSlumberland1905/nemo.cbz",
            totalPages = 48
        ),
        PublicDomainMediaItem(
            id = "comic_krazy_kat",
            title = "Krazy Kat Classics",
            authorOrCreator = "George Herriman",
            type = "COMIC",
            genre = "Comics",
            description = "Acclaimed avant-garde comic strip set in Coconino County depicting the poetic triangular relationship between Krazy Kat, Ignatz Mouse, and Offissa Pupp.",
            coverUrl = "https://archive.org/services/img/krazykatclassics",
            streamOrReadUrl = "https://archive.org/download/krazykatclassics/krazy.cbz",
            totalPages = 52
        ),
        PublicDomainMediaItem(
            id = "comic_planet_1",
            title = "Planet Comics: Cosmic Patrol #1",
            authorOrCreator = "Fiction House • Golden Age",
            type = "COMIC",
            genre = "Comics",
            description = "Vintage 1940s golden age space opera comic featuring rocket heroes, alien worlds, rayguns, and interplanetary exploration.",
            coverUrl = "https://archive.org/services/img/planet_comics_01",
            streamOrReadUrl = "https://archive.org/download/planet_comics_01/planet.cbz",
            totalPages = 64
        ),
        PublicDomainMediaItem(
            id = "comic_captain_marvel",
            title = "Whiz Comics: Origin of Captain Marvel",
            authorOrCreator = "Bill Parker • C.C. Beck",
            type = "COMIC",
            genre = "Comics",
            description = "The historic golden age debut of Billy Batson receiving the magical powers of Shazam to protect Fawcett City.",
            coverUrl = "https://archive.org/services/img/whizcomics02",
            streamOrReadUrl = "https://archive.org/download/whizcomics02/whiz.cbz",
            totalPages = 68
        )
    )

    val curatedAudiobooks: List<PublicDomainMediaItem> = listOf(
        PublicDomainMediaItem(
            id = "audiobook_sherlock_holmes",
            title = "The Adventures of Sherlock Holmes",
            authorOrCreator = "Sir Arthur Conan Doyle",
            type = "AUDIOBOOK",
            genre = "Mystery",
            description = "LibriVox full dramatized voice recording of the twelve classic detective stories of Sherlock Holmes and Dr. Watson.",
            coverUrl = "https://archive.org/services/img/sherlock_holmes_adventures_64kb_librivox",
            streamOrReadUrl = "https://archive.org/download/sherlock_holmes_adventures_64kb_librivox/sherlock_holmes_adventures_01_doyle_64kb.mp3",
            durationSeconds = 3840L
        ),
        PublicDomainMediaItem(
            id = "audiobook_dracula_librivox",
            title = "Dracula",
            authorOrCreator = "Bram Stoker",
            type = "AUDIOBOOK",
            genre = "Horror",
            description = "Unabridged dramatic reading of Bram Stoker's gothic masterpiece detailing the hunt for the terrifying vampire Count Dracula.",
            coverUrl = "https://archive.org/services/img/dracula_librivox",
            streamOrReadUrl = "https://archive.org/download/dracula_librivox/dracula_01_stoker_64kb.mp3",
            durationSeconds = 5400L
        ),
        PublicDomainMediaItem(
            id = "audiobook_pride_prejudice",
            title = "Pride and Prejudice",
            authorOrCreator = "Jane Austen",
            type = "AUDIOBOOK",
            genre = "Classics",
            description = "Full audio narration of Jane Austen's beloved romantic classic featuring Elizabeth Bennet and Mr. Darcy.",
            coverUrl = "https://archive.org/services/img/pride_and_prejudice_librivox",
            streamOrReadUrl = "https://archive.org/download/pride_and_prejudice_librivox/prideandprejudice_01_austen_64kb.mp3",
            durationSeconds = 4800L
        ),
        PublicDomainMediaItem(
            id = "audiobook_art_of_war",
            title = "The Art of War",
            authorOrCreator = "Sun Tzu",
            type = "AUDIOBOOK",
            genre = "Philosophy",
            description = "Unabridged audio performance of Sun Tzu's quintessential work on tactics, statecraft, and human nature.",
            coverUrl = "https://archive.org/services/img/art_of_war_librivox",
            streamOrReadUrl = "https://archive.org/download/art_of_war_librivox/artofwar_01_suntzu_64kb.mp3",
            durationSeconds = 2400L
        ),
        PublicDomainMediaItem(
            id = "audiobook_frankenstein",
            title = "Frankenstein; or The Modern Prometheus",
            authorOrCreator = "Mary Wollstonecraft Shelley",
            type = "AUDIOBOOK",
            genre = "Sci-Fi",
            description = "Full dramatic audio performance of Victor Frankenstein and his tragic, sentient creature.",
            coverUrl = "https://archive.org/services/img/frankenstein_librivox",
            streamOrReadUrl = "https://archive.org/download/frankenstein_librivox/frankenstein_01_shelley_64kb.mp3",
            durationSeconds = 4200L
        ),
        PublicDomainMediaItem(
            id = "audiobook_time_machine",
            title = "The Time Machine",
            authorOrCreator = "H. G. Wells",
            type = "AUDIOBOOK",
            genre = "Sci-Fi",
            description = "Engaging audio recording following the Time Traveller's journey into the far future.",
            coverUrl = "https://archive.org/services/img/time_machine_librivox",
            streamOrReadUrl = "https://archive.org/download/time_machine_librivox/timemachine_01_wells_64kb.mp3",
            durationSeconds = 3600L
        ),
        PublicDomainMediaItem(
            id = "audiobook_alice_wonderland",
            title = "Alice's Adventures in Wonderland",
            authorOrCreator = "Lewis Carroll",
            type = "AUDIOBOOK",
            genre = "Fantasy",
            description = "Charming voice reading of Alice's journey down the rabbit hole to meet the Cheshire Cat and the Queen of Hearts.",
            coverUrl = "https://archive.org/services/img/alice_in_wonderland_librivox",
            streamOrReadUrl = "https://archive.org/download/alice_in_wonderland_librivox/alice_01_carroll_64kb.mp3",
            durationSeconds = 3100L
        ),
        PublicDomainMediaItem(
            id = "audiobook_great_gatsby",
            title = "The Great Gatsby",
            authorOrCreator = "F. Scott Fitzgerald",
            type = "AUDIOBOOK",
            genre = "Classics",
            description = "Full-length narration of Jay Gatsby and Nick Carraway in 1920s New York.",
            coverUrl = "https://archive.org/services/img/great_gatsby_librivox",
            streamOrReadUrl = "https://archive.org/download/great_gatsby_librivox/gatsby_01_fitzgerald_64kb.mp3",
            durationSeconds = 4500L
        )
    )

    val curatedMusic: List<PublicDomainMediaItem> = listOf(
        PublicDomainMediaItem(
            id = "music_beethoven_symphony_5",
            title = "Symphony No. 5 in C Minor, Op. 67",
            authorOrCreator = "Ludwig van Beethoven",
            type = "MUSIC",
            genre = "Classical",
            description = "One of the most recognized and influential classical compositions in history, famous for its distinctive four-note opening motif.",
            coverUrl = "https://archive.org/services/img/beethoven_symphony_5_musopen",
            streamOrReadUrl = "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-__Fly_Paper.mp3",
            durationSeconds = 440L
        ),
        PublicDomainMediaItem(
            id = "music_clair_de_lune",
            title = "Clair de Lune (Suite Bergamasque)",
            authorOrCreator = "Claude Debussy",
            type = "MUSIC",
            genre = "Classical",
            description = "Exquisite impressionist piano masterwork evoking gentle moonlight and shimmering reflections.",
            coverUrl = "https://archive.org/services/img/debussy_clair_de_lune",
            streamOrReadUrl = "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-__Fly_Paper.mp3",
            durationSeconds = 310L
        ),
        PublicDomainMediaItem(
            id = "music_four_seasons_spring",
            title = "The Four Seasons: Spring (La Primavera)",
            authorOrCreator = "Antonio Vivaldi",
            type = "MUSIC",
            genre = "Classical",
            description = "Vibrant Baroque violin concerto capturing birdsong, murmuring streams, and gentle spring breezes.",
            coverUrl = "https://archive.org/services/img/vivaldi_four_seasons_spring",
            streamOrReadUrl = "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-__Fly_Paper.mp3",
            durationSeconds = 215L
        ),
        PublicDomainMediaItem(
            id = "music_chopin_nocturne",
            title = "Nocturne in E-Flat Major, Op. 9, No. 2",
            authorOrCreator = "Frédéric Chopin",
            type = "MUSIC",
            genre = "Classical",
            description = "Romantic piano poetry celebrated for its graceful cantabile melody and ornamental coloratura.",
            coverUrl = "https://archive.org/services/img/chopin_nocturne_op9_no2",
            streamOrReadUrl = "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-__Fly_Paper.mp3",
            durationSeconds = 270L
        ),
        PublicDomainMediaItem(
            id = "music_scott_joplin_maple_leaf",
            title = "Maple Leaf Rag",
            authorOrCreator = "Scott Joplin",
            type = "MUSIC",
            genre = "Jazz & Ragtime",
            description = "Iconic 1899 ragtime piano masterpiece that defined the American syncopated musical movement.",
            coverUrl = "https://archive.org/services/img/scott_joplin_maple_leaf_rag",
            streamOrReadUrl = "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-__Fly_Paper.mp3",
            durationSeconds = 195L
        ),
        PublicDomainMediaItem(
            id = "music_mozart_eine_kleine",
            title = "Eine kleine Nachtmusik, K. 525",
            authorOrCreator = "Wolfgang Amadeus Mozart",
            type = "MUSIC",
            genre = "Classical",
            description = "Serenade for string quartet and double bass, renowned for its radiant elegance and joyful harmonic structure.",
            coverUrl = "https://archive.org/services/img/mozart_eine_kleine_nachtmusik",
            streamOrReadUrl = "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-__Fly_Paper.mp3",
            durationSeconds = 345L
        ),
        PublicDomainMediaItem(
            id = "music_bach_air_on_g_string",
            title = "Air on the G String (Orchestral Suite No. 3)",
            authorOrCreator = "Johann Sebastian Bach",
            type = "MUSIC",
            genre = "Classical",
            description = "Sublime Baroque movement creating a serene, timeless atmosphere through sustained melodic lines.",
            coverUrl = "https://archive.org/services/img/bach_air_on_g_string",
            streamOrReadUrl = "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-__Fly_Paper.mp3",
            durationSeconds = 290L
        )
    )

    fun getAllPublicDomainBookshelfItems(): List<BookshelfItem> {
        val list = mutableListOf<BookshelfItem>()
        (curatedEBooks + curatedComics).forEach { item ->
            list.add(
                BookshelfItem(
                    id = item.id,
                    title = item.title,
                    authorOrArtist = item.authorOrCreator,
                    coverUrl = item.coverUrl,
                    genre = item.genre,
                    isComic = item.type == "COMIC",
                    progressPercent = 0,
                    pageCount = item.totalPages,
                    description = item.description,
                    publicDomainUrl = item.streamOrReadUrl
                )
            )
        }
        return list
    }
}
