package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.CropFree
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
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

    BackHandler(enabled = activeToolId != null) {
        activeToolId = null
    }

    if (activeToolId != null) {
        when (activeToolId) {
            "id_photo_maker" -> IdPhotoMakerToolScreen(viewModel = viewModel, onBackClicked = { activeToolId = null }, onNavigateToDocPreview = onNavigateToDocPreview)
            "count_cam" -> CountCamToolScreen(viewModel = viewModel, onBackClicked = { activeToolId = null })
            "smart_eraser" -> SmartEraserToolScreen(viewModel = viewModel, onBackClicked = { activeToolId = null })
            "timestamp" -> TimestampToolScreen(viewModel = viewModel, onBackClicked = { activeToolId = null })

            "excel_to_pdf" -> ExcelToPdfToolScreen(viewModel = viewModel, onBackClicked = { activeToolId = null }, onNavigateToDocPreview = onNavigateToDocPreview)
            "word_to_pdf" -> WordToPdfToolScreen(viewModel = viewModel, onBackClicked = { activeToolId = null }, onNavigateToDocPreview = onNavigateToDocPreview)
            "img2pdf" -> ImageToPdfToolScreen(viewModel = viewModel, onBackClicked = { activeToolId = null }, onNavigateToDocPreview = onNavigateToDocPreview)
            "text2pdf" -> TextToPdfToolScreen(viewModel = viewModel, onBackClicked = { activeToolId = null }, onNavigateToDocPreview = onNavigateToDocPreview)
            "ocr", "ocr_pdf", "ocr_image", "extract_text", "question_set" -> OcrTextToolScreen(viewModel = viewModel, onBackClicked = { activeToolId = null }, onNavigateToDocPreview = onNavigateToDocPreview)
            "merge" -> PdfMergeScreen(viewModel = viewModel, onBackClicked = { activeToolId = null }, onNavigateToDocPreview = onNavigateToDocPreview)
            "split" -> PdfSplitScreen(viewModel = viewModel, onBackClicked = { activeToolId = null }, onNavigateToDocPreview = onNavigateToDocPreview)
            "delete_pages" -> PdfDeletePagesScreen(viewModel = viewModel, onBackClicked = { activeToolId = null }, onNavigateToDocPreview = onNavigateToDocPreview)
            "rearrange_pages" -> PdfRearrangeScreen(viewModel = viewModel, onBackClicked = { activeToolId = null }, onNavigateToDocPreview = onNavigateToDocPreview)
            "rotate" -> PdfRotateScreen(viewModel = viewModel, onBackClicked = { activeToolId = null }, onNavigateToDocPreview = onNavigateToDocPreview)
            "compress" -> PdfCompressScreen(viewModel = viewModel, onBackClicked = { activeToolId = null }, onNavigateToDocPreview = onNavigateToDocPreview)
            "watermark" -> PdfWatermarkScreen(viewModel = viewModel, onBackClicked = { activeToolId = null }, onNavigateToDocPreview = onNavigateToDocPreview)
            "protect" -> PdfProtectScreen(viewModel = viewModel, onBackClicked = { activeToolId = null }, onNavigateToDocPreview = onNavigateToDocPreview)
            "unlock" -> PdfUnlockScreen(viewModel = viewModel, onBackClicked = { activeToolId = null }, onNavigateToDocPreview = onNavigateToDocPreview)
            "pdf_to_jpg" -> PdfToImagesScreen(viewModel = viewModel, onBackClicked = { activeToolId = null }, onNavigateToDocPreview = onNavigateToDocPreview)
            "sign" -> SignPdfToolScreen(viewModel = viewModel, onBackClicked = { activeToolId = null }, onNavigateToDocPreview = onNavigateToDocPreview)
            "qr_scan", "scan_code" -> QrCodeScannerToolScreen(viewModel = viewModel, onBackClicked = { activeToolId = null })
            "barcode_scan" -> BarcodeScannerToolScreen(viewModel = viewModel, onBackClicked = { activeToolId = null })
            "id_cards", "id_card_scan", "passport_scan" -> SpecializedScanToolScreen(title = "ID & Passport Scanner", description = "Scan Front & Back sides into single PDF ID", icon = Icons.Default.Badge, viewModel = viewModel, onBackClicked = { activeToolId = null }, onNavigateToDocPreview = onNavigateToDocPreview)
            "book", "book_scan" -> BookScanToolScreen(viewModel = viewModel, onBackClicked = { activeToolId = null }, onNavigateToDocPreview = onNavigateToDocPreview)
            "receipt_scan" -> SpecializedScanToolScreen(title = "Receipt & Invoice Scanner", description = "Expense auto-categorization & receipt crop", icon = Icons.Default.ReceiptLong, viewModel = viewModel, onBackClicked = { activeToolId = null }, onNavigateToDocPreview = onNavigateToDocPreview)
            "whiteboard", "scan_whiteboard" -> SpecializedScanToolScreen(title = "Whiteboard Capture", description = "Glare & shadow reduction for boards", icon = Icons.Default.CameraAlt, viewModel = viewModel, onBackClicked = { activeToolId = null }, onNavigateToDocPreview = onNavigateToDocPreview)
            "slides" -> {
                onNavigateToCamera()
                activeToolId = null
            }
            "scan_doc" -> {
                onNavigateToCamera()
                activeToolId = null
            }
            "ai_chat" -> {
                activeToolId = null
            }
            else -> {
                OcrTextToolScreen(viewModel = viewModel, onBackClicked = { activeToolId = null }, onNavigateToDocPreview = onNavigateToDocPreview)
            }
        }
        return
    }

    val allTools = listOf(
        DedicatedToolItem("scan_doc", "Document Scan", "Live edge detection camera scanner", "SCAN", Icons.Default.DocumentScanner, Color(0xFF06B6D4)),
        DedicatedToolItem("id_cards", "ID Cards", "Front & Back auto alignment", "SCAN", Icons.Default.Badge, Color(0xFFEC4899)),
        DedicatedToolItem("id_photo_maker", "ID Photo Maker", "Standard passport photo print sheet", "SCAN", Icons.Default.Badge, Color(0xFFA855F7)),
        DedicatedToolItem("timestamp", "Timestamp Stamp", "Burn date, time & GPS on scans", "SCAN", Icons.Default.Timer, Color(0xFFF97316)),
        DedicatedToolItem("book", "Book Scan", "Curve flattening & thumb removal", "SCAN", Icons.Default.Book, Color(0xFF8B5CF6)),
        DedicatedToolItem("whiteboard", "Whiteboard", "Reflection & glare reduction", "SCAN", Icons.Default.CameraAlt, Color(0xFF10B981)),
        DedicatedToolItem("slides", "Slides", "Presentation slide sequence scan", "SCAN", Icons.Default.Slideshow, Color(0xFF2563EB)),

        DedicatedToolItem("extract_text", "Extract Text", "Extract editable text with Gemini", "OCR", Icons.Default.AutoAwesome, Color(0xFFA855F7)),
        DedicatedToolItem("question_set", "Question Set", "Extract questions from test papers", "OCR", Icons.Default.Description, Color(0xFF16C7B5)),
        DedicatedToolItem("ocr_pdf", "OCR PDF", "Make scanned PDF searchable", "OCR", Icons.Default.PictureAsPdf, Color(0xFFEF4444)),
        DedicatedToolItem("translate_text", "Translate Text", "Multi-language document translation", "OCR", Icons.Default.Translate, Color(0xFF3B82F6)),

        DedicatedToolItem("merge", "Merge PDFs", "Combine multiple PDF files", "PDF", Icons.Default.CallMerge, Color(0xFFEC4899)),
        DedicatedToolItem("split", "Split PDF", "Extract custom page ranges", "PDF", Icons.Default.CallSplit, Color(0xFF0284C7)),
        DedicatedToolItem("compress", "Compress PDF", "Shrink size up to 70%", "PDF", Icons.Default.Compress, Color(0xFF14B8A6)),
        DedicatedToolItem("protect", "Protect PDF", "Encrypt PDF with PIN passcode", "PDF", Icons.Default.Lock, Color(0xFFF59E0B)),
        DedicatedToolItem("unlock", "Unlock PDF", "Remove PDF password protection", "PDF", Icons.Default.LockOpen, Color(0xFF10B981)),
        DedicatedToolItem("watermark", "Watermark PDF", "Stamp custom security text overlay", "PDF", Icons.Default.BrandingWatermark, Color(0xFF22D3EE)),
        DedicatedToolItem("rotate", "Rotate Pages", "Adjust page orientation", "PDF", Icons.Default.RotateRight, Color(0xFF6366F1)),
        DedicatedToolItem("delete_pages", "Delete Pages", "Remove unwanted pages", "PDF", Icons.Default.DeleteSweep, Color(0xFFEF4444)),
        DedicatedToolItem("rearrange_pages", "Rearrange Pages", "Reorder PDF page sequence", "PDF", Icons.Default.FormatListNumbered, Color(0xFF8B5CF6)),
        DedicatedToolItem("sign", "Sign PDF", "Draw e-signature onto document", "PDF", Icons.Default.Draw, Color(0xFFEC4899)),

        DedicatedToolItem("smart_eraser", "Smart Eraser", "Inpaint & remove unwanted marks", "ENHANCE", Icons.Default.AutoAwesome, Color(0xFF10B981)),
        DedicatedToolItem("count_cam", "CountCam", "Count objects via camera touch", "ENHANCE", Icons.Default.CropFree, Color(0xFFA855F7)),
        DedicatedToolItem("pdf_to_jpg", "PDF → JPG", "Render pages as HD image files", "CONVERT", Icons.Default.Image, Color(0xFFEC4899)),
        DedicatedToolItem("img2pdf", "JPG → PDF", "Combine photos into single PDF", "CONVERT", Icons.Default.PictureAsPdf, Color(0xFF0EA5E9)),
        DedicatedToolItem("word_to_pdf", "Word → PDF", "Render DOCX into PDF document", "CONVERT", Icons.Default.Description, Color(0xFF1D4ED8)),
        DedicatedToolItem("excel_to_pdf", "Excel → PDF", "Export spreadsheets to PDF", "CONVERT", Icons.Default.TableChart, Color(0xFF059669)),

        DedicatedToolItem("scan_code", "Scan Code", "QR & Barcode scanner", "UTILITIES", Icons.Default.QrCodeScanner, Color(0xFF16C7B5))
    )

    var query by remember { mutableStateOf("") }
    val visibleTools = remember(query) {
        val q = query.trim()
        if (q.isEmpty()) allTools
        else allTools.filter { it.title.contains(q, ignoreCase = true) || it.description.contains(q, ignoreCase = true) }
    }
    val groupedTools = visibleTools.groupBy { it.category }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Tools",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Pick a tool. Everything runs directly on your device.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search tools (sign, count, id photo, eraser...)") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (visibleTools.isEmpty()) {
            Text(
                text = "No tool found for \"$query\".",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 24.dp)
            )
        }

        groupedTools.forEach { (category, toolsList) ->
            Text(
                text = category,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                toolsList.chunked(2).forEach { rowTools ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowTools.forEach { tool ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .heightIn(min = 118.dp)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
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
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(tool.color.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(tool.icon, contentDescription = null, tint = tool.color, modifier = Modifier.size(22.dp))
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Column {
                                        Text(
                                            tool.title,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 14.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            tool.description,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis
                                        )
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
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
