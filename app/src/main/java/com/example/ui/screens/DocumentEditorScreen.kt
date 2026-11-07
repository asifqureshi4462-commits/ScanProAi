package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.PointF
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ManageHistory
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ScannedDocument
import com.example.ui.ScanProViewModel
import com.example.util.DocumentFilterMode
import com.example.util.DocumentQuad
import com.example.util.OpenCvDocumentDetector
import com.example.util.PdfEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sqrt

data class FilterOption(
    val id: String,
    val name: String,
    val mode: DocumentFilterMode
)

@Composable
fun DocumentEditorScreen(
    viewModel: ScanProViewModel,
    initialDocTitle: String? = null,
    onBackToCamera: () -> Unit,
    onAddMorePages: () -> Unit,
    onSaveAndOpenDocument: (ScannedDocument) -> Unit,
    onExtractTextClicked: (ScannedDocument) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val capturedBitmaps by viewModel.capturedBitmaps.collectAsState()
    val editablePages = remember { mutableStateListOf<Bitmap>() }

    LaunchedEffect(capturedBitmaps) {
        if (capturedBitmaps.isNotEmpty() && editablePages.isEmpty()) {
            editablePages.addAll(capturedBitmaps)
        }
    }

    if (editablePages.isEmpty()) {
        LaunchedEffect(Unit) {
            onBackToCamera()
        }
        return
    }

    var currentPageIndex by remember { mutableIntStateOf(0) }
    val currentSafeIndex = currentPageIndex.coerceIn(0, (editablePages.size - 1).coerceAtLeast(0))

    val defaultName = remember {
        val dateStr = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(Date())
        initialDocTitle ?: "CamScanner $dateStr"
    }
    var documentTitle by remember { mutableStateOf(defaultName) }
    var isRenamingTitle by remember { mutableStateOf(false) }
    var tempRenameText by remember { mutableStateOf(documentTitle) }

    val filterOptions = remember {
        listOf(
            FilterOption("orig", "Original", DocumentFilterMode.ORIGINAL),
            FilterOption("light", "Lighten", DocumentFilterMode.LIGHTEN),
            FilterOption("enhance", "Enhance", DocumentFilterMode.ENHANCE),
            FilterOption("magic", "✦ Magic Pro", DocumentFilterMode.MAGIC_COLOR),
            FilterOption("no_wm", "No Watermark", DocumentFilterMode.ORIGINAL),
            FilterOption("no_shadow", "No Shadow", DocumentFilterMode.NO_SHADOW),
            FilterOption("no_hw", "No Handwriting", DocumentFilterMode.BLACK_AND_WHITE),
            FilterOption("bw", "B&W", DocumentFilterMode.BLACK_AND_WHITE),
            FilterOption("eco", "Eco", DocumentFilterMode.ECO),
            FilterOption("gray", "Grayscale", DocumentFilterMode.GRAYSCALE),
            FilterOption("invert", "Invert", DocumentFilterMode.INVERT)
        )
    }

    var selectedFilterId by remember { mutableStateOf("no_shadow") }
    val activeFilter = filterOptions.firstOrNull { it.id == selectedFilterId } ?: filterOptions[0]

    var filterLevel by remember { mutableFloatStateOf(0.7f) }
    var isCompareModeActive by remember { mutableStateOf(false) }

    var showCropContour by remember { mutableStateOf(true) }
    var detectedQuad by remember(currentSafeIndex) {
        val bmp = editablePages.getOrNull(currentSafeIndex)
        mutableStateOf(
            if (bmp != null) OpenCvDocumentDetector.detectDocumentEdges(bmp)
            else OpenCvDocumentDetector.defaultQuad()
        )
    }
    var activeDraggingCorner by remember { mutableStateOf<String?>(null) }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showPageManagerDialog by remember { mutableStateOf(false) }
    var isCompilingPdf by remember { mutableStateOf(false) }

    val rawCurrentBitmap = editablePages.getOrNull(currentSafeIndex) ?: editablePages.first()

    val processedBitmap = remember(rawCurrentBitmap, selectedFilterId, filterLevel) {
        if (selectedFilterId == "orig") rawCurrentBitmap
        else OpenCvDocumentDetector.applyFilter(rawCurrentBitmap, activeFilter.mode)
    }

    val displayBitmap = if (isCompareModeActive) rawCurrentBitmap else processedBitmap

    BackHandler {
        onBackToCamera()
    }

    if (isRenamingTitle) {
        AlertDialog(
            onDismissRequest = { isRenamingTitle = false },
            containerColor = Color(0xFF1E293B),
            title = { Text("Rename Document", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = tempRenameText,
                    onValueChange = { tempRenameText = it },
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempRenameText.isNotBlank()) {
                            documentTitle = tempRenameText.trim()
                        }
                        isRenamingTitle = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16C7B5))
                ) {
                    Text("OK", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { isRenamingTitle = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            containerColor = Color(0xFF1E293B),
            title = { Text("Delete this page?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to remove page ${currentSafeIndex + 1}?",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editablePages.size > 1) {
                            editablePages.removeAt(currentSafeIndex)
                            viewModel.removeCapturedBitmap(currentSafeIndex)
                            currentPageIndex = (currentSafeIndex - 1).coerceAtLeast(0)
                        } else {
                            editablePages.clear()
                            viewModel.clearCapturedBitmaps()
                            onBackToCamera()
                        }
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    if (showPageManagerDialog) {
        AlertDialog(
            onDismissRequest = { showPageManagerDialog = false },
            containerColor = Color(0xFF0F172A),
            title = {
                Text(
                    "Manage Pages (${editablePages.size})",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    editablePages.forEachIndexed { idx, bmp ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (idx == currentSafeIndex) Color(0xFF16C7B5).copy(alpha = 0.2f) else Color(0xFF1E293B))
                                .clickable {
                                    currentPageIndex = idx
                                    showPageManagerDialog = false
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Page ${idx + 1}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            if (editablePages.size > 1) {
                                IconButton(
                                    onClick = {
                                        editablePages.removeAt(idx)
                                        viewModel.removeCapturedBitmap(idx)
                                        currentPageIndex = (currentPageIndex - 1).coerceAtLeast(0)
                                    }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPageManagerDialog = false }) {
                    Text("Done", color = Color(0xFF16C7B5), fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0E17))
            .statusBarsPadding()
    ) {
        // TOP TOOLBAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                IconButton(onClick = onBackToCamera) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            tempRenameText = documentTitle
                            isRenamingTitle = true
                        }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = documentTitle,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Rename",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            TextButton(
                onClick = onAddMorePages
            ) {
                Text(
                    text = "Add",
                    color = Color(0xFF16C7B5),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // CENTRAL DOCUMENT CANVAS
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = displayBitmap.asImageBitmap(),
                    contentDescription = "Document Scan",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )

                if (showCropContour) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(rawCurrentBitmap) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        val w = size.width.toFloat()
                                        val h = size.height.toFloat()
                                        val threshold = 70f
                                        val tlD = dist(offset.x, offset.y, detectedQuad.topLeft.x * w, detectedQuad.topLeft.y * h)
                                        val trD = dist(offset.x, offset.y, detectedQuad.topRight.x * w, detectedQuad.topRight.y * h)
                                        val brD = dist(offset.x, offset.y, detectedQuad.bottomRight.x * w, detectedQuad.bottomRight.y * h)
                                        val blD = dist(offset.x, offset.y, detectedQuad.bottomLeft.x * w, detectedQuad.bottomLeft.y * h)
                                        val m = minOf(tlD, trD, brD, blD)
                                        activeDraggingCorner = if (m <= threshold) {
                                            when (m) {
                                                tlD -> "TL"
                                                trD -> "TR"
                                                brD -> "BR"
                                                else -> "BL"
                                            }
                                        } else null
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        val w = size.width.toFloat()
                                        val h = size.height.toFloat()
                                        if (w <= 0f || h <= 0f) return@detectDragGestures
                                        val dxN = dragAmount.x / w
                                        val dyN = dragAmount.y / h
                                        when (activeDraggingCorner) {
                                            "TL" -> detectedQuad = detectedQuad.copy(
                                                topLeft = PointF(
                                                    (detectedQuad.topLeft.x + dxN).coerceIn(0f, 0.45f),
                                                    (detectedQuad.topLeft.y + dyN).coerceIn(0f, 0.45f)
                                                )
                                            )
                                            "TR" -> detectedQuad = detectedQuad.copy(
                                                topRight = PointF(
                                                    (detectedQuad.topRight.x + dxN).coerceIn(0.55f, 1f),
                                                    (detectedQuad.topRight.y + dyN).coerceIn(0f, 0.45f)
                                                )
                                            )
                                            "BR" -> detectedQuad = detectedQuad.copy(
                                                bottomRight = PointF(
                                                    (detectedQuad.bottomRight.x + dxN).coerceIn(0.55f, 1f),
                                                    (detectedQuad.bottomRight.y + dyN).coerceIn(0.55f, 1f)
                                                )
                                            )
                                            "BL" -> detectedQuad = detectedQuad.copy(
                                                bottomLeft = PointF(
                                                    (detectedQuad.bottomLeft.x + dxN).coerceIn(0f, 0.45f),
                                                    (detectedQuad.bottomLeft.y + dyN).coerceIn(0.55f, 1f)
                                                )
                                            )
                                        }
                                    },
                                    onDragEnd = { activeDraggingCorner = null },
                                    onDragCancel = { activeDraggingCorner = null }
                                )
                            }
                    ) {
                        val w = size.width
                        val h = size.height

                        val tlX = detectedQuad.topLeft.x * w
                        val tlY = detectedQuad.topLeft.y * h
                        val trX = detectedQuad.topRight.x * w
                        val trY = detectedQuad.topRight.y * h
                        val brX = detectedQuad.bottomRight.x * w
                        val brY = detectedQuad.bottomRight.y * h
                        val blX = detectedQuad.bottomLeft.x * w
                        val blY = detectedQuad.bottomLeft.y * h

                        val path = Path().apply {
                            moveTo(tlX, tlY)
                            lineTo(trX, trY)
                            lineTo(brX, brY)
                            lineTo(blX, blY)
                            close()
                        }

                        drawPath(
                            path = path,
                            color = Color(0xFF16C7B5),
                            style = Stroke(
                                width = 3.5f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
                            )
                        )

                        val pinRadius = 14f
                        listOf(
                            Offset(tlX, tlY),
                            Offset(trX, trY),
                            Offset(brX, brY),
                            Offset(blX, blY)
                        ).forEach { pin ->
                            drawCircle(color = Color.Black.copy(alpha = 0.5f), radius = pinRadius + 3f, center = pin)
                            drawCircle(color = Color(0xFF16C7B5), radius = pinRadius, center = pin)
                            drawCircle(color = Color.White, radius = 4f, center = pin)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(18.dp)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .clickable { showDeleteConfirmDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Page",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // OVERLAID BUTTONS
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .clickable { showDeleteConfirmDialog = true }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Remove", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Prev",
                            tint = if (currentSafeIndex > 0) Color.White else Color.Gray,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable(enabled = currentSafeIndex > 0) {
                                    currentPageIndex--
                                }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${currentSafeIndex + 1}/${editablePages.size}",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Next",
                            tint = if (currentSafeIndex < editablePages.size - 1) Color.White else Color.Gray,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable(enabled = currentSafeIndex < editablePages.size - 1) {
                                    currentPageIndex++
                                }
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isCompareModeActive) Color(0xFF16C7B5) else Color.Black.copy(alpha = 0.7f))
                        .clickable { isCompareModeActive = !isCompareModeActive }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Compare,
                            contentDescription = null,
                            tint = if (isCompareModeActive) Color.Black else Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Compare",
                            color = if (isCompareModeActive) Color.Black else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // HORIZONTAL FILTER STRIP
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF070B12))
                .padding(vertical = 8.dp)
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(filterOptions) { _, filter ->
                    val isSelected = selectedFilterId == filter.id

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable {
                                selectedFilterId = filter.id
                            }
                            .padding(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 62.dp, height = 48.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) Color(0xFF16C7B5) else Color.White.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                )
                        ) {
                            Image(
                                bitmap = rawCurrentBitmap.asImageBitmap(),
                                contentDescription = filter.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFF16C7B5).copy(alpha = 0.2f))
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = filter.name,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color(0xFF16C7B5) else Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }

        // BOTTOM EDITING TOOLS DOCK
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF04070D))
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                EditingDockItem(
                    icon = Icons.Default.PhotoCamera,
                    label = "Retake",
                    onClick = onBackToCamera
                )

                EditingDockItem(
                    icon = Icons.Default.RotateLeft,
                    label = "Left",
                    onClick = {
                        val matrix = Matrix().apply { postRotate(-90f) }
                        val rotated = Bitmap.createBitmap(rawCurrentBitmap, 0, 0, rawCurrentBitmap.width, rawCurrentBitmap.height, matrix, true)
                        editablePages[currentSafeIndex] = rotated
                        viewModel.updateCapturedBitmap(currentSafeIndex, rotated)
                    }
                )

                EditingDockItem(
                    icon = Icons.Default.Crop,
                    label = "Crop",
                    onClick = {
                        val warped = OpenCvDocumentDetector.cropAndWarpPerspective(rawCurrentBitmap, detectedQuad)
                        editablePages[currentSafeIndex] = warped
                        viewModel.updateCapturedBitmap(currentSafeIndex, warped)
                        detectedQuad = OpenCvDocumentDetector.defaultQuad()
                        Toast.makeText(context, "Page rectified & cropped!", Toast.LENGTH_SHORT).show()
                    }
                )

                EditingDockItem(
                    icon = Icons.Default.TextFields,
                    label = "Extract Text",
                    onClick = {
                        val tempDoc = ScannedDocument(
                            title = documentTitle,
                            filePath = "",
                            fileType = "PDF"
                        )
                        viewModel.runAiOcrOnDocument(tempDoc)
                        onExtractTextClicked(tempDoc)
                    }
                )

                EditingDockItem(
                    icon = Icons.Default.Draw,
                    label = "Sign",
                    onClick = {
                        Toast.makeText(context, "Signature tool opened for document stamp", Toast.LENGTH_SHORT).show()
                    }
                )

                EditingDockItem(
                    icon = Icons.Default.ManageHistory,
                    label = "Manage Pages",
                    onClick = { showPageManagerDialog = true }
                )

                Box(
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .size(width = 54.dp, height = 38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF16C7B5))
                        .clickable(enabled = !isCompilingPdf) {
                            isCompilingPdf = true
                            scope.launch(Dispatchers.IO) {
                                val filteredPages = editablePages.map { bmp ->
                                    if (selectedFilterId == "orig") bmp
                                    else OpenCvDocumentDetector.applyFilter(bmp, activeFilter.mode)
                                }

                                val pdfFile = PdfEngine.createPdfFromBitmaps(
                                    context = context,
                                    bitmaps = filteredPages,
                                    outputFileName = "${documentTitle.replace(" ", "_")}.pdf"
                                )

                                val newDoc = ScannedDocument(
                                    title = "${documentTitle}.pdf",
                                    filePath = pdfFile.absolutePath,
                                    fileType = "PDF",
                                    fileSizeBytes = pdfFile.length(),
                                    pageCount = filteredPages.size,
                                    category = "Documents",
                                    createdAt = System.currentTimeMillis()
                                )

                                val db = com.example.data.local.ScanProDatabase.getDatabase(context)
                                val id = db.documentDao().insertDocument(newDoc)
                                val savedDoc = newDoc.copy(id = id)
                                viewModel.setActiveDocument(savedDoc)
                                viewModel.clearCapturedBitmaps()

                                withContext(Dispatchers.Main) {
                                    isCompilingPdf = false
                                    Toast.makeText(context, "Document saved to Vault!", Toast.LENGTH_SHORT).show()
                                    onSaveAndOpenDocument(savedDoc)
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompilingPdf) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp))
                    } else {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Confirm & Save",
                            tint = Color.Black,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EditingDockItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun dist(x1: Float, y1: Float, x2: Float, y2: Float): Float {
    val dx = x2 - x1
    val dy = y2 - y1
    return sqrt(dx * dx + dy * dy)
}
