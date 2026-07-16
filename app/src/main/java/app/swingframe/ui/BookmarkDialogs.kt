package app.swingframe.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.swingframe.project.FrameBookmark
import app.swingframe.ui.theme.SwingFrameColors

private val BookmarkPresets = listOf("Address", "Top", "Impact", "Finish")

@Composable
fun AddBookmarkDialog(
    frameIndex: Int,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var label by remember(frameIndex) { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SwingFrameColors.Panel,
        title = { Text("Mark frame ${frameIndex + 1}", color = SwingFrameColors.TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Choose a swing position or enter a custom label.",
                    color = SwingFrameColors.TextSecondary,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(BookmarkPresets) { preset ->
                        Surface(
                            onClick = { label = preset },
                            shape = RoundedCornerShape(3.dp),
                            color = if (label == preset) SwingFrameColors.Accent else SwingFrameColors.PanelSoft,
                        ) {
                            Text(
                                preset.uppercase(),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                color = if (label == preset) SwingFrameColors.OnAccent else SwingFrameColors.TextSecondary,
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it.take(40) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Label") },
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = SwingFrameColors.TextSecondary) }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (label.isNotBlank()) {
                        onSave(label)
                        onDismiss()
                    }
                },
                enabled = label.isNotBlank(),
            ) {
                Text("Save", color = if (label.isNotBlank()) SwingFrameColors.Accent else SwingFrameColors.TextMuted)
            }
        },
    )
}

@Composable
fun BookmarkListDialog(
    bookmarks: List<FrameBookmark>,
    onDismiss: () -> Unit,
    onJump: (Int) -> Unit,
    onDelete: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SwingFrameColors.Panel,
        title = { Text("Marked frames", color = SwingFrameColors.TextPrimary) },
        text = {
            if (bookmarks.isEmpty()) {
                Text("No marked frames yet.", color = SwingFrameColors.TextSecondary)
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(bookmarks, key = { it.id }) { bookmark ->
                        Surface(
                            onClick = {
                                onJump(bookmark.frameIndex)
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(3.dp),
                            color = SwingFrameColors.PanelSoft,
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        bookmark.label,
                                        color = SwingFrameColors.TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        "FRAME ${bookmark.frameIndex + 1}",
                                        color = SwingFrameColors.Accent,
                                    )
                                }
                                IconButton(onClick = { onDelete(bookmark.id) }) {
                                    Icon(
                                        Icons.Filled.DeleteOutline,
                                        contentDescription = "Delete bookmark",
                                        tint = SwingFrameColors.TextMuted,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done", color = SwingFrameColors.Accent) }
        },
    )
}
