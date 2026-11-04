package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing a scanned document with full metadata,
 * including storage file path, scanning and modification timestamps,
 * and AI/OCR result summaries.
 */
@Entity(
    tableName = "scanned_documents",
    indices = [
        Index(value = ["filePath"]),
        Index(value = ["createdAt"]),
        Index(value = ["scannedTimestamp"]),
        Index(value = ["category"]),
        Index(value = ["isFavorite"])
    ]
)
data class ScannedDocument(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val filePath: String, // Absolute file path on storage
    val thumbnailUri: String? = null,
    val fileType: String = "PDF", // PDF, DOCX, XLSX, PPT, JPG, PNG, TXT
    val fileSizeBytes: Long = 0L,
    val pageCount: Int = 1,
    val category: String = "Documents", // Documents, ID Card, Receipts, Books, Notes, Uncategorized
    val extractedText: String? = null, // Full OCR recognized text
    val translatedText: String? = null,
    val summary: String? = null, // OCR / AI abstract
    val ocrSummary: String? = summary, // Structured OCR result summary
    val ocrLanguage: String? = null, // OCR detected language code (e.g. "en", "es")
    val ocrConfidence: Float? = null, // OCR recognition accuracy confidence (0.0f - 1.0f)
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(), // Creation timestamp
    val updatedAt: Long = System.currentTimeMillis(), // Last modified timestamp
    val scannedTimestamp: Long = createdAt, // Hardware scanning event timestamp
    val tags: String = "",
    val isLocked: Boolean = false,
    val password: String? = null
)

/**
 * Lightweight projection data class for document metadata queries.
 */
data class DocumentMetadata(
    val id: Long,
    val title: String,
    val filePath: String,
    val thumbnailUri: String?,
    val fileType: String,
    val fileSizeBytes: Long,
    val pageCount: Int,
    val category: String,
    val summary: String?,
    val ocrSummary: String?,
    val isFavorite: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val scannedTimestamp: Long
)
