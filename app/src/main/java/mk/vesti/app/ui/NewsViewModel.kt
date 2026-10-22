package mk.vesti.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mk.vesti.app.data.NewsRepository
import mk.vesti.app.data.FavoritesRepository
import mk.vesti.core.Category
import mk.vesti.core.Story

/** The chips above the feed: the ranked front page, newest first, or one category. */
sealed interface FeedTab {
    val label: String

    data object Top : FeedTab {
        override val label = "Сите"
    }

    data object Latest : FeedTab {
        override val label = "Најнови вести"
    }

    data class Section(val category: Category) : FeedTab {
        override val label = category.label
    }

    companion object {
        val all: List<FeedTab> = listOf(Top, Latest) + Category.entries.map(::Section)
    }
}

sealed interface Screen {
    data object Feed : Screen
    data class StoryDetail(val story: Story) : Screen
    data object Favorites : Screen
}

data class NewsUiState(
    val loading: Boolean = false,
    val stories: List<Story> = emptyList(),
    val tab: FeedTab = FeedTab.Top,
    /** Screens opened on top of the feed; the last one is visible. */
    val backStack: List<Screen> = emptyList(),
    /** True while showing stories from the cache because the last refresh got nothing. */
    val offline: Boolean = false,
    val failedSources: List<String> = emptyList(),
    val error: String? = null,
) {
    val screen: Screen get() = backStack.lastOrNull() ?: Screen.Feed

    val visibleStories: List<Story>
        get() = when (tab) {
            FeedTab.Top -> stories
            FeedTab.Latest -> stories.sortedByDescending { it.latestAt }
            is FeedTab.Section -> stories.filter { it.category == tab.category }
        }
}

class NewsViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = NewsRepository(app)
    private val favoritesRepository = FavoritesRepository(app)
    val favorites: StateFlow<Set<String>> = favoritesRepository.sources

    private val _state = MutableStateFlow(NewsUiState(loading = true))
    val state: StateFlow<NewsUiState> = _state

    /** Story link from a tapped notification, opened once the feed has loaded. */
    private var pendingStoryLink: String? = null

    init {
        viewModelScope.launch {
            val cached = repository.cached()
            _state.update { if (it.stories.isEmpty()) it.copy(stories = cached) else it }
            refresh(force = true)
        }
    }

    fun refresh(force: Boolean = false) {
        if (_state.value.loading && !force) return
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching { repository.load() }
                .onSuccess { r ->
                    _state.update {
                        val empty = r.stories.isEmpty()
                        it.copy(
                            loading = false,
                            stories = if (empty) it.stories else r.stories,
                            offline = empty && it.stories.isNotEmpty(),
                            failedSources = r.failedSources,
                            error = if (empty && it.stories.isEmpty()) "Не успеавме да вчитаме вести. Провери ја интернет врската." else null,
                        )
                    }
                }
                .onFailure { e -> _state.update { it.copy(loading = false, offline = it.stories.isNotEmpty(), error = e.message) } }
            openPendingStory()
        }
    }

    fun selectTab(tab: FeedTab) = _state.update { it.copy(tab = tab) }

    fun open(screen: Screen) = _state.update { it.copy(backStack = it.backStack + screen) }

    fun back() = _state.update { it.copy(backStack = it.backStack.dropLast(1)) }

    fun openFromNotification(link: String?) {
        pendingStoryLink = link ?: return
        if (!_state.value.loading) openPendingStory()
    }

    private fun openPendingStory() {
        val link = pendingStoryLink ?: return
        val story = _state.value.stories.firstOrNull { s -> s.articles.any { it.link == link } } ?: return
        pendingStoryLink = null
        _state.update { it.copy(backStack = listOf(Screen.StoryDetail(story))) }
    }

    fun toggleFavorite(source: String) = favoritesRepository.toggle(source)
}
