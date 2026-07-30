package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BrandingWatermark
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
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

data class PdfToolItem(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
fun ToolsScreen(
    viewModel: ScanProViewModel,
    onNavigateToDocPreview: (ScannedDocument) -> Unit
) {
    val documents by viewModel.documents.collectAsState()
    val processingState by viewModel.processingState.collectAsState()

    var activeTool by remember { mutableStateOf<PdfToolItem?>(null) }
    var selectedDocForTool by remember { mutableStateOf<ScannedDocument?>(null) }

    var watermarkTextInput by remember { mutableStateOf("CONFIDENTIAL - SCANPRO AI") }
    var passcodePinInput by remember { mutableStateOf("1234") }
    var selectedDocsForMerge by remember { mutableStateOf(setOf<Long>()) }
    var showSignatureDialog by remember { mutableStateOf(false) }

    val primaryCyan = MaterialTheme.colorScheme.primary

    val tools = listOf(
        PdfToolItem("watermark", "Watermark PDF", "Add custom text stamp overlay", Icons.Default.BrandingWatermark, primaryCyan),
        PdfToolItem("compress", "Compress PDF", "Reduce PDF file size by up to 60%", Icons.Default.Compress, Color(0xFF0D9488)),
        PdfToolItem("rotate", "Rotate PDF", "Rotate pages by 90° or 180°", Icons.Default.RotateRight, Color(0xFFA855F7)),
        PdfToolItem("merge", "Merge PDFs", "Combine multiple PDF files", Icons.Default.CallMerge, Color(0xFFFF2E93)),
        PdfToolItem("split", "Split PDF", "Extract pages into new document", Icons.Default.CallSplit, primaryCyan),
        PdfToolItem("sign", "Sign PDF", "Draw & embed electronic signature", Icons.Default.Draw, Color(0xFF0D9488)),
        PdfToolItem("ocr", "OCR PDF Text", "Extract editable text using Gemini", Icons.Default.AutoAwesome, Color(0xFFA855F7)),
        PdfToolItem("protect", "Protect PDF", "Encrypt document with password PIN", Icons.Default.Lock, Color(0xFFF59E0B)),
        PdfToolItem("img2pdf", "Image to PDF", "Convert JPG/PNG photo to PDF", Icons.Default.Image, primaryCyan)
    )

    // Interactive Tool Processing Dialog
    if (activeTool != null) {
        val tool = activeTool!!
        AlertDialog(
            onDismissRequest = { activeTool = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(tool.icon, contentDescription = null, tint = tool.color)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(tool.title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(tool.description, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))

                    if (tool.id == "watermark") {
                        OutlinedTextField(
                            value = watermarkTextInput,
                            onValueChange = { watermarkTextInput = it },
                            label = { Text("Watermark Text") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else if (tool.id == "protect") {
                        OutlinedTextField(
                            value = passcodePinInput,
                            onValueChange = { passcodePinInput = it },
                            label = { Text("Security PIN") },
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

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        if (tool.id == "merge") "Select Documents to Merge:" else "Select Target Document:",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(modifier = Modifier.height(180.dp)) {
                        items(documents) { doc ->
                            val isSelected = if (tool.id == "merge") {
                                selectedDocsForMerge.contains(doc.id)
                            } else {
                                selectedDocForTool?.id == doc.id
                            }
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                                    .clickable {
                                        if (tool.id == "merge") {
                                            selectedDocsForMerge = if (selectedDocsForMerge.contains(doc.id)) {
                                                selectedDocsForMerge - doc.id
                                            } else {
                                                selectedDocsForMerge + doc.id
                                            }
                                        } else {
                                            selectedDocForTool = doc
                                        }
                                    }
                            ) {
                                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(doc.title, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val doc = selectedDocForTool ?: documents.firstOrNull()
                        when (tool.id) {
                            "merge" -> {
                                val mergeList = documents.filter { selectedDocsForMerge.contains(it.id) }
                                val listToMerge = if (mergeList.isNotEmpty()) mergeList else documents.take(2)
                                if (listToMerge.isNotEmpty()) {
                                    viewModel.mergeDocuments(listToMerge)
                                }
                            }
                            "split" -> {
                                if (doc != null) viewModel.splitDocument(doc)
                            }
                            "protect" -> {
                                if (doc != null) viewModel.protectDocument(doc, passcodePinInput)
                            }
                            "watermark" -> {
                                if (doc != null) viewModel.watermarkDocument(doc, watermarkTextInput)
                            }
                            "compress" -> {
                                if (doc != null) viewModel.compressDocument(doc)
                            }
                            "rotate" -> {
                                if (doc != null) viewModel.rotateDocument(doc)
                            }
                            "ocr" -> {
                                if (doc != null) {
                                    viewModel.runAiOcrOnDocument(doc)
                                    onNavigateToDocPreview(doc)
                                }
                            }
                            "sign" -> {
                                showSignatureDialog = true
                            }
                            else -> {
                                if (doc != null) viewModel.compressDocument(doc)
                            }
                        }
                        activeTool = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                ) {
                    Text("Execute Tool", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { activeTool = null }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Text(
            text = "PDF & Document AI Tools",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Select a professional PDF utility tool below",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (processingState.isProcessing) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(processingState.statusText, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                }
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(tools) { tool ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                        .clickable {
                            activeTool = tool
                            selectedDocForTool = documents.firstOrNull()
                        }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(tool.color.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(tool.icon, contentDescription = null, tint = tool.color, modifier = Modifier.size(24.dp))
                        }

                        Column {
                            Text(tool.title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(tool.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                        }
                    }
                }
            }
        }
    }
}
