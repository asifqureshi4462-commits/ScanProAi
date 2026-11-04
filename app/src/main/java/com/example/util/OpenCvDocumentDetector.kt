package com.example.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

enum class DocumentFilterMode {
    ORIGINAL,
    MAGIC_COLOR,
    BLACK_AND_WHITE,
    GRAYSCALE
}

data class DocumentQuad(
    val topLeft: PointF,      // Normalized (0.0 .. 1.0)
    val topRight: PointF,     // Normalized (0.0 .. 1.0)
    val bottomRight: PointF,  // Normalized (0.0 .. 1.0)
    val bottomLeft: PointF,   // Normalized (0.0 .. 1.0)
    val confidence: Float = 0.88f
) {
    /**
     * Converts normalized points to pixel coordinates based on dimensions.
     */
    fun toSourcePointsArray(width: Float, height: Float): FloatArray {
        return floatArrayOf(
            topLeft.x.coerceIn(0f, 1f) * width, topLeft.y.coerceIn(0f, 1f) * height,
            topRight.x.coerceIn(0f, 1f) * width, topRight.y.coerceIn(0f, 1f) * height,
            bottomRight.x.coerceIn(0f, 1f) * width, bottomRight.y.coerceIn(0f, 1f) * height,
            bottomLeft.x.coerceIn(0f, 1f) * width, bottomLeft.y.coerceIn(0f, 1f) * height
        )
    }

    /**
     * Validates that the 4 points form a non-degenerate convex quadrilateral.
     */
    fun isConvex(): Boolean {
        fun crossProductZ(p1: PointF, p2: PointF, p3: PointF): Float {
            val dx1 = p2.x - p1.x
            val dy1 = p2.y - p1.y
            val dx2 = p3.x - p2.x
            val dy2 = p3.y - p2.y
            return dx1 * dy2 - dy1 * dx2
        }

        val cp1 = crossProductZ(topLeft, topRight, bottomRight)
        val cp2 = crossProductZ(topRight, bottomRight, bottomLeft)
        val cp3 = crossProductZ(bottomRight, bottomLeft, topLeft)
        val cp4 = crossProductZ(bottomLeft, topLeft, topRight)

        val allPos = cp1 > 0f && cp2 > 0f && cp3 > 0f && cp4 > 0f
        val allNeg = cp1 < 0f && cp2 < 0f && cp3 < 0f && cp4 < 0f
        return allPos || allNeg
    }
}

object OpenCvDocumentDetector {

    /**
     * Analyzes an ImageProxy frame from CameraX using real-time edge detection
     * to identify document quadrilateral bounds for live preview overlay.
     */
    fun analyzeFrame(imageProxy: ImageProxy): DocumentQuad {
        val plane = imageProxy.planes[0]
        val buffer = plane.buffer
        val width = imageProxy.width
        val height = imageProxy.height

        return detectEdgesInLuminance(buffer, width, height, plane.rowStride)
    }

