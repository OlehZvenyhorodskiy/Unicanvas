package com.example.ui.editor

import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.data.models.PageSizePreset
import com.example.data.models.StrokeEntity
import com.example.data.models.StrokePoint
import com.example.data.models.ToolType
import java.io.File

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
    onZoomChanged: (Float) -> Unit,
    onStrokeAdded: (StrokeEntity) -> Unit,
    onEraseAtPoint: (Offset, Float) -> Unit,
    onTwoFingerTap: () -> Unit,
    onMoveShape: (String, Float, Float) -> Unit = { _, _, _ -> },
    onMoveText: (String, Float, Float) -> Unit = { _, _, _ -> },
    onMoveImage: (String, Float, Float) -> Unit = { _, _, _ -> },
    onMoveChart: (String, Float, Float) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var currentScale by remember { mutableStateOf(zoomScale) }

    LaunchedEffect(zoomScale) {
        currentScale = zoomScale
    }

    val activeStrokePoints = remember { mutableStateListOf<StrokePoint>() }
    var selectedElementId by remember { mutableStateOf<String?>(null) }
    var selectedElementType by remember { mutableStateOf<String?>(null) }
    var dragStartOffset by remember { mutableStateOf(Offset.Zero) }
    var elementOriginalPos by remember { mutableStateOf(Offset.Zero) }

    val bgColor = canvasEntity?.backgroundColor?.let { Color(it) } ?: Color.White
    val pattern = canvasEntity?.backgroundPattern ?: BackgroundPattern.BLANK

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .pointerInput(drawWithFingers) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val oldScale = currentScale
                    val newScale = (oldScale * zoom).coerceIn(0.25f, 8.0f)
                    val zoomFactor = newScale / oldScale
                    // Zoom centered directly on the gesture centroid
                    panOffset = centroid - (centroid - panOffset) * zoomFactor + pan
                    if (newScale != oldScale) {
                        currentScale = newScale
                        onZoomChanged(newScale)
                    }
                }
            }
            .pointerInteropFilter { motionEvent ->
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
                if (!isStylus && !drawWithFingers && currentTool != ToolType.ERASER && currentTool != ToolType.SELECTOR) {
                    return@pointerInteropFilter false
                }

                val x = (motionEvent.x - panOffset.x) / currentScale
                val y = (motionEvent.y - panOffset.y) / currentScale
                var rawPoint = Offset(x, y)

                // Ruler snap if active
                if (currentTool != ToolType.SELECTOR) {
                    rulerState.snapPointIfClose(rawPoint)?.let { snapped ->
                        rawPoint = snapped
                    }
                }

                val pressure = motionEvent.pressure
                val tilt = motionEvent.getAxisValue(MotionEvent.AXIS_TILT)

                when (motionEvent.action) {
                    MotionEvent.ACTION_DOWN -> {
                        activeStrokePoints.clear()
                        if (currentTool == ToolType.SELECTOR) {
                            dragStartOffset = rawPoint
                            selectedElementId = null
                            selectedElementType = null

                            pageEntity?.let { page ->
                                // Check shapes
                                page.shapes.reversed().forEach { shape ->
                                    if (selectedElementId == null && rawPoint.x >= shape.x && rawPoint.x <= shape.x + shape.width &&
                                        rawPoint.y >= shape.y && rawPoint.y <= shape.y + shape.height) {
                                        selectedElementId = shape.id
                                        selectedElementType = "SHAPE"
                                        elementOriginalPos = Offset(shape.x, shape.y)
                                    }
                                }
                                // Check images
                                page.images.reversed().forEach { img ->
                                    if (selectedElementId == null && rawPoint.x >= img.x && rawPoint.x <= img.x + img.width &&
                                        rawPoint.y >= img.y && rawPoint.y <= img.y + img.height) {
                                        selectedElementId = img.id
                                        selectedElementType = "IMAGE"
                                        elementOriginalPos = Offset(img.x, img.y)
                                    }
                                }
                                // Check text
                                page.textBlocks.reversed().forEach { text ->
                                    if (selectedElementId == null && rawPoint.x >= text.x && rawPoint.x <= text.x + text.width &&
                                        rawPoint.y >= text.y && rawPoint.y <= text.y + text.height) {
                                        selectedElementId = text.id
                                        selectedElementType = "TEXT"
                                        elementOriginalPos = Offset(text.x, text.y)
                                    }
                                }
                                // Check charts
                                page.charts.reversed().forEach { chart ->
                                    if (selectedElementId == null && rawPoint.x >= chart.x && rawPoint.x <= chart.x + chart.width &&
                                        rawPoint.y >= chart.y && rawPoint.y <= chart.y + chart.height) {
                                        selectedElementId = chart.id
                                        selectedElementType = "CHART"
                                        elementOriginalPos = Offset(chart.x, chart.y)
                                    }
                                }
                            }
                        } else if (currentTool == ToolType.ERASER) {
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
                        if (currentTool == ToolType.SELECTOR) {
                            val id = selectedElementId
                            val type = selectedElementType
                            if (id != null && type != null) {
                                val dx = rawPoint.x - dragStartOffset.x
                                val dy = rawPoint.y - dragStartOffset.y
                                val newX = elementOriginalPos.x + dx
                                val newY = elementOriginalPos.y + dy
                                when (type) {
                                    "SHAPE" -> onMoveShape(id, newX, newY)
                                    "IMAGE" -> onMoveImage(id, newX, newY)
                                    "TEXT" -> onMoveText(id, newX, newY)
                                    "CHART" -> onMoveChart(id, newX, newY)
                                }
                            }
                        } else if (currentTool == ToolType.ERASER) {
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
                        if (currentTool != ToolType.ERASER && currentTool != ToolType.SELECTOR && activeStrokePoints.isNotEmpty()) {
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

            // Page Size boundary outline
            canvasEntity?.let { c ->
                if (c.pageSizePreset != PageSizePreset.UNLIMITED) {
                    val (pageW, pageH) = when (c.pageSizePreset) {
                        PageSizePreset.A4_VERTICAL -> Pair(1240f, 1754f)
                        PageSizePreset.A4_HORIZONTAL -> Pair(1754f, 1240f)
                        PageSizePreset.RATIO_16_9_VERTICAL -> Pair(1080f, 1920f)
                        PageSizePreset.RATIO_16_9_HORIZONTAL -> Pair(1920f, 1080f)
                        PageSizePreset.LETTER_11X85 -> Pair(1200f, 1550f)
                        PageSizePreset.CUSTOM -> Pair(c.customWidth ?: 1200f, c.customHeight ?: 1600f)
                        else -> Pair(1200f, 1600f)
                    }
                    val left = panOffset.x
                    val top = panOffset.y
                    drawRect(
                        color = Color(0x336366F1),
                        topLeft = Offset(left, top),
                        size = androidx.compose.ui.geometry.Size(pageW * currentScale, pageH * currentScale),
                        style = Stroke(width = 2.5f * currentScale)
                    )
                }
            }

            // 1. Draw Background Pattern
            when (pattern) {
                BackgroundPattern.DOTTED -> {
                    val dotSpacing = 36f * currentScale
                    val dotRadius = 4.2f * currentScale.coerceAtLeast(0.6f)
                    var x = panOffset.x % dotSpacing
                    if (x < 0) x += dotSpacing
                    while (x < canvasWidth) {
                        var y = panOffset.y % dotSpacing
                        if (y < 0) y += dotSpacing
                        while (y < canvasHeight) {
                            drawCircle(
                                color = Color(0x441E293B),
                                radius = dotRadius,
                                center = Offset(x, y)
                            )
                            y += dotSpacing
                        }
                        x += dotSpacing
                    }
                }
                BackgroundPattern.LINED -> {
                    val lineSpacing = 36f * currentScale
                    var y = panOffset.y % lineSpacing
                    if (y < 0) y += lineSpacing
                    while (y < canvasHeight) {
                        drawLine(
                            color = Color(0x331E293B),
                            start = Offset(0f, y),
                            end = Offset(canvasWidth, y),
                            strokeWidth = 1.5f * currentScale.coerceAtLeast(0.8f)
                        )
                        y += lineSpacing
                    }
                }
                BackgroundPattern.BLANK -> {}
            }

            // 2. Render Page Elements (Images, Shapes, Charts, Text, Strokes)
            pageEntity?.let { page ->
                // Render Images
                page.images.forEach { image ->
                    drawIntoCanvas { canvas ->
                        try {
                            val file = File(image.sourceUri)
                            if (file.exists()) {
                                val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                                if (bitmap != null) {
                                    val dstRect = android.graphics.RectF(
                                        image.x * currentScale + panOffset.x,
                                        image.y * currentScale + panOffset.y,
                                        (image.x + image.width) * currentScale + panOffset.x,
                                        (image.y + image.height) * currentScale + panOffset.y
                                    )
                                    canvas.nativeCanvas.drawBitmap(bitmap, null, dstRect, null)
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

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
