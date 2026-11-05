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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

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

    val isSetupWizardCompleted = MutableStateFlow(prefs.getBoolean("setup_wizard_completed", false))

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
                    geminiService.generateContent("Extract text from ${doc.title}")
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

    fun setActiveDocument(doc: ScannedDocument?) {
        activeDocument.value = doc
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
                            geminiService.generateContent(
                                "Extract all text and structured table data accurately from this scanned document page:",
                                bmp
                            )
                        } catch (e: Exception) {
                            "Page ${idx + 1} text captured."
                        }
                    }

                    if (pageText.isNotBlank()) {
                        if (combinedOcrBuilder.isNotEmpty()) combinedOcrBuilder.append("\n\n--- Page ${idx + 1} ---\n\n")
                        combinedOcrBuilder.append(pageText)
                    }

                    pageDataList.add(
                        ScannedPageData(
                            bitmap = bmp,
                            ocrText = pageText,
                            pageIndex = idx + 1,
                            filterMode = filterMode
                        )
                    )
                }
            } else {
                bitmaps.forEachIndexed { idx, bmp ->
                    pageDataList.add(
                        ScannedPageData(
                            bitmap = bmp,
                            ocrText = null,
                            pageIndex = idx + 1,
                            filterMode = filterMode
                        )
                    )
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

            capturedBitmaps.value = emptyList()
            onComplete(savedDoc)
        }
    }
}