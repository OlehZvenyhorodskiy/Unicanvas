package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.core.drawing.RulerState
import kotlin.math.roundToInt

@Composable
fun RulerOverlayComponent(
    rulerState: RulerState,
    onRulerChange: (RulerState) -> Unit,
    onCloseClick: () -> Unit
) {
    if (!rulerState.isVisible) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(rulerState) {
                detectTransformGestures { centroid, pan, zoom, rotation ->
                    val distToCenter = (centroid - rulerState.center).getDistance()
                    if (distToCenter <= rulerState.length / 2f + 60f) {
                        val newCenter = rulerState.center + pan
                        val newAngle = rulerState.angleRad + (rotation * Math.PI / 180f).toFloat()
                        onRulerChange(rulerState.copy(center = newCenter, angleRad = newAngle))
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val angleDeg = (rulerState.angleRad * 180f / Math.PI).toFloat()

            translate(left = rulerState.center.x, top = rulerState.center.y) {
                rotate(degrees = angleDeg, pivot = Offset.Zero) {
                    val halfW = rulerState.length / 2f
                    val halfH = rulerState.width / 2f

                    // Draw semi-transparent ruler body
                    drawRect(
                        color = Color(0x991E293B),
                        topLeft = Offset(-halfW, -halfH),
                        size = androidx.compose.ui.geometry.Size(rulerState.length, rulerState.width)
                    )

                    // Draw ruler ticks
                    val step = 15f
                    var x = -halfW
                    var cmCount = 0
                    while (x <= halfW) {
                        val isMajor = cmCount % 5 == 0
                        val tickLen = if (isMajor) 22f else 10f

                        // Top ticks
                        drawLine(
                            color = Color.White,
                            start = Offset(x, -halfH),
                            end = Offset(x, -halfH + tickLen),
                            strokeWidth = if (isMajor) 2.5f else 1.2f
                        )

                        // Bottom ticks
                        drawLine(
                            color = Color.White,
                            start = Offset(x, halfH),
                            end = Offset(x, halfH - tickLen),
                            strokeWidth = if (isMajor) 2.5f else 1.2f
                        )

                        x += step
                        cmCount++
                    }
                }
            }
        }

        // Close button on top of ruler center
        Surface(
            shape = CircleShape,
            color = Color.Red,
            modifier = Modifier
                .offset {
                    IntOffset(
                        (rulerState.center.x - 18.dp.toPx()).roundToInt(),
                        (rulerState.center.y - 18.dp.toPx()).roundToInt()
                    )
                }
                .size(36.dp)
        ) {
            IconButton(onClick = onCloseClick) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Закрити лінійку",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
