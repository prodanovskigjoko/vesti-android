package mk.vesti.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Outlets the reader marked as favourites, by source name. */
class FavoritesRepository(context: Context) {
    private val prefs = context.getSharedPreferences("favorites", Context.MODE_PRIVATE)

    private val _sources = MutableStateFlow(prefs.getStringSet(KEY, emptySet()).orEmpty())
    val sources: StateFlow<Set<String>> = _sources

    fun toggle(source: String) {
        val current = _sources.value
        val updated = if (source in current) current - source else current + source
        prefs.edit().putStringSet(KEY, updated).apply()
        _sources.value = updated
    }

    private companion object {
        const val KEY = "sources"
    }
}
