package com.example.data.repository

import com.example.config.ApiKeyManager
import com.example.config.StoryPromptConfig
import com.example.data.api.ContentPayload
import com.example.data.api.GeminiApiService
import com.example.data.api.GenerateContentRequest
import com.example.data.api.GenerationConfigPayload
import com.example.data.api.PartPayload
import com.example.data.db.Story
import com.example.data.db.StoryDao
import com.example.data.db.StoryMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class StoryRepository(
    private val storyDao: StoryDao,
    private val apiService: GeminiApiService
) {
    val allStoriesFlow: Flow<List<Story>> = storyDao.getAllStoriesFlow()

    suspend fun getAllStories(): List<Story> = withContext(Dispatchers.IO) {
        storyDao.getAllStories()
    }

    suspend fun getStoryById(id: Long): Story? = withContext(Dispatchers.IO) {
        storyDao.getStoryById(id)
    }

    suspend fun getLatestStory(): Story? = withContext(Dispatchers.IO) {
        storyDao.getLatestStory()
    }

    suspend fun insertStory(story: Story): Long = withContext(Dispatchers.IO) {
        storyDao.insertStory(story)
    }

    suspend fun updateStory(story: Story) = withContext(Dispatchers.IO) {
        storyDao.updateStory(story)
    }

    suspend fun deleteStory(storyId: Long) = withContext(Dispatchers.IO) {
        storyDao.deleteStoryById(storyId)
        storyDao.clearMessagesForStory(storyId)
    }

    fun getMessagesForStoryFlow(storyId: Long): Flow<List<StoryMessage>> {
        return storyDao.getMessagesForStoryFlow(storyId)
    }

    suspend fun getMessagesForStory(storyId: Long): List<StoryMessage> = withContext(Dispatchers.IO) {
        storyDao.getMessagesForStory(storyId)
    }

    suspend fun updateMessageImageUrl(messageId: Long, imageUrl: String?) = withContext(Dispatchers.IO) {
        storyDao.updateMessageImageUrl(messageId, imageUrl)
    }

    /**
     * Mengambil daftar model dinamis dari Google Generative Language API
     * URL: https://generativelanguage.googleapis.com/v1beta/models?key={$apiKey}
     */
    suspend fun fetchAvailableModels(customApiKey: String? = null): Result<List<String>> = withContext(Dispatchers.IO) {
        val keys = ApiKeyManager.getAllAvailableKeys(customApiKey)
        if (keys.isEmpty()) {
            return@withContext Result.success(StoryPromptConfig.AVAILABLE_MODELS)
        }
        val apiKey = ApiKeyManager.getNextApiKey(customApiKey)
        try {
            val response = apiService.listModels(apiKey = apiKey, pageSize = 100)
            val filteredModels = response.models
                .orEmpty()
                .filter { modelInfo ->
                    modelInfo.supportedGenerationMethods?.contains("generateContent") == true
                }
                .map { modelInfo ->
                    modelInfo.name.removePrefix("models/")
                }
                .filter { it.contains("gemini", ignoreCase = true) }
                .distinct()

            if (filteredModels.isNotEmpty()) {
                Result.success(filteredModels)
            } else {
                Result.success(StoryPromptConfig.AVAILABLE_MODELS)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Mempersiapkan riwayat percakapan untuk Gemini API untuk cerita tertentu.
     */
    suspend fun getFormattedGeminiHistory(storyId: Long, limit: Int = 30): List<ContentPayload> = withContext(Dispatchers.IO) {
        val recentMessagesDesc = storyDao.getRecentMessagesDesc(storyId, limit)
        val chronological = recentMessagesDesc.reversed()

        val contents = mutableListOf<ContentPayload>()
        for (msg in chronological) {
            val role = if (msg.role == "model") "model" else "user"
            if (contents.isEmpty() && role == "model") {
                continue
            }
            contents.add(
                ContentPayload(
                    role = role,
                    parts = listOf(PartPayload(text = msg.content))
                )
            )
        }
        contents
    }

    /**
     * Eksekusi request ke Gemini API dengan rotasi bergilir (rolling) API Key.
     */
    private suspend fun callGeminiWithRollingKeys(
        customApiKeyText: String?,
        modelName: String,
        request: GenerateContentRequest
    ): String {
        val keys = ApiKeyManager.getAllAvailableKeys(customApiKeyText)
        if (keys.isEmpty()) {
            throw IllegalStateException("API Key Gemini belum disetel. Masukkan satu atau beberapa API Key di tab System Instruction.")
        }

        var lastException: Exception? = null
        for (attempt in keys.indices) {
            val apiKey = ApiKeyManager.getNextApiKey(customApiKeyText)
            try {
                val response = apiService.generateContent(
                    model = modelName.ifBlank { StoryPromptConfig.DEFAULT_MODEL },
                    apiKey = apiKey,
                    request = request
                )
                val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!text.isNullOrBlank()) {
                    return text
                }
            } catch (e: Exception) {
                lastException = e
                val errorMsg = e.message.orEmpty()
                if (keys.size > 1 && (
                    errorMsg.contains("429") ||
                    errorMsg.contains("RESOURCE_EXHAUSTED") ||
                    errorMsg.contains("quota", ignoreCase = true) ||
                    errorMsg.contains("rate", ignoreCase = true) ||
                    errorMsg.contains("403") ||
                    errorMsg.contains("API_KEY_INVALID")
                )) {
                    continue
                } else {
                    throw e
                }
            }
        }
        throw lastException ?: IllegalStateException("Gagal memanggil API dengan semua kunci yang tersedia.")
    }

    /**
     * Mengirim chat/aksi user ke model Gemini dan menyimpan ke Room pada cerita terkait.
     */
    suspend fun sendUserAction(
        storyId: Long,
        userInput: String,
        systemInstruction: String,
        modelName: String,
        customApiKey: String? = null,
        temperature: Float = StoryPromptConfig.DEFAULT_TEMPERATURE,
        topP: Float = StoryPromptConfig.DEFAULT_TOP_P,
        topK: Int = StoryPromptConfig.DEFAULT_TOP_K,
        maxOutputTokens: Int = StoryPromptConfig.DEFAULT_MAX_OUTPUT_TOKENS
    ): Result<StoryMessage> = withContext(Dispatchers.IO) {
        try {
            // 1. Simpan input user ke database Room
            val userMsg = StoryMessage(
                storyId = storyId,
                role = "user",
                content = userInput.trim(),
                timestamp = System.currentTimeMillis()
            )
            storyDao.insertMessage(userMsg)
            storyDao.touchStory(storyId)

            // 2. Siapkan riwayat (maksimal 30 pesan terakhir)
            val history = getFormattedGeminiHistory(storyId, 30)

            val request = GenerateContentRequest(
                contents = history,
                systemInstruction = if (systemInstruction.isNotBlank()) {
                    ContentPayload(parts = listOf(PartPayload(text = systemInstruction)))
                } else null,
                generationConfig = GenerationConfigPayload(
                    temperature = temperature,
                    topP = topP,
                    topK = topK,
                    maxOutputTokens = maxOutputTokens
                ),
                safetySettings = StoryPromptConfig.HARDCODED_SAFETY_SETTINGS
            )

            // 3. Panggil API Gemini dengan rolling key
            val responseText = callGeminiWithRollingKeys(
                customApiKeyText = customApiKey,
                modelName = modelName,
                request = request
            )

            // 4. Simpan respon model ke database Room
            val modelMsg = StoryMessage(
                storyId = storyId,
                role = "model",
                content = responseText.trim(),
                timestamp = System.currentTimeMillis()
            )
            storyDao.insertMessage(modelMsg)
            storyDao.touchStory(storyId)

            Result.success(modelMsg)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Tombol Regenerate pada respon AI terakhir di cerita terkait.
     */
    suspend fun regenerateLastResponse(
        storyId: Long,
        systemInstruction: String,
        modelName: String,
        customApiKey: String? = null,
        temperature: Float = StoryPromptConfig.DEFAULT_TEMPERATURE,
        topP: Float = StoryPromptConfig.DEFAULT_TOP_P,
        topK: Int = StoryPromptConfig.DEFAULT_TOP_K,
        maxOutputTokens: Int = StoryPromptConfig.DEFAULT_MAX_OUTPUT_TOKENS
    ): Result<StoryMessage> = withContext(Dispatchers.IO) {
        try {
            val lastModelMessage = storyDao.getLastModelMessage(storyId)
                ?: return@withContext Result.failure(IllegalStateException("Tidak ada respon model untuk diregenerasi."))

            // 1. Hapus respon model terakhir dari Room
            storyDao.deleteMessageById(lastModelMessage.id)

            // 2. Ambil riwayat terkoreksi
            val history = getFormattedGeminiHistory(storyId, 30)
            if (history.isEmpty()) {
                return@withContext Result.failure(IllegalStateException("Tidak ada riwayat untuk dikirim."))
            }

            val request = GenerateContentRequest(
                contents = history,
                systemInstruction = if (systemInstruction.isNotBlank()) {
                    ContentPayload(parts = listOf(PartPayload(text = systemInstruction)))
                } else null,
                generationConfig = GenerationConfigPayload(
                    temperature = (temperature + 0.05f).coerceAtMost(2.0f),
                    topP = topP,
                    topK = topK,
                    maxOutputTokens = maxOutputTokens
                ),
                safetySettings = StoryPromptConfig.HARDCODED_SAFETY_SETTINGS
            )

            val responseText = callGeminiWithRollingKeys(
                customApiKeyText = customApiKey,
                modelName = modelName,
                request = request
            )

            val newModelMsg = StoryMessage(
                storyId = storyId,
                role = "model",
                content = responseText.trim(),
                timestamp = System.currentTimeMillis()
            )
            storyDao.insertMessage(newModelMsg)
            storyDao.touchStory(storyId)

            Result.success(newModelMsg)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Mengedit pesan user dan meminta model memberikan respon baru menimpa respon lama pada cerita terkait.
     */
    suspend fun editUserActionAndRegenerate(
        storyId: Long,
        messageId: Long,
        newContent: String,
        systemInstruction: String,
        modelName: String,
        customApiKey: String? = null,
        temperature: Float = StoryPromptConfig.DEFAULT_TEMPERATURE,
        topP: Float = StoryPromptConfig.DEFAULT_TOP_P,
        topK: Int = StoryPromptConfig.DEFAULT_TOP_K,
        maxOutputTokens: Int = StoryPromptConfig.DEFAULT_MAX_OUTPUT_TOKENS
    ): Result<StoryMessage> = withContext(Dispatchers.IO) {
        try {
            val userMsg = storyDao.getMessageById(messageId)
                ?: return@withContext Result.failure(IllegalStateException("Pesan tidak ditemukan."))

            // 1. Update isi pesan user
            storyDao.updateMessageContent(messageId, newContent.trim())

            // 2. Hapus seluruh respon/pesan setelah pesan user ini
            storyDao.deleteMessagesAfterId(storyId, messageId)

            // 3. Ambil riwayat terkoreksi
            val history = getFormattedGeminiHistory(storyId, 30)
            if (history.isEmpty()) {
                return@withContext Result.failure(IllegalStateException("Riwayat percakapan kosong."))
            }

            val request = GenerateContentRequest(
                contents = history,
                systemInstruction = if (systemInstruction.isNotBlank()) {
                    ContentPayload(parts = listOf(PartPayload(text = systemInstruction)))
                } else null,
                generationConfig = GenerationConfigPayload(
                    temperature = temperature,
                    topP = topP,
                    topK = topK,
                    maxOutputTokens = maxOutputTokens
                ),
                safetySettings = StoryPromptConfig.HARDCODED_SAFETY_SETTINGS
            )

            val responseText = callGeminiWithRollingKeys(
                customApiKeyText = customApiKey,
                modelName = modelName,
                request = request
            )

            val newModelMsg = StoryMessage(
                storyId = storyId,
                role = "model",
                content = responseText.trim(),
                timestamp = System.currentTimeMillis()
            )
            storyDao.insertMessage(newModelMsg)
            storyDao.touchStory(storyId)

            Result.success(newModelMsg)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Memulai sesi cerita baru dengan chat pembuka yang dihasilkan oleh model dari System Instruction.
     */
    suspend fun startNewSessionWithOpening(
        storyId: Long,
        systemInstruction: String,
        modelName: String,
        customApiKey: String? = null,
        temperature: Float = StoryPromptConfig.DEFAULT_TEMPERATURE,
        topP: Float = StoryPromptConfig.DEFAULT_TOP_P,
        topK: Int = StoryPromptConfig.DEFAULT_TOP_K,
        maxOutputTokens: Int = StoryPromptConfig.DEFAULT_MAX_OUTPUT_TOKENS
    ): Result<StoryMessage> = withContext(Dispatchers.IO) {
        try {
            // Hapus riwayat pesan lama pada cerita ini saja
            storyDao.clearMessagesForStory(storyId)

            val availableKeys = ApiKeyManager.getAllAvailableKeys(customApiKey)
            if (availableKeys.isEmpty()) {
                val fallbackIntro = StoryMessage(
                    storyId = storyId,
                    role = "model",
                    content = "Halo! Silakan masukkan satu atau beberapa API Key Gemini Anda di tab System Instruction agar saya dapat merespons sesuai konfigurasi.",
                    timestamp = System.currentTimeMillis()
                )
                storyDao.insertMessage(fallbackIntro)
                storyDao.touchStory(storyId)
                return@withContext Result.success(fallbackIntro)
            }

            val triggerPrompt = "Mulai"

            val request = GenerateContentRequest(
                contents = listOf(
                    ContentPayload(
                        role = "user",
                        parts = listOf(PartPayload(text = triggerPrompt))
                    )
                ),
                systemInstruction = if (systemInstruction.isNotBlank()) {
                    ContentPayload(parts = listOf(PartPayload(text = systemInstruction)))
                } else null,
                generationConfig = GenerationConfigPayload(
                    temperature = temperature,
                    topP = topP,
                    topK = topK,
                    maxOutputTokens = maxOutputTokens
                ),
                safetySettings = StoryPromptConfig.HARDCODED_SAFETY_SETTINGS
            )

            val openingText = callGeminiWithRollingKeys(
                customApiKeyText = customApiKey,
                modelName = modelName,
                request = request
            )

            val modelMsg = StoryMessage(
                storyId = storyId,
                role = "model",
                content = openingText.trim(),
                timestamp = System.currentTimeMillis()
            )
            storyDao.insertMessage(modelMsg)
            storyDao.touchStory(storyId)

            Result.success(modelMsg)
        } catch (e: Exception) {
            val fallbackMsg = StoryMessage(
                storyId = storyId,
                role = "model",
                content = "Halo! Sesi baru telah dimulai. (${e.localizedMessage ?: "Koneksi API belum aktif"}). Silakan masukkan pesan pertamamu.",
                timestamp = System.currentTimeMillis()
            )
            storyDao.insertMessage(fallbackMsg)
            storyDao.touchStory(storyId)
            Result.failure(e)
        }
    }
}
