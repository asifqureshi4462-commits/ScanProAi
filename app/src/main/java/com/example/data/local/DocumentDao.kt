package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for managing scanned document metadata,
 * file paths, timestamps, and OCR result summaries in Room database.
 */
@Dao
interface DocumentDao {

    // --- Query Operations ---

    /**
     * Observes all scanned documents ordered by last updated timestamp descending.
     */
    @Query("SELECT * FROM scanned_documents ORDER BY updatedAt DESC")
    fun getAllDocuments(): Flow<List<ScannedDocument>>

    /**
     * Observes lightweight metadata for all documents without large OCR text blobs.
     */
    @Query("SELECT id, title, filePath, thumbnailUri, fileType, fileSizeBytes, pageCount, category, summary, ocrSummary, isFavorite, createdAt, updatedAt, scannedTimestamp FROM scanned_documents ORDER BY updatedAt DESC")
    fun getAllDocumentMetadata(): Flow<List<DocumentMetadata>>

    /**
     * Retrieves a single scanned document by its primary key ID.
     */
    @Query("SELECT * FROM scanned_documents WHERE id = :id LIMIT 1")
    suspend fun getDocumentById(id: Long): ScannedDocument?

    /**
     * Observes a single document as Flow by ID.
     */
    @Query("SELECT * FROM scanned_documents WHERE id = :id LIMIT 1")
    fun getDocumentByIdFlow(id: Long): Flow<ScannedDocument?>

    /**
     * Retrieves a document by its exact storage file path.
     */
    @Query("SELECT * FROM scanned_documents WHERE filePath = :filePath LIMIT 1")
    suspend fun getDocumentByFilePath(filePath: String): ScannedDocument?

    /**
     * Observes all favorited scanned documents.
     */
    @Query("SELECT * FROM scanned_documents WHERE isFavorite = 1 ORDER BY updatedAt DESC")
    fun getFavoriteDocuments(): Flow<List<ScannedDocument>>

    /**
     * Observes scanned documents belonging to a specific category.
     */
    @Query("SELECT * FROM scanned_documents WHERE category = :category ORDER BY updatedAt DESC")
    fun getDocumentsByCategory(category: String): Flow<List<ScannedDocument>>

    /**
     * Full-text search across titles, OCR extracted text, and summaries.
     */
    @Query("SELECT * FROM scanned_documents WHERE title LIKE '%' || :query || '%' OR extractedText LIKE '%' || :query || '%' OR summary LIKE '%' || :query || '%' OR ocrSummary LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun searchDocuments(query: String): Flow<List<ScannedDocument>>

    /**
     * Retrieves documents that have OCR analysis or summaries available.
     */
    @Query("SELECT * FROM scanned_documents WHERE summary IS NOT NULL OR ocrSummary IS NOT NULL ORDER BY updatedAt DESC")
    fun getDocumentsWithOcrSummary(): Flow<List<ScannedDocument>>

    /**
     * Filters documents by timestamp range (e.g. for calendar/history filters).
     */
    @Query("SELECT * FROM scanned_documents WHERE createdAt BETWEEN :startTimestamp AND :endTimestamp ORDER BY createdAt DESC")
    fun getDocumentsByDateRange(startTimestamp: Long, endTimestamp: Long): Flow<List<ScannedDocument>>

    /**
     * Retrieves the most recently scanned documents up to a specified limit.
     */
    @Query("SELECT * FROM scanned_documents ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentDocuments(limit: Int = 10): Flow<List<ScannedDocument>>

    /**
     * Aggregates total storage bytes occupied by scanned documents.
     */
    @Query("SELECT SUM(fileSizeBytes) FROM scanned_documents")
    fun getTotalStorageUsageBytes(): Flow<Long?>

    /**
     * Counts the total number of scanned documents.
     */
    @Query("SELECT COUNT(*) FROM scanned_documents")
    fun getDocumentCount(): Flow<Int>

    // --- Insert Operations ---

    /**
     * Inserts or replaces a scanned document.
     * @return Generated row ID.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: ScannedDocument): Long

    /**
     * Batch inserts multiple scanned documents.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocuments(documents: List<ScannedDocument>): List<Long>

    // --- Update Operations ---

    /**
     * Updates an existing document entity.
     */
    @Update
    suspend fun updateDocument(document: ScannedDocument)

    /**
     * Updates OCR extracted text, summary, and timestamp for a specific document.
     */
    @Query("UPDATE scanned_documents SET extractedText = :extractedText, summary = :summary, ocrSummary = :ocrSummary, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateOcrResult(id: Long, extractedText: String?, summary: String?, ocrSummary: String?, updatedAt: Long = System.currentTimeMillis())

    /**
     * Updates the file path of a document when moved or renamed on disk.
     */
    @Query("UPDATE scanned_documents SET filePath = :filePath, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateFilePath(id: Long, filePath: String, updatedAt: Long = System.currentTimeMillis())

    /**
     * Updates favorite bookmark state.
     */
    @Query("UPDATE scanned_documents SET isFavorite = :isFavorite, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Long, isFavorite: Boolean, updatedAt: Long = System.currentTimeMillis())

    // --- Delete Operations ---

    /**
     * Deletes a document record from the database.
     */
    @Delete
    suspend fun deleteDocument(document: ScannedDocument)

    /**
     * Deletes a document by ID.
     */
    @Query("DELETE FROM scanned_documents WHERE id = :id")
    suspend fun deleteDocumentById(id: Long)

    /**
     * Deletes multiple document records.
     */
    @Delete
    suspend fun deleteMultipleDocuments(documents: List<ScannedDocument>)

    /**
     * Deletes documents matching a list of IDs.
     */
    @Query("DELETE FROM scanned_documents WHERE id IN (:ids)")
    suspend fun deleteDocumentsByIds(ids: List<Long>)

    /**
     * Clears all scanned documents.
     */
    @Query("DELETE FROM scanned_documents")
    suspend fun clearAll()
}
