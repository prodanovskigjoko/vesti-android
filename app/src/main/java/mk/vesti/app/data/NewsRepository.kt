package mk.vesti.app.data

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
import java.util.concurrent.TimeUnit

class NewsRepository(
    private val sources: List<FeedSource> = DefaultSources,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build(),
) {
    private val parser = RssParser()
    private val clusterer = StoryClusterer()

    data class Result(val stories: List<Story>, val failedSources: List<String>)

    suspend fun load(): Result = withContext(Dispatchers.IO) {
        val perSource = coroutineScope {
            sources.map { source -> async { source to runCatching { fetch(source) } } }.awaitAll()
        }
        val articles = perSource
            .flatMap { (_, r) -> r.getOrDefault(emptyList()) }
            .distinctBy { it.link }
        Result(
            stories = clusterer.cluster(articles),
            failedSources = perSource.filter { it.second.isFailure }.map { it.first.name },
        )
    }

    private fun fetch(source: FeedSource): List<Article> {
        val request = Request.Builder()
            .url(source.url)
            .header("User-Agent", "Vesti/0.1 (Android RSS reader)")
            .build()
        client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "HTTP ${response.code}" }
            val body = response.body ?: error("empty body")
            val xml = Xml.newPullParser().apply {
                setFeature(org.xmlpull.v1.XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                setInput(body.byteStream(), null)
            }
            return parser.parse(xml, source)
        }
    }
}
