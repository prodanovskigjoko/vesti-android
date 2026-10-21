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
    val saved by vm.saved.collectAsStateWithLifecycle()
    val savedLinks = saved.map { it.link }.toSet()

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
                savedCount = saved.size,
                onRefresh = { vm.refresh() },
                onTab = vm::selectTab,
                onOpen = { vm.open(Screen.StoryDetail(it)) },
                onOpenSaved = { vm.open(Screen.Saved) },
            )
            is Screen.StoryDetail -> StoryScreen(
                story = screen.story,
                savedLinks = savedLinks,
                onToggleSaved = vm::toggleSaved,
                onBack = vm::back,
            )
            Screen.Saved -> SavedScreen(
                articles = saved,
                onOpen = { vm.open(Screen.Reader(it)) },
                onRemove = vm::toggleSaved,
                onBack = vm::back,
            )
            is Screen.Reader -> ReaderScreen(
                article = screen.article,
                loadOffline = { vm.savedRepository.offlinePage(it) },
                onBack = vm::back,
            )
        }
    }
}
