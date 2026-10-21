package mk.vesti.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import mk.vesti.app.ui.VestiApp
import mk.vesti.app.ui.theme.VestiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VestiTheme {
                VestiApp()
            }
        }
    }
}
