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

data class NewsUiState(
    val loading: Boolean = false,
    val stories: List<Story> = emptyList(),
    /** null shows every category, like time.mk's front page. */
    val category: Category? = null,
    val openStory: Story? = null,
    val failedSources: List<String> = emptyList(),
    val error: String? = null,
) {
    val visibleStories: List<Story>
        get() = if (category == null) stories else stories.filter { it.category == category }
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

    fun selectCategory(category: Category?) = _state.update { it.copy(category = category) }

    fun openStory(story: Story?) = _state.update { it.copy(openStory = story) }
}
