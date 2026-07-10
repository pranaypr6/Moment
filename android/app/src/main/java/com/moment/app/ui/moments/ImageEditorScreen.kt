package com.moment.app.ui.moments

import android.graphics.*
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.exifinterface.media.ExifInterface
import coil.compose.AsyncImage
import com.moment.app.ui.theme.*
import java.io.File
import java.io.FileOutputStream
import java.util.*

enum class EditMode { NONE, DRAW, TEXT }

data class DrawPath(
    val path: android.graphics.Path,
    val color: Color,
    val strokeWidth: Float
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageEditorScreen(
    imageUri: Uri,
    onFinishEditing: (Uri) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    var editMode by remember { mutableStateOf(EditMode.NONE) }
    var selectedColor by remember { mutableStateOf(HeartRed) }
    
    val paths = remember { mutableStateListOf<DrawPath>() }
    var currentPath by remember { mutableStateOf<android.graphics.Path?>(null) }
    
    var textOverlay by remember { mutableStateOf("") }
    var textPosition by remember { mutableStateOf(Offset(0.5f, 0.5f)) } // Normalized 0-1 coordinates
    var showTextInput by remember { mutableStateOf(false) }

    val originalBitmap = remember(imageUri) {
        try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            
            // Fix orientation
            val exifInputStream = context.contentResolver.openInputStream(imageUri)
            val exif = exifInputStream?.let { ExifInterface(it) }
            exifInputStream?.close()
            
            val orientation = exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            
            if (bitmap != null && orientation != null) {
                rotateBitmap(bitmap, orientation)
            } else {
                bitmap
            }
        } catch (e: Exception) {
            null
        }
    }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = { Text("Make It Special", color = White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel", tint = White)
                    }
                },
                actions = {
                    TextButton(onClick = {
                        if (originalBitmap != null) {
                            val editedUri = saveEditedBitmap(
                                context, 
                                originalBitmap, 
                                paths, 
                                textOverlay, 
                                textPosition,
                                selectedColor
                            )
                            onFinishEditing(editedUri)
                        }
                    }) {
                        Text("Continue", color = HeartRed, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        },
        bottomBar = {
            EditorBottomBar(
                currentMode = editMode,
                onModeChange = { mode -> 
                    editMode = if (editMode == mode) EditMode.NONE else mode
                    if (mode == EditMode.TEXT) showTextInput = true
                },
                onUndo = { if (paths.isNotEmpty()) paths.removeAt(paths.size - 1) },
                selectedColor = selectedColor,
                onColorSelect = { selectedColor = it }
            )
        }
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val screenWidth = constraints.maxWidth.toFloat()
            val screenHeight = constraints.maxHeight.toFloat()

            if (originalBitmap != null) {
                // Calculate Fit coordinates
                val bitmapWidth = originalBitmap.width.toFloat()
                val bitmapHeight = originalBitmap.height.toFloat()
                
                val scale = minOf(screenWidth / bitmapWidth, screenHeight / bitmapHeight)
                val drawWidth = bitmapWidth * scale
                val drawHeight = bitmapHeight * scale
                val offsetX = (screenWidth - drawWidth) / 2
                val offsetY = (screenHeight - drawHeight) / 2

                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = originalBitmap,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                    
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(editMode, offsetX, offsetY, scale) {
                                if (editMode == EditMode.DRAW) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            val normX = (offset.x - offsetX) / drawWidth
                                            val normY = (offset.y - offsetY) / drawHeight
                                            
                                            val path = android.graphics.Path().apply { 
                                                moveTo(normX * 1000f, normY * 1000f) 
                                            }
                                            currentPath = path
                                            paths.add(DrawPath(path, selectedColor, 10f))
                                        },
                                        onDrag = { change, _ ->
                                            val normX = (change.position.x - offsetX) / drawWidth
                                            val normY = (change.position.y - offsetY) / drawHeight
                                            
                                            currentPath?.lineTo(normX * 1000f, normY * 1000f)
                                            val last = paths.removeAt(paths.size - 1)
                                            paths.add(last)
                                        },
                                        onDragEnd = { currentPath = null }
                                    )
                                }
                            }
                    ) {
                        paths.forEach { drawPath ->
                            val matrix = Matrix().apply {
                                postScale(drawWidth / 1000f, drawHeight / 1000f)
                                postTranslate(offsetX, offsetY)
                            }
                            val scaledPath = android.graphics.Path(drawPath.path).apply {
                                transform(matrix)
                            }

                            drawContext.canvas.nativeCanvas.drawPath(
                                scaledPath,
                                Paint().apply {
                                    color = drawPath.color.toArgb()
                                    style = Paint.Style.STROKE
                                    strokeWidth = drawPath.strokeWidth * (drawWidth / 1000f)
                                    strokeCap = Paint.Cap.ROUND
                                    strokeJoin = Paint.Join.ROUND
                                    isAntiAlias = true
                                }
                            )
                        }
                    }

                    if (textOverlay.isNotBlank()) {
                        val textX = offsetX + (textPosition.x * drawWidth)
                        val textY = offsetY + (textPosition.y * drawHeight)

                        Box(
                            modifier = Modifier
                                .offset(
                                    x = (textX / density.density).dp,
                                    y = (textY / density.density).dp
                                )
                                .pointerInput(editMode, offsetX, offsetY, scale) {
                                    if (editMode == EditMode.TEXT) {
                                        detectDragGestures { _, dragAmount ->
                                            val newX = (textPosition.x * drawWidth + dragAmount.x) / drawWidth
                                            val newY = (textPosition.y * drawHeight + dragAmount.y) / drawHeight
                                            textPosition = Offset(
                                                newX.coerceIn(0f, 1f),
                                                newY.coerceIn(0f, 1f)
                                            )
                                        }
                                    }
                                }
                                .clickable { if (editMode == EditMode.TEXT) showTextInput = true }
                        ) {
                            Text(
                                text = textOverlay,
                                color = selectedColor,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            )
                        }
                    }
                }
            }

            if (showTextInput) {
                AlertDialog(
                    onDismissRequest = { showTextInput = false },
                    title = { Text("Add a little note...") },
                    text = {
                        OutlinedTextField(
                            value = textOverlay,
                            onValueChange = { textOverlay = it },
                            placeholder = { Text("Whisper sweet nothings...") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        Button(onClick = { showTextInput = false }) { Text("Done") }
                    },
                    containerColor = White,
                    shape = RoundedCornerShape(24.dp)
                )
            }
        }
    }
}

