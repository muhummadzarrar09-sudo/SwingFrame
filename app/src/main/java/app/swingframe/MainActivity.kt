package app.swingframe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import app.swingframe.presentation.splash.AppSplashScreen
import app.swingframe.presentation.video.VideoPlayerScreen
import app.swingframe.ui.theme.SwingFrameTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SwingFrameTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var showSplash by remember { mutableStateOf(true) }

                    LaunchedEffect(Unit) {
                        delay(2500) // Show logo for 2.5 seconds
                        showSplash = false
                    }

                    if (showSplash) {
                        AppSplashScreen()
                    } else {
                        VideoPlayerScreen()
                    }
                }
            }
        }
    }
}
