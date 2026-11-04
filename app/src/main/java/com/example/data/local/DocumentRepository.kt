package com.example.data.local

import kotlinx.coroutines.flow.Flow

/**
 * Repository layer for Scanned Documents, providing high-level abstraction
 * over Room DocumentDao for document metadata, file paths, timestamps,
 * and OCR summaries.
 */
class DocumentRepository(private val documentDao: DocumentDao) {

    /**
     * Observes all scanned documents ordered by recent updates.
     */
    val allDocuments: Flow<List<ScannedDocument>> = documentDao.getAllDocuments()

    /**
     * Observes lightweight metadata projections for all scanned documents.
     */
    val allMetadata: Flow<List<DocumentMetadata>> = documentDao.getAllDocumentMetadata()

    /**
     * Observes bookmarked/favorite documents.
     */
    val favoriteDocuments: Flow<List<ScannedDocument>> = documentDao.getFavoriteDocuments()

    /**
     * Observes total storage consumed by scanned document files.
     */
    val totalStorageUsageBytes: Flow<Long?> = documentDao.getTotalStorageUsageBytes()

    /**
     * Observes total document count.
     */
    val documentCount: Flow<Int> = documentDao.getDocumentCount()

    /**
     * Fetches documents in a specific category (e.g. ID Card, Receipts, Books, Notes).
     */
    fun getDocumentsByCategory(category: String): Flow<List<ScannedDocument>> =
        documentDao.getDocumentsByCategory(category)

    /**
     * Performs search across title, extracted OCR text, and summaries.
     */
    fun searchDocuments(query: String): Flow<List<ScannedDocument>> =
        documentDao.searchDocuments(query)

    /**
     * Observes all documents that contain OCR summaries.
     */
    fun getDocumentsWithOcrSummary(): Flow<List<ScannedDocument>> =
        documentDao.getDocumentsWithOcrSummary()

    /**
     * Observes documents within a specific timestamp window.
     */
    fun getDocumentsByDateRange(startTimestamp: Long, endTimestamp: Long): Flow<List<ScannedDocument>> =
        documentDao.getDocumentsByDateRange(startTimestamp, endTimestamp)

    /**
     * Observes recently scanned documents up to a given limit.
     */
    fun getRecentDocuments(limit: Int = 10): Flow<List<ScannedDocument>> =
        documentDao.getRecentDocuments(limit)

    /**
     * Retrieves a single document by ID.
     */
    suspend fun getDocumentById(id: Long): ScannedDocument? =
        documentDao.getDocumentById(id)

    /**
     * Observes a single document by ID as a reactive Flow.
     */
    fun getDocumentByIdFlow(id: Long): Flow<ScannedDocument?> =
        documentDao.getDocumentByIdFlow(id)

    /**
     * Retrieves a document by its storage file path.
     */
    suspend fun getDocumentByFilePath(filePath: String): ScannedDocument? =
        documentDao.getDocumentByFilePath(filePath)

    /**
     * Inserts or replaces a scanned document.
     */
    suspend fun insertDocument(document: ScannedDocument): Long =
        documentDao.insertDocument(document)

    /**
     * Batch inserts multiple scanned documents.
     */
    suspend fun insertDocuments(documents: List<ScannedDocument>): List<Long> =
        documentDao.insertDocuments(documents)

    /**
     * Updates an existing document record.
     */
    suspend fun updateDocument(document: ScannedDocument) =
        documentDao.updateDocument(document)

    /**
     * Updates OCR extracted text, summary, and timestamp.
     */
    suspend fun updateOcrResult(id: Long, extractedText: String?, ocrSummary: String?) {
        val now = System.currentTimeMillis()
        documentDao.updateOcrResult(
            id = id,
            extractedText = extractedText,
            summary = ocrSummary,
            ocrSummary = ocrSummary,
            updatedAt = now
        )
    }

    /**
     * Updates the storage file path of a document.
     */
    suspend fun updateFilePath(id: Long, newFilePath: String) {
        documentDao.updateFilePath(id, newFilePath, System.currentTimeMillis())
    }

    /**
     * Toggles bookmark / favorite status.
     */
    suspend fun toggleFavorite(document: ScannedDocument) {
        documentDao.updateFavoriteStatus(
            id = document.id,
            isFavorite = !document.isFavorite,
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Deletes a document record.
     */
    suspend fun deleteDocument(document: ScannedDocument) =
        documentDao.deleteDocument(document)

    /**
     * Deletes a document record by ID.
     */
    suspend fun deleteDocumentById(id: Long) =
        documentDao.deleteDocumentById(id)

    /**
     * Deletes multiple document records.
     */
    suspend fun deleteMultipleDocuments(documents: List<ScannedDocument>) =
        documentDao.deleteMultipleDocuments(documents)

    /**
     * Deletes multiple document records by ID list.
     */
    suspend fun deleteDocumentsByIds(ids: List<Long>) =
        documentDao.deleteDocumentsByIds(ids)
}
