package mk.vesti.app.data

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** One shared client so the feed, saved pages and the background worker reuse connections. */
object Http {
    const val USER_AGENT = "Vesti/0.1 (Android RSS reader)"

    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }
}
