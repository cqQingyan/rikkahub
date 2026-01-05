package me.rerere.rikkahub.domain.usecase.chat

import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.UIMessagePart
import me.rerere.rikkahub.data.model.Conversation
import kotlin.uuid.Uuid

interface ChatUseCase {
    suspend fun forkMessage(
        conversation: Conversation,
        message: UIMessage,
        saveConversation: suspend (Conversation) -> Unit
    ): Conversation

    suspend fun deleteMessage(
        conversation: Conversation,
        message: UIMessage,
        saveConversation: suspend (Conversation) -> Unit
    )

    suspend fun editMessage(
        conversation: Conversation,
        messageId: Uuid,
        newParts: List<UIMessagePart>,
        saveConversation: suspend (Conversation) -> Unit
    )
}
