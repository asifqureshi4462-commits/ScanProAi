package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scanned_documents")
data class ScannedDocument(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val filePath: String,
    val thumbnailUri: String? = null,
    val fileType: String = "PDF", // PDF, DOCX, XLSX, PPT, JPG, PNG, TXT
    val fileSizeBytes: Long = 0L,
    val pageCount: Int = 1,
    val category: String = "Documents", // Documents, ID Card, Receipts, Books, Notes, Uncategorized
    val extractedText: String? = null,
    val translatedText: String? = null,
    val summary: String? = null,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val tags: String = "",
    val isLocked: Boolean = false,
    val password: String? = null
)
