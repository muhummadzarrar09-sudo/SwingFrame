package app.swingframe.export

import android.net.Uri
import app.swingframe.annotation.AnnotationShape

enum class ExportType { STILL, VIDEO }

data class ExportStatus(
    val isExporting: Boolean = false,
    val type: ExportType? = null,
    val progressPercent: Int = 0,
    val message: String = "",
    val savedUri: Uri? = null,
    val errorMessage: String? = null,
)

data class VideoExportRequest(
    val sourceUri: Uri,
    val frameTimestampsUs: List<Long>,
    val startFrameIndex: Int,
    val endFrameIndex: Int,
    val playbackSpeed: Float,
    val burnAnnotations: Boolean,
    val carryForward: Boolean,
    val annotationsByFrame: Map<Int, List<AnnotationShape>>,
)