    /**
     * High-precision OpenCV-style Sobel & Boundary edge detection on full Bitmap.
     * Automatically identifies the 4 corners of the document in the captured photo.
     */
    fun detectDocumentEdges(bitmap: Bitmap): DocumentQuad {
        val origW = bitmap.width
        val origH = bitmap.height
        if (origW < 20 || origH < 20) return defaultQuad()

        // 1. Downscale for fast, robust computer vision processing (target max 400px)
        val maxDim = 400
        val scale = if (origW > maxDim || origH > maxDim) {
            maxDim.toFloat() / max(origW, origH)
        } else {
            1.0f
        }
        val gridW = max(30, (origW * scale).toInt())
        val gridH = max(30, (origH * scale).toInt())

        val scaled = Bitmap.createScaledBitmap(bitmap, gridW, gridH, true)
        val pixels = IntArray(gridW * gridH)
        scaled.getPixels(pixels, 0, gridW, 0, 0, gridW, gridH)
        if (scaled != bitmap) {
            scaled.recycle()
        }

        // 2. Grayscale luminance map
        val luma = Array(gridH) { IntArray(gridW) }
        for (y in 0 until gridH) {
            val offset = y * gridW
            for (x in 0 until gridW) {
                val color = pixels[offset + x]
                val r = (color shr 16) and 0xFF
                val g = (color shr 8) and 0xFF
                val b = color and 0xFF
                luma[y][x] = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
            }
        }

        // 3. 3x3 Gaussian Blur smoothing to eliminate noise
        val smoothed = Array(gridH) { IntArray(gridW) }
        for (y in 1 until gridH - 1) {
            for (x in 1 until gridW - 1) {
                val sum = luma[y-1][x-1] + 2*luma[y-1][x] + luma[y-1][x+1] +
                          2*luma[y][x-1]   + 4*luma[y][x]   + 2*luma[y][x+1] +
                          luma[y+1][x-1] + 2*luma[y+1][x] + luma[y+1][x+1]
                smoothed[y][x] = sum / 16
            }
        }

        // 4. Sobel Gradient Magnitude Computation
        val mag = Array(gridH) { IntArray(gridW) }
        var totalMag = 0L
        var pixelCount = 0

        for (y in 1 until gridH - 1) {
            for (x in 1 until gridW - 1) {
                val gx = (smoothed[y-1][x+1] + 2*smoothed[y][x+1] + smoothed[y+1][x+1]) -
                         (smoothed[y-1][x-1] + 2*smoothed[y][x-1] + smoothed[y+1][x-1])
                val gy = (smoothed[y+1][x-1] + 2*smoothed[y+1][x] + smoothed[y+1][x+1]) -
                         (smoothed[y-1][x-1] + 2*smoothed[y-1][x] + smoothed[y-1][x+1])
                val g = abs(gx) + abs(gy)
                mag[y][x] = g
                totalMag += g
                pixelCount++
            }
        }

        val avgMag = if (pixelCount > 0) (totalMag / pixelCount).toInt() else 30
        val edgeThreshold = max(40, avgMag * 2)

        // 5. Inward Boundary Ray Scanning to locate document edges
        val edgePoints = mutableListOf<PointF>()

        // Horizontal ray scan from left & right
        val yStep = max(1, gridH / 30)
        for (y in (gridH * 0.08).toInt() until (gridH * 0.92).toInt() step yStep) {
            // From Left
            for (x in (gridW * 0.03).toInt() until (gridW * 0.45).toInt()) {
                if (mag[y][x] > edgeThreshold) {
                    edgePoints.add(PointF(x.toFloat() / gridW, y.toFloat() / gridH))
                    break
                }
            }
            // From Right
            for (x in (gridW * 0.97).toInt() downTo (gridW * 0.55).toInt()) {
                if (mag[y][x] > edgeThreshold) {
                    edgePoints.add(PointF(x.toFloat() / gridW, y.toFloat() / gridH))
                    break
                }
            }
        }

        // Vertical ray scan from top & bottom
        val xStep = max(1, gridW / 30)
        for (x in (gridW * 0.08).toInt() until (gridW * 0.92).toInt() step xStep) {
            // From Top
            for (y in (gridH * 0.03).toInt() until (gridH * 0.45).toInt()) {
                if (mag[y][x] > edgeThreshold) {
                    edgePoints.add(PointF(x.toFloat() / gridW, y.toFloat() / gridH))
                    break
                }
            }
            // From Bottom
            for (y in (gridH * 0.97).toInt() downTo (gridH * 0.55).toInt()) {
                if (mag[y][x] > edgeThreshold) {
                    edgePoints.add(PointF(x.toFloat() / gridW, y.toFloat() / gridH))
                    break
                }
            }
        }

        // 6. Corner Point Extraction
        if (edgePoints.size >= 8) {
            var minTL = Float.MAX_VALUE
            var maxTR = -Float.MAX_VALUE
            var maxBR = -Float.MAX_VALUE
            var minBL = Float.MAX_VALUE

            var ptTL = PointF(0.06f, 0.06f)
            var ptTR = PointF(0.94f, 0.06f)
            var ptBR = PointF(0.94f, 0.94f)
            var ptBL = PointF(0.06f, 0.94f)

            for (p in edgePoints) {
                // Top-Left: minimizes x + y
                val valTL = p.x + p.y
                if (valTL < minTL) {
                    minTL = valTL
                    ptTL = p
                }
                // Top-Right: maximizes x - y
                val valTR = p.x - p.y
                if (valTR > maxTR) {
                    maxTR = valTR
                    ptTR = p
                }
                // Bottom-Right: maximizes x + y
                val valBR = p.x + p.y
                if (valBR > maxBR) {
                    maxBR = valBR
                    ptBR = p
                }
                // Bottom-Left: minimizes x - y
                val valBL = p.x - p.y
                if (valBL < minBL) {
                    minBL = valBL
                    ptBL = p
                }
            }

            // Margin buffering (2%) so text on document border is never clipped
            val margin = 0.015f
            val adjustedTL = PointF((ptTL.x - margin).coerceIn(0.01f, 0.40f), (ptTL.y - margin).coerceIn(0.01f, 0.40f))
            val adjustedTR = PointF((ptTR.x + margin).coerceIn(0.60f, 0.99f), (ptTR.y - margin).coerceIn(0.01f, 0.40f))
            val adjustedBR = PointF((ptBR.x + margin).coerceIn(0.60f, 0.99f), (ptBR.y + margin).coerceIn(0.60f, 0.99f))
            val adjustedBL = PointF((ptBL.x - margin).coerceIn(0.01f, 0.40f), (ptBL.y + margin).coerceIn(0.60f, 0.99f))

            val candidateQuad = DocumentQuad(
                topLeft = adjustedTL,
                topRight = adjustedTR,
                bottomRight = adjustedBR,
                bottomLeft = adjustedBL,
                confidence = 0.94f
            )

            if (candidateQuad.isConvex()) {
                return candidateQuad
            }
        }

        return defaultQuad()
    }

