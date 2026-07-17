package app.swingframe.ui

import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import app.swingframe.annotation.AnnotationShape
import app.swingframe.annotation.AnnotationStyle
import app.swingframe.annotation.AnnotationTool
import app.swingframe.model.IndexedVideo
import app.swingframe.model.SwingFrameUiState
import app.swingframe.model.TimelineThumbnail
import app.swingframe.project.FrameBookmark
import app.swingframe.ui.theme.SwingFrameColors
import app.swingframe.util.formatBitrate
import app.swingframe.util.formatDurationUs
import app.swingframe.util.formatFps
import app.swingframe.util.formatResolution
import app.swingframe.util.formatTimestampUs
import kotlin.math.roundToInt

@Composable
fun FrameViewerScreen(
    state: SwingFrameUiState,
    video: IndexedVideo,
    onBack: () -> Unit,
    onImportAnother: () -> Unit,
    onSeek: (Int) -> Unit,
    onStep: (Int) -> Unit,
    onTogglePlayback: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onAddBookmark: (String) -> Unit,
    onDeleteBookmark: (String) -> Unit,
    onJumpToBookmark: (Int) -> Unit,
    onExportStill: (Boolean) -> Unit,
    onExportVideo: (Int, Int, Float, Boolean) -> Unit,
    onCancelExport: () -> Unit,
    onDismissExportStatus: () -> Unit,
    onSelectAnnotationTool: (AnnotationTool) -> Unit,
    onSelectAnnotation: (String?) -> Unit,
    onAddAnnotation: (AnnotationShape) -> Unit,
    onReplaceAnnotation: (AnnotationShape) -> Unit,
    onSetAnnotationColor: (Int) -> Unit,
    onSetAnnotationStrokeWidth: (Float) -> Unit,
    onToggleAnnotationOverlay: () -> Unit,
    onToggleCarryForward: () -> Unit,
    onUndoAnnotation: () -> Unit,
    onRedoAnnotation: () -> Unit,
    onDeleteSelectedAnnotation: () -> Unit,
    onClearCurrentFrameAnnotations: () -> Unit,
    onDismissError: () -> Unit,
) {
    var showInfo by remember { mutableStateOf(false) }
    var showAddBookmark by remember { mutableStateOf(false) }
    var showBookmarkList by remember { mutableStateOf(false) }
    var showExportSetup by remember { mutableStateOf(false) }
    var scale by remember(video.source.uri) { mutableFloatStateOf(1f) }
    var panX by remember(video.source.uri) { mutableFloatStateOf(0f) }
    var panY by remember(video.source.uri) { mutableFloatStateOf(0f) }
    var stageSize by remember(video.source.uri) { mutableStateOf(IntSize.Zero) }
    val timestampUs = video.frameTimestampsUs.getOrElse(state.currentFrameIndex) { 0L }
    val frameContentReady = state.resolvedFrameIndex == state.currentFrameIndex && state.currentBitmap != null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SwingFrameColors.Background),
    ) {
        Column(Modifier.fillMaxSize()) {
            EditorHeader(
                title = video.source.displayName,
                frameIndex = state.currentFrameIndex,
                totalFrames = video.totalFrames,
                timestamp = formatTimestampUs(timestampUs),
                fps = formatFps(video.detectedFps),
                variableRate = video.isVariableFrameRate,
                zoomed = scale > 1.01f,
                onBack = onBack,
                onImportAnother = onImportAnother,
                onExport = { showExportSetup = true },
                onInfo = { showInfo = true },
                onAddBookmark = { showAddBookmark = true },
                onOpenBookmarks = { showBookmarkList = true },
                onResetZoom = {
                    scale = 1f
                    panX = 0f
                    panY = 0f
                },
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                FrameStage(
                    bitmap = state.currentBitmap,
                    scale = scale,
                    panX = panX,
                    panY = panY,
                    onTransform = { zoomChange, panChange ->
                        val nextScale = (scale * zoomChange).coerceIn(1f, 6f)
                        scale = nextScale
                        if (nextScale <= 1.01f) {
                            panX = 0f
                            panY = 0f
                        } else {
                            val maxPanX = stageSize.width * (nextScale - 1f) / 2f
                            val maxPanY = stageSize.height * (nextScale - 1f) / 2f
                            panX = (panX + panChange.x).coerceIn(-maxPanX, maxPanX)
                            panY = (panY + panChange.y).coerceIn(-maxPanY, maxPanY)
                        }
                    },
                    onResetZoom = {
                        scale = 1f
                        panX = 0f
                        panY = 0f
                    },
                    frameIndex = state.currentFrameIndex,
                    annotations = if (frameContentReady) state.currentFrameAnnotations else emptyList(),
                    carriedAnnotations = if (frameContentReady) state.carriedAnnotations else emptyList(),
                    selectedAnnotationId = if (frameContentReady) state.selectedAnnotationId else null,
                    annotationTool = state.annotationTool,
                    annotationStyle = AnnotationStyle(
                        colorArgb = state.annotationColorArgb,
                        strokeWidthDp = state.annotationStrokeWidthDp,
                    ),
                    annotationOverlayVisible = state.annotationOverlayVisible && frameContentReady,
                    onSelectAnnotation = onSelectAnnotation,
                    onAddAnnotation = onAddAnnotation,
                    onReplaceAnnotation = onReplaceAnnotation,
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged { stageSize = it },
                )

                AnimatedVisibility(
                    visible = state.isFrameLoading,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                ) {
                    FrameResolvingBadge()
                }
            }

            AnnotationToolBar(
                selectedTool = state.annotationTool,
                enabled = state.annotationOverlayVisible && frameContentReady,
                onSelectTool = onSelectAnnotationTool,
            )

            AnnotationActionBar(
                tool = state.annotationTool,
                enabled = frameContentReady,
                colorArgb = state.annotationColorArgb,
                strokeWidthDp = state.annotationStrokeWidthDp,
                overlayVisible = state.annotationOverlayVisible,
                carryForward = state.carryForwardEnabled,
                hasSelection = state.selectedAnnotationId != null,
                hasAnnotations = state.currentFrameAnnotations.isNotEmpty(),
                canUndo = state.canUndoAnnotations,
                canRedo = state.canRedoAnnotations,
                onSetColor = onSetAnnotationColor,
                onSetStrokeWidth = onSetAnnotationStrokeWidth,
                onToggleOverlay = onToggleAnnotationOverlay,
                onToggleCarryForward = onToggleCarryForward,
                onUndo = onUndoAnnotation,
                onRedo = onRedoAnnotation,
                onDelete = onDeleteSelectedAnnotation,
                onClear = onClearCurrentFrameAnnotations,
            )

            TimelineDock(
                thumbnails = state.timelineThumbnails,
                bookmarks = state.bookmarks,
                frameIndex = state.currentFrameIndex,
                totalFrames = video.totalFrames,
                timestamp = formatTimestampUs(timestampUs),
                isPlaying = state.isPlaying,
                speed = state.playbackSpeed,
                onSeek = onSeek,
                onStep = onStep,
                onTogglePlayback = onTogglePlayback,
                onSpeedChange = onSpeedChange,
            )
        }

        AnimatedVisibility(
            visible = state.errorMessage != null,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 20.dp),
        ) {
            state.errorMessage?.let { ErrorCard(it, onDismissError) }
        }
    }

    if (showInfo) SourceInfoDialog(video = video, onDismiss = { showInfo = false })
    if (showAddBookmark) {
        AddBookmarkDialog(
            frameIndex = state.currentFrameIndex,
            onDismiss = { showAddBookmark = false },
            onSave = onAddBookmark,
        )
    }
    if (showBookmarkList) {
        BookmarkListDialog(
            bookmarks = state.bookmarks,
            onDismiss = { showBookmarkList = false },
            onJump = onJumpToBookmark,
            onDelete = onDeleteBookmark,
        )
    }
    if (showExportSetup) {
        ExportSetupDialog(
            currentFrameIndex = state.currentFrameIndex,
            totalFrames = video.totalFrames,
            bookmarks = state.bookmarks,
            onDismiss = { showExportSetup = false },
            onExportStill = onExportStill,
            onExportVideo = onExportVideo,
        )
    }
    ExportStatusDialog(
        status = state.exportStatus,
        onCancel = onCancelExport,
        onDismiss = onDismissExportStatus,
    )
}

