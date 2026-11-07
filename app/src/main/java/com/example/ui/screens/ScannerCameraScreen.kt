package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaActionSound
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.data.local.ScannedDocument
import com.example.ui.ScanProViewModel
import com.example.ui.camera.CameraPreferences
import com.example.ui.camera.CameraScanResolution
import com.example.ui.camera.CameraSettingsManager
import com.example.ui.camera.FlashOption
import com.example.ui.camera.ScanCaptureMode
import com.example.ui.theme.CyanPrimary
import com.example.util.DocumentFilterMode
import com.example.util.DocumentQuad
import com.example.util.OcrPdfMode
import com.example.util.OpenCvDocumentDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class AllFeaturesGridItem(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val badgeColor: Color,
    val category: String,
    val isHot: Boolean = false
)

@Composable
fun ScannerCameraScreen(
    viewModel: ScanProViewModel,
    onCloseScanner: () -> Unit,
    onNavigateToEditor: () -> Unit,
    onOpenToolDedicated: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    val settingsManager = remember { CameraSettingsManager(context) }
    var cameraPrefs by remember { mutableStateOf(settingsManager.load()) }

    val capturedBitmaps by viewModel.capturedBitmaps.collectAsState()

    var captureMode by remember { mutableStateOf(ScanCaptureMode.SINGLE) }
    var selectedRibbonMode by remember { mutableStateOf("Scan") }

    // Overlays
    var showAllFeaturesSheet by remember { mutableStateOf(false) }
    var showFlashMenu by remember { mutableStateOf(false) }
    var showEnhancementPanel by remember { mutableStateOf(false) }
    var showResolutionPicker by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    var selectedFilterMode by remember { mutableStateOf(DocumentFilterMode.ORIGINAL) }
    var detectedQuad by remember { mutableStateOf<DocumentQuad?>(null) }

    // CameraX instance management
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    var previewViewInstance by remember { mutableStateOf<PreviewView?>(null) }
    var cameraProviderInstance by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var isCapturing by remember { mutableStateOf(false) }

    // Focus & Zoom
    var focusTapOffset by remember { mutableStateOf<Offset?>(null) }
    var zoomScale by remember { mutableFloatStateOf(1f) }

    val mediaActionSound = remember { MediaActionSound().apply { load(MediaActionSound.SHUTTER_CLICK) } }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
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

    BackHandler(enabled = showAllFeaturesSheet || showEnhancementPanel || showResolutionPicker || showSettingsDialog) {
        when {
            showAllFeaturesSheet -> showAllFeaturesSheet = false
            showEnhancementPanel -> showEnhancementPanel = false
            showResolutionPicker -> showResolutionPicker = false
            showSettingsDialog -> showSettingsDialog = false
        }
    }

    LaunchedEffect(cameraPrefs.flashOption, boundCamera) {
        val cam = boundCamera ?: return@LaunchedEffect
        if (cam.cameraInfo.hasFlashUnit()) {
            when (cameraPrefs.flashOption) {
                FlashOption.STAY_ON -> cam.cameraControl.enableTorch(true)
                FlashOption.OFF, FlashOption.ON, FlashOption.AUTO -> cam.cameraControl.enableTorch(false)
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            try {
                cameraProviderInstance?.unbindAll()
                mediaActionSound.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LaunchedEffect(focusTapOffset) {
        if (focusTapOffset != null) {
            delay(1600)
            focusTapOffset = null
        }
    }

    val imageImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            coroutineScope.launch(Dispatchers.IO) {
                val imported = uris.mapNotNull { uri ->
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                }
                withContext(Dispatchers.Main) {
                    imported.forEach { bmp -> viewModel.addCapturedBitmap(bmp) }
                    Toast.makeText(context, "Imported ${imported.size} image(s)", Toast.LENGTH_SHORT).show()
                    onNavigateToEditor()
                }
            }
        }
    }

    val fileImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            coroutineScope.launch(Dispatchers.IO) {
                var importedCount = 0
                uris.forEach { uri ->
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val docsDir = File(context.filesDir, "imported").apply { if (!exists()) mkdirs() }
                    val destFile = File(docsDir, "Doc_${System.currentTimeMillis()}.pdf")
                    FileOutputStream(destFile).use { out -> inputStream?.copyTo(out) }
                    val doc = ScannedDocument(
                        title = destFile.name,
                        filePath = destFile.absolutePath,
                        fileType = "PDF",
                        fileSizeBytes = destFile.length(),
                        pageCount = 1,
                        category = "Imported"
                    )
                    viewModel.addNewDocument(doc)
                    importedCount++
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Imported $importedCount document(s)", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. TOP TOOLBAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onCloseScanner() }
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0F1E2A))
                            .border(1.5.dp, Color(0xFF16C7B5), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CropFree,
                            contentDescription = "Logo",
                            tint = Color(0xFF16C7B5),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ScanPro ",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "AI",
                        color = Color(0xFF16C7B5),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF16C7B5))
                            .clickable { showResolutionPicker = true }
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cameraPrefs.resolution.label,
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    IconButton(
                        onClick = { showFlashMenu = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        val flashIcon = when (cameraPrefs.flashOption) {
                            FlashOption.OFF -> Icons.Default.FlashOff
                            FlashOption.ON -> Icons.Default.FlashOn
                            FlashOption.AUTO -> Icons.Default.FlashAuto
                            FlashOption.STAY_ON -> Icons.Default.FlashOn
                        }
                        val flashTint = if (cameraPrefs.flashOption == FlashOption.OFF) Color.White else Color(0xFF16C7B5)
                        Icon(flashIcon, contentDescription = "Flashlight", tint = flashTint, modifier = Modifier.size(22.dp))
                    }

                    IconButton(
                        onClick = { showEnhancementPanel = !showEnhancementPanel },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Filters",
                            tint = if (showEnhancementPanel) Color(0xFF16C7B5) else Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Settings", tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }
            }

            // 2. CAMERA PREVIEW VIEWPORT
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, Color(0xFF16C7B5).copy(alpha = 0.4f), RoundedCornerShape(18.dp))
            ) {
                if (hasCameraPermission) {
                    val transformableState = rememberTransformableState { zoomChange, _, _ ->
                        zoomScale = (zoomScale * zoomChange).coerceIn(1f, 5f)
                        boundCamera?.cameraControl?.setZoomRatio(zoomScale)
                    }

                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx).apply {
                                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            }
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                try {
                                    val cameraProvider = cameraProviderFuture.get()
                                    cameraProviderInstance = cameraProvider

                                    val preview = Preview.Builder().build().also {
                                        it.setSurfaceProvider(previewView.surfaceProvider)
                                    }

                                    val capture = ImageCapture.Builder()
                                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                                        .setResolutionSelector(settingsManager.buildResolutionSelector(cameraPrefs.resolution))
                                        .setFlashMode(
                                            when (cameraPrefs.flashOption) {
                                                FlashOption.ON -> ImageCapture.FLASH_MODE_ON
                                                FlashOption.AUTO -> ImageCapture.FLASH_MODE_AUTO
                                                else -> ImageCapture.FLASH_MODE_OFF
                                            }
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

                            previewView
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .transformable(state = transformableState)
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Camera Permission Required", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                if (cameraPrefs.showGrid) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val gridPaintColor = Color.White.copy(alpha = 0.25f)
                        drawLine(gridPaintColor, Offset(w / 3f, 0f), Offset(w / 3f, h), strokeWidth = 1.5f)
                        drawLine(gridPaintColor, Offset(2 * w / 3f, 0f), Offset(2 * w / 3f, h), strokeWidth = 1.5f)
                        drawLine(gridPaintColor, Offset(0f, h / 3f), Offset(w, h / 3f), strokeWidth = 1.5f)
                        drawLine(gridPaintColor, Offset(0f, 2 * h / 3f), Offset(w, 2 * h / 3f), strokeWidth = 1.5f)
                    }
                }

                Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                    val w = size.width
                    val h = size.height
                    val cornerLen = 42f
                    val strokeW = 6f
                    val cornerColor = Color(0xFF16C7B5)

                    drawLine(cornerColor, Offset(0f, 0f), Offset(cornerLen, 0f), strokeWidth = strokeW)
                    drawLine(cornerColor, Offset(0f, 0f), Offset(0f, cornerLen), strokeWidth = strokeW)

                    drawLine(cornerColor, Offset(w, 0f), Offset(w - cornerLen, 0f), strokeWidth = strokeW)
                    drawLine(cornerColor, Offset(w, 0f), Offset(w, cornerLen), strokeWidth = strokeW)

                    drawLine(cornerColor, Offset(w, h), Offset(w - cornerLen, h), strokeWidth = strokeW)
                    drawLine(cornerColor, Offset(w, h), Offset(w, h - cornerLen), strokeWidth = strokeW)

                    drawLine(cornerColor, Offset(0f, h), Offset(cornerLen, h), strokeWidth = strokeW)
                    drawLine(cornerColor, Offset(0f, h), Offset(0f, h - cornerLen), strokeWidth = strokeW)
                }

                val infiniteScan = rememberInfiniteTransition(label = "scan_laser")
                val scanLineY = infiniteScan.animateFloat(
                    initialValue = -35f,
                    targetValue = 35f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1400, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "laser_y"
                )

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier.size(100.dp)) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val arm = 22f
                            val stroke = 3.5f
                            val reticleColor = Color.White.copy(alpha = 0.9f)

                            drawLine(reticleColor, Offset(0f, 0f), Offset(arm, 0f), strokeWidth = stroke)
                            drawLine(reticleColor, Offset(0f, 0f), Offset(0f, arm), strokeWidth = stroke)

                            drawLine(reticleColor, Offset(w, 0f), Offset(w - arm, 0f), strokeWidth = stroke)
                            drawLine(reticleColor, Offset(w, 0f), Offset(w, arm), strokeWidth = stroke)

                            drawLine(reticleColor, Offset(w, h), Offset(w - arm, h), strokeWidth = stroke)
                            drawLine(reticleColor, Offset(w, h), Offset(w, h - arm), strokeWidth = stroke)

                            drawLine(reticleColor, Offset(0f, h), Offset(arm, h), strokeWidth = stroke)
                            drawLine(reticleColor, Offset(0f, h), Offset(0f, h - arm), strokeWidth = stroke)

                            val cy = h / 2f + scanLineY.value
                            drawLine(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color.Transparent, Color(0xFF16C7B5), Color.White, Color(0xFF16C7B5), Color.Transparent)
                                ),
                                start = Offset(0f, cy),
                                end = Offset(w, cy),
                                strokeWidth = 3f
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                focusTapOffset = offset
                                previewViewInstance?.let { pView ->
                                    boundCamera?.let { cam ->
                                        val point = pView.meteringPointFactory.createPoint(offset.x, offset.y)
                                        val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
                                            .setAutoCancelDuration(3, java.util.concurrent.TimeUnit.SECONDS)
                                            .build()
                                        cam.cameraControl.startFocusAndMetering(action)
                                    }
                                }
                            }
                        }
                )

                focusTapOffset?.let { tap ->
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(Color(0xFF16C7B5), radius = 42f, center = tap, style = Stroke(width = 3f))
                        drawCircle(Color(0xFF16C7B5), radius = 5f, center = tap)
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.8f))
                        .border(1.dp, Color(0xFF16C7B5).copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 4.dp, vertical = 3.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val isSingle = captureMode == ScanCaptureMode.SINGLE
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSingle) Color(0xFF16C7B5) else Color.Transparent)
                                .clickable { captureMode = ScanCaptureMode.SINGLE }
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Single",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSingle) Color.Black else Color.White
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (!isSingle) Color(0xFF16C7B5) else Color.Transparent)
                                .clickable { captureMode = ScanCaptureMode.BATCH }
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Batch",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (!isSingle) Color.Black else Color.White
                            )
                        }
                    }
                }
            }

            // 3. PERSISTENT ALL FEATURES TRIGGER BUTTON
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0F1E2A))
                    .border(1.dp, Color(0xFF16C7B5).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .clickable { showAllFeaturesSheet = true }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "All Features",
                        tint = Color(0xFF16C7B5),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "All Features",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = Color(0xFF16C7B5),
                    modifier = Modifier.size(18.dp)
                )
            }

            // 4. BOTTOM CAMERA CONTROLS DOCK
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(Color(0xFF070B12))
                    .border(1.5.dp, Color(0xFF16C7B5).copy(alpha = 0.5f), RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .padding(top = 10.dp, bottom = 4.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { onCloseScanner() }
                        ) {
                            Icon(Icons.Default.History, contentDescription = "History", tint = Color.White, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.height(3.dp))
                            Text("History", color = Color.White, fontSize = 11.sp)
                        }

                        // Shutter Button (Navigates directly to DocumentEditorScreen)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(78.dp)
                                .clip(CircleShape)
                                .border(3.dp, Color(0xFF16C7B5), CircleShape)
                                .clickable {
                                    if (isCapturing) return@clickable
                                    val capture = imageCapture
                                    if (capture != null) {
                                        isCapturing = true
                                        if (cameraPrefs.shutterSound) {
                                            runCatching { mediaActionSound.play(MediaActionSound.SHUTTER_CLICK) }
                                        }

                                        capture.takePicture(
                                            ContextCompat.getMainExecutor(context),
                                            object : ImageCapture.OnImageCapturedCallback() {
                                                override fun onCaptureSuccess(image: ImageProxy) {
                                                    val buffer = image.planes[0].buffer
                                                    val bytes = ByteArray(buffer.remaining())
                                                    buffer.get(bytes)
                                                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                                    val rotated = rotateBitmapIfNeeded(bitmap, image.imageInfo.rotationDegrees)

                                                    val processedBitmap = if (cameraPrefs.autoCrop) {
                                                        val detected = OpenCvDocumentDetector.detectDocumentEdges(rotated)
                                                        val quadToUse = if (detected.confidence >= 0.82f) detected else (detectedQuad ?: detected)
                                                        val cropped = OpenCvDocumentDetector.cropAndWarpPerspective(rotated, quadToUse)
                                                        OpenCvDocumentDetector.applyFilter(cropped, selectedFilterMode)
                                                    } else {
                                                        OpenCvDocumentDetector.applyFilter(rotated, selectedFilterMode)
                                                    }

                                                    viewModel.addCapturedBitmap(processedBitmap)
                                                    image.close()
                                                    isCapturing = false

                                                    onNavigateToEditor()
                                                }

                                                override fun onError(exception: ImageCaptureException) {
                                                    exception.printStackTrace()
                                                    isCapturing = false
                                                    Toast.makeText(context, "Capture failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        )
                                    } else {
                                        val mockBitmap = createMockDocumentBitmap()
                                        val filtered = OpenCvDocumentDetector.applyFilter(mockBitmap, selectedFilterMode)
                                        viewModel.addCapturedBitmap(filtered)
                                        onNavigateToEditor()
                                    }
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(62.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            ) {
                                if (isCapturing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.fillMaxSize(),
                                        color = Color(0xFF16C7B5),
                                        strokeWidth = 3.dp
                                    )
                                }
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { imageImportLauncher.launch("image/*") }
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = "Import Images", tint = Color.White, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.height(3.dp))
                            Text("Import\nImages", color = Color.White, fontSize = 10.sp, textAlign = TextAlign.Center)
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable {
                                fileImportLauncher.launch(
                                    arrayOf("application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "text/plain")
                                )
                            }
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = "Import Files", tint = Color.White, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.height(3.dp))
                            Text("Import\nFiles", color = Color.White, fontSize = 10.sp, textAlign = TextAlign.Center)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val ribbonModes = listOf("Extract Text", "To Word", "Sign", "Scan", "Smart Erase", "ID Cards", "Question")
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(ribbonModes) { mode ->
                            val isSelected = selectedRibbonMode == mode
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable {
                                        selectedRibbonMode = mode
                                        when (mode) {
                                            "Extract Text" -> onOpenToolDedicated?.invoke("extract_text")
                                            "To Word" -> onOpenToolDedicated?.invoke("pdf_to_word")
                                            "Sign" -> onOpenToolDedicated?.invoke("sign")
                                            "ID Cards" -> onOpenToolDedicated?.invoke("id_card_scan")
                                            "Smart Erase" -> onOpenToolDedicated?.invoke("smart_eraser")
                                            "Question" -> onOpenToolDedicated?.invoke("ocr")
                                            "Scan" -> Unit
                                        }
                                    }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = mode,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color(0xFF16C7B5) else Color.White.copy(alpha = 0.7f)
                                )
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 2.dp)
                                            .height(2.5.dp)
                                            .width(24.dp)
                                            .background(Color(0xFF16C7B5), RoundedCornerShape(2.dp))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. ALL FEATURES EXPANDABLE BOTTOM SHEET
        AnimatedVisibility(
            visible = showAllFeaturesSheet,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            AllFeaturesBottomSheet(
                onDismiss = { showAllFeaturesSheet = false },
                onSelectFeature = { featureId ->
                    showAllFeaturesSheet = false
                    when (featureId) {
                        "scan" -> Unit
                        "id_cards" -> onOpenToolDedicated?.invoke("id_card_scan")
                        "book" -> onOpenToolDedicated?.invoke("book_scan")
                        "timestamp" -> onOpenToolDedicated?.invoke("timestamp")
                        "whiteboard" -> onOpenToolDedicated?.invoke("whiteboard")
                        "slides" -> onOpenToolDedicated?.invoke("slides")
                        "to_word" -> onOpenToolDedicated?.invoke("pdf_to_word")
                        "to_excel" -> onOpenToolDedicated?.invoke("pdf_to_excel")
                        "sign" -> onOpenToolDedicated?.invoke("sign")
                        "smart_eraser" -> onOpenToolDedicated?.invoke("smart_eraser")
                        "question_set" -> onOpenToolDedicated?.invoke("ocr")
                        "translate" -> onOpenToolDedicated?.invoke("ocr")
                        "restore_photo" -> onOpenToolDedicated?.invoke("smart_eraser")
                        "scan_code" -> onOpenToolDedicated?.invoke("qr_scan")
                        "count_cam" -> onOpenToolDedicated?.invoke("count_cam")
                        "id_photo_maker" -> onOpenToolDedicated?.invoke("id_photo_maker")
                        "greeting_card" -> Toast.makeText(context, "Greeting card template mode", Toast.LENGTH_SHORT).show()
                        "ai_chat" -> onOpenToolDedicated?.invoke("ai_chat")
                        else -> onOpenToolDedicated?.invoke(featureId)
                    }
                }
            )
        }

        // 6. FLASH MENU POPUP DIALOG
        if (showFlashMenu) {
            AlertDialog(
                onDismissRequest = { showFlashMenu = false },
                containerColor = Color(0xFF0F172A),
                title = { Text("Flashlight Mode", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FlashOption.values().forEach { opt ->
                            val isSel = cameraPrefs.flashOption == opt
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSel) Color(0xFF16C7B5).copy(alpha = 0.2f) else Color.Transparent)
                                    .border(1.dp, if (isSel) Color(0xFF16C7B5) else Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                                    .clickable {
                                        val updated = cameraPrefs.copy(flashOption = opt)
                                        cameraPrefs = updated
                                        settingsManager.save(updated)
                                        showFlashMenu = false
                                    }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(opt.label, color = if (isSel) Color(0xFF16C7B5) else Color.White, fontWeight = FontWeight.Bold)
                                if (isSel) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF16C7B5))
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showFlashMenu = false }) { Text("Close", color = Color(0xFF16C7B5)) }
                }
            )
        }

        // 7. RESOLUTION SELECTOR (HD) DIALOG
        if (showResolutionPicker) {
            AlertDialog(
                onDismissRequest = { showResolutionPicker = false },
                containerColor = Color(0xFF0F172A),
                title = { Text("Camera Capture Resolution", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CameraScanResolution.values().forEach { res ->
                            val isSel = cameraPrefs.resolution == res
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSel) Color(0xFF16C7B5).copy(alpha = 0.2f) else Color.Transparent)
                                    .border(1.dp, if (isSel) Color(0xFF16C7B5) else Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                                    .clickable {
                                        val updated = cameraPrefs.copy(resolution = res)
                                        cameraPrefs = updated
                                        settingsManager.save(updated)
                                        showResolutionPicker = false
                                        Toast.makeText(context, "Resolution set to ${res.label} (${res.approxWidth}x${res.approxHeight})", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(res.label, color = if (isSel) Color(0xFF16C7B5) else Color.White, fontWeight = FontWeight.Bold)
                                    Text("${res.approxWidth} x ${res.approxHeight} px", color = Color.Gray, fontSize = 11.sp)
                                }
                                if (isSel) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF16C7B5))
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showResolutionPicker = false }) { Text("Close", color = Color(0xFF16C7B5)) }
                }
            )
        }

        // 8. IMAGE ENHANCEMENT PANEL OVERLAY
        AnimatedVisibility(
            visible = showEnhancementPanel,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 60.dp, start = 14.dp, end = 14.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0F172A).copy(alpha = 0.95f))
                    .border(1.dp, Color(0xFF16C7B5).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Image Enhancement Filters", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    IconButton(onClick = { showEnhancementPanel = false }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val filterPairs = listOf(
                        DocumentFilterMode.ORIGINAL to "Original",
                        DocumentFilterMode.MAGIC_COLOR to "Magic Pro",
                        DocumentFilterMode.BLACK_AND_WHITE to "B&W Clean",
                        DocumentFilterMode.GRAYSCALE to "Grayscale"
                    )
                    items(filterPairs) { (mode, label) ->
                        val isSel = selectedFilterMode == mode
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) Color(0xFF16C7B5) else Color(0xFF1E293B))
                                .clickable {
                                    selectedFilterMode = mode
                                    Toast.makeText(context, "Applied $label filter", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(label, color = if (isSel) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 9. THREE-DOT SETTINGS MENU DIALOG
        if (showSettingsDialog) {
            AlertDialog(
                onDismissRequest = { showSettingsDialog = false },
                containerColor = Color(0xFF0F172A),
                title = { Text("Camera & Scanner Settings", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("OpenCV Auto Crop", color = Color.White)
                            Switch(
                                checked = cameraPrefs.autoCrop,
                                onCheckedChange = {
                                    val u = cameraPrefs.copy(autoCrop = it)
                                    cameraPrefs = u; settingsManager.save(u)
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF16C7B5))
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Rule of Thirds Grid", color = Color.White)
                            Switch(
                                checked = cameraPrefs.showGrid,
                                onCheckedChange = {
                                    val u = cameraPrefs.copy(showGrid = it)
                                    cameraPrefs = u; settingsManager.save(u)
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF16C7B5))
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Shutter Audio Sound", color = Color.White)
                            Switch(
                                checked = cameraPrefs.shutterSound,
                                onCheckedChange = {
                                    val u = cameraPrefs.copy(shutterSound = it)
                                    cameraPrefs = u; settingsManager.save(u)
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF16C7B5))
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Auto Document Capture", color = Color.White)
                            Switch(
                                checked = cameraPrefs.autoCapture,
                                onCheckedChange = {
                                    val u = cameraPrefs.copy(autoCapture = it)
                                    cameraPrefs = u; settingsManager.save(u)
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF16C7B5))
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSettingsDialog = false }) { Text("Save & Close", color = Color(0xFF16C7B5), fontWeight = FontWeight.Bold) }
                }
            )
        }
    }
}

