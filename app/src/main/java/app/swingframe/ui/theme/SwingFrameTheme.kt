package app.swingframe.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** Quiet editorial palette: ink, warm paper, and one golf-amber interaction signal. */
object SwingFrameColors {
    val Background = Color(0xFF090A09)
    val Canvas = Color(0xFF050605)
    val Panel = Color(0xFF111310)
    val PanelElevated = Color(0xFF181A16)
    val PanelSoft = Color(0xFF20221D)
    val Stroke = Color(0xFF31332C)
    val StrokeSoft = Color(0xFF23251F)

    val Accent = Color(0xFFE5A63B)
    val AccentBright = Color(0xFFF1BC62)
    val AccentDeep = Color(0xFF8D5C16)
    val OnAccent = Color(0xFF211708)
    val Intelligence = Color(0xFF806BFF)

    val TextPrimary = Color(0xFFF1F0E9)
    val TextSecondary = Color(0xFFB7B5AC)
    val TextMuted = Color(0xFF77776F)
    val Paper = Color(0xFFE8E4D8)
    val Error = Color(0xFFFF756B)
    val Success = Color(0xFF8BCB9B)
}

private val SwingFrameColorScheme = darkColorScheme(
    primary = SwingFrameColors.Accent,
    onPrimary = SwingFrameColors.OnAccent,
    primaryContainer = Color(0xFF3A2912),
    onPrimaryContainer = SwingFrameColors.AccentBright,
    secondary = SwingFrameColors.TextSecondary,
    background = SwingFrameColors.Background,
    onBackground = SwingFrameColors.TextPrimary,
    surface = SwingFrameColors.Panel,
    onSurface = SwingFrameColors.TextPrimary,
    surfaceVariant = SwingFrameColors.PanelSoft,
    onSurfaceVariant = SwingFrameColors.TextSecondary,
    outline = SwingFrameColors.Stroke,
    error = SwingFrameColors.Error,
)

@Composable
fun SwingFrameTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.navigationBarColor = SwingFrameColors.Background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = SwingFrameColorScheme,
        typography = SwingFrameTypography,
        content = content,
    )
}
