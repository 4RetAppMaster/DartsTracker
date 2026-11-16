package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Lightweight bundled subset of the 20 Material extended icons used by DartsTracker,
 * replacing the ~15 MB material-icons-extended artifact.
 */
object AppIcons {
    private inline fun materialIconVector(
        name: String,
        autoMirror: Boolean = false,
        block: PathBuilder.() -> Unit
    ): ImageVector = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
        autoMirror = autoMirror
    ).apply {
        path(fill = SolidColor(Color.Black), pathBuilder = block)
    }.build()

    val AddCircleOutline: ImageVector by lazy {
        materialIconVector("AddCircleOutline") {
            moveTo(13.0f, 7.0f)
            horizontalLineToRelative(-2.0f)
            verticalLineToRelative(4.0f)
            lineTo(7.0f, 11.0f)
            verticalLineToRelative(2.0f)
            horizontalLineToRelative(4.0f)
            verticalLineToRelative(4.0f)
            horizontalLineToRelative(2.0f)
            verticalLineToRelative(-4.0f)
            horizontalLineToRelative(4.0f)
            verticalLineToRelative(-2.0f)
            horizontalLineToRelative(-4.0f)
            lineTo(13.0f, 7.0f)
            close()
            moveTo(12.0f, 2.0f)
            curveTo(6.48f, 2.0f, 2.0f, 6.48f, 2.0f, 12.0f)
            reflectiveCurveToRelative(4.48f, 10.0f, 10.0f, 10.0f)
            reflectiveCurveToRelative(10.0f, -4.48f, 10.0f, -10.0f)
            reflectiveCurveTo(17.52f, 2.0f, 12.0f, 2.0f)
            close()
            moveTo(12.0f, 20.0f)
            curveToRelative(-4.41f, 0.0f, -8.0f, -3.59f, -8.0f, -8.0f)
            reflectiveCurveToRelative(3.59f, -8.0f, 8.0f, -8.0f)
            reflectiveCurveToRelative(8.0f, 3.59f, 8.0f, 8.0f)
            reflectiveCurveToRelative(-3.59f, 8.0f, -8.0f, 8.0f)
            close()
        }
    }

    val RemoveCircleOutline: ImageVector by lazy {
        materialIconVector("RemoveCircleOutline") {
            moveTo(7.0f, 11.0f)
            verticalLineToRelative(2.0f)
            horizontalLineToRelative(10.0f)
            verticalLineToRelative(-2.0f)
            lineTo(7.0f, 11.0f)
            close()
            moveTo(12.0f, 2.0f)
            curveTo(6.48f, 2.0f, 2.0f, 6.48f, 2.0f, 12.0f)
            reflectiveCurveToRelative(4.48f, 10.0f, 10.0f, 10.0f)
            reflectiveCurveToRelative(10.0f, -4.48f, 10.0f, -10.0f)
            reflectiveCurveTo(17.52f, 2.0f, 12.0f, 2.0f)
            close()
            moveTo(12.0f, 20.0f)
            curveToRelative(-4.41f, 0.0f, -8.0f, -3.59f, -8.0f, -8.0f)
            reflectiveCurveToRelative(3.59f, -8.0f, 8.0f, -8.0f)
            reflectiveCurveToRelative(8.0f, 3.59f, 8.0f, 8.0f)
            reflectiveCurveToRelative(-3.59f, 8.0f, -8.0f, 8.0f)
            close()
        }
    }

    val Adjust: ImageVector by lazy {
        materialIconVector("Adjust") {
            moveTo(12.0f, 2.0f)
            curveTo(6.49f, 2.0f, 2.0f, 6.49f, 2.0f, 12.0f)
            reflectiveCurveToRelative(4.49f, 10.0f, 10.0f, 10.0f)
            reflectiveCurveToRelative(10.0f, -4.49f, 10.0f, -10.0f)
            reflectiveCurveTo(17.51f, 2.0f, 12.0f, 2.0f)
            close()
            moveTo(12.0f, 20.0f)
            curveToRelative(-4.41f, 0.0f, -8.0f, -3.59f, -8.0f, -8.0f)
            reflectiveCurveToRelative(3.59f, -8.0f, 8.0f, -8.0f)
            reflectiveCurveToRelative(8.0f, 3.59f, 8.0f, 8.0f)
            reflectiveCurveToRelative(-3.59f, 8.0f, -8.0f, 8.0f)
            close()
            moveTo(15.0f, 12.0f)
            curveToRelative(0.0f, 1.66f, -1.34f, 3.0f, -3.0f, 3.0f)
            reflectiveCurveToRelative(-3.0f, -1.34f, -3.0f, -3.0f)
            reflectiveCurveToRelative(1.34f, -3.0f, 3.0f, -3.0f)
            reflectiveCurveToRelative(3.0f, 1.34f, 3.0f, 3.0f)
            close()
        }
    }

    val CloudOff: ImageVector by lazy {
        materialIconVector("CloudOff") {
            moveTo(19.35f, 10.04f)
            curveTo(18.67f, 6.59f, 15.64f, 4.0f, 12.0f, 4.0f)
            curveToRelative(-1.48f, 0.0f, -2.85f, 0.43f, -4.01f, 1.17f)
            lineToRelative(1.46f, 1.46f)
            curveTo(10.21f, 6.23f, 11.08f, 6.0f, 12.0f, 6.0f)
            curveToRelative(3.04f, 0.0f, 5.5f, 2.46f, 5.5f, 5.5f)
            verticalLineToRelative(0.5f)
            horizontalLineTo(19.0f)
            curveToRelative(1.66f, 0.0f, 3.0f, 1.34f, 3.0f, 3.0f)
            curveToRelative(0.0f, 1.13f, -0.64f, 2.11f, -1.56f, 2.62f)
            lineToRelative(1.45f, 1.45f)
            curveTo(23.16f, 18.16f, 24.0f, 16.68f, 24.0f, 15.0f)
            curveToRelative(0.0f, -2.64f, -2.05f, -4.78f, -4.65f, -4.96f)
            close()
            moveTo(3.0f, 5.27f)
            lineToRelative(2.75f, 2.74f)
            curveTo(2.56f, 8.15f, 0.0f, 10.77f, 0.0f, 14.0f)
            curveToRelative(0.0f, 3.31f, 2.69f, 6.0f, 6.0f, 6.0f)
            horizontalLineToRelative(11.73f)
            lineToRelative(2.0f, 2.0f)
            lineTo(21.0f, 20.73f)
            lineTo(4.27f, 4.0f)
            lineTo(3.0f, 5.27f)
            close()
            moveTo(7.73f, 10.0f)
            lineToRelative(8.0f, 8.0f)
            horizontalLineTo(6.0f)
            curveToRelative(-2.21f, 0.0f, -4.0f, -1.79f, -4.0f, -4.0f)
            reflectiveCurveToRelative(1.79f, -4.0f, 4.0f, -4.0f)
            horizontalLineToRelative(1.73f)
            close()
        }
    }

    val ContentCopy: ImageVector by lazy {
        materialIconVector("ContentCopy") {
            moveTo(16.0f, 1.0f)
            lineTo(4.0f, 1.0f)
            curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
            verticalLineToRelative(14.0f)
            horizontalLineToRelative(2.0f)
            lineTo(4.0f, 3.0f)
            horizontalLineToRelative(12.0f)
            lineTo(16.0f, 1.0f)
            close()
            moveTo(19.0f, 5.0f)
            lineTo(8.0f, 5.0f)
            curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
            verticalLineToRelative(14.0f)
            curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
            horizontalLineToRelative(11.0f)
            curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
            lineTo(21.0f, 7.0f)
            curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
            close()
            moveTo(19.0f, 21.0f)
            lineTo(8.0f, 21.0f)
            lineTo(8.0f, 7.0f)
            horizontalLineToRelative(11.0f)
            verticalLineToRelative(14.0f)
            close()
        }
    }

    val ContentPaste: ImageVector by lazy {
        materialIconVector("ContentPaste") {
            moveTo(19.0f, 2.0f)
            horizontalLineToRelative(-4.18f)
            curveTo(14.4f, 0.84f, 13.3f, 0.0f, 12.0f, 0.0f)
            curveToRelative(-1.3f, 0.0f, -2.4f, 0.84f, -2.82f, 2.0f)
            lineTo(5.0f, 2.0f)
            curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
            verticalLineToRelative(16.0f)
            curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
            horizontalLineToRelative(14.0f)
            curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
            lineTo(21.0f, 4.0f)
            curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
            close()
            moveTo(12.0f, 2.0f)
            curveToRelative(0.55f, 0.0f, 1.0f, 0.45f, 1.0f, 1.0f)
            reflectiveCurveToRelative(-0.45f, 1.0f, -1.0f, 1.0f)
            reflectiveCurveToRelative(-1.0f, -0.45f, -1.0f, -1.0f)
            reflectiveCurveToRelative(0.45f, -1.0f, 1.0f, -1.0f)
            close()
            moveTo(19.0f, 20.0f)
            lineTo(5.0f, 20.0f)
            lineTo(5.0f, 4.0f)
            horizontalLineToRelative(2.0f)
            verticalLineToRelative(3.0f)
            horizontalLineToRelative(10.0f)
            lineTo(17.0f, 4.0f)
            horizontalLineToRelative(2.0f)
            verticalLineToRelative(16.0f)
            close()
        }
    }

    val DeleteOutline: ImageVector by lazy {
        materialIconVector("DeleteOutline") {
            moveTo(6.0f, 19.0f)
            curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
            horizontalLineToRelative(8.0f)
            curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
            lineTo(18.0f, 7.0f)
            lineTo(6.0f, 7.0f)
            verticalLineToRelative(12.0f)
            close()
            moveTo(8.0f, 9.0f)
            horizontalLineToRelative(8.0f)
            verticalLineToRelative(10.0f)
            lineTo(8.0f, 19.0f)
            lineTo(8.0f, 9.0f)
            close()
            moveTo(15.5f, 4.0f)
            lineToRelative(-1.0f, -1.0f)
            horizontalLineToRelative(-5.0f)
            lineToRelative(-1.0f, 1.0f)
            lineTo(5.0f, 4.0f)
            verticalLineToRelative(2.0f)
            horizontalLineToRelative(14.0f)
            lineTo(19.0f, 4.0f)
            close()
        }
    }

    val EmojiEvents: ImageVector by lazy {
        materialIconVector("EmojiEvents") {
            moveTo(19.0f, 5.0f)
            horizontalLineToRelative(-2.0f)
            verticalLineTo(3.0f)
            horizontalLineTo(7.0f)
            verticalLineToRelative(2.0f)
            horizontalLineTo(5.0f)
            curveTo(3.9f, 5.0f, 3.0f, 5.9f, 3.0f, 7.0f)
            verticalLineToRelative(1.0f)
            curveToRelative(0.0f, 2.55f, 1.92f, 4.63f, 4.39f, 4.94f)
            curveToRelative(0.63f, 1.5f, 1.98f, 2.63f, 3.61f, 2.96f)
            verticalLineTo(19.0f)
            horizontalLineTo(7.0f)
            verticalLineToRelative(2.0f)
            horizontalLineToRelative(10.0f)
            verticalLineToRelative(-2.0f)
            horizontalLineToRelative(-4.0f)
            verticalLineToRelative(-3.1f)
            curveToRelative(1.63f, -0.33f, 2.98f, -1.46f, 3.61f, -2.96f)
            curveTo(19.08f, 12.63f, 21.0f, 10.55f, 21.0f, 8.0f)
            verticalLineTo(7.0f)
            curveTo(21.0f, 5.9f, 20.1f, 5.0f, 19.0f, 5.0f)
            close()
            moveTo(5.0f, 8.0f)
            verticalLineTo(7.0f)
            horizontalLineToRelative(2.0f)
            verticalLineToRelative(3.82f)
            curveTo(5.84f, 10.4f, 5.0f, 9.3f, 5.0f, 8.0f)
            close()
            moveTo(19.0f, 8.0f)
            curveToRelative(0.0f, 1.3f, -0.84f, 2.4f, -2.0f, 2.82f)
            verticalLineTo(7.0f)
            horizontalLineToRelative(2.0f)
            verticalLineTo(8.0f)
            close()
        }
    }

    val FileDownload: ImageVector by lazy {
        materialIconVector("FileDownload") {
            moveTo(19.0f, 9.0f)
            horizontalLineToRelative(-4.0f)
            verticalLineTo(3.0f)
            horizontalLineTo(9.0f)
            verticalLineToRelative(6.0f)
            horizontalLineTo(5.0f)
            lineToRelative(7.0f, 7.0f)
            lineToRelative(7.0f, -7.0f)
            close()
            moveTo(5.0f, 18.0f)
            verticalLineToRelative(2.0f)
            horizontalLineToRelative(14.0f)
            verticalLineToRelative(-2.0f)
            horizontalLineTo(5.0f)
            close()
        }
    }

    val FolderOpen: ImageVector by lazy {
        materialIconVector("FolderOpen") {
            moveTo(20.0f, 6.0f)
            horizontalLineToRelative(-8.0f)
            lineToRelative(-2.0f, -2.0f)
            lineTo(4.0f, 4.0f)
            curveToRelative(-1.1f, 0.0f, -1.99f, 0.9f, -1.99f, 2.0f)
            lineTo(2.0f, 18.0f)
            curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
            horizontalLineToRelative(16.0f)
            curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
            lineTo(22.0f, 8.0f)
            curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
            close()
            moveTo(20.0f, 18.0f)
            lineTo(4.0f, 18.0f)
            lineTo(4.0f, 8.0f)
            horizontalLineToRelative(16.0f)
            verticalLineToRelative(10.0f)
            close()
        }
    }

    val People: ImageVector by lazy {
        materialIconVector("People") {
            moveTo(16.0f, 11.0f)
            curveToRelative(1.66f, 0.0f, 2.99f, -1.34f, 2.99f, -3.0f)
            reflectiveCurveTo(17.66f, 5.0f, 16.0f, 5.0f)
            curveToRelative(-1.66f, 0.0f, -3.0f, 1.34f, -3.0f, 3.0f)
            reflectiveCurveToRelative(1.34f, 3.0f, 3.0f, 3.0f)
            close()
            moveTo(8.0f, 11.0f)
            curveToRelative(1.66f, 0.0f, 2.99f, -1.34f, 2.99f, -3.0f)
            reflectiveCurveTo(9.66f, 5.0f, 8.0f, 5.0f)
            curveTo(6.34f, 5.0f, 5.0f, 6.34f, 5.0f, 8.0f)
            reflectiveCurveToRelative(1.34f, 3.0f, 3.0f, 3.0f)
            close()
            moveTo(8.0f, 13.0f)
            curveToRelative(-2.33f, 0.0f, -7.0f, 1.17f, -7.0f, 3.5f)
            lineTo(1.0f, 19.0f)
            horizontalLineToRelative(14.0f)
            verticalLineToRelative(-2.5f)
            curveToRelative(0.0f, -2.33f, -4.67f, -3.5f, -7.0f, -3.5f)
            close()
            moveTo(16.0f, 13.0f)
            curveToRelative(-0.29f, 0.0f, -0.62f, 0.02f, -0.97f, 0.05f)
            curveToRelative(1.16f, 0.84f, 1.97f, 1.97f, 1.97f, 3.45f)
            lineTo(17.0f, 19.0f)
            horizontalLineToRelative(6.0f)
            verticalLineToRelative(-2.5f)
            curveToRelative(0.0f, -2.33f, -4.67f, -3.5f, -7.0f, -3.5f)
            close()
        }
    }

    val PersonAdd: ImageVector by lazy {
        materialIconVector("PersonAdd") {
            moveTo(15.0f, 12.0f)
            curveToRelative(2.21f, 0.0f, 4.0f, -1.79f, 4.0f, -4.0f)
            reflectiveCurveToRelative(-1.79f, -4.0f, -4.0f, -4.0f)
            reflectiveCurveToRelative(-4.0f, 1.79f, -4.0f, 4.0f)
            reflectiveCurveToRelative(1.79f, 4.0f, 4.0f, 4.0f)
            close()
            moveTo(6.0f, 10.0f)
            lineTo(6.0f, 7.0f)
            lineTo(4.0f, 7.0f)
            verticalLineToRelative(3.0f)
            lineTo(1.0f, 10.0f)
            verticalLineToRelative(2.0f)
            horizontalLineToRelative(3.0f)
            verticalLineToRelative(3.0f)
            horizontalLineToRelative(2.0f)
            verticalLineToRelative(-3.0f)
            horizontalLineToRelative(3.0f)
            verticalLineToRelative(-2.0f)
            lineTo(6.0f, 10.0f)
            close()
            moveTo(15.0f, 14.0f)
            curveToRelative(-2.67f, 0.0f, -8.0f, 1.34f, -8.0f, 4.0f)
            verticalLineToRelative(2.0f)
            horizontalLineToRelative(16.0f)
            verticalLineToRelative(-2.0f)
            curveToRelative(0.0f, -2.66f, -5.33f, -4.0f, -8.0f, -4.0f)
            close()
        }
    }

    val PersonSearch: ImageVector by lazy {
        ImageVector.Builder("PersonSearch", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(10.0f, 8.0f)
                moveToRelative(-4.0f, 0.0f)
                arcToRelative(4.0f, 4.0f, 0.0f, true, true, 8.0f, 0.0f)
                arcToRelative(4.0f, 4.0f, 0.0f, true, true, -8.0f, 0.0f)
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(10.35f, 14.01f)
                curveTo(7.62f, 13.91f, 2.0f, 15.27f, 2.0f, 18.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(9.54f)
                curveTo(9.07f, 17.24f, 10.31f, 14.11f, 10.35f, 14.01f)
                close()
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(19.43f, 18.02f)
                curveTo(19.79f, 17.43f, 20.0f, 16.74f, 20.0f, 16.0f)
                curveToRelative(0.0f, -2.21f, -1.79f, -4.0f, -4.0f, -4.0f)
                reflectiveCurveToRelative(-4.0f, 1.79f, -4.0f, 4.0f)
                curveToRelative(0.0f, 2.21f, 1.79f, 4.0f, 4.0f, 4.0f)
                curveToRelative(0.74f, 0.0f, 1.43f, -0.22f, 2.02f, -0.57f)
                lineTo(20.59f, 22.0f)
                lineTo(22.0f, 20.59f)
                lineTo(19.43f, 18.02f)
                close()
                moveTo(16.0f, 18.0f)
                curveToRelative(-1.1f, 0.0f, -2.0f, -0.9f, -2.0f, -2.0f)
                curveToRelative(0.0f, -1.1f, 0.9f, -2.0f, 2.0f, -2.0f)
                reflectiveCurveToRelative(2.0f, 0.9f, 2.0f, 2.0f)
                curveTo(18.0f, 17.1f, 17.1f, 18.0f, 16.0f, 18.0f)
                close()
            }
        }.build()
    }

    val RestartAlt: ImageVector by lazy {
        ImageVector.Builder("RestartAlt", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12.0f, 5.0f)
                verticalLineTo(2.0f)
                lineTo(8.0f, 6.0f)
                lineToRelative(4.0f, 4.0f)
                verticalLineTo(7.0f)
                curveToRelative(3.31f, 0.0f, 6.0f, 2.69f, 6.0f, 6.0f)
                curveToRelative(0.0f, 2.97f, -2.17f, 5.43f, -5.0f, 5.91f)
                verticalLineToRelative(2.02f)
                curveToRelative(3.95f, -0.49f, 7.0f, -3.85f, 7.0f, -7.93f)
                curveTo(20.0f, 8.58f, 16.42f, 5.0f, 12.0f, 5.0f)
                close()
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(6.0f, 13.0f)
                curveToRelative(0.0f, -1.65f, 0.67f, -3.15f, 1.76f, -4.24f)
                lineTo(6.34f, 7.34f)
                curveTo(4.9f, 8.79f, 4.0f, 10.79f, 4.0f, 13.0f)
                curveToRelative(0.0f, 4.08f, 3.05f, 7.44f, 7.0f, 7.93f)
                verticalLineToRelative(-2.02f)
                curveTo(8.17f, 18.43f, 6.0f, 15.97f, 6.0f, 13.0f)
                close()
            }
        }.build()
    }

    val SaveAlt: ImageVector by lazy {
        materialIconVector("SaveAlt") {
            moveTo(19.0f, 12.0f)
            verticalLineToRelative(7.0f)
            lineTo(5.0f, 19.0f)
            verticalLineToRelative(-7.0f)
            lineTo(3.0f, 12.0f)
            verticalLineToRelative(7.0f)
            curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
            horizontalLineToRelative(14.0f)
            curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
            verticalLineToRelative(-7.0f)
            horizontalLineToRelative(-2.0f)
            close()
            moveTo(13.0f, 12.67f)
            lineToRelative(2.59f, -2.58f)
            lineTo(17.0f, 11.5f)
            lineToRelative(-5.0f, 5.0f)
            lineToRelative(-5.0f, -5.0f)
            lineToRelative(1.41f, -1.41f)
            lineTo(11.0f, 12.67f)
            lineTo(11.0f, 3.0f)
            horizontalLineToRelative(2.0f)
            close()
        }
    }

    val Shuffle: ImageVector by lazy {
        materialIconVector("Shuffle") {
            moveTo(10.59f, 9.17f)
            lineTo(5.41f, 4.0f)
            lineTo(4.0f, 5.41f)
            lineToRelative(5.17f, 5.17f)
            lineToRelative(1.42f, -1.41f)
            close()
            moveTo(14.5f, 4.0f)
            lineToRelative(2.04f, 2.04f)
            lineTo(4.0f, 18.59f)
            lineTo(5.41f, 20.0f)
            lineTo(17.96f, 7.46f)
            lineTo(20.0f, 9.5f)
            lineTo(20.0f, 4.0f)
            horizontalLineToRelative(-5.5f)
            close()
            moveTo(14.83f, 13.41f)
            lineToRelative(-1.41f, 1.41f)
            lineToRelative(3.13f, 3.13f)
            lineTo(14.5f, 20.0f)
            lineTo(20.0f, 20.0f)
            verticalLineToRelative(-5.5f)
            lineToRelative(-2.04f, 2.04f)
            lineToRelative(-3.13f, -3.13f)
            close()
        }
    }

    val SyncAlt: ImageVector by lazy {
        ImageVector.Builder("SyncAlt", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(18.0f, 12.0f)
                lineToRelative(4.0f, -4.0f)
                lineToRelative(-4.0f, -4.0f)
                lineToRelative(0.0f, 3.0f)
                lineToRelative(-15.0f, 0.0f)
                lineToRelative(0.0f, 2.0f)
                lineToRelative(15.0f, 0.0f)
                close()
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(6.0f, 12.0f)
                lineToRelative(-4.0f, 4.0f)
                lineToRelative(4.0f, 4.0f)
                lineToRelative(0.0f, -3.0f)
                lineToRelative(15.0f, 0.0f)
                lineToRelative(0.0f, -2.0f)
                lineToRelative(-15.0f, 0.0f)
                close()
            }
        }.build()
    }

    val SystemUpdate: ImageVector by lazy {
        materialIconVector("SystemUpdate") {
            moveTo(17.0f, 1.01f)
            lineTo(7.0f, 1.0f)
            curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
            verticalLineToRelative(18.0f)
            curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
            horizontalLineToRelative(10.0f)
            curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
            lineTo(19.0f, 3.0f)
            curveToRelative(0.0f, -1.1f, -0.9f, -1.99f, -2.0f, -1.99f)
            close()
            moveTo(17.0f, 19.0f)
            lineTo(7.0f, 19.0f)
            lineTo(7.0f, 5.0f)
            horizontalLineToRelative(10.0f)
            verticalLineToRelative(14.0f)
            close()
            moveTo(16.0f, 13.0f)
            horizontalLineToRelative(-3.0f)
            lineTo(13.0f, 8.0f)
            horizontalLineToRelative(-2.0f)
            verticalLineToRelative(5.0f)
            lineTo(8.0f, 13.0f)
            lineToRelative(4.0f, 4.0f)
            lineToRelative(4.0f, -4.0f)
            close()
        }
    }

    val Undo: ImageVector by lazy {
        materialIconVector("Undo", autoMirror = true) {
            moveTo(12.5f, 8.0f)
            curveToRelative(-2.65f, 0.0f, -5.05f, 0.99f, -6.9f, 2.6f)
            lineTo(2.0f, 7.0f)
            verticalLineToRelative(9.0f)
            horizontalLineToRelative(9.0f)
            lineToRelative(-3.62f, -3.62f)
            curveToRelative(1.39f, -1.16f, 3.16f, -1.88f, 5.12f, -1.88f)
            curveToRelative(3.54f, 0.0f, 6.55f, 2.31f, 7.6f, 5.5f)
            lineToRelative(2.37f, -0.78f)
            curveTo(21.08f, 11.03f, 17.15f, 8.0f, 12.5f, 8.0f)
            close()
        }
    }

}
