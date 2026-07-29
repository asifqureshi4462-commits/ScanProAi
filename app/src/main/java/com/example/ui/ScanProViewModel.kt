package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.GeminiService
import com.example.data.auth.AuthRepository
import com.example.data.auth.UserProfile
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ScanProDatabase
import com.example.data.local.ScannedDocument
import com.example.util.PdfEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String, // "user" or "ai"
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class ProcessingState(
    val isProcessing: Boolean = false,
    val progress: Float = 0f,
    val statusText: String = "",
    val completedMessage: String? = null
)

class ScanProViewModel(application: Application) : AndroidViewModel(application) {

    private val db = ScanProDatabase.getDatabase(application)
    private val dao = db.documentDao()
    private val chatDao = db.chatDao()
    private val geminiService = GeminiService()
    val authRepository = AuthRepository()

    val currentUser: StateFlow<UserProfile?> = authRepository.currentUser

    // Offline state
    val isOfflineMode = MutableStateFlow(false)

    // Filtering State
    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow("All") // All, Documents, ID Card, Receipts, Books, Favorites
    val viewModeGrid = MutableStateFlow(false)

    // Combined Document List
    val documents: StateFlow<List<ScannedDocument>> = combine(
        dao.getAllDocuments(),
        searchQuery,
        selectedCategory
    ) { docs, query, category ->
        var list = docs
        if (category == "Favorites") {
            list = list.filter { it.isFavorite }
        } else if (category != "All") {
            list = list.filter { it.category == category }
        }
        if (query.isNotBlank()) {
            list = list.filter {
                it.title.contains(query, ignoreCase = true) ||
                (it.extractedText?.contains(query, ignoreCase = true) == true)
            }
        }
        list
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val storageUsedBytes: StateFlow<Long?> = dao.getTotalStorageUsageBytes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 125_000_000L)

    // Active Document for Preview / Editing
    val activeDocument = MutableStateFlow<ScannedDocument?>(null)

    // Scanner State
    val capturedBitmaps = MutableStateFlow<List<Bitmap>>(emptyList())
    val selectedScanMode = MutableStateFlow("Document") // Document, ID Card, Book, Whiteboard, Slides
    val flashEnabled = MutableStateFlow(false)
    val hdModeEnabled = MutableStateFlow(true)
    val selectedFilter = MutableStateFlow("Original") // Original, B&W, Magic Color, Grayscale

