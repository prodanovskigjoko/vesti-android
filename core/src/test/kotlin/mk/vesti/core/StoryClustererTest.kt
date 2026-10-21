package mk.vesti.core

import kotlin.test.Test
import kotlin.test.assertEquals

class StoryClustererTest {
    private val now = 1_792_843_500_000L
    private fun art(title: String, source: String, hoursAgo: Int = 0, summary: String = "") = Article(
        title, "https://$source.mk/${title.hashCode()}", summary, null, source, Category.MAKEDONIJA,
        now - hoursAgo * 3_600_000L,
    )

    @Test
    fun groupsSameEventAcrossOutletsAndInflections() {
        val stories = StoryClusterer().cluster(listOf(
            art("Мицкоски: Уставните измени не се на дневен ред", "a1on"),
            art("Мицкоски вели дека уставните измени не се на дневниот ред", "telma", 1),
            art("Премиерот Мицкоски за уставните измени", "sitel", 2),
            art("Вардар победи во дербито против Работнички", "sport1", 1),
            art("Пораст на цените на струјата од први ноември", "mia", 3),
            art("Од први ноември поскапува струјата", "plusinfo", 4, "Регулаторната комисија одобри пораст на цените на струјата"),
        ))
        assertEquals(3, stories.size)
        assertEquals(3, stories.first().sourceCount)
        assertEquals(setOf(3, 2, 1), stories.map { it.articles.size }.toSet())
    }

    @Test
    fun transliteratedLatinHeadlineMatchesCyrillic() {
        val stories = StoryClusterer().cluster(listOf(
            art("Земјотрес во Скопје, нема повредени", "a"),
            art("Zemjotres vo Skopje, nema povredeni", "b", 1),
            art("Ново кино се отвора во Битола", "c", 1),
        ))
        assertEquals(2, stories.size)
    }

    @Test
    fun doesNotMergeOutsideTimeWindow() {
        val stories = StoryClusterer().cluster(listOf(
            art("Протест пред Собранието", "a"),
            art("Протест пред Собранието", "b", 80),
        ))
        assertEquals(2, stories.size)
    }
}
