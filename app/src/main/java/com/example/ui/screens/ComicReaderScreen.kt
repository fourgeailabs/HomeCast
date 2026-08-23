package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.ComicContentFetcher
import com.example.ui.theme.AccentIndigo
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.SurfaceGlass
import kotlinx.coroutines.launch

data class ComicPage(
    val pageNumber: Int,
    val fullPageArtUrl: String = "",
    val pageTitle: String = ""
)

data class ComicData(
    val id: String,
    val title: String,
    val series: String = "",
    val issueNumber: String = "#1",
    val writer: String = "Unknown",
    val artist: String = "Unknown",
    val coverUrl: String = "",
    val downloadUrl: String = "",
    val pageCount: Int = 0,
    val serverHostUrl: String = "",
    val serverApiKey: String = "",
    val pages: List<ComicPage> = emptyList()
)

enum class ComicReadingMode {
    WESTERN_LTR,
    MANGA_RTL,
    WEBTOON_VERTICAL
}

enum class ComicScaleMode {
    FIT_SCREEN,
    FIT_WIDTH
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComicReaderScreen(
    comic: ComicData,
    onClose: () -> Unit,
    onSwitchToNovel: (() -> Unit)? = null,
    viewModel: com.example.ui.MainViewModel? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var pages by remember { mutableStateOf(comic.pages) }
    var isLoading by remember { mutableStateOf(true) }
    var currentPageIndex by remember { mutableIntStateOf(0) }
    var readingMode by remember { mutableStateOf(ComicReadingMode.WESTERN_LTR) }
    var scaleMode by remember { mutableStateOf(ComicScaleMode.FIT_SCREEN) }
    var showHud by remember { mutableStateOf(true) }
    var showThumbnails by remember { mutableStateOf(false) }

    // Load saved reading progress from JSON backup / database
    LaunchedEffect(comic.id) {
        val saved = viewModel?.loadComicProgress(comic.id)
            ?: com.example.data.SettingsBackupManager(context).getMediaProgress(comic.id)
        if (saved != null && saved.currentPage >= 0) {
            currentPageIndex = saved.currentPage
        }
    }

    // Helper to persist current comic reading progress
    fun persistComicProgress(pageIdx: Int) {
        val total = pages.size.coerceAtLeast(1)
        val safeIdx = pageIdx.coerceIn(0, total - 1)
        if (viewModel != null) {
            viewModel.saveComicProgress(
                id = comic.id,
                title = comic.title,
                writer = comic.writer,
                pageIndex = safeIdx,
                totalPages = total
            )
        } else {
            val progressPercent = (((safeIdx + 1).toFloat() / total.toFloat()) * 100).toInt().coerceIn(0, 100)
            val backupManager = com.example.data.SettingsBackupManager(context)
            backupManager.saveMediaProgress(
                com.example.data.MediaProgress(
                    id = comic.id,
                    type = "COMIC",
                    title = comic.title,
                    creator = comic.writer,
                    currentPage = safeIdx,
                    totalPages = total,
                    progressPercent = progressPercent,
                    lastUpdated = System.currentTimeMillis()
                )
            )
        }
    }

    // Save reading progress on exit / back / screen disposal
    DisposableEffect(currentPageIndex, pages.size) {
        onDispose {
            persistComicProgress(currentPageIndex)
        }
    }

    // Fetch real comic pages if none provided
    LaunchedEffect(comic) {
        if (comic.pages.isNotEmpty()) {
            pages = comic.pages
            isLoading = false
        } else {
            isLoading = true
            val fetchedPages = ComicContentFetcher.fetchComicPages(
                context = context,
                comicId = comic.id,
                title = comic.title,
                downloadUrl = comic.downloadUrl,
                coverUrl = comic.coverUrl,
                pageCount = comic.pageCount,
                serverHostUrl = comic.serverHostUrl,
                serverApiKey = comic.serverApiKey
            )
            pages = if (fetchedPages.isNotEmpty()) {
                fetchedPages
            } else if (comic.coverUrl.isNotBlank()) {
                listOf(ComicPage(1, comic.coverUrl, "Cover"))
            } else {
                emptyList()
            }
            isLoading = false
        }
    }

    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF090D16)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Color(0xFF38BDF8))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Loading comic pages from server...", color = Color.White, fontSize = 14.sp)
            }
        }
        return
    }

    if (pages.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF090D16)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(Icons.Default.BrokenImage, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("No comic pages found", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Unable to load comic image pages from the server.", color = Color.Gray, fontSize = 13.sp, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onClose, colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)) {
                    Text("Close", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
        return
    }

    val safeCurrentPage = pages.getOrElse(currentPageIndex) { pages.first() }

    // Multi-touch Zoom & Pan state
    var manualScale by remember { mutableFloatStateOf(1f) }
    var manualOffset by remember { mutableStateOf(Offset.Zero) }

    // AI Guided Cinematic Panel Mode
    var isCinematicMode by remember { mutableStateOf(false) }
    var currentPanelIndex by remember { mutableIntStateOf(0) } // 0..3 panels

    fun applyCinematicPanelZoom(panelIdx: Int) {
        val targetScale = 2.2f
        val offsetMap = mapOf(
            0 to Offset(180f, 240f),   // Panel 1: Top-Left
            1 to Offset(-180f, 240f),  // Panel 2: Top-Right
            2 to Offset(180f, -240f),  // Panel 3: Bottom-Left
            3 to Offset(-180f, -240f)  // Panel 4: Bottom-Right
        )
        manualScale = targetScale
        manualOffset = offsetMap[panelIdx] ?: Offset.Zero
    }

    // Page Slide Transition
    val slideAnim = remember { Animatable(0f) }

    fun goToNextPage() {
        if (isCinematicMode) {
            if (currentPanelIndex < 3) {
                currentPanelIndex += 1
                applyCinematicPanelZoom(currentPanelIndex)
                return
            } else {
                currentPanelIndex = 0
            }
        }
        if (currentPageIndex < pages.size - 1) {
            coroutineScope.launch {
                manualScale = if (isCinematicMode) 2.2f else 1f
                manualOffset = Offset.Zero
                slideAnim.snapTo(if (readingMode == ComicReadingMode.MANGA_RTL) -150f else 150f)
                val newPage = currentPageIndex + 1
                currentPageIndex = newPage
                persistComicProgress(newPage)
                if (isCinematicMode) applyCinematicPanelZoom(0)
                slideAnim.animateTo(0f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow))
            }
        }
    }

    fun goToPrevPage() {
        if (isCinematicMode) {
            if (currentPanelIndex > 0) {
                currentPanelIndex -= 1
                applyCinematicPanelZoom(currentPanelIndex)
                return
            } else {
                currentPanelIndex = 3
            }
        }
        if (currentPageIndex > 0) {
            coroutineScope.launch {
                manualScale = if (isCinematicMode) 2.2f else 1f
                manualOffset = Offset.Zero
                slideAnim.snapTo(if (readingMode == ComicReadingMode.MANGA_RTL) 150f else -150f)
                val newPage = currentPageIndex - 1
                currentPageIndex = newPage
                persistComicProgress(newPage)
                if (isCinematicMode) applyCinematicPanelZoom(3)
                slideAnim.animateTo(0f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow))
            }
        }
    }

    fun handleTap(tapOffset: Offset, screenWidth: Float) {
        when {
            tapOffset.x < screenWidth * 0.28f -> {
                if (readingMode == ComicReadingMode.MANGA_RTL) goToNextPage() else goToPrevPage()
            }
            tapOffset.x > screenWidth * 0.72f -> {
                if (readingMode == ComicReadingMode.MANGA_RTL) goToPrevPage() else goToNextPage()
            }
            else -> {
                showHud = !showHud
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF07090E))
    ) {
        // --- COMIC DISPLAY AREA ---
        if (readingMode == ComicReadingMode.WEBTOON_VERTICAL) {
            // Continuous Vertical Webtoon Scroll
            val listState = rememberLazyListState(initialFirstVisibleItemIndex = currentPageIndex)
            
            // Sync page index with scroll position
            LaunchedEffect(listState.firstVisibleItemIndex) {
                currentPageIndex = listState.firstVisibleItemIndex
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { showHud = !showHud })
                    },
                contentPadding = PaddingValues(vertical = if (showHud) 72.dp else 0.dp)
            ) {
                itemsIndexed(pages) { index, page ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(page.fullPageArtUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Page ${index + 1}",
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.FillWidth
                        )
                    }
                }
            }
        } else {
            // Single Page Flip (Western LTR or Manga RTL)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(readingMode, currentPageIndex) {
                        detectTapGestures(
                            onDoubleTap = {
                                manualScale = if (manualScale > 1.2f) 1f else 2.2f
                                manualOffset = Offset.Zero
                            },
                            onTap = { tapOffset ->
                                handleTap(tapOffset, size.width.toFloat())
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            manualScale = (manualScale * zoom).coerceIn(0.9f, 4.5f)
                            manualOffset = if (manualScale > 1f) {
                                Offset(
                                    x = manualOffset.x + pan.x,
                                    y = manualOffset.y + pan.y
                                )
                            } else {
                                Offset.Zero
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = if (showHud) 64.dp else 0.dp,
                            bottom = if (showHud) 90.dp else 0.dp
                        )
                        .graphicsLayer {
                            translationX = slideAnim.value + manualOffset.x
                            translationY = manualOffset.y
                            scaleX = manualScale
                            scaleY = manualScale
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(safeCurrentPage.fullPageArtUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Page ${safeCurrentPage.pageNumber}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = if (scaleMode == ComicScaleMode.FIT_WIDTH) ContentScale.FillWidth else ContentScale.Fit
                    )
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
                color = Color(0xFF0F172A).copy(alpha = 0.94f),
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Close Comic Reader", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(
                                comic.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                if (comic.writer.isNotBlank() && comic.writer != "Unknown") "${comic.writer} • Page ${currentPageIndex + 1}/${pages.size}" else "Page ${currentPageIndex + 1} of ${pages.size}",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.7f),
                                maxLines = 1
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // AI Guided Cinematic Panel Toggle Button
                        Button(
                            onClick = {
                                isCinematicMode = !isCinematicMode
                                if (isCinematicMode) {
                                    currentPanelIndex = 0
                                    applyCinematicPanelZoom(0)
                                } else {
                                    manualScale = 1f
                                    manualOffset = Offset.Zero
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isCinematicMode) Color(0xFF38BDF8) else Color(0xFF1E293B),
                                contentColor = if (isCinematicMode) Color.Black else Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.Movie, contentDescription = "Cinematic Mode", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cinematic", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Fit Mode Toggle
                        IconButton(
                            onClick = {
                                scaleMode = if (scaleMode == ComicScaleMode.FIT_SCREEN) ComicScaleMode.FIT_WIDTH else ComicScaleMode.FIT_SCREEN
                            }
                        ) {
                            Icon(
                                if (scaleMode == ComicScaleMode.FIT_WIDTH) Icons.Default.FitScreen else Icons.Default.ZoomOutMap,
                                contentDescription = "Scale Mode",
                                tint = if (scaleMode == ComicScaleMode.FIT_WIDTH) Color(0xFF38BDF8) else Color.White
                            )
                        }

                        // Reading Mode Switcher Dropdown/Button
                        IconButton(
                            onClick = {
                                readingMode = when (readingMode) {
                                    ComicReadingMode.WESTERN_LTR -> ComicReadingMode.MANGA_RTL
                                    ComicReadingMode.MANGA_RTL -> ComicReadingMode.WEBTOON_VERTICAL
                                    ComicReadingMode.WEBTOON_VERTICAL -> ComicReadingMode.WESTERN_LTR
                                }
                            }
                        ) {
                            Icon(
                                when (readingMode) {
                                    ComicReadingMode.WESTERN_LTR -> Icons.Default.East
                                    ComicReadingMode.MANGA_RTL -> Icons.Default.West
                                    ComicReadingMode.WEBTOON_VERTICAL -> Icons.Default.UnfoldMore
                                },
                                contentDescription = "Reading Mode",
                                tint = Color(0xFF38BDF8)
                            )
                        }

                        // Thumbnails toggle
                        IconButton(onClick = { showThumbnails = !showThumbnails }) {
                            Icon(
                                Icons.Default.ViewCarousel,
                                contentDescription = "Thumbnails",
                                tint = if (showThumbnails) Color(0xFF38BDF8) else Color.White
                            )
                        }
                    }
                }
            }
        }

        // --- BOTTOM COMIC HUD & SCRUBBER ---
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
                // Optional Thumbnail Carousel Strip
                AnimatedVisibility(visible = showThumbnails) {
                    Surface(
                        color = Color(0xFF0F172A).copy(alpha = 0.96f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(pages) { idx, page ->
                                val isSelected = idx == currentPageIndex
                                Box(
                                    modifier = Modifier
                                        .size(54.dp, 76.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .border(
                                            width = if (isSelected) 2.5.dp else 1.dp,
                                            color = if (isSelected) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .background(Color(0xFF1E293B))
                                        .clickable {
                                            currentPageIndex = idx
                                            manualScale = 1f
                                            manualOffset = Offset.Zero
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = page.fullPageArtUrl,
                                        contentDescription = "Thumb ${idx + 1}",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.65f),
                                        shape = RoundedCornerShape(topStart = 4.dp),
                                        modifier = Modifier.align(Alignment.BottomEnd)
                                    ) {
                                        Text(
                                            "${idx + 1}",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
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
                            // Left Nav Action
                            FilledTonalIconButton(
                                onClick = {
                                    if (readingMode == ComicReadingMode.MANGA_RTL) goToNextPage() else goToPrevPage()
                                },
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = Color(0xFF1E293B),
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    if (readingMode == ComicReadingMode.MANGA_RTL) Icons.Default.ArrowForward else Icons.Default.ArrowBack,
                                    contentDescription = "Previous Page"
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "Page ${currentPageIndex + 1} of ${pages.size}",
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
                                            when (readingMode) {
                                                ComicReadingMode.WESTERN_LTR -> "WESTERN LTR"
                                                ComicReadingMode.MANGA_RTL -> "MANGA RTL"
                                                ComicReadingMode.WEBTOON_VERTICAL -> "WEBTOON SCROLL"
                                            },
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF38BDF8),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    when (readingMode) {
                                        ComicReadingMode.WESTERN_LTR -> "Tap right for next page • Double tap to zoom"
                                        ComicReadingMode.MANGA_RTL -> "Manga Mode: Tap left for next page"
                                        ComicReadingMode.WEBTOON_VERTICAL -> "Vertical scroll enabled"
                                    },
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }

                            // Right Nav Action
                            FilledIconButton(
                                onClick = {
                                    if (readingMode == ComicReadingMode.MANGA_RTL) goToPrevPage() else goToNextPage()
                                },
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = Color(0xFF0284C7),
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    if (readingMode == ComicReadingMode.MANGA_RTL) Icons.Default.ArrowBack else Icons.Default.ArrowForward,
                                    contentDescription = "Next Page"
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Page Scrub Slider
                        Slider(
                            value = currentPageIndex.toFloat(),
                            onValueChange = {
                                currentPageIndex = it.toInt().coerceIn(0, pages.size - 1)
                                manualScale = 1f
                                manualOffset = Offset.Zero
                            },
                            valueRange = 0f..(pages.size - 1).coerceAtLeast(1).toFloat(),
                            steps = (pages.size - 2).coerceAtLeast(0),
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
