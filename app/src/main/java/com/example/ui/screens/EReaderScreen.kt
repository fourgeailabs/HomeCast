package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BookContentFetcher
import com.example.data.MediaBookmark
import com.example.data.MediaProgress
import com.example.data.SettingsBackupManager
import com.example.ui.MainViewModel
import com.example.ui.theme.AccentIndigo
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.SurfaceGlass
import com.example.ui.theme.SurfaceGlassBorder
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

enum class ReaderTheme(
    val title: String,
    val bg: Color,
    val surface: Color,
    val text: Color,
    val accent: Color
) {
    SEPIA(
        title = "Classic Sepia",
        bg = Color(0xFFF7F1E5),
        surface = Color(0xFFEFE8D8),
        text = Color(0xFF2C2416),
        accent = Color(0xFF9E6B38)
    ),
    REAL_PAPER(
        title = "Warm Amber Paper",
        bg = Color(0xFFF5E8D2),
        surface = Color(0xFFEAD8BA),
        text = Color(0xFF2A1F16),
        accent = Color(0xFFB45309)
    ),
    PAPER_WHITE(
        title = "Paper White",
        bg = Color(0xFFFCFAF2),
        surface = Color(0xFFF4EFE6),
        text = Color(0xFF1E1E1E),
        accent = Color(0xFF4A6B82)
    ),
    DARK(
        title = "OLED Slate",
        bg = Color(0xFF121418),
        surface = Color(0xFF1B1F26),
        text = Color(0xFFE2E4E9),
        accent = Color(0xFF4FD1C5)
    ),
    NIGHT_WARM(
        title = "Amber Night",
        bg = Color(0xFF1A1512),
        surface = Color(0xFF261E1A),
        text = Color(0xFFE8D7C8),
        accent = Color(0xFFE09F67)
    ),
    SOLAR_MINT(
        title = "Solar Mint",
        bg = Color(0xFFEBF7EE),
        surface = Color(0xFFD8F3DC),
        text = Color(0xFF1B4332),
        accent = Color(0xFF2D6A4F)
    )
}

enum class ReaderFont(val title: String, val fontFamily: FontFamily) {
    SERIF("Literata Serif", FontFamily.Serif),
    SANS("Sans Clean", FontFamily.SansSerif),
    MONOSPACE("Typewriter", FontFamily.Monospace),
    CURSIVE("Script Classic", FontFamily.Cursive)
}

data class EBookData(
    val id: String,
    val title: String,
    val author: String,
    val totalChapters: Int = 12,
    val chapters: List<BookChapter> = emptyList(),
    val downloadUrl: String? = null,
    val publicDomainUrl: String? = null,
    val serverHostUrl: String = "",
    val serverApiKey: String = ""
)

data class BookChapter(
    val title: String,
    val startPage: Int,
    val paragraphs: List<String>
)