@Composable
private fun FrameStage(
    bitmap: android.graphics.Bitmap?,
    scale: Float,
    panX: Float,
    panY: Float,
    onTransform: (Float, Offset) -> Unit,
    onResetZoom: () -> Unit,
    frameIndex: Int,
    annotations: List<AnnotationShape>,
    carriedAnnotations: List<AnnotationShape>,
    selectedAnnotationId: String?,
    annotationTool: AnnotationTool,
    annotationStyle: AnnotationStyle,
    annotationOverlayVisible: Boolean,
    onSelectAnnotation: (String?) -> Unit,
    onAddAnnotation: (AnnotationShape) -> Unit,
    onReplaceAnnotation: (AnnotationShape) -> Unit,
    modifier: Modifier = Modifier,
) {
    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        onTransform(zoomChange, panChange)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, SwingFrameColors.Stroke, RoundedCornerShape(6.dp))
            .background(SwingFrameColors.Canvas)
            .then(
                if (!annotationOverlayVisible) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(onDoubleTap = { onResetZoom() })
                    }
                } else {
                    Modifier
                },
            )
            .transformable(transformableState),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = panX
                    translationY = panY
                },
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Exact current video frame",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            } else {
                Canvas(Modifier.fillMaxSize()) {
                    drawLine(
                        color = SwingFrameColors.Stroke,
                        start = Offset(size.width * 0.18f, size.height * 0.70f),
                        end = Offset(size.width * 0.72f, size.height * 0.28f),
                        strokeWidth = 1.dp.toPx(),
                    )
                    drawCircle(
                        color = SwingFrameColors.Accent.copy(alpha = 0.16f),
                        radius = 4.dp.toPx(),
                        center = center,
                    )
                }
            }

            AnnotationCanvas(
                frameIndex = frameIndex,
                bitmapWidth = bitmap?.width ?: 0,
                bitmapHeight = bitmap?.height ?: 0,
                annotations = annotations,
                carriedAnnotations = carriedAnnotations,
                selectedAnnotationId = selectedAnnotationId,
                tool = annotationTool,
                style = annotationStyle,
                overlayVisible = annotationOverlayVisible,
                onSelect = onSelectAnnotation,
                onAdd = onAddAnnotation,
                onReplace = onReplaceAnnotation,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun EditorHeader(
    title: String,
    frameIndex: Int,
    totalFrames: Int,
    timestamp: String,
    fps: String,
    variableRate: Boolean,
    zoomed: Boolean,
    onBack: () -> Unit,
    onImportAnother: () -> Unit,
    onExport: () -> Unit,
    onInfo: () -> Unit,
    onAddBookmark: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onResetZoom: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMoreMenu by remember { mutableStateOf(false) }
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = SwingFrameColors.Background.copy(alpha = 0.98f),
    ) {
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 7.dp, end = 7.dp, bottom = 10.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, "Back", tint = SwingFrameColors.TextPrimary)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "EXACT FRAME / ANALYSIS",
                        style = MaterialTheme.typography.labelSmall,
                        color = SwingFrameColors.Accent,
                    )
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        color = SwingFrameColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onExport) {
                    Icon(Icons.Filled.SaveAlt, "Export analysis", tint = SwingFrameColors.Accent)
                }
                Box {
                    IconButton(onClick = { showMoreMenu = true }) {
                        Icon(Icons.Filled.MoreVert, "More actions", tint = SwingFrameColors.TextSecondary)
                    }
                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false },
                    ) {
                        if (zoomed) {
                            DropdownMenuItem(
                                text = { Text("Reset zoom") },
                                leadingIcon = { Icon(Icons.Filled.AspectRatio, contentDescription = null) },
                                onClick = {
                                    showMoreMenu = false
                                    onResetZoom()
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Mark current frame") },
                            leadingIcon = { Icon(Icons.Filled.BookmarkAdd, contentDescription = null) },
                            onClick = {
                                showMoreMenu = false
                                onAddBookmark()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Marked frames") },
                            leadingIcon = { Icon(Icons.Filled.Bookmarks, contentDescription = null) },
                            onClick = {
                                showMoreMenu = false
                                onOpenBookmarks()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Open another video") },
                            leadingIcon = { Icon(Icons.Filled.FolderOpen, contentDescription = null) },
                            onClick = {
                                showMoreMenu = false
                                onImportAnother()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Source information") },
                            leadingIcon = { Icon(Icons.Filled.Info, contentDescription = null) },
                            onClick = {
                                showMoreMenu = false
                                onInfo()
                            },
                        )
                    }
                }
            }

            HorizontalDivider(color = SwingFrameColors.StrokeSoft)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EditorialStat("FRAME", "${frameIndex + 1} / $totalFrames", Modifier.weight(1.1f))
                StatRule()
                EditorialStat("TIME", timestamp, Modifier.weight(1f))
                StatRule()
                EditorialStat(
                    if (variableRate) "VFR" else "SOURCE",
                    fps,
                    Modifier.weight(0.9f),
                    accent = variableRate,
                )
            }
        }
    }
}