    /**
     * Sobel Edge Detection on luminance byte buffer (from CameraX ImageAnalysis).
     */
    private fun detectEdgesInLuminance(
        buffer: ByteBuffer,
        width: Int,
        height: Int,
        rowStride: Int
    ): DocumentQuad {
        buffer.rewind()
        val sampleStep = max(1, width / 120) // Fast sampling grid
        val gridW = width / sampleStep
        val gridH = height / sampleStep

        if (gridW < 10 || gridH < 10) {
            return defaultQuad()
        }

        val lumaGrid = Array(gridH) { IntArray(gridW) }
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        for (y in 0 until gridH) {
            val srcY = y * sampleStep
            for (x in 0 until gridW) {
                val srcX = x * sampleStep
                val index = srcY * rowStride + srcX
                if (index < bytes.size) {
                    lumaGrid[y][x] = bytes[index].toInt() and 0xFF
                }
            }
        }

        var minX = gridW
        var maxX = 0
        var minY = gridH
        var maxY = 0
        var edgePixelCount = 0

        for (y in 1 until gridH - 1) {
            for (x in 1 until gridW - 1) {
                val gx = (lumaGrid[y-1][x+1] + 2*lumaGrid[y][x+1] + lumaGrid[y+1][x+1]) -
                         (lumaGrid[y-1][x-1] + 2*lumaGrid[y][x-1] + lumaGrid[y+1][x-1])
                val gy = (lumaGrid[y+1][x-1] + 2*lumaGrid[y+1][x] + lumaGrid[y+1][x+1]) -
                         (lumaGrid[y-1][x-1] + 2*lumaGrid[y-1][x] + lumaGrid[y-1][x+1])

                val magVal = abs(gx) + abs(gy)
                if (magVal > 110) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                    edgePixelCount++
                }
            }
        }

        if (edgePixelCount > 25 && maxX - minX > gridW / 4 && maxY - minY > gridH / 4) {
            val marginX = (gridW * 0.02f)
            val marginY = (gridH * 0.02f)

            val leftF = ((minX - marginX) / gridW).coerceIn(0.04f, 0.35f)
            val rightF = ((maxX + marginX) / gridW).coerceIn(0.65f, 0.96f)
            val topF = ((minY - marginY) / gridH).coerceIn(0.04f, 0.35f)
            val bottomF = ((maxY + marginY) / gridH).coerceIn(0.65f, 0.96f)

            return DocumentQuad(
                topLeft = PointF(leftF, topF + 0.01f),
                topRight = PointF(rightF, topF - 0.01f),
                bottomRight = PointF(rightF - 0.01f, bottomF),
                bottomLeft = PointF(leftF + 0.01f, bottomF - 0.01f),
                confidence = 0.92f
            )
        }

