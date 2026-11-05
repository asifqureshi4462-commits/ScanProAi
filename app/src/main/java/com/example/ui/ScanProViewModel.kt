package com.example.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String,
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

    private val prefs = application.getSharedPreferences("scanpro_prefs", Context.MODE_PRIVATE)

    val isSetupWizardCompleted = MutableStateFlow(
        prefs.getBoolean("setup_wizard_completed", false)
    )

    fun markSetupWizardCompleted(completed: Boolean = true) {
        prefs.edit().putBoolean("setup_wizard_completed", completed).apply()
        isSetupWizardCompleted.value = completed
    }

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

    val appLockEnabled = MutableStateFlow(prefs.getBoolean("app_lock_enabled", false))

    fun hasAppLockPin(): Boolean = !prefs.getString("app_lock_pin", null).isNullOrBlank()

    fun setAppLockPin(pin: String) {
        prefs.edit().putString("app_lock_pin", pin).apply()
    }

    fun setAppLockEnabled(enabled: Boolean) {
        if (enabled && !hasAppLockPin()) return
        prefs.edit().putBoolean("app_lock_enabled", enabled).apply()
        appLockEnabled.value = enabled
    }

    fun verifyAppLockPin(input: String): Boolean {
        return input.isNotBlank() && prefs.getString("app_lock_pin", null) == input
    }

    val cloudBackupEnabled = MutableStateFlow(prefs.getBoolean("cloud_backup_enabled", true))

    fun setCloudBackupEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("cloud_backup_enabled", enabled).apply()
        cloudBackupEnabled.value = enabled
    }

    val ocrLanguage = MutableStateFlow(prefs.getString("ocr_language", "English") ?: "English")

    fun setOcrLanguage(lang: String) {
        prefs.edit().putString("ocr_language", lang).apply()
        ocrLanguage.value = lang
    }

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
                    syncStatusMessage.value = "Cloud sync isn't set up yet (missing google-services.json)."
                }
                user == null || user.uid.startsWith("guest_") -> {
                    isSyncing.value = false
                    syncStatusMessage.value = "Sign in with a real account to enable cloud sync."
                }
                else -> {
                    try {
                        val docs = documents.value
                        if (docs.isEmpty()) {
                            isSyncing.value = false
                            syncStatusMessage.value = "Nothing to sync yet."
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

    val isOfflineMode = MutableStateFlow(false)
    fun toggleOfflineMode() {
        isOfflineMode.value = !isOfflineMode.value
    }

    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow("All")
    val viewModeGrid = MutableStateFlow(false)

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

    val activeDocument = MutableStateFlow<ScannedDocument?>(null)
    val capturedBitmaps = MutableStateFlow<List<Bitmap>>(emptyList())
    val selectedScanMode = MutableStateFlow("Document")
    val flashEnabled = MutableStateFlow(false)
    val hdModeEnabled = MutableStateFlow(true)
    val selectedFilter = MutableStateFlow("Original")

    fun addCapturedBitmap(bitmap: Bitmap) {
        capturedBitmaps.value = capturedBitmaps.value + bitmap
    }

    fun updateCapturedBitmap(index: Int, bitmap: Bitmap) {
        val list = capturedBitmaps.value.toMutableList()
        if (index in list.indices) {
            list[index] = bitmap
            capturedBitmaps.value = list
        }
    }

    fun removeCapturedBitmap(index: Int) {
        val list = capturedBitmaps.value.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            capturedBitmaps.value = list
        }
    }

    fun clearCapturedBitmaps() {
        capturedBitmaps.value = emptyList()
    }

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
    val processingState = MutableStateFlow(ProcessingState())

    init {
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
                        message = "👋 Hello! I'm ScanPro AI. You can ask me to summarize PDFs, translate text into 50+ languages, extract table data, or answer document questions!",
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
                        message = "📄 I can automatically extract line items, calculate totals, convert currencies, and summarize vendor payment terms!",
                        timestamp = System.currentTimeMillis() - 120000
                    )
                )
                chatDao.insertAll(initialList)
            }
        }
    }

    fun sendChatMessage(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            val userMsg = ChatMessageEntity(
                sender = "user",
                message = text,
                timestamp = System.currentTimeMillis()
            )
            chatDao.insertChatMessage(userMsg)

            isAiThinking.value = true
            val contextText = activeDocument.value?.extractedText ?: "General Document Assistant"
            val response = if (isOfflineMode.value) {
                "Offline AI Response for: $text"
            } else {
                geminiService.askDocumentQuestion(contextText, text)
            }
            val aiMsg = ChatMessageEntity(
                sender = "ai",
                message = response,
                timestamp = System.currentTimeMillis()
            )
            chatDao.insertChatMessage(aiMsg)
            isAiThinking.value = false
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            chatDao.clearAllMessages()
        }
    }

    private suspend fun seedInitialDocuments() {
        val app = getApplication<Application>()
        val sample1File = PdfEngine.createPdfFromText(
            app,
            "AI_Scan_Contract_2026",
            "SCANPRO AI MASTER SERVICE AGREEMENT\n\n1. Purpose: AI-Powered Document Management & Automation.\n2. Security: End-to-end cloud encryption, biometric vault protection."
        )

        val sample2File = PdfEngine.createPdfFromText(
            app,
            "Passport_ID_Scan",
            "IDENTITY SCAN VERIFICATION\nName: Alex Vance\nID No: SP-992014-X"
        )

        val sample3File = PdfEngine.createPdfFromText(
            app,
            "Quarterly_Financial_Report",
            "Q2 FINANCIAL SUMMARY & INSIGHTS\nTotal Revenue: $4,250,000\nNet Growth: +34%"
        )

        dao.insertDocument(
            ScannedDocument(
                title = "[Sample] AI_Scan_Contract_2026.pdf",
                filePath = sample1File.absolutePath,
                fileType = "PDF",
                fileSizeBytes = sample1File.length(),
                pageCount = 1,
                category = "Documents",
                extractedText = "SCANPRO AI MASTER SERVICE AGREEMENT 1. Purpose: AI-Powered Document Management",
                summary = "Master Service Agreement sample document.",
                isFavorite = true,
                createdAt = System.currentTimeMillis() - 86400000L
            )
        )

        dao.insertDocument(
            ScannedDocument(
                title = "[Sample] Passport_ID_Scan.pdf",
                filePath = sample2File.absolutePath,
                fileType = "PDF",
                fileSizeBytes = sample2File.length(),
                pageCount = 1,
                category = "ID Card",
                extractedText = "IDENTITY SCAN VERIFICATION Name: Alex Vance ID No: SP-992014-X",
                summary = "Passport identity scan sample document.",
                isFavorite = false,
                createdAt = System.currentTimeMillis() - 172800000L
            )
        )

        dao.insertDocument(
            ScannedDocument(
                title = "[Sample] Q2_Financial_Report.pdf",
                filePath = sample3File.absolutePath,
                fileType = "PDF",
                fileSizeBytes = sample3File.length(),
                pageCount = 1,
                category = "Receipts",
                extractedText = "Q2 FINANCIAL SUMMARY Revenue: $4,250,000 Net Growth: +34%",
                summary = "Quarterly financial summary report.",
                isFavorite = true,
                createdAt = System.currentTimeMillis() - 259200000L
            )
        )
    }

    fun trySampleDocument() {
        viewModelScope.launch {
            seedInitialDocuments()
        }
    }

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
            docs.forEach { doc ->
                try {
                    val file = File(doc.filePath)
                    if (file.exists()) file.delete()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                dao.deleteDocument(doc)
            }
            if (activeDocument.value != null && docs.any { it.id == activeDocument.value!!.id }) {
                activeDocument.value = null
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

    fun extractTextFromDocument(doc: ScannedDocument, onResult: (String) -> Unit) {
        viewModelScope.launch {
            if (!doc.extractedText.isNullOrBlank()) {
                onResult(doc.extractedText)
                return@launch
            }
            val file = File(doc.filePath)
            val bmp = PdfEngine.renderPdfPageToBitmap(getApplication(), file, 0)
            val extracted = if (bmp != null) {
                geminiService.extractTextFromImage(bmp, ocrLanguage.value)
            } else {
                "Unable to render image for OCR."
            }
            dao.updateOcrResult(doc.id, extracted, doc.summary, doc.ocrSummary)
            setActiveDocument(doc.copy(extractedText = extracted))
            onResult(extracted)
        }
    }

    fun runAiOcrOnDocument(doc: ScannedDocument) {
        viewModelScope.launch {
            processingState.value = ProcessingState(isProcessing = true, statusText = "Extracting OCR text with Gemini AI...")
            val file = File(doc.filePath)
            val bmp = PdfEngine.renderPdfPageToBitmap(getApplication(), file, 0)
            val text = if (bmp != null) {
                geminiService.extractTextFromImage(bmp, ocrLanguage.value)
            } else {
                "No text could be extracted."
            }
            val updated = doc.copy(extractedText = text, updatedAt = System.currentTimeMillis())
            dao.updateDocument(updated)
            setActiveDocument(updated)
            processingState.value = ProcessingState(isProcessing = false, completedMessage = "OCR Text extracted successfully!")
        }
    }

    fun runAiSummarizeDocument(doc: ScannedDocument) {
        viewModelScope.launch {
            processingState.value = ProcessingState(isProcessing = true, statusText = "Generating AI Summary...")
            val text = doc.extractedText ?: doc.title
            val sum = geminiService.summarizeDocument(text)
            val updated = doc.copy(summary = sum, ocrSummary = sum, updatedAt = System.currentTimeMillis())
            dao.updateDocument(updated)
            setActiveDocument(updated)
            processingState.value = ProcessingState(isProcessing = false, completedMessage = "Summary ready!")
        }
    }

    fun runAiTranslateDocument(doc: ScannedDocument, language: String) {
        viewModelScope.launch {
            processingState.value = ProcessingState(isProcessing = true, statusText = "Translating document to $language...")
            val text = doc.extractedText ?: doc.title
            val trans = geminiService.translateDocument(text, language)
            val updated = doc.copy(translatedText = trans, updatedAt = System.currentTimeMillis())
            dao.updateDocument(updated)
            setActiveDocument(updated)
            processingState.value = ProcessingState(isProcessing = false, completedMessage = "Translated to $language!")
        }
    }

    fun exportAndShareDocumentWithOcr(doc: ScannedDocument, ocrMode: OcrPdfMode) {
        viewModelScope.launch {
            processingState.value = ProcessingState(isProcessing = true, statusText = "Exporting PDF with OCR...")
            val file = File(doc.filePath)
            val count = PdfEngine.getPdfPageCount(getApplication(), file)
            val pages = mutableListOf<ScannedPageData>()
            for (i in 0 until count) {
                val bmp = PdfEngine.renderPdfPageToBitmap(getApplication(), file, i)
                if (bmp != null) {
                    pages.add(ScannedPageData(bmp, doc.extractedText, i + 1))
                }
            }
            val out = PdfEngine.createPdfWithOcr(getApplication(), pages, doc.title, ocrMode)
            PdfEngine.sharePdf(getApplication(), out, doc.title)
            processingState.value = ProcessingState(isProcessing = false)
        }
    }

    fun exportOcrTextToPdfAndShare(title: String, text: String) {
        viewModelScope.launch {
            val file = PdfEngine.exportOcrTextToPdf(getApplication(), title, text)
            PdfEngine.sharePdf(getApplication(), file, title)
        }
    }

    fun saveScannedDocument(
        title: String,
        category: String,
        includeOcr: Boolean,
        ocrMode: OcrPdfMode,
        filterMode: DocumentFilterMode,
        shareAfterSaving: Boolean,
        password: String?,
        onSaved: () -> Unit
    ) {
        viewModelScope.launch {
            processingState.value = ProcessingState(isProcessing = true, statusText = "Compiling PDF document...")
            val bitmaps = capturedBitmaps.value
            if (bitmaps.isEmpty()) {
                processingState.value = ProcessingState(isProcessing = false, errorMessage = "No captured pages to save.")
                return@launch
            }

            val app = getApplication<Application>()
            val pages = bitmaps.mapIndexed { idx, bmp ->
                ScannedPageData(
                    bitmap = bmp,
                    ocrText = if (includeOcr) "Page ${idx + 1} Scanned Content" else null,
                    pageIndex = idx + 1,
                    filterMode = filterMode
                )
            }

            var generatedPdf = PdfEngine.createPdfWithOcr(app, pages, title, if (includeOcr) ocrMode else OcrPdfMode.NONE)

            if (!password.isNullOrBlank()) {
                generatedPdf = PdfEngine.protectPdf(app, generatedPdf, password)
            }

            val newDoc = ScannedDocument(
                title = generatedPdf.name,
                filePath = generatedPdf.absolutePath,
                fileType = "PDF",
                fileSizeBytes = generatedPdf.length(),
                pageCount = bitmaps.size,
                category = category,
                isLocked = !password.isNullOrBlank(),
                password = password,
                createdAt = System.currentTimeMillis()
            )

            val id = dao.insertDocument(newDoc)
            val savedDoc = newDoc.copy(id = id)
            setActiveDocument(savedDoc)
            clearCapturedBitmaps()

            if (shareAfterSaving) {
                PdfEngine.sharePdf(app, generatedPdf, savedDoc.title)
            }

            processingState.value = ProcessingState(isProcessing = false, completedMessage = "Document saved successfully!")
            onSaved()
        }
    }

    fun signDocument(doc: ScannedDocument, signatureBitmap: Bitmap) {
        viewModelScope.launch {
            processingState.value = ProcessingState(isProcessing = true, statusText = "Applying signature...")
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
            processingState.value = ProcessingState(isProcessing = false, completedMessage = "Document signed!")
        }
    }

    fun mergeDocuments(docsToMerge: List<ScannedDocument>) {
        viewModelScope.launch {
            processingState.value = ProcessingState(isProcessing = true, statusText = "Merging documents...")
            val files = docsToMerge.map { File(it.filePath) }.filter { it.exists() }
            if (files.size < 2) return@launch
            val mergedFile = PdfEngine.mergePdfs(getApplication(), files)
            val newDoc = ScannedDocument(
                title = mergedFile.name,
                filePath = mergedFile.absolutePath,
                fileType = "PDF",
                fileSizeBytes = mergedFile.length(),
                pageCount = docsToMerge.sumOf { it.pageCount },
                category = docsToMerge.first().category,
                createdAt = System.currentTimeMillis()
            )
            val id = dao.insertDocument(newDoc)
            setActiveDocument(newDoc.copy(id = id))
            processingState.value = ProcessingState(isProcessing = false, completedMessage = "Merged successfully!")
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

            val newSize = outputFile.length()
            val message = if (originalSize > 0) {
                val percent = ((originalSize - newSize) * 100.0 / originalSize)
                if (percent >= 1.0) "Compressed PDF by ${percent.toInt()}%!" else "Compression complete."
            } else {
                "Compression complete."
            }

            processingState.value = ProcessingState(
                isProcessing = false,
                completedMessage = message
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

    fun splitDocument(doc: ScannedDocument, rangeSpec: String = "") {
        viewModelScope.launch {
            processingState.value = ProcessingState(isProcessing = true, progress = 0.4f, statusText = "Splitting PDF document...")
            val inputFile = File(doc.filePath)
            val totalPages = PdfEngine.getPdfPageCount(getApplication(), inputFile)

            val outputFiles = try {
                if (rangeSpec.isBlank()) {
                    PdfEngine.splitPdf(getApplication(), inputFile, splitInterval = 1)
                } else {
                    val groups = PdfEngine.parsePageRangeSpec(rangeSpec, totalPages)
                    if (groups.isEmpty()) {
                        processingState.value = ProcessingState(
                            isProcessing = false,
                            errorMessage = "Page range didn't match any pages."
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

    fun convertPdfToExcel(doc: ScannedDocument, onComplete: (ScannedDocument) -> Unit) {
        viewModelScope.launch {
            processingState.value = ProcessingState(isProcessing = true, progress = 0.5f, statusText = "Converting PDF to Excel...")
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
            processingState.value = ProcessingState(isProcessing = true, progress = 0.5f, statusText = "Converting PDF to Word...")
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
            processingState.value = ProcessingState(isProcessing = true, progress = 0.5f, statusText = "Generating PowerPoint...")
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
            processingState.value = ProcessingState(isProcessing = false, completedMessage = "Converted to PowerPoint successfully!")
            onComplete(insertedDoc)
        }
    }
}
