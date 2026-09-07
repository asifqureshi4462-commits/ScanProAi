package com.example.data.ai

import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class GeminiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun getApiKey(): String {
        return try {
            val key = BuildConfig.GEMINI_API_KEY
            if (key.isNotBlank() && key != "MY_GEMINI_API_KEY") key else ""
        } catch (e: Exception) {
            ""
        }
    }

    /** True if a real Gemini API key is configured and live calls will be made. */
    fun isConfigured(): Boolean = getApiKey().isNotEmpty()

    /**
     * Runs real OCR on [bitmap] via Gemini Vision. If no API key is
     * configured, this returns a clearly labeled placeholder instead of
     * fabricated "extracted" text, so the UI never presents demo content
     * as if it were a real reading of the user's document.
     */
    suspend fun extractTextFromImage(bitmap: Bitmap, language: String = "English"): String {
        if (!isConfigured()) {
            return "⚠️ DEMO MODE — no Gemini API key configured.\n\n" +
                "This app cannot read your image without a real API key. Add your " +
                "Gemini key to .env (see .env.example) to enable real OCR text extraction."
        }
        val prompt = "Extract ALL visible text from this image exactly as it appears in $language, " +
            "preserving line breaks and structure as closely as possible. " +
            "Return ONLY the extracted text with no commentary, no markdown, and no extra formatting."
        return generateContent(prompt, bitmap)
    }

    suspend fun generateContent(prompt: String, bitmap: Bitmap? = null): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            return@withContext getOfflineResponse(prompt, bitmap)
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

        try {
            val partsArray = JSONArray()
            partsArray.put(JSONObject().put("text", prompt))

            if (bitmap != null) {
                val base64Image = bitmapToBase64(bitmap)
                val inlineData = JSONObject().apply {
                    put("mimeType", "image/jpeg")
                    put("data", base64Image)
                }
                partsArray.put(JSONObject().put("inlineData", inlineData))
            }

            val contentsArray = JSONArray().put(
                JSONObject().put("parts", partsArray)
            )

            val jsonBody = JSONObject().apply {
                put("contents", contentsArray)
            }

            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseString = response.body?.string() ?: ""

            if (response.isSuccessful && responseString.isNotBlank()) {
                val jsonResponse = JSONObject(responseString)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val contentObj = firstCandidate.optJSONObject("content")
                    val parts = contentObj?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).optString("text", "No text generated.")
                    }
                }
            }

            getOfflineResponse(prompt, bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            getOfflineResponse(prompt, bitmap)
        }
    }

    suspend fun summarizeDocument(documentText: String): String {
        val prompt = "You are ScanPro AI Document Assistant. Please provide a clean, highly structured, professional executive summary of the following document. Highlight key points, dates, actions, and key takeaways:\n\n$documentText"
        return generateContent(prompt)
    }

    suspend fun translateDocument(documentText: String, targetLanguage: String): String {
        val prompt = "Translate the following document text accurately into $targetLanguage while retaining professional tone, formatting, and formatting structure:\n\n$documentText"
        return generateContent(prompt)
    }

    suspend fun correctAndFormatText(documentText: String): String {
        val prompt = "Clean up, correct grammar, format cleanly, and fix any OCR typos in the following extracted document text:\n\n$documentText"
        return generateContent(prompt)
    }

    suspend fun askDocumentQuestion(documentContext: String, question: String): String {
        val prompt = "You are ScanPro AI, an intelligent document context assistant. Answer the user's question accurately using ONLY the context provided below.\n\nDOCUMENT CONTEXT:\n$documentContext\n\nUSER QUESTION: $question"
        return generateContent(prompt)
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    private fun getOfflineResponse(prompt: String, bitmap: Bitmap?): String {
        val demoBanner = "⚠️ DEMO MODE (no Gemini API key configured) — this is placeholder text, " +
            "not a real AI analysis of your content. Add a key to .env to enable real responses.\n\n"
        val lower = prompt.lowercase()
        return demoBanner + when {
            lower.contains("summarize") || lower.contains("summary") -> {
                "📄 **Sample Executive Summary (demo)**\n\n• **Document Type**: Sample placeholder\n• **Key Findings**: Configure a Gemini API key to get a real summary of your actual document.\n• **Action Items**: N/A in demo mode.\n• **Status**: Demo output only."
            }
            lower.contains("translate") -> {
                "🌐 **Sample Translation (demo)**\n\nThis is placeholder text. Configure a Gemini API key to get a real translation of your document."
            }
            lower.contains("correct") || lower.contains("grammar") -> {
                "✨ **Sample Correction (demo)**\n\nThis is placeholder text. Configure a Gemini API key to get real grammar correction."
            }
            bitmap != null -> {
                "🔍 **Sample OCR Output (demo)**\n\nThis is placeholder text, not real text read from your image. " +
                    "Configure a Gemini API key to enable real OCR extraction."
            }
            else -> {
                "🤖 **ScanPro AI Assistant (demo mode)**\n\nI can't reach Gemini right now because no API key is configured, " +
                    "so I can't actually answer: \"$prompt\". Add a Gemini API key to .env to enable real AI responses."
            }
        }
    }
}
