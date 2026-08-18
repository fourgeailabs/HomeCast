package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.AccentIndigo
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.SurfaceGlass
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class ComicFrame(
    val id: String,
    val frameNumber: Int,
    val title: String,
    val speaker: String = "",
    val dialogue: String = "",
    val sfx: String = "",
    val artUrl: String = "",
    val gradientColors: List<Color> = listOf(Color(0xFF1E1B4B), Color(0xFF0F172A)),
    val relativeBounds: Rect = Rect(0f, 0f, 1f, 1f) // Normalized x, y, w, h on page
)

data class ComicPage(
    val pageNumber: Int,
    val fullPageArtUrl: String = "",
    val pageTitle: String = "",
    val frames: List<ComicFrame>
)

data class ComicData(
    val id: String,
    val title: String,
    val series: String,
    val issueNumber: String,
    val writer: String,
    val artist: String,
    val coverUrl: String,
    val pages: List<ComicPage>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComicReaderScreen(
    comic: ComicData,
    onClose: () -> Unit,
    onSwitchToNovel: (() -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()

    var currentPageIndex by remember { mutableStateOf(0) }
    var currentFrameIndex by remember { mutableStateOf(0) }
    var isGuidedPanelMode by remember { mutableStateOf(true) } // Guided Panel Zoom View
    var isMangaMode by remember { mutableStateOf(false) } // Right-to-Left vs Left-to-Right
    var showHud by remember { mutableStateOf(true) }
    var showFrameCarousel by remember { mutableStateOf(false) }

    val currentPage = comic.pages.getOrElse(currentPageIndex) { comic.pages.first() }
    val currentFrame = currentPage.frames.getOrElse(currentFrameIndex) { currentPage.frames.first() }

    // Smooth transition animatables for frame slide & zoom
    val slideAnimOffsetX = remember { Animatable(0f) }
    val zoomAnimScale = remember { Animatable(1f) }
    var manualScale by remember { mutableStateOf(1f) }
    var manualOffset by remember { mutableStateOf(Offset.Zero) }

    // Navigation functions with smooth slide-in / flick transition
    fun nextFrame() {
        coroutineScope.launch {
            // Slide in animation style
            slideAnimOffsetX.snapTo(if (isMangaMode) -100f else 100f)
            if (currentFrameIndex < currentPage.frames.size - 1) {
                currentFrameIndex++
            } else if (currentPageIndex < comic.pages.size - 1) {
                currentPageIndex++
                currentFrameIndex = 0
            }
            manualScale = 1f
            manualOffset = Offset.Zero
            slideAnimOffsetX.animateTo(
                0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }

    fun prevFrame() {
        coroutineScope.launch {
            // Flick back animation style
            slideAnimOffsetX.snapTo(if (isMangaMode) 100f else -100f)
            if (currentFrameIndex > 0) {
                currentFrameIndex--
            } else if (currentPageIndex > 0) {
                currentPageIndex--
                val prevPage = comic.pages[currentPageIndex]
                currentFrameIndex = prevPage.frames.size - 1
            }
            manualScale = 1f
            manualOffset = Offset.Zero
            slideAnimOffsetX.animateTo(
                0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }

    fun handleRightSideTap() {
        if (isMangaMode) prevFrame() else nextFrame()
    }

    fun handleLeftSideTap() {
        if (isMangaMode) nextFrame() else prevFrame()
    }

    fun toggleZoomFrame() {
        coroutineScope.launch {
            val target = if (zoomAnimScale.value > 1.05f) 1.0f else 1.35f
            zoomAnimScale.animateTo(
                target,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF07090E))
    ) {
        // --- COMIC FRAME & PAGE CANVAS VIEW ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isMangaMode, currentFrameIndex, currentPageIndex, isGuidedPanelMode) {
                    detectTapGestures(
                        onDoubleTap = {
                            toggleZoomFrame()
                        },
                        onTap = { tapOffset ->
                            val screenWidth = size.width
                            when {
                                tapOffset.x < screenWidth * 0.3f -> handleLeftSideTap()
                                tapOffset.x > screenWidth * 0.7f -> handleRightSideTap()
                                else -> showHud = !showHud
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        manualScale = (manualScale * zoom).coerceIn(0.85f, 3.5f)
                        manualOffset = Offset(
                            x = manualOffset.x + pan.x,
                            y = manualOffset.y + pan.y
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Guided Panel Mode: Focused High-Res Frame with Slide-In Animation
            if (isGuidedPanelMode) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 64.dp)
                        .graphicsLayer {
                            translationX = slideAnimOffsetX.value + manualOffset.x
                            translationY = manualOffset.y
                            scaleX = zoomAnimScale.value * manualScale
                            scaleY = zoomAnimScale.value * manualScale
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.78f)
                            .shadow(20.dp, RoundedCornerShape(18.dp))
                            .border(2.dp, Color(0xFF38BDF8).copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Frame Artwork / Gradient Canvas
                            if (currentFrame.artUrl.isNotBlank()) {
                                AsyncImage(
                                    model = currentFrame.artUrl,
                                    contentDescription = currentFrame.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Brush.radialGradient(currentFrame.gradientColors)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.AutoStories,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.3f),
                                            modifier = Modifier.size(72.dp)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            currentFrame.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 20.sp,
                                            color = Color.White,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }

                            // Dynamic Sound Effect (SFX) Badge
                            if (currentFrame.sfx.isNotBlank()) {
                                Surface(
                                    color = Color(0xFFFFCC00),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(16.dp)
                                        .graphicsLayer { rotationZ = -8f }
                                ) {
                                    Text(
                                        currentFrame.sfx,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp,
                                        color = Color.Black,
                                        fontFamily = FontFamily.Cursive,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            // Frame Dialogue Speech Balloon Overlay
                            if (currentFrame.dialogue.isNotBlank()) {
                                Surface(
                                    color = Color.White.copy(alpha = 0.95f),
                                    shape = RoundedCornerShape(16.dp),
                                    shadowElevation = 8.dp,
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(16.dp)
                                        .fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        if (currentFrame.speaker.isNotBlank()) {
                                            Text(
                                                currentFrame.speaker.uppercase(),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFF0284C7),
                                                letterSpacing = 1.sp
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                        }
                                        Text(
                                            "“${currentFrame.dialogue}”",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF0F172A),
                                            lineHeight = 19.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Full Page View: Grid of Panels
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 68.dp)
                        .graphicsLayer {
                            translationX = manualOffset.x
                            translationY = manualOffset.y
                            scaleX = manualScale
                            scaleY = manualScale
                        },
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Page ${currentPage.pageNumber}: ${currentPage.pageTitle}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.7f)
                    )

                    currentPage.frames.chunked(2).forEachIndexed { rowIdx, rowFrames ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowFrames.forEachIndexed { colIdx, frame ->
                                val frameIndexInPage = rowIdx * 2 + colIdx
                                val isSelected = frameIndexInPage == currentFrameIndex

                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .border(
                                            width = if (isSelected) 2.5.dp else 1.dp,
                                            color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            currentFrameIndex = frameIndexInPage
                                            isGuidedPanelMode = true
                                        }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Brush.linearGradient(frame.gradientColors)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            frame.title,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- TOP COMIC HUD BAR ---
        AnimatedVisibility(
            visible = showHud,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                color = Color(0xFF0F172A).copy(alpha = 0.92f),
                shadowElevation = 10.dp,
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
                            Icon(Icons.Default.ArrowBack, contentDescription = "Close Comic Reader", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(
                                "${comic.series} #${comic.issueNumber}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White,
                                maxLines = 1
                            )
                            Text(
                                "${comic.writer} • ${comic.artist}",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (onSwitchToNovel != null) {
                            FilledTonalButton(
                                onClick = onSwitchToNovel,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color(0xFF0284C7).copy(alpha = 0.25f),
                                    contentColor = Color(0xFF38BDF8)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Book Mode", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        // Manga Reading Mode Toggle
                        IconButton(onClick = { isMangaMode = !isMangaMode }) {
                            Icon(
                                if (isMangaMode) Icons.Default.SwapHoriz else Icons.Default.East,
                                contentDescription = "Reading Direction",
                                tint = if (isMangaMode) Color(0xFF38BDF8) else Color.White
                            )
                        }

                        // Guided Panel vs Full Page Toggle
                        IconButton(onClick = { isGuidedPanelMode = !isGuidedPanelMode }) {
                            Icon(
                                if (isGuidedPanelMode) Icons.Default.ZoomInMap else Icons.Default.GridView,
                                contentDescription = "Toggle View Mode",
                                tint = if (isGuidedPanelMode) Color(0xFF38BDF8) else Color.White
                            )
                        }

                        // Frame Carousel Strip
                        IconButton(onClick = { showFrameCarousel = !showFrameCarousel }) {
                            Icon(Icons.Default.ViewCarousel, contentDescription = "Panel Carousel", tint = Color.White)
                        }
                    }
                }
            }
        }

        // --- BOTTOM COMIC HUD & FRAME NAVIGATOR ---
        AnimatedVisibility(
            visible = showHud,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                // Optional Panel Carousel Strip
                AnimatedVisibility(visible = showFrameCarousel) {
                    Surface(
                        color = Color(0xFF0F172A).copy(alpha = 0.95f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(currentPage.frames) { idx, frame ->
                                val isSelected = idx == currentFrameIndex
                                Box(
                                    modifier = Modifier
                                        .size(64.dp, 44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .background(Brush.linearGradient(frame.gradientColors))
                                        .clickable {
                                            currentFrameIndex = idx
                                            isGuidedPanelMode = true
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "F${idx + 1}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                Surface(
                    color = Color(0xFF0F172A).copy(alpha = 0.95f),
                    shadowElevation = 10.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left Nav Action (Flick Back)
                            FilledTonalIconButton(
                                onClick = { prevFrame() },
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = Color(0xFF1E293B),
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    if (isMangaMode) Icons.Default.ArrowForward else Icons.Default.ArrowBack,
                                    contentDescription = "Previous Frame"
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "Page ${currentPage.pageNumber} of ${comic.pages.size}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = Color(0xFF0284C7).copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            if (isGuidedPanelMode) "FRAME ${currentFrameIndex + 1}/${currentPage.frames.size}" else "FULL PAGE",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF38BDF8),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    if (isMangaMode) "Manga Mode (Right-to-Left)" else "Tap right to slide next • Tap left to flick back",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }

                            // Right Nav Action (Slide to Next Frame)
                            FilledIconButton(
                                onClick = { nextFrame() },
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = Color(0xFF0284C7),
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    if (isMangaMode) Icons.Default.ArrowBack else Icons.Default.ArrowForward,
                                    contentDescription = "Next Frame"
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Page Scrub Slider
                        Slider(
                            value = currentPageIndex.toFloat(),
                            onValueChange = {
                                currentPageIndex = it.toInt()
                                currentFrameIndex = 0
                            },
                            valueRange = 0f..(comic.pages.size - 1).coerceAtLeast(1).toFloat(),
                            steps = (comic.pages.size - 2).coerceAtLeast(0),
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF38BDF8),
                                activeTrackColor = Color(0xFF38BDF8),
                                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
