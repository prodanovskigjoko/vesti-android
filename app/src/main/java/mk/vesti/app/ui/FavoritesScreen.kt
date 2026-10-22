package mk.vesti.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import mk.vesti.core.Article
import mk.vesti.core.DefaultSources
import mk.vesti.core.Story

/**
 * "Омилени медиуми": pick outlets, then read only their reports, newest first.
 * Tapping a report opens the whole story, so the other outlets' takes are one tap away.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    stories: List<Story>,
    favorites: Set<String>,
    onToggle: (String) -> Unit,
    onOpenStory: (Story) -> Unit,
    onBack: () -> Unit,
) {
    var editing by rememberSaveable { mutableStateOf(favorites.isEmpty()) }
    // Only one outlet's chip can be focused at a time; null shows all favourites.
    var only by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Омилени медиуми", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    if (favorites.isNotEmpty()) {
                        TextButton(onClick = { editing = !editing }) { Text(if (editing) "Готово" else "Уреди") }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        if (editing) {
            SourcePicker(favorites, onToggle, Modifier.padding(padding))
        } else {
            val shown = if (only != null && only in favorites) setOf(only!!) else favorites
            FavoritesFeed(stories, favorites, shown, only, onOnly = { only = it }, onOpenStory, Modifier.padding(padding))
        }
    }
}

@Composable
private fun SourcePicker(favorites: Set<String>, onToggle: (String) -> Unit, modifier: Modifier) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Text(
                "Избери ги медиумите што ги следиш. Нивните вести ќе ги гледаш овде, најновите први.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }
        items(DefaultSources, key = { it.name }) { source ->
            val on = source.name in favorites
            Row(
                Modifier.fillMaxWidth().clickable { onToggle(source.name) }.padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SourceBadge(source.name, 40)
                Spacer(Modifier.width(14.dp))
                Text(source.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Switch(checked = on, onCheckedChange = { onToggle(source.name) })
            }
        }
    }
}

@Composable
private fun FavoritesFeed(
    stories: List<Story>,
    favorites: Set<String>,
    shown: Set<String>,
    only: String?,
    onOnly: (String?) -> Unit,
    onOpenStory: (Story) -> Unit,
    modifier: Modifier,
) {
    val reports: List<Pair<Article, Story>> = stories
        .flatMap { s -> s.articles.filter { it.source in shown }.map { it to s } }
        .sortedByDescending { it.first.publishedAt }

    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    FilterChip(selected = only == null, onClick = { onOnly(null) }, label = { Text("Сите омилени") })
                }
                items(favorites.sorted()) { name ->
                    FilterChip(
                        selected = only == name,
                        onClick = { onOnly(if (only == name) null else name) },
                        label = { Text(name) },
                        leadingIcon = { SourceBadge(name, 20) },
                    )
                }
            }
        }
        if (reports.isEmpty()) {
            item {
                Text(
                    "Сè уште нема вести од избраните медиуми. Повлечи надолу на насловната за освежување.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp),
                )
            }
        }
        items(reports, key = { it.first.link }) { (article, story) ->
            Column(Modifier.animateItem()) {
                ReportRow(article, story, onClick = { onOpenStory(story) })
                HorizontalDivider(
                    Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )
            }
        }
    }
}

@Composable
private fun ReportRow(article: Article, story: Story, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SourceBadge(article.source, 20)
                Spacer(Modifier.width(8.dp))
                Text(
                    "${article.source} · ${relativeTime(article.publishedAt)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(article.title, style = MaterialTheme.typography.titleMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
            if (story.sourceCount > 1) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Пишуваат уште ${story.sourceCount - 1} медиуми",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        if (article.imageUrl != null) {
            Spacer(Modifier.width(14.dp))
            NewsImage(article.imageUrl, Modifier.size(88.dp).clip(MaterialTheme.shapes.medium))
        }
    }
}

/** Round initial in the outlet's colour, the same mark used across the app. */
@Composable
private fun SourceBadge(name: String, sizeDp: Int) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(sizeDp.dp).clip(CircleShape).background(sourceColor(name)),
    ) {
        Text(
            name.first().uppercase(Macedonian),
            color = Color.White,
            style = if (sizeDp >= 32) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelSmall,
        )
    }
}
