package mk.vesti.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import mk.vesti.core.Article

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedScreen(
    articles: List<Article>,
    onOpen: (Article) -> Unit,
    onRemove: (Article) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Зачувани", style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        if (articles.isEmpty()) {
            Column(
                Modifier.padding(padding).fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("☆", style = MaterialTheme.typography.displaySmall)
                Spacer(Modifier.height(12.dp))
                Text(
                    "Притисни ја ѕвездата на која било вест за да ја зачуваш и да ја читаш и без интернет.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Scaffold
        }
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
            items(articles, key = { it.link }) { a ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth().animateItem(),
                ) {
                    Row(
                        Modifier.clickable { onOpen(a) }.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (a.imageUrl != null) {
                            NewsImage(a.imageUrl, Modifier.size(72.dp).clip(MaterialTheme.shapes.small))
                            Spacer(Modifier.width(12.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                "${a.source} · ${relativeTime(a.publishedAt)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                a.title,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        IconButton(onClick = { onRemove(a) }) {
                            SavedIcon(saved = true, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}
