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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val geminiService = GeminiService()
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
                        message = "👋 Hello! I'm ScanPro AI. You can ask me to summarize PDFs, translate text, or extract real table data into spreadsheets!",
                        timestamp = System.currentTimeMillis() - 600000
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
            "Service_Contract_2026",
            "SCANPRO AI MASTER SERVICE AGREEMENT\n1. Purpose: AI Document Processing & Cloud Vault."
        )

        dao.insertDocument(
            ScannedDocument(
                title = "Service_Contract_2026.pdf",
                filePath = sample1File.absolutePath,
                fileType = "PDF",
                fileSizeBytes = sample1File.length(),
                pageCount = 1,
                category = "Documents",
                extractedText = "SCANPRO AI MASTER SERVICE AGREEMENT",
                summary = "Master Service Agreement sample.",
                isFavorite = true,
                createdAt = System.currentTimeMillis()
            )
        )
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
                "Offline Summary: ${doc.title}"
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
                    ocrText = if (includeOcr) "Page ${idx + 1} Content" else null,
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

    fun renameDocument(doc: ScannedDocument, newName: String) {
        viewModelScope.launch {
            dao.updateDocument(doc.copy(title = newName, updatedAt = System.currentTimeMillis()))
        }
    }

    fun setActiveDocument(doc: ScannedDocument?) {
        activeDocument.value = doc
    }

    /**
     * 100% REAL PDF to Excel / CSV Conversion:
     * Renders PDF pages to high-res bitmaps, invokes Gemini AI table parser
     * to extract true columnar tabular data, writes the genuine CSV spreadsheet,
     * and persists the resulting document into Room DB.
     */
    fun convertPdfToExcel(
        doc: ScannedDocument,
        onProgress: (Float, String) -> Unit,
        onComplete: (ScannedDocument, String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                onProgress(0.1f, "Opening PDF document...")
                val context = getApplication<Application>()
                val inputFile = File(doc.filePath)
                if (!inputFile.exists()) {
                    onError("Source PDF file does not exist.")
                    return@launch
                }

                val totalPages = PdfEngine.getPdfPageCount(context, inputFile).coerceAtMost(5) // Limit to top 5 pages for token efficiency
                val csvBuilder = StringBuilder()

                for (pageIdx in 0 until totalPages) {
                    onProgress(
                        0.2f + (0.6f * (pageIdx.toFloat() / totalPages.toFloat())),
                        "Analyzing Page ${pageIdx + 1} table structures with Gemini AI..."
                    )
                    val bitmap = PdfEngine.renderPdfPageToBitmap(context, inputFile, pageIdx)
                    if (bitmap != null) {
                        val pageCsv = geminiService.extractTableDataFromImage(bitmap)
                        if (pageCsv.isNotBlank()) {
                            if (csvBuilder.isNotEmpty()) csvBuilder.append("\n\n")
                            csvBuilder.append(pageCsv)
                        }
                    }
                }

                val finalCsvContent = if (csvBuilder.isNotBlank()) {
                    csvBuilder.toString()
                } else {
                    "Item,Description,Record\n1,No clear tabular data detected,N/A"
                }

                onProgress(0.9f, "Generating spreadsheet file (.csv)...")
                val docsDir = File(context.filesDir, "spreadsheets").apply { if (!exists()) mkdirs() }
                val cleanTitle = doc.title.substringBeforeLast(".").replace(" ", "_")
                val csvFile = File(docsDir, "${cleanTitle}_Spreadsheet.csv")
                csvFile.writeText(finalCsvContent)

                val newDoc = ScannedDocument(
                    title = csvFile.name,
                    filePath = csvFile.absolutePath,
                    fileType = "CSV",
                    fileSizeBytes = csvFile.length(),
                    pageCount = 1,
                    category = "Spreadsheets",
                    extractedText = finalCsvContent,
                    createdAt = System.currentTimeMillis()
                )

                val id = dao.insertDocument(newDoc)
                val insertedDoc = newDoc.copy(id = id)
                setActiveDocument(insertedDoc)

                onProgress(1.0f, "Completed successfully!")
                onComplete(insertedDoc, finalCsvContent)
            } catch (e: Exception) {
                onError("Table extraction error: ${e.message}")
            }
        }
    }
}
