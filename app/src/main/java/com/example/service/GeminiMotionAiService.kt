package com.example.service

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiMotionAiService(private val apiKeyProvider: () -> String, private val modelProvider: () -> String) {

    companion object {
        private const val TAG = "GeminiMotionAiService"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()

    /**
     * Executes AI prompt with automatic model fallback to avoid 429/503 "high demand" errors.
     */
    suspend fun executeAiMotionTask(
        taskType: String, // "GENERATE", "EDIT", "FIX"
        currentHtml: String,
        userPrompt: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val userKey = apiKeyProvider()
        val apiKey = if (userKey.isNotBlank()) userKey else BuildConfig.GEMINI_API_KEY

        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Please enter your Gemini API key in Settings (supports keys starting with AQ... or AIza...).")
            )
        }

        val primaryModel = modelProvider().ifBlank { "gemini-2.5-flash" }

        // Models to try in sequence to eliminate "high demand" / 503 errors
        val modelsToTry = mutableListOf<String>()
        modelsToTry.add(primaryModel)
        if (!modelsToTry.contains("gemini-2.5-flash")) modelsToTry.add("gemini-2.5-flash")
        if (!modelsToTry.contains("gemini-2.5-pro")) modelsToTry.add("gemini-2.5-pro")

        val systemPrompt = """
            You are a master Creative Motion Designer and Frontend Animation Engineer.
            You produce self-contained, professional HTML5/CSS3/SVG motion graphics designed for rendering into MP4 videos.

            CORE TECHNICAL REQUIREMENTS:
            1. Return ONLY the raw HTML/CSS/JS code containing inline <style> and elements.
            2. NO markdown explanations, NO conversational intro/outro.
            3. The viewport artboard is standardized to 360x640 (9:16 portrait) or 640x360 (16:9 landscape) at 3x density (1080p).
               Use responsive CSS (percentages, flexbox, grid, vw/vh) or these base artboard dimensions.
            4. Animations MUST be fluid and continuous:
               - Use CSS @keyframes with smooth easing (cubic-bezier or linear).
               - Set infinite loops or animations timed to run cleanly between 5 and 15 seconds.
               - You may also use window.onRenderFrame = function(timeMs, progress) { ... } for deterministic Canvas / SVG rendering loops.
            5. Do NOT include UI playback controls, external website header/footers, or click buttons. The artboard is the video itself.
            6. Do NOT include external CDN script tags (e.g., no external unpkg or cdnjs links) that require network at render time. Use pure CSS, SVG, or pure vanilla Canvas/JS.
        """.trimIndent()

        val fullPrompt = when (taskType) {
            "GENERATE" -> """
                Create a stunning, modern motion graphic based on this description:
                $userPrompt

                Ensure it has smooth CSS keyframe animations, vibrant modern colors, and fits a 9:16 mobile canvas.
            """.trimIndent()

            "FIX" -> """
                Analyze and fix this motion code to make it animate smoothly without errors:
                $currentHtml

                Ensure:
                1. All elements that should move have proper @keyframes and animation properties.
                2. Remove any static/broken CSS.
                3. Remove extraneous bottom buttons or control bars.
                4. Make the motion fluid.
            """.trimIndent()

            else -> """
                INSTRUCTION:
                $userPrompt

                CURRENT HTML/CSS CODE:
                $currentHtml

                Update the code according to the instruction while keeping the animations smooth.
            """.trimIndent()
        }

        var lastError = "Failed to connect to Gemini API"

        for (model in modelsToTry) {
            try {
                val rootJson = JSONObject().apply {
                    val contentsArray = JSONArray().apply {
                        val contentObj = JSONObject().apply {
                            put("role", "user")
                            val partsArray = JSONArray().apply {
                                put(JSONObject().put("text", fullPrompt))
                            }
                            put("parts", partsArray)
                        }
                        put(contentObj)
                    }
                    put("contents", contentsArray)

                    val systemInstructionObj = JSONObject().apply {
                        val partsArray = JSONArray().apply {
                            put(JSONObject().put("text", systemPrompt))
                        }
                        put("parts", partsArray)
                    }
                    put("systemInstruction", systemInstructionObj)
                }

                val requestBody = rootJson.toString().toRequestBody("application/json".toMediaType())
                val url = "$BASE_URL$model:generateContent"

                val request = Request.Builder()
                    .url(url)
                    .addHeader("x-goog-api-key", apiKey)
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                val responseString = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    val jsonResponse = JSONObject(responseString)
                    val candidates = jsonResponse.optJSONArray("candidates")
                    val firstCandidate = candidates?.optJSONObject(0)
                    val content = firstCandidate?.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")

                    val fullText = buildString {
                        if (parts != null) {
                            for (p in 0 until parts.length()) {
                                val part = parts.optJSONObject(p) ?: continue
                                if (part.optBoolean("thought", false)) continue
                                val txt = part.optString("text")
                                if (txt.isNotBlank()) append(txt)
                            }
                        }
                    }

                    if (fullText.isNotBlank()) {
                        var cleanedCode = fullText.trim()
                        if (cleanedCode.startsWith("```html", ignoreCase = true)) {
                            cleanedCode = cleanedCode.substringAfter("```html").substringBeforeLast("```").trim()
                        } else if (cleanedCode.startsWith("```")) {
                            cleanedCode = cleanedCode.substringAfter("```").substringBeforeLast("```").trim()
                        }
                        return@withContext Result.success(cleanedCode)
                    }
                } else {
                    val errCode = response.code
                    if (errCode == 429 || errCode == 503) {
                        Log.w(TAG, "Model $model returned $errCode, trying fallback...")
                        lastError = "Model $model busy. Trying fast fallback..."
                        continue
                    } else {
                        val errJson = try { JSONObject(responseString) } catch (e: Exception) { null }
                        val errMsg = errJson?.optJSONObject("error")?.optString("message") ?: "HTTP $errCode"
                        return@withContext Result.failure(RuntimeException("Gemini Error: $errMsg"))
                    }
                }

            } catch (e: Exception) {
                Log.w(TAG, "Call to $model failed: ${e.message}")
                lastError = e.message ?: "Network error"
            }
        }

        Result.failure(RuntimeException(lastError))
    }

    suspend fun editMotionCode(currentHtml: String, userPrompt: String): Result<String> {
        return executeAiMotionTask("EDIT", currentHtml, userPrompt)
    }
}
