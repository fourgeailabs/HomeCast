package com.example.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import kotlin.math.abs

/**
 * Procedural gradient palette generator for books, albums, and audiobooks
 * so that no media ever displays a blank or broken cover.
 */
object CoverPaletteGenerator {
    private val bookPalettes = listOf(
        listOf(Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFF334155)), // Midnight Slate
        listOf(Color(0xFF450A0A), Color(0xFF7F1D1D), Color(0xFF991B1B)), // Crimson Leather
        listOf(Color(0xFF14532D), Color(0xFF166534), Color(0xFF052E16)), // Emerald Cloth
        listOf(Color(0xFF1E1B4B), Color(0xFF312E81), Color(0xFF4338CA)), // Royal Indigo
        listOf(Color(0xFF78350F), Color(0xFF92400E), Color(0xFF451A03)), // Antique Amber
        listOf(Color(0xFF581C87), Color(0xFF6B21A8), Color(0xFF3B0764)), // Vintage Velvet
        listOf(Color(0xFF134E4A), Color(0xFF0F766E), Color(0xFF042F2E)), // Deep Teal
        listOf(Color(0xFF701A75), Color(0xFF86198F), Color(0xFF4A044E))  // Rich Plum
    )

    fun getPaletteForTitle(title: String): List<Color> {
        val hash = abs(title.hashCode())
        return bookPalettes[hash % bookPalettes.size]
    }
}

/**
 * High-performance, resilient Cover Art component with:
 * 1. Multi-URL caching & fallback
 * 2. Elegant procedural leather/cloth book cover styling when offline or placeholder
 * 3. Tactile spine highlights, gold foil framing, and readable typography
 */
@Composable
fun MediaCoverArt(
    title: String,
    authorOrArtist: String,
    coverUrl: String?,
    modifier: Modifier = Modifier,
    isBookAspectRatio: Boolean = true,
    genre: String = "Classic",
    cornerRadius: Dp = 12.dp
) {
    val context = LocalContext.current
    val palette = remember(title) { CoverPaletteGenerator.getPaletteForTitle(title) }
    
    // Resolve robust direct image URL
    val effectiveUrl = remember(coverUrl, title) {
        if (coverUrl.isNullOrBlank() || coverUrl == "placeholder") {
            // Check if we have standard Gutenberg / OpenLibrary mapped image
            resolveSmartCoverUrl(title, coverUrl)
        } else {
            coverUrl
        }
    }

    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .clip(shape)
            .shadow(6.dp, shape)
            .background(Brush.linearGradient(palette.take(2)))
    ) {
        if (!effectiveUrl.isNullOrBlank()) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(effectiveUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "$title cover",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            ) {
                val state = painter.state
                when (state) {
                    is AsyncImagePainter.State.Success -> {
                        SubcomposeAsyncImageContent()
                        
                        // Subtle book spine shadow on the left edge
                        if (isBookAspectRatio) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(10.dp)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent)
                                        )
                                    )
                            )
                        }
                    }
                    else -> {
                        // Procedural Artistic Fallback Cover
                        ProceduralBookCover(
                            title = title,
                            author = authorOrArtist,
                            genre = genre,
                            palette = palette,
                            isBook = isBookAspectRatio
                        )
                    }
                }
            }
        } else {
            ProceduralBookCover(
                title = title,
                author = authorOrArtist,
                genre = genre,
                palette = palette,
                isBook = isBookAspectRatio
            )
        }
    }
}

@Composable
fun ProceduralBookCover(
    title: String,
    author: String,
    genre: String,
    palette: List<Color>,
    isBook: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = palette,
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            )
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        // Decorative Gold/Silver Foil Inset Border
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = 1.dp,
                    color = Color(0xFFFDE047).copy(alpha = 0.35f),
                    shape = RoundedCornerShape(6.dp)
                )
                .padding(6.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Badge / Genre Icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                ) {
                    Icon(
                        imageVector = if (isBook) Icons.Default.MenuBook else Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color(0xFFFDE047).copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = genre.uppercase().take(12),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = Color(0xFFFDE047).copy(alpha = 0.85f),
                        maxLines = 1
                    )
                }

                // Middle: Prominent Embossed Title
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 2.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = if (title.length > 25) 11.sp else 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Serif,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 15.sp
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Box(
                        modifier = Modifier
                            .width(28.dp)
                            .height(1.dp)
                            .background(Color(0xFFFDE047).copy(alpha = 0.6f))
                    )
                }

                // Bottom: Author / Creator
                Text(
                    text = author.take(24),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Serif,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }

        // Left Spine Shadow for 3D depth
        if (isBook) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(8.dp)
                    .align(Alignment.CenterStart)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent)
                        )
                    )
            )
        }
    }
}

