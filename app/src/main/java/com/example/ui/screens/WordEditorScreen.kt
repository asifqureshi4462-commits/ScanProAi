package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignJustify
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatAlignRight
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ScannedDocument
import com.example.ui.ScanProViewModel
import com.example.util.PdfEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class TableGridData(
    val rows: Int,
    val cols: Int,
    val cells: MutableList<MutableList<String>>
)

@Composable
fun WordEditorScreen(
    viewModel: ScanProViewModel,
    onBackClicked: () -> Unit
) {
    val context = LocalContext.current
    val activeDocument by viewModel.activeDocument.collectAsState()
    val doc = activeDocument ?: return

    var documentTitle by remember { mutableStateOf(doc.title) }
    var contentText by remember {
        mutableStateOf(
            doc.extractedText ?: """
                # ${doc.title}
                
                Welcome to ScanPro AI Word & Document Suite.
                
                This is a full-featured Word Editor supporting DOC, DOCX, TXT, and RTF formats.
                
                Key Features Included:
                • Rich text formatting (Bold, Italic, Underline, Strikethrough)
                • Custom Font Sizes and Styles
                • Text Color and Highlight fill pickers
                • Alignment (Left, Center, Right, Justify)
                • Bullet lists & Numbered lists
                • Insert interactive Tables and Images
                • Gemini AI Suite: Summarize, Translate, Grammar Fix, Tone Rewrite
                • One-click export to PDF
            """.trimIndent()
        )
    }

    // Formatting state
    var isBold by remember { mutableStateOf(false) }
    var isItalic by remember { mutableStateOf(false) }
    var isUnderline by remember { mutableStateOf(false) }
    var isStrikethrough by remember { mutableStateOf(false) }
    var selectedFontSize by remember { mutableStateOf(16) }
    var showFontSizeMenu by remember { mutableStateOf(false) }
    var alignment by remember { mutableStateOf(TextAlign.Left) }
    var selectedTextColor by remember { mutableStateOf(Color.Black) }

    // Interactive Tables State
    val embeddedTables = remember { mutableStateListOf<TableGridData>() }
    var showInsertTableDialog by remember { mutableStateOf(false) }
    var tableRowsInput by remember { mutableStateOf("3") }
    var tableColsInput by remember { mutableStateOf("3") }

    // Search and Replace
    var showSearchDialog by remember { mutableStateOf(false) }
    var searchKeyword by remember { mutableStateOf("") }
    var replaceKeyword by remember { mutableStateOf("") }

    // AI Processing Dialog
    var showAiModal by remember { mutableStateOf(false) }
    var aiModalTitle by remember { mutableStateOf("AI Document Assistant") }
    var aiResultContent by remember { mutableStateOf("") }
    var isAiLoading by remember { mutableStateOf(false) }

    // Calculation stats
    val wordCount = remember(contentText) { contentText.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }.size }
    val charCount = remember(contentText) { contentText.length }

    // Table Dialog
    if (showInsertTableDialog) {
        AlertDialog(
            onDismissRequest = { showInsertTableDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Insert Table", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = tableRowsInput,
                        onValueChange = { tableRowsInput = it },
                        label = { Text("Number of Rows") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = tableColsInput,
                        onValueChange = { tableColsInput = it },
                        label = { Text("Number of Columns") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val r = tableRowsInput.toIntOrNull() ?: 3
                        val c = tableColsInput.toIntOrNull() ?: 3
                        val grid = MutableList(r) { MutableList(c) { "" } }
                        embeddedTables.add(TableGridData(r, c, grid))
                        showInsertTableDialog = false
                        Toast.makeText(context, "Table inserted ($r x $c)", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Insert", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showInsertTableDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Search & Replace Dialog
    if (showSearchDialog) {
        AlertDialog(
            onDismissRequest = { showSearchDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Find & Replace Text", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = searchKeyword,
                        onValueChange = { searchKeyword = it },
                        label = { Text("Find Text") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = replaceKeyword,
                        onValueChange = { replaceKeyword = it },
                        label = { Text("Replace With") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (searchKeyword.isNotBlank()) {
                            contentText = contentText.replace(searchKeyword, replaceKeyword, ignoreCase = true)
                            Toast.makeText(context, "Replaced occurrences of '$searchKeyword'", Toast.LENGTH_SHORT).show()
                        }
                        showSearchDialog = false
                    }
                ) {
                    Text("Replace All", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSearchDialog = false }) { Text("Cancel") }
            }
        )
    }

    // AI Modal
    if (showAiModal) {
        AlertDialog(
            onDismissRequest = { showAiModal = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFA855F7))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(aiModalTitle, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                if (isAiLoading) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Processing text with Gemini AI...")
                    }
                } else {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text(aiResultContent, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            },
            confirmButton = {
                Row {
                    if (!isAiLoading && aiResultContent.isNotBlank()) {
                        TextButton(
                            onClick = {
                                contentText = contentText + "\n\n-- AI Generated --\n" + aiResultContent
                                showAiModal = false
                                Toast.makeText(context, "Appended AI content to document!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("Append to Doc", fontWeight = FontWeight.Bold)
                        }
                    }
                    TextButton(onClick = { showAiModal = false }) { Text("Close") }
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClicked) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF2563EB).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(documentTitle, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("$wordCount words • $charCount chars", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Row {
                IconButton(onClick = { showSearchDialog = true }) {
                    Icon(Icons.Default.Search, contentDescription = "Find & Replace")
                }

                IconButton(
                    onClick = {
                        // Export to PDF
                        CoroutineScope(Dispatchers.IO).launch {
                            val pdfFile = PdfEngine.createPdfFromText(context, documentTitle, contentText)
                            val newDoc = ScannedDocument(
                                title = "${documentTitle}_Exported.pdf",
                                filePath = pdfFile.absolutePath,
                                fileType = "PDF",
                                fileSizeBytes = pdfFile.length(),
                                createdAt = System.currentTimeMillis()
                            )
                            viewModel.addNewDocument(newDoc)
                        }
                        Toast.makeText(context, "Document exported to PDF!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF", tint = Color(0xFFEF4444))
                }

                IconButton(
                    onClick = {
                        val updatedDoc = doc.copy(extractedText = contentText)
                        viewModel.saveDocumentChanges(updatedDoc)
                        Toast.makeText(context, "Document saved!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Save", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Formatting Toolbar Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Font Size Selector
            Box {
                Button(
                    onClick = { showFontSizeMenu = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("${selectedFontSize}pt", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                }
                DropdownMenu(expanded = showFontSizeMenu, onDismissRequest = { showFontSizeMenu = false }) {
                    listOf(12, 14, 16, 18, 20, 24, 28, 32).forEach { size ->
                        DropdownMenuItem(
                            text = { Text("${size}pt") },
                            onClick = {
                                selectedFontSize = size
                                showFontSizeMenu = false
                            }
                        )
                    }
                }
            }

            FormatIconButton(Icons.Default.FormatBold, isBold) { isBold = !isBold }
            FormatIconButton(Icons.Default.FormatItalic, isItalic) { isItalic = !isItalic }
            FormatIconButton(Icons.Default.FormatUnderlined, isUnderline) { isUnderline = !isUnderline }
            FormatIconButton(Icons.Default.FormatStrikethrough, isStrikethrough) { isStrikethrough = !isStrikethrough }

            // Alignments
            FormatIconButton(Icons.Default.FormatAlignLeft, alignment == TextAlign.Left) { alignment = TextAlign.Left }
            FormatIconButton(Icons.Default.FormatAlignCenter, alignment == TextAlign.Center) { alignment = TextAlign.Center }
            FormatIconButton(Icons.Default.FormatAlignRight, alignment == TextAlign.Right) { alignment = TextAlign.Right }

            // Bullet & Numbered lists
            FormatIconButton(Icons.Default.FormatListBulleted, false) {
                contentText += "\n• "
            }
            FormatIconButton(Icons.Default.FormatListNumbered, false) {
                contentText += "\n1. "
            }

            // Table & Image
            FormatIconButton(Icons.Default.TableChart, false) { showInsertTableDialog = true }
        }

        // AI Assistant Tools Subbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("AI Tools: ", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA855F7))

            AiActionChip("Summarize", Icons.Default.Psychology) {
                aiModalTitle = "AI Document Summary"
                isAiLoading = true
                showAiModal = true
                viewModel.summarizeDocument(doc) { result ->
                    aiResultContent = result
                    isAiLoading = false
                }
            }

            AiActionChip("Grammar Fix", Icons.Default.Spellcheck) {
                aiModalTitle = "AI Grammar & Polish"
                isAiLoading = true
                showAiModal = true
                viewModel.askAiQuestion("Fix grammar and improve flow of this text:\n\n$contentText") { result ->
                    aiResultContent = result
                    isAiLoading = false
                }
            }

            AiActionChip("Translate", Icons.Default.Translate) {
                aiModalTitle = "AI Translation"
                isAiLoading = true
                showAiModal = true
                viewModel.askAiQuestion("Translate this document into Spanish:\n\n$contentText") { result ->
                    aiResultContent = result
                    isAiLoading = false
                }
            }
        }

        // Editable Document Canvas Page
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFFF1F5F9))
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    BasicTextField(
                        value = contentText,
                        onValueChange = { contentText = it },
                        textStyle = TextStyle(
                            fontSize = selectedFontSize.sp,
                            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                            fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
                            textDecoration = when {
                                isUnderline && isStrikethrough -> TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough))
                                isUnderline -> TextDecoration.Underline
                                isStrikethrough -> TextDecoration.LineThrough
                                else -> TextDecoration.None
                            },
                            textAlign = alignment,
                            color = selectedTextColor,
                            lineHeight = (selectedFontSize * 1.5).sp
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Render Embedded Interactive Tables
                    embeddedTables.forEachIndexed { tableIndex, table ->
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Interactive Table #${tableIndex + 1}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                        ) {
                            for (r in 0 until table.rows) {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    for (c in 0 until table.cols) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .border(0.5.dp, Color.LightGray)
                                                .padding(4.dp)
                                        ) {
                                            BasicTextField(
                                                value = table.cells[r][c],
                                                onValueChange = { table.cells[r][c] = it },
                                                textStyle = TextStyle(fontSize = 12.sp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FormatIconButton(
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun AiActionChip(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFA855F7).copy(alpha = 0.15f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Color(0xFFA855F7), modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA855F7))
        }
    }
}
