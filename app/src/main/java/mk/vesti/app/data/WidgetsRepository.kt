package mk.vesti.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class Weather(val city: String, val temperature: Int, val min: Int, val max: Int, val code: Int)

/** Denars for one unit of [currency]. */
data class Rate(val currency: String, val denars: Double)

data class Match(
    val league: String,
    val home: String,
    val away: String,
    val homeScore: Int?,
    val awayScore: Int?,
    /** "67'", "ПП" (half time), "КРАЈ", or a kick-off time like "20:45". */
    val status: String,
    val live: Boolean,
)

data class Widgets(
    val weather: Weather? = null,
    val rates: List<Rate> = emptyList(),
    val matches: List<Match> = emptyList(),
    /** True while [matches] are sample data because no live-score provider is configured. */
    val sportIsDemo: Boolean = true,
)

/** Data for the cards above the feed. Each card loads on its own, so one failing service hides only its card. */
class WidgetsRepository(private val client: OkHttpClient = Http.client) {

    suspend fun load(): Widgets = withContext(Dispatchers.IO) {
        coroutineScope {
            val weather = async { runCatching { weather() }.getOrNull() }
            val rates = async { runCatching { rates() }.getOrDefault(emptyList()) }
            Widgets(weather = weather.await(), rates = rates.await(), matches = DemoMatches, sportIsDemo = true)
        }
    }

    /** Open-Meteo: free and keyless. */
    private fun weather(): Weather {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=41.9981&longitude=21.4254" +
            "&current=temperature_2m,weather_code&daily=temperature_2m_max,temperature_2m_min" +
            "&timezone=Europe%2FSkopje&forecast_days=1"
        val json = getJson(url)
        val current = json.getJSONObject("current")
        val daily = json.getJSONObject("daily")
        return Weather(
            city = "Скопје",
            temperature = Math.round(current.getDouble("temperature_2m")).toInt(),
            min = Math.round(daily.getJSONArray("temperature_2m_min").getDouble(0)).toInt(),
            max = Math.round(daily.getJSONArray("temperature_2m_max").getDouble(0)).toInt(),
            code = current.getInt("weather_code"),
        )
    }

    /** ExchangeRate-API open endpoint (daily rates, keyless). The official NBRM list could replace it later. */
    private fun rates(): List<Rate> {
        val rates = getJson("https://open.er-api.com/v6/latest/EUR").getJSONObject("rates")
        val mkdPerEur = rates.getDouble("MKD")
        return listOf("EUR", "USD", "CHF", "GBP").map { Rate(it, mkdPerEur / rates.getDouble(it)) }
    }

    private fun getJson(url: String): JSONObject {
        val request = Request.Builder().url(url).header("User-Agent", Http.USER_AGENT).build()
        client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "HTTP ${response.code}" }
            return JSONObject(response.body!!.string())
        }
    }

    private companion object {
        // Live scores need a paid or keyed provider (e.g. API-Football); until one is chosen the card shows these.
        val DemoMatches = listOf(
            Match("Прва МФЛ", "Вардар", "Шкупи", 2, 1, "67'", live = true),
            Match("Прва МФЛ", "Работнички", "Струга", 0, 0, "ПП", live = true),
            Match("Лига на шампиони", "Реал Мадрид", "Барселона", null, null, "21:00", live = false),
        )
    }
}

/** WMO weather code to an emoji and a Macedonian description. */
fun weatherLabel(code: Int): Pair<String, String> = when (code) {
    0 -> "☀️" to "Сончево"
    1, 2 -> "🌤️" to "Делумно облачно"
    3 -> "☁️" to "Облачно"
    45, 48 -> "🌫️" to "Магла"
    in 51..57 -> "🌦️" to "Росулка"
    in 61..67, in 80..82 -> "🌧️" to "Дожд"
    in 71..77, 85, 86 -> "❄️" to "Снег"
    in 95..99 -> "⛈️" to "Грмотевици"
    else -> "🌡️" to ""
}
