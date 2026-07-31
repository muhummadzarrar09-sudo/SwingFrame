package app.swingframe.presentation.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.swingframe.domain.ai.FlawSeverity
import app.swingframe.domain.ai.SwingReport
import app.swingframe.ui.theme.BackgroundDark
import app.swingframe.ui.theme.DeepSpaceSparkle
import app.swingframe.ui.theme.MustardGreen
import app.swingframe.ui.theme.PhthaloGreen

@Composable
fun DiagnosticPanel(
    report: SwingReport,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.5f) // Take up bottom half of screen
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "AI DIAGNOSTICS",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                fontSize = 18.sp
            )
            Text(
                text = "CLOSE",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                modifier = Modifier.clickable { onClose() }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Overall Score
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(BackgroundDark)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("SWING SCORE", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            Text(
                text = "${report.score}",
                color = if (report.score > 80) MustardGreen else if (report.score > 60) DeepSpaceSparkle else PhthaloGreen,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Flaw List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(report.flaws) { flaw ->
                val cardColor = when (flaw.severity) {
                    FlawSeverity.CRITICAL -> PhthaloGreen.copy(alpha = 0.3f)
                    FlawSeverity.WARNING -> DeepSpaceSparkle.copy(alpha = 0.3f)
                    FlawSeverity.INFO -> BackgroundDark
                }
                
                val textColor = when (flaw.severity) {
                    FlawSeverity.CRITICAL -> MaterialTheme.colorScheme.error
                    FlawSeverity.WARNING -> MaterialTheme.colorScheme.tertiary
                    FlawSeverity.INFO -> MaterialTheme.colorScheme.primary
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(cardColor)
                        .padding(12.dp)
                ) {
                    Text(
                        text = flaw.name.uppercase(),
                        color = textColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = flaw.description,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}
