package mk.vesti.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mk.vesti.app.data.Fuel
import mk.vesti.app.data.Rate
import mk.vesti.app.data.Weather
import mk.vesti.app.data.Widgets
import mk.vesti.app.data.outageWhen
import mk.vesti.app.data.weatherLabel
import mk.vesti.core.OutageMatch
import java.time.format.DateTimeFormatter
import java.util.Locale

private val CardWidth = 210.dp
private val CardHeight = 140.dp

/** Weather, power cuts, fuel prices and exchange rates, side by side above the stories. */
@Composable
fun WidgetsRow(widgets: Widgets, outages: List<OutageMatch>, hasAddresses: Boolean, onOpenOutages: () -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { WeatherCard(widgets.weather) }
        item { PowerCard(outages, hasAddresses, onOpenOutages) }
        widgets.fuel?.let { fuel -> item { FuelCard(fuel) } }
        if (widgets.rates.isNotEmpty()) item { RatesCard(widgets.rates) }
    }
}

@Composable
private fun WidgetCard(background: Brush, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    Box(
        Modifier
            .width(CardWidth)
            .height(CardHeight)
            .clip(MaterialTheme.shapes.medium)
            .background(background)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
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
private fun PowerCard(outages: List<OutageMatch>, hasAddresses: Boolean, onClick: () -> Unit) {
    WidgetCard(Brush.linearGradient(listOf(Color(0xFF434343), Color(0xFF1F1C2C))), onClick) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Text("⚡ ПРЕКИНИ НА СТРУЈА", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelSmall)
            val next = outages.firstOrNull()
            when {
                !hasAddresses -> Text(
                    "Додај ја твојата адреса и ќе те известиме ден однапред",
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                )
                next == null -> Text("Нема планирани прекини на твоите адреси", color = Color.White, style = MaterialTheme.typography.titleSmall)
                else -> Column {
                    Text(next.address.label, color = Color(0xFFFFC857), fontWeight = FontWeight.Bold)
                    Text(
                        outageWhen(next.outage).replaceFirstChar { it.uppercase() },
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Text(
                when {
                    !hasAddresses -> "Додај адреса ›"
                    outages.size > 1 -> "Уште ${outages.size - 1} ›"
                    else -> "Мои адреси ›"
                },
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun FuelCard(fuel: Fuel) {
    WidgetCard(Brush.linearGradient(listOf(Color(0xFFF7971E), Color(0xFFE5532D)))) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Text(
                "ГОРИВА · ОД ${fuel.validFrom.format(fuelDate)}",
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.labelSmall,
            )
            fuel.prices.take(4).forEach { p ->
                Row(Modifier.fillMaxWidth()) {
                    Text(p.name, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text(String.format(Locale.US, "%.2f", p.price), color = Color.White)
                }
            }
        }
    }
}

private val fuelDate = DateTimeFormatter.ofPattern("d.M.")

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
