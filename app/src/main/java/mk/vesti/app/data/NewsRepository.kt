package mk.vesti.app.data

import android.content.Context
import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import mk.vesti.core.Article
import mk.vesti.core.DefaultSources
import mk.vesti.core.FeedSource
import mk.vesti.core.RssParser
import mk.vesti.core.Story
import mk.vesti.core.StoryClusterer
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import java.io.File

class NewsRepository(
    context: Context,
    private val sources: List<FeedSource> = DefaultSources,
    private val client: OkHttpClient = Http.client,
) {
    private val parser = RssParser()
    private val clusterer = StoryClusterer()
    /** Articles from the last successful load, shown at startup and whenever the phone is offline. */
    private val cache = ArticleStore(File(context.cacheDir, "latest.json"))

    data class Result(val stories: List<Story>, val failedSources: List<String>)

    suspend fun cached(): List<Story> = withContext(Dispatchers.Default) { clusterer.cluster(cache.read()) }

    suspend fun load(): Result = withContext(Dispatchers.IO) {
        val perSource = coroutineScope {
            sources.map { source -> async { source to runCatching { fetch(source) } } }.awaitAll()
        }
        val articles = perSource
            .flatMap { (_, r) -> r.getOrDefault(emptyList()) }
            .distinctBy { it.link }
        if (articles.isNotEmpty()) cache.write(articles)
        Result(
            stories = clusterer.cluster(articles),
            failedSources = perSource.filter { it.second.isFailure }.map { it.first.name },
        )
    }

    private fun fetch(source: FeedSource): List<Article> {
        val request = Request.Builder()
            .url(source.url)
            .header("User-Agent", Http.USER_AGENT)
            .build()
        client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "HTTP ${response.code}" }
            val body = response.body ?: error("empty body")
            val xml = Xml.newPullParser().apply {
                setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                setInput(body.byteStream(), null)
            }
            return parser.parse(xml, source)
        }
    }
}
