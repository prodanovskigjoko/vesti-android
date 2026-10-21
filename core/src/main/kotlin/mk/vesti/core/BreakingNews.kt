package mk.vesti.core

/**
 * Picks stories worth a push notification: covered by at least [minSources] outlets, recent, and
 * not already announced. A story counts as announced when any of its article links was part of a
 * story we notified about before, because a cluster keeps growing (and its lead can change) as more
 * outlets report the same event.
 */
class BreakingNewsDetector(
    private val minSources: Int = 5,
    private val maxAgeMillis: Long = 6L * 60 * 60 * 1000,
) {
    fun detect(stories: List<Story>, notifiedLinks: Set<String>, now: Long): List<Story> =
        stories
            .filter { it.sourceCount >= minSources }
            .filter { it.latestAt == 0L || now - it.latestAt <= maxAgeMillis }
            .filter { s -> s.articles.none { it.link in notifiedLinks } }
            .sortedByDescending { it.sourceCount }
}
