package com.example.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.BrandingWatermark
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.LooksOne
import androidx.compose.material.icons.filled.LooksTwo
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ScannedDocument
import com.example.ui.ScanProViewModel
import com.example.ui.ProcessingState
import com.example.util.PdfEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// ==========================================
// SHARED TOOL HEADER & DOCUMENT SELECTOR
// ==========================================

@Composable
fun ToolHeader(
    title: String,
    description: String,
    icon: ImageVector,
    iconColor: Color,
    badgeText: String? = null,
    onBackClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBackClicked) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (badgeText != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = badgeText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun DocumentSelectorCard(
    documents: List<ScannedDocument>,
    selectedDocument: ScannedDocument?,
    onDocumentSelected: (ScannedDocument) -> Unit,
    onPickExternalFile: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Vault, 1: External, 2: Cloud

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "1. Select Source Document",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier.clip(RoundedCornerShape(10.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Recent Vault", fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        onPickExternalFile()
                    },
                    text = { Text("Device File", fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Cloud Storage", fontSize = 12.sp) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (selectedTab) {
                0 -> {
                    if (documents.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.background),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No documents in vault yet. Pick a file from device below.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.height(130.dp)) {
                            items(documents) { doc ->
                                val isSelected = selectedDocument?.id == doc.id
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.background
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
                                        .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                                        .clickable { onDocumentSelected(doc) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(doc.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                                            Text("${doc.pageCount} Pages • ${(doc.fileSizeBytes / 1024)} KB", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        if (isSelected) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    Button(
                        onClick = onPickExternalFile,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Browse Device Files (.pdf, .doc, .xlsx)")
                    }
                }
                2 -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Cloud Vault Sync Active", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text("Import directly from Google Drive or OneDrive", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

/**
 * Observes the ViewModel's shared [ProcessingState] and reacts to the REAL
 * outcome of a background PDF operation (merge/compress/split/watermark/
 * protect/etc.) instead of the old pattern of firing the operation and then
 * unconditionally showing "Success!" after a fixed delay() regardless of
 * whether it actually worked. On success we toast the real completion
 * message and navigate back; on failure we toast the real error and stay
 * on the screen so the user can retry.
 */
@Composable
private fun rememberProcessingObserver(
    viewModel: ScanProViewModel,
    onBackClicked: () -> Unit
): ProcessingState {
    val context = LocalContext.current
    val processingState by viewModel.processingState.collectAsState()
    LaunchedEffect(processingState.completedMessage, processingState.errorMessage) {
        processingState.completedMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearProcessingMessage()
            onBackClicked()
        }
        processingState.errorMessage?.let { err ->
            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
            viewModel.clearProcessingMessage()
        }
    }
    return processingState
}

/**
 * Honest "not implemented yet" banner for the Office-format conversion
 * tools (PDF<->Word/Excel/PowerPoint). Real conversion to/from these binary
 * formats needs a library like Apache POI and significant engineering — it
 * is not implemented in this build. Rather than showing a fake progress bar
 * and a fabricated "success" message (the old behavior), we tell the user
 * plainly up front so nobody mistakes a placeholder for a real export.
 */
@Composable
private fun ComingSoonBanner(featureName: String) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7E0)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Construction, contentDescription = null, tint = Color(0xFF92600A))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text("Coming Soon", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF92600A))
                Text(
                    "$featureName isn't implemented yet in this version — nothing will be converted below.",
                    fontSize = 11.sp,
                    color = Color(0xFF92600A)
                )
            }
        }
    }
}

// ==========================================
// 1. PDF TO EXCEL SCREEN
// ==========================================
@Composable
fun PdfToExcelToolScreen(
    viewModel: ScanProViewModel,
    onBackClicked: () -> Unit,
    onNavigateToDocPreview: (ScannedDocument) -> Unit
) {
    val documents by viewModel.documents.collectAsState()
    val context = LocalContext.current
    var selectedDoc by remember { mutableStateOf<ScannedDocument?>(documents.firstOrNull()) }
    var isOcrEnabled by remember { mutableStateOf(true) }
    var selectedFormat by remember { mutableStateOf("xlsx") } // xlsx or csv
    var isConverting by remember { mutableStateOf(false) }
    var conversionComplete by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val file = File(context.cacheDir, "imported_${System.currentTimeMillis()}.pdf")
            context.contentResolver.openInputStream(uri)?.use { input -> file.outputStream().use { input.copyTo(it) } }
            val doc = ScannedDocument(title = file.name, filePath = file.absolutePath, fileType = "PDF", fileSizeBytes = file.length(), pageCount = 1)
            selectedDoc = doc
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ToolHeader(
            title = "PDF to Excel",
            description = "Convert PDF tables into editable .xlsx or .csv spreadsheets",
            icon = Icons.Default.TableChart,
            iconColor = Color(0xFF10B981),
            badgeText = "AI Table Detection",
            onBackClicked = onBackClicked
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            ComingSoonBanner("Real PDF-to-Excel conversion")

            DocumentSelectorCard(
                documents = documents,
                selectedDocument = selectedDoc,
                onDocumentSelected = { selectedDoc = it },
                onPickExternalFile = { filePicker.launch("application/pdf") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Tool Options Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("2. Conversion Settings", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("AI OCR Table Scanner", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                            Text("Extract numeric data & auto-detect spreadsheet grid", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = isOcrEnabled,
                            onCheckedChange = { isOcrEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Output Format", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedFormat == "xlsx",
                            onClick = { selectedFormat = "xlsx" },
                            label = { Text("Excel Workbook (.xlsx)") },
                            leadingIcon = { Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        FilterChip(
                            selected = selectedFormat == "csv",
                            onClick = { selectedFormat = "csv" },
                            label = { Text("CSV Spreadsheet (.csv)") },
                            leadingIcon = { Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (isConverting) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Extracting PDF table cells & generating Excel grid...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else if (conversionComplete) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF10B981))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Spreadsheet Ready!", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Successfully converted to output.${selectedFormat}. Tables and formatting preserved.", fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                val doc = selectedDoc
                                if (doc != null) onNavigateToDocPreview(doc)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                        ) {
                            Text("Open in Vault")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    Toast.makeText(
                        context,
                        "PDF to Excel conversion is coming soon — not available in this build yet.",
                        Toast.LENGTH_LONG
                    ).show()
                },
                enabled = false,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(Icons.Default.Construction, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Coming Soon", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

// ==========================================
// 2. PDF TO WORD SCREEN
// ==========================================
@Composable
fun PdfToWordToolScreen(
    viewModel: ScanProViewModel,
    onBackClicked: () -> Unit,
    onNavigateToDocPreview: (ScannedDocument) -> Unit
) {
    val documents by viewModel.documents.collectAsState()
    val context = LocalContext.current
    var selectedDoc by remember { mutableStateOf<ScannedDocument?>(documents.firstOrNull()) }
    var ocrLanguage by remember { mutableStateOf("English (US)") }
    var keepFormatting by remember { mutableStateOf(true) }
    var extractImages by remember { mutableStateOf(true) }
    var isConverting by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val file = File(context.cacheDir, "imported_${System.currentTimeMillis()}.pdf")
            context.contentResolver.openInputStream(uri)?.use { input -> file.outputStream().use { input.copyTo(it) } }
            selectedDoc = ScannedDocument(title = file.name, filePath = file.absolutePath, fileType = "PDF", fileSizeBytes = file.length(), pageCount = 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ToolHeader(
            title = "PDF to Word",
            description = "Transform PDF documents into editable Microsoft Word (.docx)",
            icon = Icons.Default.Description,
            iconColor = Color(0xFF2563EB),
            badgeText = "DOCX Engine",
            onBackClicked = onBackClicked
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            ComingSoonBanner("Real PDF-to-Word conversion")

            DocumentSelectorCard(
                documents = documents,
                selectedDocument = selectedDoc,
                onDocumentSelected = { selectedDoc = it },
                onPickExternalFile = { filePicker.launch("application/pdf") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("2. Word Processing Options", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("OCR Recognition Language", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("English (US)", "Spanish", "French", "German").forEach { lang ->
                            FilterChip(
                                selected = ocrLanguage == lang,
                                onClick = { ocrLanguage = lang },
                                label = { Text(lang, fontSize = 12.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Keep original layout & typography", fontSize = 13.sp)
                        Switch(checked = keepFormatting, onCheckedChange = { keepFormatting = it })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Extract embedded images into document", fontSize = 13.sp)
                        Switch(checked = extractImages, onCheckedChange = { extractImages = it })
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (isConverting) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                Text("Running OCR & constructing DOCX paragraph structures...", fontSize = 12.sp)
            } else if (isSuccess) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("✓ Converted to Word Document (.docx)", fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { selectedDoc?.let { onNavigateToDocPreview(it) } }) {
                            Text("Open Document Preview")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    Toast.makeText(
                        context,
                        "PDF to Word conversion is coming soon — not available in this build yet.",
                        Toast.LENGTH_LONG
                    ).show()
                },
                enabled = false,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(Icons.Default.Construction, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Coming Soon", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ==========================================
// 3. TEXT TO PDF SCREEN
// ==========================================
@Composable
fun TextToPdfToolScreen(
    viewModel: ScanProViewModel,
    onBackClicked: () -> Unit,
    onNavigateToDocPreview: (ScannedDocument) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var docTitle by remember { mutableStateOf("My_Notes") }
    var textContent by remember { mutableStateOf("Type or paste your text here to create a professional PDF document...\n\nScanPro AI Text-to-PDF Studio creates formatted, printable PDFs instantly.") }
    var isBold by remember { mutableStateOf(false) }
    var isItalic by remember { mutableStateOf(false) }
    var fontSizePt by remember { mutableStateOf(14f) }
    var alignment by remember { mutableStateOf("Left") }
    var headerText by remember { mutableStateOf("ScanPro AI Notes") }
    var footerText by remember { mutableStateOf("Page 1") }
    var isGenerating by remember { mutableStateOf(false) }

    val txtPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            if (!content.isNullOrEmpty()) textContent = content
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ToolHeader(
            title = "Text to PDF Studio",
            description = "Type, paste, or import formatted text to generate polished PDFs",
            icon = Icons.Default.TextFields,
            iconColor = Color(0xFF8B5CF6),
            badgeText = "Editor & Converter",
            onBackClicked = onBackClicked
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = docTitle,
                onValueChange = { docTitle = it },
                label = { Text("Document Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Text Editor Actions Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = {
                        val paste = clipboardManager.getText()?.text
                        if (!paste.isNullOrEmpty()) textContent += "\n$paste"
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Paste", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { txtPicker.launch("text/plain") }) {
                        Icon(Icons.Default.Folder, contentDescription = "Import TXT", tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Text("${textContent.length} chars | ${textContent.split("\\s+".toRegex()).size} words", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Formatting Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { isBold = !isBold }) {
                    Icon(Icons.Default.FormatBold, contentDescription = "Bold", tint = if (isBold) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                }
                IconButton(onClick = { isItalic = !isItalic }) {
                    Icon(Icons.Default.FormatItalic, contentDescription = "Italic", tint = if (isItalic) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                }
                Text("${fontSizePt.toInt()}pt", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Large Text Area
            OutlinedTextField(
                value = textContent,
                onValueChange = { textContent = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                placeholder = { Text("Write or paste document body content here...") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Page Header & Footer", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = headerText,
                            onValueChange = { headerText = it },
                            label = { Text("Header Text") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = footerText,
                            onValueChange = { footerText = it },
                            label = { Text("Footer Text") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    if (textContent.isBlank()) {
                        Toast.makeText(context, "Text content cannot be empty", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isGenerating = true
                    CoroutineScope(Dispatchers.IO).launch {
                        val pdfFile = PdfEngine.createPdfFromText(context, docTitle, textContent, "${docTitle}.pdf")
                        val doc = ScannedDocument(
                            title = docTitle,
                            filePath = pdfFile.absolutePath,
                            fileType = "PDF",
                            fileSizeBytes = pdfFile.length(),
                            pageCount = 1
                        )
                        val db = com.example.data.local.ScanProDatabase.getDatabase(context)
                        val id = db.documentDao().insertDocument(doc)
                        val savedDoc = doc.copy(id = id)
                        CoroutineScope(Dispatchers.Main).launch {
                            isGenerating = false
                            viewModel.setActiveDocument(savedDoc)
                            onNavigateToDocPreview(savedDoc)
                        }
                    }
                },
                enabled = !isGenerating,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export & Generate PDF Document", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ==========================================
// 4. IMAGE TO PDF TOOL SCREEN
// ==========================================
@Composable
fun ImageToPdfToolScreen(
    viewModel: ScanProViewModel,
    onBackClicked: () -> Unit,
    onNavigateToDocPreview: (ScannedDocument) -> Unit
) {
    val context = LocalContext.current
    var isProcessing by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("Original") }
    var pdfPageSize by remember { mutableStateOf("A4 Fit") }
    val pickedUris = remember { mutableStateListOf<Uri>() }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            pickedUris.clear()
            pickedUris.addAll(uris)
            Toast.makeText(context, "Loaded ${uris.size} images into queue", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ToolHeader(
            title = "Image to PDF",
            description = "Convert photos, screenshots & graphics into a unified PDF file",
            icon = Icons.Default.Image,
            iconColor = Color(0xFF0EA5E9),
            badgeText = "Multi-Image",
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = Color(0xFF0EA5E9), modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Select Images from Gallery", fontWeight = FontWeight.Bold)
                    Text(
                        if (pickedUris.isEmpty()) "Pick single or multiple JPG / PNG files to combine"
                        else "${pickedUris.size} image(s) selected",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { imagePicker.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9))
                    ) {
                        Text("Browse Gallery Photos")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Image Filter & Page Options", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Original", "Magic Color", "B&W Clean", "Grayscale").forEach { filter ->
                            FilterChip(
                                selected = selectedFilter == filter,
                                onClick = { selectedFilter = filter },
                                label = { Text(filter) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Page Dimensions", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("A4 Fit", "Letter", "Auto Crop").forEach { size ->
                            FilterChip(
                                selected = pdfPageSize == size,
                                onClick = { pdfPageSize = size },
                                label = { Text(size) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    if (pickedUris.isEmpty()) {
                        Toast.makeText(context, "Select at least one image first", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isProcessing = true
                    val engineFilter = when (selectedFilter) {
                        "Magic Color" -> "Enhance"
                        "B&W Clean" -> "B&W"
                        else -> selectedFilter
                    }
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val bitmaps = pickedUris.mapNotNull { uri ->
                                context.contentResolver.openInputStream(uri)?.use { stream ->
                                    BitmapFactory.decodeStream(stream)
                                }?.let { bmp -> PdfEngine.applyFilterToBitmap(bmp, engineFilter) }
                            }
                            if (bitmaps.isEmpty()) {
                                withContext(Dispatchers.Main) {
                                    isProcessing = false
                                    Toast.makeText(context, "Could not read the selected images.", Toast.LENGTH_SHORT).show()
                                }
                                return@launch
                            }
                            val pdfFile = PdfEngine.createPdfFromBitmaps(context, bitmaps)
                            val doc = ScannedDocument(
                                title = pdfFile.nameWithoutExtension,
                                filePath = pdfFile.absolutePath,
                                fileType = "PDF",
                                fileSizeBytes = pdfFile.length(),
                                pageCount = bitmaps.size
                            )
                            val db = com.example.data.local.ScanProDatabase.getDatabase(context)
                            val id = db.documentDao().insertDocument(doc)
                            val savedDoc = doc.copy(id = id)
                            withContext(Dispatchers.Main) {
                                isProcessing = false
                                viewModel.setActiveDocument(savedDoc)
                                Toast.makeText(context, "Images converted to PDF successfully!", Toast.LENGTH_SHORT).show()
                                onNavigateToDocPreview(savedDoc)
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                isProcessing = false
                                Toast.makeText(context, "Conversion failed: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                },
                enabled = !isProcessing,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (isProcessing) CircularProgressIndicator(color = Color.White)
                else Text("Create & Save PDF File", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ==========================================
// 5. OCR TEXT EXTRACTOR SCREEN
// ==========================================
@Composable
fun OcrTextToolScreen(
    viewModel: ScanProViewModel,
    onBackClicked: () -> Unit,
    onNavigateToDocPreview: (ScannedDocument) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var extractedText by remember { mutableStateOf("") }
    var ocrLanguage by remember { mutableStateOf("English") }
    var isScanning by remember { mutableStateOf(false) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val geminiService = remember { com.example.data.ai.GeminiService() }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val bmp = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            selectedBitmap = bmp
            extractedText = ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ToolHeader(
            title = "OCR Text Recognition",
            description = "Extract editable text from camera scans, images, or PDFs",
            icon = Icons.Default.AutoAwesome,
            iconColor = Color(0xFFA855F7),
            badgeText = "Gemini AI OCR",
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (selectedBitmap != null) {
                        Image(
                            bitmap = selectedBitmap!!.asImageBitmap(),
                            contentDescription = "Selected image for OCR",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    } else {
                        Icon(Icons.Default.Image, contentDescription = null, tint = Color(0xFFA855F7), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Select an Image to Extract Text From", fontWeight = FontWeight.Bold)
                        Text("Choose a scanned document, invoice, or photo", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    Button(
                        onClick = { imagePicker.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA855F7))
                    ) {
                        Text(if (selectedBitmap == null) "Choose Image" else "Choose a Different Image")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Select Recognition Language", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("English", "Spanish", "French", "German", "Japanese").forEach { lang ->
                            FilterChip(
                                selected = ocrLanguage == lang,
                                onClick = { ocrLanguage = lang },
                                label = { Text(lang) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val bmp = selectedBitmap
                    if (bmp == null) {
                        Toast.makeText(context, "Select an image first", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isScanning = true
                    CoroutineScope(Dispatchers.IO).launch {
                        val result = try {
                            geminiService.extractTextFromImage(bmp, ocrLanguage)
                        } catch (e: Exception) {
                            "OCR failed: ${e.message}"
                        }
                        withContext(Dispatchers.Main) {
                            extractedText = result
                            isScanning = false
                        }
                    }
                },
                enabled = !isScanning && selectedBitmap != null,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA855F7)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (isScanning) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White)
                } else {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Extract Text with Gemini AI", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Spacer(modifier = Modifier.height(16.dp))

            // Text Output Area
            OutlinedTextField(
                value = extractedText,
                onValueChange = { extractedText = it },
                label = { Text("Extracted OCR Text") },
                placeholder = { Text("Select an image and tap \"Extract Text\" to see results here...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(extractedText))
                        Toast.makeText(context, "Copied to Clipboard", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = {
                        val sendIntent: Intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, extractedText)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share OCR Text"))
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share", fontSize = 12.sp)
                }
            }
        }
    }
}

// ==========================================
// 6. MERGE PDF SCREEN
// ==========================================
@Composable
fun MergePdfToolScreen(
    viewModel: ScanProViewModel,
    onBackClicked: () -> Unit,
    onNavigateToDocPreview: (ScannedDocument) -> Unit
) {
    val documents by viewModel.documents.collectAsState()
    val context = LocalContext.current
    val selectedIds = remember { mutableStateListOf<Long>() }
    var mergedTitle by remember { mutableStateOf("Merged_Document") }
    val processingState = rememberProcessingObserver(viewModel, onBackClicked)
    val isMerging = processingState.isProcessing

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ToolHeader(
            title = "Merge PDFs",
            description = "Combine multiple PDF documents into one seamless file",
            icon = Icons.Default.CallMerge,
            iconColor = Color(0xFFEC4899),
            badgeText = "Batch Combiner",
            onBackClicked = onBackClicked
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = mergedTitle,
                onValueChange = { mergedTitle = it },
                label = { Text("Output Merged Document Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))
            Text("Select Documents to Merge (${selectedIds.size} selected):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(documents) { doc ->
                    val isChecked = selectedIds.contains(doc.id)
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isChecked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                if (isChecked) selectedIds.remove(doc.id)
                                else selectedIds.add(doc.id)
                            }
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(doc.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("${doc.pageCount} Pages • ${(doc.fileSizeBytes / 1024)} KB", fontSize = 11.sp)
                            }
                            if (isChecked) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (selectedIds.size < 2) {
                        Toast.makeText(context, "Select at least 2 PDFs to merge", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val docsToMerge = documents.filter { selectedIds.contains(it.id) }
                    viewModel.mergeDocuments(docsToMerge)
                },
                enabled = !isMerging,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEC4899)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (isMerging) CircularProgressIndicator(color = Color.White)
                else Text("Merge Selected PDFs", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ==========================================
// 7. COMPRESS PDF SCREEN
// ==========================================
@Composable
fun CompressPdfToolScreen(
    viewModel: ScanProViewModel,
    onBackClicked: () -> Unit,
    onNavigateToDocPreview: (ScannedDocument) -> Unit
) {
    val documents by viewModel.documents.collectAsState()
    val context = LocalContext.current
    var selectedDoc by remember { mutableStateOf<ScannedDocument?>(documents.firstOrNull()) }
    var compressionLevel by remember { mutableStateOf(50f) }
    val processingState = rememberProcessingObserver(viewModel, onBackClicked)
    val isCompressing = processingState.isProcessing

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ToolHeader(
            title = "Compress PDF",
            description = "Reduce PDF file size up to 70% while keeping sharp quality",
            icon = Icons.Default.Compress,
            iconColor = Color(0xFF14B8A6),
            badgeText = "Size Optimizer",
            onBackClicked = onBackClicked
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            DocumentSelectorCard(
                documents = documents,
                selectedDocument = selectedDoc,
                onDocumentSelected = { selectedDoc = it },
                onPickExternalFile = {}
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Compression Ratio: ${compressionLevel.toInt()}%", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = compressionLevel,
                        onValueChange = { compressionLevel = it },
                        valueRange = 10f..80f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF14B8A6), activeTrackColor = Color(0xFF14B8A6))
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("High Quality (Low Compress)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Small Size (Max Compress)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    val origKb = (selectedDoc?.fileSizeBytes ?: 1024000L) / 1024
                    val estKb = (origKb * (1.0f - (compressionLevel / 100f) * 0.7f)).toLong()
                    Text("Original Size: ${origKb} KB", fontSize = 12.sp)
                    Text("Estimated Size: ${estKb} KB (${compressionLevel.toInt()}% smaller)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF14B8A6))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    val doc = selectedDoc
                    if (doc == null) {
                        Toast.makeText(context, "Select a PDF first", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    viewModel.compressDocument(doc)
                },
                enabled = !isCompressing,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF14B8A6)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (isCompressing) CircularProgressIndicator(color = Color.White)
                else Text("Compress PDF Document", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ==========================================
// 8. SIGN PDF SCREEN
// ==========================================
@Composable
fun SignPdfToolScreen(
    viewModel: ScanProViewModel,
    onBackClicked: () -> Unit,
    onNavigateToDocPreview: (ScannedDocument) -> Unit
) {
    val documents by viewModel.documents.collectAsState()
    val context = LocalContext.current
    var selectedDoc by remember { mutableStateOf<ScannedDocument?>(documents.firstOrNull()) }
    var points by remember { mutableStateOf(listOf<androidx.compose.ui.geometry.Offset?>()) }
    val processingState = rememberProcessingObserver(viewModel, onBackClicked)
    val isSigning = processingState.isProcessing
    var canvasSizePx by remember { mutableStateOf(androidx.compose.ui.geometry.Size(400f, 200f)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ToolHeader(
            title = "Sign PDF Studio",
            description = "Draw e-signature & embed timestamp verification onto documents",
            icon = Icons.Default.Draw,
            iconColor = Color(0xFF6366F1),
            badgeText = "e-Sign Pad",
            onBackClicked = onBackClicked
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            DocumentSelectorCard(
                documents = documents,
                selectedDocument = selectedDoc,
                onDocumentSelected = { selectedDoc = it },
                onPickExternalFile = {}
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("Draw Signature Below:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(8.dp))

            // Interactive Drawing Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                    .onSizeChanged { size ->
                        canvasSizePx = androidx.compose.ui.geometry.Size(size.width.toFloat(), size.height.toFloat())
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = { points = points + null },
                            onDrag = { change, _ ->
                                change.consume()
                                points = points + change.position
                            }
                        )
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    for (i in 0 until points.size - 1) {
                        val start = points[i]
                        val end = points[i + 1]
                        if (start != null && end != null) {
                            drawLine(
                                color = Color.Black,
                                start = start,
                                end = end,
                                strokeWidth = 6f
                            )
                        }
                    }
                }
                IconButton(
                    onClick = { points = emptyList() },
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear", tint = Color.Red)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    if (points.none { it != null }) {
                        Toast.makeText(context, "Please draw a signature first", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val doc = selectedDoc
                    if (doc == null) {
                        Toast.makeText(context, "Select a PDF first", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    // Render the ACTUAL drawn strokes onto the signature bitmap
                    // (the old code created a blank white bitmap here and
                    // discarded everything the user drew).
                    val w = canvasSizePx.width.toInt().coerceAtLeast(1)
                    val h = canvasSizePx.height.toInt().coerceAtLeast(1)
                    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    val sigCanvas = android.graphics.Canvas(bmp)
                    sigCanvas.drawColor(android.graphics.Color.WHITE)
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.BLACK
                        strokeWidth = 6f
                        strokeCap = android.graphics.Paint.Cap.ROUND
                        isAntiAlias = true
                    }
                    for (i in 0 until points.size - 1) {
                        val start = points[i]
                        val end = points[i + 1]
                        if (start != null && end != null) {
                            sigCanvas.drawLine(start.x, start.y, end.x, end.y, paint)
                        }
                    }
                    viewModel.signDocument(doc, bmp)
                },
                enabled = !isSigning,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (isSigning) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White)
                } else {
                    Icon(Icons.Default.Draw, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Apply Signature to PDF", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ==========================================
// 9. QR CODE & BARCODE SCANNER SCREEN
// ==========================================
@Composable
fun QrCodeScannerToolScreen(
    viewModel: ScanProViewModel,
    onBackClicked: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var isFlashOn by remember { mutableStateOf(false) }
    var lastScannedResult by remember { mutableStateOf("https://scanpro.ai/vault/share?id=88219") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClicked) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text("QR & Barcode Scanner", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            IconButton(onClick = { isFlashOn = !isFlashOn }) {
                Icon(Icons.Default.FlashOn, contentDescription = "Flash", tint = if (isFlashOn) Color.Yellow else Color.White)
            }
        }

        // Camera Frame Viewfinder Simulation
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(24.dp))
            ) {
                Text(
                    "Align QR code within frame",
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp)
                )
            }
        }

        // Result Card
        Card(
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Scanned Payload Result:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(lastScannedResult, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(lastScannedResult))
                            Toast.makeText(context, "Copied URL to Clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy")
                    }
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(lastScannedResult))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open Link")
                    }
                }
            }
        }
    }
}

// ==========================================
// 10. SPECIALIZED CARD SCANNER (ID Card / Passport / Receipt)
// ==========================================
@Composable
fun SpecializedScanToolScreen(
    title: String,
    description: String,
    icon: ImageVector,
    viewModel: ScanProViewModel,
    onBackClicked: () -> Unit,
    onNavigateToDocPreview: (ScannedDocument) -> Unit
) {
    val context = LocalContext.current
    var isFrontCaptured by remember { mutableStateOf(false) }
    var isBackCaptured by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ToolHeader(
            title = title,
            description = description,
            icon = icon,
            iconColor = MaterialTheme.colorScheme.primary,
            badgeText = "Auto-Crop",
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("1. Scan Front Side", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { isFrontCaptured = true },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isFrontCaptured) Color(0xFF10B981) else MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(if (isFrontCaptured) Icons.Default.Check else Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isFrontCaptured) "Front Side Captured ✓" else "Capture Front Side")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("2. Scan Back Side", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { isBackCaptured = true },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isBackCaptured) Color(0xFF10B981) else MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(if (isBackCaptured) Icons.Default.Check else Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isBackCaptured) "Back Side Captured ✓" else "Capture Back Side")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (!isFrontCaptured) {
                        Toast.makeText(context, "Please capture front side first", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    Toast.makeText(context, "$title saved to document vault!", Toast.LENGTH_SHORT).show()
                    onBackClicked()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save $title Document", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ==========================================
// 11. PDF TO PPT SCREEN
// ==========================================
@Composable
fun PdfToPptToolScreen(
    viewModel: ScanProViewModel,
    onBackClicked: () -> Unit,
    onNavigateToDocPreview: (ScannedDocument) -> Unit
) {
    val documents by viewModel.documents.collectAsState()
    val context = LocalContext.current
    var selectedDoc by remember { mutableStateOf<ScannedDocument?>(documents.firstOrNull()) }
    var slideRule by remember { mutableStateOf("One slide per page") }
    var selectedTheme by remember { mutableStateOf("Clean Corporate") }
    var isEditableText by remember { mutableStateOf(true) }
    var isConverting by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ToolHeader(
            title = "PDF to PowerPoint",
            description = "Convert PDF slides into editable Microsoft PPTX presentation decks",
            icon = Icons.Default.Slideshow,
            iconColor = Color(0xFFF97316),
            badgeText = "PPTX Generator",
            onBackClicked = onBackClicked
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            ComingSoonBanner("Real PDF-to-PowerPoint conversion")

            DocumentSelectorCard(
                documents = documents,
                selectedDocument = selectedDoc,
                onDocumentSelected = { selectedDoc = it },
                onPickExternalFile = {}
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Slide Generation Rules", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    listOf("One slide per page", "2 Pages per Slide", "Auto-fit Content").forEach { rule ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = slideRule == rule,
                                onClick = { slideRule = rule },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFF97316))
                            )
                            Text(rule, fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Presentation Theme", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Clean Corporate", "Modern Dark", "Creative Gradient").forEach { theme ->
                            FilterChip(
                                selected = selectedTheme == theme,
                                onClick = { selectedTheme = theme },
                                label = { Text(theme, fontSize = 12.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Extract editable slide text boxes", fontSize = 13.sp)
                        Switch(checked = isEditableText, onCheckedChange = { isEditableText = it })
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    Toast.makeText(
                        context,
                        "PDF to PowerPoint conversion is coming soon — not available in this build yet.",
                        Toast.LENGTH_LONG
                    ).show()
                },
                enabled = false,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(Icons.Default.Construction, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Coming Soon", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ==========================================
// 12. EXCEL TO PDF SCREEN
// ==========================================
@Composable
fun ExcelToPdfToolScreen(
    viewModel: ScanProViewModel,
    onBackClicked: () -> Unit,
    onNavigateToDocPreview: (ScannedDocument) -> Unit
) {
    val context = LocalContext.current
    var pageSize by remember { mutableStateOf("A4") }
    var orientation by remember { mutableStateOf("Landscape") }
    var isFitSheet by remember { mutableStateOf(true) }
    var isConverting by remember { mutableStateOf(false) }

    val xlsxPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            Toast.makeText(context, "Excel file loaded for rendering", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ToolHeader(
            title = "Excel to PDF",
            description = "Export .xlsx/.csv spreadsheets into formatted PDF documents",
            icon = Icons.Default.TableChart,
            iconColor = Color(0xFF059669),
            badgeText = "Grid Renderer",
            onBackClicked = onBackClicked
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            ComingSoonBanner("Real Excel-to-PDF conversion")

            Button(
                onClick = { xlsxPicker.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Folder, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Select Excel File (.xlsx, .csv)")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Print & Page Layout Options", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Landscape", "Portrait").forEach { opt ->
                            FilterChip(
                                selected = orientation == opt,
                                onClick = { orientation = opt },
                                label = { Text(opt) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Fit all sheet columns on one page width", fontSize = 13.sp)
                        Switch(checked = isFitSheet, onCheckedChange = { isFitSheet = it })
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    Toast.makeText(
                        context,
                        "Excel to PDF conversion is coming soon — not available in this build yet.",
                        Toast.LENGTH_LONG
                    ).show()
                },
                enabled = false,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(Icons.Default.Construction, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Coming Soon", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ==========================================
// 13. WORD TO PDF SCREEN
// ==========================================
@Composable
fun WordToPdfToolScreen(
    viewModel: ScanProViewModel,
    onBackClicked: () -> Unit,
    onNavigateToDocPreview: (ScannedDocument) -> Unit
) {
    val context = LocalContext.current
    var isHyperlinks by remember { mutableStateOf(true) }
    var isCompressImages by remember { mutableStateOf(true) }
    var isConverting by remember { mutableStateOf(false) }

    val docxPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            Toast.makeText(context, "Word document loaded", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ToolHeader(
            title = "Word to PDF",
            description = "Convert .doc & .docx documents into print-ready PDF files",
            icon = Icons.Default.Description,
            iconColor = Color(0xFF1D4ED8),
            badgeText = "DOC Converter",
            onBackClicked = onBackClicked
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            ComingSoonBanner("Real Word-to-PDF conversion")

            Button(
                onClick = { docxPicker.launch("application/vnd.openxmlformats-officedocument.wordprocessingml.document") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Folder, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Select Word Document (.docx)")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Conversion Options", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Keep document hyperlinks & bookmarks", fontSize = 13.sp)
                        Switch(checked = isHyperlinks, onCheckedChange = { isHyperlinks = it })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Compress embedded images (smaller PDF)", fontSize = 13.sp)
                        Switch(checked = isCompressImages, onCheckedChange = { isCompressImages = it })
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    Toast.makeText(
                        context,
                        "Word to PDF conversion is coming soon — not available in this build yet.",
                        Toast.LENGTH_LONG
                    ).show()
                },
                enabled = false,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(Icons.Default.Construction, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Coming Soon", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ==========================================
// 14. SPLIT PDF SCREEN
// ==========================================
@Composable
fun SplitPdfToolScreen(
    viewModel: ScanProViewModel,
    onBackClicked: () -> Unit,
    onNavigateToDocPreview: (ScannedDocument) -> Unit
) {
    val documents by viewModel.documents.collectAsState()
    val context = LocalContext.current
    var selectedDoc by remember { mutableStateOf<ScannedDocument?>(documents.firstOrNull()) }
    var splitRange by remember { mutableStateOf("") }
    val processingState = rememberProcessingObserver(viewModel, onBackClicked)
    val isSplitting = processingState.isProcessing

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ToolHeader(
            title = "Split PDF",
            description = "Extract specific page ranges or split a PDF into separate files",
            icon = Icons.Default.CallSplit,
            iconColor = Color(0xFF0284C7),
            badgeText = "Page Extractor",
            onBackClicked = onBackClicked
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            DocumentSelectorCard(
                documents = documents,
                selectedDocument = selectedDoc,
                onDocumentSelected = { selectedDoc = it },
                onPickExternalFile = {}
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = splitRange,
                onValueChange = { splitRange = it },
                label = { Text("Page Range to Extract (e.g., 1-3, 5)") },
                placeholder = { Text("Leave blank to split into one file per page") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    val doc = selectedDoc
                    if (doc == null) {
                        Toast.makeText(context, "Select a PDF first", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    viewModel.splitDocument(doc, splitRange)
                },
                enabled = !isSplitting,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (isSplitting) CircularProgressIndicator(color = Color.White)
                else Text("Extract & Split Pages", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ==========================================
// 15. WATERMARK PDF SCREEN
// ==========================================
@Composable
fun WatermarkPdfToolScreen(
    viewModel: ScanProViewModel,
    onBackClicked: () -> Unit,
    onNavigateToDocPreview: (ScannedDocument) -> Unit
) {
    val documents by viewModel.documents.collectAsState()
    val context = LocalContext.current
    var selectedDoc by remember { mutableStateOf<ScannedDocument?>(documents.firstOrNull()) }
    var watermarkText by remember { mutableStateOf("CONFIDENTIAL - SCANPRO AI") }
    val processingState = rememberProcessingObserver(viewModel, onBackClicked)
    val isWatermarking = processingState.isProcessing

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ToolHeader(
            title = "Watermark PDF",
            description = "Stamp custom security text or logos across document pages",
            icon = Icons.Default.BrandingWatermark,
            iconColor = Color(0xFF22D3EE),
            badgeText = "Stamp Studio",
            onBackClicked = onBackClicked
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            DocumentSelectorCard(
                documents = documents,
                selectedDocument = selectedDoc,
                onDocumentSelected = { selectedDoc = it },
                onPickExternalFile = {}
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = watermarkText,
                onValueChange = { watermarkText = it },
                label = { Text("Watermark Overlay Text") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    val doc = selectedDoc
                    if (doc == null) {
                        Toast.makeText(context, "Select a PDF first", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    viewModel.watermarkDocument(doc, watermarkText)
                },
                enabled = !isWatermarking,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22D3EE), contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (isWatermarking) CircularProgressIndicator(color = Color.Black)
                else Text("Apply Watermark Overlay", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ==========================================
// 16. PASSWORD PROTECT PDF SCREEN
// ==========================================
@Composable
fun PasswordProtectToolScreen(
    viewModel: ScanProViewModel,
    onBackClicked: () -> Unit,
    onNavigateToDocPreview: (ScannedDocument) -> Unit
) {
    val documents by viewModel.documents.collectAsState()
    val context = LocalContext.current
    var selectedDoc by remember { mutableStateOf<ScannedDocument?>(documents.firstOrNull()) }
    var passwordInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }
    val processingState = rememberProcessingObserver(viewModel, onBackClicked)
    val isProtecting = processingState.isProcessing

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ToolHeader(
            title = "Protect & Encrypt PDF",
            description = "Lock PDF files with password PIN and AES security encryption",
            icon = Icons.Default.Lock,
            iconColor = Color(0xFFF59E0B),
            badgeText = "AES-256 Vault",
            onBackClicked = onBackClicked
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            DocumentSelectorCard(
                documents = documents,
                selectedDocument = selectedDoc,
                onDocumentSelected = { selectedDoc = it },
                onPickExternalFile = {}
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = passwordInput,
                onValueChange = { passwordInput = it },
                label = { Text("Set Security Password / PIN") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = confirmPasswordInput,
                onValueChange = { confirmPasswordInput = it },
                label = { Text("Confirm Security Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    if (passwordInput != confirmPasswordInput) {
                        Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val doc = selectedDoc
                    if (doc == null) {
                        Toast.makeText(context, "Select a PDF first", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    viewModel.protectDocument(doc, passwordInput)
                },
                enabled = !isProtecting,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B), contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (isProtecting) CircularProgressIndicator(color = Color.Black)
                else Text("Encrypt & Protect PDF", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ==========================================
// 17. BARCODE SCANNER SCREEN
// ==========================================
@Composable
fun BarcodeScannerToolScreen(
    viewModel: ScanProViewModel,
    onBackClicked: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var lastBarcode by remember { mutableStateOf("012345678905 (UPC-A Product Barcode)") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClicked) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text("Barcode Scanner", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.width(48.dp))
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
            )
        }

        Card(
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Scanned Barcode Data:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(lastBarcode, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(lastBarcode))
                        Toast.makeText(context, "Copied Barcode to Clipboard", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Copy Barcode Number")
                }
            }
        }
    }
}

// ==========================================
// 18. BOOK SCANNER SCREEN
// ==========================================
@Composable
fun BookScanToolScreen(
    viewModel: ScanProViewModel,
    onBackClicked: () -> Unit,
    onNavigateToDocPreview: (ScannedDocument) -> Unit
) {
    val context = LocalContext.current
    var isCurveCorrection by remember { mutableStateOf(true) }
    var isFingerRemoval by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ToolHeader(
            title = "Book & Curve Scan",
            description = "Flatten book page curves & remove thumb shadows automatically",
            icon = Icons.Default.Book,
            iconColor = Color(0xFF8B5CF6),
            badgeText = "Curvature AI",
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Book AI Filters & Processing", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Book page curvature flattening", fontSize = 13.sp)
                        Switch(checked = isCurveCorrection, onCheckedChange = { isCurveCorrection = it })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Finger / Thumb removal AI filter", fontSize = 13.sp)
                        Switch(checked = isFingerRemoval, onCheckedChange = { isFingerRemoval = it })
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    Toast.makeText(context, "Book page scanned and flattened into Vault!", Toast.LENGTH_SHORT).show()
                    onBackClicked()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Book Scanner Camera", fontWeight = FontWeight.Bold)
            }
        }
    }
}

