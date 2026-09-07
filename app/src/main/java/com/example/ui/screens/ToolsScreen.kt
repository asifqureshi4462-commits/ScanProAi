package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Translate
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

    // Intercept hardware/system back press: Closes the active tool instead of exiting the app
    BackHandler(enabled = activeToolId != null) {
        activeToolId = null
    }

    // Routing to Dedicated Tool Screens
    if (activeToolId != null) {
        when (activeToolId) {
            "pdf_to_excel" -> PdfToExcelToolScreen(viewModel = viewModel, onBackClicked = { activeToolId = null }, onNavigateToDocPreview = onNavigateToDocPreview)
            "pdf_to_word" -> PdfToWordToolScreen(viewModel = viewModel, onBackClicked = { activeToolId = null }, onNavigateToDocPreview = onNavigateToDocPreview)
            "pdf_to_ppt" -> PdfToPptToolScreen(viewModel = viewModel, onBackClicked = { activeToolId = null }, onNavigateToDocPreview = onNavigateToDocPreview)
            "excel_to_pdf" -> ExcelToPdfToolScreen(viewModel = viewModel, onBackClicked = { activeToolId = null }, onNavigateToDocPreview = onNavigateToDocPreview)
            "word_to_pdf" -> WordToPdfToolScreen(viewModel = viewModel, onBackClicked = { activeToolId = null }, onNavigateToDocPreview = onNavigateToDocPreview)
            "img2pdf" -> ImageToPdfToolScreen(viewModel = viewModel, onBackClicked = { activeToolId = null }, onNavigateToDocPreview = onNavigateToDocPreview)
            "text2pdf" -> ImageToPdfToolScreen(viewModel = viewModel, onBackClicked = { activeToolId = null }, onNavigateToDocPreview = onNavigateToDocPreview)
            "ocr", "ocr_pdf", "ocr_image", "extract_text" -> OcrTextToolScreen(viewModel = viewModel, onBackClicked = { activeToolId = null }, onNavigateToDocPreview = onNavigateToDocPreview)
            "merge" -> MergePdfToolScreen(viewModel = viewModel, onBackClicked = { activeToolId = null }, onNavigateToDocPreview = onNavigateToDocPreview)
            "split", "delete_pages", "rearrange_pages" -> SplitPdfToolScreen(viewModel = viewModel, onBackClicked = { activeToolId = null }, onNavigateToDocPreview = onNavigateToDocPreview)
            "compress" -> CompressPdfToolScreen(viewModel = viewModel, onBackClicked = { activeToolId = null }, onNavigateToDocPreview = onNavigateToDocPreview)
            "watermark" -> WatermarkPdfToolScreen(viewModel = viewModel, onBackClicked = { activeToolId = null }, onNavigateToDocPreview = onNavigateToDocPreview)
            "sign" -> SignPdfToolScreen(viewModel = viewModel, onBackClicked = { activeToolId = null }, onNavigateToDocPreview = onNavigateToDocPreview)
            "protect", "unlock" -> PasswordProtectToolScreen(viewModel = viewModel, onBackClicked = { activeToolId = null }, onNavigateToDocPreview = onNavigateToDocPreview)
            "qr_scan" -> QrCodeScannerToolScreen(viewModel = viewModel, onBackClicked = { activeToolId = null })
            "barcode_scan" -> BarcodeScannerToolScreen(viewModel = viewModel, onBackClicked = { activeToolId = null })
            "id_card_scan", "passport_scan" -> SpecializedScanToolScreen(title = "ID & Passport Scanner", description = "Scan Front & Back sides into single PDF ID", icon = Icons.Default.Badge, viewModel = viewModel, onBackClicked = { activeToolId = null }, onNavigateToDocPreview = onNavigateToDocPreview)
            "book_scan" -> BookScanToolScreen(viewModel = viewModel, onBackClicked = { activeToolId = null }, onNavigateToDocPreview = onNavigateToDocPreview)
            "specialized_scan", "receipt_scan" -> SpecializedScanToolScreen(title = "Receipt & Invoice Scanner", description = "Expense auto-categorization & receipt crop", icon = Icons.Default.ReceiptLong, viewModel = viewModel, onBackClicked = { activeToolId = null }, onNavigateToDocPreview = onNavigateToDocPreview)
            "scan_doc", "scan_whiteboard", "scan_formula", "scan_timestamp" -> {
                onNavigateToCamera()
                activeToolId = null
            }
            else -> PdfToWordToolScreen(viewModel = viewModel, onBackClicked = { activeToolId = null }, onNavigateToDocPreview = onNavigateToDocPreview)
        }
        return
    }

    // Comprehensive Tool Catalog Overview
    val allTools = listOf(
        // 1. SCAN
        DedicatedToolItem("scan_doc", "Document Scan", "Live edge detection camera scanner", "SCAN", Icons.Default.DocumentScanner, Color(0xFF06B6D4)),
        DedicatedToolItem("id_card_scan", "ID Card Scan", "Front & Back auto alignment", "SCAN", Icons.Default.Badge, Color(0xFFEC4899)),
        DedicatedToolItem("passport_scan", "Passport Scan", "MRZ booklet & photo page scan", "SCAN", Icons.Default.Badge, Color(0xFF3B82F6)),
        DedicatedToolItem("book_scan", "Book Scan", "Curve flattening & thumb removal", "SCAN", Icons.Default.Book, Color(0xFF8B5CF6)),
        DedicatedToolItem("scan_whiteboard", "Whiteboard", "Reflection & glare reduction", "SCAN", Icons.Default.CameraAlt, Color(0xFF10B981)),
        DedicatedToolItem("receipt_scan", "Receipt Scanner", "Expense receipt auto crop & total", "SCAN", Icons.Default.ReceiptLong, Color(0xFFF97316)),
        DedicatedToolItem("scan_formula", "Formula Reader", "Math & physics expression scan", "SCAN", Icons.Default.Functions, Color(0xFF6366F1)),
        DedicatedToolItem("scan_timestamp", "Timestamp Camera", "Burn GPS & datetime stamp", "SCAN", Icons.Default.Timer, Color(0xFF14B8A6)),

        // 2. OCR
        DedicatedToolItem("extract_text", "Extract Text", "Extract editable text with Gemini", "OCR", Icons.Default.AutoAwesome, Color(0xFFA855F7)),
        DedicatedToolItem("ocr_pdf", "OCR PDF", "Make scanned PDF searchable", "OCR", Icons.Default.PictureAsPdf, Color(0xFFEF4444)),
        DedicatedToolItem("ocr_image", "OCR Image", "Batch image text recognition", "OCR", Icons.Default.Image, Color(0xFF0EA5E9)),
        DedicatedToolItem("translate_text", "Translate Text", "Multi-language document translation", "OCR", Icons.Default.Translate, Color(0xFF3B82F6)),
        DedicatedToolItem("ai_summary", "AI Summary", "Smart document summarization", "OCR", Icons.Default.Psychology, Color(0xFF8B5CF6)),

        // 3. PDF
        DedicatedToolItem("merge", "Merge PDFs", "Combine multiple PDF files", "PDF", Icons.Default.CallMerge, Color(0xFFEC4899)),
        DedicatedToolItem("split", "Split PDF", "Extract custom page ranges", "PDF", Icons.Default.CallSplit, Color(0xFF0284C7)),
        DedicatedToolItem("compress", "Compress PDF", "Shrink size up to 70%", "PDF", Icons.Default.Compress, Color(0xFF14B8A6)),
        DedicatedToolItem("protect", "Protect PDF", "Encrypt PDF with PIN passcode", "PDF", Icons.Default.Lock, Color(0xFFF59E0B)),
        DedicatedToolItem("unlock", "Unlock PDF", "Remove PDF password protection", "PDF", Icons.Default.LockOpen, Color(0xFF10B981)),
        DedicatedToolItem("watermark", "Watermark PDF", "Stamp custom security text overlay", "PDF", Icons.Default.BrandingWatermark, Color(0xFF22D3EE)),
        DedicatedToolItem("rotate", "Rotate Pages", "Adjust page orientation", "PDF", Icons.Default.RotateRight, Color(0xFF6366F1)),
        DedicatedToolItem("delete_pages", "Delete Pages", "Remove unwanted pages", "PDF", Icons.Default.DeleteSweep, Color(0xFFEF4444)),
        DedicatedToolItem("rearrange_pages", "Rearrange Pages", "Reorder PDF page sequence", "PDF", Icons.Default.FormatListNumbered, Color(0xFF8B5CF6)),

        // 4. CONVERT
        DedicatedToolItem("pdf_to_word", "PDF → Word", "Convert to editable .docx", "CONVERT", Icons.Default.Description, Color(0xFF2563EB)),
        DedicatedToolItem("pdf_to_excel", "PDF → Excel", "Extract tables into .xlsx / .csv", "CONVERT", Icons.Default.TableChart, Color(0xFF10B981)),
        DedicatedToolItem("pdf_to_ppt", "PDF → PPT", "Convert slides into .pptx decks", "CONVERT", Icons.Default.Slideshow, Color(0xFFF97316)),
        DedicatedToolItem("pdf_to_jpg", "PDF → JPG", "Render pages as HD image files", "CONVERT", Icons.Default.Image, Color(0xFFEC4899)),
        DedicatedToolItem("img2pdf", "JPG → PDF", "Combine photos into single PDF", "CONVERT", Icons.Default.PictureAsPdf, Color(0xFF0EA5E9)),
        DedicatedToolItem("word_to_pdf", "Word → PDF", "Render DOCX into PDF document", "CONVERT", Icons.Default.Description, Color(0xFF1D4ED8)),
        DedicatedToolItem("excel_to_pdf", "Excel → PDF", "Export spreadsheets to PDF", "CONVERT", Icons.Default.TableChart, Color(0xFF059669)),

        // 5. AI
        DedicatedToolItem("ai_chat", "AI Chat with PDF", "Ask questions & query document", "AI", Icons.Default.Chat, Color(0xFF3B82F6)),
        DedicatedToolItem("ai_notes", "AI Notes Generator", "Generate structured study notes", "AI", Icons.Default.EditNote, Color(0xFFA855F7)),
        DedicatedToolItem("ai_rewrite", "AI Rewrite", "Tone adjustment & grammar polish", "AI", Icons.Default.AutoAwesome, Color(0xFF10B981)),
        DedicatedToolItem("ai_translator", "AI Translation", "Context-aware neural translation", "AI", Icons.Default.Translate, Color(0xFFF59E0B)),
        DedicatedToolItem("ai_handwritten", "AI Handwritten Notes", "Transcribe handwritten scripts", "AI", Icons.Default.Draw, Color(0xFFEC4899)),
        DedicatedToolItem("ai_analyzer", "AI Document Analyzer", "Key insights & entity extraction", "AI", Icons.Default.Psychology, Color(0xFF6366F1))
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

        // Bottom spacer to ensure scrolling above bottom navigation bar
        Spacer(modifier = Modifier.height(80.dp))
    }
}