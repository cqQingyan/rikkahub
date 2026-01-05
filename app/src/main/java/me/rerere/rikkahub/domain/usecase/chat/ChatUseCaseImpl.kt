package me.rerere.rikkahub.domain.usecase.chat

import android.content.Context
import androidx.core.net.toUri
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.UIMessagePart
import me.rerere.rikkahub.data.model.Conversation
import me.rerere.rikkahub.utils.createChatFilesByContents
import kotlin.uuid.Uuid

class ChatUseCaseImpl(
    private val context: Context
) : ChatUseCase {
    override suspend fun forkMessage(
        conversation: Conversation,
        message: UIMessage,
        saveConversation: suspend (Conversation) -> Unit
    ): Conversation {
        val node = conversation.getMessageNodeByMessage(message)
        val nodes = conversation.messageNodes.subList(
            0, conversation.messageNodes.indexOf(node) + 1
        ).map { messageNode ->
            messageNode.copy(
                id = Uuid.random(),  // 生成新的节点 ID
                messages = messageNode.messages.map { msg ->
                    msg.copy(
                        parts = msg.parts.map { part ->
                            when (part) {
                                is UIMessagePart.Image -> {
                                    val url = part.url
                                    if (url.startsWith("file:")) {
                                        val copied = context.createChatFilesByContents(
                                            listOf(url.toUri())
                                        ).firstOrNull()
                                        if (copied != null) part.copy(url = copied.toString()) else part
                                    } else part
                                }

                                is UIMessagePart.Document -> {
                                    val url = part.url
                                    if (url.startsWith("file:")) {
                                        val copied = context.createChatFilesByContents(
                                            listOf(url.toUri())
                                        ).firstOrNull()
                                        if (copied != null) part.copy(url = copied.toString()) else part
                                    } else part
                                }

                                is UIMessagePart.Video -> {
                                    val url = part.url
                                    if (url.startsWith("file:")) {
                                        val copied = context.createChatFilesByContents(
                                            listOf(url.toUri())
                                        ).firstOrNull()
                                        if (copied != null) part.copy(url = copied.toString()) else part
                                    } else part
                                }

                                is UIMessagePart.Audio -> {
                                    val url = part.url
                                    if (url.startsWith("file:")) {
                                        val copied = context.createChatFilesByContents(
                                            listOf(url.toUri())
                                        ).firstOrNull()
                                        if (copied != null) part.copy(url = copied.toString()) else part
                                    } else part
                                }

                                else -> part
                            }
                        }
                    )
                }
            )
        }
        val newConversation = Conversation(
            id = Uuid.random(),
            assistantId = conversation.assistantId,
            messageNodes = nodes
        )
        saveConversation(newConversation)
        return newConversation
    }

    override suspend fun deleteMessage(
        conversation: Conversation,
        message: UIMessage,
        saveConversation: suspend (Conversation) -> Unit
    ) {
        val relatedMessages = collectRelatedMessages(conversation, message)

        var currentConversation = conversation
        currentConversation = deleteMessageInternal(currentConversation, message) ?: currentConversation
        relatedMessages.forEach { related ->
             currentConversation = deleteMessageInternal(currentConversation, related) ?: currentConversation
        }

        if (currentConversation != conversation) {
            saveConversation(currentConversation)
        }
    }

    private fun deleteMessageInternal(conversation: Conversation, message: UIMessage): Conversation? {
        val node = conversation.getMessageNodeByMessage(message) ?: return null
        val nodeIndex = conversation.messageNodes.indexOf(node)
        if (nodeIndex == -1) return null

        return if (node.messages.size == 1) {
            conversation.copy(
                messageNodes = conversation.messageNodes.filterIndexed { index, _ -> index != nodeIndex })
        } else {
            val updatedNodes = conversation.messageNodes.mapNotNull { currentNode ->
                if (currentNode == node) {
                     val newMessages = currentNode.messages.filter { it.id != message.id }
                    if (newMessages.isEmpty()) {
                        null
                    } else {
                        val newSelectIndex = if (currentNode.selectIndex >= newMessages.size) {
                            newMessages.lastIndex
                        } else {
                            currentNode.selectIndex
                        }
                        currentNode.copy(
                            messages = newMessages,
                            selectIndex = newSelectIndex
                        )
                    }
                } else {
                    currentNode
                }
            }
            conversation.copy(messageNodes = updatedNodes)
        }
    }

    private fun collectRelatedMessages(conversation: Conversation, message: UIMessage): List<UIMessage> {
        val currentMessages = conversation.currentMessages
        val index = currentMessages.indexOf(message)
        if (index == -1) return emptyList()

        val relatedMessages = hashSetOf<UIMessage>()
        for (i in index - 1 downTo 0) {
            if (currentMessages[i].hasPart<UIMessagePart.ToolCall>() || currentMessages[i].hasPart<UIMessagePart.ToolResult>()) {
                relatedMessages.add(currentMessages[i])
            } else {
                break
            }
        }
        for (i in index + 1 until currentMessages.size) {
            if (currentMessages[i].hasPart<UIMessagePart.ToolCall>() || currentMessages[i].hasPart<UIMessagePart.ToolResult>()) {
                relatedMessages.add(currentMessages[i])
            } else {
                break
            }
        }
        return relatedMessages.toList()
    }

    override suspend fun editMessage(
        conversation: Conversation,
        messageId: Uuid,
        newParts: List<UIMessagePart>,
        saveConversation: suspend (Conversation) -> Unit
    ) {
         val newConversation = conversation.copy(
            messageNodes = conversation.messageNodes.map { node ->
                if (!node.messages.any { it.id == messageId }) {
                    return@map node // 如果这个node没有这个消息，则不修改
                }
                node.copy(
                    messages = node.messages + UIMessage(
                        role = node.role,
                        parts = newParts,
                    ), selectIndex = node.messages.size
                )
            },
        )
        saveConversation(newConversation)
    }
}
