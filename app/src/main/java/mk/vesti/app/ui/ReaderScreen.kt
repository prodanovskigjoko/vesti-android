package mk.vesti.app.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import mk.vesti.core.Article

private sealed interface Page {
    data object Loading : Page
    data class Offline(val html: String) : Page
    data object Online : Page
}

/** Reads a saved article in-app: the downloaded copy when there is one, otherwise the live page. */
@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(article: Article, loadOffline: suspend (Article) -> String?, onBack: () -> Unit) {
    val context = LocalContext.current
    var page by remember(article.link) { mutableStateOf<Page>(Page.Loading) }
    LaunchedEffect(article.link) {
        page = loadOffline(article)?.let { Page.Offline(it) } ?: Page.Online
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(article.source, style = MaterialTheme.typography.titleMedium)
                        if (page is Page.Offline) {
                            Text(
                                "Офлајн копија",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val send = Intent(Intent.ACTION_SEND).setType("text/plain")
                            .putExtra(Intent.EXTRA_TEXT, "${article.title}\n${article.link}")
                        context.startActivity(Intent.createChooser(send, null))
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = "Сподели")
                    }
                },
            )
        },
    ) { padding ->
        when (val p = page) {
            Page.Loading -> LinearProgressIndicator(Modifier.padding(padding))
            else -> AndroidView(
                modifier = Modifier.padding(padding).fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        webViewClient = object : WebViewClient() {
                            // Links the reader taps open in the browser; redirects while loading stay here.
                            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                if (!request.hasGesture()) return false
                                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(request.url.toString())))
                                return true
                            }
                        }
                        if (p is Page.Offline) {
                            loadDataWithBaseURL(article.link, p.html, "text/html", "utf-8", null)
                        } else {
                            loadUrl(article.link)
                        }
                    }
                },
            )
        }
    }
}
