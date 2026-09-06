package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)
