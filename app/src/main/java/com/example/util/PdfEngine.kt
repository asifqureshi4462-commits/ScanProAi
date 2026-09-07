package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object PdfEngine {

    suspend fun createPdfFromBitmaps(
        context: Context,
        bitmaps: List<Bitmap>,
        outputFileName: String = "Scan_${System.currentTimeMillis()}.pdf"
    ): File = withContext(Dispatchers.IO) {
        val pdfDocument = PdfDocument()
        val docsDir = File(context.filesContextDir(), "scans")
        if (!docsDir.exists()) docsDir.mkdirs()

        val outputFile = File(docsDir, outputFileName)

        bitmaps.forEachIndexed { index, bitmap ->
            val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, index + 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            canvas.drawBitmap(bitmap, 0f, 0f, null)
            pdfDocument.finishPage(page)
        }

        FileOutputStream(outputFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        outputFile
    }

    suspend fun createPdfFromText(
        context: Context,
        title: String,
        content: String,
        outputFileName: String = "${title.replace(" ", "_")}.pdf"
    ): File = withContext(Dispatchers.IO) {
        val pdfDocument = PdfDocument()
        val docsDir = File(context.filesContextDir(), "documents")
        if (!docsDir.exists()) docsDir.mkdirs()

        val outputFile = File(docsDir, outputFileName)
        val pageWidth = 595 // A4 width in pt
        val pageHeight = 842 // A4 height in pt

        val paintTitle = Paint().apply {
            color = Color.BLACK
            textSize = 20f
            isFakeBoldText = true
        }

        val paintBody = Paint().apply {
            color = Color.DKGRAY
            textSize = 12f
        }

        val marginLeft = 40f
        val marginBottom = pageHeight - 50f
        var pageNumber = 1

        var page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = page.canvas
        var y = 50f
        canvas.drawText(title, marginLeft, y, paintTitle)
        y += 40f

        fun newPage() {
            pdfDocument.finishPage(page)
            pageNumber += 1
            page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            canvas = page.canvas
            y = 50f
        }

        fun drawLine(text: String) {
            if (y > marginBottom) newPage()
            canvas.drawText(text, marginLeft, y, paintBody)
            y += 18f
        }

        // No content is ever silently dropped: whenever a line would overflow
        // the current page, we start a new page instead of breaking out.
        val lines = content.split("\n")
        for (line in lines) {
            if (line.isEmpty()) {
                y += 12f
                if (y > marginBottom) newPage()
                continue
            }
            val words = line.split(" ")
            var currentLine = ""
            for (word in words) {
                val candidate = if (currentLine.isEmpty()) word else "$currentLine $word"
                if (paintBody.measureText(candidate) < (pageWidth - 80)) {
                    currentLine = candidate
                } else {
                    drawLine(currentLine)
                    currentLine = word
                }
            }
            if (currentLine.isNotEmpty()) {
                drawLine(currentLine)
            }
            y += 6f
        }

        pdfDocument.finishPage(page)
        FileOutputStream(outputFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        outputFile
    }

    suspend fun renderPdfPageToBitmap(context: Context, pdfFile: File, pageIndex: Int = 0): Bitmap? = withContext(Dispatchers.IO) {
        try {
            if (!pdfFile.exists() || pdfFile.length() == 0L) return@withContext null
            val fileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val pdfRenderer = PdfRenderer(fileDescriptor)
            if (pageIndex >= pdfRenderer.pageCount) {
                pdfRenderer.close()
                fileDescriptor.close()
                return@withContext null
            }
            val page = pdfRenderer.openPage(pageIndex)
            val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            pdfRenderer.close()
            fileDescriptor.close()
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getPdfPageCount(context: Context, pdfFile: File): Int = withContext(Dispatchers.IO) {
        try {
            if (!pdfFile.exists() || pdfFile.length() == 0L) return@withContext 1
            val fileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val pdfRenderer = PdfRenderer(fileDescriptor)
            val count = pdfRenderer.pageCount
            pdfRenderer.close()
            fileDescriptor.close()
            count
        } catch (e: Exception) {
            1
        }
    }

    suspend fun addWatermarkToPdf(
        context: Context,
        inputPdf: File,
        watermarkText: String,
        outputFileName: String = "Watermarked_${inputPdf.name}"
    ): File = withContext(Dispatchers.IO) {
        val pageCount = getPdfPageCount(context, inputPdf)
        val newBitmaps = mutableListOf<Bitmap>()

        for (i in 0 until pageCount) {
            val bitmap = renderPdfPageToBitmap(context, inputPdf, i) ?: continue
            val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(mutableBitmap)

            val paint = Paint().apply {
                color = Color.argb(100, 0, 242, 254) // Semi transparent cyan
                textSize = (mutableBitmap.width / 10).toFloat()
                isFakeBoldText = true
                alpha = 90
            }

            canvas.save()
            canvas.rotate(-30f, (mutableBitmap.width / 2).toFloat(), (mutableBitmap.height / 2).toFloat())
            val textWidth = paint.measureText(watermarkText)
            canvas.drawText(
                watermarkText,
                (mutableBitmap.width - textWidth) / 2,
                (mutableBitmap.height / 2).toFloat(),
                paint
            )
            canvas.restore()

            newBitmaps.add(mutableBitmap)
        }

        if (newBitmaps.isNotEmpty()) {
            createPdfFromBitmaps(context, newBitmaps, outputFileName)
        } else {
            inputPdf
        }
    }

    suspend fun addSignatureToPdf(
        context: Context,
        inputPdf: File,
        signatureBitmap: Bitmap,
        outputFileName: String = "Signed_${inputPdf.name}"
    ): File = withContext(Dispatchers.IO) {
        val pageCount = getPdfPageCount(context, inputPdf)
        val newBitmaps = mutableListOf<Bitmap>()

        for (i in 0 until pageCount) {
            val bitmap = renderPdfPageToBitmap(context, inputPdf, i) ?: continue
            val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            if (i == pageCount - 1) { // Draw signature on last page
                val canvas = Canvas(mutableBitmap)
                val sigWidth = mutableBitmap.width / 3
                val sigHeight = sigWidth * signatureBitmap.height / signatureBitmap.width
                val scaledSig = Bitmap.createScaledBitmap(signatureBitmap, sigWidth, sigHeight, true)
                canvas.drawBitmap(
                    scaledSig,
                    (mutableBitmap.width - sigWidth - 40).toFloat(),
                    (mutableBitmap.height - sigHeight - 80).toFloat(),
                    null
                )
            }
            newBitmaps.add(mutableBitmap)
        }

        createPdfFromBitmaps(context, newBitmaps, outputFileName)
    }

    suspend fun compressPdf(
        context: Context,
        inputPdf: File,
        outputFileName: String = "Compressed_${inputPdf.name}"
    ): File = withContext(Dispatchers.IO) {
        val pageCount = getPdfPageCount(context, inputPdf)
        val newBitmaps = mutableListOf<Bitmap>()

        for (i in 0 until pageCount) {
            val bitmap = renderPdfPageToBitmap(context, inputPdf, i) ?: continue
            // Downscale bitmap for compression
            val scaledWidth = (bitmap.width * 0.7).toInt()
            val scaledHeight = (bitmap.height * 0.7).toInt()
            val compressedBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
            newBitmaps.add(compressedBitmap)
        }

        createPdfFromBitmaps(context, newBitmaps, outputFileName)
    }

    suspend fun rotatePdfPages(
        context: Context,
        inputPdf: File,
        degrees: Float = 90f,
        outputFileName: String = "Rotated_${inputPdf.name}"
    ): File = withContext(Dispatchers.IO) {
        val pageCount = getPdfPageCount(context, inputPdf)
        val newBitmaps = mutableListOf<Bitmap>()

        for (i in 0 until pageCount) {
            val bitmap = renderPdfPageToBitmap(context, inputPdf, i) ?: continue
            val matrix = Matrix().apply { postRotate(degrees) }
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            newBitmaps.add(rotated)
        }

        createPdfFromBitmaps(context, newBitmaps, outputFileName)
    }

    suspend fun splitPdf(
        context: Context,
        inputPdf: File,
        splitInterval: Int = 1,
        outputPrefix: String = "Split_${inputPdf.nameWithoutExtension}"
    ): List<File> = withContext(Dispatchers.IO) {
        val pageCount = getPdfPageCount(context, inputPdf)
        val outputFiles = mutableListOf<File>()

        if (pageCount <= 0) return@withContext emptyList()

        var pageIndex = 0
        var partNumber = 1

        while (pageIndex < pageCount) {
            val partBitmaps = mutableListOf<Bitmap>()
            val endPage = (pageIndex + splitInterval).coerceAtMost(pageCount)

            for (i in pageIndex until endPage) {
                renderPdfPageToBitmap(context, inputPdf, i)?.let { partBitmaps.add(it) }
            }

            if (partBitmaps.isNotEmpty()) {
                val fileName = "${outputPrefix}_Part${partNumber}.pdf"
                val splitFile = createPdfFromBitmaps(context, partBitmaps, fileName)
                outputFiles.add(splitFile)
                partNumber++
            }
            pageIndex = endPage
        }
        outputFiles
    }

    /**
     * Parses a user-typed page range spec like "1-3, 5, 8-10" into a list of
     * page-number groups (1-indexed, clamped to [1, maxPage]). Each group in
     * the returned list becomes one output PDF in [extractPageRanges]. Empty
     * or invalid groups are silently dropped; returns an empty outer list if
     * nothing valid was found.
     */
    fun parsePageRangeSpec(spec: String, maxPage: Int): List<List<Int>> {
        if (maxPage <= 0) return emptyList()
        return spec.split(",")
            .map { group ->
                val trimmed = group.trim()
                if (trimmed.isEmpty()) {
                    emptyList()
                } else if (trimmed.contains("-")) {
                    val parts = trimmed.split("-")
                    val start = parts.getOrNull(0)?.trim()?.toIntOrNull()
                    val end = parts.getOrNull(1)?.trim()?.toIntOrNull()
                    if (start == null) emptyList()
                    else (start..(end ?: start)).filter { it in 1..maxPage }
                } else {
                    val n = trimmed.toIntOrNull()
                    if (n != null && n in 1..maxPage) listOf(n) else emptyList()
                }
            }
            .filter { it.isNotEmpty() }
    }

    /**
     * Extracts exactly the pages the user asked for. Each entry in
     * [pageGroups] (1-indexed page numbers) becomes its own output PDF file,
     * so "1-3, 5, 8-10" produces three real files containing those exact
     * pages — unlike the old behavior, which ignored the typed range
     * entirely and split the document into one file per page.
     */
    suspend fun extractPageRanges(
        context: Context,
        inputPdf: File,
        pageGroups: List<List<Int>>,
        outputPrefix: String = inputPdf.nameWithoutExtension
    ): List<File> = withContext(Dispatchers.IO) {
        val outputFiles = mutableListOf<File>()
        pageGroups.forEachIndexed { idx, pages ->
            if (pages.isEmpty()) return@forEachIndexed
            val bitmaps = pages.mapNotNull { pageNum -> renderPdfPageToBitmap(context, inputPdf, pageNum - 1) }
            if (bitmaps.isNotEmpty()) {
                val fileName = "${outputPrefix}_Part${idx + 1}.pdf"
                outputFiles.add(createPdfFromBitmaps(context, bitmaps, fileName))
            }
        }
        outputFiles
    }

    /**
     * Applies REAL PDF encryption (AES-128 via PDFBox) so the file requires
     * [userPassword] to open in any standard PDF reader. This replaces the
     * old behavior, which only stamped a plaintext "PROTECTED [PIN: ...]"
     * banner onto the page image while leaving the document fully readable
     * and printing the secret PIN directly on the page.
     */
    suspend fun protectPdf(
        context: Context,
        inputPdf: File,
        userPassword: String,
        ownerPassword: String = userPassword,
        outputFileName: String = "Protected_${inputPdf.name}"
    ): File = withContext(Dispatchers.IO) {
        com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(context.applicationContext)

        val pageCount = getPdfPageCount(context, inputPdf)
        val document = com.tom_roush.pdfbox.pdmodel.PDDocument()
        try {
            for (i in 0 until pageCount) {
                val bitmap = renderPdfPageToBitmap(context, inputPdf, i) ?: continue
                val pdRectangle = com.tom_roush.pdfbox.pdmodel.common.PDRectangle(
                    bitmap.width.toFloat(),
                    bitmap.height.toFloat()
                )
                val page = com.tom_roush.pdfbox.pdmodel.PDPage(pdRectangle)
                document.addPage(page)
                val imageXObject = com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
                    .createFromImage(document, bitmap)
                com.tom_roush.pdfbox.pdmodel.PDPageContentStream(document, page).use { stream ->
                    stream.drawImage(imageXObject, 0f, 0f, pdRectangle.width, pdRectangle.height)
                }
            }

            val accessPermission = com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission().apply {
                setCanPrint(true)
                setCanExtractContent(false)
                setCanModify(false)
                setCanModifyAnnotations(false)
                setCanFillInForm(false)
                setCanAssembleDocument(false)
            }
            val protectionPolicy = com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy(
                ownerPassword,
                userPassword,
                accessPermission
            ).apply {
                encryptionKeyLength = 128
            }
            document.protect(protectionPolicy)

            val docsDir = File(context.filesContextDir(), "documents")
            if (!docsDir.exists()) docsDir.mkdirs()
            val outputFile = File(docsDir, outputFileName)
            document.save(outputFile)
            outputFile
        } finally {
            document.close()
        }
    }

    suspend fun applyFilterToPdf(
        context: Context,
        inputPdf: File,
        filterName: String,
        outputFileName: String = "Filtered_${inputPdf.name}"
    ): File = withContext(Dispatchers.IO) {
        val pageCount = getPdfPageCount(context, inputPdf)
        val filteredBitmaps = mutableListOf<Bitmap>()

        for (i in 0 until pageCount) {
            val bitmap = renderPdfPageToBitmap(context, inputPdf, i) ?: continue
            val filtered = applyFilterToBitmap(bitmap, filterName)
            filteredBitmaps.add(filtered)
        }

        createPdfFromBitmaps(context, filteredBitmaps, outputFileName)
    }

    fun applyFilterToBitmap(src: Bitmap, filterName: String): Bitmap {
        if (filterName == "Original") return src
        val mutableBitmap = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(mutableBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val cm = android.graphics.ColorMatrix()

        when (filterName) {
            "Grayscale" -> cm.setSaturation(0f)
            "B&W" -> cm.set(floatArrayOf(
                2f, 2f, 2f, 0f, -256f,
                2f, 2f, 2f, 0f, -256f,
                2f, 2f, 2f, 0f, -256f,
                0f, 0f, 0f, 1f, 0f
            ))
            "Enhance" -> cm.set(floatArrayOf(
                1.2f, 0.1f, 0.1f, 0f, 10f,
                0.1f, 1.2f, 0.1f, 0f, 10f,
                0.1f, 0.1f, 1.2f, 0f, 10f,
                0f, 0f, 0f, 1f, 0f
            ))
            "Invert" -> cm.set(floatArrayOf(
                -1f, 0f, 0f, 0f, 255f,
                0f, -1f, 0f, 0f, 255f,
                0f, 0f, -1f, 0f, 255f,
                0f, 0f, 0f, 1f, 0f
            ))
        }

        paint.colorFilter = android.graphics.ColorMatrixColorFilter(cm)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return mutableBitmap
    }

    suspend fun mergePdfs(
        context: Context,
        pdfFiles: List<File>,
        outputFileName: String = "Merged_${System.currentTimeMillis()}.pdf"
    ): File = withContext(Dispatchers.IO) {
        val allBitmaps = mutableListOf<Bitmap>()
        for (pdf in pdfFiles) {
            val pageCount = getPdfPageCount(context, pdf)
            for (i in 0 until pageCount) {
                renderPdfPageToBitmap(context, pdf, i)?.let { allBitmaps.add(it) }
            }
        }
        createPdfFromBitmaps(context, allBitmaps, outputFileName)
    }

    fun File.filesContextDir(): File = this.parentFile ?: File("/tmp")
    private fun Context.filesContextDir(): File = this.filesDir
}