@Composable
fun EditorBottomBar(
    currentMode: EditMode,
    onModeChange: (EditMode) -> Unit,
    onUndo: () -> Unit,
    selectedColor: Color,
    onColorSelect: (Color) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.8f))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf(HeartRed, RoseQuartz, Color.White, Color.Yellow, Color.Cyan).forEach { color ->
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (selectedColor == color) 2.dp else 0.dp,
                            color = White,
                            shape = CircleShape
                        )
                        .clickable { onColorSelect(color) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onModeChange(EditMode.DRAW) }) {
                Icon(
                    Icons.Default.Brush,
                    contentDescription = "Draw",
                    tint = if (currentMode == EditMode.DRAW) HeartRed else White
                )
            }
            
            IconButton(onClick = { onModeChange(EditMode.TEXT) }) {
                Icon(
                    Icons.Default.TextFields,
                    contentDescription = "Text",
                    tint = if (currentMode == EditMode.TEXT) HeartRed else White
                )
            }

            IconButton(onClick = onUndo) {
                Icon(Icons.Default.Undo, contentDescription = "Undo", tint = White)
            }
        }
    }
}

private fun rotateBitmap(bitmap: Bitmap, orientation: Int): Bitmap {
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        else -> return bitmap
    }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

private fun saveEditedBitmap(
    context: android.content.Context,
    original: Bitmap,
    paths: List<DrawPath>,
    text: String,
    textPos: Offset,
    textColor: Color
): Uri {
    val resultBitmap = original.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(resultBitmap)
    
    val bitmapWidth = resultBitmap.width.toFloat()
    val bitmapHeight = resultBitmap.height.toFloat()

    val paint = Paint().apply {
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    paths.forEach { drawPath ->
        val matrix = Matrix().apply {
            postScale(bitmapWidth / 1000f, bitmapHeight / 1000f)
        }
        val finalPath = android.graphics.Path(drawPath.path).apply {
            transform(matrix)
        }

        paint.color = drawPath.color.toArgb()
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = drawPath.strokeWidth * (bitmapWidth / 1000f)
        canvas.drawPath(finalPath, paint)
    }

    if (text.isNotBlank()) {
        paint.style = Paint.Style.FILL
        paint.color = textColor.toArgb()
        paint.textSize = 64f * (bitmapWidth / 1080f)
        
        canvas.drawText(
            text, 
            textPos.x * bitmapWidth, 
            textPos.y * bitmapHeight, 
            paint
        )
    }

    val file = File(context.cacheDir, "EDITED_${UUID.randomUUID()}.jpg")
    val out = FileOutputStream(file)
    resultBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
    out.flush()
    out.close()
    
    return Uri.fromFile(file)
}
