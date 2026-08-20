package app.swingframe

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
                    var showSplash by rememberSaveable { mutableStateOf(true) }
                    // The deadline survives configuration changes, so a rotation mid-splash
                    // does not restart the full 2.5 s delay.
                    val splashDeadline = rememberSaveable {
                        SystemClock.uptimeMillis() + SPLASH_DURATION_MS
                    }

                    LaunchedEffect(Unit) {
                        val remaining = splashDeadline - SystemClock.uptimeMillis()
                        if (remaining > 0) delay(remaining)
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

    private companion object {
        const val SPLASH_DURATION_MS = 2500L
    }
}
