package org.freemind.mmx.android.ui.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import org.freemind.mmx.layout.LaidOutNode
import org.freemind.mmx.layout.LayoutResult
import kotlin.math.hypot

data class ViewportState(
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
)

@Composable
fun MindMapCanvas(
    layout: LayoutResult,
    selectedNodeId: String?,
    onSelectNode: (String?) -> Unit,
    onToggleFold: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var scale by rememberSaveable { mutableFloatStateOf(1f) }
    var offsetX by rememberSaveable { mutableFloatStateOf(0f) }
    var offsetY by rememberSaveable { mutableFloatStateOf(0f) }
    val viewport = remember(scale, offsetX, offsetY) {
        ViewportState(scale = scale, offsetX = offsetX, offsetY = offsetY)
    }
    val density = LocalDensity.current
    val foldHitRadius = with(density) { 14.dp.toPx() }

    val nodeFill = MaterialTheme.colorScheme.primaryContainer
    val nodeStroke = MaterialTheme.colorScheme.primary
    val selectedFill = MaterialTheme.colorScheme.tertiaryContainer
    val selectedStroke = MaterialTheme.colorScheme.tertiary
    val connectorColor = MaterialTheme.colorScheme.outline
    val textColor = MaterialTheme.colorScheme.onPrimaryContainer
    val foldColor = MaterialTheme.colorScheme.secondary

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(layout, selectedNodeId) {
                detectTapGestures(
                    onTap = { pos ->
                        val world = screenToWorld(pos, viewport, size.width.toFloat(), size.height.toFloat())
                        val foldHit = layout.nodes.firstOrNull { node ->
                            node.hasChildren && foldIndicatorCenter(node).let {
                                hypot(world.x - it.x, world.y - it.y) <= foldHitRadius / viewport.scale
                            }
                        }
                        if (foldHit != null) {
                            onToggleFold(foldHit.id)
                            return@detectTapGestures
                        }
                        val hit = layout.nodes.lastOrNull { it.bounds.contains(world.x, world.y) }
                        onSelectNode(hit?.id)
                    },
                )
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val zoom = event.calculateZoom()
                        val pan = event.calculatePan()
                        val centroid = event.calculateCentroid(useCurrent = true)
                        if (zoom != 1f || pan != Offset.Zero) {
                            val worldBefore = screenToWorld(
                                centroid,
                                ViewportState(scale, offsetX, offsetY),
                                size.width.toFloat(),
                                size.height.toFloat(),
                            )
                            val newScale = (scale * zoom).coerceIn(0.25f, 4f)
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            scale = newScale
                            offsetX = centroid.x - cx - worldBefore.x * newScale + pan.x
                            offsetY = centroid.y - cy - worldBefore.y * newScale + pan.y
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            },
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        withTransform({
            translate(cx + offsetX, cy + offsetY)
            scale(scale, scale, pivot = Offset.Zero)
        }) {
            layout.connectors.forEach { link ->
                val path = Path().apply {
                    moveTo(link.startX, link.startY)
                    val midX = (link.startX + link.endX) / 2f
                    cubicTo(midX, link.startY, midX, link.endY, link.endX, link.endY)
                }
                drawPath(path, color = connectorColor, style = Stroke(width = 2f / scale))
            }

            layout.nodes.forEach { node ->
                val selected = node.id == selectedNodeId
                val fill = if (selected) selectedFill else nodeFill
                val stroke = if (selected) selectedStroke else nodeStroke
                drawRoundRect(
                    color = fill,
                    topLeft = Offset(node.bounds.left, node.bounds.top),
                    size = Size(node.bounds.width, node.bounds.height),
                    cornerRadius = CornerRadius(10f, 10f),
                )
                drawRoundRect(
                    color = stroke,
                    topLeft = Offset(node.bounds.left, node.bounds.top),
                    size = Size(node.bounds.width, node.bounds.height),
                    cornerRadius = CornerRadius(10f, 10f),
                    style = Stroke(width = (if (selected) 3f else 1.5f) / scale),
                )

                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        color = android.graphics.Color.argb(
                            (textColor.alpha * 255).toInt(),
                            (textColor.red * 255).toInt(),
                            (textColor.green * 255).toInt(),
                            (textColor.blue * 255).toInt(),
                        )
                        textSize = 14f
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                    val label = node.text.replace('\n', ' ').let {
                        if (it.length > 28) it.take(27) + "…" else it
                    }
                    drawText(label, node.bounds.centerX, node.bounds.centerY + 5f, paint)
                }

                if (node.hasChildren) {
                    val fold = foldIndicatorCenter(node)
                    drawCircle(color = foldColor, radius = 9f, center = fold)
                    drawCircle(
                        color = Color.White,
                        radius = 9f,
                        center = fold,
                        style = Stroke(width = 1.5f / scale),
                    )
                    val paint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        color = android.graphics.Color.WHITE
                        textSize = 14f
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                    drawContext.canvas.nativeCanvas.drawText(
                        if (node.folded) "+" else "−",
                        fold.x,
                        fold.y + 5f,
                        paint,
                    )
                }
            }
        }
    }
}

private fun foldIndicatorCenter(node: LaidOutNode): Offset {
    val onRight = node.bounds.centerX >= 0f
    val x = if (onRight) node.bounds.right + 12f else node.bounds.left - 12f
    return Offset(x, node.bounds.centerY)
}

private fun screenToWorld(
    screen: Offset,
    viewport: ViewportState,
    width: Float,
    height: Float,
): Offset {
    val cx = width / 2f
    val cy = height / 2f
    return Offset(
        x = (screen.x - cx - viewport.offsetX) / viewport.scale,
        y = (screen.y - cy - viewport.offsetY) / viewport.scale,
    )
}
