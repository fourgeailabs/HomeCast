package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentIndigo
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.SurfaceGlass
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class ReaderTheme(
    val title: String,
    val bg: Color,
    val text: Color,
    val surface: Color,
    val accent: Color
) {
    REAL_PAPER(
        title = "Real Paper (Warm Amber)",
        bg = Color(0xFFF5E8D2),
        text = Color(0xFF2A1F16),
        surface = Color(0xFFEAD8BA),
        accent = Color(0xFFB45309)
    ),
    SEPIA(
        title = "Classic Sepia",
        bg = Color(0xFFFBF0D9),
        text = Color(0xFF2D241E),
        surface = Color(0xFFF2E3C6),
        accent = Color(0xFF9A5B28)
    ),
    PAPER_WHITE(
        title = "Paper White",
        bg = Color(0xFFFDFCFA),
        text = Color(0xFF1E293B),
        surface = Color(0xFFF1F5F9),
        accent = Color(0xFF3B82F6)
    ),
    OLED_DARK(
        title = "True Black",
        bg = Color(0xFF000000),
        text = Color(0xFFE2E8F0),
        surface = Color(0xFF18181B),
        accent = Color(0xFF14B8A6)
    ),
    MIDNIGHT(
        title = "Midnight",
        bg = Color(0xFF0F172A),
        text = Color(0xFFCBD5E1),
        surface = Color(0xFF1E293B),
        accent = Color(0xFF6366F1)
    ),
    SOLAR_MINT(
        title = "Solar Mint",
        bg = Color(0xFFEBF7EE),
        text = Color(0xFF1B4332),
        surface = Color(0xFFD8F3DC),
        accent = Color(0xFF2D6A4F)
    )
}

enum class ReaderFont(val title: String, val fontFamily: FontFamily) {
    SERIF("Literata Serif", FontFamily.Serif),
    SANS("Modern Sans", FontFamily.SansSerif),
    MONO("Monospace", FontFamily.Monospace),
    CURSIVE("Script Classic", FontFamily.Cursive)
}

data class BookChapter(
    val title: String,
    val startPage: Int,
    val paragraphs: List<String>
)

