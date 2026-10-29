package mk.vesti.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import mk.vesti.core.Outage
import mk.vesti.core.SavedAddress
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Planned power cuts from EVN and the addresses the reader wants them checked against. The addresses stay
 * in this app's preferences; EVN is asked for its whole public list, so it never learns which address we care about.
 */
class OutagesRepository(context: Context, private val client: OkHttpClient = Http.client) {
    private val prefs = context.getSharedPreferences("outages", Context.MODE_PRIVATE)

    private val _addresses = MutableStateFlow(readAddresses())
    val addresses: StateFlow<List<SavedAddress>> = _addresses

    fun add(address: SavedAddress) = save(_addresses.value + address)

    fun remove(address: SavedAddress) = save(_addresses.value - address)

    /** EVN's public list behind the outage map on elektrodistribucija.mk: planned cuts for about three weeks ahead. */
    suspend fun fetch(): List<Outage> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://portal-api.elektrodistribucija.mk/DSO/Prekini/ZemiPrekini")
            .header("User-Agent", Http.USER_AGENT)
            .header("Accept", "application/json")
            .build()
        val body = client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "HTTP ${response.code}" }
            response.body!!.string()
        }
        val items = JSONArray(body)
        (0 until items.length()).mapNotNull { i ->
            val o = items.getJSONObject(i)
            runCatching {
                Outage(
                    id = o.getString("prekinID"),
                    centreId = o.optInt("kecId"),
                    place = o.optString("nasMesto").trim(),
                    area = o.optString("adresa").trim(),
                    start = LocalDateTime.parse(o.getString("pocetok")),
                    end = LocalDateTime.parse(o.getString("kraj")),
                )
            }.getOrNull()
        }
    }

    private fun save(list: List<SavedAddress>) {
        val json = JSONArray(list.map { JSONObject().put("label", it.label).put("place", it.place).put("street", it.street) })
        prefs.edit().putString(KEY, json.toString()).apply()
        _addresses.value = list
    }

    private fun readAddresses(): List<SavedAddress> = runCatching {
        val json = JSONArray(prefs.getString(KEY, "[]"))
        (0 until json.length()).map {
            val o = json.getJSONObject(it)
            SavedAddress(o.getString("label"), o.getString("place"), o.optString("street"))
        }
    }.getOrDefault(emptyList())

    companion object {
        private const val KEY = "addresses"

        /** EVN times are local to North Macedonia. */
        val ZONE: ZoneId = ZoneId.of("Europe/Skopje")

        fun now(): LocalDateTime = LocalDateTime.now(ZONE)
    }
}

private val DAYS = listOf("понеделник", "вторник", "среда", "четврток", "петок", "сабота", "недела")
private val TIME = DateTimeFormatter.ofPattern("HH:mm")
private val DATE = DateTimeFormatter.ofPattern("d.M.")

/** "утре, петок 30.10. од 08:00 до 12:00" */
fun outageWhen(outage: Outage, today: LocalDate = OutagesRepository.now().toLocalDate()): String {
    val day = outage.start.toLocalDate()
    val relative = when (day) {
        today -> "денес, "
        today.plusDays(1) -> "утре, "
        else -> ""
    }
    val until = if (outage.end.toLocalDate() == day) outage.end.format(TIME) else "${outage.end.format(DATE)} ${outage.end.format(TIME)}"
    return "$relative${DAYS[day.dayOfWeek.value - 1]} ${day.format(DATE)} од ${outage.start.format(TIME)} до $until"
}
