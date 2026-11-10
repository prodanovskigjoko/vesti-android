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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mk.vesti.app.data.City
import mk.vesti.app.data.Fuel
import mk.vesti.app.data.WeatherCities
import mk.vesti.app.data.Rate
import mk.vesti.app.data.Weather
import mk.vesti.app.data.Widgets
import mk.vesti.app.data.weatherLabel
import java.time.format.DateTimeFormatter
import java.util.Locale

private val CardWidth = 210.dp
private val CardHeight = 140.dp

/** Weather for each chosen town, fuel prices and exchange rates, side by side above the stories. */
@Composable
fun WidgetsRow(widgets: Widgets, cities: List<City>, onToggleCity: (City) -> Unit) {
    var choosing by rememberSaveable { mutableStateOf(false) }
    if (choosing) CityPicker(cities, onToggleCity, onDismiss = { choosing = false })

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(cities, key = { "w:" + it.name }) { city ->
            WeatherCard(city.name, widgets.weather.firstOrNull { it.city == city.name }, onClick = { choosing = true })
        }
        item(key = "add-city") { AddCityCard(onClick = { choosing = true }) }
        widgets.fuel?.let { fuel -> item { FuelCard(fuel) } }
        if (widgets.rates.isNotEmpty()) item { RatesCard(widgets.rates) }
    }
}

@Composable
private fun WidgetCard(
    background: Brush,
    onClick: (() -> Unit)? = null,
    width: Dp = CardWidth,
    content: @Composable () -> Unit,
) {
    Box(
        Modifier
            .width(width)
            .height(CardHeight)
            .clip(MaterialTheme.shapes.medium)
            .background(background)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(14.dp),
    ) { content() }
}

@Composable
private fun WeatherCard(city: String, weather: Weather?, onClick: () -> Unit) {
    WidgetCard(WeatherBrush, onClick) {
        if (weather == null) {
            Column {
                Text(city.uppercase(Macedonian), color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(8.dp))
                Text("Временска прогноза…", color = Color.White, style = MaterialTheme.typography.labelLarge)
            }
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

private val WeatherBrush = Brush.linearGradient(listOf(Color(0xFF3A7BD5), Color(0xFF00C6FF)))

@Composable
private fun AddCityCard(onClick: () -> Unit) {
    WidgetCard(WeatherBrush, onClick, width = 96.dp) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text("+", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Light)
            Text("Додај град", color = Color.White, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** Tick towns to add a weather card for them, untick to remove; at least one stays. */
@Composable
private fun CityPicker(selected: List<City>, onToggle: (City) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Готово") } },
        title = { Text("Градови за временската прогноза") },
        text = {
            LazyColumn(Modifier.height(380.dp)) {
                items(WeatherCities, key = { it.name }) { city ->
                    val on = city in selected
                    Row(
                        Modifier.fillMaxWidth().clickable(enabled = !(on && selected.size == 1)) { onToggle(city) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = on, onCheckedChange = { onToggle(city) }, enabled = !(on && selected.size == 1))
                        Text(city.name, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
    )
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
