package mk.vesti.core

import java.time.LocalDateTime

/** One planned power cut as EVN publishes it: settlement, free-text streets or villages, and the time window. */
data class Outage(
    val id: String,
    /** EVN's customer centre, which tells the region (e.g. 13 = Битола) even when [place] names only a municipality. */
    val centreId: Int,
    /** E.g. "БИТОЛА", "БИТОЛА-МАГАРЕВО" or "ГАЗИ БАБА-СКОПЈЕ-ГАЗИ БАБА". */
    val place: String,
    /** E.g. "УЛ.МИЛТОН МАНАКИ, ДЕЛ ОД УЛ.ПАРТИЗАНСКА ..." or "С.ДИХОВО, С.МАГАРЕВО ...". */
    val area: String,
    val start: LocalDateTime,
    val end: LocalDateTime,
)

/** An address the reader saved on the phone, e.g. "Дома": Битола, Партизанска 15. */
data class SavedAddress(val label: String, val place: String, val street: String = "")

data class OutageMatch(val address: SavedAddress, val outage: Outage)

/**
 * Matches EVN outages to saved addresses. EVN writes streets as free text in capitals, so both sides are
 * normalised to Cyrillic capitals and compared as whole words. A street matches only inside the same region
 * or settlement, because the same street name exists in many towns. An address without a street (a village)
 * matches whenever the place itself is listed.
 */
class OutageMatcher {

    fun match(outages: List<Outage>, addresses: List<SavedAddress>, now: LocalDateTime): List<OutageMatch> =
        outages
            .filter { it.end.isAfter(now) }
            .sortedBy { it.start }
            .flatMap { outage -> addresses.filter { matches(outage, it) }.map { OutageMatch(it, outage) } }

    fun matches(outage: Outage, address: SavedAddress): Boolean {
        val place = normalize(address.place)
        if (place.isEmpty()) return false
        val area = normalize(outage.area)
        val inRegion = containsWords(normalize(outage.place), place) || REGIONS[outage.centreId] == place
        val street = streetKey(address.street)
        return if (street.isEmpty()) {
            containsWords(normalize(outage.place), place) || containsWords(area, place)
        } else {
            (inRegion || containsWords(area, place)) && containsWords(area, street)
        }
    }

    private fun containsWords(haystack: String, needle: String) =
        needle.isNotEmpty() && " $haystack ".contains(" $needle ")

    companion object {
        /** EVN customer centres (kecId) by the town they serve. */
        val REGIONS: Map<Int, String> = mapOf(
            10 to "СКОПЈЕ", 38 to "СКОПЈЕ", 39 to "СКОПЈЕ", 11 to "ТЕТОВО", 12 to "ОХРИД", 13 to "БИТОЛА",
            14 to "ПРИЛЕП", 15 to "ВЕЛЕС", 16 to "КУМАНОВО", 17 to "ШТИП", 18 to "СТРУМИЦА", 19 to "ГОСТИВАР",
            21 to "КИЧЕВО", 22 to "СТРУГА", 25 to "КАВАДАРЦИ", 28 to "ГЕВГЕЛИЈА", 30 to "КОЧАНИ",
            32 to "ДЕЛЧЕВО", 35 to "КРАТОВО",
        )

        private val STREET_PREFIXES = setOf("УЛ", "УЛИЦА", "БУЛ", "БУЛЕВАР")

        /** "ул. Партизанска 15" → "ПАРТИЗАНСКА"; "12 Кладенци" stays "12 КЛАДЕНЦИ". */
        fun streetKey(street: String): String {
            val words = normalize(street).split(' ').filter { it.isNotEmpty() }.toMutableList()
            while (words.isNotEmpty() && words.first() in STREET_PREFIXES) words.removeAt(0)
            while (words.size > 1 && HOUSE_NUMBER.matches(words.last())) words.removeAt(words.lastIndex)
            return words.joinToString(" ")
        }

        private val HOUSE_NUMBER = Regex("""\d+[А-ЯЃЅЈЉЊЌЏ]?""")

        /** Capitals, Latin spelling turned into Macedonian Cyrillic, punctuation as spaces. */
        fun normalize(text: String): String =
            transliterate(text.uppercase())
                .replace(Regex("""[^\p{L}\p{N}]+"""), " ")
                .trim()

        private val DIGRAPHS = listOf(
            "DZH" to "Џ", "DŽ" to "Џ", "DZ" to "Ѕ", "LJ" to "Љ", "NJ" to "Њ", "GJ" to "Ѓ", "KJ" to "Ќ",
            "CH" to "Ч", "SH" to "Ш", "ZH" to "Ж",
        )
        private val LETTERS = mapOf(
            'A' to 'А', 'B' to 'Б', 'V' to 'В', 'G' to 'Г', 'D' to 'Д', 'E' to 'Е', 'Ž' to 'Ж', 'Z' to 'З',
            'I' to 'И', 'J' to 'Ј', 'K' to 'К', 'L' to 'Л', 'M' to 'М', 'N' to 'Н', 'O' to 'О', 'P' to 'П',
            'R' to 'Р', 'S' to 'С', 'T' to 'Т', 'U' to 'У', 'F' to 'Ф', 'H' to 'Х', 'C' to 'Ц', 'Č' to 'Ч',
            'Š' to 'Ш', 'Ć' to 'Ќ', 'Ǵ' to 'Ѓ', 'Ḱ' to 'Ќ', 'W' to 'В', 'Y' to 'Ј', 'X' to 'Х', 'Q' to 'К',
        )

        private fun transliterate(upper: String): String {
            var s = upper
            for ((latin, cyr) in DIGRAPHS) s = s.replace(latin, cyr)
            return buildString(s.length) { s.forEach { append(LETTERS[it] ?: it) } }
        }
    }
}
