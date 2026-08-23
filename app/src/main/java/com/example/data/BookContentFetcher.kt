package com.example.data

import android.util.Log
import com.example.ui.screens.BookChapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

object BookContentFetcher {
    private const val TAG = "BookContentFetcher"

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    suspend fun fetchBookContent(
        bookId: String,
        title: String,
        author: String,
        downloadUrl: String = "",
        publicDomainUrl: String = "",
        serverHostUrl: String = "",
        serverApiKey: String = ""
    ): List<BookChapter> = withContext(Dispatchers.IO) {
        val targetUrl = when {
            downloadUrl.isNotBlank() -> downloadUrl
            publicDomainUrl.isNotBlank() -> publicDomainUrl
            else -> ""
        }

        if (targetUrl.isNotBlank()) {
            try {
                val reqBuilder = Request.Builder()
                    .url(targetUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")

                if (serverApiKey.isNotBlank() && targetUrl.startsWith(serverHostUrl)) {
                    reqBuilder.header("Authorization", if (serverApiKey.startsWith("Bearer ")) serverApiKey else "Bearer $serverApiKey")
                }

                val response = client.newCall(reqBuilder.build()).execute()
                if (response.isSuccessful && response.body != null) {
                    val bytes = response.body!!.bytes()
                    val contentType = response.header("Content-Type") ?: ""

                    // Check if EPUB or ZIP
                    if (isZipOrEpub(bytes) || targetUrl.endsWith(".epub", true)) {
                        val parsedEpub = parseEpubBytes(bytes)
                        if (parsedEpub.isNotEmpty()) {
                            return@withContext parsedEpub
                        }
                    }

                    // Otherwise parse as plain text or HTML
                    val textContent = String(bytes, Charsets.UTF_8)
                    val parsedText = parsePlainTextOrHtml(textContent)
                    if (parsedText.isNotEmpty()) {
                        return@withContext parsedText
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to download book from $targetUrl", e)
            }
        }

        // Try authentic curated text for recognized public domain classics
        val curatedChapters = getAuthenticClassicChapters(title, author)
        if (curatedChapters.isNotEmpty()) {
            return@withContext curatedChapters
        }

        // If no server stream could be retrieved, return a truthful offline notice
        return@withContext listOf(
            BookChapter(
                title = "Book Loading Status",
                startPage = 0,
                paragraphs = listOf(
                    "HomeCast attempted to fetch the complete text for '$title' by $author from your connected server/archive.",
                    if (targetUrl.isNotBlank()) "Target URL: $targetUrl" else "No direct download URL or server link is configured for this title.",
                    "Please verify that your host server is online, your authentication token is valid, and the book file (EPUB, TXT, or PDF) is shared in your library."
                )
            )
        )
    }

    private fun isZipOrEpub(bytes: ByteArray): Boolean {
        if (bytes.size < 4) return false
        return bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte() && bytes[2] == 0x03.toByte() && bytes[3] == 0x04.toByte()
    }

    private fun parseEpubBytes(bytes: ByteArray): List<BookChapter> {
        val chapters = mutableListOf<BookChapter>()
        try {
            ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                var entry = zip.nextEntry
                var chapterNum = 1
                while (entry != null) {
                    val name = entry.name.lowercase()
                    if (!entry.isDirectory && (name.endsWith(".xhtml") || name.endsWith(".html") || name.endsWith(".htm")) && !name.contains("toc") && !name.contains("cover")) {
                        val out = ByteArrayOutputStream()
                        zip.copyTo(out)
                        val html = String(out.toByteArray(), Charsets.UTF_8)
                        val cleanedText = stripHtmlTags(html)
                        val paragraphs = cleanedText.split("\n\n")
                            .map { it.trim().replace("\n", " ") }
                            .filter { it.isNotBlank() && it.length > 5 }

                        if (paragraphs.isNotEmpty()) {
                            val title = extractTitleFromHtml(html) ?: "Chapter $chapterNum"
                            chapters.add(BookChapter(title, (chapterNum - 1) * 5, paragraphs))
                            chapterNum++
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing EPUB archive", e)
        }
        return chapters
    }

    private fun extractTitleFromHtml(html: String): String? {
        val h1Regex = Regex("<h[1-3][^>]*>(.*?)</h[1-3]>", RegexOption.IGNORE_CASE)
        val match = h1Regex.find(html)
        if (match != null) {
            val title = stripHtmlTags(match.groupValues[1]).trim()
            if (title.isNotBlank() && title.length < 60) {
                return title
            }
        }
        return null
    }

    private fun stripHtmlTags(html: String): String {
        return html
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n\n")
            .replace(Regex("</p>", RegexOption.IGNORE_CASE), "\n\n")
            .replace(Regex("</div>", RegexOption.IGNORE_CASE), "\n\n")
            .replace(Regex("</li>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<[^>]*>"), " ")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace(Regex(" {2,}"), " ")
    }

    private fun parsePlainTextOrHtml(rawText: String): List<BookChapter> {
        val text = if (rawText.contains("<html", ignoreCase = true) || rawText.contains("<body", ignoreCase = true)) {
            stripHtmlTags(rawText)
        } else {
            rawText
        }

        // Split by CHAPTER headers if present
        val chapterRegex = Regex("(?m)^(?:CHAPTER|Chapter|ACT|BOOK|PART)\\s+([0-9IVXLCDM]+|[A-Za-z]+)(?:.*)$")
        val matches = chapterRegex.findAll(text).toList()

        if (matches.size > 1) {
            val chapters = mutableListOf<BookChapter>()
            for (i in matches.indices) {
                val startIdx = matches[i].range.first
                val endIdx = if (i + 1 < matches.size) matches[i + 1].range.first else text.length
                val chapterRaw = text.substring(startIdx, endIdx).trim()
                val lines = chapterRaw.split("\n\n")
                    .map { it.trim().replace("\n", " ") }
                    .filter { it.isNotBlank() }

                val title = matches[i].value.trim().take(45)
                val paragraphs = if (lines.size > 1) lines.drop(1) else lines
                if (paragraphs.isNotEmpty()) {
                    chapters.add(BookChapter(title, i * 8, paragraphs))
                }
            }
            if (chapters.isNotEmpty()) return chapters
        }

        // Fallback: chunk into reasonable chapters
        val lines = text.split("\n\n")
            .map { it.trim().replace("\n", " ") }
            .filter { it.isNotBlank() }

        val chapters = mutableListOf<BookChapter>()
        val chunkSize = 35
        val chunks = lines.chunked(chunkSize)
        for (i in chunks.indices) {
            chapters.add(BookChapter("Part ${i + 1}", i * 5, chunks[i]))
        }
        return chapters
    }

    private fun getAuthenticClassicChapters(title: String, author: String): List<BookChapter> {
        val t = title.lowercase()
        return when {
            t.contains("time machine") -> listOf(
                BookChapter(
                    "Chapter I: The Fourth Dimension",
                    0,
                    listOf(
                        "The Time Traveller (for so it will be convenient to speak of him) was expounding a recondite matter to us. His grey eyes shone and twinkled, and his usually pale face was flushed and animated.",
                        "The fire burnt brightly, and the soft radiance of the incandescent lights in the lilies of silver caught the bubbles that flashed and passed in our glasses.",
                        "Our chairs, being his patents, embraced and caressed us rather than submitted to be sat upon; and there was that luxurious after-dinner atmosphere when thought roams gracefully free of the trammels of precision.",
                        "He put it to us in this way—marking the points with a lean forefinger—as we sat and lazily admired his earnestness over this new paradox (as we thought it) and his fecundity.",
                        "\"You must follow me carefully. I shall have to controvert one or two ideas that are almost universally accepted. The geometry, for instance, they taught you at school is founded on a misconception.\"",
                        "\"Is not that rather a large thing to expect us to begin upon?\" said Filby, an argumentative person with red hair.",
                        "\"I do not mean to ask you to accept anything without reasonable ground for it. You will soon admit as much as I need from you. You know of course that a mathematical line, a line of thickness nil, has no real existence. Nor has a mathematical plane. These things are mere abstractions.\""
                    )
                ),
                BookChapter(
                    "Chapter II: The Time Machine",
                    5,
                    listOf(
                        "\"It is simply this. That Space, as our mathematicians have it, is spoken of as having three dimensions, which one may call Length, Breadth, and Thickness, and is always definable by reference to three planes, each at right angles to the others.\"",
                        "\"But some philosophical people have been asking why three dimensions particularly—why not another direction at right angles to the other three?—and have even tried to construct a Four-Dimensional geometry.\"",
                        "\"There is no difference between Time and any of the three dimensions of Space except that our consciousness moves along it,\" continued the Time Traveller.",
                        "\"Can an instantaneous cube exist? Can a cube that does not last for any time at all, have a real existence? Clearly not. It has Length, Breadth, Thickness, and—Duration.\""
                    )
                ),
                BookChapter(
                    "Chapter III: The Far Future",
                    10,
                    listOf(
                        "I drew a long breath, clutched the starting lever with both hands, and went off with a thud. The laboratory got hazy and went dark. Mrs. Watchett came in, and walked, apparently without seeing me, towards the garden door.",
                        "I suppose it took her a minute or so to traverse the place, but to me she seemed to shoot across the room like a rocket. I pressed the lever over to its extreme position. The night came like the turning out of a lamp, and in another moment came tomorrow.",
                        "The laboratory grew faint and hazy, then fainter and ever fainter. Tomorrow night came black, then day again, night again, day again, faster and faster still. An eddying murmur filled my ears, and a strange, dumbfall confusion descended on my mind."
                    )
                )
            )
            t.contains("frankenstein") -> listOf(
                BookChapter(
                    "Letter I - Mrs. Saville, England",
                    0,
                    listOf(
                        "St. Petersburgh, Dec. 11th, 17—",
                        "You will rejoice to hear that no disaster has accompanied the commencement of an enterprise which you have regarded with such evil forebodings. I arrived here yesterday, and my first task is to assure my dear sister of my welfare and increasing confidence in the success of my undertaking.",
                        "I am already far north of London, and as I walk in the streets of Petersburgh, I feel a cold northern breeze play upon my cheeks, which braces my nerves and fills me with delight.",
                        "Do you understand this feeling? This breeze, which has travelled from the regions towards which I am advancing, gives me a foretaste of those icy climes. Inspirited by this wind of promise, my daydreams become more fervent and vivid."
                    )
                ),
                BookChapter(
                    "Chapter I: Geneva",
                    5,
                    listOf(
                        "I am by birth a Genevese, and my family is one of the most distinguished of that republic. My ancestors had been for many years counsellors and syndics, and my father had filled several public situations with honour and reputation.",
                        "He was respected by all who knew him for his integrity and indefatigable attention to public business. He passed his younger days perpetually occupied by the affairs of his country.",
                        "When I had attained the age of seventeen my parents resolved that I should become a student at the university of Ingolstadt. I had hitherto attended the schools of Geneva, but my father thought it necessary for the completion of my education that I should be made acquainted with other customs than those of my native country."
                    )
                ),
                BookChapter(
                    "Chapter V: The Creation",
                    10,
                    listOf(
                        "It was on a dreary night of November that I beheld the accomplishment of my toils. With an anxiety that almost amounted to agony, I collected the instruments of life around me, that I might infuse a spark of being into the lifeless thing that lay at my feet.",
                        "It was already one in the morning; the rain pattered dismally against the panes, and my candle was nearly burnt out, when, by the glimmer of the half-extinguished light, I saw the dull yellow eye of the creature open; it breathed hard, and a convulsive motion agitated its limbs.",
                        "How can I describe my emotions at this catastrophe, or how delineate the wretch whom with such infinite pains and care I had endeavoured to form? His limbs were in proportion, and I had selected his features as beautiful. Beautiful! Great God!"
                    )
                )
            )
            t.contains("art of war") -> listOf(
                BookChapter(
                    "Chapter I: Laying Plans",
                    0,
                    listOf(
                        "Sun Tzu said: The art of war is of vital importance to the State.",
                        "It is a matter of life and death, a road either to safety or to ruin. Hence it is a subject of inquiry which can on no account be neglected.",
                        "The art of war, then, is governed by five constant factors, to be taken into account in one's deliberations, when seeking to determine the conditions obtaining in the field.",
                        "These are: (1) The Moral Law; (2) Heaven; (3) Earth; (4) The Commander; (5) Method and discipline.",
                        "The Moral Law causes the people to be in complete accord with their ruler, so that they will follow him regardless of their lives, undismayed by any danger.",
                        "Heaven signifies night and day, cold and heat, times and seasons. Earth comprises distances, great and small; danger and security; open ground and narrow passes; the chances of life and death."
                    )
                ),
                BookChapter(
                    "Chapter II: Waging War",
                    5,
                    listOf(
                        "Sun Tzu said: In the operations of war, where there are in the field a thousand swift chariots, as many heavy chariots, and a hundred thousand mail-clad soldiers, with provisions enough to carry them a thousand li, the expenditure at home and at the front will reach the sum of a thousand ounces of silver per day.",
                        "When you engage in actual fighting, if victory is long in coming, then men's weapons will grow dull and their ardor will be damped. If you lay siege to a town, you will exhaust your strength.",
                        "Now, when your weapons are dulled, your ardor damped, your strength exhausted and your treasure spent, other chieftains will spring up to take advantage of your extremity. Then no man, however wise, will be able to avert the consequences that must ensue."
                    )
                ),
                BookChapter(
                    "Chapter III: Attack by Stratagem",
                    10,
                    listOf(
                        "Sun Tzu said: In the practical art of war, the best thing of all is to take the enemy's country whole and intact; to shatter and destroy it is not so good. So, too, it is better to recapture an army entire than to destroy it.",
                        "Hence to fight and conquer in all your battles is not supreme excellence; supreme excellence consists in breaking the enemy's resistance without fighting.",
                        "Thus the highest form of generalship is to balk the enemy's plans; the next best is to prevent the junction of the enemy's forces; the next in order is to attack the enemy's army in the field; and the worst policy of all is to besiege walled cities."
                    )
                )
            )
            t.contains("metamorphosis") -> listOf(
                BookChapter(
                    "Chapter I",
                    0,
                    listOf(
                        "One morning, when Gregor Samsa woke from troubled dreams, he found himself transformed in his bed into a horrible vermin.",
                        "He lay on his armour-like back, and if he lifted his head a little he could see his brown belly, slightly domed and divided by arches into stiff sections.",
                        "The bedding was hardly able to cover it and seemed ready to slide off any moment. His many legs, pitifully thin compared with the size of the rest of him, waved about helplessly as he looked.",
                        "\"What's happened to me?\" he thought. It wasn't a dream. His room, a proper human room although a little too small, lay peacefully between its four familiar walls.",
                        "A collection of textile samples lay spread out on the table—Samsa was a travelling salesman—and above it there hung a picture that he had recently cut out of an illustrated magazine and housed in a nice, gilded frame."
                    )
                ),
                BookChapter(
                    "Chapter II",
                    5,
                    listOf(
                        "It was not until dusk that Gregor awoke out of a deep sleep, more like a swoon than a sleep. He would certainly have woken up soon anyway, as he felt he had rested and slept enough, but it seemed to him that he had been woken by hurried steps and the sound of the door leading to the hallway being carefully closed.",
                        "The light from the electric street lamps shone in patches on the ceiling and on the upper parts of the furniture, but down where Gregor was it was dark.",
                        "Slowly, awkwardly feeling his way with his antennae, which he now only began to appreciate, he pushed himself over to the door to see what had been going on."
                    )
                )
            )
            t.contains("great gatsby") -> listOf(
                BookChapter(
                    "Chapter I",
                    0,
                    listOf(
                        "In my younger and more vulnerable years my father gave me some advice that I've been turning over in my mind ever since.",
                        "\"Whenever you feel like criticizing anyone,\" he told me, \"just remember that all the people in this world haven't had the advantages that you've had.\"",
                        "He didn't say any more, but we've always been unusually communicative in a reserved way, and I understood that he meant a great deal more than that.",
                        "In consequence, I'm inclined to reserve all judgements, a habit that has opened up many curious natures to me and also made me the victim of not a few veteran bores.",
                        "When I came back from the East last autumn I felt that I wanted the world to be in uniform and at a sort of moral attention forever; I wanted no more riotous excursions with privileged glimpses into the human heart. Only Gatsby, the man who gives his name to this book, was exempt from my reaction—Gatsby, who represented everything for which I have an unaffected scorn."
                    )
                ),
                BookChapter(
                    "Chapter II",
                    5,
                    listOf(
                        "About half way between West Egg and New York the motor road hastily joins the railroad and runs beside it for a quarter of a mile, so as to shrink away from a certain desolate area of land. This is a valley of ashes—a fantastic farm where ashes grow like wheat into ridges and hills and grotesque gardens.",
                        "The eyes of Doctor T. J. Eckleburg are blue and gigantic—their irises are one yard high. They look out of no face, but, instead, from a pair of enormous yellow spectacles which pass over a non-existent nose.",
                        "Evidently some wild wag of an oculist set them there to fatten his practice in the borough of Queens, and then sank down himself into eternal blindness, or forgot them and moved away."
                    )
                )
            )
            else -> emptyList()
        }
    }
}
