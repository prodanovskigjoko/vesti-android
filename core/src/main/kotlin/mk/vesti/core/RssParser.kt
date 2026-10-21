package mk.vesti.core

import org.xmlpull.v1.XmlPullParser
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Parses RSS 2.0 and Atom feeds. The caller supplies the XmlPullParser (Android's built-in
 * `Xml.newPullParser()` in the app, kxml2 in tests), already set to its input.
 */
class RssParser {

    fun parse(parser: XmlPullParser, source: FeedSource): List<Article> {
        val articles = mutableListOf<Article>()
        var item: ItemBuilder? = null
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            val name = parser.name?.lowercase()
            when (event) {
                XmlPullParser.START_TAG -> when {
                    name == "item" || name == "entry" -> item = ItemBuilder()
                    item != null -> readField(parser, item)
                }
                XmlPullParser.END_TAG ->
                    if ((name == "item" || name == "entry") && item != null) {
                        item.build(source)?.let { articles += it }
                        item = null
                    }
            }
            event = parser.next()
        }
        return articles
    }

    /** Expects a parser that is not namespace-aware, so prefixed tags keep names like `media:content`. */
    private fun readField(parser: XmlPullParser, item: ItemBuilder) {
        when (parser.name.lowercase()) {
            "title" -> item.title = readText(parser)
            "link" -> {
                val href = parser.getAttributeValue(null, "href")
                if (href != null) {
                    val rel = parser.getAttributeValue(null, "rel")
                    if (item.link == null || rel == null || rel == "alternate") item.link = href
                } else {
                    item.link = readText(parser)
                }
            }
            "description", "summary" -> item.summary = item.summary ?: readText(parser)
            "content:encoded", "content" -> {
                val html = readText(parser)
                item.imageUrl = item.imageUrl ?: firstImage(html)
                if (item.summary.isNullOrBlank()) item.summary = html
            }
            "pubdate", "published", "updated", "dc:date" ->
                item.date = item.date ?: parseDate(readText(parser))
            "category" -> item.categories += parser.getAttributeValue(null, "term") ?: readText(parser)
            "enclosure", "media:content", "media:thumbnail" -> {
                val url = parser.getAttributeValue(null, "url")
                val type = parser.getAttributeValue(null, "type")
                if (url != null && (type == null || type.startsWith("image"))) item.imageUrl = item.imageUrl ?: url
            }
        }
    }

    private fun readText(parser: XmlPullParser): String {
        val sb = StringBuilder()
        var depth = 1
        while (depth > 0) {
            when (parser.next()) {
                XmlPullParser.TEXT, XmlPullParser.CDSECT -> sb.append(parser.text)
                XmlPullParser.START_TAG -> depth++
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.END_DOCUMENT -> depth = 0
            }
        }
        return sb.toString().trim()
    }

    private class ItemBuilder {
        var title: String? = null
        var link: String? = null
        var summary: String? = null
        var imageUrl: String? = null
        var date: Long? = null
        val categories = mutableListOf<String>()

        fun build(source: FeedSource): Article? {
            val t = title?.let(::stripHtml)?.takeIf { it.isNotBlank() } ?: return null
            val l = link?.takeIf { it.isNotBlank() } ?: return null
            val raw = summary.orEmpty()
            return Article(
                title = t,
                link = l,
                summary = stripHtml(raw).take(500),
                imageUrl = imageUrl ?: firstImage(raw),
                source = source.name,
                category = source.category ?: Categorizer.fromLabels(categories) ?: Category.MAKEDONIJA,
                publishedAt = date ?: 0L,
            )
        }
    }

    companion object {
        // Weekday is dropped before parsing: feeds often get it wrong, which strict RFC 1123 rejects.
        private val rfc822 = listOf("d MMM yyyy HH:mm[:ss] Z", "d MMM yyyy HH:mm[:ss] zzz")
            .map { DateTimeFormatter.ofPattern(it, Locale.ENGLISH) }

        fun parseDate(text: String): Long? {
            val s = text.trim()
            runCatching { return OffsetDateTime.parse(s).toInstant().toEpochMilli() }
            val noWeekday = s.substringAfter(", ").replace(Regex("\\s+"), " ")
            for (f in rfc822) runCatching { return ZonedDateTime.parse(noWeekday, f).toInstant().toEpochMilli() }
            return null
        }

        private val entities = mapOf(
            "&nbsp;" to " ", "&amp;" to "&", "&quot;" to "\"", "&#039;" to "'", "&#39;" to "'",
            "&lt;" to "<", "&gt;" to ">", "&#8211;" to "–", "&#8220;" to "“", "&#8221;" to "”",
            "&#8222;" to "„", "&#8230;" to "…", "&hellip;" to "…",
        )

        fun stripHtml(html: String): String {
            var s = html.replace(Regex("<[^>]*>"), " ")
            for ((e, c) in entities) s = s.replace(e, c)
            return s.replace(Regex("\\s+"), " ").trim()
        }

        private val imgSrc = Regex("<img[^>]+src=[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)

        fun firstImage(html: String): String? = imgSrc.find(html)?.groupValues?.get(1)
    }
}
