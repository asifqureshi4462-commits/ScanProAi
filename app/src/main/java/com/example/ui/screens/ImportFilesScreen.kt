package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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

@Composable
fun ImportFilesScreen(
    viewModel: ScanProViewModel,
    onBackClicked: () -> Unit,
    onDocumentImported: (ScannedDocument) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val recentDocuments by viewModel.documents.collectAsState()

    var isProcessing by remember { mutableStateOf(false) }
    var importedFileName by remember { mutableStateOf<String?>(null) }

    // Multi-format File Launcher
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            isProcessing = true
            coroutineScope.launch(Dispatchers.IO) {
                var lastImportedDoc: ScannedDocument? = null
                uris.forEach { uri ->
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val docsDir = File(context.filesDir, "imported").apply { if (!exists()) mkdirs() }
                    val extension = context.contentResolver.getType(uri)?.let {
                        when {
                            it.contains("pdf") -> "pdf"
                            it.contains("word") || it.contains("msword") -> "docx"
                            it.contains("excel") || it.contains("sheet") -> "xlsx"
                            it.contains("presentation") || it.contains("powerpoint") -> "pptx"
                            it.contains("image") -> "jpg"
                            else -> "pdf"
                        }
                    } ?: "pdf"

                    val destFile = File(docsDir, "Imported_${System.currentTimeMillis()}.$extension")
                    FileOutputStream(destFile).use { out -> inputStream?.copyTo(out) }

                    val doc = ScannedDocument(
                        title = destFile.name,
                        filePath = destFile.absolutePath,
                        fileType = extension.uppercase(),
                        fileSizeBytes = destFile.length(),
                        pageCount = 1,
                        category = "Imported",
                        createdAt = System.currentTimeMillis()
                    )
                    viewModel.addNewDocument(doc)
                    lastImportedDoc = doc
                }

                withContext(Dispatchers.Main) {
                    isProcessing = false
                    Toast.makeText(context, "Imported ${uris.size} document(s) successfully!", Toast.LENGTH_SHORT).show()
                    lastImportedDoc?.let { onDocumentImported(it) }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClicked) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text("Import Files", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Add PDF, Office & Image Documents", color = Color(0xFF16C7B5), fontSize = 11.sp)
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main Action Cards
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1E2A)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, Color(0xFF16C7B5).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .clickable {
                            documentPickerLauncher.launch(
                                arrayOf(
                                    "application/pdf",
                                    "application/msword",
                                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                    "application/vnd.ms-excel",
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                    "text/plain",
                                    "image/*"
                                )
                            )
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF16C7B5).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Color(0xFF16C7B5), modifier = Modifier.size(28.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Browse Device Files", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("PDF, Word, Excel, PPTX, Images", color = Color.Gray, fontSize = 12.sp)
                        }
                        Icon(Icons.Default.UploadFile, contentDescription = null, tint = Color(0xFF16C7B5))
                    }
                }
            }

            // Google Drive / Cloud Integration card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1724)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                        .clickable {
                            documentPickerLauncher.launch(arrayOf("application/pdf"))
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF3B82F6).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CloudQueue, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(26.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Google Drive & Cloud", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Import directly from Cloud Storage Provider", color = Color.Gray, fontSize = 11.sp)
                        }
                    }
                }
            }

            // Progress status indicator
            if (isProcessing) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(color = Color(0xFF16C7B5), modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Ingesting document into Vault...", color = Color.White, fontSize = 13.sp)
                    }
                }
            }

            // Recent Files Section
            item {
                Text(
                    text = "RECENT VAULT DOCUMENTS",
                    color = Color(0xFF16C7B5),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                )
            }

            items(recentDocuments.take(8)) { doc ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1724)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDocumentImported(doc) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when {
                                doc.fileType.contains("XLS") -> Icons.Default.TableChart
                                doc.fileType.contains("DOC") -> Icons.Default.Description
                                doc.fileType.contains("JPG") || doc.fileType.contains("PNG") -> Icons.Default.Image
                                else -> Icons.Default.PictureAsPdf
                            },
                            contentDescription = null,
                            tint = Color(0xFF16C7B5),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(doc.title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${doc.fileType} • ${doc.fileSizeBytes / 1024} KB", color = Color.Gray, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
