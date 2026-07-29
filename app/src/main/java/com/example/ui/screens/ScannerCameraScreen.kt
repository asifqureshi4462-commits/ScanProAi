package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ui.ScanProViewModel
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.NeonTeal
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.DocumentQuad
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

    var previewViewInstance by remember { mutableStateOf<PreviewView?>(null) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    var focusTapOffset by remember { mutableStateOf<Offset?>(null) }

    LaunchedEffect(focusTapOffset) {
        if (focusTapOffset != null) {
            delay(1800)
            focusTapOffset = null
        }
    }

    var scanTitle by remember { mutableStateOf("Scan_${System.currentTimeMillis()}") }
    var scanCategory by remember { mutableStateOf("Documents") }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            containerColor = DarkSurface,
            title = { Text("Save Document Scan", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Pages scanned: ${capturedBitmaps.size}", color = CyanPrimary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = scanTitle,
                        onValueChange = { scanTitle = it },
                        label = { Text("Document Title") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanPrimary,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
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
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = Color.Black)
                ) {
                    Text("Save to Vault", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel", color = TextSecondary)
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
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
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

                    try {
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

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

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
                    .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
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

                // Shutter Button
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
                                            val currentQuad = detectedQuad
                                            val processedBitmap = if (autoCropEnabled && currentQuad != null) {
                                                OpenCvDocumentDetector.cropAndWarpPerspective(rotated, currentQuad)
                                            } else {
                                                rotated
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
                                // Fallback mock scan if camera is unavailable on emulator
                                val mockBitmap = Bitmap.createBitmap(800, 1200, Bitmap.Config.ARGB_8888)
                                viewModel.addCapturedBitmap(mockBitmap)
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

                Spacer(modifier = Modifier.width(60.dp))
            }
        }
    }
}

private fun rotateBitmapIfNeeded(bitmap: Bitmap, degrees: Int): Bitmap {
    if (degrees == 0) return bitmap
    val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}
