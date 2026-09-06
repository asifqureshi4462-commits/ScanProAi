package com.example.ui.screens

import android.Manifest
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
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOff
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Hd
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ui.ScanProViewModel
import com.example.ui.theme.CyanPrimary
import com.example.util.DocumentQuad
import com.example.util.OpenCvDocumentDetector
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private val scannerTips = listOf(
    "Keep your phone parallel to the document",
    "Make sure the whole page is inside the frame",
    "Good lighting improves OCR accuracy",
    "Hold steady for a sharp, blur-free scan"
)

@Composable
fun ScannerCameraScreen(
    viewModel: ScanProViewModel,
    onCloseScanner: () -> Unit,
    onSaveSuccess: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptic = LocalHapticFeedback.current

    val capturedBitmaps by viewModel.capturedBitmaps.collectAsState()
    val selectedScanMode by viewModel.selectedScanMode.collectAsState()
    val flashEnabled by viewModel.flashEnabled.collectAsState()
    val hdModeEnabled by viewModel.hdModeEnabled.collectAsState()

    val scanModes = listOf("Document", "ID Card", "Book", "Whiteboard", "Slides")

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isCapturing by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showCaptureFlash by remember { mutableStateOf(false) }

    var detectedQuad by remember { mutableStateOf<DocumentQuad?>(null) }
    var autoCropEnabled by remember { mutableStateOf(true) }
    var gridEnabled by remember { mutableStateOf(false) }

    var zoomRatio by remember { mutableFloatStateOf(1f) }
    var maxZoomRatio by remember { mutableFloatStateOf(5f) }
    var showZoomBadge by remember { mutableStateOf(false) }

    var tipIndex by remember { mutableIntStateOf(0) }

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

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val bmp = BitmapFactory.decodeStream(stream)
                        if (bmp != null) viewModel.addCapturedBitmap(bmp)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Rotate through helpful scanning tips
    LaunchedEffect(Unit) {
        while (true) {
            delay(3500)
            tipIndex = (tipIndex + 1) % scannerTips.size
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

    LaunchedEffect(showCaptureFlash) {
        if (showCaptureFlash) {
            delay(90)
            showCaptureFlash = false
        }
    }

    LaunchedEffect(showZoomBadge) {
        if (showZoomBadge) {
            delay(1200)
            showZoomBadge = false
        }
    }

    var scanTitle by remember { mutableStateOf("Scan_${System.currentTimeMillis()}") }
    val scanCategory by remember { mutableStateOf("Documents") }

    fun handleCapturedBitmap(bitmap: Bitmap, rotationDegrees: Int) {
        val rotated = rotateBitmapIfNeeded(bitmap, rotationDegrees)
        val currentQuad = detectedQuad
        val processedBitmap = if (autoCropEnabled && currentQuad != null) {
            OpenCvDocumentDetector.cropAndWarpPerspective(rotated, currentQuad)
        } else {
            rotated
        }
        viewModel.addCapturedBitmap(processedBitmap)
    }

    fun triggerCapture() {
        if (isCapturing) return
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        showCaptureFlash = true
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
                        handleCapturedBitmap(bitmap, image.imageInfo.rotationDegrees)
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
            // Fallback mock scan if camera is unavailable (e.g. emulator without camera)
            val mockBitmap = Bitmap.createBitmap(800, 1200, Bitmap.Config.ARGB_8888)
            viewModel.addCapturedBitmap(mockBitmap)
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Save Document Scan", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Pages scanned: ${capturedBitmaps.size}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
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
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSaveDialog = false
                        viewModel.saveScannedDocument(scanTitle, scanCategory) {
                            onSaveSuccess()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                ) {
                    Text("Save to Vault", fontWeight = FontWeight.Bold)
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
                                maxZoomRatio = camera.cameraInfo.zoomState.value?.maxZoomRatio ?: 5f
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
                    textAlign = TextAlign.Center
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

        // Pinch-to-zoom Layer
        val transformState = rememberTransformableState { zoomChange, _, _ ->
            val newRatio = (zoomRatio * zoomChange).coerceIn(1f, maxZoomRatio)
            zoomRatio = newRatio
            showZoomBadge = true
            boundCamera?.cameraControl?.setZoomRatio(newRatio)
        }

        // Focus-on-Tap Touch Target Layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .transformable(state = transformState)
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

        // Rule-of-Thirds Grid Overlay
        if (gridEnabled && hasCameraPermission) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val gridColor = Color.White.copy(alpha = 0.35f)
                drawLine(gridColor, Offset(w / 3f, 0f), Offset(w / 3f, h), strokeWidth = 1.5f)
                drawLine(gridColor, Offset(2 * w / 3f, 0f), Offset(2 * w / 3f, h), strokeWidth = 1.5f)
                drawLine(gridColor, Offset(0f, h / 3f), Offset(w, h / 3f), strokeWidth = 1.5f)
                drawLine(gridColor, Offset(0f, 2 * h / 3f), Offset(w, 2 * h / 3f), strokeWidth = 1.5f)
            }
        }

        // Focus Reticle Indicator Overlay
        val tapOffset = focusTapOffset
        if (tapOffset != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = tapOffset.x
                val cy = tapOffset.y
                val radius = 50f

                drawCircle(
                    color = Color(0xFF00F2FE),
                    radius = radius,
                    center = Offset(cx, cy),
                    style = Stroke(width = 4f)
                )
                drawCircle(
                    color = Color(0xFF00F2FE),
                    radius = 6f,
                    center = Offset(cx, cy)
                )
                val len = 14f
                val gap = radius + 6f
                drawLine(Color(0xFF00F2FE), Offset(cx - gap - len, cy), Offset(cx - gap, cy), strokeWidth = 3f)
                drawLine(Color(0xFF00F2FE), Offset(cx + gap, cy), Offset(cx + gap + len, cy), strokeWidth = 3f)
                drawLine(Color(0xFF00F2FE), Offset(cx, cy - gap - len), Offset(cx, cy - gap), strokeWidth = 3f)
                drawLine(Color(0xFF00F2FE), Offset(cx, cy + gap), Offset(cx, cy + gap + len), strokeWidth = 3f)
            }
        }

        // Pulsing glow for the auto-detected quad outline
        val glowTransition = rememberInfiniteTransition(label = "quadGlow")
        val glowAlpha by glowTransition.animateFloat(
            initialValue = 0.55f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glowAlpha"
        )

        // Real-Time Quad Auto-Detection Glowing Canvas Overlay
        val quad = detectedQuad
        if (quad != null && autoCropEnabled && hasCameraPermission) {
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

                drawPath(path = path, color = Color(0x2800F2FE))
                drawPath(path = path, color = Color(0xFF00F2FE).copy(alpha = glowAlpha), style = Stroke(width = 6f))

                val handleRadius = 14f
                val cornerColor = Color(0xFF00F2FE).copy(alpha = glowAlpha)
                drawCircle(color = cornerColor, radius = handleRadius, center = Offset(tlX, tlY))
                drawCircle(color = cornerColor, radius = handleRadius, center = Offset(trX, trY))
                drawCircle(color = cornerColor, radius = handleRadius, center = Offset(brX, brY))
                drawCircle(color = cornerColor, radius = handleRadius, center = Offset(blX, blY))
            }
        }

        // Capture flash effect
        AnimatedVisibility(
            visible = showCaptureFlash,
            enter = fadeIn(tween(30)),
            exit = fadeOut(tween(220)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.85f)))
        }

        // Top gradient scrim (legibility for top controls)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent)
                    )
                )
        )

        // Bottom gradient scrim (legibility for bottom controls)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))
                    )
                )
        )

        // Smart Crop / Confidence Status Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 90.dp),
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
                    val confidenceText = if (autoCropEnabled && quad != null) {
                        val pct = (quad.confidence * 100).roundToInt().coerceIn(0, 99)
                        if (pct >= 85) "PERFECT ALIGNMENT • TAP TO CAPTURE [$pct%]"
                        else "AUTO-CROP ACTIVE [CONFIDENCE $pct%]"
                    } else {
                        "ALIGN $selectedScanMode INSIDE FRAME"
                    }
                    Text(
                        text = confidenceText,
                        color = CyanPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Zoom ratio badge (shows briefly while pinching)
        AnimatedVisibility(
            visible = showZoomBadge,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "${String.format("%.1f", zoomRatio)}x",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
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

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IconButton(
                    onClick = { gridEnabled = !gridEnabled },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = if (gridEnabled) Icons.Default.GridOn else Icons.Default.GridOff,
                        contentDescription = "Grid Lines",
                        tint = if (gridEnabled) CyanPrimary else Color.White
                    )
                }

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

                IconButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "Import from Gallery",
                        tint = Color.White
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
            // Rotating scanning tip
            AnimatedVisibility(visible = hasCameraPermission, enter = fadeIn(), exit = fadeOut()) {
                Text(
                    text = "💡 ${scannerTips[tipIndex]}",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 10.dp, start = 32.dp, end = 32.dp)
                )
            }

            // Quick Zoom Presets
            if (hasCameraPermission) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(bottom = 14.dp)
                ) {
                    listOf(1f, 2f, 3f).forEach { level ->
                        if (level <= maxZoomRatio) {
                            val isActive = kotlin.math.abs(zoomRatio - level) < 0.15f
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (isActive) CyanPrimary else Color.Black.copy(alpha = 0.5f))
                                    .clickable {
                                        zoomRatio = level
                                        showZoomBadge = true
                                        boundCamera?.cameraControl?.setZoomRatio(level)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "${level.toInt()}x",
                                    color = if (isActive) Color.Black else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Modes Selector Carousel
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                items(scanModes) { mode ->
                    val isSelected = selectedScanMode == mode
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (isSelected) CyanPrimary else Color.Black.copy(alpha = 0.45f))
                            .clickable { viewModel.selectedScanMode.value = mode }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = mode.uppercase(),
                            color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            // Captured Pages Preview Thumbnails
            if (capturedBitmaps.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .padding(horizontal = 16.dp)
                ) {
                    items(capturedBitmaps) { bmp ->
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, CyanPrimary, RoundedCornerShape(8.dp))
                        ) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
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

                // Shutter Button with press animation
                val shutterInteraction = remember { MutableInteractionSource() }
                val isPressed by shutterInteraction.collectIsPressedAsState()
                val shutterScale by animateFloatAsState(
                    targetValue = if (isPressed) 0.88f else 1f,
                    animationSpec = tween(120),
                    label = "shutterScale"
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(76.dp)
                        .scale(shutterScale)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.3f))
                        .clickable(
                            interactionSource = shutterInteraction,
                            indication = null
                        ) { triggerCapture() }
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

                // Last Captured Document Thumbnail Button (also opens gallery import when empty)
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
                            } else {
                                galleryLauncher.launch("image/*")
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
                            contentDescription = "Import from gallery",
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun rotateBitmapIfNeeded(bitmap: Bitmap, degrees: Int): Bitmap {
    if (degrees == 0) return bitmap
    val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}
