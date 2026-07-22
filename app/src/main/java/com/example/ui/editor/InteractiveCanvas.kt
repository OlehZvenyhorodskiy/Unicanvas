package com.example.ui.editor

import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import com.example.core.drawing.DrawingEngine
import com.example.core.drawing.RulerState
import com.example.data.models.BackgroundPattern
import com.example.data.models.CanvasEntity
import com.example.data.models.HslaColor
import com.example.data.models.PageEntity
import com.example.data.models.StrokeEntity
import com.example.data.models.StrokePoint
import com.example.data.models.ToolType
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun InteractiveCanvas(
    canvasEntity: CanvasEntity?,
    pageEntity: PageEntity?,
    currentTool: ToolType,
    strokeWidth: Float,
    strokeOpacity: Float,
    currentColor: HslaColor,
    drawWithFingers: Boolean,
    rulerState: RulerState,
    zoomScale: Float,
    onStrokeAdded: (StrokeEntity) -> Unit,
    onEraseAtPoint: (Offset, Float) -> Unit,
    onTwoFingerTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var currentScale by remember { mutableStateOf(zoomScale) }

    val activeStrokePoints = remember { mutableStateListOf<StrokePoint>() }
    var pointerCount by remember { mutableStateOf(0) }

    val bgColor = canvasEntity?.backgroundColor?.let { Color(it) } ?: Color.White
    val pattern = canvasEntity?.backgroundPattern ?: BackgroundPattern.BLANK

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .pointerInput(drawWithFingers) {
                detectTransformGestures { centroid, pan, zoom, rotation ->
                    panOffset += pan
                    currentScale = (currentScale * zoom).coerceIn(0.25f, 8.0f)
                }
            }
            .pointerInteropFilter { motionEvent ->
                pointerCount = motionEvent.pointerCount

                // Two-finger tap detection for Undo gesture
                if (motionEvent.pointerCount == 2 && motionEvent.actionMasked == MotionEvent.ACTION_POINTER_DOWN) {
                    onTwoFingerTap()
                    activeStrokePoints.clear()
                    return@pointerInteropFilter true
                }

                if (motionEvent.pointerCount > 1) {
                    activeStrokePoints.clear()
                    return@pointerInteropFilter false
                }

                val toolTypeIndex = motionEvent.getToolType(0)
                val isStylus = toolTypeIndex == MotionEvent.TOOL_TYPE_STYLUS || toolTypeIndex == MotionEvent.TOOL_TYPE_ERASER
                if (!isStylus && !drawWithFingers && currentTool != ToolType.ERASER) {
                    return@pointerInteropFilter false
                }

                val x = (motionEvent.x - panOffset.x) / currentScale
                val y = (motionEvent.y - panOffset.y) / currentScale
                var rawPoint = Offset(x, y)

                // Ruler snap if active
                rulerState.snapPointIfClose(rawPoint)?.let { snapped ->
                    rawPoint = snapped
                }

                val pressure = motionEvent.pressure
                val tilt = motionEvent.getAxisValue(MotionEvent.AXIS_TILT)

                when (motionEvent.action) {
                    MotionEvent.ACTION_DOWN -> {
                        activeStrokePoints.clear()
                        if (currentTool == ToolType.ERASER) {
                            onEraseAtPoint(rawPoint, strokeWidth * 2f)
                        } else {
                            activeStrokePoints.add(
                                StrokePoint(
                                    x = rawPoint.x,
                                    y = rawPoint.y,
                                    pressure = pressure,
                                    tilt = tilt,
                                    timestampMs = System.currentTimeMillis()
                                )
                            )
                        }
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (currentTool == ToolType.ERASER) {
                            onEraseAtPoint(rawPoint, strokeWidth * 2f)
                        } else {
                            activeStrokePoints.add(
                                StrokePoint(
                                    x = rawPoint.x,
                                    y = rawPoint.y,
                                    pressure = pressure,
                                    tilt = tilt,
                                    timestampMs = System.currentTimeMillis()
                                )
                            )
                        }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        if (currentTool != ToolType.ERASER && activeStrokePoints.isNotEmpty()) {
                            val newStroke = StrokeEntity(
                                tool = currentTool,
                                colorHsla = currentColor.copy(alpha = strokeOpacity),
                                baseWidth = strokeWidth,
                                points = activeStrokePoints.toList()
                            )
                            onStrokeAdded(newStroke)
                        }
                        activeStrokePoints.clear()
                    }
                }
                true
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // 1. Draw Background Pattern (under content, fixed coordinates)
            when (pattern) {
                BackgroundPattern.DOTTED -> {
                    val dotSpacing = 28f * currentScale
                    val dotRadius = 2.2f * currentScale
                    var x = panOffset.x % dotSpacing
                    while (x < canvasWidth) {
                        var y = panOffset.y % dotSpacing
                        while (y < canvasHeight) {
                            drawCircle(
                                color = Color(0x33000000),
                                radius = dotRadius,
                                center = Offset(x, y)
                            )
                            y += dotSpacing
                        }
                        x += dotSpacing
                    }
                }
                BackgroundPattern.LINED -> {
                    val lineSpacing = 32f * currentScale
                    var y = panOffset.y % lineSpacing
                    while (y < canvasHeight) {
                        drawLine(
                            color = Color(0x22000000),
                            start = Offset(0f, y),
                            end = Offset(canvasWidth, y),
                            strokeWidth = 1.2f
                        )
                        y += lineSpacing
                    }
                }
                BackgroundPattern.BLANK -> {}
            }

            // 2. Render Page Elements (Shapes, Charts, Text, Strokes)
            pageEntity?.let { page ->
                // Render Shapes
                page.shapes.forEach { shape ->
                    val path = DrawingEngine.createShapePath(
                        shape.shapeType,
                        Rect(
                            shape.x * currentScale + panOffset.x,
                            shape.y * currentScale + panOffset.y,
                            (shape.x + shape.width) * currentScale + panOffset.x,
                            (shape.y + shape.height) * currentScale + panOffset.y
                        )
                    )
                    drawPath(path, Color(shape.fillColor))
                    drawPath(path, Color(shape.strokeColor), style = Stroke(shape.strokeWidth * currentScale))
                }

                // Render Charts
                page.charts.forEach { chart ->
                    val cx = chart.x * currentScale + panOffset.x
                    val cy = chart.y * currentScale + panOffset.y
                    val cw = chart.width * currentScale
                    val ch = chart.height * currentScale

                    drawRect(
                        color = Color(0xFFF8FAFC),
                        topLeft = Offset(cx, cy),
                        size = androidx.compose.ui.geometry.Size(cw, ch)
                    )
                    drawRect(
                        color = Color(0xFF64748B),
                        topLeft = Offset(cx, cy),
                        size = androidx.compose.ui.geometry.Size(cw, ch),
                        style = Stroke(1.5f * currentScale)
                    )

                    // Axes
                    drawLine(
                        color = Color.Black,
                        start = Offset(cx + 20f, cy + ch - 20f),
                        end = Offset(cx + cw - 10f, cy + ch - 20f),
                        strokeWidth = 2.5f
                    )
                    drawLine(
                        color = Color.Black,
                        start = Offset(cx + 20f, cy + ch - 20f),
                        end = Offset(cx + 20f, cy + 10f),
                        strokeWidth = 2.5f
                    )
                }

                // Render Text Blocks
                page.textBlocks.forEach { textBlock ->
                    drawIntoCanvas { canvas ->
                        val paint = android.graphics.Paint().apply {
                            color = textBlock.color
                            textSize = textBlock.fontSize * currentScale * 1.5f
                            isAntiAlias = true
                            isFakeBoldText = textBlock.isBold
                        }
                        canvas.nativeCanvas.drawText(
                            textBlock.text,
                            textBlock.x * currentScale + panOffset.x,
                            (textBlock.y + textBlock.fontSize) * currentScale + panOffset.y,
                            paint
                        )
                    }
                }

                // Render Saved Strokes
                page.strokes.forEach { stroke ->
                    val scaledPoints = stroke.points.map { p ->
                        StrokePoint(
                            x = p.x * currentScale + panOffset.x,
                            y = p.y * currentScale + panOffset.y,
                            pressure = p.pressure,
                            tilt = p.tilt
                        )
                    }
                    val path = DrawingEngine.createSmoothPath(scaledPoints)

                    val strokeWidth = when (stroke.tool) {
                        ToolType.PENCIL -> stroke.baseWidth * currentScale * (1f + (stroke.points.firstOrNull()?.tilt ?: 0f))
                        ToolType.INK_PEN -> stroke.baseWidth * currentScale * 1.2f
                        else -> stroke.baseWidth * currentScale
                    }

                    drawPath(
                        path = path,
                        color = stroke.colorHsla.toColor(),
                        style = Stroke(
                            width = strokeWidth,
                            cap = androidx.compose.ui.graphics.StrokeCap.Round,
                            join = androidx.compose.ui.graphics.StrokeJoin.Round
                        )
                    )
                }
            }

            // 3. Render Active Drawing Stroke
            if (activeStrokePoints.isNotEmpty()) {
                val scaledPoints = activeStrokePoints.map { p ->
                    StrokePoint(
                        x = p.x * currentScale + panOffset.x,
                        y = p.y * currentScale + panOffset.y,
                        pressure = p.pressure,
                        tilt = p.tilt
                    )
                }
                val activePath = DrawingEngine.createSmoothPath(scaledPoints)
                drawPath(
                    path = activePath,
                    color = currentColor.copy(alpha = strokeOpacity).toColor(),
                    style = Stroke(
                        width = strokeWidth * currentScale,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                        join = androidx.compose.ui.graphics.StrokeJoin.Round
                    )
                )
            }
        }
    }
}
