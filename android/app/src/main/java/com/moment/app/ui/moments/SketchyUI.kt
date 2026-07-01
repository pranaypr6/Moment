package com.moment.app.ui.moments

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun SketchyCloseIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier.padding(8.dp)) {
        val stroke = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        // Draw X with slight offset/curve to make it look hand-drawn
        val p1 = Path().apply {
            moveTo(size.width * 0.1f, size.height * 0.1f)
            quadraticBezierTo(size.width * 0.5f, size.height * 0.4f, size.width * 0.9f, size.height * 0.9f)
        }
        val p2 = Path().apply {
            moveTo(size.width * 0.9f, size.height * 0.1f)
            quadraticBezierTo(size.width * 0.4f, size.height * 0.5f, size.width * 0.1f, size.height * 0.9f)
        }
        drawPath(p1, color, style = stroke)
        drawPath(p2, color, style = stroke)
    }
}

@Composable
fun SketchyFlashIcon(modifier: Modifier = Modifier, isOff: Boolean = true, color: Color) {
    Canvas(modifier = modifier.padding(8.dp)) {
        val stroke = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        // Lightning bolt
        val p1 = Path().apply {
            moveTo(size.width * 0.6f, size.height * 0.1f)
            lineTo(size.width * 0.3f, size.height * 0.55f)
            lineTo(size.width * 0.7f, size.height * 0.55f)
            lineTo(size.width * 0.4f, size.height * 0.9f)
        }
        drawPath(p1, color, style = stroke)
        
        if (isOff) {
            val p2 = Path().apply {
                moveTo(size.width * 0.8f, size.height * 0.2f)
                lineTo(size.width * 0.2f, size.height * 0.8f)
            }
            drawPath(p2, color, style = stroke)
        }
    }
}

@Composable
fun SketchyFlipIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier.padding(8.dp)) {
        val stroke = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        // Two curved arrows
        val p1 = Path().apply {
            moveTo(size.width * 0.2f, size.height * 0.4f)
            quadraticBezierTo(size.width * 0.1f, size.height * 0.1f, size.width * 0.6f, size.height * 0.1f)
            quadraticBezierTo(size.width * 0.8f, size.height * 0.1f, size.width * 0.9f, size.height * 0.3f)
        }
        // Arrow head 1
        val h1 = Path().apply {
            moveTo(size.width * 0.7f, size.height * 0.3f)
            lineTo(size.width * 0.9f, size.height * 0.3f)
            lineTo(size.width * 0.9f, size.height * 0.1f)
        }
        
        val p2 = Path().apply {
            moveTo(size.width * 0.8f, size.height * 0.6f)
            quadraticBezierTo(size.width * 0.9f, size.height * 0.9f, size.width * 0.4f, size.height * 0.9f)
            quadraticBezierTo(size.width * 0.2f, size.height * 0.9f, size.width * 0.1f, size.height * 0.7f)
        }
        // Arrow head 2
        val h2 = Path().apply {
            moveTo(size.width * 0.3f, size.height * 0.7f)
            lineTo(size.width * 0.1f, size.height * 0.7f)
            lineTo(size.width * 0.1f, size.height * 0.9f)
        }
        
        drawPath(p1, color, style = stroke)
        drawPath(h1, color, style = stroke)
        drawPath(p2, color, style = stroke)
        drawPath(h2, color, style = stroke)
    }
}

@Composable
fun SketchyGalleryIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier.padding(8.dp)) {
        val stroke = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        
        // Background photo frame
        val bgPath = Path().apply {
            moveTo(size.width * 0.1f, size.height * 0.2f)
            lineTo(size.width * 0.8f, size.height * 0.1f)
            lineTo(size.width * 0.9f, size.height * 0.8f)
            lineTo(size.width * 0.2f, size.height * 0.9f)
            close()
        }
        
        // Foreground photo frame
        val fgPath = Path().apply {
            moveTo(size.width * 0.2f, size.height * 0.3f)
            lineTo(size.width * 0.9f, size.height * 0.2f)
            lineTo(size.width * 0.95f, size.height * 0.9f)
            lineTo(size.width * 0.3f, size.height * 0.95f)
            close()
        }
        
        // Little mountain / sun scribble inside
        val innerPath = Path().apply {
            moveTo(size.width * 0.3f, size.height * 0.8f)
            lineTo(size.width * 0.5f, size.height * 0.5f)
            lineTo(size.width * 0.65f, size.height * 0.7f)
            lineTo(size.width * 0.85f, size.height * 0.4f)
            lineTo(size.width * 0.9f, size.height * 0.8f)
        }
        
        drawPath(bgPath, color, style = stroke)
        drawPath(fgPath, color, style = stroke)
        drawPath(innerPath, color, style = stroke)
    }
}

