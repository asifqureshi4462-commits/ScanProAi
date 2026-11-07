package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.LruCache
import androidx.core.content.FileProvider
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import com.tom_roush.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState
import com.tom_roush.pdfbox.util.Matrix
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** Result of one PDF operation. */
sealed class PdfOpResult {
    data class Success(
        val files: List<File>,
        val pageCounts: List<Int> = emptyList(),
        val fileType: String = "PDF",
        val note: String? = null,
        val locked: Boolean = false
    ) : PdfOpResult()

    data class Failure(val message: String) : PdfOpResult()
}

/** Result of looking inside a PDF without opening it fully. */
sealed class PdfProbe {
    data class Ok(val pageCount: Int) : PdfProbe()
    object Encrypted : PdfProbe()
    object Invalid : PdfProbe()
}

enum class CompressLevel(val label: String, val hint: String, val maxDim: Int, val quality: Int) {
    LIGHT("Light", "Best quality, small saving", 2400, 85),
    BALANCED("Balanced", "Good quality, medium saving", 1600, 70),
    STRONG("Strong", "Smallest size, lower quality", 1100, 50)
}

data class ParsedRanges(val groups: List<List<Int>>, val error: String?)

/**
 * Real PDF tools built on PDFBox. Unlike the old PdfEngine functions these keep
 * text / vectors (no rasterising), never overwrite existing files and report
 * real errors instead of fake success.
 */
object PdfToolsEngine {

    private val thumbCache = object : LruCache<String, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    // ---------------------------------------------------------------- helpers

    private fun initPdfBox(context: Context) {
        PDFBoxResourceLoader.init(context.applicationContext)
    }

    fun documentsDir(context: Context): File =
        File(context.filesDir, "documents").apply { if (!exists()) mkdirs() }

    fun scansDir(context: Context): File =
        File(context.filesDir, "scans").apply { if (!exists()) mkdirs() }

    fun workDir(context: Context): File =
        File(context.cacheDir, "pdf_tools").apply { if (!exists()) mkdirs() }

    fun cleanName(name: String): String {
        val cleaned = name.replace(Regex("[\\\\/:*?\"<>|\\u0000-\\u001F]"), "_").trim().trimEnd('.')
        return cleaned.ifBlank { "Document" }.take(80)
    }

    /** Returns a file in [dir] that does not exist yet (adds _1, _2 ... if needed). */
    fun uniqueFile(dir: File, baseName: String, ext: String): File {
        val base = cleanName(baseName.removeSuffix(".$ext").removeSuffix(".${ext.uppercase()}"))
        var f = File(dir, "$base.$ext")
        var i = 1
        while (f.exists()) {
            f = File(dir, "${base}_$i.$ext")
            i++
        }
        return f
    }

    fun friendlyError(t: Throwable): String = when {
        t is InvalidPasswordException -> "Wrong password. Please try again."
        t is OutOfMemoryError -> "This file is too large for the phone's memory. Try a smaller file or fewer pages."
        t is SecurityException -> "This PDF is password protected. Unlock it first."
        t is IOException && (t.message ?: "").contains("header", ignoreCase = true) ->
            "This file is not a valid PDF or it is damaged."
        t is IOException && (t.message ?: "").contains("Missing root", ignoreCase = true) ->
            "This PDF is damaged and cannot be read."
        else -> "Something went wrong: ${t.message ?: t.javaClass.simpleName}"
    }

    private suspend fun guard(block: suspend () -> PdfOpResult): PdfOpResult =
        try {
            block()
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            PdfOpResult.Failure(friendlyError(t))
        }

    // ---------------------------------------------------------------- inspect

