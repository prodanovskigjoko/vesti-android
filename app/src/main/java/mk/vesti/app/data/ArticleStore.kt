package mk.vesti.app.data

import mk.vesti.core.Article
import mk.vesti.core.Category
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** A list of articles kept as a JSON file in app storage. Writes go through a temp file so a crash can't corrupt it. */
class ArticleStore(private val file: File) {

    @Synchronized
    fun read(): List<Article> = runCatching {
        if (!file.exists()) return emptyList()
        val array = JSONArray(file.readText())
        (0 until array.length()).map { fromJson(array.getJSONObject(it)) }
    }.getOrDefault(emptyList())

    @Synchronized
    fun write(articles: List<Article>) {
        val array = JSONArray()
        articles.forEach { array.put(toJson(it)) }
        file.parentFile?.mkdirs()
        val tmp = File(file.path + ".tmp")
        tmp.writeText(array.toString())
        tmp.renameTo(file)
    }

    private fun toJson(a: Article) = JSONObject()
        .put("title", a.title)
        .put("link", a.link)
        .put("summary", a.summary)
        .put("image", a.imageUrl ?: JSONObject.NULL)
        .put("source", a.source)
        .put("category", a.category.name)
        .put("published", a.publishedAt)

    private fun fromJson(o: JSONObject) = Article(
        title = o.getString("title"),
        link = o.getString("link"),
        summary = o.optString("summary"),
        imageUrl = if (o.isNull("image")) null else o.getString("image"),
        source = o.getString("source"),
        category = runCatching { Category.valueOf(o.getString("category")) }.getOrDefault(Category.MAKEDONIJA),
        publishedAt = o.optLong("published"),
    )
}
