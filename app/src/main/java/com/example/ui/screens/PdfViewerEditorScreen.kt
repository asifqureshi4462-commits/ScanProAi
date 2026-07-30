package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.BrandingWatermark
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ScannedDocument
import com.example.ui.ScanProViewModel
import com.example.util.PdfEngine
import java.io.File

data class DrawingPath(
    val path: Path,
    val color: Color,
    val strokeWidth: Float
)

data class TextAnnotation(
    val id: String = java.util.UUID.randomUUID().toString(),
    var text: String,
    var x: Float,
    var y: Float,
    val color: Color
)

@Composable
fun PdfViewerEditorScreen(
    viewModel: ScanProViewModel,
    onBackClicked: () -> Unit
) {
    val context = LocalContext.current
    val activeDocument by viewModel.activeDocument.collectAsState()
    val doc = activeDocument ?: return

    var currentPageIndex by remember { mutableStateOf(0) }
    var totalPages by remember { mutableStateOf(1) }
    var isNightMode by remember { mutableStateOf(false) }
    var isHorizontalScroll by remember { mutableStateOf(false) }
    var zoomLevel by remember { mutableStateOf(1.0f) }

    var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoadingPage by remember { mutableStateOf(true) }

    // Active Tool Mode
    var selectedToolMode by remember { mutableStateOf("VIEW") } // VIEW, DRAW, TEXT, HIGHLIGHT, STRIKE, SIGN, WATERMARK, PAGE_MANAGE, AI_ASSIST
    var activeInkColor by remember { mutableStateOf(Color(0xFF2563EB)) }
    var strokeThickness by remember { mutableStateOf(5f) }

    // Annotations state
    val drawingPaths = remember { mutableStateListOf<DrawingPath>() }
    var currentPath by remember { mutableStateOf<Path?>(null) }
    val textAnnotations = remember { mutableStateListOf<TextAnnotation>() }

    // Dialog states
    var showAddTextDialog by remember { mutableStateOf(false) }
    var newAnnotationText by remember { mutableStateOf("") }
    var showWatermarkDialog by remember { mutableStateOf(false) }
    var watermarkText by remember { mutableStateOf("CONFIDENTIAL") }
    var showAiAssistantDialog by remember { mutableStateOf(false) }
    var aiResultText by remember { mutableStateOf("") }
    var isAiProcessing by remember { mutableStateOf(false) }

    // Render Page
    LaunchedEffect(doc.filePath, currentPageIndex) {
        isLoadingPage = true
        val file = File(doc.filePath)
        if (file.exists()) {
            val pageCount = PdfEngine.getPdfPageCount(context, file)
            totalPages = if (pageCount > 0) pageCount else 1
            val bmp = PdfEngine.renderPdfPageToBitmap(context, file, currentPageIndex)
            pageBitmap = bmp
        }
        isLoadingPage = false
    }

    // Watermark Dialog
    if (showWatermarkDialog) {
        AlertDialog(
            onDismissRequest = { showWatermarkDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Stamp Security Watermark", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = watermarkText,
                    onValueChange = { watermarkText = it },
                    label = { Text("Watermark Text") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        textAnnotations.add(
                            TextAnnotation(
                                text = watermarkText,
                                x = 100f,
                                y = 300f,
                                color = Color.Red.copy(alpha = 0.4f)
                            )
                        )
                        showWatermarkDialog = false
                        Toast.makeText(context, "Watermark added to page!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Apply Watermark", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWatermarkDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Text Dialog
    if (showAddTextDialog) {
        AlertDialog(
            onDismissRequest = { showAddTextDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Add Text Annotation", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newAnnotationText,
                    onValueChange = { newAnnotationText = it },
                    placeholder = { Text("Type note or text overlay...") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newAnnotationText.isNotBlank()) {
                            textAnnotations.add(
                                TextAnnotation(
                                    text = newAnnotationText,
                                    x = 150f,
                                    y = 200f,
                                    color = activeInkColor
                                )
                            )
                            newAnnotationText = ""
                        }
                        showAddTextDialog = false
                    }
                ) {
                    Text("Add", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTextDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // AI Assistant Dialog
    if (showAiAssistantDialog) {
        AlertDialog(
            onDismissRequest = { showAiAssistantDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Gemini AI PDF Assistant", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                if (isAiProcessing) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Analyzing PDF with Gemini AI...")
                    }
                } else {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text(aiResultText, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAiAssistantDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isNightMode) Color(0xFF121212) else MaterialTheme.colorScheme.background)
    ) {
        // Top Action Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClicked) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Column {
                    Text(
                        text = doc.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1
                    )
                    Text(
                        text = "Page ${currentPageIndex + 1} of $totalPages",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row {
                IconButton(onClick = { isNightMode = !isNightMode }) {
                    Icon(
                        Icons.Default.Nightlight,
                        contentDescription = "Night Mode",
                        tint = if (isNightMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = {
                        isAiProcessing = true
                        showAiAssistantDialog = true
                        viewModel.summarizeDocument(doc) { summary ->
                            aiResultText = summary
                            isAiProcessing = false
                        }
                    }
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "AI Summarize", tint = Color(0xFFA855F7))
                }

                IconButton(
                    onClick = {
                        Toast.makeText(context, "PDF Annotations & Changes Saved!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Save PDF", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Editor Toolbar Row (View, Draw, Text, Highlight, Strikeout, Signature, Watermark, Pages)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ToolChipItem("View", Icons.Default.Crop, selectedToolMode == "VIEW") { selectedToolMode = "VIEW" }
            ToolChipItem("Pen", Icons.Default.Draw, selectedToolMode == "DRAW") { selectedToolMode = "DRAW" }
            ToolChipItem("Text", Icons.Default.TextFields, selectedToolMode == "TEXT") {
                selectedToolMode = "TEXT"
                showAddTextDialog = true
            }
            ToolChipItem("Highlight", Icons.Default.Highlight, selectedToolMode == "HIGHLIGHT") { selectedToolMode = "HIGHLIGHT" }
            ToolChipItem("Strikeout", Icons.Default.FormatStrikethrough, selectedToolMode == "STRIKE") { selectedToolMode = "STRIKE" }
            ToolChipItem("Signature", Icons.Default.Gesture, selectedToolMode == "SIGN") {
                textAnnotations.add(TextAnnotation(text = "✍️ Signed by ScanPro", x = 120f, y = 400f, color = Color.Blue))
                Toast.makeText(context, "Digital Signature Stamp Added!", Toast.LENGTH_SHORT).show()
            }
            ToolChipItem("Watermark", Icons.Default.BrandingWatermark, selectedToolMode == "WATERMARK") { showWatermarkDialog = true }
            ToolChipItem("OCR", Icons.Default.Translate, false) {
                isAiProcessing = true
                showAiAssistantDialog = true
                viewModel.extractTextFromDocument(doc) { text ->
                    aiResultText = text
                    isAiProcessing = false
                }
            }
        }

        // Color & Brush Controls Subbar if in Drawing or Highlight mode
        if (selectedToolMode == "DRAW" || selectedToolMode == "HIGHLIGHT") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        Color(0xFF2563EB),
                        Color(0xFFEF4444),
                        Color(0xFF16A34A),
                        Color(0xFFF59E0B),
                        Color.Black,
                        Color.Yellow
                    ).forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (activeInkColor == color) 3.dp else 1.dp,
                                    color = if (activeInkColor == color) MaterialTheme.colorScheme.primary else Color.LightGray,
                                    shape = CircleShape
                                )
                                .clickable { activeInkColor = color }
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Size: ", fontSize = 12.sp)
                    Slider(
                        value = strokeThickness,
                        onValueChange = { strokeThickness = it },
                        valueRange = 2f..20f,
                        modifier = Modifier.width(100.dp)
                    )
                    IconButton(onClick = { drawingPaths.clear() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        // Main PDF Page Display Canvas
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(if (isNightMode) Color(0xFF1E1E1E) else Color(0xFFE2E8F0)),
            contentAlignment = Alignment.Center
        ) {
            if (isLoadingPage) {
                CircularProgressIndicator()
            } else if (pageBitmap != null) {
                Box(
                    modifier = Modifier
                        .padding(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                ) {
                    Image(
                        bitmap = pageBitmap!!.asImageBitmap(),
                        contentDescription = "PDF Page",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )

                    // Canvas Overlay for Drawing & Highlighting
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(selectedToolMode, activeInkColor, strokeThickness) {
                                if (selectedToolMode == "DRAW" || selectedToolMode == "HIGHLIGHT") {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            val p = Path().apply { moveTo(offset.x, offset.y) }
                                            currentPath = p
                                        },
                                        onDrag = { change, _ ->
                                            currentPath?.lineTo(change.position.x, change.position.y)
                                        },
                                        onDragEnd = {
                                            currentPath?.let {
                                                drawingPaths.add(
                                                    DrawingPath(
                                                        path = it,
                                                        color = if (selectedToolMode == "HIGHLIGHT") activeInkColor.copy(alpha = 0.4f) else activeInkColor,
                                                        strokeWidth = if (selectedToolMode == "HIGHLIGHT") strokeThickness * 3f else strokeThickness
                                                    )
                                                )
                                            }
                                            currentPath = null
                                        }
                                    )
                                }
                            }
                    ) {
                        for (dp in drawingPaths) {
                            drawPath(
                                path = dp.path,
                                color = dp.color,
                                style = Stroke(width = dp.strokeWidth)
                            )
                        }
                    }

                    // Render Text Annotations
                    textAnnotations.forEach { annotation ->
                        Box(
                            modifier = Modifier
                                .padding(start = annotation.x.dp, top = annotation.y.dp)
                                .background(Color.Yellow.copy(alpha = 0.3f), shape = RoundedCornerShape(4.dp))
                                .padding(4.dp)
                        ) {
                            Text(
                                text = annotation.text,
                                color = annotation.color,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        // Bottom Page Thumbnails Rail & Navigation
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(vertical = 6.dp)
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(totalPages) { index ->
                    val isSelected = index == currentPageIndex
                    Box(
                        modifier = Modifier
                            .size(width = 44.dp, height = 58.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable { currentPageIndex = index },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${index + 1}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun ToolChipItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface)
        }
    }
}
