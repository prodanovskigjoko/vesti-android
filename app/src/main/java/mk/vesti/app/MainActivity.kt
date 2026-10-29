package mk.vesti.app

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import mk.vesti.app.notify.BreakingNewsWorker
import mk.vesti.app.notify.OutageWorker
import mk.vesti.app.ui.NewsViewModel
import mk.vesti.app.ui.VestiApp
import mk.vesti.app.ui.theme.VestiTheme

class MainActivity : ComponentActivity() {
    private val vm: NewsViewModel by viewModels()

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        BreakingNewsWorker.schedule(this)
        OutageWorker.schedule(this)
        if (savedInstanceState == null) {
            if (Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            handle(intent)
        }
        setContent {
            VestiTheme {
                VestiApp(vm)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent) {
        if (intent.getBooleanExtra(EXTRA_OPEN_OUTAGES, false)) vm.openOutages()
        else vm.openFromNotification(intent.getStringExtra(EXTRA_STORY_LINK))
    }

    companion object {
        const val EXTRA_STORY_LINK = "story_link"
        const val EXTRA_OPEN_OUTAGES = "open_outages"
    }
}