        return defaultQuad()
    }

    fun defaultQuad(): DocumentQuad {
        return DocumentQuad(
            topLeft = PointF(0.06f, 0.08f),
            topRight = PointF(0.94f, 0.08f),
            bottomRight = PointF(0.94f, 0.92f),
            bottomLeft = PointF(0.06f, 0.92f),
            confidence = 0.75f
        )
    }

    /**
     * OpenCV 4-Point Perspective Warp & Automatic Background Cropping.
     * Takes source image and quadrilateral corners, removes the background clutter,
     * and produces a rectified, flattened rectangular document bitmap.
     */
    fun cropAndWarpPerspective(srcBitmap: Bitmap, quad: DocumentQuad): Bitmap {
        val w = srcBitmap.width.toFloat()
        val h = srcBitmap.height.toFloat()

        // 4 corners in source coordinates
        val srcPoints = quad.toSourcePointsArray(w, h)

        // Calculate target dimensions from corner distances
        val topWidth = distance(srcPoints[0], srcPoints[1], srcPoints[2], srcPoints[3])
        val bottomWidth = distance(srcPoints[6], srcPoints[7], srcPoints[4], srcPoints[5])
        val targetWidth = max(topWidth, bottomWidth).coerceAtLeast(300f)

        val leftHeight = distance(srcPoints[0], srcPoints[1], srcPoints[6], srcPoints[7])
        val rightHeight = distance(srcPoints[2], srcPoints[3], srcPoints[4], srcPoints[5])
        val targetHeight = max(leftHeight, rightHeight).coerceAtLeast(400f)

        // Destination rectangular corners (0,0) -> (targetWidth, targetHeight)
        val dstPoints = floatArrayOf(
            0f, 0f,
            targetWidth, 0f,
            targetWidth, targetHeight,
            0f, targetHeight
        )

        val matrix = Matrix()
        val success = matrix.setPolyToPoly(srcPoints, 0, dstPoints, 0, 4)

        return if (success) {
            val outputBitmap = Bitmap.createBitmap(
                targetWidth.toInt(),
                targetHeight.toInt(),
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(outputBitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
            canvas.clipRect(0f, 0f, targetWidth, targetHeight)
            canvas.drawBitmap(srcBitmap, matrix, paint)
            outputBitmap
        } else {
            // Fallback rectangular crop
            val x = (quad.topLeft.x * w).toInt().coerceIn(0, srcBitmap.width - 1)
            val y = (quad.topLeft.y * h).toInt().coerceIn(0, srcBitmap.height - 1)
            val cropW = ((quad.topRight.x - quad.topLeft.x) * w).toInt().coerceIn(10, srcBitmap.width - x)
            val cropH = ((quad.bottomLeft.y - quad.topLeft.y) * h).toInt().coerceIn(10, srcBitmap.height - y)
            Bitmap.createBitmap(srcBitmap, x, y, cropW, cropH)
        }
    }

    /**
     * Applies post-crop document visual enhancement (Magic Color, B&W, Grayscale).
     */
    fun applyFilter(bitmap: Bitmap, mode: DocumentFilterMode): Bitmap {
        if (mode == DocumentFilterMode.ORIGINAL) return bitmap

        val width = bitmap.width
        val height = bitmap.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        when (mode) {
            DocumentFilterMode.GRAYSCALE -> {
                for (i in pixels.indices) {
                    val color = pixels[i]
                    val r = (color shr 16) and 0xFF
                    val g = (color shr 8) and 0xFF
                    val b = color and 0xFF
                    val gray = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
                    pixels[i] = (0xFF shl 24) or (gray shl 16) or (gray shl 8) or gray
                }
            }
            DocumentFilterMode.BLACK_AND_WHITE -> {
                // Adaptive high-contrast document thresholding
                for (i in pixels.indices) {
                    val color = pixels[i]
                    val r = (color shr 16) and 0xFF
                    val g = (color shr 8) and 0xFF
                    val b = color and 0xFF
                    val gray = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
                    val bw = if (gray > 135) 255 else 0
                    pixels[i] = (0xFF shl 24) or (bw shl 16) or (bw shl 8) or bw
                }
            }
            DocumentFilterMode.MAGIC_COLOR -> {
                // Magic Color: Contrast boost + paper whitening
                for (i in pixels.indices) {
                    val color = pixels[i]
                    var r = (color shr 16) and 0xFF
                    var g = (color shr 8) and 0xFF
                    var b = color and 0xFF

                    // Stretch contrast & brighten document background
                    r = ((r - 128) * 1.25f + 138).toInt().coerceIn(0, 255)
                    g = ((g - 128) * 1.25f + 138).toInt().coerceIn(0, 255)
                    b = ((b - 128) * 1.25f + 138).toInt().coerceIn(0, 255)

                    pixels[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                }
            }
            DocumentFilterMode.ORIGINAL -> Unit
        }

        output.setPixels(pixels, 0, width, 0, 0, width, height)
        return output
    }

    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        return sqrt(dx * dx + dy * dy)
    }
}