@Composable
fun AllFeaturesBottomSheet(
    onDismiss: () -> Unit,
    onSelectFeature: (String) -> Unit
) {
    val items = listOf(
        AllFeaturesGridItem("slides", "Slides", Icons.Default.Slideshow, Color(0xFF2563EB), "Scan Documents"),
        AllFeaturesGridItem("to_excel", "To Excel", Icons.Default.TableChart, Color(0xFF10B981), "Scan Documents"),
        AllFeaturesGridItem("id_photo_maker", "Id Photo\nMaker", Icons.Default.Badge, Color(0xFFA855F7), "Scan Documents"),
        AllFeaturesGridItem("timestamp", "Timestamp", Icons.Default.Timer, Color(0xFFF97316), "Scan Documents"),

        AllFeaturesGridItem("to_word", "To Word", Icons.Default.Description, Color(0xFF2563EB), "Convert Formats"),
        AllFeaturesGridItem("to_excel", "To Excel", Icons.Default.TableChart, Color(0xFF10B981), "Convert Formats"),

        AllFeaturesGridItem("sign", "Sign", Icons.Default.Draw, Color(0xFFEC4899), "Doc Tools"),
        AllFeaturesGridItem("scan", "Scan", Icons.Default.CropFree, Color(0xFF16C7B5), "Doc Tools"),
        AllFeaturesGridItem("smart_eraser", "Smart\nEraser", Icons.Default.AutoAwesome, Color(0xFF10B981), "Doc Tools"),
        AllFeaturesGridItem("id_cards", "ID Cards", Icons.Default.Badge, Color(0xFF0EA5E9), "Doc Tools"),

        AllFeaturesGridItem("question_set", "Question Set", Icons.Default.QuestionAnswer, Color(0xFFA855F7), "Enhance Images"),
        AllFeaturesGridItem("translate", "Translate", Icons.Default.Translate, Color(0xFF16C7B5), "Enhance Images"),
        AllFeaturesGridItem("restore_photo", "Restore Photo", Icons.Default.Image, Color(0xFFF97316), "Enhance Images"),
        AllFeaturesGridItem("book", "Book", Icons.Default.Book, Color(0xFF0EA5E9), "Enhance Images"),

        AllFeaturesGridItem("whiteboard", "Whiteboard", Icons.Default.Tv, Color(0xFF10B981), "Capture Screens"),
        AllFeaturesGridItem("scan_code", "Scan Code", Icons.Default.QrCodeScanner, Color(0xFF16C7B5), "Capture Screens"),

        AllFeaturesGridItem("count_cam", "CountCam", Icons.Default.CenterFocusStrong, Color(0xFFA855F7), "More Tools"),
        AllFeaturesGridItem("greeting_card", "Greeting Card", Icons.Default.Star, Color(0xFF3B82F6), "More Tools"),
        AllFeaturesGridItem("ai_chat", "AI Chat", Icons.Default.AutoAwesome, Color(0xFFA855F7), "More Tools", isHot = true)
    )

    val categories = items.map { it.category }.distinct()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxSize(0.78f)
                .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                .background(Color(0xFF060B12))
                .border(1.5.dp, Color(0xFF16C7B5).copy(alpha = 0.6f), RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                .clickable(enabled = false) {}
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFF16C7B5), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("All Features", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                categories.forEach { category ->
                    Text(
                        text = category,
                        color = Color(0xFF16C7B5),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    val catItems = items.filter { it.category == category }
                    val rows = catItems.chunked(4)

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        rows.forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowItems.forEach { item ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1.05f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFF0D1724))
                                            .border(1.dp, Color(0xFF16C7B5).copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                            .clickable { onSelectFeature(item.id) }
                                            .padding(6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = item.icon,
                                                contentDescription = item.title,
                                                tint = item.badgeColor,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = item.title,
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                textAlign = TextAlign.Center,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                                repeat(4 - rowItems.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

private fun rotateBitmapIfNeeded(bitmap: Bitmap, degrees: Int): Bitmap {
    if (degrees == 0) return bitmap
    val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

private fun createMockDocumentBitmap(): Bitmap {
    val bmp = Bitmap.createBitmap(900, 1200, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)
    canvas.drawColor(android.graphics.Color.rgb(25, 35, 48))
    val paperPaint = android.graphics.Paint().apply { color = android.graphics.Color.WHITE }
    canvas.drawRect(80f, 90f, 820f, 1110f, paperPaint)
    val textPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 32f
        isFakeBoldText = true
    }
    canvas.drawText("SCANPRO AI DOCUMENT RECORD", 120f, 180f, textPaint)
    textPaint.textSize = 20f
    textPaint.isFakeBoldText = false
    canvas.drawText("Invoice ID: #INV-2026-X89", 120f, 230f, textPaint)
    canvas.drawText("Date: November 07, 2026", 120f, 270f, textPaint)
    canvas.drawText("Status: Authenticated & Encrypted in Room DB", 120f, 310f, textPaint)
    return bmp
}