@Composable
fun SketchyCaptureButton(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val cx = size.width / 2
        val cy = size.height / 2
        val radius = size.width / 2 * 0.9f
        
        // Outer sketchy circle (drawn with a path that loops twice imperfectly)
        val outerPath = Path().apply {
            moveTo(cx + radius, cy)
            // First loop
            cubicTo(cx + radius, cy + radius * 1.1f, cx - radius * 1.1f, cy + radius, cx - radius, cy)
            cubicTo(cx - radius, cy - radius * 1.1f, cx + radius * 1.1f, cy - radius, cx + radius * 0.9f, cy + radius * 0.1f)
            // Second loop
            cubicTo(cx + radius * 0.9f, cy + radius * 1.2f, cx - radius * 1.2f, cy + radius * 0.9f, cx - radius * 1.05f, cy - radius * 0.1f)
            cubicTo(cx - radius * 1.05f, cy - radius * 1.2f, cx + radius * 1.2f, cy - radius * 1.1f, cx + radius, cy)
        }
        drawPath(outerPath, color, style = stroke)
        
        // Inner scribbles (diagonal lines)
        val innerStroke = Stroke(width = 4f, cap = StrokeCap.Round)
        val numLines = 15
        for (i in 0 until numLines) {
            val progress = i / numLines.toFloat()
            // Map progress to x coordinates from -radius to +radius
            val xOffset = -radius * 0.8f + (radius * 1.6f * progress)
            // Calculate corresponding y limit based on circle equation
            val yLimit = Math.sqrt((radius * radius * 0.64f - xOffset * xOffset).toDouble()).toFloat()
            if (yLimit > 0) {
                // Diagonal lines
                val startX = cx + xOffset - yLimit * 0.3f
                val startY = cy - yLimit
                val endX = cx + xOffset + yLimit * 0.3f
                val endY = cy + yLimit
                // Add some randomness
                val randomOffset = (Random.nextFloat() - 0.5f) * 10f
                drawLine(
                    color = color,
                    start = Offset(startX + randomOffset, startY),
                    end = Offset(endX - randomOffset, endY),
                    strokeWidth = innerStroke.width,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

fun Modifier.sketchyBorder(color: Color, strokeWidth: Float = 8f, cornerRadius: Float = 80f): Modifier = this.drawWithCache {
        val path = Path()
        val w = size.width
        val h = size.height
        val r = cornerRadius
        
        // Draw an imperfect rounded rectangle
        path.moveTo(r, 0f)
        // Top edge wavy
        path.quadraticBezierTo(w * 0.5f, -strokeWidth, w - r, strokeWidth * 0.5f)
        // Top right corner
        path.quadraticBezierTo(w, 0f, w - strokeWidth * 0.2f, r)
        // Right edge wavy
        path.quadraticBezierTo(w + strokeWidth, h * 0.5f, w - strokeWidth * 0.5f, h - r)
        // Bottom right corner
        path.quadraticBezierTo(w, h, w - r, h - strokeWidth * 0.2f)
        // Bottom edge wavy
        path.quadraticBezierTo(w * 0.5f, h + strokeWidth, r, h - strokeWidth * 0.5f)
        // Bottom left corner
        path.quadraticBezierTo(0f, h, strokeWidth * 0.2f, h - r)
        // Left edge wavy
        path.quadraticBezierTo(-strokeWidth, h * 0.5f, strokeWidth * 0.5f, r)
        // Top left corner overlapping
        path.quadraticBezierTo(0f, 0f, r + strokeWidth * 2f, strokeWidth)
        
        onDrawWithContent {
            drawContent()
            drawPath(path, color, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
