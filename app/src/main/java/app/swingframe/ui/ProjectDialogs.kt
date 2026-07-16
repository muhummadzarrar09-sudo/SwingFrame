package app.swingframe.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import app.swingframe.project.LocalProject
import app.swingframe.project.RelinkConflict
import app.swingframe.ui.theme.SwingFrameColors

@Composable
fun RelinkConflictDialog(
    conflict: RelinkConflict,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = SwingFrameColors.Panel,
        title = { Text("Replacement does not match", color = SwingFrameColors.TextPrimary) },
        text = {
            Text(
                "“${conflict.replacementName}” does not match the stored fingerprint for “${conflict.projectName}”. " +
                    "Using a different clip can misalign every annotation and bookmark. Continue only if this is a re-encoded copy of the same swing.",
                color = SwingFrameColors.TextSecondary,
            )
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("Cancel", color = SwingFrameColors.TextSecondary) }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Relink anyway", color = SwingFrameColors.Error) }
        },
    )
}

@Composable
fun DeleteProjectDialog(
    project: LocalProject,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = SwingFrameColors.Panel,
        title = { Text("Delete local project?", color = SwingFrameColors.TextPrimary) },
        text = {
            Text(
                "This removes “${project.displayName}” from Recents and deletes its local annotation vectors and bookmarks. The original video is not deleted.",
                color = SwingFrameColors.TextSecondary,
            )
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("Keep", color = SwingFrameColors.TextSecondary) }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Delete", color = SwingFrameColors.Error) }
        },
    )
}
