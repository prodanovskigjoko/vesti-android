package mk.vesti.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import mk.vesti.core.Article
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest

/**
 * "Зачувај за подоцна": saved articles plus a downloaded copy of each article page, so the
 * text can be read without a connection.
 */
class SavedRepository(context: Context, private val client: OkHttpClient = Http.client) {
    private val store = ArticleStore(File(context.filesDir, "saved.json"))
    private val pagesDir = File(context.filesDir, "pages")

    private val _saved = MutableStateFlow(store.read())
    val saved: StateFlow<List<Article>> = _saved

    fun isSaved(link: String): Boolean = _saved.value.any { it.link == link }

    suspend fun save(article: Article) = withContext(Dispatchers.IO) {
        if (isSaved(article.link)) return@withContext
        val updated = listOf(article) + _saved.value
        store.write(updated)
        _saved.value = updated
        // Best effort: without the page the article still opens online.
        runCatching { download(article) }
    }

    suspend fun remove(article: Article) = withContext(Dispatchers.IO) {
        val updated = _saved.value.filterNot { it.link == article.link }
        store.write(updated)
        _saved.value = updated
        pageFile(article.link).delete()
    }

    /** The downloaded page, or null when it was never fetched (then the reader loads it online). */
    suspend fun offlinePage(article: Article): String? = withContext(Dispatchers.IO) {
        pageFile(article.link).takeIf { it.exists() }?.readText()
    }

    private fun download(article: Article) {
        val request = Request.Builder().url(article.link).header("User-Agent", Http.USER_AGENT).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return
            val html = response.body?.string() ?: return
            pagesDir.mkdirs()
            pageFile(article.link).writeText(html)
        }
    }

    private fun pageFile(link: String): File {
        val hash = MessageDigest.getInstance("SHA-1").digest(link.toByteArray())
            .joinToString("") { "%02x".format(it) }
        return File(pagesDir, "$hash.html")
    }
}
