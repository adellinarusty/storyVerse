package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StoryDao {

    // === MANAJEMEN CERITA (STORIES) ===
    @Query("SELECT * FROM stories ORDER BY updatedAt DESC")
    fun getAllStoriesFlow(): Flow<List<Story>>

    @Query("SELECT * FROM stories ORDER BY updatedAt DESC")
    suspend fun getAllStories(): List<Story>

    @Query("SELECT * FROM stories WHERE id = :id")
    suspend fun getStoryById(id: Long): Story?

    @Query("SELECT * FROM stories ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getLatestStory(): Story?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStory(story: Story): Long

    @Update
    suspend fun updateStory(story: Story)

    @Delete
    suspend fun deleteStory(story: Story)

    @Query("DELETE FROM stories WHERE id = :storyId")
    suspend fun deleteStoryById(storyId: Long)

    @Query("UPDATE stories SET updatedAt = :timestamp WHERE id = :storyId")
    suspend fun touchStory(storyId: Long, timestamp: Long = System.currentTimeMillis())

    // === MANAJEMEN PESAN / MEMORI PER CERITA ===
    @Query("SELECT * FROM story_messages WHERE storyId = :storyId ORDER BY timestamp ASC, id ASC")
    fun getMessagesForStoryFlow(storyId: Long): Flow<List<StoryMessage>>

    @Query("SELECT * FROM story_messages WHERE storyId = :storyId ORDER BY timestamp ASC, id ASC")
    suspend fun getMessagesForStory(storyId: Long): List<StoryMessage>

    @Query("SELECT * FROM story_messages WHERE storyId = :storyId ORDER BY timestamp DESC, id DESC LIMIT :limit")
    suspend fun getRecentMessagesDesc(storyId: Long, limit: Int): List<StoryMessage>

    @Query("SELECT * FROM story_messages WHERE storyId = :storyId AND role = 'model' ORDER BY timestamp DESC, id DESC LIMIT 1")
    suspend fun getLastModelMessage(storyId: Long): StoryMessage?

    @Query("SELECT * FROM story_messages WHERE id = :id")
    suspend fun getMessageById(id: Long): StoryMessage?

    @Query("UPDATE story_messages SET content = :newContent WHERE id = :id")
    suspend fun updateMessageContent(id: Long, newContent: String)

    @Query("UPDATE story_messages SET imageUrl = :imageUrl WHERE id = :id")
    suspend fun updateMessageImageUrl(id: Long, imageUrl: String?)

    @Query("DELETE FROM story_messages WHERE storyId = :storyId AND id > :id")
    suspend fun deleteMessagesAfterId(storyId: Long, id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: StoryMessage): Long

    @Query("DELETE FROM story_messages WHERE id = :id")
    suspend fun deleteMessageById(id: Long)

    @Query("DELETE FROM story_messages WHERE storyId = :storyId")
    suspend fun clearMessagesForStory(storyId: Long)
}
