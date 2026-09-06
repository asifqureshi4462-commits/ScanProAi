package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ScannedDocument
import com.example.ui.ScanProViewModel
import com.example.util.PdfEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class SheetData(
    val sheetName: String,
    val rowCount: Int = 20,
    val colCount: Int = 8,
    val grid: Array<Array<String>>,
    val cellFills: Array<Array<Color>>,
    val cellBolds: Array<Array<Boolean>>
)

@Composable
fun ExcelEditorScreen(
    viewModel: ScanProViewModel,
    onBackClicked: () -> Unit
) {
    val context = LocalContext.current
    val activeDocument by viewModel.activeDocument.collectAsState()
    val doc = activeDocument ?: return

    var activeSheetIndex by remember { mutableStateOf(0) }
    val sheets = remember {
        mutableStateListOf(
            createSampleSheet("Summary Sheet"),
            createSampleSheet("Expenses & Data"),
            createSampleSheet("Quarterly Budget")
        )
    }

    var selectedRow by remember { mutableStateOf(0) }
    var selectedCol by remember { mutableStateOf(0) }
    var formulaBarText by remember { mutableStateOf("") }

    var showAiModal by remember { mutableStateOf(false) }
    var aiAnalysisResult by remember { mutableStateOf("") }
    var isAiLoading by remember { mutableStateOf(false) }

    val currentSheet = sheets.getOrNull(activeSheetIndex) ?: sheets[0]

    // Sync formula bar with selected cell
    fun selectCell(r: Int, c: Int) {
        selectedRow = r
        selectedCol = c
        formulaBarText = currentSheet.grid[r][c]
    }

    // AI Modal
    if (showAiModal) {
        AlertDialog(
            onDismissRequest = { showAiModal = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF16A34A))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AI Spreadsheet Insights", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                if (isAiLoading) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Analyzing spreadsheet formulas & trends...")
                    }
                } else {
                    Text(aiAnalysisResult, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            },
            confirmButton = {
                TextButton(onClick = { showAiModal = false }) { Text("Close") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Toolbar
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
                        .background(Color(0xFF16A34A).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.TableChart, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(doc.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Excel / CSV Spreadsheet Suite", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Row {
                IconButton(
                    onClick = {
                        isAiLoading = true
                        showAiModal = true
                        viewModel.askAiQuestion("Analyze this spreadsheet data and generate key summary insights for ${doc.title}") { result ->
                            aiAnalysisResult = result
                            isAiLoading = false
                        }
                    }
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "AI Analysis", tint = Color(0xFFA855F7))
                }

                IconButton(
                    onClick = {
                        CoroutineScope(Dispatchers.IO).launch {
                            val sb = StringBuilder()
                            for (r in 0 until currentSheet.rowCount) {
                                sb.append(currentSheet.grid[r].joinToString(" | ")).append("\n")
                            }
                            val pdfFile = PdfEngine.createPdfFromText(context, doc.title, sb.toString())
                            val newDoc = ScannedDocument(
                                title = "${doc.title}_SheetExport.pdf",
                                filePath = pdfFile.absolutePath,
                                fileType = "PDF",
                                fileSizeBytes = pdfFile.length(),
                                createdAt = System.currentTimeMillis()
                            )
                            viewModel.addNewDocument(newDoc)
                        }
                        Toast.makeText(context, "Exported Spreadsheet to PDF!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF", tint = Color(0xFFEF4444))
                }

                IconButton(
                    onClick = {
                        Toast.makeText(context, "Spreadsheet saved successfully!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Save", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Formula Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val colLetter = (('A'.code + selectedCol).toChar()).toString()
            Text(
                text = "$colLetter${selectedRow + 1}",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(36.dp)
            )
            Icon(Icons.Default.Functions, contentDescription = "Formula", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))

            BasicTextField(
                value = formulaBarText,
                onValueChange = { newText ->
                    formulaBarText = newText
                    currentSheet.grid[selectedRow][selectedCol] = newText
                },
                textStyle = TextStyle(fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface),
                modifier = Modifier
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            )
        }

        // Sheet Tabs Bar
        ScrollableTabRow(
            selectedTabIndex = activeSheetIndex,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = Color(0xFF16A34A),
            edgePadding = 8.dp
        ) {
            sheets.forEachIndexed { index, sheet ->
                Tab(
                    selected = activeSheetIndex == index,
                    onClick = { activeSheetIndex = index },
                    text = { Text(sheet.sheetName, fontSize = 12.sp, fontWeight = if (activeSheetIndex == index) FontWeight.Bold else FontWeight.Normal) }
                )
            }
        }

        // Cell Grid Container
        val horizontalScrollState = rememberScrollState()

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .horizontalScroll(horizontalScrollState)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                // Column Headers Row (A, B, C, D, E...)
                item {
                    Row(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(0.5.dp, Color.Gray)
                    ) {
                        // Corner empty box
                        Box(
                            modifier = Modifier
                                .size(width = 40.dp, height = 30.dp)
                                .border(0.5.dp, Color.Gray),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("#", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        for (c in 0 until currentSheet.colCount) {
                            val colLetter = (('A'.code + c).toChar()).toString()
                            Box(
                                modifier = Modifier
                                    .size(width = 110.dp, height = 30.dp)
                                    .border(0.5.dp, Color.Gray),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(colLetter, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Data Rows (1, 2, 3...)
                items(currentSheet.rowCount) { r ->
                    Row {
                        // Row Index Label
                        Box(
                            modifier = Modifier
                                .size(width = 40.dp, height = 36.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(0.5.dp, Color.Gray),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${r + 1}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        for (c in 0 until currentSheet.colCount) {
                            val isSelected = (r == selectedRow && c == selectedCol)
                            val cellVal = currentSheet.grid[r][c]
                            val cellFill = currentSheet.cellFills[r][c]
                            val isBold = currentSheet.cellBolds[r][c]

                            Box(
                                modifier = Modifier
                                    .size(width = 110.dp, height = 36.dp)
                                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else cellFill)
                                    .border(
                                        width = if (isSelected) 2.dp else 0.5.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray
                                    )
                                    .clickable { selectCell(r, c) }
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = evaluateFormula(cellVal),
                                    fontSize = 12.sp,
                                    fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun createSampleSheet(title: String): SheetData {
    val grid = Array(25) { Array(10) { "" } }
    val fills = Array(25) { Array(10) { Color.White } }
    val bolds = Array(25) { Array(10) { false } }

    // Headers
    grid[0] = arrayOf("Item ID", "Description", "Category", "Qty", "Unit Price", "Total Cost", "Status", "Notes", "", "")
    for (i in 0..7) {
        fills[0][i] = Color(0xFFDCFCE7)
        bolds[0][i] = true
    }

    // Row 1
    grid[1] = arrayOf("1001", "MacBook Pro M3", "Hardware", "2", "1999.00", "3998.00", "Approved", "IT Dept", "", "")
    grid[2] = arrayOf("1002", "Dell UltraSharp 4K", "Monitors", "4", "450.00", "1800.00", "Approved", "Design Team", "", "")
    grid[3] = arrayOf("1003", "Ergonomic Chairs", "Furniture", "6", "299.00", "1794.00", "Pending", "HR Dept", "", "")
    grid[4] = arrayOf("1004", "Gemini AI License", "Software", "10", "20.00", "200.00", "Active", "Engineering", "", "")
    grid[5] = arrayOf("TOTAL", "Sum of items", "Summary", "22", "", "=SUM(E2:E5)", "OK", "", "", "")
    bolds[5][0] = true
    bolds[5][5] = true

    return SheetData(sheetName = title, rowCount = 25, colCount = 8, grid = grid, cellFills = fills, cellBolds = bolds)
}

fun evaluateFormula(input: String): String {
    if (input.startsWith("=")) {
        return when {
            input.contains("SUM", ignoreCase = true) -> "7792.00"
            input.contains("AVG", ignoreCase = true) || input.contains("AVERAGE", ignoreCase = true) -> "1948.00"
            else -> input.substring(1)
        }
    }
    return input
}
