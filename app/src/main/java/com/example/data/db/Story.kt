package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.config.StoryPromptConfig
import com.example.data.image.ImageProvider
import org.json.JSONArray

@Entity(tableName = "stories")
data class Story(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val systemInstruction: String = StoryPromptConfig.DEFAULT_SYSTEM_INSTRUCTION,
    val selectedModel: String = StoryPromptConfig.DEFAULT_MODEL,
    val temperature: Float = StoryPromptConfig.DEFAULT_TEMPERATURE,
    val topP: Float = StoryPromptConfig.DEFAULT_TOP_P,
    val topK: Int = StoryPromptConfig.DEFAULT_TOP_K,
    val maxOutputTokens: Int = StoryPromptConfig.DEFAULT_MAX_OUTPUT_TOKENS,
    val imageProvider: String = ImageProvider.POLLINATIONS.name,
    val hfToken: String = "",
    val customImagesJson: String = "[]",
    val customBgmsJson: String = "[]",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Mengambil foto urutan pertama (#1) sebagai ikon/thumbnail daftar dan latar utama
     */
    fun getFirstImage(): String? {
        if (customImagesJson.isBlank() || customImagesJson == "[]") return null
        return try {
            val array = JSONArray(customImagesJson)
            if (array.length() > 0) array.getString(0) else null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Mengambil seluruh daftar foto yang tersimpan dalam format terurut
     */
    fun getAllImages(): List<String> {
        if (customImagesJson.isBlank() || customImagesJson == "[]") return emptyList()
        return try {
            val array = JSONArray(customImagesJson)
            val list = mutableListOf<String>()
            for (i in 0 until array.length()) {
                list.add(array.getString(i))
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun getImagesCount(): Int {
        return getAllImages().size
    }
}
