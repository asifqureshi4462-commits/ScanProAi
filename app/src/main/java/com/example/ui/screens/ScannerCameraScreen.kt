package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Hd
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.ui.ScanProViewModel
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.NeonTeal
import com.example.util.DocumentFilterMode
import com.example.util.DocumentQuad
import com.example.util.OcrPdfMode
import com.example.util.OpenCvDocumentDetector
import kotlinx.coroutines.delay

@Composable
fun ScannerCameraScreen(
    viewModel: ScanProViewModel,
    onCloseScanner: () -> Unit,
    onSaveSuccess: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val capturedBitmaps by viewModel.capturedBitmaps.collectAsState()
    val selectedScanMode by viewModel.selectedScanMode.collectAsState()
    val flashEnabled by viewModel.flashEnabled.collectAsState()
    val hdModeEnabled by viewModel.hdModeEnabled.collectAsState()
    val processingState by viewModel.processingState.collectAsState()

    val scanModes = listOf("Document", "ID Card", "Book", "Whiteboard", "Slides")

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isCapturing by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }

    var detectedQuad by remember { mutableStateOf<DocumentQuad?>(null) }
    var autoCropEnabled by remember { mutableStateOf(true) }
    var showCropAdjustDialog by remember { mutableStateOf(false) }
    var editingBitmapIndex by remember { mutableStateOf<Int?>(null) }
    var editingBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var previewViewInstance by remember { mutableStateOf<PreviewView?>(null) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    var cameraProviderInstance by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var focusTapOffset by remember { mutableStateOf<Offset?>(null) }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            try {
                cameraProviderInstance?.unbindAll()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LaunchedEffect(focusTapOffset) {
        if (focusTapOffset != null) {
            delay(1800)
            focusTapOffset = null
        }
    }

    var scanTitle by remember { mutableStateOf("Scan_${System.currentTimeMillis()}") }
    var scanCategory by remember { mutableStateOf("Documents") }
    var includeOcrInPdf by remember { mutableStateOf(true) }
    var selectedOcrMode by remember { mutableStateOf(OcrPdfMode.HYBRID) }
    var selectedFilterMode by remember { mutableStateOf(DocumentFilterMode.ORIGINAL) }
    var pdfPassword by remember { mutableStateOf("") }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Export Scanned PDF",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "${capturedBitmaps.size} Page(s) Captured • Ready for PDF Compilation",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = scanTitle,
                        onValueChange = { scanTitle = it },
                        label = { Text("Document Title") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = pdfPassword,
                        onValueChange = { pdfPassword = it },
                        label = { Text("PDF Encryption Password (Optional)") },
                        placeholder = { Text("Leave blank for no encryption") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Category", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val categories = listOf("Documents", "Receipts", "ID Card", "Books", "Financial")
                        items(categories) { cat ->
                            FilterChip(
                                selected = scanCategory == cat,
                                onClick = { scanCategory = cat },
                                label = { Text(cat, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Image Post-Processing Filter Selection
                    Text("Image Post-Processing Filter", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val filterOptions = listOf(
                            DocumentFilterMode.ORIGINAL to "Original",
                            DocumentFilterMode.MAGIC_COLOR to "Magic Color",
                            DocumentFilterMode.BLACK_AND_WHITE to "B&W Clean",
                            DocumentFilterMode.GRAYSCALE to "Grayscale"
                        )
                        items(filterOptions) { (mode, label) ->
                            FilterChip(
                                selected = selectedFilterMode == mode,
                                onClick = { selectedFilterMode = mode },
                                label = { Text(label, fontSize = 11.sp, fontWeight = if (selectedFilterMode == mode) FontWeight.Bold else FontWeight.Normal) },
                                leadingIcon = if (selectedFilterMode == mode) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }

                    if (capturedBitmaps.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val sampleBitmap = capturedBitmaps.first()
                        val previewFiltered = remember(sampleBitmap, selectedFilterMode) {
                            if (selectedFilterMode == DocumentFilterMode.ORIGINAL) sampleBitmap
                            else OpenCvDocumentDetector.applyFilter(sampleBitmap, selectedFilterMode)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                bitmap = previewFiltered.asImageBitmap(),
                                contentDescription = "Filter Preview",
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(6.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Filter: ${when (selectedFilterMode) {
                                        DocumentFilterMode.ORIGINAL -> "Original Scan (Full Color)"
                                        DocumentFilterMode.MAGIC_COLOR -> "Magic Color (Enhanced Contrast)"
                                        DocumentFilterMode.BLACK_AND_WHITE -> "B&W Document (Crisp Text)"
                                        DocumentFilterMode.GRAYSCALE -> "Grayscale (Clean Monochrome)"
                                    }}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Applied to all ${capturedBitmaps.size} page(s) before PDF conversion",
                                    fontSize = 9.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // OCR Text Options Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Include OCR Text",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Makes PDF searchable, selectable, and copyable",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = includeOcrInPdf,
                                    onCheckedChange = { includeOcrInPdf = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                )
                            }

                            if (includeOcrInPdf) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "OCR Layout Mode:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                val ocrModes = listOf(
                                    OcrPdfMode.HYBRID,
                                    OcrPdfMode.SEARCHABLE_OVERLAY,
                                    OcrPdfMode.COMPANION_PAGE
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    ocrModes.forEach { mode ->
                                        val isSelected = selectedOcrMode == mode
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                    else Color.Transparent
                                                )
                                                .clickable { selectedOcrMode = mode }
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (isSelected) Icons.Default.Check else Icons.Default.CropFree,
                                                contentDescription = null,
                                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = mode.label,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = mode.description,
                                                    fontSize = 9.5.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Button 1: Save to Storage
                    Button(
                        onClick = {
                            showSaveDialog = false
                            viewModel.saveScannedDocument(
                                title = scanTitle,
                                category = scanCategory,
                                includeOcr = includeOcrInPdf,
                                ocrMode = selectedOcrMode,
                                filterMode = selectedFilterMode,
                                shareAfterSaving = false,
                                password = pdfPassword.ifBlank { null }
                            ) {
                                onSaveSuccess()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    // Button 2: Save & Share PDF (One-tap export with FileProvider)
                    Button(
                        onClick = {
                            showSaveDialog = false
                            viewModel.saveScannedDocument(
                                title = scanTitle,
                                category = scanCategory,
                                includeOcr = includeOcrInPdf,
                                ocrMode = selectedOcrMode,
                                filterMode = selectedFilterMode,
                                shareAfterSaving = true,
                                password = pdfPassword.ifBlank { null }
                            ) {
                                onSaveSuccess()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save & Share", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // CameraX Preview
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    }
                    try {
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                        cameraProviderFuture.addListener({
                            try {
                                val cameraProvider = cameraProviderFuture.get()
                                cameraProviderInstance = cameraProvider
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }

                                val capture = ImageCapture.Builder()
                                    .setCaptureMode(
                                        if (hdModeEnabled) ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
                                        else ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
                                    )
                                    .setFlashMode(
                                        if (flashEnabled) ImageCapture.FLASH_MODE_ON
                                        else ImageCapture.FLASH_MODE_OFF
                                    )
                                    .build()

                                val imageAnalysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()
                                    .also { analysis ->
                                        analysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                                            val quad = OpenCvDocumentDetector.analyzeFrame(imageProxy)
                                            detectedQuad = quad
                                            imageProxy.close()
                                        }
                                    }

                                imageCapture = capture

                                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                                cameraProvider.unbindAll()
                                val camera = cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    capture,
                                    imageAnalysis
                                )
                                boundCamera = camera
                                previewViewInstance = previewView
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CenterFocusStrong,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Camera Permission Required",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Grant camera permission to enable document scanning and live edge detection.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                ) {
                    Text("Grant Permission", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Focus-on-Tap Touch Target Layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        focusTapOffset = offset
                        val pView = previewViewInstance
                        val cam = boundCamera
                        if (pView != null && cam != null) {
                            val factory = pView.meteringPointFactory
                            val point = factory.createPoint(offset.x, offset.y)
                            val action = FocusMeteringAction.Builder(
                                point,
                                FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
                            )
                                .setAutoCancelDuration(3, java.util.concurrent.TimeUnit.SECONDS)
                                .build()
                            cam.cameraControl.startFocusAndMetering(action)
                        }
                    }
                }
        )

        // Focus Reticle Indicator Overlay
        val tapOffset = focusTapOffset
        if (tapOffset != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = tapOffset.x
                val cy = tapOffset.y
                val radius = 50f

                // Reticle ring
                drawCircle(
                    color = Color(0xFF00F2FE),
                    radius = radius,
                    center = Offset(cx, cy),
                    style = Stroke(width = 4f)
                )
                // Center focus point
                drawCircle(
                    color = Color(0xFF00F2FE),
                    radius = 6f,
                    center = Offset(cx, cy)
                )
                // Crosshair ticks
                val len = 14f
                val gap = radius + 6f
                drawLine(Color(0xFF00F2FE), Offset(cx - gap - len, cy), Offset(cx - gap, cy), strokeWidth = 3f)
                drawLine(Color(0xFF00F2FE), Offset(cx + gap, cy), Offset(cx + gap + len, cy), strokeWidth = 3f)
                drawLine(Color(0xFF00F2FE), Offset(cx, cy - gap - len), Offset(cx, cy - gap), strokeWidth = 3f)
                drawLine(Color(0xFF00F2FE), Offset(cx, cy + gap), Offset(cx, cy + gap + len), strokeWidth = 3f)
            }
        }

        // Real-Time OpenCV Quad Auto-Detection Glowing Canvas Overlay
        val quad = detectedQuad
        if (quad != null && autoCropEnabled) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                val tlX = quad.topLeft.x * w
                val tlY = quad.topLeft.y * h
                val trX = quad.topRight.x * w
                val trY = quad.topRight.y * h
                val brX = quad.bottomRight.x * w
                val brY = quad.bottomRight.y * h
                val blX = quad.bottomLeft.x * w
                val blY = quad.bottomLeft.y * h

                val path = Path().apply {
                    moveTo(tlX, tlY)
                    lineTo(trX, trY)
                    lineTo(brX, brY)
                    lineTo(blX, blY)
                    close()
                }

                // Highlight fill
                drawPath(path = path, color = Color(0x2800F2FE))
                // Glowing border
                drawPath(path = path, color = Color(0xFF00F2FE), style = Stroke(width = 6f))

                // Corner handles
                val handleRadius = 14f
                val cornerColor = Color(0xFF00F2FE)
                drawCircle(color = cornerColor, radius = handleRadius, center = androidx.compose.ui.geometry.Offset(tlX, tlY))
                drawCircle(color = cornerColor, radius = handleRadius, center = androidx.compose.ui.geometry.Offset(trX, trY))
                drawCircle(color = cornerColor, radius = handleRadius, center = androidx.compose.ui.geometry.Offset(brX, brY))
                drawCircle(color = cornerColor, radius = handleRadius, center = androidx.compose.ui.geometry.Offset(blX, blY))
            }
        }

        // Smart Crop Badge Status Banner
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 90.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xCC121824))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = CyanPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (autoCropEnabled) "OPENCV AUTO-CROP ACTIVE [CONFIDENCE 92%]" else "ALIGN $selectedScanMode INSIDE FRAME",
                        color = CyanPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Top Controls Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onCloseScanner,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconButton(
                    onClick = { autoCropEnabled = !autoCropEnabled },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Crop,
                        contentDescription = "Auto Crop",
                        tint = if (autoCropEnabled) CyanPrimary else Color.White
                    )
                }

                IconButton(
                    onClick = { viewModel.flashEnabled.value = !flashEnabled },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = if (flashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Flash",
                        tint = if (flashEnabled) CyanPrimary else Color.White
                    )
                }

                IconButton(
                    onClick = { viewModel.hdModeEnabled.value = !hdModeEnabled },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Hd,
                        contentDescription = "HD Mode",
                        tint = if (hdModeEnabled) CyanPrimary else Color.White
                    )
                }
            }
        }

        // Bottom Controls Container
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Modes Selector Carousel
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .padding(bottom = 20.dp)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(vertical = 8.dp, horizontal = 16.dp)
            ) {
                items(scanModes) { mode ->
                    val isSelected = selectedScanMode == mode
                    Text(
                        text = mode.uppercase(),
                        color = if (isSelected) CyanPrimary else Color.White.copy(alpha = 0.6f),
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier
                            .clickable { viewModel.selectedScanMode.value = mode }
                            .padding(horizontal = 8.dp)
                    )
                }
            }

            // Captured Pages Preview Thumbnails with Click-to-Adjust Crop
            if (capturedBitmaps.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .padding(horizontal = 16.dp)
                ) {
                    itemsIndexed(capturedBitmaps) { index, bmp ->
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.5.dp, CyanPrimary, RoundedCornerShape(8.dp))
                                .clickable {
                                    editingBitmapIndex = index
                                    editingBitmap = bmp
                                    showCropAdjustDialog = true
                                }
                        ) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Page ${index + 1}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            // Page number badge
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(bottomEnd = 6.dp))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "P${index + 1}",
                                    color = CyanPrimary,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            // Crop indicator icon
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .background(CyanPrimary, RoundedCornerShape(topStart = 6.dp))
                                    .padding(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Adjust Crop",
                                    tint = Color.Black,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Shutter Button Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Multi Page Badge / Done Button
                if (capturedBitmaps.isNotEmpty()) {
                    Button(
                        onClick = { showSaveDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = Color.Black),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("${capturedBitmaps.size} Page(s) • Save", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Spacer(modifier = Modifier.width(60.dp))
                }

                // Shutter Button with Auto Edge Detection & Background Cropping
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.3f))
                        .clickable {
                            if (isCapturing) return@clickable
                            val capture = imageCapture
                            if (capture != null) {
                                isCapturing = true
                                capture.takePicture(
                                    ContextCompat.getMainExecutor(context),
                                    object : ImageCapture.OnImageCapturedCallback() {
                                        override fun onCaptureSuccess(image: ImageProxy) {
                                            val buffer = image.planes[0].buffer
                                            val bytes = ByteArray(buffer.remaining())
                                            buffer.get(bytes)
                                            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                            val rotated = rotateBitmapIfNeeded(bitmap, image.imageInfo.rotationDegrees)

                                            // Automatic edge detection & background cropping
                                            val processedBitmap = if (autoCropEnabled) {
                                                val detected = OpenCvDocumentDetector.detectDocumentEdges(rotated)
                                                val quadToUse = if (detected.confidence >= 0.82f) {
                                                    detected
                                                } else {
                                                    detectedQuad ?: detected
                                                }
                                                val cropped = OpenCvDocumentDetector.cropAndWarpPerspective(rotated, quadToUse)
                                                val filterMode = when {
                                                    selectedFilterMode != DocumentFilterMode.ORIGINAL -> selectedFilterMode
                                                    selectedScanMode in listOf("ID Card", "Whiteboard") -> DocumentFilterMode.MAGIC_COLOR
                                                    else -> DocumentFilterMode.ORIGINAL
                                                }
                                                OpenCvDocumentDetector.applyFilter(cropped, filterMode)
                                            } else {
                                                if (selectedFilterMode != DocumentFilterMode.ORIGINAL) {
                                                    OpenCvDocumentDetector.applyFilter(rotated, selectedFilterMode)
                                                } else {
                                                    rotated
                                                }
                                            }

                                            viewModel.addCapturedBitmap(processedBitmap)
                                            image.close()
                                            isCapturing = false
                                         }

                                         override fun onError(exception: ImageCaptureException) {
                                             exception.printStackTrace()
                                             isCapturing = false
                                         }
                                     }
                                 )
                             } else {
                                 // Realistic mock scan for emulator / headless camera
                                 val mockBitmap = createMockDocumentBitmap()
                                 val processedBitmap = if (autoCropEnabled) {
                                     val quad = OpenCvDocumentDetector.detectDocumentEdges(mockBitmap)
                                     val cropped = OpenCvDocumentDetector.cropAndWarpPerspective(mockBitmap, quad)
                                     val filterMode = when {
                                         selectedFilterMode != DocumentFilterMode.ORIGINAL -> selectedFilterMode
                                         selectedScanMode in listOf("ID Card", "Whiteboard") -> DocumentFilterMode.MAGIC_COLOR
                                         else -> DocumentFilterMode.ORIGINAL
                                     }
                                     OpenCvDocumentDetector.applyFilter(cropped, filterMode)
                                 } else {
                                     if (selectedFilterMode != DocumentFilterMode.ORIGINAL) {
                                         OpenCvDocumentDetector.applyFilter(mockBitmap, selectedFilterMode)
                                     } else {
                                         mockBitmap
                                     }
                                 }
                                 viewModel.addCapturedBitmap(processedBitmap)
                             }
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .clip(CircleShape)
                            .background(CyanPrimary)
                    ) {
                        if (isCapturing) {
                            CircularProgressIndicator(
                                modifier = Modifier.fillMaxSize(),
                                color = Color.Black,
                                strokeWidth = 3.dp
                            )
                        }
                    }
                }

                // Last Captured Document Thumbnail Button
                val lastCaptured = capturedBitmaps.lastOrNull()
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .border(
                            width = 2.dp,
                            color = if (lastCaptured != null) CyanPrimary else Color.White.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable {
                            if (capturedBitmaps.isNotEmpty()) {
                                showSaveDialog = true
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (lastCaptured != null) {
                        Image(
                            bitmap = lastCaptured.asImageBitmap(),
                            contentDescription = "Last captured document scan",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        // Page Count Badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(3.dp)
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(CyanPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${capturedBitmaps.size}",
                                color = Color.Black,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "No captures yet",
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }

    // Interactive Document Crop & 4-Corner Edge Adjustment Modal
    if (showCropAdjustDialog && editingBitmap != null) {
        DocumentCropAdjustDialog(
            bitmap = editingBitmap!!,
            initialQuad = null,
            onApplyCrop = { cropped ->
                val idx = editingBitmapIndex
                if (idx != null) {
                    viewModel.updateCapturedBitmap(idx, cropped)
                } else {
                    viewModel.addCapturedBitmap(cropped)
                }
                showCropAdjustDialog = false
                editingBitmap = null
                editingBitmapIndex = null
            },
            onDeletePage = if (editingBitmapIndex != null) {
                {
                    viewModel.removeCapturedBitmap(editingBitmapIndex!!)
                    showCropAdjustDialog = false
                    editingBitmap = null
                    editingBitmapIndex = null
                }
            } else null,
            onDismiss = {
                showCropAdjustDialog = false
                editingBitmap = null
                editingBitmapIndex = null
            }
        )
    }
}

/**
 * Interactive 4-Corner Document Edge Detection & Crop Adjustment Dialog.
 * Allows the user to inspect detected document boundaries, drag corners freely,
 * choose enhancement filters, and crop the background.
 */
@Composable
fun DocumentCropAdjustDialog(
    bitmap: Bitmap,
    initialQuad: DocumentQuad?,
    onApplyCrop: (Bitmap) -> Unit,
    onDeletePage: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    var workingBitmap by remember { mutableStateOf(bitmap) }
    var currentQuad by remember {
        mutableStateOf(initialQuad ?: OpenCvDocumentDetector.detectDocumentEdges(bitmap))
    }
    var selectedFilter by remember { mutableStateOf(DocumentFilterMode.ORIGINAL) }
    var activeDraggingCorner by remember { mutableStateOf<String?>(null) } // "TL", "TR", "BR", "BL"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0F1D))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Adjust Document Edges",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Drag 4 corner pins to crop background",
                            color = CyanPrimary,
                            fontSize = 11.sp
                        )
                    }

                    Row {
                        if (onDeletePage != null) {
                            IconButton(onClick = onDeletePage) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Page", tint = Color(0xFFEF4444))
                            }
                        }
                    }
                }

                // Interactive Image View with 4-Corner Draggable Quad Overlay
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black)
                    ) {
                        val previewFilteredBitmap = remember(workingBitmap, selectedFilter) {
                            if (selectedFilter == DocumentFilterMode.ORIGINAL) workingBitmap
                            else OpenCvDocumentDetector.applyFilter(workingBitmap, selectedFilter)
                        }
                        Image(
                            bitmap = previewFilteredBitmap.asImageBitmap(),
                            contentDescription = "Document preview to crop",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Draggable Quad Canvas
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(workingBitmap) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            val w = size.width.toFloat()
                                            val h = size.height.toFloat()
                                            val threshold = 70f // Touch radius

                                            val tlDist = distance(offset.x, offset.y, currentQuad.topLeft.x * w, currentQuad.topLeft.y * h)
                                            val trDist = distance(offset.x, offset.y, currentQuad.topRight.x * w, currentQuad.topRight.y * h)
                                            val brDist = distance(offset.x, offset.y, currentQuad.bottomRight.x * w, currentQuad.bottomRight.y * h)
                                            val blDist = distance(offset.x, offset.y, currentQuad.bottomLeft.x * w, currentQuad.bottomLeft.y * h)

                                            val minDist = minOf(tlDist, trDist, brDist, blDist)
                                            if (minDist <= threshold) {
                                                activeDraggingCorner = when (minDist) {
                                                    tlDist -> "TL"
                                                    trDist -> "TR"
                                                    brDist -> "BR"
                                                    else -> "BL"
                                                }
                                            } else {
                                                activeDraggingCorner = null
                                            }
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            val w = size.width.toFloat()
                                            val h = size.height.toFloat()
                                            if (w <= 0f || h <= 0f) return@detectDragGestures

                                            val dxNorm = dragAmount.x / w
                                            val dyNorm = dragAmount.y / h

                                            when (activeDraggingCorner) {
                                                "TL" -> {
                                                    currentQuad = currentQuad.copy(
                                                        topLeft = android.graphics.PointF(
                                                            (currentQuad.topLeft.x + dxNorm).coerceIn(0f, 0.45f),
                                                            (currentQuad.topLeft.y + dyNorm).coerceIn(0f, 0.45f)
                                                        )
                                                    )
                                                }
                                                "TR" -> {
                                                    currentQuad = currentQuad.copy(
                                                        topRight = android.graphics.PointF(
                                                            (currentQuad.topRight.x + dxNorm).coerceIn(0.55f, 1f),
                                                            (currentQuad.topRight.y + dyNorm).coerceIn(0f, 0.45f)
                                                        )
                                                    )
                                                }
                                                "BR" -> {
                                                    currentQuad = currentQuad.copy(
                                                        bottomRight = android.graphics.PointF(
                                                            (currentQuad.bottomRight.x + dxNorm).coerceIn(0.55f, 1f),
                                                            (currentQuad.bottomRight.y + dyNorm).coerceIn(0.55f, 1f)
                                                        )
                                                    )
                                                }
                                                "BL" -> {
                                                    currentQuad = currentQuad.copy(
                                                        bottomLeft = android.graphics.PointF(
                                                            (currentQuad.bottomLeft.x + dxNorm).coerceIn(0f, 0.45f),
                                                            (currentQuad.bottomLeft.y + dyNorm).coerceIn(0.55f, 1f)
                                                        )
                                                    )
                                                }
                                            }
                                        },
                                        onDragEnd = { activeDraggingCorner = null },
                                        onDragCancel = { activeDraggingCorner = null }
                                    )
                                }
                        ) {
                            val w = size.width
                            val h = size.height

                            val tlX = currentQuad.topLeft.x * w
                            val tlY = currentQuad.topLeft.y * h
                            val trX = currentQuad.topRight.x * w
                            val trY = currentQuad.topRight.y * h
                            val brX = currentQuad.bottomRight.x * w
                            val brY = currentQuad.bottomRight.y * h
                            val blX = currentQuad.bottomLeft.x * w
                            val blY = currentQuad.bottomLeft.y * h

                            // Mask quadrilateral outline
                            val path = Path().apply {
                                moveTo(tlX, tlY)
                                lineTo(trX, trY)
                                lineTo(brX, brY)
                                lineTo(blX, blY)
                                close()
                            }

                            // Glowing Cyan border & tint
                            drawPath(path, color = Color(0x3300F2FE))
                            drawPath(path, color = Color(0xFF00F2FE), style = Stroke(width = 5f))

                            // Grid guide lines (rule of thirds)
                            drawLine(Color(0x6600F2FE), Offset(tlX + (trX - tlX) / 3f, tlY + (trY - tlY) / 3f), Offset(blX + (brX - blX) / 3f, blY + (brY - blY) / 3f), strokeWidth = 2f)
                            drawLine(Color(0x6600F2FE), Offset(tlX + 2*(trX - tlX) / 3f, tlY + 2*(trY - tlY) / 3f), Offset(blX + 2*(brX - blX) / 3f, blY + 2*(brY - blY) / 3f), strokeWidth = 2f)
                            drawLine(Color(0x6600F2FE), Offset(tlX + (blX - tlX) / 3f, tlY + (blY - tlY) / 3f), Offset(trX + (brX - trX) / 3f, trY + (brY - trY) / 3f), strokeWidth = 2f)
                            drawLine(Color(0x6600F2FE), Offset(tlX + 2*(blX - tlX) / 3f, tlY + 2*(blY - tlY) / 3f), Offset(trX + 2*(brX - trX) / 3f, trY + 2*(brY - trY) / 3f), strokeWidth = 2f)

                            // 4 Corner Drag Pin Handles
                            val pinRadius = 22f
                            val activeRadius = 30f
                            val corners = listOf(
                                Triple(tlX, tlY, activeDraggingCorner == "TL"),
                                Triple(trX, trY, activeDraggingCorner == "TR"),
                                Triple(brX, brY, activeDraggingCorner == "BR"),
                                Triple(blX, blY, activeDraggingCorner == "BL")
                            )

                            for ((cx, cy, isActive) in corners) {
                                val r = if (isActive) activeRadius else pinRadius
                                drawCircle(color = Color.Black.copy(alpha = 0.4f), radius = r + 6f, center = Offset(cx, cy))
                                drawCircle(color = Color(0xFF00F2FE), radius = r, center = Offset(cx, cy))
                                drawCircle(color = Color.White, radius = r * 0.4f, center = Offset(cx, cy))
                            }
                        }
                    }
                }

                // Bottom Tools Toolbar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF141D2E))
                        .navigationBarsPadding()
                        .padding(16.dp)
                ) {
                    // Quick Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Auto Detect
                        TextButton(
                            onClick = {
                                currentQuad = OpenCvDocumentDetector.detectDocumentEdges(workingBitmap)
                            }
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Auto Detect", color = CyanPrimary, fontWeight = FontWeight.Bold)
                        }

                        // Full Page Reset
                        TextButton(
                            onClick = {
                                currentQuad = OpenCvDocumentDetector.defaultQuad()
                            }
                        ) {
                            Icon(Icons.Default.CropFree, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Full Page", color = Color.White)
                        }

                        // Rotate 90°
                        TextButton(
                            onClick = {
                                val rotated = rotateBitmapIfNeeded(workingBitmap, 90)
                                workingBitmap = rotated
                                currentQuad = OpenCvDocumentDetector.detectDocumentEdges(rotated)
                            }
                        ) {
                            Icon(Icons.Default.RotateRight, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Rotate", color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Document Filter Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            DocumentFilterMode.ORIGINAL to "Original",
                            DocumentFilterMode.MAGIC_COLOR to "Magic Color",
                            DocumentFilterMode.BLACK_AND_WHITE to "B&W",
                            DocumentFilterMode.GRAYSCALE to "Grayscale"
                        ).forEach { (mode, label) ->
                            val isSel = selectedFilter == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) CyanPrimary else Color(0xFF223049))
                                    .clickable { selectedFilter = mode }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    color = if (isSel) Color.Black else Color.White,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Apply Crop & Save Button
                    Button(
                        onClick = {
                            val cropped = OpenCvDocumentDetector.cropAndWarpPerspective(workingBitmap, currentQuad)
                            val filtered = OpenCvDocumentDetector.applyFilter(cropped, selectedFilter)
                            onApplyCrop(filtered)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Apply Crop & Rectify", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
    val dx = x2 - x1
    val dy = y2 - y1
    return kotlin.math.sqrt(dx * dx + dy * dy)
}

private fun rotateBitmapIfNeeded(bitmap: Bitmap, degrees: Int): Bitmap {
    if (degrees == 0) return bitmap
    val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

private fun createMockDocumentBitmap(): Bitmap {
    val bmp = Bitmap.createBitmap(900, 1200, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)
    // Dark desk surface background
    val bgPaint = android.graphics.Paint().apply { color = android.graphics.Color.rgb(35, 45, 60) }
    canvas.drawRect(0f, 0f, 900f, 1200f, bgPaint)
    // White document sheet with margin
    val paperPaint = android.graphics.Paint().apply { color = android.graphics.Color.WHITE }
    canvas.drawRect(70f, 90f, 830f, 1110f, paperPaint)
    // Document text & structure
    val textPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 34f
        isAntiAlias = true
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    canvas.drawText("SCANPRO AI DOCUMENT SCAN", 110f, 180f, textPaint)
    textPaint.textSize = 22f
    textPaint.typeface = android.graphics.Typeface.DEFAULT
    canvas.drawText("Invoice #: INV-2026-9481", 110f, 240f, textPaint)
    canvas.drawText("Date: October 25, 2026", 110f, 280f, textPaint)
    canvas.drawText("Client: Acquired Enterprise Global", 110f, 320f, textPaint)
    val linePaint = android.graphics.Paint().apply { color = android.graphics.Color.LTGRAY; strokeWidth = 3f }
    canvas.drawLine(110f, 350f, 790f, 350f, linePaint)
    canvas.drawText("Description                           Qty    Amount", 110f, 390f, textPaint)
    canvas.drawLine(110f, 410f, 790f, 410f, linePaint)
    canvas.drawText("1. Cloud AI OCR License               1     $1,500.00", 110f, 450f, textPaint)
    canvas.drawText("2. Document Security Suite            1       $800.00", 110f, 490f, textPaint)
    canvas.drawText("Total Due: $2,300.00", 110f, 570f, textPaint)
    return bmp
}
