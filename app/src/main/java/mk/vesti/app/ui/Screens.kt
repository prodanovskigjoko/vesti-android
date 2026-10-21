package mk.vesti.app.ui

import android.content.Intent
import android.net.Uri
import android.text.format.DateUtils
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import mk.vesti.core.Article
import mk.vesti.core.Category
import mk.vesti.core.Story

@Composable
fun VestiApp(vm: NewsViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val open = state.openStory
    if (open != null) {
        BackHandler { vm.openStory(null) }
        StoryScreen(open, onBack = { vm.openStory(null) })
    } else {
        FeedScreen(state, vm::refresh, vm::selectCategory, vm::openStory)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedScreen(
    state: NewsUiState,
    onRefresh: () -> Unit,
    onCategory: (Category?) -> Unit,
    onOpen: (Story) -> Unit,
) {
    val tabs = listOf<Category?>(null) + Category.entries
    Scaffold(
        topBar = {
            Column {
                TopAppBar(title = { Text("Вести", fontWeight = FontWeight.Bold) })
                PrimaryScrollableTabRow(selectedTabIndex = tabs.indexOf(state.category), edgePadding = 8.dp) {
                    tabs.forEach { c ->
                        Tab(
                            selected = c == state.category,
                            onClick = { onCategory(c) },
                            text = { Text(c?.label ?: "Сите") },
                        )
                    }
                }
            }
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.loading,
            onRefresh = onRefresh,
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                state.error?.let { msg -> item { Text(msg, color = MaterialTheme.colorScheme.error) } }
                items(state.visibleStories, key = { it.lead.link }) { story ->
                    StoryCard(story, onClick = { onOpen(story) })
                }
            }
        }
    }
}

@Composable
private fun StoryCard(story: Story, onClick: () -> Unit) {
    val lead = story.lead
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column {
            lead.imageUrl?.let {
                AsyncImage(
                    model = it,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                )
            }
            Column(Modifier.padding(12.dp)) {
                Text(lead.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                if (lead.summary.isNotBlank()) {
                    Text(
                        lead.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(8.dp))
                }
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text(
                        "${lead.source} · ${relativeTime(story.latestAt)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    if (story.sourceCount > 1) {
                        AssistChip(onClick = onClick, label = { Text("${story.sourceCount} извори") })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StoryScreen(story: Story, onBack: () -> Unit) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(story.category.label) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(12.dp)) {
            item {
                Text(story.lead.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
            }
            items(story.articles.sortedByDescending { it.publishedAt }, key = { it.link }) { a ->
                ArticleRow(a) {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(a.link)))
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun ArticleRow(article: Article, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp)) {
        article.imageUrl?.let {
            AsyncImage(
                model = it,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(72.dp).clip(RoundedCornerShape(8.dp)),
            )
            Spacer(Modifier.size(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                "${article.source} · ${relativeTime(article.publishedAt)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(article.title, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

private fun relativeTime(millis: Long): String =
    if (millis == 0L) "" else DateUtils.getRelativeTimeSpanString(millis).toString()
