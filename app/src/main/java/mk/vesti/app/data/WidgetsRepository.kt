package mk.vesti.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class Weather(val city: String, val temperature: Int, val min: Int, val max: Int, val code: Int)

/** Denars for one unit of [currency]. */
data class Rate(val currency: String, val denars: Double)

/** Maximum retail price set by the energy regulator (РКЕ), e.g. БС-95 at 91.00 ден/л. */
data class FuelPrice(val name: String, val price: Double, val unit: String)

data class Fuel(val validFrom: LocalDate, val prices: List<FuelPrice>)

/** The detailed forecast screen: now, the next 24 hours and 7 days. */
data class Forecast(
    val city: String,
    val now: Weather,
    val feelsLike: Int,
    val humidity: Int,
    /** km/h */
    val wind: Int,
    val hours: List<HourForecast>,
    val days: List<DayForecast>,
)

data class HourForecast(val time: LocalDateTime, val temperature: Int, val code: Int, val rainChance: Int)

data class DayForecast(
    val date: LocalDate,
    val max: Int,
    val min: Int,
    val code: Int,
    val rainChance: Int,
    val sunrise: LocalTime,
    val sunset: LocalTime,
)

data class Widgets(
    /** One entry per chosen town that answered; a town that failed simply has no entry. */
    val weather: List<Weather> = emptyList(),
    val rates: List<Rate> = emptyList(),
    val fuel: Fuel? = null,
)

/** Data for the cards above the feed. Each card loads on its own, so one failing service hides only its card. */
class WidgetsRepository(private val client: OkHttpClient = Http.client) {

    suspend fun load(cities: List<City>): Widgets = withContext(Dispatchers.IO) {
        coroutineScope {
            val weather = async { weather(cities) }
            val rates = async { runCatching { rates() }.getOrDefault(emptyList()) }
            val fuel = async { runCatching { fuel() }.getOrNull() }
            Widgets(weather = weather.await(), rates = rates.await(), fuel = fuel.await())
        }
    }

    /** Each town in parallel; a town that fails is left out. */
    suspend fun weather(cities: List<City>): List<Weather> = withContext(Dispatchers.IO) {
        coroutineScope {
            cities.map { async { runCatching { weather(it) }.getOrNull() } }.mapNotNull { it.await() }
        }
    }

    /** Open-Meteo: free and keyless. */
    private fun weather(city: City): Weather {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=${city.latitude}&longitude=${city.longitude}" +
            "&current=temperature_2m,weather_code&daily=temperature_2m_max,temperature_2m_min" +
            "&timezone=Europe%2FSkopje&forecast_days=1"
        val json = getJson(url)
        val current = json.getJSONObject("current")
        val daily = json.getJSONObject("daily")
        return Weather(
            city = city.name,
            temperature = Math.round(current.getDouble("temperature_2m")).toInt(),
            min = Math.round(daily.getJSONArray("temperature_2m_min").getDouble(0)).toInt(),
            max = Math.round(daily.getJSONArray("temperature_2m_max").getDouble(0)).toInt(),
            code = current.getInt("weather_code"),
        )
    }

    suspend fun forecast(city: City): Forecast = withContext(Dispatchers.IO) {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=${city.latitude}&longitude=${city.longitude}" +
            "&current=temperature_2m,apparent_temperature,relative_humidity_2m,wind_speed_10m,weather_code" +
            "&hourly=temperature_2m,weather_code,precipitation_probability" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,sunrise,sunset" +
            "&timezone=Europe%2FSkopje&forecast_days=7"
        val json = getJson(url)
        val current = json.getJSONObject("current")
        val hourly = json.getJSONObject("hourly")
        val daily = json.getJSONObject("daily")
        fun JSONObject.ints(key: String) = getJSONArray(key).let { a -> (0 until a.length()).map { Math.round(a.optDouble(it, 0.0)).toInt() } }

        val hourTimes = hourly.getJSONArray("time").let { a -> (0 until a.length()).map { LocalDateTime.parse(a.getString(it)) } }
        val hourTemps = hourly.ints("temperature_2m")
        val hourCodes = hourly.ints("weather_code")
        val hourRain = hourly.ints("precipitation_probability")
        val nowTime = LocalDateTime.parse(current.getString("time"))
        val first = hourTimes.indexOfFirst { !it.isBefore(nowTime.withMinute(0)) }.coerceAtLeast(0)
        val hours = (first until minOf(first + 24, hourTimes.size)).map {
            HourForecast(hourTimes[it], hourTemps[it], hourCodes[it], hourRain[it])
        }

        val dates = daily.getJSONArray("time")
        val max = daily.ints("temperature_2m_max")
        val min = daily.ints("temperature_2m_min")
        val codes = daily.ints("weather_code")
        val rain = daily.ints("precipitation_probability_max")
        val days = (0 until dates.length()).map {
            DayForecast(
                date = LocalDate.parse(dates.getString(it)),
                max = max[it], min = min[it], code = codes[it], rainChance = rain[it],
                sunrise = LocalDateTime.parse(daily.getJSONArray("sunrise").getString(it)).toLocalTime(),
                sunset = LocalDateTime.parse(daily.getJSONArray("sunset").getString(it)).toLocalTime(),
            )
        }
        Forecast(
            city = city.name,
            now = Weather(
                city = city.name,
                temperature = Math.round(current.getDouble("temperature_2m")).toInt(),
                min = days.firstOrNull()?.min ?: 0,
                max = days.firstOrNull()?.max ?: 0,
                code = current.getInt("weather_code"),
            ),
            feelsLike = Math.round(current.getDouble("apparent_temperature")).toInt(),
            humidity = current.getInt("relative_humidity_2m"),
            wind = Math.round(current.getDouble("wind_speed_10m")).toInt(),
            hours = hours,
            days = days,
        )
    }

    /** ExchangeRate-API open endpoint (daily rates, keyless). The official NBRM list could replace it later. */
    private fun rates(): List<Rate> {
        val rates = getJson("https://open.er-api.com/v6/latest/EUR").getJSONObject("rates")
        val mkdPerEur = rates.getDouble("MKD")
        return listOf("EUR", "USD", "CHF", "GBP").map { Rate(it, mkdPerEur / rates.getDouble(it)) }
    }

    /**
     * РКЕ publishes its weekly price decision only as a web page, so the prices are kept in docs/fuel.json
     * in this repository and updated there after each decision; the app reads the raw file.
     */
    private fun fuel(): Fuel {
        val json = getJson("https://raw.githubusercontent.com/prodanovskigjoko/vesti-android/main/docs/fuel.json")
        val prices = json.getJSONArray("prices")
        return Fuel(
            validFrom = LocalDate.parse(json.getString("validFrom")),
            prices = (0 until prices.length()).map {
                val p = prices.getJSONObject(it)
                FuelPrice(p.getString("name"), p.getDouble("price"), p.getString("unit"))
            },
        )
    }

    private fun getJson(url: String): JSONObject {
        val request = Request.Builder().url(url).header("User-Agent", Http.USER_AGENT).build()
        client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "HTTP ${response.code}" }
            return JSONObject(response.body!!.string())
        }
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
