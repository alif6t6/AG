package com.example.api

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.util.concurrent.TimeUnit

object GeminiApi {
    private const val TAG = "GeminiApi"
    private const val BASE_URL = "https://generativelanguage.googleapis.com"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val mediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Helper to retrieve active API Key. Prioritizes user input key from settings,
     * falls back to BuildConfig.GEMINI_API_KEY.
     */
    fun getApiKey(userKey: String): String {
        return userKey.ifBlank {
            val buildConfigKey = BuildConfig.GEMINI_API_KEY
            if (buildConfigKey == "MY_GEMINI_API_KEY") "" else buildConfigKey
        }
    }

    /**
     * Executes non-streaming text generation.
     */
    suspend fun generateText(
        prompt: String,
        systemInstruction: String,
        apiKey: String,
        model: String = "gemini-3.5-flash",
        maxTokens: Int = 2048
    ): String = kotlinx.coroutines.withContext(Dispatchers.IO) {
        val activeKey = getApiKey(apiKey)
        if (activeKey.isBlank()) {
            return@withContext "API KEY MISSING: Please configure your Gemini API Key in Settings or the Secrets panel."
        }

        val url = "$BASE_URL/v1beta/models/$model:generateContent?key=$activeKey"
        
        val requestBodyJson = makePayload(prompt, systemInstruction, maxTokens)
        val body = requestBodyJson.toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val responseStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    Log.e(TAG, "Request failed: code=${response.code} body=$responseStr")
                    return@withContext "Error: Gemini API returned code ${response.code}\n${parseErrorDetails(responseStr)}"
                }

                val json = JSONObject(responseStr)
                val candidates = json.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val contentObj = firstCandidate.optJSONObject("content")
                    val parts = contentObj?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).optString("text")
                    }
                }
                return@withContext "Error: No text found in candidates response."
            }
        } catch (e: Exception) {
            Log.e(TAG, "generateText exception", e)
            return@withContext "Exception checking connection: ${e.message}"
        }
    }

    /**
     * Flow-based content streaming. Reads response stream from Direct REST chunk-by-chunk.
     */
    fun streamGenerateText(
        prompt: String,
        systemInstruction: String,
        apiKey: String,
        model: String = "gemini-3.5-flash",
        maxTokens: Int = 2048
    ): Flow<String> = flow {
        val activeKey = getApiKey(apiKey)
        if (activeKey.isBlank()) {
            emit("API KEY MISSING: Please configure your Gemini API Key first.")
            return@flow
        }

        val url = "$BASE_URL/v1beta/models/$model:streamGenerateContent?key=$activeKey"
        val requestBodyJson = makePayload(prompt, systemInstruction, maxTokens)
        val body = requestBodyJson.toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        var callSuccess = false
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val err = response.body?.string() ?: ""
                    emit("Error: Gemini API streaming failed with code ${response.code}\n${parseErrorDetails(err)}")
                    return@flow
                }

                val reader = response.body?.byteStream()?.bufferedReader()
                if (reader == null) {
                    emit("Error: Unable to open response byte stream.")
                    return@flow
                }

                callSuccess = true
                var line: String?
                // The stream is returned as a JSON array of objects, or SSE format. Let's parse line by line.
                // Standard streamGenerateContent returns a json stream. Let's process it robustly.
                val jsonBuffer = StringBuilder()
                while (reader.readLine().also { line = it } != null) {
                    val trimmed = line!!.trim()
                    if (trimmed.isEmpty()) continue
                    
                    // The Gemini REST stream comes as consecutive JSON array items like:
                    // [
                    //   { "candidates": [...] },
                    //   { "candidates": [...] }
                    // ]
                    // Or as separate raw JSON objects. To parse robustly:
                    var cleanLine = trimmed
                    if (cleanLine.startsWith("[")) cleanLine = cleanLine.substring(1)
                    if (cleanLine.endsWith(",")) cleanLine = cleanLine.substring(0, cleanLine.length - 1)
                    if (cleanLine.endsWith("]")) cleanLine = cleanLine.substring(0, cleanLine.length - 1)
                    cleanLine = cleanLine.trim()

                    if (cleanLine.isEmpty()) continue

                    try {
                        val json = JSONObject(cleanLine)
                        val textPart = parseTextFromCandidate(json)
                        if (textPart.isNotEmpty()) {
                            emit(textPart)
                        }
                    } catch (e: Exception) {
                        // Sometimes the JSON spans multiple lines. Attempt cumulative buffer parsing fallback.
                        jsonBuffer.append(trimmed)
                        var str = jsonBuffer.toString()
                        if (str.startsWith("[")) str = str.substring(1)
                        if (str.endsWith("]")) str = str.substring(0, str.length - 1)
                        if (str.endsWith(",")) str = str.substring(0, str.length - 1)
                        try {
                            val json = JSONObject(str)
                            val textPart = parseTextFromCandidate(json)
                            if (textPart.isNotEmpty()) {
                                emit(textPart)
                                jsonBuffer.clear()
                            }
                        } catch (ex: Exception) {
                            // Wait for more lines
                        }
                    }
                }
            }
        } catch (e: Exception) {
            if (!callSuccess) {
                emit("Exception: ${e.message}")
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun parseTextFromCandidate(json: JSONObject): String {
        val candidates = json.optJSONArray("candidates")
        if (candidates != null && candidates.length() > 0) {
            val firstCandidate = candidates.getJSONObject(0)
            val contentObj = firstCandidate.optJSONObject("content")
            val parts = contentObj?.optJSONArray("parts")
            if (parts != null && parts.length() > 0) {
                return parts.getJSONObject(0).optString("text", "")
            }
        }
        return ""
    }

    private fun parseErrorDetails(responseBody: String): String {
        return try {
            val json = JSONObject(responseBody)
            val error = json.optJSONObject("error")
            val message = error?.optString("message") ?: ""
            if (message.isNotEmpty()) "Details: $message" else responseBody
        } catch (e: Exception) {
            responseBody
        }
    }

    private fun makePayload(prompt: String, systemInstruction: String, maxTokens: Int): String {
        val root = JSONObject()
        
        // System instruction
        if (systemInstruction.isNotEmpty()) {
            val sysInstructionObj = JSONObject()
            val sysPartsArr = JSONArray()
            val sysPartObj = JSONObject()
            sysPartObj.put("text", systemInstruction)
            sysPartsArr.put(sysPartObj)
            sysInstructionObj.put("parts", sysPartsArr)
            root.put("systemInstruction", sysInstructionObj)
        }

        // Contents prompt
        val contentsArr = JSONArray()
        val contentObj = JSONObject()
        val partsArr = JSONArray()
        val partObj = JSONObject()
        partObj.put("text", prompt)
        partsArr.put(partObj)
        contentObj.put("parts", partsArr)
        contentsArr.put(contentObj)
        root.put("contents", contentsArr)

        // Config
        val configObj = JSONObject()
        configObj.put("temperature", 0.7)
        configObj.put("maxOutputTokens", maxTokens)
        root.put("generationConfig", configObj)

        return root.toString()
    }
}
