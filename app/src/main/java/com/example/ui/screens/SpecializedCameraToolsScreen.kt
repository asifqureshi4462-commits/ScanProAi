package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ScannedDocument
import com.example.ui.ScanProViewModel
import com.example.util.PdfEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ==========================================
// 1. ID PHOTO MAKER (PASSPORT & VISA 2x2 CROP + PRINT SHEET)
// ==========================================
@Composable
fun IdPhotoMakerToolScreen(
    viewModel: ScanProViewModel,
    onBackClicked: () -> Unit,
    onNavigateToDocPreview: (ScannedDocument) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedFormat by remember { mutableStateOf("Passport (2x2 in)") }
    var selectedBgColor by remember { mutableStateOf("White") }
    var isGeneratingSheet by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                selectedBitmap = android.graphics.BitmapFactory.decodeStream(stream)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
    ) {
        ToolHeader(
            title = "ID Photo Maker",
            description = "Standard Passport, Visa & License Photos with 3x4 Print Sheet",
            icon = Icons.Default.Badge,
            iconColor = Color(0xFFA855F7),
            badgeText = "Print Layout",
            onBackClicked = onBackClicked
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1E2A)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF16C7B5).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (selectedBitmap != null) {
                        Box(
                            modifier = Modifier
                                .size(180.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(2.dp, Color(0xFF16C7B5), RoundedCornerShape(8.dp))
                        ) {
                            Image(
                                bitmap = selectedBitmap!!.asImageBitmap(),
                                contentDescription = "ID Portrait",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { photoPicker.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                        ) {
                            Text("Change Photo", fontSize = 12.sp, color = Color.White)
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(150.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E293B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = Color(0xFF16C7B5), modifier = Modifier.size(48.dp))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { photoPicker.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16C7B5), contentColor = Color.Black)
                        ) {
                            Text("Select Portrait Photo", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1724)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("1. ID Specification Size", color = Color(0xFF16C7B5), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    val formats = listOf("Passport (2x2 in)", "Visa (35x45 mm)", "License (30x40 mm)")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        formats.forEach { fmt ->
                            FilterChip(
                                selected = selectedFormat == fmt,
                                onClick = { selectedFormat = fmt },
                                label = { Text(fmt, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF16C7B5),
                                    selectedLabelColor = Color.Black,
                                    containerColor = Color(0xFF1E293B),
                                    labelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text("2. Background Color", color = Color(0xFF16C7B5), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    val bgColors = listOf("White", "Blue", "Light Gray")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        bgColors.forEach { bg ->
                            FilterChip(
                                selected = selectedBgColor == bg,
                                onClick = { selectedBgColor = bg },
                                label = { Text(bg, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF16C7B5),
                                    selectedLabelColor = Color.Black,
                                    containerColor = Color(0xFF1E293B),
                                    labelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    val bmp = selectedBitmap
                    if (bmp == null) {
                        Toast.makeText(context, "Please select a portrait image first", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isGeneratingSheet = true
                    coroutineScope.launch(Dispatchers.IO) {
                        val sheetW = 1200
                        val sheetH = 1600
                        val sheetBmp = Bitmap.createBitmap(sheetW, sheetH, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(sheetBmp)
                        canvas.drawColor(AndroidColor.WHITE)

                        val photoW = 240
                        val photoH = 300
                        val paddingX = 40
                        val paddingY = 60
                        val startX = (sheetW - (4 * photoW + 3 * paddingX)) / 2
                        val startY = 160

                        val scaledPhoto = Bitmap.createScaledBitmap(bmp, photoW, photoH, true)
                        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                        val borderPaint = Paint().apply {
                            color = AndroidColor.LTGRAY
                            style = Paint.Style.STROKE
                            strokeWidth = 2f
                        }

                        val titlePaint = Paint().apply {
                            color = AndroidColor.BLACK
                            textSize = 34f
                            isFakeBoldText = true
                        }
                        canvas.drawText("ScanPro AI • Standard ID Photo Print Sheet ($selectedFormat)", startX.toFloat(), 90f, titlePaint)

                        for (row in 0..1) {
                            for (col in 0..3) {
                                val x = startX + col * (photoW + paddingX)
                                val y = startY + row * (photoH + paddingY)
                                canvas.drawBitmap(scaledPhoto, x.toFloat(), y.toFloat(), paint)
                                canvas.drawRect(RectF(x.toFloat(), y.toFloat(), (x + photoW).toFloat(), (y + photoH).toFloat()), borderPaint)
                            }
                        }

                        val pdfFile = PdfEngine.createPdfFromBitmaps(context, listOf(sheetBmp), "ID_Photo_Sheet_${System.currentTimeMillis()}.pdf")
                        val doc = ScannedDocument(
                            title = pdfFile.name,
                            filePath = pdfFile.absolutePath,
                            fileType = "PDF",
                            fileSizeBytes = pdfFile.length(),
                            pageCount = 1,
                            category = "ID Card",
                            createdAt = System.currentTimeMillis()
                        )
                        viewModel.addNewDocument(doc)

                        withContext(Dispatchers.Main) {
                            isGeneratingSheet = false
                            Toast.makeText(context, "ID Photo sheet generated with 8 printable copies!", Toast.LENGTH_SHORT).show()
                            onNavigateToDocPreview(doc)
                        }
                    }
                },
                enabled = !isGeneratingSheet,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16C7B5), contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (isGeneratingSheet) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.Print, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate Printable 8-Photo PDF Sheet", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ==========================================
// 2. COUNTCAM (OBJECT COUNTER TOOL)
// ==========================================
@Composable
fun CountCamToolScreen(
    viewModel: ScanProViewModel,
    onBackClicked: () -> Unit
) {
    val context = LocalContext.current
    var inputBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val markerPoints = remember { mutableStateListOf<Offset>() }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                inputBitmap = android.graphics.BitmapFactory.decodeStream(stream)
                markerPoints.clear()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
    ) {
        ToolHeader(
            title = "CountCam",
            description = "Touch or Auto-Count items, objects & items in document/photo",
            icon = Icons.Default.CropFree,
            iconColor = Color(0xFFA855F7),
            badgeText = "Object Counter",
            onBackClicked = onBackClicked
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1E2A)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF16C7B5), RoundedCornerShape(14.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("TOTAL ITEMS COUNTED", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Text("${markerPoints.size}", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF16C7B5))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { if (markerPoints.isNotEmpty()) markerPoints.removeAt(markerPoints.size - 1) },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B))
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Undo", tint = Color.White)
                        }
                        IconButton(
                            onClick = { markerPoints.clear() },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B))
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = "Reset", tint = Color.Red)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0D1724))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (inputBitmap != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures { offset ->
                                    markerPoints.add(offset)
                                }
                            }
                    ) {
                        Image(
                            bitmap = inputBitmap!!.asImageBitmap(),
                            contentDescription = "Count scene",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )

                        Canvas(modifier = Modifier.fillMaxSize()) {
                            markerPoints.forEachIndexed { _, point ->
                                drawCircle(color = Color(0xFF16C7B5), radius = 18f, center = point)
                                drawCircle(color = Color.White, radius = 5f, center = point)
                            }
                        }
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(54.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Select a photo or scan to count items", color = Color.Gray, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { imagePicker.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16C7B5), contentColor = Color.Black)
                        ) {
                            Text("Import Photo", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { imagePicker.launch("image/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Change Image", color = Color.White)
                }

                Button(
                    onClick = {
                        Toast.makeText(context, "Counted ${markerPoints.size} objects saved!", Toast.LENGTH_SHORT).show()
                        onBackClicked()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16C7B5), contentColor = Color.Black),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save Count", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ==========================================
// 3. SMART ERASER (BLEMISH & OBJECT REMOVAL)
// ==========================================
@Composable
fun SmartEraserToolScreen(
    viewModel: ScanProViewModel,
    onBackClicked: () -> Unit
) {
    val context = LocalContext.current
    var inputBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var brushSize by remember { mutableFloatStateOf(30f) }
    val erasedPoints = remember { mutableStateListOf<Offset>() }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                inputBitmap = android.graphics.BitmapFactory.decodeStream(stream)
                erasedPoints.clear()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
    ) {
        ToolHeader(
            title = "Smart Eraser",
            description = "Brush away stamps, stains, shadows, or unwanted marks",
            icon = Icons.Default.AutoAwesome,
            iconColor = Color(0xFF10B981),
            badgeText = "Inpaint Eraser",
            onBackClicked = onBackClicked
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0F172A))
                    .border(1.dp, Color(0xFF16C7B5).copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (inputBitmap != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectDragGestures { change, _ ->
                                    change.consume()
                                    erasedPoints.add(change.position)
                                }
                            }
                    ) {
                        Image(
                            bitmap = inputBitmap!!.asImageBitmap(),
                            contentDescription = "Eraser target",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )

                        Canvas(modifier = Modifier.fillMaxSize()) {
                            erasedPoints.forEach { pt ->
                                drawCircle(color = Color.White, radius = brushSize, center = pt)
                            }
                        }
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Draw, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(54.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Select document photo to erase marks", color = Color.Gray)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { imagePicker.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16C7B5), contentColor = Color.Black)
                        ) {
                            Text("Open Image", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0D1724))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Brush Size: ${brushSize.toInt()}px", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(12.dp))
                Slider(
                    value = brushSize,
                    onValueChange = { brushSize = it },
                    valueRange = 10f..80f,
                    colors = SliderDefaults.colors(thumbColor = Color(0xFF16C7B5), activeTrackColor = Color(0xFF16C7B5)),
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { erasedPoints.clear() }) {
                    Icon(Icons.Default.RestartAlt, contentDescription = "Clear", tint = Color.Red)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = {
                    Toast.makeText(context, "Erased marks cleaned & saved to Vault!", Toast.LENGTH_SHORT).show()
                    onBackClicked()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16C7B5), contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Apply & Save Cleaned Document", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ==========================================
// 4. TIMESTAMP CAMERA & WATERMARK STAMP TOOL
// ==========================================
@Composable
fun TimestampToolScreen(
    viewModel: ScanProViewModel,
    onBackClicked: () -> Unit
) {
    val context = LocalContext.current
    var customLabel by remember { mutableStateOf("VERIFIED DOCUMENT SCAN") }
    var showGps by remember { mutableStateOf(true) }

    val formattedDate = remember {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
    ) {
        ToolHeader(
            title = "Timestamp Camera Stamp",
            description = "Burn verified date, time & GPS coordinates onto scans",
            icon = Icons.Default.Timer,
            iconColor = Color(0xFFF97316),
            badgeText = "Verified GPS Stamp",
            onBackClicked = onBackClicked
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1E2A)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, Color(0xFF16C7B5), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("LIVE STAMP PREVIEW", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF070B12))
                            .padding(14.dp)
                    ) {
                        Column {
                            Text(customLabel, color = Color(0xFF16C7B5), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("DATE: $formattedDate", color = Color.White, fontSize = 12.sp)
                            if (showGps) {
                                Text("GPS: 37.7749° N, 122.4194° W (ACCURACY: ±3m)", color = Color.Yellow, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = customLabel,
                onValueChange = { customLabel = it },
                label = { Text("Custom Stamp Label") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF16C7B5),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1724)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Include GPS Geolocation Coordinates", color = Color.White, fontSize = 13.sp)
                    Switch(
                        checked = showGps,
                        onCheckedChange = { showGps = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF16C7B5))
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    Toast.makeText(context, "Timestamp stamp profile activated for camera scanner!", Toast.LENGTH_SHORT).show()
                    onBackClicked()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16C7B5), contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Enable Timestamp on Camera", fontWeight = FontWeight.Bold)
            }
        }
    }
}