@Composable
private fun EditorialStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
) {
    Column(modifier.padding(horizontal = 8.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (accent) SwingFrameColors.Accent else SwingFrameColors.TextMuted,
        )
        Text(
            value,
            style = MaterialTheme.typography.labelLarge,
            color = SwingFrameColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
    }
}

@Composable
private fun StatRule() {
    Box(
        Modifier
            .size(width = 1.dp, height = 28.dp)
            .background(SwingFrameColors.Stroke),
    )
}

@Composable
private fun FrameResolvingBadge() {
    Surface(
        shape = RoundedCornerShape(3.dp),
        color = SwingFrameColors.Panel.copy(alpha = 0.94f),
        border = androidx.compose.foundation.BorderStroke(1.dp, SwingFrameColors.Stroke),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(12.dp),
                color = SwingFrameColors.Accent,
                strokeWidth = 1.5.dp,
            )
            Spacer(Modifier.width(7.dp))
            Text("RESOLVING", style = MaterialTheme.typography.labelSmall, color = SwingFrameColors.TextSecondary)
        }
    }
}

@Composable
private fun TimelineDock(
    thumbnails: List<TimelineThumbnail>,
    bookmarks: List<FrameBookmark>,
    frameIndex: Int,
    totalFrames: Int,
    timestamp: String,
    isPlaying: Boolean,
    speed: Float,
    onSeek: (Int) -> Unit,
    onStep: (Int) -> Unit,
    onTogglePlayback: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentBookmark = bookmarks.firstOrNull { it.frameIndex == frameIndex }
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = SwingFrameColors.Panel,
        border = androidx.compose.foundation.BorderStroke(1.dp, SwingFrameColors.StrokeSoft),
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 11.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    currentBookmark?.label?.uppercase() ?: "TIMELINE",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (currentBookmark != null) SwingFrameColors.Accent else SwingFrameColors.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "F ${frameIndex + 1}",
                    style = MaterialTheme.typography.labelLarge,
                    color = SwingFrameColors.Accent,
                )
                Spacer(Modifier.width(8.dp))
                Text(timestamp, style = MaterialTheme.typography.labelMedium, color = SwingFrameColors.TextSecondary)
            }

            Spacer(Modifier.height(8.dp))
            FilmstripScrubber(
                thumbnails = thumbnails,
                bookmarks = bookmarks,
                frameIndex = frameIndex,
                totalFrames = totalFrames,
                onFrameChange = onSeek,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
            )

            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TransportButton(
                    icon = Icons.Filled.SkipPrevious,
                    description = "Previous frame",
                    enabled = frameIndex > 0,
                    onClick = { onStep(-1) },
                )
                Spacer(Modifier.width(6.dp))
                Surface(
                    onClick = onTogglePlayback,
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = SwingFrameColors.Accent,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            if (isPlaying) "Pause" else "Play",
                            tint = SwingFrameColors.OnAccent,
                            modifier = Modifier.size(23.dp),
                        )
                    }
                }
                Spacer(Modifier.width(6.dp))
                TransportButton(
                    icon = Icons.Filled.SkipNext,
                    description = "Next frame",
                    enabled = frameIndex < totalFrames - 1,
                    onClick = { onStep(1) },
                )
                Spacer(Modifier.weight(1f))
                SpeedSelector(selected = speed, onSelect = onSpeedChange)
            }
        }
    }
}

