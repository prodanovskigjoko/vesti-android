package mk.vesti.core

import kotlin.math.ln
import kotlin.math.sqrt

/**
 * Groups articles that report the same event, the way time.mk shows one story with many sources.
 * Each article becomes a TF-IDF vector over its headline (weighted double) and summary; an article
 * joins the most similar existing story when cosine similarity clears [threshold] and the story is
 * within [windowMillis], otherwise it starts a new story.
 */
class StoryClusterer(
    private val threshold: Double = 0.30,
    private val windowMillis: Long = 36L * 60 * 60 * 1000,
) {
    private class Cluster(val articles: MutableList<Article>, val centroid: MutableMap<String, Double>) {
        var newest: Long = 0
        var oldest: Long = Long.MAX_VALUE
    }

    fun cluster(articles: List<Article>): List<Story> {
        if (articles.isEmpty()) return emptyList()
        val docs = articles.map { termCounts(it) }
        val idf = inverseDocumentFrequency(docs)
        val vectors = docs.map { normalize(it.mapValues { (t, c) -> c * (idf[t] ?: 0.0) }) }

        val clusters = mutableListOf<Cluster>()
        val order = articles.indices.sortedByDescending { articles[it].publishedAt }
        for (i in order) {
            val article = articles[i]
            val vector = vectors[i]
            val best = clusters
                .filter { it.withinWindow(article.publishedAt) }
                .map { it to cosine(vector, it.centroid) }
                .maxByOrNull { it.second }
            val target = if (best != null && best.second >= threshold) {
                best.first
            } else {
                Cluster(mutableListOf(), mutableMapOf()).also { clusters += it }
            }
            target.add(article, vector)
        }

        return clusters
            .map { Story(it.articles.toList()) }
            .sortedWith(compareByDescending<Story> { rank(it) })
    }

    private fun Cluster.withinWindow(time: Long): Boolean =
        time == 0L || newest == 0L || (time >= oldest - windowMillis && time <= newest + windowMillis)

    private fun Cluster.add(article: Article, vector: Map<String, Double>) {
        val n = articles.size
        articles += article
        val keys = centroid.keys + vector.keys
        for (k in keys) centroid[k] = ((centroid[k] ?: 0.0) * n + (vector[k] ?: 0.0)) / (n + 1)
        if (article.publishedAt != 0L) {
            newest = maxOf(newest, article.publishedAt)
            oldest = minOf(oldest, article.publishedAt)
        }
    }

    /** Stories covered by more outlets rank higher; recency breaks ties within roughly a few hours. */
    private fun rank(story: Story): Double {
        val hours = story.latestAt / 3_600_000.0
        return story.sourceCount * 6.0 + hours
    }

    private fun termCounts(article: Article): Map<String, Double> {
        val counts = HashMap<String, Double>()
        TextNormalizer.tokens(article.title).forEach { counts.merge(it, 2.0, Double::plus) }
        TextNormalizer.tokens(article.summary.take(300)).forEach { counts.merge(it, 1.0, Double::plus) }
        return counts
    }

    private fun inverseDocumentFrequency(docs: List<Map<String, Double>>): Map<String, Double> {
        val df = HashMap<String, Int>()
        docs.forEach { d -> d.keys.forEach { df.merge(it, 1, Int::plus) } }
        val n = docs.size.toDouble()
        return df.mapValues { (_, f) -> ln((n + 1) / (f + 1)) + 1.0 }
    }

    private fun normalize(v: Map<String, Double>): Map<String, Double> {
        val norm = sqrt(v.values.sumOf { it * it })
        return if (norm == 0.0) v else v.mapValues { it.value / norm }
    }

    private fun cosine(a: Map<String, Double>, b: Map<String, Double>): Double {
        val (small, large) = if (a.size <= b.size) a to b else b to a
        val dot = small.entries.sumOf { (k, x) -> x * (large[k] ?: 0.0) }
        val nb = sqrt(b.values.sumOf { it * it })
        return if (nb == 0.0) 0.0 else dot / nb
    }
}
