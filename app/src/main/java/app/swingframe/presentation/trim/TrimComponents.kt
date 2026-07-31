package app.swingframe.presentation.trim

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.swingframe.ui.theme.*

@Composable
fun RangeSliderTrimmer(
    duration: Long,
    startPosition: Long,
    endPosition: Long,
    onStartChange: (Long) -> Unit,
    onEndChange: (Long) -> Unit
) {
    if (duration <= 0) return

    val startFloat = startPosition.toFloat() / duration
    val endFloat = endPosition.toFloat() / duration

    var sliderPosition by remember(startFloat, endFloat) {
        mutableStateOf(startFloat..endFloat)
    }

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text(
            text = "TRIM SWING CLIP",
            color = MaterialTheme.colorScheme.tertiary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        RangeSlider(
            value = sliderPosition,
            onValueChange = { range ->
                sliderPosition = range
            },
            onValueChangeFinished = {
                val newStart = (sliderPosition.start * duration).toLong()
                val newEnd = (sliderPosition.endInclusive * duration).toLong()
                onStartChange(newStart)
                onEndChange(newEnd)
            },
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
            )
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(formatTime(startPosition), color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
            Text(formatTime(endPosition), color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val milliseconds = (ms % 1000) / 10
    return String.format("%02d:%02d.%02d", minutes, seconds, milliseconds)
}
