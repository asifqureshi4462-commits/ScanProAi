package com.example

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import com.example.util.DocumentFilterMode
import com.example.util.DocumentQuad
import com.example.util.OpenCvDocumentDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OpenCvDocumentDetectorTest {

    @Test
    fun testDocumentQuadConvexity() {
        val validConvexQuad = DocumentQuad(
            topLeft = PointF(0.1f, 0.1f),
            topRight = PointF(0.9f, 0.12f),
            bottomRight = PointF(0.88f, 0.88f),
            bottomLeft = PointF(0.12f, 0.85f),
            confidence = 0.95f
        )
        assertTrue(validConvexQuad.isConvex())

        val pts = validConvexQuad.toSourcePointsArray(1000f, 2000f)
        assertEquals(8, pts.size)
        assertEquals(100f, pts[0], 0.1f)
        assertEquals(200f, pts[1], 0.1f)
    }

    @Test
    fun testDetectDocumentEdgesOnSampleDocument() {
        // Create synthetic image with dark desk background and centered white document sheet
        val bitmap = Bitmap.createBitmap(400, 600, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val deskPaint = Paint().apply { color = Color.rgb(30, 40, 50) }
        canvas.drawRect(0f, 0f, 400f, 600f, deskPaint)

        val paperPaint = Paint().apply { color = Color.WHITE }
        canvas.drawRect(50f, 80f, 350f, 520f, paperPaint)

        val textPaint = Paint().apply { color = Color.BLACK }
        canvas.drawRect(80f, 120f, 320f, 140f, textPaint)
        canvas.drawRect(80f, 160f, 280f, 180f, textPaint)

        val quad = OpenCvDocumentDetector.detectDocumentEdges(bitmap)
        assertNotNull(quad)
        assertTrue(quad.confidence > 0.70f)
        assertTrue(quad.isConvex())

        // Top-left should be near 0.12, 0.13
        assertTrue(quad.topLeft.x in 0.01f..0.35f)
        assertTrue(quad.topLeft.y in 0.01f..0.35f)
        // Bottom-right should be near 0.87, 0.86
        assertTrue(quad.bottomRight.x in 0.65f..0.99f)
        assertTrue(quad.bottomRight.y in 0.65f..0.99f)
    }

    @Test
    fun testCropAndWarpPerspective() {
        val bitmap = Bitmap.createBitmap(800, 1000, Bitmap.Config.ARGB_8888)
        val quad = DocumentQuad(
            topLeft = PointF(0.1f, 0.1f),
            topRight = PointF(0.9f, 0.1f),
            bottomRight = PointF(0.9f, 0.9f),
            bottomLeft = PointF(0.1f, 0.9f),
            confidence = 0.92f
        )

        val cropped = OpenCvDocumentDetector.cropAndWarpPerspective(bitmap, quad)
        assertNotNull(cropped)
        assertTrue(cropped.width > 200)
        assertTrue(cropped.height > 200)
    }

    @Test
    fun testDocumentEnhancementFilters() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.GRAY)

        val magic = OpenCvDocumentDetector.applyFilter(bitmap, DocumentFilterMode.MAGIC_COLOR)
        assertNotNull(magic)
        assertEquals(100, magic.width)
        assertEquals(100, magic.height)

        val bw = OpenCvDocumentDetector.applyFilter(bitmap, DocumentFilterMode.BLACK_AND_WHITE)
        assertNotNull(bw)

        val gray = OpenCvDocumentDetector.applyFilter(bitmap, DocumentFilterMode.GRAYSCALE)
        assertNotNull(gray)
    }
}
