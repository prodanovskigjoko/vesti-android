package mk.vesti.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun VestiApp(vm: NewsViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()

    BackHandler(enabled = state.backStack.isNotEmpty()) { vm.back() }
    AnimatedContent(
        targetState = state.screen,
        transitionSpec = {
            if (targetState != Screen.Feed) {
                (slideInHorizontally { it / 3 } + fadeIn()) togetherWith fadeOut()
            } else {
                fadeIn() togetherWith (slideOutHorizontally { it / 3 } + fadeOut())
            }
        },
        label = "screen",
    ) { screen ->
        when (screen) {
            Screen.Feed -> FeedScreen(
                state = state,
                favoriteCount = favorites.size,
                onRefresh = { vm.refresh() },
                onTab = vm::selectTab,
                onOpen = { vm.open(Screen.StoryDetail(it)) },
                onOpenFavorites = { vm.open(Screen.Favorites) },
            )
            is Screen.StoryDetail -> StoryScreen(
                story = screen.story,
                favorites = favorites,
                onToggleFavorite = vm::toggleFavorite,
                onBack = vm::back,
            )
            Screen.Favorites -> FavoritesScreen(
                stories = state.stories,
                favorites = favorites,
                onToggle = vm::toggleFavorite,
                onOpenStory = { vm.open(Screen.StoryDetail(it)) },
                onBack = vm::back,
            )
        }
    }
}
