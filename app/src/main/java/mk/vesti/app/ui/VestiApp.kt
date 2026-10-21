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
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun VestiApp(vm: NewsViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    AnimatedContent(
        targetState = state.openStory,
        transitionSpec = {
            if (targetState != null) {
                (slideInHorizontally { it / 3 } + fadeIn()) togetherWith fadeOut()
            } else {
                fadeIn() togetherWith (slideOutHorizontally { it / 3 } + fadeOut())
            }
        },
        label = "screen",
    ) { story ->
        if (story != null) {
            BackHandler { vm.openStory(null) }
            StoryScreen(story, onBack = { vm.openStory(null) })
        } else {
            FeedScreen(state, vm::refresh, vm::selectCategory, vm::openStory)
        }
    }
}
