package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.DocumentDao
import com.example.data.local.DocumentRepository
import com.example.data.local.ScanProDatabase
import com.example.data.local.ScannedDocument
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DocumentDaoTest {

    private lateinit var database: ScanProDatabase
    private lateinit var documentDao: DocumentDao
    private lateinit var repository: DocumentRepository

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ScanProDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        documentDao = database.documentDao()
        repository = DocumentRepository(documentDao)
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun insertAndRetrieveDocumentWithOcrSummaryAndMetadata() = runBlocking {
        val now = System.currentTimeMillis()
        val doc = ScannedDocument(
            title = "Invoice_October_2026.pdf",
            filePath = "/storage/emulated/0/Documents/ScanPro/Invoice_October_2026.pdf",
            thumbnailUri = "file:///storage/emulated/0/Documents/ScanPro/thumb_1.jpg",
            fileType = "PDF",
            fileSizeBytes = 2048500L,
            pageCount = 3,
            category = "Receipts",
            extractedText = "Total Amount Due: $1,250.00\nVendor: Acquired Tech Corp\nDate: Oct 10, 2026",
            summary = "Invoice from Acquired Tech Corp totaling $1,250.00 due immediately.",
            ocrSummary = "Invoice from Acquired Tech Corp totaling $1,250.00 due immediately.",
            ocrLanguage = "en",
            ocrConfidence = 0.98f,
            createdAt = now,
            updatedAt = now,
            scannedTimestamp = now
        )

        val id = repository.insertDocument(doc)
        assertTrue(id > 0)

        val retrieved = repository.getDocumentById(id)
        assertNotNull(retrieved)
        assertEquals("Invoice_October_2026.pdf", retrieved?.title)
        assertEquals("/storage/emulated/0/Documents/ScanPro/Invoice_October_2026.pdf", retrieved?.filePath)
        assertEquals("Invoice from Acquired Tech Corp totaling $1,250.00 due immediately.", retrieved?.ocrSummary)
        assertEquals("Receipts", retrieved?.category)
        assertEquals(now, retrieved?.createdAt)

        // Verify Flow querying
        val docsWithSummary = repository.getDocumentsWithOcrSummary().first()
        assertEquals(1, docsWithSummary.size)

        val allMetadata = repository.allMetadata.first()
        assertEquals(1, allMetadata.size)
        assertEquals(retrieved?.filePath, allMetadata[0].filePath)
    }

    @Test
    fun updateOcrResultAndFilePath() = runBlocking {
        val doc = ScannedDocument(
            title = "Receipt.pdf",
            filePath = "/storage/temp/Receipt.pdf",
            fileSizeBytes = 50000L
        )

        val id = repository.insertDocument(doc)

        // Update OCR result
        repository.updateOcrResult(
            id = id,
            extractedText = "Expense summary: Lunch $24.50",
            ocrSummary = "Lunch expense receipt of $24.50"
        )

        // Update file path
        repository.updateFilePath(id, "/storage/documents/Receipt_Renamed.pdf")

        val updated = repository.getDocumentById(id)
        assertNotNull(updated)
        assertEquals("/storage/documents/Receipt_Renamed.pdf", updated?.filePath)
        assertEquals("Lunch expense receipt of $24.50", updated?.ocrSummary)
        assertEquals("Expense summary: Lunch $24.50", updated?.extractedText)
    }
}