    suspend fun probe(file: File): PdfProbe = withContext(Dispatchers.IO) {
        if (!file.exists() || file.length() == 0L) return@withContext PdfProbe.Invalid
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            val count = renderer.pageCount
            if (count > 0) PdfProbe.Ok(count) else PdfProbe.Invalid
        } catch (e: SecurityException) {
            PdfProbe.Encrypted
        } catch (e: Throwable) {
            PdfProbe.Invalid
        } finally {
            try { renderer?.close() } catch (_: Throwable) {}
            try { pfd?.close() } catch (_: Throwable) {}
        }
    }

    /** Creates an unlocked working copy of a password protected PDF. */
    suspend fun unlockToFile(context: Context, input: File, password: String, output: File): PdfOpResult =
        withContext(Dispatchers.IO) {
            guard {
                initPdfBox(context)
                PDDocument.load(input, password).use { doc ->
                    doc.setAllSecurityToBeRemoved(true)
                    doc.save(output)
                }
                PdfOpResult.Success(listOf(output))
            }
        }

    // ---------------------------------------------------------------- page ranges

    /**
     * Parses "1-3, 5, 8-" into groups of 1-based page numbers.
     * Returns an error text when something is invalid so the UI can show it.
     */
    fun parseRanges(spec: String, maxPage: Int): ParsedRanges {
        if (maxPage <= 0) return ParsedRanges(emptyList(), "This PDF has no pages.")
        if (spec.isBlank()) return ParsedRanges(emptyList(), "Enter page numbers, e.g. 1-3, 5")
        val groups = mutableListOf<List<Int>>()
        for (raw in spec.split(",", ";")) {
            val part = raw.trim()
            if (part.isEmpty()) continue
            val pieces = part.split("-").map { it.trim() }
            val pages: List<Int> = try {
                when {
                    pieces.size == 1 -> listOf(pieces[0].toInt())
                    pieces.size == 2 -> {
                        val a = if (pieces[0].isEmpty()) 1 else pieces[0].toInt()
                        val b = if (pieces[1].isEmpty()) maxPage else pieces[1].toInt()
                        (min(a, b)..max(a, b)).toList()
                    }
                    else -> return ParsedRanges(emptyList(), "\"$part\" is not a valid range.")
                }
            } catch (e: NumberFormatException) {
                return ParsedRanges(emptyList(), "\"$part\" is not a valid page number.")
            }
            if (pages.any { it < 1 || it > maxPage }) {
                return ParsedRanges(emptyList(), "\"$part\" is outside 1-$maxPage (this PDF has $maxPage pages).")
            }
            groups.add(pages)
        }
        if (groups.isEmpty()) return ParsedRanges(emptyList(), "Enter page numbers, e.g. 1-3, 5")
        return ParsedRanges(groups, null)
    }

    fun flatPages(groups: List<List<Int>>): List<Int> = groups.flatten().distinct().sorted()

    // ---------------------------------------------------------------- merge

    suspend fun merge(
        context: Context,
        inputs: List<File>,
        outputName: String,
        onProgress: (Float) -> Unit = {}
    ): PdfOpResult = withContext(Dispatchers.IO) {
        guard {
            initPdfBox(context)
            if (inputs.size < 2) return@guard PdfOpResult.Failure("Select at least 2 PDF files to merge.")
            val out = PDDocument()
            val opened = ArrayList<PDDocument>()
            try {
                inputs.forEachIndexed { index, file ->
                    val src = PDDocument.load(file)
                    opened.add(src)
                    for (p in 0 until src.numberOfPages) {
                        out.importPage(src.getPage(p))
                    }
                    onProgress((index + 1f) / inputs.size)
                }
                val dest = uniqueFile(documentsDir(context), outputName, "pdf")
                out.save(dest)
                PdfOpResult.Success(listOf(dest), listOf(out.numberOfPages))
            } finally {
                opened.forEach { try { it.close() } catch (_: Throwable) {} }
                try { out.close() } catch (_: Throwable) {}
            }
        }
    }

    // ---------------------------------------------------------------- pick / split / delete / rearrange

    /**
     * Builds one new PDF per entry of [groups] (1-based page numbers, in the
     * order given). Used for split, extract, delete (keep the rest) and rearrange.
     */
    suspend fun buildFromPages(
        context: Context,
        input: File,
        groups: List<List<Int>>,
        baseName: String,
        onProgress: (Float) -> Unit = {}
    ): PdfOpResult = withContext(Dispatchers.IO) {
        guard {
            initPdfBox(context)
            if (groups.isEmpty() || groups.any { it.isEmpty() }) {
                return@guard PdfOpResult.Failure("No pages selected.")
            }
            val src = PDDocument.load(input)
            try {
                val total = src.numberOfPages
                val files = ArrayList<File>()
                val counts = ArrayList<Int>()
                groups.forEachIndexed { gi, pages ->
                    val out = PDDocument()
                    try {
                        pages.forEach { n ->
                            if (n < 1 || n > total) {
                                return@guard PdfOpResult.Failure("Page $n does not exist (PDF has $total pages).")
                            }
                            out.importPage(src.getPage(n - 1))
                        }
                        val name = if (groups.size == 1) baseName else "${baseName}_Part${gi + 1}"
                        val dest = uniqueFile(documentsDir(context), name, "pdf")
                        out.save(dest)
                        files.add(dest)
                        counts.add(pages.size)
                    } finally {
                        try { out.close() } catch (_: Throwable) {}
                    }
                    onProgress((gi + 1f) / groups.size)
                }
                PdfOpResult.Success(files, counts)
            } finally {
                try { src.close() } catch (_: Throwable) {}
            }
        }
    }

    // ---------------------------------------------------------------- rotate

    suspend fun rotate(
        context: Context,
        input: File,
        pages: List<Int>?, // null = all pages
        degrees: Int,
        outputName: String
    ): PdfOpResult = withContext(Dispatchers.IO) {
        guard {
            initPdfBox(context)
            PDDocument.load(input).use { doc ->
                val total = doc.numberOfPages
                val targets = pages ?: (1..total).toList()
                if (targets.isEmpty()) return@guard PdfOpResult.Failure("No pages selected.")
                targets.forEach { n ->
                    if (n < 1 || n > total) return@guard PdfOpResult.Failure("Page $n does not exist.")
                    val page = doc.getPage(n - 1)
                    page.rotation = (((page.rotation + degrees) % 360) + 360) % 360
                }
                val dest = uniqueFile(documentsDir(context), outputName, "pdf")
                doc.save(dest)
                PdfOpResult.Success(listOf(dest), listOf(total))
            }
        }
    }

    // ---------------------------------------------------------------- watermark

    fun isWatermarkTextSupported(text: String): Boolean =
        text.isNotEmpty() && text.all { it.code in 32..126 || it.code in 160..255 }

    suspend fun watermark(
        context: Context,
        input: File,
        text: String,
        opacity: Float,
        sizePercent: Float, // 0.3 .. 1.0 of the available width
        rgb: Triple<Int, Int, Int>,
        diagonal: Boolean,
        outputName: String,
        onProgress: (Float) -> Unit = {}
    ): PdfOpResult = withContext(Dispatchers.IO) {
        guard {
            initPdfBox(context)
            val clean = text.trim()
            if (!isWatermarkTextSupported(clean)) {
                return@guard PdfOpResult.Failure(
                    "Watermark text supports English letters, numbers and symbols only."
                )
            }
            PDDocument.load(input).use { doc ->
                val font = PDType1Font.HELVETICA_BOLD
                val gs = PDExtendedGraphicsState()
                gs.nonStrokingAlphaConstant = opacity
                gs.strokingAlphaConstant = opacity
                val unitWidth = font.getStringWidth(clean) / 1000f // width at font size 1
                val total = doc.numberOfPages
                for (i in 0 until total) {
                    val page = doc.getPage(i)
                    val box = page.mediaBox
                    val w = box.width
                    val h = box.height
                    val rot = ((page.rotation % 360) + 360) % 360
                    val dispW = if (rot == 90 || rot == 270) h else w
                    val dispH = if (rot == 90 || rot == 270) w else h
                    val avail = if (diagonal) sqrt(dispW * dispW + dispH * dispH) else dispW
                    val fontSize = (avail * sizePercent / unitWidth).coerceIn(8f, 220f)
                    val textWidth = unitWidth * fontSize
                    val displayAngle = if (diagonal) atan2(dispH, dispW) else 0f
                    val angle = displayAngle + Math.toRadians(rot.toDouble()).toFloat()
                    val cx = box.lowerLeftX + w / 2f
                    val cy = box.lowerLeftY + h / 2f
                    // start point so the text is centred on the page centre
                    val dx = -textWidth / 2f
                    val dy = -fontSize * 0.3f
                    val x0 = cx + dx * cos(angle) - dy * sin(angle)
                    val y0 = cy + dx * sin(angle) + dy * cos(angle)

                    PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
                        cs.setGraphicsStateParameters(gs)
                        cs.setNonStrokingColor(rgb.first, rgb.second, rgb.third)
                        cs.beginText()
                        cs.setFont(font, fontSize)
                        cs.setTextMatrix(Matrix.getRotateInstance(angle.toDouble(), x0, y0))
                        cs.showText(clean)
                        cs.endText()
                    }
                    onProgress((i + 1f) / total)
                }
                val dest = uniqueFile(documentsDir(context), outputName, "pdf")
                doc.save(dest)
                PdfOpResult.Success(listOf(dest), listOf(total))
            }
        }
    }

    // ---------------------------------------------------------------- compress

    suspend fun compress(
        context: Context,
        input: File,
        level: CompressLevel,
        outputName: String,
        onProgress: (Float) -> Unit = {}
    ): PdfOpResult = withContext(Dispatchers.IO) {
        guard {
            initPdfBox(context)
            val originalSize = input.length()
            PDDocument.load(input).use { doc ->
                var replaced = 0
                val total = doc.numberOfPages
                for (pi in 0 until total) {
                    val resources = doc.getPage(pi).resources
                    if (resources != null) {
                        val names = resources.xObjectNames.toList()
                        for (name in names) {
                            try {
                                val xo = resources.getXObject(name)
                                if (xo !is PDImageXObject) continue
                                if (xo.isStencil || xo.softMask != null) continue
                                val bmp = xo.image ?: continue
                                if (bmp.width < 64 || bmp.height < 64) {
                                    bmp.recycle()
                                    continue
                                }
                                val longest = max(bmp.width, bmp.height)
                                val scaled = if (longest > level.maxDim) {
                                    val f = level.maxDim.toFloat() / longest
                                    Bitmap.createScaledBitmap(
                                        bmp,
                                        max(1, (bmp.width * f).toInt()),
                                        max(1, (bmp.height * f).toInt()),
                                        true
                                    )
                                } else bmp
                                val baos = ByteArrayOutputStream()
                                scaled.compress(Bitmap.CompressFormat.JPEG, level.quality, baos)
                                val originalLen = xo.stream.length
                                if (baos.size() < originalLen * 0.95) {
                                    val newImage = JPEGFactory.createFromStream(doc, ByteArrayInputStream(baos.toByteArray()))
                                    resources.put(name, newImage)
                                    replaced++
                                }
                                if (scaled !== bmp) scaled.recycle()
                                bmp.recycle()
                            } catch (e: OutOfMemoryError) {
                                throw e
                            } catch (e: Throwable) {
                                // skip images that cannot be re-encoded
                            }
                        }
                    }
                    onProgress((pi + 1f) / total)
                }
                if (replaced == 0) {
                    return@guard PdfOpResult.Failure(
                        "This PDF is already well optimised (or has no large images), so it cannot be made smaller."
                    )
                }
                val dest = uniqueFile(documentsDir(context), outputName, "pdf")
                doc.save(dest)
                if (dest.length() >= originalSize) {
                    dest.delete()
                    return@guard PdfOpResult.Failure(
                        "Compression did not reduce the size of this PDF, so nothing was saved."
                    )
                }
                val saved = ((originalSize - dest.length()) * 100.0 / originalSize).toInt()
                PdfOpResult.Success(
                    listOf(dest), listOf(total),
                    note = "Size reduced by $saved% (${formatSize(originalSize)} to ${formatSize(dest.length())})"
                )
            }
        }
    }

    // ---------------------------------------------------------------- protect / unlock

    suspend fun protect(
        context: Context,
        input: File,
        password: String,
        outputName: String
    ): PdfOpResult = withContext(Dispatchers.IO) {
        guard {
            initPdfBox(context)
            PDDocument.load(input).use { doc ->
                val permission = AccessPermission()
                val policy = StandardProtectionPolicy(password, password, permission)
                policy.encryptionKeyLength = 128
                doc.protect(policy)
                val dest = uniqueFile(documentsDir(context), outputName, "pdf")
                doc.save(dest)
                PdfOpResult.Success(listOf(dest), listOf(doc.numberOfPages), locked = true)
            }
        }
    }

    /** Saves an unlocked copy: [unlockedInput] must already be a decrypted file. */
    suspend fun saveCopy(context: Context, unlockedInput: File, outputName: String): PdfOpResult =
        withContext(Dispatchers.IO) {
            guard {
                val dest = uniqueFile(documentsDir(context), outputName, "pdf")
                unlockedInput.copyTo(dest, overwrite = false)
                val count = (probe(dest) as? PdfProbe.Ok)?.pageCount ?: 1
                PdfOpResult.Success(listOf(dest), listOf(count))
            }
        }

    // ---------------------------------------------------------------- pdf -> images

    suspend fun pdfToImages(
        context: Context,
        input: File,
        pages: List<Int>?, // null = all
        png: Boolean,
        dpi: Int,
        baseName: String,
        onProgress: (Float) -> Unit = {}
    ): PdfOpResult = withContext(Dispatchers.IO) {
        guard {
            var pfd: ParcelFileDescriptor? = null
            var renderer: PdfRenderer? = null
            try {
                pfd = ParcelFileDescriptor.open(input, ParcelFileDescriptor.MODE_READ_ONLY)
                renderer = PdfRenderer(pfd)
                val total = renderer.pageCount
                val targets = pages ?: (1..total).toList()
                if (targets.isEmpty()) return@guard PdfOpResult.Failure("No pages selected.")
                val ext = if (png) "png" else "jpg"
                val files = ArrayList<File>()
                targets.forEachIndexed { idx, n ->
                    if (n < 1 || n > total) return@guard PdfOpResult.Failure("Page $n does not exist.")
                    val page = renderer.openPage(n - 1)
                    val bitmap: Bitmap = try {
                        val scale = dpi / 72f
                        var w = (page.width * scale).toInt()
                        var h = (page.height * scale).toInt()
                        val longest = max(w, h)
                        if (longest > 4096) {
                            val f = 4096f / longest
                            w = (w * f).toInt()
                            h = (h * f).toInt()
                        }
                        val b = Bitmap.createBitmap(max(w, 1), max(h, 1), Bitmap.Config.ARGB_8888)
                        val c = Canvas(b)
                        c.drawColor(Color.WHITE)
                        page.render(b, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        b
                    } finally {
                        page.close()
                    }
                    val dest = uniqueFile(scansDir(context), "${baseName}_Page_${n}", ext)
                    FileOutputStream(dest).use { out ->
                        bitmap.compress(
                            if (png) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG,
                            92,
                            out
                        )
                    }
                    bitmap.recycle()
                    files.add(dest)
                    onProgress((idx + 1f) / targets.size)
                }
                PdfOpResult.Success(files, fileType = ext.uppercase())
            } finally {
                try { renderer?.close() } catch (_: Throwable) {}
                try { pfd?.close() } catch (_: Throwable) {}
            }
        }
    }

    // ---------------------------------------------------------------- thumbnails

    suspend fun thumbnail(file: File, pageIndex: Int, widthPx: Int): Bitmap? = withContext(Dispatchers.IO) {
        val key = "${file.absolutePath}|${file.lastModified()}|$pageIndex|$widthPx"
        thumbCache.get(key)?.let { return@withContext it }
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            if (pageIndex < 0 || pageIndex >= renderer.pageCount) return@withContext null
            val page = renderer.openPage(pageIndex)
            try {
                val ratio = page.height.toFloat() / max(page.width, 1)
                val bmp = Bitmap.createBitmap(widthPx, max(1, (widthPx * ratio).toInt()), Bitmap.Config.ARGB_8888)
                val c = Canvas(bmp)
                c.drawColor(Color.WHITE)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                thumbCache.put(key, bmp)
                bmp
            } finally {
                page.close()
            }
        } catch (e: Throwable) {
            null
        } finally {
            try { renderer?.close() } catch (_: Throwable) {}
            try { pfd?.close() } catch (_: Throwable) {}
        }
    }

    // ---------------------------------------------------------------- share / save to phone

    fun formatSize(bytes: Long): String = when {
        bytes >= 1024L * 1024L -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        bytes >= 1024L -> "${bytes / 1024} KB"
        else -> "$bytes B"
    }

    private fun mimeFor(file: File): String = when (file.extension.lowercase()) {
        "pdf" -> "application/pdf"
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        else -> "application/octet-stream"
    }

    fun share(context: Context, files: List<File>) {
        try {
            val existing = files.filter { it.exists() }
            if (existing.isEmpty()) return
            val uris = ArrayList<Uri>()
            existing.forEach {
                uris.add(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", it))
            }
            val intent = if (uris.size == 1) {
                Intent(Intent.ACTION_SEND).apply {
                    type = mimeFor(existing[0])
                    putExtra(Intent.EXTRA_STREAM, uris[0])
                }
            } else {
                Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    type = if (existing.all { mimeFor(it) == mimeFor(existing[0]) }) mimeFor(existing[0]) else "*/*"
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                }
            }
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val chooser = Intent.createChooser(intent, "Share via")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            // nothing to share
        }
    }

    /**
     * Copies files to the phone's public Downloads/ScanPro folder (Android 10+).
     * Returns the number of files saved, or -1 if not supported on this Android version.
     */
    fun saveToDownloads(context: Context, files: List<File>): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return -1
        var saved = 0
        files.filter { it.exists() }.forEach { file ->
            try {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, file.name)
                    put(MediaStore.Downloads.MIME_TYPE, mimeFor(file))
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/ScanPro")
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        file.inputStream().use { it.copyTo(out) }
                    }
                    saved++
                }
            } catch (e: Exception) {
                // skip this file
            }
        }
        return saved
    }
}
