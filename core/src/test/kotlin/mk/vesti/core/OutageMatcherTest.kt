package mk.vesti.core

import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OutageMatcherTest {
    private val start = LocalDateTime.of(2026, 10, 30, 8, 0)
    private fun outage(centre: Int, place: String, area: String) =
        Outage("id", centre, place, area, start, start.plusHours(4))

    private val bitolaStreets = outage(
        13, "БИТОЛА",
        "УЛ.МИЛТОН МАНАКИ, ДЕЛ ОД УЛ.ПАРТИЗАНСКА , УЛ.СТАЛЕ ПОПОВ, УЛ.12 КЛАДЕНЦИ СО ОКОЛНИТЕ ОБЈЕКТИ.",
    )
    private val villages = outage(13, "БИТОЛА-МАГАРЕВО", "С.ДИХОВО, С.МАГАРЕВО, С.ТРНОВО, МОЛИКА , КОПАНКИ")
    private val skopje = outage(10, "ГАЗИ БАБА-МАЏАРИ-ХИПОДРОМ", "УЛ. ПАРТИЗАНСКА, УЛ. ДЕБАРЦА")
    private val matcher = OutageMatcher()

    @Test
    fun streetMatchesInItsOwnTownOnly() {
        assertTrue(matcher.matches(bitolaStreets, SavedAddress("Дома", "Битола", "ул. Партизанска 15")))
        assertTrue(matcher.matches(bitolaStreets, SavedAddress("Работа", "bitola", "12 Kladenci")))
        assertFalse(matcher.matches(bitolaStreets, SavedAddress("Дома", "Прилеп", "Партизанска")))
        assertFalse(matcher.matches(bitolaStreets, SavedAddress("Дома", "Битола", "Гоце Делчев")))
    }

    @Test
    fun skopjeMunicipalitiesCountAsSkopje() {
        assertTrue(matcher.matches(skopje, SavedAddress("Дома", "Скопје", "Дебарца")))
        assertFalse(matcher.matches(skopje, SavedAddress("Дома", "Битола", "Партизанска")))
    }

    @Test
    fun villageWithoutStreetMatchesByName() {
        assertTrue(matcher.matches(villages, SavedAddress("Родители", "Дихово")))
        assertFalse(matcher.matches(villages, SavedAddress("Родители", "Бистрица")))
    }

    @Test
    fun finishedOutagesAreDropped() {
        val address = SavedAddress("Дома", "Битола", "Партизанска")
        assertEquals(1, matcher.match(listOf(bitolaStreets), listOf(address), start).size)
        assertTrue(matcher.match(listOf(bitolaStreets), listOf(address), start.plusHours(5)).isEmpty())
    }
}
