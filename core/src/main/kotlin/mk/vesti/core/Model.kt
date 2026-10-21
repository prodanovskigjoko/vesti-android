package mk.vesti.core

enum class Category(val label: String) {
    MAKEDONIJA("Македонија"),
    SVET("Свет"),
    EKONOMIJA("Економија"),
    HRONIKA("Хроника"),
    SPORT("Спорт"),
    KULTURA("Култура"),
    TEHNOLOGIJA("Технологија"),
    ZABAVA("Забава"),
}

data class FeedSource(
    val name: String,
    val url: String,
    /** Category for every item of a section feed; null for a general feed whose items get classified. */
    val category: Category? = null,
)

data class Article(
    val title: String,
    val link: String,
    val summary: String,
    val imageUrl: String?,
    val source: String,
    val category: Category,
    /** Epoch millis; 0 when the feed gave no usable date. */
    val publishedAt: Long,
)

/** One news event as reported by one or more outlets. */
data class Story(
    val articles: List<Article>,
) {
    /** The article shown on the card: prefer one with an image, then the earliest report. */
    val lead: Article = articles.sortedWith(
        compareBy<Article> { it.imageUrl == null }.thenBy { it.publishedAt }
    ).first()
    val sourceCount: Int = articles.map { it.source }.distinct().size
    val latestAt: Long = articles.maxOf { it.publishedAt }
    val category: Category = articles.groupingBy { it.category }.eachCount().maxBy { it.value }.key
}