/**
 * Intelligent helper to resolve reliable cover images for public domain and classic literature
 */
fun resolveSmartCoverUrl(title: String, originalUrl: String?): String? {
    if (!originalUrl.isNullOrBlank() && originalUrl != "placeholder") {
        return originalUrl
    }
    
    val clean = title.trim().lowercase()
    return when {
        clean.contains("time machine") -> "https://www.gutenberg.org/cache/epub/35/pg35.cover.medium.jpg"
        clean.contains("frankenstein") -> "https://www.gutenberg.org/cache/epub/84/pg84.cover.medium.jpg"
        clean.contains("art of war") -> "https://www.gutenberg.org/cache/epub/132/pg132.cover.medium.jpg"
        clean.contains("great gatsby") -> "https://www.gutenberg.org/cache/epub/64317/pg64317.cover.medium.jpg"
        clean.contains("metamorphosis") -> "https://www.gutenberg.org/cache/epub/5200/pg5200.cover.medium.jpg"
        clean.contains("dracula") -> "https://www.gutenberg.org/cache/epub/345/pg345.cover.medium.jpg"
        clean.contains("sherlock holmes") -> "https://www.gutenberg.org/cache/epub/1661/pg1661.cover.medium.jpg"
        clean.contains("pride and prejudice") -> "https://www.gutenberg.org/cache/epub/1342/pg1342.cover.medium.jpg"
        clean.contains("alice") -> "https://www.gutenberg.org/cache/epub/11/pg11.cover.medium.jpg"
        clean.contains("moby dick") -> "https://www.gutenberg.org/cache/epub/2701/pg2701.cover.medium.jpg"
        clean.contains("dorian gray") -> "https://www.gutenberg.org/cache/epub/174/pg174.cover.medium.jpg"
        clean.contains("war and peace") -> "https://www.gutenberg.org/cache/epub/2600/pg2600.cover.medium.jpg"
        clean.contains("odyssey") -> "https://www.gutenberg.org/cache/epub/1727/pg1727.cover.medium.jpg"
        clean.contains("iliad") -> "https://www.gutenberg.org/cache/epub/6130/pg6130.cover.medium.jpg"
        clean.contains("meditations") -> "https://www.gutenberg.org/cache/epub/2680/pg2680.cover.medium.jpg"
        clean.contains("prince") -> "https://www.gutenberg.org/cache/epub/1232/pg1232.cover.medium.jpg"
        clean.contains("tale of two cities") -> "https://www.gutenberg.org/cache/epub/98/pg98.cover.medium.jpg"
        clean.contains("crime and punishment") -> "https://www.gutenberg.org/cache/epub/2554/pg2554.cover.medium.jpg"
        clean.contains("jane eyre") -> "https://www.gutenberg.org/cache/epub/1260/pg1260.cover.medium.jpg"
        clean.contains("wuthering heights") -> "https://www.gutenberg.org/cache/epub/768/pg768.cover.medium.jpg"
        clean.contains("yellow wallpaper") -> "https://www.gutenberg.org/cache/epub/1952/pg1952.cover.medium.jpg"
        clean.contains("call of the wild") -> "https://www.gutenberg.org/cache/epub/215/pg215.cover.medium.jpg"
        clean.contains("jekyll") -> "https://www.gutenberg.org/cache/epub/43/pg43.cover.medium.jpg"
        clean.contains("twenty thousand leagues") -> "https://www.gutenberg.org/cache/epub/164/pg164.cover.medium.jpg"
        clean.contains("treasure island") -> "https://www.gutenberg.org/cache/epub/120/pg120.cover.medium.jpg"
        clean.contains("war of the worlds") -> "https://www.gutenberg.org/cache/epub/36/pg36.cover.medium.jpg"
        else -> null
    }
}