@Composable
private fun FilmstripScrubber(
    thumbnails: List<TimelineThumbnail>,
    bookmarks: List<FrameBookmark>,
    frameIndex: Int,
    totalFrames: Int,
    onFrameChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val safeCount = totalFrames.coerceAtLeast(1)
    val progress = if (safeCount <= 1) 0f else frameIndex.toFloat() / (safeCount - 1).toFloat()

    Box(
        modifier = modifier
            .semantics {
                contentDescription = "Video timeline, frame ${frameIndex + 1} of $safeCount"
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = frameIndex.toFloat(),
                    range = 0f..(safeCount - 1).coerceAtLeast(0).toFloat(),
                    steps = (safeCount - 2).coerceAtLeast(0),
                )
                setProgress { value ->
                    onFrameChange(value.roundToInt().coerceIn(0, safeCount - 1))
                    true
                }
            }
            .clip(RoundedCornerShape(3.dp))
            .background(SwingFrameColors.Canvas)
            .border(1.dp, SwingFrameColors.Stroke, RoundedCornerShape(3.dp))
            .pointerInput(safeCount) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var lastFrame = -1
                    var lastHapticAt = 0L

                    fun seekAt(x: Float) {
                        val fraction = (x / size.width.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
                        val target = (fraction * (safeCount - 1)).roundToInt()
                        if (target != lastFrame) {
                            lastFrame = target
                            val now = SystemClock.uptimeMillis()
                            if (now - lastHapticAt >= HAPTIC_INTERVAL_MS) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                lastHapticAt = now
                            }
                            onFrameChange(target)
                        }
                    }

                    seekAt(down.position.x)
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        seekAt(change.position.x)
                        change.consume()
                    }
                }
            },
    ) {
        if (thumbnails.isEmpty()) {
            Row(Modifier.fillMaxSize()) {
                repeat(10) { index ->
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .background(
                                if (index % 2 == 0) SwingFrameColors.PanelElevated else SwingFrameColors.PanelSoft,
                            ),
                    )
                }
            }
        } else {
            Row(Modifier.fillMaxSize()) {
                thumbnails.sortedBy { it.frameIndex }.forEach { thumbnail ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                    ) {
                        Image(
                            bitmap = thumbnail.bitmap.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.18f)),
                        )
                    }
                }
            }
        }

        Canvas(Modifier.fillMaxSize()) {
            val x = size.width * progress
            drawRect(
                brush = Brush.horizontalGradient(
                    listOf(Color.Black.copy(alpha = 0.20f), Color.Transparent, Color.Black.copy(alpha = 0.20f)),
                ),
            )
            bookmarks.forEach { bookmark ->
                if (bookmark.frameIndex in 0 until safeCount) {
                    val markerProgress = if (safeCount <= 1) 0f else bookmark.frameIndex.toFloat() / (safeCount - 1)
                    val markerX = size.width * markerProgress
                    drawLine(
                        color = SwingFrameColors.AccentBright,
                        start = Offset(markerX, size.height - 9.dp.toPx()),
                        end = Offset(markerX, size.height),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                    drawCircle(
                        color = SwingFrameColors.Accent,
                        radius = 2.4.dp.toPx(),
                        center = Offset(markerX, size.height - 10.dp.toPx()),
                    )
                }
            }
            drawLine(
                color = SwingFrameColors.Accent,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Square,
            )
            val marker = Path().apply {
                moveTo(x - 5.dp.toPx(), 0f)
                lineTo(x + 5.dp.toPx(), 0f)
                lineTo(x, 6.dp.toPx())
                close()
            }
            drawPath(marker, SwingFrameColors.Accent)
        }
    }
}

