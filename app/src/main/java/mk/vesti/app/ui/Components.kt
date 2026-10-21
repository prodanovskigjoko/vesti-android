package mk.vesti.app.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import mk.vesti.app.ui.theme.accent
import mk.vesti.core.Category
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.absoluteValue

val Macedonian: Locale = Locale.forLanguageTag("mk")

/** Small uppercase label tinted with the category's accent colour. */
@Composable
fun CategoryPill(category: Category, onImage: Boolean = false) {
    val accent = category.accent()
    Text(
        category.label.uppercase(Macedonian),
        style = MaterialTheme.typography.labelSmall,
        color = if (onImage) Color.White else accent,
        modifier = Modifier
            .clip(CircleShape)
            .background(if (onImage) accent else accent.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/** Overlapping initials of the outlets covering a story, like participant avatars in a chat. */
@Composable
fun SourceAvatars(sources: List<String>, max: Int = 4, size: Dp = 22.dp) {
    val shown = sources.distinct().take(max)
    val ring = MaterialTheme.colorScheme.surfaceContainerLow
    Box {
        shown.forEachIndexed { i, name ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(start = size * 0.66f * i)
                    .size(size)
                    .clip(CircleShape)
                    .background(sourceColor(name))
                    .border(1.5.dp, ring, CircleShape),
            ) {
                Text(
                    name.first().uppercase(Macedonian),
                    color = Color.White,
                    fontSize = (size.value * 0.45f).sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

private val avatarPalette = listOf(
    0xFFE63946, 0xFF457B9D, 0xFF2A9D8F, 0xFFF4A261, 0xFF6D597A, 0xFF1D3557, 0xFFE76F51, 0xFF8AB17D,
).map { Color(it) }

fun sourceColor(name: String): Color = avatarPalette[name.hashCode().absoluteValue % avatarPalette.size]

/** Network image with a soft placeholder, so cards keep their shape while loading or on failure. */
@Composable
fun NewsImage(url: String?, modifier: Modifier = Modifier) {
    val fill = MaterialTheme.colorScheme.surfaceContainerHigh
    if (url == null) {
        Box(modifier.background(fill))
    } else {
        SubcomposeAsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier,
            loading = { Box(Modifier.fillMaxSize().background(fill)) },
            error = { Box(Modifier.fillMaxSize().background(fill)) },
        )
    }
}

/** Pulsing grey blocks shaped like story cards, shown on first load. */
@Composable
fun SkeletonFeed() {
    val pulse by rememberInfiniteTransition(label = "skeleton").animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "alpha",
    )
    val block = MaterialTheme.colorScheme.surfaceContainerHigh
    Column(Modifier.alpha(pulse).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(Modifier.fillMaxWidth().height(280.dp).clip(MaterialTheme.shapes.large).background(block))
        repeat(4) {
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Box(Modifier.width(80.dp).height(14.dp).clip(CircleShape).background(block))
                    Spacer(Modifier.height(10.dp))
                    Box(Modifier.fillMaxWidth().height(16.dp).clip(CircleShape).background(block))
                    Spacer(Modifier.height(6.dp))
                    Box(Modifier.fillMaxWidth(0.7f).height(16.dp).clip(CircleShape).background(block))
                }
                Spacer(Modifier.width(12.dp))
                Box(Modifier.size(88.dp).clip(MaterialTheme.shapes.medium).background(block))
            }
        }
    }
}

private val dayFormat = DateTimeFormatter.ofPattern("EEEE, d MMMM", Macedonian)
private val shortDate = DateTimeFormatter.ofPattern("d MMM", Macedonian)

fun todayLabel(): String =
    Instant.now().atZone(ZoneId.systemDefault()).format(dayFormat).replaceFirstChar { it.titlecase(Macedonian) }

/** "пред 5 мин", "пред 3 ч", "вчера", or a short date. Always Macedonian, whatever the phone's language. */
fun relativeTime(millis: Long, now: Long = System.currentTimeMillis()): String {
    if (millis == 0L) return ""
    val minutes = (now - millis) / 60_000
    return when {
        minutes < 1 -> "сега"
        minutes < 60 -> "пред $minutes мин"
        minutes < 24 * 60 -> "пред ${minutes / 60} ч"
        minutes < 48 * 60 -> "вчера"
        else -> Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(shortDate)
    }
}

/** Star used for "Зачувај за подоцна": filled when the article is saved. */
@Composable
fun SavedIcon(saved: Boolean, tint: Color) {
    Icon(
        if (saved) Icons.Filled.Star else Icons.Outlined.Star,
        contentDescription = if (saved) "Отстрани од зачувани" else "Зачувај за подоцна",
        tint = tint,
    )
}
