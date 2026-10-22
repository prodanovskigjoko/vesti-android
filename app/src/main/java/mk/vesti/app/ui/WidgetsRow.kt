package mk.vesti.app.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mk.vesti.app.data.Match
import mk.vesti.app.data.Rate
import mk.vesti.app.data.Weather
import mk.vesti.app.data.Widgets
import mk.vesti.app.data.weatherLabel
import java.util.Locale

private val CardWidth = 210.dp
private val CardHeight = 140.dp

/** Weather, exchange rates and live scores, side by side above the stories. */
@Composable
fun WidgetsRow(widgets: Widgets) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { WeatherCard(widgets.weather) }
        if (widgets.matches.isNotEmpty()) item { SportCard(widgets.matches, widgets.sportIsDemo) }
        if (widgets.rates.isNotEmpty()) item { RatesCard(widgets.rates) }
    }
}

@Composable
private fun WidgetCard(background: Brush, content: @Composable () -> Unit) {
    Box(
        Modifier
            .width(CardWidth)
            .height(CardHeight)
            .clip(MaterialTheme.shapes.medium)
            .background(background)
            .padding(14.dp),
    ) { content() }
}

@Composable
private fun WeatherCard(weather: Weather?) {
    WidgetCard(Brush.linearGradient(listOf(Color(0xFF3A7BD5), Color(0xFF00C6FF)))) {
        if (weather == null) {
            Text("Временска прогноза…", color = Color.White, style = MaterialTheme.typography.labelLarge)
            return@WidgetCard
        }
        val (icon, label) = weatherLabel(weather.code)
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Text(weather.city.uppercase(Macedonian), color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelSmall)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${weather.temperature}°", color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Light)
                Spacer(Modifier.width(8.dp))
                Text(icon, fontSize = 30.sp)
            }
            Text(
                "$label · ${weather.min}° / ${weather.max}°",
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun RatesCard(rates: List<Rate>) {
    WidgetCard(Brush.linearGradient(listOf(Color(0xFF11998E), Color(0xFF38EF7D)))) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Text("КУРСНА ЛИСТА · МКД", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelSmall)
            rates.take(4).forEach { r ->
                Row(Modifier.fillMaxWidth()) {
                    Text(r.currency, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text(String.format(Locale.US, "%.2f", r.denars), color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun SportCard(matches: List<Match>, demo: Boolean) {
    WidgetCard(Brush.linearGradient(listOf(Color(0xFF1F1C2C), Color(0xFF3A3557)))) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LiveDot()
                Spacer(Modifier.width(6.dp))
                Text(
                    if (demo) "ВО ЖИВО · ДЕМО" else "ВО ЖИВО",
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            matches.take(3).forEach { MatchLine(it) }
        }
    }
}

@Composable
private fun MatchLine(m: Match) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            "${m.home} – ${m.away}",
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(6.dp))
        val score = if (m.homeScore != null && m.awayScore != null) "${m.homeScore}:${m.awayScore}" else ""
        Text(score, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Spacer(Modifier.width(6.dp))
        Text(
            m.status,
            color = if (m.live) Color(0xFFFF5252) else Color.White.copy(alpha = 0.7f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun LiveDot() {
    val alpha by rememberInfiniteTransition(label = "live").animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "alpha",
    )
    Box(Modifier.size(8.dp).alpha(alpha).clip(CircleShape).background(Color(0xFFFF5252)))
}
