package mk.vesti.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BreakingNewsDetectorTest {
    private val now = 1_792_584_300_000L
    private fun story(sources: Int, hoursAgo: Int = 0, prefix: String = "s") = Story(
        (1..sources).map {
            Article("Наслов", "https://$prefix$it.mk/a", "", null, "$prefix$it", Category.MAKEDONIJA,
                now - hoursAgo * 3_600_000L)
        },
    )

    @Test
    fun onlyBigRecentStoriesAreBreaking() {
        val big = story(6, prefix = "big")
        val small = story(3, prefix = "small")
        val old = story(7, hoursAgo = 10, prefix = "old")
        assertEquals(listOf(big), BreakingNewsDetector().detect(listOf(small, big, old), emptySet(), now))
    }

    @Test
    fun grownStoryIsNotAnnouncedTwice() {
        val first = story(5)
        val notified = first.articles.map { it.link }.toSet()
        val grown = Story(first.articles + story(2, prefix = "new").articles)
        assertTrue(BreakingNewsDetector().detect(listOf(grown), notified, now).isEmpty())
    }
}