data class FormattedPage(
    val chapterIndex: Int,
    val chapterTitle: String,
    val pageIndexInChapter: Int,
    val totalPagesInChapter: Int,
    val textBlocks: List<String>,
    val excerpt: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EReaderScreen(
    eBook: EBookData,
    onClose: () -> Unit,
    onSwitchToComic: (() -> Unit)? = null,
    viewModel: MainViewModel? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val backupManager = remember { SettingsBackupManager(context) }

    var chapters by remember { mutableStateOf(eBook.chapters) }
    var isLoading by remember { mutableStateOf(true) }

    // Reader Settings State
    var currentTheme by remember { mutableStateOf(ReaderTheme.SEPIA) }
    var currentFont by remember { mutableStateOf(ReaderFont.SERIF) }
    var fontSizeSp by remember { mutableStateOf(17f) }
    var lineSpacingMultiplier by remember { mutableStateOf(1.5f) }
    var marginPaddingDp by remember { mutableStateOf(24f) }
    var textAlignJustified by remember { mutableStateOf(true) }

    // Navigation / Page State
    var currentChapterIndex by remember { mutableIntStateOf(0) }
    var currentPageInChapter by remember { mutableIntStateOf(0) }
    var isImmersiveMode by remember { mutableStateOf(false) }
    var showFontMenu by remember { mutableStateOf(false) }
    var showTocDrawer by remember { mutableStateOf(false) }
    var showBookmarksDrawer by remember { mutableStateOf(false) }
    var lastSavedProgress by remember { mutableStateOf<MediaProgress?>(null) }

    // Bookmarks list for this specific book
    var bookBookmarks by remember { mutableStateOf<List<MediaBookmark>>(emptyList()) }

    fun refreshBookmarks() {
        bookBookmarks = if (viewModel != null) {
            viewModel.getBookmarks(eBook.id)
        } else {
            backupManager.getBookmarks(eBook.id)
        }
    }

    // Load saved progress and bookmarks from JSON / Room on initial launch
    LaunchedEffect(eBook.id) {
        val saved = viewModel?.loadEBookProgress(eBook.id) ?: backupManager.getMediaProgress(eBook.id)
        lastSavedProgress = saved
        if (saved != null) {
            currentChapterIndex = saved.currentChapter.coerceAtLeast(0)
            currentPageInChapter = saved.currentPage.coerceAtLeast(0)
        }
        refreshBookmarks()
    }

    LaunchedEffect(eBook) {
        if (eBook.chapters.isNotEmpty()) {
            chapters = eBook.chapters
            isLoading = false
        } else {
            isLoading = true
            val fetched = BookContentFetcher.fetchBookContent(
                bookId = eBook.id,
                title = eBook.title,
                author = eBook.author,
                downloadUrl = eBook.downloadUrl ?: "",
                publicDomainUrl = eBook.publicDomainUrl ?: "",
                serverHostUrl = eBook.serverHostUrl,
                serverApiKey = eBook.serverApiKey
            )
            chapters = fetched
            isLoading = false
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFFFDFBF7)), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = AccentTeal)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Loading book chapters...", color = Color.Black, fontWeight = FontWeight.SemiBold)
            }
        }
        return
    }

    // === GRANULAR SCREEN-SIZED PAGE PAGINATION ENGINE ===
    // Calculate target word capacity per page dynamically based on typography
    val wordsPerPage = remember(fontSizeSp, lineSpacingMultiplier) {
        (130 * (17f / fontSizeSp) * (1.5f / lineSpacingMultiplier)).toInt().coerceIn(60, 200)
    }

    // Paginate each chapter into discrete, screen-fitting FormattedPages
    val paginatedChapters: List<List<FormattedPage>> = remember(chapters, wordsPerPage) {
        if (chapters.isEmpty()) {
            listOf(
                listOf(
                    FormattedPage(
                        chapterIndex = 0,
                        chapterTitle = "Chapter 1",
                        pageIndexInChapter = 0,
                        totalPagesInChapter = 1,
                        textBlocks = listOf("No content available for this book."),
                        excerpt = "No content available."
                    )
                )
            )
        } else {
            chapters.mapIndexed { chapIdx, chapter ->
                val allRawBlocks = mutableListOf<String>()
                chapter.paragraphs.forEach { para ->
                    val trimmed = para.trim()
                    if (trimmed.isNotBlank()) {
                        val wordCount = trimmed.split(Regex("\\s+")).size
                        if (wordCount > 75) {
                            // Split long paragraphs into sentence chunks
                            val sentences = trimmed.split(Regex("(?<=[.!?])\\s+"))
                            var curChunk = StringBuilder()
                            var curWords = 0
                            for (s in sentences) {
                                val sWords = s.split(Regex("\\s+")).size
                                if (curWords + sWords > 65 && curChunk.isNotEmpty()) {
                                    allRawBlocks.add(curChunk.toString().trim())
                                    curChunk = StringBuilder(s).append(" ")
                                    curWords = sWords
                                } else {
                                    curChunk.append(s).append(" ")
                                    curWords += sWords
                                }
                            }
                            if (curChunk.isNotEmpty()) {
                                allRawBlocks.add(curChunk.toString().trim())
                            }
                        } else {
                            allRawBlocks.add(trimmed)
                        }
                    }
                }

                if (allRawBlocks.isEmpty()) {
                    allRawBlocks.add("Chapter begins...")
                }

                // Group text blocks into pages based on word budget
                val pagesForChapter = mutableListOf<List<String>>()
                var curPageBlocks = mutableListOf<String>()
                var curPageWords = 0

                for (block in allRawBlocks) {
                    val bWords = block.split(Regex("\\s+")).size
                    if (curPageWords + bWords > wordsPerPage && curPageBlocks.isNotEmpty()) {
                        pagesForChapter.add(curPageBlocks.toList())
                        curPageBlocks = mutableListOf(block)
                        curPageWords = bWords
                    } else {
                        curPageBlocks.add(block)
                        curPageWords += bWords
                    }
                }
                if (curPageBlocks.isNotEmpty()) {
                    pagesForChapter.add(curPageBlocks.toList())
                }

                val totalPgs = pagesForChapter.size.coerceAtLeast(1)
                pagesForChapter.mapIndexed { pIdx, blocks ->
                    val excerptSnippet = blocks.firstOrNull()?.take(90)?.plus("...") ?: "Page ${pIdx + 1}"
                    FormattedPage(
                        chapterIndex = chapIdx,
                        chapterTitle = chapter.title,
                        pageIndexInChapter = pIdx,
                        totalPagesInChapter = totalPgs,
                        textBlocks = blocks,
                        excerpt = excerptSnippet
                    )
                }
            }
        }
    }

    val safeChapterIdx = currentChapterIndex.coerceIn(0, (paginatedChapters.size - 1).coerceAtLeast(0))
    val currentChapterPages = paginatedChapters.getOrElse(safeChapterIdx) { emptyList() }
    val totalPagesInCurrentChapter = currentChapterPages.size.coerceAtLeast(1)
    val safePageIdx = currentPageInChapter.coerceIn(0, totalPagesInCurrentChapter - 1)
    val activePage = currentChapterPages.getOrElse(safePageIdx) {
        FormattedPage(
            chapterIndex = safeChapterIdx,
            chapterTitle = chapters.getOrNull(safeChapterIdx)?.title ?: "Chapter ${safeChapterIdx + 1}",
            pageIndexInChapter = 0,
            totalPagesInChapter = 1,
            textBlocks = listOf("Loading page content..."),
            excerpt = "Loading..."
        )
    }

    // Precalculate total book pages across all chapters
    val chapterPageCounts = remember(paginatedChapters) {
        paginatedChapters.map { it.size.coerceAtLeast(1) }
    }
    val totalBookPages = remember(chapterPageCounts) {
        chapterPageCounts.sum().coerceAtLeast(1)
    }
    val absolutePageNumber = remember(safeChapterIdx, safePageIdx, chapterPageCounts) {
        val prevPages = chapterPageCounts.take(safeChapterIdx).sum()
        (prevPages + safePageIdx + 1).coerceIn(1, totalBookPages)
    }
    val overallProgressPercent = remember(absolutePageNumber, totalBookPages) {
        ((absolutePageNumber.toFloat() / totalBookPages.toFloat()) * 100).toInt().coerceIn(0, 100)
    }

    // Helper to persist current reading progress to Room, SharedPreferences, and JSON backup file
    fun persistReadingProgress(chap: Int, page: Int) {
        val safeChap = chap.coerceIn(0, (paginatedChapters.size - 1).coerceAtLeast(0))
        val chapPages = paginatedChapters.getOrElse(safeChap) { emptyList() }.size.coerceAtLeast(1)
        val safePg = page.coerceIn(0, chapPages - 1)
        val prevPages = chapterPageCounts.take(safeChap).sum()
        val absPg = (prevPages + safePg + 1).coerceIn(1, totalBookPages)
        val pct = ((absPg.toFloat() / totalBookPages.toFloat()) * 100).toInt().coerceIn(0, 100)

        if (viewModel != null) {
            viewModel.saveEBookProgress(
                id = eBook.id,
                title = eBook.title,
                author = eBook.author,
                chapterIndex = safeChap,
                pageIndex = safePg,
                totalPages = totalBookPages,
                progressPercent = pct
            )
        } else {
            backupManager.saveMediaProgress(
                MediaProgress(
                    id = eBook.id,
                    type = "EBOOK",
                    title = eBook.title,
                    creator = eBook.author,
                    currentChapter = safeChap,
                    currentPage = safePg,
                    totalPages = totalBookPages,
                    progressPercent = pct,
                    lastUpdated = System.currentTimeMillis()
                )
            )
        }
    }

    // Save reading progress on exit / back / screen disposal
    DisposableEffect(safeChapterIdx, safePageIdx) {
        onDispose {
            persistReadingProgress(safeChapterIdx, safePageIdx)
        }
    }

    // Realistic Page Flip Animation
    val pageTurnAnim = remember { Animatable(0f) }
    var isTurningForward by remember { mutableStateOf(true) }

    fun triggerPageTurn(forward: Boolean, animate: Boolean = true) {
        coroutineScope.launch {
            isTurningForward = forward
            if (animate) {
                pageTurnAnim.snapTo(0f)
                pageTurnAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
                )
            }
            if (forward) {
                // If there are more pages in this chapter, turn to NEXT PAGE
                if (safePageIdx < totalPagesInCurrentChapter - 1) {
                    val nextPg = safePageIdx + 1
                    currentPageInChapter = nextPg
                    persistReadingProgress(safeChapterIdx, nextPg)
                } else if (safeChapterIdx < paginatedChapters.size - 1) {
                    // Only advance to NEXT CHAPTER once all constituent pages are read
                    val nextChap = safeChapterIdx + 1
                    currentChapterIndex = nextChap
                    currentPageInChapter = 0
                    persistReadingProgress(nextChap, 0)
                }
            } else {
                // If not at the first page of this chapter, turn to PREVIOUS PAGE
                if (safePageIdx > 0) {
                    val prevPg = safePageIdx - 1
                    currentPageInChapter = prevPg
                    persistReadingProgress(safeChapterIdx, prevPg)
                } else if (safeChapterIdx > 0) {
                    // Move to previous chapter's last page
                    val prevChap = safeChapterIdx - 1
                    currentChapterIndex = prevChap
                    val prevPagesCount = paginatedChapters.getOrElse(prevChap) { emptyList() }.size.coerceAtLeast(1)
                    val prevLastPg = (prevPagesCount - 1).coerceAtLeast(0)
                    currentPageInChapter = prevLastPg
                    persistReadingProgress(prevChap, prevLastPg)
                }
            }
            pageTurnAnim.snapTo(0f)
        }
    }

    val isCurrentSpotBookmarked = bookBookmarks.any { it.chapterIndex == safeChapterIdx && it.pageIndex == safePageIdx }
    val wordsInCurrentPage = remember(activePage) { activePage.textBlocks.sumOf { it.split(" ").size } }
    val readingSpeedWpm = 230
    val minutesLeftInChapter = (((totalPagesInCurrentChapter - safePageIdx) * wordsPerPage) / readingSpeedWpm).coerceAtLeast(1)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(currentTheme.bg)
    ) {
        // --- REALISTIC PAPER TEXTURE & BINDING SPINE SHADOW LAYER ---
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Spine Shadow (Left Margin)
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.14f),
                        Color.Black.copy(alpha = 0.04f),
                        Color.Transparent
                    ),
                    startX = 0f,
                    endX = 42.dp.toPx()
                ),
                size = Size(42.dp.toPx(), size.height)
            )

            // Outer Page Edge Shadow (Right Margin)
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.06f)
                    ),
                    startX = size.width - 24.dp.toPx(),
                    endX = size.width
                ),
                topLeft = Offset(size.width - 24.dp.toPx(), 0f),
                size = Size(24.dp.toPx(), size.height)
            )

            // Paper grain
            if (currentTheme == ReaderTheme.REAL_PAPER || currentTheme == ReaderTheme.SEPIA) {
                val step = 32f
                var y = 0f
                while (y < size.height) {
                    val alpha = if (((y / step).toInt() % 2) == 0) 0.025f else 0.015f
                    drawLine(
                        color = Color(0xFF6B4226).copy(alpha = alpha),
                        start = Offset(0f, y),
                        end = Offset(size.width, y + 1f),
                        strokeWidth = 1f
                    )
                    y += step
                }
            }
        }

        // --- REALISTIC BOOK PAGE CANVAS & GESTURE LAYER ---
        val underlyingPage = remember(safeChapterIdx, safePageIdx, isTurningForward, paginatedChapters, currentChapterPages, totalPagesInCurrentChapter) {
            if (isTurningForward) {
                if (safePageIdx < totalPagesInCurrentChapter - 1) {
                    currentChapterPages.getOrNull(safePageIdx + 1)
                } else if (safeChapterIdx < paginatedChapters.size - 1) {
                    paginatedChapters.getOrNull(safeChapterIdx + 1)?.firstOrNull()
                } else null
            } else {
                if (safePageIdx > 0) {
                    currentChapterPages.getOrNull(safePageIdx - 1)
                } else if (safeChapterIdx > 0) {
                    paginatedChapters.getOrNull(safeChapterIdx - 1)?.lastOrNull()
                } else null
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(safePageIdx, safeChapterIdx) {
                    detectTapGestures(
                        onTap = { offset ->
                            val width = size.width
                            when {
                                offset.x < width * 0.28f -> triggerPageTurn(forward = false)
                                offset.x > width * 0.72f -> triggerPageTurn(forward = true)
                                else -> isImmersiveMode = !isImmersiveMode
                            }
                        }
                    )
                }
                .pointerInput(safePageIdx, safeChapterIdx) {
                    var totalDragX = 0f
                    val width = size.width.toFloat()
                    detectHorizontalDragGestures(
                        onDragStart = { totalDragX = 0f },
                        onDragEnd = {
                            coroutineScope.launch {
                                if (totalDragX < -50f) {
                                    isTurningForward = true
                                    pageTurnAnim.animateTo(1f, tween(220, easing = FastOutSlowInEasing))
                                    triggerPageTurn(forward = true, animate = false)
                                } else if (totalDragX > 50f) {
                                    isTurningForward = false
                                    pageTurnAnim.animateTo(1f, tween(220, easing = FastOutSlowInEasing))
                                    triggerPageTurn(forward = false, animate = false)
                                } else {
                                    pageTurnAnim.animateTo(0f, tween(200))
                                }
                                totalDragX = 0f
                            }
                        },
                        onDragCancel = {
                            coroutineScope.launch { pageTurnAnim.animateTo(0f, tween(200)) }
                            totalDragX = 0f
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            totalDragX += dragAmount
                            isTurningForward = totalDragX < 0
                            val progress = (abs(totalDragX) / width).coerceIn(0f, 1f)
                            coroutineScope.launch { pageTurnAnim.snapTo(progress) }
                        }
                    )
                }
        ) {
            val animProgress = pageTurnAnim.value

            // 1. UNDERLYING PAGE LAYER (Revealed beneath the rolling page)
            if (animProgress > 0f && underlyingPage != null) {
                RenderBookPageLayout(
                    page = underlyingPage,
                    eBookTitle = eBook.title,
                    currentTheme = currentTheme,
                    currentFont = currentFont,
                    fontSizeSp = fontSizeSp,
                    lineSpacingMultiplier = lineSpacingMultiplier,
                    marginPaddingDp = marginPaddingDp,
                    textAlignJustified = textAlignJustified,
                    totalBookPages = totalBookPages,
                    chapterPageCounts = chapterPageCounts,
                    wordsPerPage = wordsPerPage,
                    readingSpeedWpm = readingSpeedWpm,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 2. PRIMARY ROLLING / CURLING PAGE LAYER
            RenderBookPageLayout(
                page = activePage,
                eBookTitle = eBook.title,
                currentTheme = currentTheme,
                currentFont = currentFont,
                fontSizeSp = fontSizeSp,
                lineSpacingMultiplier = lineSpacingMultiplier,
                marginPaddingDp = marginPaddingDp,
                textAlignJustified = textAlignJustified,
                totalBookPages = totalBookPages,
                chapterPageCounts = chapterPageCounts,
                wordsPerPage = wordsPerPage,
                readingSpeedWpm = readingSpeedWpm,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        if (animProgress > 0f) {
                            cameraDistance = 14f * density
                            if (isTurningForward) {
                                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                                scaleX = (1f - animProgress * 0.85f).coerceAtLeast(0.02f)
                                translationX = -animProgress * (density * 160f)
                                rotationY = -animProgress * 32f
                                alpha = (1f - animProgress * 0.65f).coerceIn(0f, 1f)
                            } else {
                                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 0.5f)
                                scaleX = (animProgress * 0.85f + 0.15f).coerceIn(0.02f, 1f)
                                translationX = (1f - animProgress) * (density * 160f)
                                rotationY = (1f - animProgress) * 32f
                                alpha = (animProgress * 0.65f + 0.35f).coerceIn(0f, 1f)
                            }
                        }
                    }
            )

            // 3. REALISTIC 3D PAPER ROLL / CYLINDER CURL OVERLAY CANVAS
            if (animProgress > 0f) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val foldX = if (isTurningForward) width * (1f - animProgress) else width * animProgress
                    val cylinderWidth = width * 0.22f
                    val foldFactor = (Math.sin(animProgress.toDouble() * Math.PI)).toFloat()

                    // A) Ambient Drop Shadow Cast Onto Revealing Page
                    if (isTurningForward) {
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.45f * foldFactor),
                                    Color.Black.copy(alpha = 0.15f * foldFactor),
                                    Color.Transparent
                                ),
                                startX = foldX,
                                endX = foldX + cylinderWidth * 1.6f
                            ),
                            topLeft = Offset(foldX, 0f),
                            size = Size(cylinderWidth * 1.6f, height)
                        )
                    } else {
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.15f * foldFactor),
                                    Color.Black.copy(alpha = 0.45f * foldFactor)
                                ),
                                startX = foldX - cylinderWidth * 1.6f,
                                endX = foldX
                            ),
                            topLeft = Offset(foldX - cylinderWidth * 1.6f, 0f),
                            size = Size(cylinderWidth * 1.6f, height)
                        )
                    }

                    // B) Rolled Cylinder Backside Paper Surface
                    if (isTurningForward) {
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    currentTheme.surface.copy(alpha = 0.95f),
                                    currentTheme.bg.copy(alpha = 0.88f),
                                    Color.Black.copy(alpha = 0.18f * foldFactor)
                                ),
                                startX = foldX - cylinderWidth * 0.7f,
                                endX = foldX
                            ),
                            topLeft = Offset(foldX - cylinderWidth * 0.7f, 0f),
                            size = Size(cylinderWidth * 0.7f, height)
                        )
                    } else {
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.18f * foldFactor),
                                    currentTheme.bg.copy(alpha = 0.88f),
                                    currentTheme.surface.copy(alpha = 0.95f)
                                ),
                                startX = foldX,
                                endX = foldX + cylinderWidth * 0.7f
                            ),
                            topLeft = Offset(foldX, 0f),
                            size = Size(cylinderWidth * 0.7f, height)
                        )
                    }

                    // C) Glossy Specular Highlight Along Roll Apex
                    val highlightX = if (isTurningForward) foldX - cylinderWidth * 0.35f else foldX + cylinderWidth * 0.35f
                    val highlightWidth = 24.dp.toPx()
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.55f * foldFactor),
                                Color.Transparent
                            ),
                            startX = highlightX - highlightWidth,
                            endX = highlightX + highlightWidth
                        ),
                        topLeft = Offset(highlightX - highlightWidth, 0f),
                        size = Size(highlightWidth * 2f, height)
                    )

                    // D) 3D Curved Top & Bottom Page Silhouette Arcs
                    val arcDepth = 18.dp.toPx() * foldFactor
                    val topPath = androidx.compose.ui.graphics.Path().apply {
                        moveTo(foldX - cylinderWidth * 0.7f, 0f)
                        quadraticBezierTo(
                            foldX, -arcDepth,
                            foldX + cylinderWidth * 0.7f, 0f
                        )
                        lineTo(foldX + cylinderWidth * 0.7f, 8.dp.toPx())
                        quadraticBezierTo(
                            foldX, -arcDepth + 8.dp.toPx(),
                            foldX - cylinderWidth * 0.7f, 8.dp.toPx()
                        )
                        close()
                    }
                    drawPath(
                        path = topPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.40f * foldFactor),
                                Color.Black.copy(alpha = 0.15f * foldFactor)
                            )
                        )
                    )

                    val bottomPath = androidx.compose.ui.graphics.Path().apply {
                        moveTo(foldX - cylinderWidth * 0.7f, height)
                        quadraticBezierTo(
                            foldX, height + arcDepth,
                            foldX + cylinderWidth * 0.7f, height
                        )
                        lineTo(foldX + cylinderWidth * 0.7f, height - 8.dp.toPx())
                        quadraticBezierTo(
                            foldX, height + arcDepth - 8.dp.toPx(),
                            foldX - cylinderWidth * 0.7f, height - 8.dp.toPx()
                        )
                        close()
                    }
                    drawPath(
                        path = bottomPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.15f * foldFactor),
                                Color.White.copy(alpha = 0.40f * foldFactor)
                            )
                        )
                    )
                }
            }
        }

        // --- KINDLE IMMERSION TOP APP BAR ---
        AnimatedVisibility(
            visible = !isImmersiveMode,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                color = currentTheme.surface.copy(alpha = 0.96f),
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Close Reader", tint = currentTheme.text)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(
                                eBook.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = currentTheme.text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "${eBook.author} • Ch. ${safeChapterIdx + 1}, Pg. ${safePageIdx + 1}/$totalPagesInCurrentChapter",
                                fontSize = 11.sp,
                                color = currentTheme.text.copy(alpha = 0.7f),
                                maxLines = 1
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (onSwitchToComic != null) {
                            FilledTonalButton(
                                onClick = onSwitchToComic,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = currentTheme.accent.copy(alpha = 0.2f),
                                    contentColor = currentTheme.accent
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(Icons.Default.AutoStories, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Comics", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        // Bookmark Toggle Button
                        IconButton(onClick = {
                            val existing = bookBookmarks.firstOrNull { it.chapterIndex == safeChapterIdx && it.pageIndex == safePageIdx }
                            if (existing != null) {
                                if (viewModel != null) viewModel.deleteBookmark(existing.id) else backupManager.deleteBookmark(existing.id)
                            } else {
                                val newBookmark = MediaBookmark(
                                    mediaId = eBook.id,
                                    mediaType = "EBOOK",
                                    title = eBook.title,
                                    chapterIndex = safeChapterIdx,
                                    chapterTitle = activePage.chapterTitle,
                                    pageIndex = safePageIdx,
                                    excerpt = activePage.excerpt,
                                    createdAt = System.currentTimeMillis()
                                )
                                if (viewModel != null) viewModel.saveBookmark(newBookmark) else backupManager.saveBookmark(newBookmark)
                            }
                            refreshBookmarks()
                        }) {
                            Icon(
                                if (isCurrentSpotBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = if (isCurrentSpotBookmarked) "Remove Bookmark" else "Add Bookmark",
                                tint = if (isCurrentSpotBookmarked) currentTheme.accent else currentTheme.text
                            )
                        }

                        // Bookmarks Drawer Button
                        IconButton(onClick = {
                            refreshBookmarks()
                            showBookmarksDrawer = true
                        }) {
                            Icon(Icons.Default.Bookmarks, contentDescription = "Saved Bookmarks", tint = currentTheme.text)
                        }

                        // Typography / Fonts Menu Button
                        IconButton(onClick = { showFontMenu = true }) {
                            Icon(Icons.Default.FormatSize, contentDescription = "Typography", tint = currentTheme.text)
                        }

                        // Table of Contents Button
                        IconButton(onClick = { showTocDrawer = true }) {
                            Icon(Icons.Default.List, contentDescription = "Table of Contents", tint = currentTheme.text)
                        }
                    }
                }
            }
        }

        // --- BOTTOM PROGRESS & CHAPTER NAVIGATION BAR ---
        AnimatedVisibility(
            visible = !isImmersiveMode,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                color = currentTheme.surface.copy(alpha = 0.96f),
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (safeChapterIdx > 0) {
                                    currentChapterIndex = safeChapterIdx - 1
                                    currentPageInChapter = 0
                                    persistReadingProgress(safeChapterIdx - 1, 0)
                                }
                            },
                            enabled = safeChapterIdx > 0
                        ) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "Previous Chapter", tint = currentTheme.text)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Chapter ${safeChapterIdx + 1} of ${paginatedChapters.size}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = currentTheme.text
                            )
                            Text(
                                "${activePage.chapterTitle} • Page ${safePageIdx + 1}/$totalPagesInCurrentChapter",
                                fontSize = 11.sp,
                                color = currentTheme.text.copy(alpha = 0.7f)
                            )
                        }

                        IconButton(
                            onClick = {
                                if (safeChapterIdx < paginatedChapters.size - 1) {
                                    currentChapterIndex = safeChapterIdx + 1
                                    currentPageInChapter = 0
                                    persistReadingProgress(safeChapterIdx + 1, 0)
                                }
                            },
                            enabled = safeChapterIdx < paginatedChapters.size - 1
                        ) {
                            Icon(Icons.Default.SkipNext, contentDescription = "Next Chapter", tint = currentTheme.text)
                        }
                    }

                    // Granular Page Scrubber Slider within current chapter
                    Slider(
                        value = safePageIdx.toFloat(),
                        onValueChange = {
                            val newPg = it.toInt()
                            currentPageInChapter = newPg
                            persistReadingProgress(safeChapterIdx, newPg)
                        },
                        valueRange = 0f..(totalPagesInCurrentChapter - 1).coerceAtLeast(1).toFloat(),
                        steps = (totalPagesInCurrentChapter - 2).coerceAtLeast(0),
                        colors = SliderDefaults.colors(
                            thumbColor = currentTheme.accent,
                            activeTrackColor = currentTheme.accent,
                            inactiveTrackColor = currentTheme.text.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // --- TYPOGRAPHY & THEMES BOTTOM SHEET ---
        if (showFontMenu) {
            ModalBottomSheet(
                onDismissRequest = { showFontMenu = false },
                containerColor = currentTheme.surface,
                tonalElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text("Themes & Typography", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = currentTheme.text)
                    Spacer(modifier = Modifier.height(16.dp))

                    // 1. Color Themes
                    Text("Reading Theme", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = currentTheme.text.copy(alpha = 0.7f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ReaderTheme.values().forEach { theme ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .shadow(if (currentTheme == theme) 6.dp else 1.dp, RoundedCornerShape(12.dp))
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(theme.bg)
                                    .clickable { currentTheme = theme }
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Aa",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = theme.text
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 2. Font Size Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Font Size: ${fontSizeSp.toInt()}sp", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = currentTheme.text.copy(alpha = 0.7f))
                        Text("${fontSizeSp.toInt()}", fontWeight = FontWeight.Bold, color = currentTheme.accent)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("A", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = currentTheme.text)
                        Slider(
                            value = fontSizeSp,
                            onValueChange = { fontSizeSp = it },
                            valueRange = 13f..28f,
                            colors = SliderDefaults.colors(
                                thumbColor = currentTheme.accent,
                                activeTrackColor = currentTheme.accent
                            ),
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                        )
                        Text("A", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = currentTheme.text)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3. Font Family Selector
                    Text("Typeface", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = currentTheme.text.copy(alpha = 0.7f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ReaderFont.values().forEach { font ->
                            val isSelected = currentFont == font
                            FilterChip(
                                selected = isSelected,
                                onClick = { currentFont = font },
                                label = { Text(font.title, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = currentTheme.accent.copy(alpha = 0.25f),
                                    selectedLabelColor = currentTheme.accent
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 4. Line Spacing & Alignment
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Justify Text", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = currentTheme.text)
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = textAlignJustified,
                                onCheckedChange = { textAlignJustified = it }
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { lineSpacingMultiplier = (lineSpacingMultiplier - 0.2f).coerceAtLeast(1.2f) }) {
                                Icon(Icons.Default.FormatLineSpacing, contentDescription = "Decrease Spacing", tint = currentTheme.text)
                            }
                            Text(String.format(Locale.getDefault(), "%.1fx", lineSpacingMultiplier), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = currentTheme.accent)
                            IconButton(onClick = { lineSpacingMultiplier = (lineSpacingMultiplier + 0.2f).coerceAtMost(2.2f) }) {
                                Icon(Icons.Default.FormatLineSpacing, contentDescription = "Increase Spacing", tint = currentTheme.text)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // --- TABLE OF CONTENTS BOTTOM SHEET ---
        if (showTocDrawer) {
            ModalBottomSheet(
                onDismissRequest = { showTocDrawer = false },
                containerColor = currentTheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Table of Contents", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = currentTheme.text)
                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        itemsIndexed(chapters) { idx, chapter ->
                            val isCurrent = idx == safeChapterIdx
                            val pgsCount = paginatedChapters.getOrElse(idx) { emptyList() }.size.coerceAtLeast(1)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isCurrent) currentTheme.accent.copy(alpha = 0.18f) else Color.Transparent)
                                    .clickable {
                                        currentChapterIndex = idx
                                        currentPageInChapter = 0
                                        persistReadingProgress(idx, 0)
                                        showTocDrawer = false
                                    }
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    chapter.title,
                                    fontSize = 14.sp,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isCurrent) currentTheme.accent else currentTheme.text,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    "$pgsCount pgs",
                                    fontSize = 12.sp,
                                    color = currentTheme.text.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // --- BOOKMARKS & LAST SPOT READ BOTTOM SHEET ---
        if (showBookmarksDrawer) {
            ModalBottomSheet(
                onDismissRequest = { showBookmarksDrawer = false },
                containerColor = currentTheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Bookmarks & Last Spot",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 19.sp,
                            color = currentTheme.text
                        )
                        IconButton(onClick = {
                            val newBookmark = MediaBookmark(
                                mediaId = eBook.id,
                                mediaType = "EBOOK",
                                title = eBook.title,
                                chapterIndex = safeChapterIdx,
                                chapterTitle = activePage.chapterTitle,
                                pageIndex = safePageIdx,
                                excerpt = activePage.excerpt,
                                createdAt = System.currentTimeMillis()
                            )
                            if (viewModel != null) viewModel.saveBookmark(newBookmark) else backupManager.saveBookmark(newBookmark)
                            refreshBookmarks()
                        }) {
                            Icon(Icons.Default.BookmarkAdd, contentDescription = "Add Current Page Bookmark", tint = currentTheme.accent)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 1. Last Spot Read Quick Resume Card
                    val lastSpot = lastSavedProgress
                    if (lastSpot != null) {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = currentTheme.accent.copy(alpha = 0.15f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, currentTheme.accent.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    currentChapterIndex = lastSpot.currentChapter.coerceIn(0, paginatedChapters.size - 1)
                                    val totalChapPgs = paginatedChapters.getOrElse(currentChapterIndex) { emptyList() }.size.coerceAtLeast(1)
                                    currentPageInChapter = lastSpot.currentPage.coerceIn(0, totalChapPgs - 1)
                                    showBookmarksDrawer = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(currentTheme.accent.copy(alpha = 0.25f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.History, contentDescription = null, tint = currentTheme.accent, modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            "Last Spot Read (${lastSpot.progressPercent}%)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = currentTheme.text
                                        )
                                        Text(
                                            "Chapter ${lastSpot.currentChapter + 1} • Page ${lastSpot.currentPage + 1}",
                                            fontSize = 11.sp,
                                            color = currentTheme.text.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                                Button(
                                    onClick = {
                                        currentChapterIndex = lastSpot.currentChapter.coerceIn(0, paginatedChapters.size - 1)
                                        val totalChapPgs = paginatedChapters.getOrElse(currentChapterIndex) { emptyList() }.size.coerceAtLeast(1)
                                        currentPageInChapter = lastSpot.currentPage.coerceIn(0, totalChapPgs - 1)
                                        showBookmarksDrawer = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = currentTheme.accent),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Resume", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // 2. Saved Bookmarks List
                    Text(
                        "Saved Bookmarks (${bookBookmarks.size})",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = currentTheme.text.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (bookBookmarks.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No bookmarks saved for this book yet.\nTap the bookmark icon above to save your favorite spots.",
                                textAlign = TextAlign.Center,
                                fontSize = 13.sp,
                                color = currentTheme.text.copy(alpha = 0.5f),
                                lineHeight = 18.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(bookBookmarks) { bookmark ->
                                val dateStr = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(bookmark.createdAt))
                                Surface(
                                    color = currentTheme.surface,
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, currentTheme.text.copy(alpha = 0.12f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                currentChapterIndex = bookmark.chapterIndex.coerceIn(0, paginatedChapters.size - 1)
                                                val totalChapPgs = paginatedChapters.getOrElse(currentChapterIndex) { emptyList() }.size.coerceAtLeast(1)
                                                currentPageInChapter = bookmark.pageIndex.coerceIn(0, totalChapPgs - 1)
                                                persistReadingProgress(currentChapterIndex, currentPageInChapter)
                                                showBookmarksDrawer = false
                                            }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Bookmark, contentDescription = null, tint = currentTheme.accent, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    "Chapter ${bookmark.chapterIndex + 1} • Page ${bookmark.pageIndex + 1}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = currentTheme.text
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    dateStr,
                                                    fontSize = 10.sp,
                                                    color = currentTheme.text.copy(alpha = 0.5f)
                                                )
                                            }
                                            if (bookmark.excerpt.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    "\"${bookmark.excerpt}\"",
                                                    fontSize = 11.sp,
                                                    fontStyle = FontStyle.Italic,
                                                    color = currentTheme.text.copy(alpha = 0.7f),
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        IconButton(onClick = {
                                            if (viewModel != null) viewModel.deleteBookmark(bookmark.id) else backupManager.deleteBookmark(bookmark.id)
                                            refreshBookmarks()
                                        }) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Bookmark", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun RenderBookPageLayout(
    page: FormattedPage,
    eBookTitle: String,
    currentTheme: ReaderTheme,
    currentFont: ReaderFont,
    fontSizeSp: Float,
    lineSpacingMultiplier: Float,
    marginPaddingDp: Float,
    textAlignJustified: Boolean,
    totalBookPages: Int,
    chapterPageCounts: List<Int>,
    wordsPerPage: Int,
    readingSpeedWpm: Int,
    modifier: Modifier = Modifier
) {
    val prevPages = chapterPageCounts.take(page.chapterIndex).sum()
    val absolutePageNumber = (prevPages + page.pageIndexInChapter + 1).coerceIn(1, totalBookPages)
    val overallProgressPercent = ((absolutePageNumber.toFloat() / totalBookPages.toFloat()) * 100).toInt().coerceIn(0, 100)
    val minutesLeftInChapter = (((page.totalPagesInChapter - page.pageIndexInChapter) * wordsPerPage) / readingSpeedWpm).coerceAtLeast(1)

    Column(
        modifier = modifier
            .padding(horizontal = marginPaddingDp.dp, vertical = 32.dp)
    ) {
        // Header (Book Title & Chapter Title)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                eBookTitle.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = currentTheme.text.copy(alpha = 0.5f),
                letterSpacing = 1.2.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                page.chapterTitle,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = currentTheme.text.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        HorizontalDivider(
            color = currentTheme.text.copy(alpha = 0.12f),
            thickness = 0.8.dp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Chapter Head banner on page 0
        if (page.pageIndexInChapter == 0) {
            Text(
                page.chapterTitle,
                fontFamily = currentFont.fontFamily,
                fontSize = (fontSizeSp + 6f).sp,
                fontWeight = FontWeight.Bold,
                color = currentTheme.text,
                lineHeight = ((fontSizeSp + 6f) * 1.3f).sp,
                modifier = Modifier.padding(bottom = 14.dp)
            )
        }

        // Body Text Blocks
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy((fontSizeSp * 0.75f).dp)
        ) {
            page.textBlocks.forEach { block ->
                Text(
                    text = block,
                    fontFamily = currentFont.fontFamily,
                    fontSize = fontSizeSp.sp,
                    color = currentTheme.text,
                    lineHeight = (fontSizeSp * lineSpacingMultiplier).sp,
                    textAlign = if (textAlignJustified) TextAlign.Justify else TextAlign.Start,
                    letterSpacing = 0.2.sp
                )
            }
        }

        // Footer (Granular Page Counter & Remaining Time)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Page ${page.pageIndexInChapter + 1} of ${page.totalPagesInChapter} in Ch. ${page.chapterIndex + 1} • $minutesLeftInChapter min left",
                fontSize = 11.sp,
                color = currentTheme.text.copy(alpha = 0.6f),
                fontWeight = FontWeight.Medium
            )

            Text(
                "Book Page $absolutePageNumber / $totalBookPages ($overallProgressPercent%)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = currentTheme.text.copy(alpha = 0.7f)
            )
        }
    }
}
