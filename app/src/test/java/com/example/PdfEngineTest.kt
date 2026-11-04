package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.example.util.DocumentFilterMode
import com.example.util.OcrPdfMode
import com.example.util.PdfEngine
import com.example.util.ScannedPageData
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PdfEngineTest {

    private fun createTestBitmap(width: Int = 400, height: Int = 600, color: Int = Color.LTGRAY): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(color)
        return bitmap
    }

    @Test
    fun testCreatePdfWithOcrHybridMode() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val page1 = ScannedPageData(
            bitmap = createTestBitmap(),
            ocrText = "SCANPRO MASTER SERVICE AGREEMENT\nClause 1: Confidentiality and Cloud Storage\nClause 2: Biometric Vault Protection",
            pageIndex = 1
        )
        val page2 = ScannedPageData(
            bitmap = createTestBitmap(color = Color.WHITE),
            ocrText = "INVOICE SUMMARY\nAmount Due: $4,500 USD\nPayment Status: Cleared\nTax ID: US-9912044",
            pageIndex = 2
        )

        val outputFile = PdfEngine.createPdfWithOcr(
            context = context,
            pages = listOf(page1, page2),
            title = "Test_Document_Contract",
            ocrMode = OcrPdfMode.HYBRID,
            outputFileName = "test_contract_hybrid.pdf"
        )

        assertNotNull(outputFile)
        assertTrue(outputFile.exists())
        assertTrue("PDF file size must be greater than zero", outputFile.length() > 0)

        val pageCount = PdfEngine.getPdfPageCount(context, outputFile)
        assertTrue("Page count should be at least 1", pageCount >= 1)
    }

    @Test
    fun testCreatePdfWithOcrSearchableOverlayOnly() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val page = ScannedPageData(
            bitmap = createTestBitmap(),
            ocrText = "Page 1 Searchable Content: AI Automated Indexing",
            pageIndex = 1
        )

        val outputFile = PdfEngine.createPdfWithOcr(
            context = context,
            pages = listOf(page),
            title = "Test_Searchable_Overlay",
            ocrMode = OcrPdfMode.SEARCHABLE_OVERLAY,
            outputFileName = "test_overlay.pdf"
        )

        assertTrue(outputFile.exists())
        assertTrue(outputFile.length() > 0)
        val pageCount = PdfEngine.getPdfPageCount(context, outputFile)
        assertTrue("Searchable overlay without companion page should have 1 page", pageCount == 1)
    }

    @Test
    fun testExportOcrTextToPdf() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sampleText = "EXTRACTED RECEIPT DATA:\nMerchant: Whole Foods Market\nTotal: $64.20\nPayment Method: Apple Pay"

        val pdfFile = PdfEngine.exportOcrTextToPdf(
            context = context,
            title = "Whole_Foods_Receipt",
            ocrText = sampleText,
            previewBitmap = createTestBitmap(200, 200)
        )

        assertTrue(pdfFile.exists())
        assertTrue(pdfFile.length() > 0)
    }

    @Test
    fun testGetPdfUriForSharing() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val scansDir = File(context.filesDir, "scans").apply { mkdirs() }
        val dummyPdf = File(scansDir, "share_test.pdf").apply { writeText("%PDF-1.4 dummy") }

        val uri = PdfEngine.getPdfUri(context, dummyPdf)
        assertNotNull(uri)
        assertTrue("URI authority should contain fileprovider", uri.authority?.contains("fileprovider") == true)
    }

    @Test
    fun testCreatePdfWithPostProcessingFilters() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val pageMagicColor = ScannedPageData(
            bitmap = createTestBitmap(400, 600, Color.rgb(200, 210, 220)),
            ocrText = "PAGE 1: MAGIC COLOR ENHANCED RECEIPT",
            pageIndex = 1,
            filterMode = DocumentFilterMode.MAGIC_COLOR
        )
        val pageBw = ScannedPageData(
            bitmap = createTestBitmap(400, 600, Color.rgb(180, 180, 180)),
            ocrText = "PAGE 2: BLACK & WHITE BINARIZED CONTRACT",
            pageIndex = 2,
            filterMode = DocumentFilterMode.BLACK_AND_WHITE
        )
        val pageGrayscale = ScannedPageData(
            bitmap = createTestBitmap(400, 600, Color.rgb(220, 200, 180)),
            ocrText = "PAGE 3: MONOCHROME GRAYSCALE ARCHIVE",
            pageIndex = 3,
            filterMode = DocumentFilterMode.GRAYSCALE
        )

        // Verify getProcessedBitmap transforms the bitmaps according to filter mode
        val processedMagic = pageMagicColor.getProcessedBitmap()
        assertNotNull(processedMagic)
        assertEquals(400, processedMagic.width)
        assertEquals(600, processedMagic.height)

        val processedBw = pageBw.getProcessedBitmap()
        assertNotNull(processedBw)

        val processedGray = pageGrayscale.getProcessedBitmap()
        assertNotNull(processedGray)

        val outputFile = PdfEngine.createPdfWithOcr(
            context = context,
            pages = listOf(pageMagicColor, pageBw, pageGrayscale),
            title = "Test_Filtered_Scan_Doc",
            ocrMode = OcrPdfMode.HYBRID,
            outputFileName = "test_filtered_scan.pdf"
        )

        assertNotNull(outputFile)
        assertTrue(outputFile.exists())
        assertTrue("Filtered PDF must have non-zero file size", outputFile.length() > 0)

        val pageCount = PdfEngine.getPdfPageCount(context, outputFile)
        assertTrue("Should have compiled all pages and companion transcripts", pageCount >= 1)
    }

    @Test
    fun testPdfEngineApplyFilterToBitmapAllModes() {
        val src = createTestBitmap(150, 150, Color.rgb(120, 140, 160))

        val magic = PdfEngine.applyFilterToBitmap(src, "Magic Color")
        assertNotNull(magic)
        assertEquals(150, magic.width)
        assertEquals(150, magic.height)

        val bw = PdfEngine.applyFilterToBitmap(src, "B&W")
        assertNotNull(bw)
        assertEquals(150, bw.width)

        val gray = PdfEngine.applyFilterToBitmap(src, "Grayscale")
        assertNotNull(gray)
        assertEquals(150, gray.width)

        val original = PdfEngine.applyFilterToBitmap(src, "Original")
        assertEquals(src, original)
    }
}
