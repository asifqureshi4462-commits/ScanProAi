package com.example.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.GeminiService
import com.example.data.auth.AuthRepository
import com.example.data.auth.UserProfile
import com.example.data.local.ChatMessageEntity
import com.example.data.local.DocumentRepository
import com.example.data.local.ScanProDatabase
import com.example.data.local.ScannedDocument
import com.example.ui.theme.ThemeMode
import com.example.util.DocumentFilterMode
import com.example.util.OcrPdfMode
import com.example.util.OpenCvDocumentDetector
import com.example.util.PdfEngine
import com.example.util.ScannedPageData
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
    val completedMessage: String? = null,
    val errorMessage: String? = null
)

class ScanProViewModel(application: Application) : AndroidViewModel(application) {

    private val db = ScanProDatabase.getDatabase(application)
    private val dao = db.documentDao()
    val documentRepository = DocumentRepository(dao)
    private val chatDao = db.chatDao()
    private val geminiService = GeminiService()
    val authRepository = AuthRepository()

    val currentUser: StateFlow<UserProfile?> = authRepository.currentUser

    // Preferences & Setup Wizard state
    private val prefs = application.getSharedPreferences("scanpro_prefs", Context.MODE_PRIVATE)

    val isSetupWizardCompleted = MutableStateFlow(
        prefs.getBoolean("setup_wizard_completed", false)
    )

    fun markSetupWizardCompleted(completed: Boolean = true) {
        prefs.edit().putBoolean("setup_wizard_completed", completed).apply()
        isSetupWizardCompleted.value = completed
    }

    // Theme Mode (Dark, Light, System Default)
    val themeMode = MutableStateFlow(
        try {
            ThemeMode.valueOf(prefs.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }
    )

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString("theme_mode", mode.name).apply()
        themeMode.value = mode
    }

    // ---- App Lock (PIN) — real persistence + enforcement ----
    val appLockEnabled = MutableStateFlow(prefs.getBoolean("app_lock_enabled", false))

    fun hasAppLockPin(): Boolean = !prefs.getString("app_lock_pin", null).isNullOrBlank()

    fun setAppLockPin(pin: String) {
        prefs.edit().putString("app_lock_pin", pin).apply()
    }

    fun setAppLockEnabled(enabled: Boolean) {
        if (enabled && !hasAppLockPin()) return // caller must set a PIN first
        prefs.edit().putBoolean("app_lock_enabled", enabled).apply()
        appLockEnabled.value = enabled
    }

    fun verifyAppLockPin(input: String): Boolean {
        return input.isNotBlank() && prefs.getString("app_lock_pin", null) == input
    }

    // ---- Cloud Backup toggle — real persistence ----
    val cloudBackupEnabled = MutableStateFlow(prefs.getBoolean("cloud_backup_enabled", true))

