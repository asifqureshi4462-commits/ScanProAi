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

    fun isConfigured(): Boolean = getApiKey().isNotEmpty()

    suspend fun extractTextFromImage(
        bitmap: Bitmap,
        language: String = "English"
    ): String {
        if (!isConfigured()) {
            return "⚠️ DEMO MODE — no Gemini API key configured.\n\n" +
                "Add your Gemini API key to .env (see .env.example) to enable real OCR text extraction."
        }

        val prompt =
            "Extract ALL visible text from this image exactly as it appears in $language, " +
                "preserving line breaks and structure as closely as possible. " +
                "Return ONLY the extracted text with no commentary, no markdown, and no extra formatting."

        return generateContent(prompt, bitmap)
    }

    suspend fun extractTableDataFromImage(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext "Item,Description,Quantity,Price,Total\n1,General Item 1,1,150.00,150.00\n2,General Item 2,2,75.00,150.00\nTotal,,,,300.00"
        }

        val prompt = """
            You are an expert document and spreadsheet parser.
            Analyze this document image and extract ALL tables, ledger entries, tabular lists, or structured invoice line items.
            Format the output strictly as valid CSV (Comma-Separated Values).
            - Every row on a new line.
            - Do not include markdown codeblocks (no ```csv or ```).
            - Do not include any explanations, greetings, or notes.
            - Ensure all columns line up logically.
            - Wrap fields containing commas inside double quotes.
        """.trimIndent()

        val raw = generateContent(prompt, bitmap)
        raw.replace("```csv", "")
            .replace("```", "")
            .trim()
    }

    suspend fun generateContent(
        prompt: String,
        bitmap: Bitmap? = null
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()

        if (apiKey.isEmpty()) {
            return@withContext getOfflineResponse(prompt, bitmap)
        }

        val url =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

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
                .post(
                    jsonBody.toString()
                        .toRequestBody("application/json".toMediaType())
                )
                .build()

            val response = client.newCall(request).execute()
            val responseString = response.body?.string() ?: ""

            if (response.isSuccessful && responseString.isNotBlank()) {
                val jsonResponse = JSONObject(responseString)
                val candidates = jsonResponse.optJSONArray("candidates")

                if (candidates != null && candidates.length() > 0) {
                    for (i in 0 until candidates.length()) {
                        val candidate = candidates.optJSONObject(i)
                        val content = candidate?.optJSONObject("content")
                        val parts = content?.optJSONArray("parts")

                        if (parts != null) {
                            val sb = StringBuilder()

                            for (j in 0 until parts.length()) {
                                val part = parts.optJSONObject(j)
                                val text = part?.optString("text", "")

                                if (text?.isNotBlank() == true) {
                                    sb.append(text).append("\n\n")
                                }
                            }

                            val result = sb.toString().trim()
                            if (result.isNotEmpty()) {
                                return@withContext result
                            }
                        }
                    }
                }
            }

            return@withContext getOfflineResponse(prompt, bitmap)
        } catch (e: Exception) {
            return@withContext getOfflineResponse(prompt, bitmap)
        }
    }

    suspend fun summarizeDocument(text: String): String =
        withContext(Dispatchers.IO) {
            val apiKey = getApiKey()

            if (apiKey.isEmpty()) {
                return@withContext "Offline summary: ${text.take(240)}"
            }

            val prompt = """
                Summarize the following document clearly and concisely.
                Keep the result readable, useful, and professional.
                Do not add any markdown.

                Document:
                $text
            """.trimIndent()

            try {
                val raw = generateText(prompt)
                if (raw.isNotBlank()) raw else "Summary unavailable."
            } catch (e: Exception) {
                "Summary unavailable."
            }
        }

    suspend fun askDocumentQuestion(
        context: String,
        question: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()

        if (apiKey.isEmpty()) {
            return@withContext "Offline AI Response for: $question"
        }

        val prompt = """
            Use the following document context to answer the question.
            If the answer is not available in the context, say so honestly.

            Context:
            $context

            Question:
            $question
        """.trimIndent()

        try {
            val raw = generateText(prompt)
            if (raw.isNotBlank()) raw
            else "No answer found in the provided context."
        } catch (e: Exception) {
            "No answer found in the provided context."
        }
    }

    suspend fun translateDocument(
        text: String,
        targetLang: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()

        if (apiKey.isEmpty()) {
            return@withContext "Offline translation to $targetLang: ${text.take(200)}"
        }

        val prompt = """
            Translate the following text to $targetLang.
            Preserve the meaning and keep the result natural.

            Text:
            $text
        """.trimIndent()

        try {
            val raw = generateText(prompt)
            if (raw.isNotBlank()) raw else "Translation unavailable."
        } catch (e: Exception) {
            "Translation unavailable."
        }
    }

    private suspend fun generateText(prompt: String): String =
        withContext(Dispatchers.IO) {
            val apiKey = getApiKey()

            if (apiKey.isEmpty()) {
                return@withContext "Offline mode: no API key configured."
            }

            val url =
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

            try {
                val jsonBody = JSONObject().apply {
                    put(
                        "contents",
                        JSONArray().put(
                            JSONObject().put(
                                "parts",
                                JSONArray().put(
                                    JSONObject().put("text", prompt)
                                )
                            )
                        )
                    )
                }

                val request = Request.Builder()
                    .url(url)
                    .post(
                        jsonBody.toString()
                            .toRequestBody("application/json".toMediaType())
                    )
                    .build()

                val response = client.newCall(request).execute()
                val responseString = response.body?.string() ?: ""

                if (response.isSuccessful && responseString.isNotBlank()) {
                    val jsonResponse = JSONObject(responseString)
                    val candidates = jsonResponse.optJSONArray("candidates")

                    if (candidates != null) {
                        for (i in 0 until candidates.length()) {
                            val candidate = candidates.optJSONObject(i)
                            val content = candidate?.optJSONObject("content")
                            val parts = content?.optJSONArray("parts")

                            if (parts != null) {
                                val result = buildString {
                                    for (j in 0 until parts.length()) {
                                        val part = parts.optJSONObject(j)
                                        val text = part?.optString("text", "")

                                        if (text?.isNotBlank() == true) {
                                            append(text)
                                        }
                                    }
                                }.trim()

                                if (result.isNotEmpty()) {
                                    return@withContext result
                                }
                            }
                        }
                    }
                }

                "No answer generated."
            } catch (e: Exception) {
                "No answer generated."
            }
        }

    private fun getOfflineResponse(
        prompt: String,
        bitmap: Bitmap? = null
    ): String {
        val hasImage = bitmap != null

        return if (hasImage) {
            "Offline OCR: image analysis is unavailable because no Gemini API key is configured."
        } else {
            "Offline AI: no Gemini API key is configured. Please add a valid Gemini API key."
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        val bytes = outputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}
