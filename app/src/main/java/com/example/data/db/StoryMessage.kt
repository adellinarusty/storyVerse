package com.example.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "story_messages",
    indices = [Index(value = ["storyId"])]
)
data class StoryMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val storyId: Long = 0,
    val role: String, // "user" atau "model"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val imageUrl: String? = null
)