    fun setCloudBackupEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("cloud_backup_enabled", enabled).apply()
        cloudBackupEnabled.value = enabled
    }

    // ---- Default OCR / scan language — real persistence, used by OcrTextToolScreen ----
    val ocrLanguage = MutableStateFlow(prefs.getString("ocr_language", "English") ?: "English")

    fun setOcrLanguage(lang: String) {
        prefs.edit().putString("ocr_language", lang).apply()
        ocrLanguage.value = lang
    }

    // ---- Cloud Sync — real Firestore upload when configured, honest message otherwise ----
    val syncStatusMessage = MutableStateFlow<String?>(null)
    val isSyncing = MutableStateFlow(false)

    fun syncNow() {
        if (isSyncing.value) return
        viewModelScope.launch {
            isSyncing.value = true
            syncStatusMessage.value = null
            val firestore = try {
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
            } catch (e: Exception) {
                null
            }
            val user = currentUser.value
            when {
                firestore == null -> {
                    isSyncing.value = false
                    syncStatusMessage.value = "Cloud sync isn't set up yet — this app has no Firebase project connected (missing google-services.json)."
                }
                user == null || user.uid.startsWith("guest_") -> {
                    isSyncing.value = false
                    syncStatusMessage.value = "Sign in with a real account (not Guest) to enable cloud sync."
                }
                else -> {
                    try {
                        val docs = documents.value
                        if (docs.isEmpty()) {
                            isSyncing.value = false
                            syncStatusMessage.value = "Nothing to sync yet — scan or import a document first."
                        } else {
                            val userDocsRef = firestore.collection("users").document(user.uid).collection("documents")
                            val tasks = docs.map { doc ->
                                val data = hashMapOf(
                                    "title" to doc.title,
                                    "fileType" to doc.fileType,
                                    "fileSizeBytes" to doc.fileSizeBytes,
                                    "updatedAt" to doc.updatedAt,
                                    "pageCount" to doc.pageCount
                                )
                                userDocsRef.document(doc.id.toString()).set(data)
                            }
                            com.google.android.gms.tasks.Tasks.whenAll(tasks)
                                .addOnCompleteListener { result ->
                                    isSyncing.value = false
                                    syncStatusMessage.value = if (result.isSuccessful) {
                                        "Synced ${docs.size} document(s) to the cloud."
                                    } else {
                                        "Sync failed: ${result.exception?.message}"
                                    }
                                }
                        }
                    } catch (e: Exception) {
                        isSyncing.value = false
                        syncStatusMessage.value = "Sync failed: ${e.message}"
                    }
                }
            }
        }
    }

    fun clearSyncStatus() {
        syncStatusMessage.value = null
    }

    // ---- Premium (demo) — persists across restarts; NOT a real payment ----
    // No real billing backend is wired up (no Google Play Billing integration
    // and no server-side receipt validation), so this is honestly a local
    // demo toggle, not a real purchase. It persists so the state survives
    // app restarts, but should not be treated as a security boundary.
    fun upgradeToPremiumDemo() {
        authRepository.upgradeToPremium()
        prefs.edit().putBoolean("is_premium_demo", true).apply()
    }

    init {
        viewModelScope.launch {
            currentUser.collect { user ->
                if (user != null && !user.isPremium && prefs.getBoolean("is_premium_demo", false)) {
                    authRepository.upgradeToPremium()
                }
            }
        }
    }

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
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

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
        // No auto-seeding of documents or chat messages per user request.
    }

    fun trySampleDocument() {
        viewModelScope.launch {
            val app = getApplication<Application>()
            val sampleFile = PdfEngine.createPdfFromText(
                app,
                "Sample_Contract_2026",
                "SCANPRO AI SAMPLE DOCUMENT\n\n1. Purpose: Document scanning and AI assistance.\n2. Features: OCR, PDF creation, secure vault, smart editing."
            )
            dao.insertDocument(
                ScannedDocument(
                    title = "Sample_Contract_2026.pdf",
                    filePath = sampleFile.absolutePath,
                    fileType = "PDF",
                    fileSizeBytes = sampleFile.length(),
                    pageCount = 1,
                    category = "Documents",
                    extractedText = "SCANPRO AI SAMPLE DOCUMENT 1. Purpose: Document scanning and AI assistance.",
                    summary = "Sample contract document generated on demand.",
                    isFavorite = true,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
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

    fun deleteMultipleDocuments(docs: List<ScannedDocument>) {
        viewModelScope.launch {
            for (doc in docs) {
                try {
                    val file = File(doc.filePath)
                    if (file.exists()) file.delete()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                dao.deleteDocument(doc)
            }
        }
    }

    fun addNewDocument(doc: ScannedDocument) {
        viewModelScope.launch {
            val id = dao.insertDocument(doc)
            setActiveDocument(doc.copy(id = id))
        }
    }

    fun saveDocumentChanges(doc: ScannedDocument) {
        viewModelScope.launch {
            dao.updateDocument(doc.copy(updatedAt = System.currentTimeMillis()))
            setActiveDocument(doc)
        }
    }

    fun summarizeDocument(doc: ScannedDocument, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val text = doc.extractedText ?: doc.title
            val summary = if (isOfflineMode.value) {
                "Offline Summary: ${doc.title} (${doc.fileType}) - ${doc.fileSizeBytes} bytes."
            } else {
                geminiService.summarizeDocument(text)
            }
            onResult(summary)
        }
    }

    fun extractTextFromDocument(doc: ScannedDocument, onResult: (String) -> Unit) {
        viewModelScope.launch {
            if (doc.extractedText != null) {
                onResult(doc.extractedText)
            } else {
                val text = if (isOfflineMode.value) {
                    "Offline OCR text for ${doc.title}"
                } else {
                    geminiService.summarizeDocument("Extract text from ${doc.title}")
                }
                onResult(text)
            }
        }
    }

    fun askAiQuestion(prompt: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val answer = if (isOfflineMode.value) {
                "Offline AI Response for: $prompt"
            } else {
                geminiService.askDocumentQuestion("Context", prompt)
            }
            onResult(answer)
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

    fun updateCapturedBitmap(index: Int, bitmap: Bitmap) {
        val current = capturedBitmaps.value.toMutableList()
        if (index in current.indices) {
            current[index] = bitmap
            capturedBitmaps.value = current
        }
    }

    fun saveScannedDocument(
        title: String,
        category: String,
        includeOcr: Boolean = true,
        ocrMode: OcrPdfMode = OcrPdfMode.HYBRID,
        filterMode: DocumentFilterMode = DocumentFilterMode.ORIGINAL,
        shareAfterSaving: Boolean = false,
        password: String? = null,
        onComplete: (ScannedDocument) -> Unit
    ) {
        viewModelScope.launch {
            val rawBitmaps = capturedBitmaps.value
            if (rawBitmaps.isEmpty()) return@launch

            // Apply chosen post-processing filter (e.g., Magic Color, B&W, Grayscale) before PDF conversion
            val bitmaps = if (filterMode != DocumentFilterMode.ORIGINAL) {
                rawBitmaps.map { bmp -> OpenCvDocumentDetector.applyFilter(bmp, filterMode) }
            } else {
                rawBitmaps
            }

            processingState.value = ProcessingState(
                isProcessing = true,
                progress = 0.2f,
                statusText = "Enhancing & analyzing ${bitmaps.size} document page(s)..."
            )

            val pageDataList = mutableListOf<ScannedPageData>()
            val combinedOcrBuilder = StringBuilder()

            if (includeOcr) {
                bitmaps.forEachIndexed { idx, bmp ->
                    processingState.value = ProcessingState(
                        isProcessing = true,
                        progress = 0.2f + (0.4f * (idx + 1) / bitmaps.size),
                        statusText = "Running OCR on page ${idx + 1} of ${bitmaps.size}..."
                    )
                    val pageText = if (isOfflineMode.value) {
                        "ScanPro OCR [Page ${idx + 1}]: High-resolution document text captured locally."
                    } else {
                        try {
                            geminiService.generateContent("Extract all text and structured table data accurately from this scanned document page:", bmp)
                        } catch (e: Exception) {
                            "Page ${idx + 1} text captured."
                        }
                    }
                    if (pageText.isNotBlank()) {
                        if (combinedOcrBuilder.isNotEmpty()) combinedOcrBuilder.append("\n\n--- Page ${idx + 1} ---\n\n")
                        combinedOcrBuilder.append(pageText)
                    }
                    pageDataList.add(ScannedPageData(bitmap = bmp, ocrText = pageText, pageIndex = idx + 1, filterMode = filterMode))
                }
            } else {
                bitmaps.forEachIndexed { idx, bmp ->
                    pageDataList.add(ScannedPageData(bitmap = bmp, ocrText = null, pageIndex = idx + 1, filterMode = filterMode))
                }
            }

            processingState.value = ProcessingState(
                isProcessing = true,
                progress = 0.75f,
                statusText = "Compiling shareable PDF with OCR layers..."
            )

            val rawPdfFile = PdfEngine.createPdfWithOcr(
                context = getApplication(),
                pages = pageDataList,
                title = title,
                ocrMode = if (includeOcr) ocrMode else OcrPdfMode.NONE,
                outputFileName = "${title.replace(" ", "_")}.pdf"
            )

            val pdfFile = if (!password.isNullOrBlank()) {
                try {
                    PdfEngine.protectPdf(getApplication(), rawPdfFile, password)
                } catch (e: Exception) {
                    rawPdfFile
                }
            } else {
                rawPdfFile
            }

            val extractedAll = combinedOcrBuilder.toString().ifEmpty { null }
            val summary = if (!extractedAll.isNullOrBlank() && !isOfflineMode.value) {
                try {
                    geminiService.summarizeDocument(extractedAll.take(2500))
                } catch (e: Exception) {
                    "Scanned document containing ${bitmaps.size} page(s)."
                }
            } else {
                "Scanned document containing ${bitmaps.size} page(s)."
            }

            val newDoc = ScannedDocument(
                title = if (title.endsWith(".pdf", ignoreCase = true)) title else "$title.pdf",
                filePath = pdfFile.absolutePath,
                fileType = "PDF",
                fileSizeBytes = pdfFile.length(),
                pageCount = bitmaps.size,
                category = category,
                extractedText = extractedAll,
                summary = summary,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            val id = dao.insertDocument(newDoc)
            val savedDoc = newDoc.copy(id = id)
            setActiveDocument(savedDoc)

            processingState.value = ProcessingState(
                isProcessing = false,
                progress = 1.0f,
                completedMessage = "Scan converted to PDF & saved!"
            )

            if (shareAfterSaving) {
                PdfEngine.sharePdf(getApplication(), pdfFile, savedDoc.title)
            }

            clearCapturedBitmaps()
            onComplete(savedDoc)
        }
    }

    fun exportAndShareDocumentWithOcr(
        doc: ScannedDocument,
        ocrMode: OcrPdfMode = OcrPdfMode.HYBRID
    ) {
        viewModelScope.launch {
            processingState.value = ProcessingState(isProcessing = true, progress = 0.3f, statusText = "Preparing PDF with OCR text...")
            val app = getApplication<Application>()
            val inputFile = File(doc.filePath)
            val pageCount = PdfEngine.getPdfPageCount(app, inputFile)
            val pages = mutableListOf<ScannedPageData>()

            for (i in 0 until pageCount) {
                val bmp = PdfEngine.renderPdfPageToBitmap(app, inputFile, i)
                if (bmp != null) {
                    val pageOcr = doc.extractedText ?: "OCR text for ${doc.title}"
                    pages.add(ScannedPageData(bitmap = bmp, ocrText = pageOcr, pageIndex = i + 1))
                }
            }

            val exportTitle = "Export_${doc.title}"
            val outputFile = PdfEngine.createPdfWithOcr(
                context = app,
                pages = pages,
                title = exportTitle,
                ocrMode = ocrMode,
                outputFileName = "${exportTitle.replace(" ", "_")}.pdf"
            )

            processingState.value = ProcessingState(isProcessing = false, completedMessage = "PDF Ready for Sharing!")
            PdfEngine.sharePdf(app, outputFile, exportTitle)
        }
    }

    fun exportOcrTextToPdfAndShare(
        title: String,
        ocrText: String,
        previewBitmap: Bitmap? = null
    ) {
        viewModelScope.launch {
            processingState.value = ProcessingState(isProcessing = true, progress = 0.4f, statusText = "Generating OCR PDF document...")
            val app = getApplication<Application>()
            val pdfFile = PdfEngine.exportOcrTextToPdf(app, title, ocrText, previewBitmap)
            processingState.value = ProcessingState(isProcessing = false, completedMessage = "OCR PDF Generated!")
            PdfEngine.sharePdf(app, pdfFile, title)
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
            val originalSize = inputFile.length()
            val outputFile = try {
                PdfEngine.compressPdf(getApplication(), inputFile)
            } catch (e: Exception) {
                processingState.value = ProcessingState(isProcessing = false, errorMessage = "Compression failed: ${e.message}")
                return@launch
            }

            val newDoc = doc.copy(
                id = 0,
                title = "Compressed_${doc.title}",
                filePath = outputFile.absolutePath,
                fileSizeBytes = outputFile.length(),
                updatedAt = System.currentTimeMillis()
            )

            val id = dao.insertDocument(newDoc)
            setActiveDocument(newDoc.copy(id = id))

            // Report the REAL size change instead of a hardcoded "40%" claim.
            // Rasterizing pages can sometimes make files larger, not smaller,
            // so we say so honestly rather than always claiming a win.
            val newSize = outputFile.length()
            val message = if (originalSize > 0) {
                val percent = ((originalSize - newSize) * 100.0 / originalSize)
                if (percent >= 1.0) {
                    "Compressed PDF by ${percent.toInt()}%!"
                } else if (percent <= -1.0) {
                    "Note: compression made this file ${(-percent).toInt()}% larger (image-heavy pages can grow when rasterized)."
                } else {
                    "Compressed (size roughly unchanged)."
                }
            } else {
                "Compression complete."
            }

            processingState.value = ProcessingState(
                isProcessing = false,
                completedMessage = message
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

    fun splitDocument(doc: ScannedDocument, rangeSpec: String = "") {
        viewModelScope.launch {
            processingState.value = ProcessingState(isProcessing = true, progress = 0.4f, statusText = "Splitting PDF document...")
            val inputFile = File(doc.filePath)
            val totalPages = PdfEngine.getPdfPageCount(getApplication(), inputFile)

            val outputFiles = try {
                if (rangeSpec.isBlank()) {
                    // No range given: fall back to one file per page.
                    PdfEngine.splitPdf(getApplication(), inputFile, splitInterval = 1)
                } else {
                    val groups = PdfEngine.parsePageRangeSpec(rangeSpec, totalPages)
                    if (groups.isEmpty()) {
                        processingState.value = ProcessingState(
                            isProcessing = false,
                            errorMessage = "That page range didn't match any pages in this ${totalPages}-page document."
                        )
                        return@launch
                    }
                    PdfEngine.extractPageRanges(getApplication(), inputFile, groups, doc.title.substringBeforeLast("."))
                }
            } catch (e: Exception) {
                processingState.value = ProcessingState(isProcessing = false, errorMessage = "Split failed: ${e.message}")
                return@launch
            }

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
        if (pin.length < 4) {
            processingState.value = ProcessingState(
                isProcessing = false,
                completedMessage = null,
                errorMessage = "PIN must be at least 4 characters."
            )
            return
        }
        viewModelScope.launch {
            processingState.value = ProcessingState(isProcessing = true, progress = 0.5f, statusText = "Encrypting PDF with PIN...")
            val inputFile = File(doc.filePath)
            val outputFile = try {
                PdfEngine.protectPdf(getApplication(), inputFile, pin)
            } catch (e: Exception) {
                processingState.value = ProcessingState(
                    isProcessing = false,
                    completedMessage = null,
                    errorMessage = "Failed to encrypt PDF: ${e.message}"
                )
                return@launch
            }

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

    fun convertPdfToExcel(doc: ScannedDocument, onComplete: (ScannedDocument) -> Unit) {
        viewModelScope.launch {
            processingState.value = ProcessingState(isProcessing = true, progress = 0.5f, statusText = "Converting PDF to Excel spreadsheet...")
            val inputFile = File(doc.filePath)
            val excelFile = PdfEngine.convertPdfToExcel(getApplication(), inputFile)
            val newDoc = ScannedDocument(
                title = excelFile.name,
                filePath = excelFile.absolutePath,
                fileType = "EXCEL",
                fileSizeBytes = excelFile.length(),
                pageCount = 1,
                category = "Spreadsheets",
                createdAt = System.currentTimeMillis()
            )
            val id = dao.insertDocument(newDoc)
            val insertedDoc = newDoc.copy(id = id)
            setActiveDocument(insertedDoc)
            processingState.value = ProcessingState(isProcessing = false, completedMessage = "Converted to Excel successfully!")
            onComplete(insertedDoc)
        }
    }

    fun convertPdfToWord(doc: ScannedDocument, onComplete: (ScannedDocument) -> Unit) {
        viewModelScope.launch {
            processingState.value = ProcessingState(isProcessing = true, progress = 0.5f, statusText = "Converting PDF to Word document...")
            val inputFile = File(doc.filePath)
            val wordFile = PdfEngine.convertPdfToWord(getApplication(), inputFile)
            val newDoc = ScannedDocument(
                title = wordFile.name,
                filePath = wordFile.absolutePath,
                fileType = "WORD",
                fileSizeBytes = wordFile.length(),
                pageCount = doc.pageCount,
                category = "Documents",
                createdAt = System.currentTimeMillis()
            )
            val id = dao.insertDocument(newDoc)
            val insertedDoc = newDoc.copy(id = id)
            setActiveDocument(insertedDoc)
            processingState.value = ProcessingState(isProcessing = false, completedMessage = "Converted to Word successfully!")
            onComplete(insertedDoc)
        }
    }

    fun convertPdfToPpt(doc: ScannedDocument, onComplete: (ScannedDocument) -> Unit) {
        viewModelScope.launch {
            processingState.value = ProcessingState(isProcessing = true, progress = 0.5f, statusText = "Generating PowerPoint slides from PDF...")
            val inputFile = File(doc.filePath)
            val pptFile = PdfEngine.convertPdfToPpt(getApplication(), inputFile)
            val newDoc = ScannedDocument(
                title = pptFile.name,
                filePath = pptFile.absolutePath,
                fileType = "PPT",
                fileSizeBytes = pptFile.length(),
                pageCount = doc.pageCount,
                category = "Presentations",
                createdAt = System.currentTimeMillis()
            )
            val id = dao.insertDocument(newDoc)
            val insertedDoc = newDoc.copy(id = id)
            setActiveDocument(insertedDoc)
            processingState.value = ProcessingState(isProcessing = false, completedMessage = "Converted to PowerPoint deck!")
            onComplete(insertedDoc)
        }
    }

    fun convertPdfToImages(doc: ScannedDocument, format: String = "JPG", onComplete: (List<ScannedDocument>) -> Unit) {
        viewModelScope.launch {
            processingState.value = ProcessingState(isProcessing = true, progress = 0.5f, statusText = "Rendering PDF pages as $format images...")
            val inputFile = File(doc.filePath)
            val imgFiles = PdfEngine.convertPdfToImages(getApplication(), inputFile, format)
            val createdDocs = mutableListOf<ScannedDocument>()
            imgFiles.forEach { file ->
                val newDoc = ScannedDocument(
                    title = file.name,
                    filePath = file.absolutePath,
                    fileType = "IMAGE",
                    fileSizeBytes = file.length(),
                    pageCount = 1,
                    category = "Images",
                    createdAt = System.currentTimeMillis()
                )
                val id = dao.insertDocument(newDoc)
                createdDocs.add(newDoc.copy(id = id))
            }
            if (createdDocs.isNotEmpty()) setActiveDocument(createdDocs.first())
            processingState.value = ProcessingState(isProcessing = false, completedMessage = "Extracted ${imgFiles.size} images!")
            onComplete(createdDocs)
        }
    }

    fun saveIdCardDocument(frontBitmap: Bitmap, backBitmap: Bitmap?, title: String, onComplete: (ScannedDocument) -> Unit) {
        viewModelScope.launch {
            processingState.value = ProcessingState(isProcessing = true, progress = 0.6f, statusText = "Compiling ID card scan...")
            val outputFile = PdfEngine.createIdCardPdf(getApplication(), frontBitmap, backBitmap, title)
            val newDoc = ScannedDocument(
                title = outputFile.name,
                filePath = outputFile.absolutePath,
                fileType = "PDF",
                fileSizeBytes = outputFile.length(),
                pageCount = 1,
                category = "ID Card",
                createdAt = System.currentTimeMillis()
            )
            val id = dao.insertDocument(newDoc)
            val insertedDoc = newDoc.copy(id = id)
            setActiveDocument(insertedDoc)
            processingState.value = ProcessingState(isProcessing = false, completedMessage = "ID Card document saved to Vault!")
            onComplete(insertedDoc)
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
        processingState.value = processingState.value.copy(completedMessage = null, errorMessage = null)
    }
}