@Composable
private fun TransportButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(48.dp),
        shape = CircleShape,
        color = Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(1.dp, SwingFrameColors.Stroke),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                description,
                modifier = Modifier.size(20.dp),
                tint = if (enabled) SwingFrameColors.TextPrimary else SwingFrameColors.TextMuted.copy(alpha = 0.38f),
            )
        }
    }
}

@Composable
private fun SpeedSelector(selected: Float, onSelect: (Float) -> Unit) {
    val speeds = listOf(0.1f, 0.25f, 0.5f, 1f)
    var expanded by remember { mutableStateOf(false) }
    Box {
        Surface(
            onClick = { expanded = true },
            modifier = Modifier.height(48.dp),
            shape = RoundedCornerShape(3.dp),
            color = SwingFrameColors.PanelSoft,
            border = androidx.compose.foundation.BorderStroke(1.dp, SwingFrameColors.StrokeSoft),
        ) {
            Row(
                modifier = Modifier.padding(start = 12.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (selected == 1f) "1×" else "${selected}×",
                    style = MaterialTheme.typography.labelLarge,
                    color = SwingFrameColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = SwingFrameColors.TextSecondary)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            speeds.forEach { value ->
                DropdownMenuItem(
                    text = { Text(if (value == 1f) "Normal · 1×" else "Slow · ${value}×") },
                    onClick = {
                        expanded = false
                        onSelect(value)
                    },
                )
            }
        }
    }
}

@Composable
private fun SourceInfoDialog(video: IndexedVideo, onDismiss: () -> Unit) {
    val metadata = video.source.metadata
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SwingFrameColors.Panel,
        titleContentColor = SwingFrameColors.TextPrimary,
        textContentColor = SwingFrameColors.TextSecondary,
        title = { Text("Source ledger") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DialogValue("File", video.source.displayName)
                DialogValue("Resolution", formatResolution(metadata.width, metadata.height, metadata.rotationDegrees))
                DialogValue("Duration", formatDurationUs(metadata.durationUs))
                DialogValue("Indexed frames", video.totalFrames.toString())
                DialogValue(
                    "Detected timing",
                    "${formatFps(video.detectedFps)}${if (video.isVariableFrameRate) " / variable" else ""}",
                )
                DialogValue("Codec", "${metadata.codecLabel} / ${metadata.mimeType}")
                DialogValue("Bitrate", formatBitrate(metadata.bitrate))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done", color = SwingFrameColors.Accent) }
        },
    )
}

@Composable
private fun DialogValue(label: String, value: String) {
    Column {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = SwingFrameColors.TextMuted)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = SwingFrameColors.TextPrimary)
    }
}

private const val HAPTIC_INTERVAL_MS = 28L
