package com.example.data.image

import android.content.Context
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class ImageGenerationService(
    private val context: Context,
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
) {

    /**
     * Mengekstraksi atau menyusun prompt adegan visual dari teks cerita:
     * 1. Cek tag eksplisit [SCENE_ART: ...] atau [IMAGE_PROMPT: ...]
     * 2. Jika tidak ada, ambil aksi di antara tanda #...# atau cuplikan narasi
     */
    fun extractScenePrompt(rawText: String): String {
        // 1. Cek tag eksplisit
        val sceneArtRegex = Regex("""\[(?:SCENE_ART|IMAGE_PROMPT|IMAGE):\s*(.*?)]""", RegexOption.IGNORE_CASE)
        val match = sceneArtRegex.find(rawText)
        if (match != null) {
            val extracted = match.groupValues[1].trim()
            if (extracted.isNotBlank()) {
                return "$extracted, highly detailed anime visual novel illustration, 4k digital art"
            }
        }

        // 2. Cek deskripsi di antara tanda pagar #...#
        val hashMatches = Regex("""#(.*?)#""").findAll(rawText).map { it.groupValues[1].trim() }.filter { it.isNotBlank() }.toList()
        val actionsText = if (hashMatches.isNotEmpty()) {
            hashMatches.joinToString(", ")
        } else {
            // Ambil 2 kalimat narasi awal
            rawText.replace(Regex("""".*?""""), "") // bersihkan kutipan dialog
                .lines()
                .filter { it.isNotBlank() }
                .take(2)
                .joinToString(" ")
        }

        val basePrompt = if (actionsText.isNotBlank()) actionsText.take(200) else "intimate atmospheric bedroom scene, anime visual novel digital art"
        return "Anime visual novel illustration, dramatic lighting, detailed background: $basePrompt"
    }

    /**
     * Membersihkan teks yang akan ditampilkan di chat bubble agar tag [SCENE_ART: ...] tidak mengganggu pembaca.
     */
    fun cleanDisplayText(rawText: String): String {
        return rawText.replace(Regex("""\[(?:SCENE_ART|IMAGE_PROMPT|IMAGE):\s*.*?]""", RegexOption.IGNORE_CASE), "").trim()
    }

    /**
     * Membangkitkan gambar adegan berdasarkan provider yang dipilih dan menyimpannya secara lokal.
     * Mengembalikan URL lokal (path file) atau URL web.
     */
    suspend fun generateImage(
        messageId: Long,
        rawText: String,
        provider: ImageProvider,
        geminiApiKey: String?,
        hfToken: String?
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val prompt = extractScenePrompt(rawText)
            val imagesDir = File(context.filesDir, "story_art").apply { mkdirs() }
            val outputFile = File(imagesDir, "art_${messageId}_${System.currentTimeMillis()}.jpg")

            when (provider) {
                ImageProvider.POLLINATIONS -> {
                    val seed = (System.currentTimeMillis() % 100000).toInt()
                    val encoded = URLEncoder.encode(prompt, "UTF-8")
                    val pollUrl = "https://image.pollinations.ai/prompt/$encoded?width=800&height=512&nologo=true&seed=$seed"

                    // Coba unduh dan simpan lokal agar permanen
                    runCatching {
                        val request = Request.Builder().url(pollUrl).build()
                        val response = okHttpClient.newCall(request).execute()
                        if (response.isSuccessful) {
                            response.body?.bytes()?.let { bytes ->
                                FileOutputStream(outputFile).use { it.write(bytes) }
                                return@withContext Result.success(outputFile.absolutePath)
                            }
                        }
                    }
                    // Jika unduh gagal, kembalikan URL langsung untuk Coil
                    Result.success(pollUrl)
                }

                ImageProvider.GEMINI_IMAGEN -> {
                    if (geminiApiKey.isNullOrBlank()) {
                        return@withContext Result.failure(IllegalStateException("API Key Gemini belum diatur untuk Imagen 3."))
                    }

                    val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/imagen-3.0-generate-002:predict?key=$geminiApiKey"
                    val jsonBody = JSONObject().apply {
                        put("instances", JSONArray().apply {
                            put(JSONObject().apply { put("prompt", prompt) })
                        })
                        put("parameters", JSONObject().apply {
                            put("sampleCount", 1)
                            put("aspectRatio", "16:9")
                        })
                    }

                    val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
                    val request = Request.Builder()
                        .url(endpoint)
                        .post(requestBody)
                        .build()

                    val response = okHttpClient.newCall(request).execute()
                    val responseStr = response.body?.string().orEmpty()

                    if (!response.isSuccessful) {
                        return@withContext Result.failure(IllegalStateException("Imagen 3 Gagal (${response.code}): $responseStr"))
                    }

                    val responseJson = JSONObject(responseStr)
                    val predictions = responseJson.optJSONArray("predictions")
                    val base64Data = predictions?.optJSONObject(0)?.optString("bytesBase64Encoded")

                    if (base64Data.isNullOrBlank()) {
                        return@withContext Result.failure(IllegalStateException("Imagen 3 tidak mengembalikan data gambar."))
                    }

                    val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
                    FileOutputStream(outputFile).use { it.write(decodedBytes) }

                    Result.success(outputFile.absolutePath)
                }

                ImageProvider.HUGGING_FACE -> {
                    val endpoint = "https://api-inference.huggingface.co/models/black-forest-labs/FLUX.1-schnell"
                    val jsonBody = JSONObject().apply {
                        put("inputs", prompt)
                    }

                    val builder = Request.Builder()
                        .url(endpoint)
                        .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))

                    if (!hfToken.isNullOrBlank()) {
                        builder.addHeader("Authorization", "Bearer ${hfToken.trim()}")
                    }

                    val response = okHttpClient.newCall(builder.build()).execute()
                    if (!response.isSuccessful) {
                        val err = response.body?.string().orEmpty()
                        return@withContext Result.failure(IllegalStateException("Hugging Face Gagal (${response.code}): $err"))
                    }

                    val imageBytes = response.body?.bytes()
                        ?: return@withContext Result.failure(IllegalStateException("Hugging Face tidak mengembalikan byte gambar."))

                    FileOutputStream(outputFile).use { it.write(imageBytes) }
                    Result.success(outputFile.absolutePath)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
