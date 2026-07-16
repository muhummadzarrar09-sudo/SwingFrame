package app.swingframe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import app.swingframe.ui.SwingFrameApp
import app.swingframe.ui.theme.SwingFrameTheme

class MainActivity : ComponentActivity() {
    private val viewModel: SwingFrameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SwingFrameTheme {
                SwingFrameApp(viewModel = viewModel)
            }
        }
    }
}
