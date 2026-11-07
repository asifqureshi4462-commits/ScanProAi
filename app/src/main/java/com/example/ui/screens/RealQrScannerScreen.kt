package com.example.ui.screens

import android.Manifest
import android.annotation.OptIn
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@Composable
fun RealQrScannerScreen(
    onBackClicked: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var scannedResult by remember { mutableStateOf<String?>(null) }
    var scannedFormat by remember { mutableStateOf("QR Code") }
    var isFlashOn by remember { mutableStateOf(false) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    var cameraProviderInstance by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    BackHandler {
        onBackClicked()
    }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            cameraProviderInstance?.unbindAll()
        }
    }

    fun vibratePhone() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(80)
            }
        } catch (_: Exception) {}
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    }
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    val cameraExecutor = Executors.newSingleThreadExecutor()

                    val options = BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(
                            Barcode.FORMAT_QR_CODE,
                            Barcode.FORMAT_DATA_MATRIX,
                            Barcode.FORMAT_UPC_A,
                            Barcode.FORMAT_UPC_E,
                            Barcode.FORMAT_EAN_8,
                            Barcode.FORMAT_EAN_13,
                            Barcode.FORMAT_CODE_128,
                            Barcode.FORMAT_CODE_39
                        )
                        .build()
                    val barcodeScanner = BarcodeScanning.getClient(options)

                    cameraProviderFuture.addListener({
                        try {
                            val provider = cameraProviderFuture.get()
                            cameraProviderInstance = provider

                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            val analysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                                .also { analyzer ->
                                    analyzer.setAnalyzer(cameraExecutor) { imageProxy ->
                                        processImageProxy(barcodeScanner, imageProxy) { barcodeValue, formatLabel ->
                                            if (scannedResult == null) {
                                                vibratePhone()
                                                scannedResult = barcodeValue
                                                scannedFormat = formatLabel
                                            }
                                        }
                                    }
                                }

                            provider.unbindAll()
                            val cam = provider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                analysis
                            )
                            boundCamera = cam
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F172A))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = Color(0xFF16C7B5),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        "Camera permission is required to scan QR & Barcodes.",
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16C7B5), contentColor = Color.Black)
                    ) {
                        Text("Grant Camera Permission", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Animated Laser Line in Center Viewfinder Frame
        val infiniteTransition = rememberInfiniteTransition(label = "scanner_laser")
        val laserOffset by infiniteTransition.animateFloat(
            initialValue = -110f,
            targetValue = 110f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "laser_y"
        )

        // Viewfinder Target Frame
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 60.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .clip(RoundedCornerShape(20.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val cornerLength = 36f
                    val stroke = 6f
                    val cornerColor = Color(0xFF16C7B5)

                    // Corner Brackets
                    drawLine(cornerColor, Offset(0f, 0f), Offset(cornerLength, 0f), strokeWidth = stroke)
                    drawLine(cornerColor, Offset(0f, 0f), Offset(0f, cornerLength), strokeWidth = stroke)

                    drawLine(cornerColor, Offset(w, 0f), Offset(w - cornerLength, 0f), strokeWidth = stroke)
                    drawLine(cornerColor, Offset(w, 0f), Offset(w, cornerLength), strokeWidth = stroke)

                    drawLine(cornerColor, Offset(0f, h), Offset(cornerLength, h), strokeWidth = stroke)
                    drawLine(cornerColor, Offset(0f, h), Offset(0f, h - cornerLength), strokeWidth = stroke)

                    drawLine(cornerColor, Offset(w, h), Offset(w - cornerLength, h), strokeWidth = stroke)
                    drawLine(cornerColor, Offset(w, h), Offset(w, h - cornerLength), strokeWidth = stroke)

                    // Animated Scanning Laser
                    val centerY = h / 2f + laserOffset
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, Color(0xFF16C7B5), Color.White, Color(0xFF16C7B5), Color.Transparent)
                        ),
                        start = Offset(0f, centerY),
                        end = Offset(w, centerY),
                        strokeWidth = 3.5f
                    )
                }
            }
        }

        // Top Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClicked,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Text(
                text = "QR & Barcode Scanner",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )

            IconButton(
                onClick = {
                    val cam = boundCamera
                    if (cam != null && cam.cameraInfo.hasFlashUnit()) {
                        isFlashOn = !isFlashOn
                        cam.cameraControl.enableTorch(isFlashOn)
                    } else {
                        Toast.makeText(context, "Flashlight unavailable", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
            ) {
                Icon(
                    imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = "Flashlight",
                    tint = if (isFlashOn) Color(0xFF16C7B5) else Color.White
                )
            }
        }

        // Bottom Result Card (Always within mobile screen bounds, never cut off)
        AnimatedVisibility(
            visible = scannedResult != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        ) {
            val resultText = scannedResult ?: ""
            val isWebUrl = resultText.startsWith("http://", ignoreCase = true) || resultText.startsWith("https://", ignoreCase = true)

            Card(
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1724)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, Color(0xFF16C7B5).copy(alpha = 0.5f), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SCANNED $scannedFormat".uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF16C7B5)
                            )
                        }

                        IconButton(
                            onClick = { scannedResult = null },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Scan Again", tint = Color.Gray, modifier = Modifier.size(20.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Safe scrollable box for large content without screen overflow
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 120.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E293B))
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp)
                    ) {
                        Text(
                            text = resultText,
                            fontSize = 14.sp,
                            color = Color.White,
                            lineHeight = 20.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action Buttons (Copy, Open URL, Share)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Scanned Code", resultText))
                                Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B), contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        if (isWebUrl) {
                            Button(
                                onClick = {
                                    try {
                                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(resultText)).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        context.startActivity(browserIntent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Could not open link: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16C7B5), contentColor = Color.Black),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1.3f)
                            ) {
                                Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Open Link", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, resultText)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Scanned Result"))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B), contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalGetImage::class)
private fun processImageProxy(
    barcodeScanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    imageProxy: ImageProxy,
    onSuccess: (String, String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        barcodeScanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                val firstBarcode = barcodes.firstOrNull()
                if (firstBarcode != null) {
                    val rawVal = firstBarcode.rawValue ?: firstBarcode.displayValue
                    if (!rawVal.isNullOrBlank()) {
                        val formatName = when (firstBarcode.format) {
                            Barcode.FORMAT_QR_CODE -> "QR Code"
                            Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8 -> "EAN Barcode"
                            Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E -> "UPC Barcode"
                            Barcode.FORMAT_CODE_128, Barcode.FORMAT_CODE_39 -> "Standard Barcode"
                            else -> "Code"
                        }
                        onSuccess(rawVal, formatName)
                    }
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}
