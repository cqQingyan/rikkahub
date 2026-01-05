package me.rerere.rikkahub.service.chat

import kotlin.uuid.Uuid

data class ChatError(
    val id: Uuid = Uuid.random(),
    val error: Throwable,
    val timestamp: Long = System.currentTimeMillis()
)
