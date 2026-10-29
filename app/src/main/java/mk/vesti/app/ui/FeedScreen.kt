package mk.vesti.app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import mk.vesti.app.ui.theme.accent
import mk.vesti.core.Story

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FeedScreen(
    state: NewsUiState,
    favoriteCount: Int,
    onRefresh: () -> Unit,
    onTab: (FeedTab) -> Unit,
    onOpen: (Story) -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenOutages: () -> Unit,
    hasAddresses: Boolean,
    onLoadMore: () -> Unit,
    /** Hoisted so the scroll position survives opening a story and coming back. */
    listState: LazyListState = rememberLazyListState(),
) {
    val stories = state.visibleStories
    val scope = rememberCoroutineScope()
    // Next page once the reader is within a few rows of the end.
    val nearEnd by remember(listState) {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && last >= info.totalItemsCount - 4
        }
    }
    LaunchedEffect(nearEnd, state.hasMore, state.shownCount) {
        if (nearEnd && state.hasMore) onLoadMore()
    }
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = state.loading && state.stories.isNotEmpty(),
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
        ) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(bottom = 24.dp),
                modifier = Modifier.fillMaxSize().navigationBarsPadding(),
            ) {
                item { Header(favoriteCount, onOpenFavorites, onOpenAbout) }
                item { WidgetsRow(state.widgets, state.outages, hasAddresses, onOpenOutages) }
                stickyHeader {
                    TabChips(state.tab) { tab ->
                        onTab(tab)
                        scope.launch { listState.scrollToItem(0) }
                    }
                }
                if (state.offline) item { OfflineBanner() }

                when {
                    state.loading && state.stories.isEmpty() -> item { SkeletonFeed() }
                    stories.isEmpty() -> item { EmptyState(state.error, onRefresh) }
                    else -> {
                        item(key = "hero:" + stories.first().lead.link) {
                            HeroCard(stories.first(), onClick = { onOpen(stories.first()) })
                        }
                        itemsIndexed(stories.drop(1), key = { _, s -> s.lead.link }) { i, story ->
                            Column(Modifier.animateItem()) {
                                if (i > 0) {
                                    HorizontalDivider(
                                        Modifier.padding(horizontal = 16.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    )
                                }
                                CompactStoryRow(story, onClick = { onOpen(story) })
                            }
                        }
                        if (state.hasMore) {
                            item(key = "more") {
                                Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(favoriteCount: Int, onOpenFavorites: () -> Unit, onOpenAbout: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                todayLabel(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text("Вести", style = MaterialTheme.typography.displaySmall)
        }
        IconButton(onClick = onOpenAbout, modifier = Modifier.padding(bottom = 6.dp)) {
            Icon(Icons.Outlined.Info, contentDescription = "За апликацијата")
        }
        BadgedBox(
            badge = { if (favoriteCount > 0) Badge { Text("$favoriteCount") } },
            modifier = Modifier.padding(bottom = 6.dp),
        ) {
            FilledTonalIconButton(onClick = onOpenFavorites) {
                Icon(Icons.Filled.FavoriteBorder, contentDescription = "Омилени медиуми")
            }
        }
    }
}

@Composable
private fun OfflineBanner() {
    Text(
        "Нема интернет. Ги гледаш последните вчитани вести.",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(12.dp),
    )
}

@Composable
private fun TabChips(selected: FeedTab, onSelect: (FeedTab) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background),
    ) {
        items(FeedTab.all) { tab ->
            val section = tab as? FeedTab.Section
            val accent = section?.category?.accent() ?: MaterialTheme.colorScheme.onSurface
            FilterChip(
                selected = tab == selected,
                onClick = { onSelect(tab) },
                label = { Text(tab.label) },
                // A red "live" dot marks the newest-first feed.
                leadingIcon = if (tab == FeedTab.Latest) {
                    { Box(Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary)) }
                } else {
                    null
                },
                shape = MaterialTheme.shapes.large,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = accent,
                    selectedLabelColor = if (section == null) MaterialTheme.colorScheme.surface else Color.White,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = tab == selected,
                    borderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
            )
        }
    }
}

/** The top story: full-bleed image with the headline laid over a dark gradient. */
@Composable
private fun HeroCard(story: Story, onClick: () -> Unit) {
    val lead = story.lead
    Box(
        Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .height(320.dp)
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
    ) {
        NewsImage(lead.imageUrl, Modifier.fillMaxSize())
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.35f to Color.Black.copy(alpha = 0.15f),
                    1f to Color.Black.copy(alpha = 0.88f),
                ),
            ),
        )
        Column(Modifier.align(Alignment.BottomStart).padding(20.dp)) {
            CategoryPill(story.category, onImage = true)
            Spacer(Modifier.height(10.dp))
            Text(
                lead.title,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(12.dp))
            MetaLine(story, color = Color.White.copy(alpha = 0.85f))
        }
    }
}

@Composable
private fun CompactStoryRow(story: Story, onClick: () -> Unit) {
    val lead = story.lead
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            CategoryPill(story.category)
            Spacer(Modifier.height(8.dp))
            Text(
                lead.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            MetaLine(story, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (lead.imageUrl != null) {
            Spacer(Modifier.width(14.dp))
            NewsImage(lead.imageUrl, Modifier.size(96.dp).clip(MaterialTheme.shapes.medium))
        }
    }
}

/** Outlet avatars, "Телма и уште 4" and the age of the latest report. */
@Composable
private fun MetaLine(story: Story, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        SourceAvatars(story.articles.map { it.source })
        Spacer(Modifier.width(8.dp))
        val others = story.sourceCount - 1
        val who = if (others > 0) "${story.lead.source} и уште $others" else story.lead.source
        Text(
            "$who · ${relativeTime(story.latestAt)}",
            style = MaterialTheme.typography.labelMedium,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun EmptyState(error: String?, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("📰", style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(12.dp))
        Text(error ?: "Нема вести во оваа категорија.", style = MaterialTheme.typography.titleMedium)
        if (error != null) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry) { Text("Обиди се повторно") }
        }
    }
}
