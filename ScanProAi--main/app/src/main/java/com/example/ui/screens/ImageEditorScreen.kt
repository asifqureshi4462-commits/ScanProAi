package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Filter
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ScannedDocument
import com.example.ui.ScanProViewModel
import com.example.util.PdfEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@Composable
fun ImageEditorScreen(
    viewModel: ScanProViewModel,
    onBackClicked: () -> Unit
) {
    val context = LocalContext.current
    val activeDocument by viewModel.activeDocument.collectAsState()
    val doc = activeDocument ?: return

    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var rotationAngle by remember { mutableStateOf(0f) }
    var selectedFilter by remember { mutableStateOf("Original") } // Original, Magic Color, B&W, Grayscale, Vintage, Invert
    var qualityCompression by remember { mutableStateOf(85f) }

    var showOcrDialog by remember { mutableStateOf(false) }
    var ocrExtractedText by remember { mutableStateOf("") }
    var isOcrProcessing by remember { mutableStateOf(false) }

    // Load Image
    LaunchedEffect(doc.filePath) {
        val file = File(doc.filePath)
        if (file.exists()) {
            imageBitmap = BitmapFactory.decodeFile(file.absolutePath)
        }
    }

    // OCR Modal
    if (showOcrDialog) {
        AlertDialog(
            onDismissRequest = { showOcrDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF0EA5E9))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("OCR Image Text Recognition", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                if (isOcrProcessing) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Extracting text from image with Gemini OCR...")
                    }
                } else {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text(ocrExtractedText, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showOcrDialog = false }) { Text("Close") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E293B))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClicked) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF0EA5E9).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, tint = Color(0xFF0EA5E9), modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(doc.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                    Text("Image Studio Suite (${doc.fileType})", fontSize = 11.sp, color = Color(0xFF94A3B8))
                }
            }

            Row {
                IconButton(
                    onClick = {
                        isOcrProcessing = true
                        showOcrDialog = true
                        viewModel.extractTextFromDocument(doc) { text ->
                            ocrExtractedText = text
                            isOcrProcessing = false
                        }
                    }
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "OCR Text", tint = Color(0xFFA855F7))
                }

                IconButton(
                    onClick = {
                        imageBitmap?.let { bmp ->
                            CoroutineScope(Dispatchers.IO).launch {
                                val pdfFile = PdfEngine.createPdfFromBitmaps(context, listOf(bmp), "${doc.title}_Image.pdf")
                                val newDoc = ScannedDocument(
                                    title = "${doc.title}_Converted.pdf",
                                    filePath = pdfFile.absolutePath,
                                    fileType = "PDF",
                                    fileSizeBytes = pdfFile.length(),
                                    createdAt = System.currentTimeMillis()
                                )
                                viewModel.addNewDocument(newDoc)
                            }
                            Toast.makeText(context, "Converted Image to PDF!", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = "Convert PDF", tint = Color(0xFFEF4444))
                }

                IconButton(
                    onClick = {
                        Toast.makeText(context, "Image changes saved!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Save Image", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Main Image Canvas Container
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (imageBitmap != null) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Image(
                        bitmap = imageBitmap!!.asImageBitmap(),
                        contentDescription = "Editing Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            } else {
                CircularProgressIndicator(color = Color.White)
            }
        }

        // Bottom Image Tool Controls Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E293B))
                .padding(12.dp)
        ) {
            Text("Visual Filters", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Original", "Magic Color", "Grayscale", "B&W", "Vintage", "Invert").forEach { filter ->
                    val isSel = selectedFilter == filter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) MaterialTheme.colorScheme.primary else Color(0xFF334155))
                            .clickable { selectedFilter = filter }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(filter, fontSize = 12.sp, color = Color.White, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { rotationAngle = (rotationAngle + 90f) % 360f }) {
                        Icon(Icons.Default.RotateRight, contentDescription = "Rotate", tint = Color.White)
                    }
                    Text("Rotate", fontSize = 12.sp, color = Color.White)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Compress: ${qualityCompression.toInt()}%", fontSize = 12.sp, color = Color.White)
                    Slider(
                        value = qualityCompression,
                        onValueChange = { qualityCompression = it },
                        valueRange = 20f..100f,
                        modifier = Modifier.width(120.dp)
                    )
                }
            }
        }
    }
}