data class EBookData(
    val id: String,
    val title: String,
    val author: String,
    val totalChapters: Int,
    val chapters: List<BookChapter>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EReaderScreen(
    eBook: EBookData,
    onClose: () -> Unit,
    onSwitchToComic: (() -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()

    // Reader Settings State
    var currentTheme by remember { mutableStateOf(ReaderTheme.SEPIA) }
    var currentFont by remember { mutableStateOf(ReaderFont.SERIF) }
    var fontSizeSp by remember { mutableStateOf(17f) }
    var lineSpacingMultiplier by remember { mutableStateOf(1.5f) }
    var marginPaddingDp by remember { mutableStateOf(24f) }
    var textAlignJustified by remember { mutableStateOf(true) }

    // Navigation / Page State
    var currentChapterIndex by remember { mutableStateOf(0) }
    var currentPageInChapter by remember { mutableStateOf(0) }
    var isImmersiveMode by remember { mutableStateOf(false) }
    var showFontMenu by remember { mutableStateOf(false) }
    var showTocDrawer by remember { mutableStateOf(false) }
    var showBookmarksDrawer by remember { mutableStateOf(false) }

    val bookmarks = remember { mutableStateListOf<Pair<Int, Int>>() } // (chapter, page)

    // Compute active chapter and content
    val activeChapter = eBook.chapters.getOrElse(currentChapterIndex) { eBook.chapters.first() }
    val paragraphsPerPage = 3
    val totalPagesInChapter = (activeChapter.paragraphs.size + paragraphsPerPage - 1) / paragraphsPerPage
    val safePage = currentPageInChapter.coerceIn(0, (totalPagesInChapter - 1).coerceAtLeast(0))

    val displayedParagraphs = remember(currentChapterIndex, safePage, activeChapter) {
        val start = safePage * paragraphsPerPage
        val end = (start + paragraphsPerPage).coerceAtMost(activeChapter.paragraphs.size)
        if (start < activeChapter.paragraphs.size) {
            activeChapter.paragraphs.subList(start, end)
        } else {
            emptyList()
        }
    }

    // Realistic Page Flip Animation (Animatable rotation and curl shadow)
    val pageTurnAnim = remember { Animatable(0f) }
    var isTurningForward by remember { mutableStateOf(true) }

    fun triggerPageTurn(forward: Boolean, animate: Boolean = true) {
        coroutineScope.launch {
            isTurningForward = forward
            if (animate) {
                pageTurnAnim.snapTo(0f)
                pageTurnAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
                )
            }
            if (forward) {
                if (safePage < totalPagesInChapter - 1) {
                    currentPageInChapter = safePage + 1
                } else if (currentChapterIndex < eBook.chapters.size - 1) {
                    currentChapterIndex++
                    currentPageInChapter = 0
                }
            } else {
                if (safePage > 0) {
                    currentPageInChapter = safePage - 1
                } else if (currentChapterIndex > 0) {
                    currentChapterIndex--
                    val prevChapter = eBook.chapters[currentChapterIndex]
                    val prevTotalPages = (prevChapter.paragraphs.size + paragraphsPerPage - 1) / paragraphsPerPage
                    currentPageInChapter = (prevTotalPages - 1).coerceAtLeast(0)
                }
            }
            pageTurnAnim.snapTo(0f)
        }
    }

    val isBookmarked = bookmarks.contains(currentChapterIndex to safePage)
    val wordsInChapter = remember(activeChapter) { activeChapter.paragraphs.sumOf { it.split(" ").size } }
    val readingSpeedWpm = 230
    val minutesLeftInChapter = ((wordsInChapter * (1f - (safePage.toFloat() / totalPagesInChapter.coerceAtLeast(1)))) / readingSpeedWpm).toInt().coerceAtLeast(1)

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

            // Realistic Paper Fiber & Micro-Grain Noise (warm subtle paper grain)
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(safePage, currentChapterIndex) {
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
                .pointerInput(safePage, currentChapterIndex) {
                    var totalDragX = 0f
                    val width = size.width.toFloat()
                    detectHorizontalDragGestures(
                        onDragStart = {
                            totalDragX = 0f
                        },
                        onDragEnd = {
                            coroutineScope.launch {
                                if (totalDragX < -60f) {
                                    isTurningForward = true
                                    pageTurnAnim.animateTo(1f, tween(150, easing = LinearOutSlowInEasing))
                                    triggerPageTurn(forward = true, animate = false)
                                } else if (totalDragX > 60f) {
                                    isTurningForward = false
                                    pageTurnAnim.animateTo(1f, tween(150, easing = LinearOutSlowInEasing))
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
                            val progress = (kotlin.math.abs(totalDragX) / width).coerceIn(0f, 1f)
                            coroutineScope.launch {
                                pageTurnAnim.snapTo(progress)
                            }
                        }
                    )
                }
        ) {
            // Main Text Content Container with Realistic Paper Margin & Shadows
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = marginPaddingDp.dp, vertical = 32.dp)
                    .graphicsLayer {
                        if (pageTurnAnim.value > 0f) {
                            val progress = pageTurnAnim.value
                            // Set pivot to the spine (left edge)
                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                            
                            if (isTurningForward) {
                                // Peel from right to left
                                rotationY = -progress * 90f // fold over to the left
                                translationX = -progress * 100f
                                scaleX = 1f - (progress * 0.1f)
                                scaleY = 1f + (Math.sin(progress.toDouble() * Math.PI).toFloat() * 0.05f) // bend vertically
                                alpha = 1f - progress
                            } else {
                                // Bring from left to right
                                val invProgress = 1f - progress
                                rotationY = -invProgress * 90f
                                translationX = -invProgress * 100f
                                scaleX = 1f - (invProgress * 0.1f)
                                scaleY = 1f + (Math.sin(invProgress.toDouble() * Math.PI).toFloat() * 0.05f)
                                alpha = progress
                            }
                            cameraDistance = 16f * density
                        }
                    }
            ) {
                // Header (Book Title & Chapter)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        eBook.title.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = currentTheme.text.copy(alpha = 0.5f),
                        letterSpacing = 1.2.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        activeChapter.title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = currentTheme.text.copy(alpha = 0.5f),
                        maxLines = 1
                    )
                }

                Divider(
                    color = currentTheme.text.copy(alpha = 0.12f),
                    thickness = 0.8.dp,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                // Chapter Head banner on page 0
                if (safePage == 0) {
                    Text(
                        activeChapter.title,
                        fontFamily = currentFont.fontFamily,
                        fontSize = (fontSizeSp + 6f).sp,
                        fontWeight = FontWeight.Bold,
                        color = currentTheme.text,
                        lineHeight = ((fontSizeSp + 6f) * 1.3f).sp,
                        modifier = Modifier.padding(bottom = 18.dp)
                    )
                }

                // Body Paragraphs with Drop-Cap on initial paragraph
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy((fontSizeSp * 0.8f).dp)
                ) {
                    displayedParagraphs.forEachIndexed { idx, paragraph ->
                        Text(
                            text = paragraph,
                            fontFamily = currentFont.fontFamily,
                            fontSize = fontSizeSp.sp,
                            color = currentTheme.text,
                            lineHeight = (fontSizeSp * lineSpacingMultiplier).sp,
                            textAlign = if (textAlignJustified) TextAlign.Justify else TextAlign.Start,
                            letterSpacing = 0.2.sp
                        )
                    }
                }

                // Footer (Kindle Style Page counter & time left)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Page ${safePage + 1} of $totalPagesInChapter • $minutesLeftInChapter mins left in chapter",
                        fontSize = 11.sp,
                        color = currentTheme.text.copy(alpha = 0.55f),
                        fontWeight = FontWeight.Medium
                    )

                    val overallProgressPercent = (((currentChapterIndex.toFloat() / eBook.chapters.size.coerceAtLeast(1)) +
                            ((safePage.toFloat() / totalPagesInChapter.coerceAtLeast(1)) / eBook.chapters.size.coerceAtLeast(1))) * 100).toInt()
                    Text(
                        "$overallProgressPercent%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = currentTheme.text.copy(alpha = 0.65f)
                    )
                }
            }

            // Realistic Page Edge Shadow Overlay during page curl
            if (pageTurnAnim.value > 0f) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val curlProgress = pageTurnAnim.value
                    val shadowWidth = size.width * 0.25f
                    val shadowX = if (isTurningForward) size.width * (1f - curlProgress) else size.width * curlProgress

                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.15f * (1f - abs(curlProgress - 0.5f) * 2f)),
                                Color.Transparent
                            ),
                            startX = shadowX - shadowWidth,
                            endX = shadowX + shadowWidth
                        ),
                        size = size
                    )
                }
            }
        }

        // --- KINDLE IMMERSION APP BARS (Slide in/out on center tap) ---
        AnimatedVisibility(
            visible = !isImmersiveMode,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                color = currentTheme.surface.copy(alpha = 0.95f),
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                                eBook.author,
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
                                Text("Comics Mode", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        IconButton(onClick = {
                            val pair = currentChapterIndex to safePage
                            if (isBookmarked) bookmarks.remove(pair) else bookmarks.add(pair)
                        }) {
                            Icon(
                                if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Bookmark",
                                tint = if (isBookmarked) currentTheme.accent else currentTheme.text
                            )
                        }

                        IconButton(onClick = { showFontMenu = true }) {
                            Icon(Icons.Default.FormatSize, contentDescription = "Typography", tint = currentTheme.text)
                        }

                        IconButton(onClick = { showTocDrawer = true }) {
                            Icon(Icons.Default.List, contentDescription = "Table of Contents", tint = currentTheme.text)
                        }
                    }
                }
            }
        }

        // Bottom Progress & Chapter Scrubber Bar
        AnimatedVisibility(
            visible = !isImmersiveMode,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                color = currentTheme.surface.copy(alpha = 0.95f),
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
                                if (currentChapterIndex > 0) {
                                    currentChapterIndex--
                                    currentPageInChapter = 0
                                }
                            },
                            enabled = currentChapterIndex > 0
                        ) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "Previous Chapter", tint = currentTheme.text)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Chapter ${currentChapterIndex + 1} of ${eBook.chapters.size}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = currentTheme.text
                            )
                            Text(
                                "${activeChapter.title} • Page ${safePage + 1}/$totalPagesInChapter",
                                fontSize = 11.sp,
                                color = currentTheme.text.copy(alpha = 0.7f)
                            )
                        }

                        IconButton(
                            onClick = {
                                if (currentChapterIndex < eBook.chapters.size - 1) {
                                    currentChapterIndex++
                                    currentPageInChapter = 0
                                }
                            },
                            enabled = currentChapterIndex < eBook.chapters.size - 1
                        ) {
                            Icon(Icons.Default.SkipNext, contentDescription = "Next Chapter", tint = currentTheme.text)
                        }
                    }

                    // Chapter Progress Slider
                    Slider(
                        value = safePage.toFloat(),
                        onValueChange = { currentPageInChapter = it.toInt() },
                        valueRange = 0f..(totalPagesInChapter - 1).coerceAtLeast(1).toFloat(),
                        steps = (totalPagesInChapter - 2).coerceAtLeast(0),
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
                            Text(String.format("%.1fx", lineSpacingMultiplier), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = currentTheme.accent)
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
                        itemsIndexed(eBook.chapters) { idx, chapter ->
                            val isCurrent = idx == currentChapterIndex
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isCurrent) currentTheme.accent.copy(alpha = 0.18f) else Color.Transparent)
                                    .clickable {
                                        currentChapterIndex = idx
                                        currentPageInChapter = 0
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
                                    color = if (isCurrent) currentTheme.accent else currentTheme.text
                                )
                                Text(
                                    "Ch. ${idx + 1}",
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
    }
}