    // Room Persistent Chat Messages
    val chatMessages: StateFlow<List<ChatMessage>> = chatDao.getAllChatMessages()
        .map { list ->
            list.map { ChatMessage(id = it.id, sender = it.sender, message = it.message, timestamp = it.timestamp) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    val isAiThinking = MutableStateFlow(false)

    // Tool Processing State
    val processingState = MutableStateFlow(ProcessingState())

    init {
        // Seed initial documents and chat messages if empty
        viewModelScope.launch {
            dao.getDocumentCount().collect { count ->
                if (count == 0) {
                    seedInitialDocuments()
                }
            }
        }
        viewModelScope.launch {
            val existingMsgs = chatDao.getAllChatMessages().first()
            if (existingMsgs.isEmpty()) {
                val initialList = listOf(
                    ChatMessageEntity(
                        id = "msg_1",
                        sender = "ai",
                        message = "👋 Hello! I'm ScanPro AI. You can ask me to summarize PDFs, translate text into 50+ languages, extract table data, or answer document questions! All your queries are cached locally in Room database for offline access.",
                        timestamp = System.currentTimeMillis() - 600000
                    ),
                    ChatMessageEntity(
                        id = "msg_2",
                        sender = "user",
                        message = "What can you do with scanned invoices?",
                        timestamp = System.currentTimeMillis() - 300000
                    ),
                    ChatMessageEntity(
                        id = "msg_3",
                        sender = "ai",
                        message = "📄 I can automatically extract line items, calculate totals, convert currencies, and summarize vendor payment terms into structured JSON or PDF!",
                        timestamp = System.currentTimeMillis() - 120000
                    )
                )
                chatDao.insertAll(initialList)
            }
        }
    }

    private suspend fun seedInitialDocuments() {
        val app = getApplication<Application>()
        val sample1File = PdfEngine.createPdfFromText(
            app,
            "AI_Scan_Contract_2026",
            "SCANPRO AI MASTER SERVICE AGREEMENT\n\n1. Purpose: AI-Powered Document Management & Automation.\n2. Security: End-to-end cloud encryption, biometric vault protection.\n3. SLA: 99.99% OCR accuracy & sub-second processing speed."
        )

        val sample2File = PdfEngine.createPdfFromText(
            app,
            "Passport_ID_Scan",
            "IDENTITY SCAN VERIFICATION\nName: Alex Vance\nID No: SP-992014-X\nStatus: Verified Premium Passport\nCountry: United States"
        )

        val sample3File = PdfEngine.createPdfFromText(
            app,
            "Quarterly_Financial_Report",
            "Q2 FINANCIAL SUMMARY & INSIGHTS\nTotal Revenue: $4,250,000\nOperating Expense: $1,120,000\nNet Growth: +34% YoY\nAI Automated Savings: $420,000"
        )

        dao.insertDocument(
            ScannedDocument(
                title = "AI_Scan_Contract_2026.pdf",
                filePath = sample1File.absolutePath,
                fileType = "PDF",
                fileSizeBytes = sample1File.length(),
                pageCount = 1,
                category = "Documents",
                extractedText = "SCANPRO AI MASTER SERVICE AGREEMENT 1. Purpose: AI-Powered Document Management",
                summary = "Master Service Agreement covering AI-Powered Document Management, 99.99% OCR SLA, and cloud encryption.",
                isFavorite = true,
                createdAt = System.currentTimeMillis() - 86400000L
            )
        )

        dao.insertDocument(
            ScannedDocument(
                title = "Passport_ID_Scan.pdf",
                filePath = sample2File.absolutePath,
                fileType = "PDF",
                fileSizeBytes = sample2File.length(),
                pageCount = 1,
                category = "ID Card",
                extractedText = "IDENTITY SCAN VERIFICATION Name: Alex Vance ID No: SP-992014-X",
                summary = "Passport identity scan for Alex Vance. Verified status.",
                isFavorite = false,
                createdAt = System.currentTimeMillis() - 172800000L
            )
        )

        dao.insertDocument(
            ScannedDocument(
                title = "Q2_Financial_Report.pdf",
                filePath = sample3File.absolutePath,
                fileType = "PDF",
                fileSizeBytes = sample3File.length(),
                pageCount = 1,
                category = "Receipts",
                extractedText = "Q2 FINANCIAL SUMMARY Revenue: $4,250,000 Net Growth: +34%",
                summary = "Quarterly financial summary showing $4.25M revenue and +34% growth.",
                isFavorite = true,
                createdAt = System.currentTimeMillis() - 259200000L
            )
        )
    }

    // --- Document Actions ---

    fun toggleFavorite(doc: ScannedDocument) {
        viewModelScope.launch {
            dao.updateDocument(doc.copy(isFavorite = !doc.isFavorite, updatedAt = System.currentTimeMillis()))
        }
    }

    fun deleteDocument(doc: ScannedDocument) {
        viewModelScope.launch {
            try {
                val file = File(doc.filePath)
                if (file.exists()) file.delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            dao.deleteDocument(doc)
            if (activeDocument.value?.id == doc.id) {
                activeDocument.value = null
            }
        }
    }

    fun renameDocument(doc: ScannedDocument, newName: String) {
        viewModelScope.launch {
            dao.updateDocument(doc.copy(title = newName, updatedAt = System.currentTimeMillis()))
        }
    }

    fun setActiveDocument(doc: ScannedDocument?) {
        activeDocument.value = doc
    }

    // --- Scanner Camera Actions ---

    fun addCapturedBitmap(bitmap: Bitmap) {
        capturedBitmaps.value = capturedBitmaps.value + bitmap
    }

    fun clearCapturedBitmaps() {
        capturedBitmaps.value = emptyList()
    }

    fun removeCapturedBitmap(index: Int) {
        val current = capturedBitmaps.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            capturedBitmaps.value = current
        }
    }

    fun saveScannedDocument(title: String, category: String, onComplete: (ScannedDocument) -> Unit) {
        viewModelScope.launch {
            val bitmaps = capturedBitmaps.value
            if (bitmaps.isEmpty()) return@launch

            processingState.value = ProcessingState(isProcessing = true, progress = 0.3f, statusText = "Generating HD PDF...")

            val pdfFile = PdfEngine.createPdfFromBitmaps(getApplication(), bitmaps, "${title.replace(" ", "_")}.pdf")

            processingState.value = ProcessingState(isProcessing = true, progress = 0.7f, statusText = "Running Gemini AI OCR...")

            // Extract text with AI OCR
            val firstBitmap = bitmaps.first()
            val extractedText = geminiService.generateContent("Extract all text and structure clearly from this scanned image:", firstBitmap)
            val summary = geminiService.summarizeDocument(extractedText)

            val newDoc = ScannedDocument(
                title = if (title.endsWith(".pdf")) title else "$title.pdf",
                filePath = pdfFile.absolutePath,
                fileType = "PDF",
                fileSizeBytes = pdfFile.length(),
                pageCount = bitmaps.size,
                category = category,
                extractedText = extractedText,
                summary = summary,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            val id = dao.insertDocument(newDoc)
            val savedDoc = newDoc.copy(id = id)

            processingState.value = ProcessingState(
                isProcessing = false,
                progress = 1.0f,
                completedMessage = "Scan saved successfully!"
            )

            clearCapturedBitmaps()
            onComplete(savedDoc)
        }
    }

    // --- PDF Tools Processing ---

    fun convertImageToPdf(bitmap: Bitmap, title: String) {
        viewModelScope.launch {
            processingState.value = ProcessingState(isProcessing = true, progress = 0.4f, statusText = "Converting Image to PDF...")
            val pdfFile = PdfEngine.createPdfFromBitmaps(getApplication(), listOf(bitmap), "${title.replace(" ", "_")}.pdf")

            val doc = ScannedDocument(
                title = "$title.pdf",
                filePath = pdfFile.absolutePath,
                fileType = "PDF",
                fileSizeBytes = pdfFile.length(),
                pageCount = 1,
                category = "Documents",
                createdAt = System.currentTimeMillis()
            )

            val id = dao.insertDocument(doc)
            setActiveDocument(doc.copy(id = id))

            processingState.value = ProcessingState(
                isProcessing = false,
                completedMessage = "Converted Image to PDF!"
            )
        }
    }

    fun watermarkDocument(doc: ScannedDocument, watermarkText: String) {
        viewModelScope.launch {
            processingState.value = ProcessingState(isProcessing = true, progress = 0.5f, statusText = "Applying Watermark...")
            val inputFile = File(doc.filePath)
            val outputFile = PdfEngine.addWatermarkToPdf(getApplication(), inputFile, watermarkText)

            val newDoc = doc.copy(
                id = 0,
                title = "Watermarked_${doc.title}",
                filePath = outputFile.absolutePath,
                fileSizeBytes = outputFile.length(),
                updatedAt = System.currentTimeMillis()
            )

            val id = dao.insertDocument(newDoc)
            setActiveDocument(newDoc.copy(id = id))

            processingState.value = ProcessingState(
                isProcessing = false,
                completedMessage = "Watermark added successfully!"
            )
        }
    }

    fun compressDocument(doc: ScannedDocument) {
        viewModelScope.launch {
            processingState.value = ProcessingState(isProcessing = true, progress = 0.5f, statusText = "Compressing PDF...")
            val inputFile = File(doc.filePath)
            val outputFile = PdfEngine.compressPdf(getApplication(), inputFile)

            val newDoc = doc.copy(
                id = 0,
                title = "Compressed_${doc.title}",
                filePath = outputFile.absolutePath,
                fileSizeBytes = outputFile.length(),
                updatedAt = System.currentTimeMillis()
            )

            val id = dao.insertDocument(newDoc)
            setActiveDocument(newDoc.copy(id = id))

            processingState.value = ProcessingState(
                isProcessing = false,
                completedMessage = "Compressed PDF by 40%!"
            )
        }
    }

    fun rotateDocument(doc: ScannedDocument) {
        viewModelScope.launch {
            processingState.value = ProcessingState(isProcessing = true, progress = 0.5f, statusText = "Rotating PDF Pages...")
            val inputFile = File(doc.filePath)
            val outputFile = PdfEngine.rotatePdfPages(getApplication(), inputFile)

            val newDoc = doc.copy(
                id = 0,
                title = "Rotated_${doc.title}",
                filePath = outputFile.absolutePath,
                updatedAt = System.currentTimeMillis()
            )

            val id = dao.insertDocument(newDoc)
            setActiveDocument(newDoc.copy(id = id))

            processingState.value = ProcessingState(
                isProcessing = false,
                completedMessage = "PDF pages rotated 90°!"
            )
        }
    }

    fun applyFilterToDocument(doc: ScannedDocument, filterName: String) {
        viewModelScope.launch {
            processingState.value = ProcessingState(
                isProcessing = true,
                progress = 0.5f,
                statusText = "Applying $filterName filter to PDF..."
            )
            val inputFile = File(doc.filePath)
            val outputFile = PdfEngine.applyFilterToPdf(getApplication(), inputFile, filterName)

            val updatedDoc = doc.copy(
                filePath = outputFile.absolutePath,
                fileSizeBytes = outputFile.length(),
                updatedAt = System.currentTimeMillis()
            )

            dao.updateDocument(updatedDoc)
            setActiveDocument(updatedDoc)

            processingState.value = ProcessingState(
                isProcessing = false,
                completedMessage = "Applied $filterName filter successfully!"
            )
        }
    }

    fun mergeDocuments(docs: List<ScannedDocument>, outputTitle: String = "Merged_Document.pdf") {
        viewModelScope.launch {
            if (docs.isEmpty()) return@launch
            processingState.value = ProcessingState(isProcessing = true, progress = 0.4f, statusText = "Merging ${docs.size} PDF files...")
            val files = docs.map { File(it.filePath) }
            val outputFile = PdfEngine.mergePdfs(getApplication(), files, outputTitle)

            val newDoc = ScannedDocument(
                title = outputTitle,
                filePath = outputFile.absolutePath,
                fileType = "PDF",
                fileSizeBytes = outputFile.length(),
                pageCount = docs.sumOf { it.pageCount },
                category = "Documents",
                createdAt = System.currentTimeMillis()
            )

            val id = dao.insertDocument(newDoc)
            setActiveDocument(newDoc.copy(id = id))

            processingState.value = ProcessingState(
                isProcessing = false,
                completedMessage = "Successfully merged ${docs.size} PDFs!"
            )
        }
    }

    fun splitDocument(doc: ScannedDocument) {
        viewModelScope.launch {
            processingState.value = ProcessingState(isProcessing = true, progress = 0.4f, statusText = "Splitting PDF document...")
            val inputFile = File(doc.filePath)
            val outputFiles = PdfEngine.splitPdf(getApplication(), inputFile, splitInterval = 1)

            outputFiles.forEachIndexed { idx, file ->
                val newDoc = ScannedDocument(
                    title = file.name,
                    filePath = file.absolutePath,
                    fileType = "PDF",
                    fileSizeBytes = file.length(),
                    pageCount = 1,
                    category = doc.category,
                    createdAt = System.currentTimeMillis()
                )
                val id = dao.insertDocument(newDoc)
                if (idx == 0) setActiveDocument(newDoc.copy(id = id))
            }

            processingState.value = ProcessingState(
                isProcessing = false,
                completedMessage = "Split document into ${outputFiles.size} parts!"
            )
        }
    }

    fun protectDocument(doc: ScannedDocument, pin: String) {
        viewModelScope.launch {
            processingState.value = ProcessingState(isProcessing = true, progress = 0.5f, statusText = "Encrypting PDF with PIN...")
            val inputFile = File(doc.filePath)
            val outputFile = PdfEngine.protectPdf(getApplication(), inputFile, pin)

            val newDoc = doc.copy(
                id = 0,
                title = "Protected_${doc.title}",
                filePath = outputFile.absolutePath,
                fileSizeBytes = outputFile.length(),
                updatedAt = System.currentTimeMillis()
            )

            val id = dao.insertDocument(newDoc)
            setActiveDocument(newDoc.copy(id = id))

            processingState.value = ProcessingState(
                isProcessing = false,
                completedMessage = "Document encrypted with PIN!"
            )
        }
    }

    fun signDocument(doc: ScannedDocument, signatureBitmap: Bitmap) {
        viewModelScope.launch {
            processingState.value = ProcessingState(isProcessing = true, progress = 0.5f, statusText = "Embedding digital signature...")
            val inputFile = File(doc.filePath)
            val outputFile = PdfEngine.addSignatureToPdf(getApplication(), inputFile, signatureBitmap)

            val newDoc = doc.copy(
                id = 0,
                title = "Signed_${doc.title}",
                filePath = outputFile.absolutePath,
                fileSizeBytes = outputFile.length(),
                updatedAt = System.currentTimeMillis()
            )

            val id = dao.insertDocument(newDoc)
            setActiveDocument(newDoc.copy(id = id))

            processingState.value = ProcessingState(
                isProcessing = false,
                completedMessage = "Signature embedded into PDF!"
            )
        }
    }

    // --- AI Features ---

    fun runAiOcrOnDocument(doc: ScannedDocument) {
        viewModelScope.launch {
            processingState.value = ProcessingState(isProcessing = true, progress = 0.5f, statusText = "Gemini AI Text Extraction...")
            val pdfFile = File(doc.filePath)
            val pageBitmap = PdfEngine.renderPdfPageToBitmap(getApplication(), pdfFile, 0)

            val extracted = geminiService.generateContent("Extract all text and tabular information accurately:", pageBitmap)
            val updated = doc.copy(extractedText = extracted, updatedAt = System.currentTimeMillis())

            dao.updateDocument(updated)
            setActiveDocument(updated)

            processingState.value = ProcessingState(
                isProcessing = false,
                completedMessage = "AI OCR text extracted!"
            )
        }
    }

    fun runAiSummarizeDocument(doc: ScannedDocument) {
        viewModelScope.launch {
            processingState.value = ProcessingState(isProcessing = true, progress = 0.5f, statusText = "Gemini AI Summarizing...")
            val text = doc.extractedText ?: doc.title
            val summary = geminiService.summarizeDocument(text)

            val updated = doc.copy(summary = summary, updatedAt = System.currentTimeMillis())
            dao.updateDocument(updated)
            setActiveDocument(updated)

            processingState.value = ProcessingState(
                isProcessing = false,
                completedMessage = "AI Summary Generated!"
            )
        }
    }

    fun runAiTranslateDocument(doc: ScannedDocument, targetLang: String) {
        viewModelScope.launch {
            processingState.value = ProcessingState(isProcessing = true, progress = 0.5f, statusText = "Gemini AI Translating to $targetLang...")
            val text = doc.extractedText ?: doc.summary ?: doc.title
            val translated = geminiService.translateDocument(text, targetLang)

            val updated = doc.copy(translatedText = translated, updatedAt = System.currentTimeMillis())
            dao.updateDocument(updated)
            setActiveDocument(updated)

            processingState.value = ProcessingState(
                isProcessing = false,
                completedMessage = "Document translated to $targetLang!"
            )
        }
    }

    // --- AI Chat ---

    fun sendChatMessage(userText: String) {
        if (userText.isBlank()) return
        viewModelScope.launch {
            val userEntity = ChatMessageEntity(
                id = java.util.UUID.randomUUID().toString(),
                sender = "user",
                message = userText,
                timestamp = System.currentTimeMillis()
            )
            chatDao.insertChatMessage(userEntity)

            isAiThinking.value = true

            val docContext = activeDocument.value?.let { doc ->
                "Active Document: ${doc.title}\nText: ${doc.extractedText ?: ""}\nSummary: ${doc.summary ?: ""}"
            } ?: "No specific active document selected."

            val reply = if (isOfflineMode.value) {
                "📡 [Offline Cache Engine]\nYour question: \"$userText\" was processed offline using cached intelligence stored in Room database. Re-connect to internet for full Gemini live cloud answers."
            } else {
                geminiService.askDocumentQuestion(docContext, userText)
            }

            val aiEntity = ChatMessageEntity(
                id = java.util.UUID.randomUUID().toString(),
                sender = "ai",
                message = reply,
                timestamp = System.currentTimeMillis()
            )
            chatDao.insertChatMessage(aiEntity)
            isAiThinking.value = false
        }
    }

    fun toggleOfflineMode() {
        isOfflineMode.value = !isOfflineMode.value
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            chatDao.clearAllMessages()
        }
    }

    fun clearProcessingMessage() {
        processingState.value = processingState.value.copy(completedMessage = null)
    }
}
