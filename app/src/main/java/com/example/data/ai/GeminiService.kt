package com.example.data.ai

import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Base64
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

sealed class AiResult {
    data class Success(val text: String) : AiResult()
    data class Error(val kind: ErrorKind, val message: String) : AiResult() {
        enum class ErrorKind {
            NoApiKey, Network, Quota, Blocked, Timeout, Unknown
        }
    }
}

class GeminiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
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

    fun isConfigured(): Boolean = getApiKey().isNotEmpty()

    private fun downscaleBitmap(bitmap: Bitmap, maxDimension: Int = 2048): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) return bitmap
        val ratio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int
        if (width > height) {
            newWidth = maxDimension
            newHeight = (maxDimension / ratio).toInt()
        } else {
            newHeight = maxDimension
            newWidth = (maxDimension * ratio).toInt()
        }
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    suspend fun extractTextFromImage(bitmap: Bitmap, language: String = "English"): AiResult = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            return@withContext AiResult.Error(
                AiResult.Error.ErrorKind.NoApiKey,
                "No Gemini API key configured. Please add your API key to .env."
            )
        }
        val prompt = "Extract ALL visible text from this image exactly as it appears in $language, " +
            "preserving line breaks and structure as closely as possible. " +
            "Return ONLY the extracted text with no commentary, no markdown, and no extra formatting."
        val downscaled = downscaleBitmap(bitmap)
        val result = generateContentInternal(prompt, downscaled, 0)
        if (downscaled != bitmap) {
            try { downscaled.recycle() } catch (ignored: Exception) {}
        }
        result
    }

    suspend fun generateContent(prompt: String, bitmap: Bitmap? = null): AiResult = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            return@withContext AiResult.Error(
                AiResult.Error.ErrorKind.NoApiKey,
                "No Gemini API key configured. Please add your API key to .env."
            )
        }
        val downscaled = bitmap?.let { downscaleBitmap(it) }
        val result = generateContentInternal(prompt, downscaled, 0)
        if (downscaled != null && downscaled != bitmap) {
            try { downscaled.recycle() } catch (ignored: Exception) {}
        }
        result
    }

    private suspend fun generateContentInternal(prompt: String, bitmap: Bitmap?, retryCount: Int): AiResult {
        val apiKey = getApiKey()
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"

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
                .addHeader("x-goog-api-key", apiKey)
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val responseString = executeCallWithCancellation(request)
            val jsonResponse = JSONObject(responseString)

            val candidates = jsonResponse.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val finishReason = firstCandidate.optString("finishReason", "")
                if (finishReason == "SAFETY" || finishReason == "RECITATION") {
                    return AiResult.Error(AiResult.Error.ErrorKind.Blocked, "Response blocked due to safety or policy settings.")
                }
                val contentObj = firstCandidate.optJSONObject("content")
                val parts = contentObj?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    val text = parts.getJSONObject(0).optString("text", "")
                    return AiResult.Success(text)
                }
            }

            return AiResult.Error(AiResult.Error.ErrorKind.Unknown, "Empty response from Gemini API.")
        } catch (e: Exception) {
            val msg = e.message ?: "Unknown error"
            val kind = when {
                msg.contains("429") || msg.contains("Quota") -> {
                    if (retryCount < 1) {
                        kotlinx.coroutines.delay(2000L)
                        return generateContentInternal(prompt, bitmap, retryCount + 1)
                    }
                    AiResult.Error.ErrorKind.Quota
                }
                msg.contains("5") && retryCount < 1 -> {
                    kotlinx.coroutines.delay(1500L)
                    return generateContentInternal(prompt, bitmap, retryCount + 1)
                }
                msg.contains("timeout", ignoreCase = true) -> AiResult.Error.ErrorKind.Timeout
                msg.contains("network", ignoreCase = true) || msg.contains("unable to resolve", ignoreCase = true) -> AiResult.Error.ErrorKind.Network
                else -> AiResult.Error.ErrorKind.Unknown
            }
            return AiResult.Error(kind, msg)
        }
    }

    private suspend fun executeCallWithCancellation(request: Request): String = suspendCancellableCoroutine { continuation ->
        val call = client.newCall(request)
        continuation.invokeOnCancellation {
            call.cancel()
        }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) {
                    continuation.resumeWith(Result.failure(e))
                }
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val bodyString = response.body?.string() ?: ""
                    if (response.isSuccessful) {
                        continuation.resume(bodyString)
                    } else {
                        if (response.code == 429) {
                            continuation.resumeWith(Result.failure(IOException("Rate limit exceeded (429).")))
                        } else if (response.code >= 500) {
                            continuation.resumeWith(Result.failure(IOException("Server error (${response.code}): $bodyString")))
                        } else {
                            continuation.resumeWith(Result.failure(IOException("API error (${response.code}): $bodyString")))
                        }
                    }
                } catch (e: Exception) {
                    continuation.resumeWith(Result.failure(e))
                }
            }
        })
    }

    suspend fun summarizeDocument(documentText: String): AiResult {
        val prompt = "You are ScanPro AI Document Assistant. Please provide a clean, highly structured, professional executive summary of the following document. Highlight key points, dates, actions, and key takeaways:\n\n$documentText"
        return generateContent(prompt)
    }

    suspend fun translateDocument(documentText: String, targetLanguage: String): AiResult {
        val prompt = "Translate the following document text accurately into $targetLanguage while retaining professional tone, formatting, and formatting structure:\n\n$documentText"
        return generateContent(prompt)
    }

    suspend fun correctAndFormatText(documentText: String): AiResult {
        val prompt = "Clean up, correct grammar, format cleanly, and fix any OCR typos in the following extracted document text:\n\n$documentText"
        return generateContent(prompt)
    }

    suspend fun askDocumentQuestion(documentContext: String, question: String): AiResult {
        val prompt = "You are ScanPro AI, an intelligent document context assistant. Answer the user's question accurately using ONLY the context provided below.\n\nDOCUMENT CONTEXT:\n$documentContext\n\nUSER QUESTION: $question"
        return generateContent(prompt)
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }
}
