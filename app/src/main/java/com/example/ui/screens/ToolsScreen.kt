package com.example.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.BrandingWatermark
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ScannedDocument
import com.example.ui.ScanProViewModel

data class DedicatedToolItem(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
fun ToolsScreen(
    viewModel: ScanProViewModel,
    initialToolId: String? = null,
    onNavigateToDocPreview: (ScannedDocument) -> Unit,
    onNavigateToCamera: () -> Unit
) {
    var activeToolId by remember(initialToolId) { mutableStateOf(initialToolId) }

    // Routing to Dedicated Tool Screens
    if (activeToolId != null) {
        when (activeToolId) {
            "pdf_to_excel" -> PdfToExcelToolScreen(viewModel = viewModel, onBackClicked = { activeToolId = null }, onNavigateToDocPreview = onNavigateToDocPreview)
            "pdf_to_word" -> PdfToWordToolScreen(viewModel = viewModel, onBackClicked = { activeToolId = null }, onNavigateToDocPreview = onNavigateToDocPreview)
            "pdf_to_ppt" -> PdfToPptToolScreen(viewModel = viewModel, onBackClicked = { activeToolId = null }, onNavigateToDocPreview = onNavigateToDocPreview)
            "excel_to_pdf" -> ExcelToPdfToolScreen(viewModel = viewModel, onBackClicked = { activeToolId = null }, onNavigateToDocPreview = onNavigateToDocPreview)
            "word_to_pdf" -> WordToPdfToolScreen(viewModel = viewModel, onBackClicked = { activeToolId = null }, onNavigateToDocPreview = onNavigateToDocPreview)
            "img2pdf" -> ImageToPdfToolScreen(viewModel = viewModel, onBackClicked = { activeToolId = null }, onNavigateToDocPreview = onNavigateToDocPreview)
            "text2pdf" -> TextToPdfToolScreen(viewModel = viewModel, onBackClicked = { activeToolId = null }, onNavigateToDocPreview = onNavigateToDocPreview)
            "ocr" -> OcrTextToolScreen(viewModel = viewModel, onBackClicked = { activeToolId = null }, onNavigateToDocPreview = onNavigateToDocPreview)
            "merge" -> MergePdfToolScreen(viewModel = viewModel, onBackClicked = { activeToolId = null }, onNavigateToDocPreview = onNavigateToDocPreview)
            "split" -> SplitPdfToolScreen(viewModel = viewModel, onBackClicked = { activeToolId = null }, onNavigateToDocPreview = onNavigateToDocPreview)
            "compress" -> CompressPdfToolScreen(viewModel = viewModel, onBackClicked = { activeToolId = null }, onNavigateToDocPreview = onNavigateToDocPreview)
            "watermark" -> WatermarkPdfToolScreen(viewModel = viewModel, onBackClicked = { activeToolId = null }, onNavigateToDocPreview = onNavigateToDocPreview)
            "sign" -> SignPdfToolScreen(viewModel = viewModel, onBackClicked = { activeToolId = null }, onNavigateToDocPreview = onNavigateToDocPreview)
            "protect" -> PasswordProtectToolScreen(viewModel = viewModel, onBackClicked = { activeToolId = null }, onNavigateToDocPreview = onNavigateToDocPreview)
            "qr_scan" -> QrCodeScannerToolScreen(viewModel = viewModel, onBackClicked = { activeToolId = null })
            "barcode_scan" -> BarcodeScannerToolScreen(viewModel = viewModel, onBackClicked = { activeToolId = null })
            "id_card_scan" -> SpecializedScanToolScreen(title = "ID Card Scan", description = "Scan Front & Back sides into single PDF ID", icon = Icons.Default.Badge, viewModel = viewModel, onBackClicked = { activeToolId = null }, onNavigateToDocPreview = onNavigateToDocPreview)
            "book_scan" -> BookScanToolScreen(viewModel = viewModel, onBackClicked = { activeToolId = null }, onNavigateToDocPreview = onNavigateToDocPreview)
            "specialized_scan" -> SpecializedScanToolScreen(title = "Receipt & Invoice Scanner", description = "Expense auto-categorization & receipt crop", icon = Icons.Default.ReceiptLong, viewModel = viewModel, onBackClicked = { activeToolId = null }, onNavigateToDocPreview = onNavigateToDocPreview)
            "scan_doc" -> {
                onNavigateToCamera()
                activeToolId = null
            }
            else -> PdfToWordToolScreen(viewModel = viewModel, onBackClicked = { activeToolId = null }, onNavigateToDocPreview = onNavigateToDocPreview)
        }
        return
    }

    // Comprehensive Tool Catalog Overview
    val allTools = listOf(
        // PDF Conversions
        DedicatedToolItem("pdf_to_excel", "PDF to Excel", "Extract tables into .xlsx / .csv", "CONVERT FROM PDF", Icons.Default.TableChart, Color(0xFF10B981)),
        DedicatedToolItem("pdf_to_word", "PDF to Word", "Transform into editable .docx", "CONVERT FROM PDF", Icons.Default.Description, Color(0xFF2563EB)),
        DedicatedToolItem("pdf_to_ppt", "PDF to PowerPoint", "Convert slides into .pptx decks", "CONVERT FROM PDF", Icons.Default.Slideshow, Color(0xFFF97316)),
        DedicatedToolItem("img2pdf", "Image to PDF", "Combine photos into single PDF", "CONVERT TO PDF", Icons.Default.Image, Color(0xFF0EA5E9)),
        DedicatedToolItem("text2pdf", "Text to PDF", "Format & export text to PDF", "CONVERT TO PDF", Icons.Default.TextFields, Color(0xFF8B5CF6)),
        DedicatedToolItem("excel_to_pdf", "Excel to PDF", "Export spreadsheets to PDF", "CONVERT TO PDF", Icons.Default.TableChart, Color(0xFF059669)),
        DedicatedToolItem("word_to_pdf", "Word to PDF", "Render DOCX into PDF document", "CONVERT TO PDF", Icons.Default.Description, Color(0xFF1D4ED8)),

        // Scanners & AI
        DedicatedToolItem("scan_doc", "Document Scanner", "Live camera scanner with edge AI", "CAMERA & SCAN", Icons.Default.DocumentScanner, Color(0xFF06B6D4)),
        DedicatedToolItem("ocr", "OCR Text Recognizer", "Extract editable text with Gemini", "CAMERA & SCAN", Icons.Default.AutoAwesome, Color(0xFFA855F7)),
        DedicatedToolItem("qr_scan", "QR Code Scanner", "Fast camera payload reader", "CAMERA & SCAN", Icons.Default.QrCodeScanner, Color(0xFF3B82F6)),
        DedicatedToolItem("barcode_scan", "Barcode Scanner", "Read product UPC & barcodes", "CAMERA & SCAN", Icons.Default.QrCodeScanner, Color(0xFF6366F1)),
        DedicatedToolItem("id_card_scan", "ID Card Scan", "Combine Front + Back card scan", "CAMERA & SCAN", Icons.Default.Badge, Color(0xFFEC4899)),
        DedicatedToolItem("book_scan", "Book & Curve Scan", "Flatten pages & remove thumb", "CAMERA & SCAN", Icons.Default.Book, Color(0xFF8B5CF6)),

        // PDF Utilities & Security
        DedicatedToolItem("merge", "Merge PDFs", "Combine multiple PDF files", "PDF UTILITIES", Icons.Default.CallMerge, Color(0xFFEC4899)),
        DedicatedToolItem("split", "Split PDF", "Extract custom page ranges", "PDF UTILITIES", Icons.Default.CallSplit, Color(0xFF0284C7)),
        DedicatedToolItem("compress", "Compress PDF", "Shrink size up to 70%", "PDF UTILITIES", Icons.Default.Compress, Color(0xFF14B8A6)),
        DedicatedToolItem("watermark", "Watermark PDF", "Stamp custom text security overlay", "SECURITY", Icons.Default.BrandingWatermark, Color(0xFF22D3EE)),
        DedicatedToolItem("sign", "Sign PDF", "Draw e-signature & timestamp", "SECURITY", Icons.Default.Draw, Color(0xFF6366F1)),
        DedicatedToolItem("protect", "Protect PDF", "Encrypt PDF with PIN passcode", "SECURITY", Icons.Default.Lock, Color(0xFFF59E0B))
    )

    val groupedTools = allTools.groupBy { it.category }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Professional Tool Suite",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Dedicated workflow screen & options for every utility",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        groupedTools.forEach { (category, toolsList) ->
            Text(
                text = category,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                toolsList.chunked(2).forEach { rowTools ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowTools.forEach { tool ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(125.dp)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                                    .clickable { activeToolId = tool.id }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(tool.color.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(tool.icon, contentDescription = null, tint = tool.color, modifier = Modifier.size(20.dp))
                                    }

                                    Column {
                                        Text(tool.title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(tool.description, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                                    }
                                }
                            }
                        }
                        if (rowTools.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}
