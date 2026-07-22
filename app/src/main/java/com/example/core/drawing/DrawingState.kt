package com.example.core.drawing

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import com.example.data.models.EraserMode
import com.example.data.models.HslaColor
import com.example.data.models.ShapeEntity
import com.example.data.models.ShapeType
import com.example.data.models.StrokeEntity
import com.example.data.models.StrokePoint
import com.example.data.models.ToolType
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class RulerState(
    val isVisible: Boolean = false,
    val center: Offset = Offset(500f, 400f),
    val angleRad: Float = 0f,
    val length: Float = 800f,
    val width: Float = 90f
) {
    fun getEdgeLines(): Pair<Pair<Offset, Offset>, Pair<Offset, Offset>> {
        val dx = cos(angleRad) * length / 2f
        val dy = sin(angleRad) * length / 2f
        val nx = -sin(angleRad) * width / 2f
        val ny = cos(angleRad) * width / 2f

        val topStart = Offset(center.x - dx + nx, center.y - dy + ny)
        val topEnd = Offset(center.x + dx + nx, center.y + dy + ny)
        val bottomStart = Offset(center.x - dx - nx, center.y - dy - ny)
        val bottomEnd = Offset(center.x + dx - nx, center.y + dy - ny)

        return Pair(Pair(topStart, topEnd), Pair(bottomStart, bottomEnd))
    }

    fun snapPointIfClose(point: Offset, thresholdDp: Float = 14f): Offset? {
        if (!isVisible) return null
        val (top, bottom) = getEdgeLines()

        val snapTop = projectPointToSegment(point, top.first, top.second)
        if ((point - snapTop).getDistance() <= thresholdDp) return snapTop

        val snapBottom = projectPointToSegment(point, bottom.first, bottom.second)
        if ((point - snapBottom).getDistance() <= thresholdDp) return snapBottom

        return null
    }

    private fun projectPointToSegment(p: Offset, a: Offset, b: Offset): Offset {
        val ab = b - a
        val abSq = ab.x * ab.x + ab.y * ab.y
        if (abSq == 0f) return a
        val ap = p - a
        val t = ((ap.x * ab.x + ap.y * ab.y) / abSq).coerceIn(0f, 1f)
        return Offset(a.x + t * ab.x, a.y + t * ab.y)
    }
}

object DrawingEngine {

    fun createSmoothPath(points: List<StrokePoint>): Path {
        val path = Path()
        if (points.isEmpty()) return path
        if (points.size == 1) {
            val p = points.first()
            path.addOval(Rect(p.x - 1f, p.y - 1f, p.x + 1f, p.y + 1f))
            return path
        }

        path.moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size - 1) {
            val p0 = points[i]
            val p1 = points[i + 1]
            val midX = (p0.x + p1.x) / 2f
            val midY = (p0.y + p1.y) / 2f
            path.quadraticTo(p0.x, p0.y, midX, midY)
        }
        val last = points.last()
        path.lineTo(last.x, last.y)
        return path
    }

    fun isPointInStroke(point: Offset, stroke: StrokeEntity, radius: Float): Boolean {
        for (p in stroke.points) {
            val distSq = (p.x - point.x) * (p.x - point.x) + (p.y - point.y) * (p.y - point.y)
            if (distSq <= (radius + stroke.baseWidth) * (radius + stroke.baseWidth)) {
                return true
            }
        }
        return false
    }

    fun erasePixelMode(stroke: StrokeEntity, eraserPos: Offset, radius: Float): List<StrokeEntity> {
        val result = mutableListOf<StrokeEntity>()
        var currentChunk = mutableListOf<StrokePoint>()

        for (p in stroke.points) {
            val distSq = (p.x - eraserPos.x) * (p.x - eraserPos.x) + (p.y - eraserPos.y) * (p.y - eraserPos.y)
            if (distSq <= radius * radius) {
                if (currentChunk.isNotEmpty()) {
                    result.add(stroke.copy(id = java.util.UUID.randomUUID().toString(), points = currentChunk))
                    currentChunk = mutableListOf()
                }
            } else {
                currentChunk.add(p)
            }
        }
        if (currentChunk.isNotEmpty()) {
            result.add(stroke.copy(id = java.util.UUID.randomUUID().toString(), points = currentChunk))
        }
        return result
    }

    fun createShapePath(shapeType: ShapeType, rect: Rect): Path {
        val path = Path()
        when (shapeType) {
            ShapeType.CIRCLE -> {
                path.addOval(rect)
            }
            ShapeType.SQUARE -> {
                path.addRect(rect)
            }
            ShapeType.TRIANGLE -> {
                path.moveTo(rect.center.x, rect.top)
                path.lineTo(rect.right, rect.bottom)
                path.lineTo(rect.left, rect.bottom)
                path.close()
            }
            ShapeType.ARROW -> {
                val midY = rect.center.y
                val shaftHeight = rect.height * 0.25f
                val headWidth = rect.width * 0.35f
                path.moveTo(rect.left, midY - shaftHeight / 2)
                path.lineTo(rect.right - headWidth, midY - shaftHeight / 2)
                path.lineTo(rect.right - headWidth, rect.top)
                path.lineTo(rect.right, midY)
                path.lineTo(rect.right - headWidth, rect.bottom)
                path.lineTo(rect.right - headWidth, midY + shaftHeight / 2)
                path.lineTo(rect.left, midY + shaftHeight / 2)
                path.close()
            }
            ShapeType.BOLD_ARROW -> {
                val midY = rect.center.y
                val shaftHeight = rect.height * 0.4f
                val headWidth = rect.width * 0.4f
                path.moveTo(rect.left, midY - shaftHeight / 2)
                path.lineTo(rect.right - headWidth, midY - shaftHeight / 2)
                path.lineTo(rect.right - headWidth, rect.top)
                path.lineTo(rect.right, midY)
                path.lineTo(rect.right - headWidth, rect.bottom)
                path.lineTo(rect.right - headWidth, midY + shaftHeight / 2)
                path.lineTo(rect.left, midY + shaftHeight / 2)
                path.close()
            }
            ShapeType.STAR -> {
                val cx = rect.center.x
                val cy = rect.center.y
                val outerR = minOf(rect.width, rect.height) / 2f
                val innerR = outerR * 0.4f
                val points = 5
                for (i in 0 until points * 2) {
                    val r = if (i % 2 == 0) outerR else innerR
                    val angle = i * Math.PI / points - Math.PI / 2
                    val x = cx + (r * cos(angle)).toFloat()
                    val y = cy + (r * sin(angle)).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
            }
        }
        return path
    }
}
