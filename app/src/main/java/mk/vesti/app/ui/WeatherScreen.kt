package mk.vesti.app.ui

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mk.vesti.app.data.City
import mk.vesti.app.data.DayForecast
import mk.vesti.app.data.Forecast
import mk.vesti.app.data.HourForecast
import mk.vesti.app.data.weatherLabel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val Hour = DateTimeFormatter.ofPattern("HH:mm")
private val DayNames = listOf("Понеделник", "Вторник", "Среда", "Четврток", "Петок", "Сабота", "Недела")
private val SkyBlue = Brush.linearGradient(listOf(Color(0xFF3A7BD5), Color(0xFF00C6FF)))

/** Now, the next 24 hours and the week for one of the reader's towns (Open-Meteo). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(
    city: City,
    cities: List<City>,
    forecast: Result<Forecast>?,
    onCity: (City) -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Временска прогноза", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        // A result for another town (still on screen while switching) counts as loading.
        val shown = forecast?.takeIf { r -> r.getOrNull()?.city.let { it == null || it == city.name } }
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            if (cities.size > 1) {
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(cities, key = { it.name }) { c ->
                            FilterChip(selected = c == city, onClick = { if (c != city) onCity(c) }, label = { Text(c.name) })
                        }
                    }
                }
            }
            val data = shown?.getOrNull()
            when {
                shown == null -> item {
                    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }
                data == null -> item {
                    Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Не успеавме да ја вчитаме прогнозата. Провери ја интернет врската.",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = onRetry) { Text("Обиди се повторно") }
                    }
                }
                else -> {
                    item { NowCard(data) }
                    item { SectionTitle("Наредни 24 часа") }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) { items(data.hours) { HourCell(it) } }
                    }
                    item { SectionTitle("7 дена") }
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        ) {
                            Column {
                                data.days.forEachIndexed { i, day ->
                                    if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    DayRow(day)
                                }
                            }
                        }
                    }
                    item {
                        Text(
                            "Извор: Open-Meteo",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NowCard(f: Forecast) {
    val (icon, label) = weatherLabel(f.now.code)
    val today = f.days.firstOrNull()
    Column(
        Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(SkyBlue)
            .padding(20.dp),
    ) {
        Text(f.city.uppercase(Macedonian), color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${f.now.temperature}°", color = Color.White, fontSize = 72.sp, fontWeight = FontWeight.Light)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(icon, fontSize = 40.sp)
                Text(label, color = Color.White, style = MaterialTheme.typography.titleMedium)
            }
        }
        Text(
            "Се чувствува како ${f.feelsLike}° · ${f.now.min}° / ${f.now.max}°",
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Stat("Влажност", "${f.humidity}%")
            Stat("Ветер", "${f.wind} km/h")
            if (today != null) {
                Stat("Изгрев", today.sunrise.format(Hour))
                Stat("Залез", today.sunset.format(Hour))
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column {
        Text(label, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall)
        Text(value, color = Color.White, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 10.dp),
    )
}

@Composable
private fun HourCell(h: HourForecast) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.medium) {
        Column(
            Modifier.width(64.dp).padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(h.time.format(Hour), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(weatherLabel(h.code).first, fontSize = 22.sp)
            Text("${h.temperature}°", style = MaterialTheme.typography.titleMedium)
            Text(
                if (h.rainChance > 0) "💧${h.rainChance}%" else " ",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF3A7BD5),
            )
        }
    }
}

@Composable
private fun DayRow(d: DayForecast) {
    val today = LocalDate.now()
    val name = when (d.date) {
        today -> "Денес"
        today.plusDays(1) -> "Утре"
        else -> DayNames[d.date.dayOfWeek.value - 1]
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        Text(weatherLabel(d.code).first, fontSize = 22.sp)
        Text(
            if (d.rainChance > 0) "💧${d.rainChance}%" else "",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF3A7BD5),
            modifier = Modifier.width(56.dp).padding(start = 8.dp),
        )
        Text("${d.min}°", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
        Text("${d.max}°", fontWeight = FontWeight.SemiBold, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
    }
}
