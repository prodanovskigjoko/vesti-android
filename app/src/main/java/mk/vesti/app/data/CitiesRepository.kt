package mk.vesti.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class City(val name: String, val latitude: Double, val longitude: Double)

/** Towns offered for the weather cards. Coordinates are the town centres, so no location permission is needed. */
val WeatherCities: List<City> = listOf(
    City("Скопје", 41.9981, 21.4254),
    City("Битола", 41.0314, 21.3347),
    City("Куманово", 42.1322, 21.7144),
    City("Прилеп", 41.3464, 21.5542),
    City("Тетово", 42.0069, 20.9715),
    City("Велес", 41.7156, 21.7756),
    City("Охрид", 41.1172, 20.8016),
    City("Гостивар", 41.7958, 20.9083),
    City("Штип", 41.7458, 22.1958),
    City("Струмица", 41.4378, 22.6427),
    City("Кавадарци", 41.4331, 22.0119),
    City("Кочани", 41.9164, 22.4128),
    City("Кичево", 41.5129, 20.9589),
    City("Струга", 41.1778, 20.6783),
    City("Гевгелија", 41.1392, 22.5025),
    City("Радовиш", 41.6383, 22.4647),
    City("Неготино", 41.4839, 22.0892),
    City("Дебар", 41.5250, 20.5272),
    City("Крива Паланка", 42.2017, 22.3317),
    City("Свети Николе", 41.8650, 21.9425),
    City("Кратово", 42.0783, 22.1808),
    City("Пробиштип", 42.0031, 22.1786),
    City("Виница", 41.8828, 22.5092),
    City("Делчево", 41.9672, 22.7750),
    City("Берово", 41.7031, 22.8572),
    City("Валандово", 41.3169, 22.5611),
    City("Дојран", 41.1817, 22.7228),
    City("Ресен", 41.0893, 21.0109),
    City("Крушево", 41.3689, 21.2483),
    City("Демир Хисар", 41.2208, 21.2031),
    City("Македонски Брод", 41.5133, 21.2153),
)

/** The towns the reader shows weather for, in the order they were added. Skopje until they choose. */
class CitiesRepository(context: Context) {
    private val prefs = context.getSharedPreferences("weather", Context.MODE_PRIVATE)

    private val _selected = MutableStateFlow(read())
    val selected: StateFlow<List<City>> = _selected

    /** Adds or removes [city]; the last town cannot be removed, so the card never disappears. */
    fun toggle(city: City) {
        val current = _selected.value
        val updated = when {
            city !in current -> current + city
            current.size > 1 -> current - city
            else -> return
        }
        prefs.edit().putString(KEY, updated.joinToString(SEPARATOR) { it.name }).apply()
        _selected.value = updated
    }

    private fun read(): List<City> {
        val names = prefs.getString(KEY, null)?.split(SEPARATOR).orEmpty()
        return names.mapNotNull { n -> WeatherCities.firstOrNull { it.name == n } }.ifEmpty { listOf(WeatherCities.first()) }
    }

    private companion object {
        const val KEY = "cities"
        const val SEPARATOR = "|"
    }
}
