package mk.vesti.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mk.vesti.app.data.NewsRepository
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

data class NewsUiState(
    val loading: Boolean = false,
    val stories: List<Story> = emptyList(),
    val tab: FeedTab = FeedTab.Top,
    val openStory: Story? = null,
    val failedSources: List<String> = emptyList(),
    val error: String? = null,
) {
    val visibleStories: List<Story>
        get() = when (tab) {
            FeedTab.Top -> stories
            FeedTab.Latest -> stories.sortedByDescending { it.latestAt }
            is FeedTab.Section -> stories.filter { it.category == tab.category }
        }
}

class NewsViewModel(private val repository: NewsRepository = NewsRepository()) : ViewModel() {
    private val _state = MutableStateFlow(NewsUiState())
    val state: StateFlow<NewsUiState> = _state

    init {
        refresh()
    }

    fun refresh() {
        if (_state.value.loading) return
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching { repository.load() }
                .onSuccess { r ->
                    _state.update {
                        it.copy(
                            loading = false,
                            stories = r.stories,
                            failedSources = r.failedSources,
                            error = if (r.stories.isEmpty()) "Не успеавме да вчитаме вести." else null,
                        )
                    }
                }
                .onFailure { e -> _state.update { it.copy(loading = false, error = e.message) } }
        }
    }

    fun selectTab(tab: FeedTab) = _state.update { it.copy(tab = tab) }

    fun openStory(story: Story?) = _state.update { it.copy(openStory = story) }
}
