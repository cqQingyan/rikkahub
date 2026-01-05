package me.rerere.rikkahub.data.repository

import android.content.Context
import android.database.sqlite.SQLiteBlobTooBigException
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import me.rerere.ai.ui.UIMessage
import me.rerere.rikkahub.data.db.AppDatabase
import me.rerere.rikkahub.data.db.dao.ConversationDAO
import me.rerere.rikkahub.data.db.dao.MessageNodeDAO
import me.rerere.rikkahub.data.db.dao.MessageNodeFtsDAO
import me.rerere.rikkahub.data.db.entity.ConversationEntity
import me.rerere.rikkahub.data.db.entity.MessageNodeEntity
import me.rerere.rikkahub.data.db.entity.MessageNodeFtsEntity
import me.rerere.rikkahub.data.model.Conversation
import me.rerere.rikkahub.data.model.MessageNode
import me.rerere.rikkahub.utils.JsonInstant
import me.rerere.rikkahub.utils.deleteChatFiles
import java.time.Instant
import kotlin.uuid.Uuid

class ConversationRepository(
    private val context: Context,
    private val conversationDAO: ConversationDAO,
    private val messageNodeDAO: MessageNodeDAO,
    private val messageNodeFtsDAO: MessageNodeFtsDAO,
    private val database: AppDatabase,
) {
    companion object {
        private const val PAGE_SIZE = 20
        private const val INITIAL_LOAD_SIZE = 40
    }

    suspend fun getRecentConversations(assistantId: Uuid, limit: Int = 10): List<Conversation> {
        return conversationDAO.getRecentConversationsOfAssistant(
            assistantId = assistantId.toString(),
            limit = limit
        ).map { entity ->
            val nodes = loadMessageNodes(entity.id)
            conversationEntityToConversation(entity, nodes)
        }
    }

    fun getConversationsOfAssistant(assistantId: Uuid): Flow<List<Conversation>> {
        return conversationDAO
            .getConversationsOfAssistant(assistantId.toString())
            .map { flow ->
                flow.map { entity ->
                    // 列表视图不需要完整的 nodes，使用空列表
                    conversationEntityToConversation(entity, emptyList())
                }
            }
    }

    fun getConversationsOfAssistantPaging(assistantId: Uuid): Flow<PagingData<Conversation>> = Pager(
        config = PagingConfig(
            pageSize = PAGE_SIZE,
            initialLoadSize = INITIAL_LOAD_SIZE,
            enablePlaceholders = false
        ),
        pagingSourceFactory = { conversationDAO.getConversationsOfAssistantPaging(assistantId.toString()) }
    ).flow.map { pagingData ->
        pagingData.map { entity ->
            conversationSummaryToConversation(entity)
        }
    }

    fun searchConversations(titleKeyword: String): Flow<List<Conversation>> {
        val sanitizedQuery = titleKeyword.replace("\"", "") + "*"
        return messageNodeFtsDAO
            .searchConversations(query = titleKeyword, ftsQuery = sanitizedQuery)
            .map { list ->
                list.map { entity ->
                    conversationEntityToConversation(entity, emptyList())
                }
            }
    }

    fun searchConversationsPaging(titleKeyword: String): Flow<PagingData<Conversation>> = Pager(
        config = PagingConfig(
            pageSize = PAGE_SIZE,
            initialLoadSize = INITIAL_LOAD_SIZE,
            enablePlaceholders = false
        ),
        pagingSourceFactory = {
            if (titleKeyword.isBlank()) {
                conversationDAO.searchConversationsPaging(titleKeyword)
            } else {
                val sanitizedQuery = titleKeyword.replace("\"", "") + "*"
                messageNodeFtsDAO.searchConversationsPaging(query = titleKeyword, ftsQuery = sanitizedQuery)
            }
        }
    ).flow.map { pagingData ->
        pagingData.map { entity ->
            conversationSummaryToConversation(entity)
        }
    }

    fun searchConversationsOfAssistant(assistantId: Uuid, titleKeyword: String): Flow<List<Conversation>> {
        return conversationDAO
            .searchConversationsOfAssistant(assistantId.toString(), titleKeyword)
            .map { flow ->
                flow.map { entity ->
                    conversationEntityToConversation(entity, emptyList())
                }
            }
    }

    fun searchConversationsOfAssistantPaging(assistantId: Uuid, titleKeyword: String): Flow<PagingData<Conversation>> =
        Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                initialLoadSize = INITIAL_LOAD_SIZE,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                conversationDAO.searchConversationsOfAssistantPaging(
                    assistantId.toString(),
                    titleKeyword
                )
            }
        ).flow.map { pagingData ->
            pagingData.map { entity ->
                conversationSummaryToConversation(entity)
            }
        }

    suspend fun getConversationById(uuid: Uuid): Conversation? {
        val entity = conversationDAO.getConversationById(uuid.toString())
        return if (entity != null) {
            val nodes = loadMessageNodes(entity.id)
            conversationEntityToConversation(entity, nodes)
        } else null
    }

    suspend fun insertConversation(conversation: Conversation) {
        database.withTransaction {
            conversationDAO.insert(
                conversationToConversationEntity(conversation)
            )
            saveMessageNodes(conversation.id.toString(), conversation.messageNodes)
        }
    }

    suspend fun updateConversation(conversation: Conversation) {
        database.withTransaction {
            conversationDAO.update(
                conversationToConversationEntity(conversation)
            )
            // 删除旧的节点，插入新的节点
            val conversationId = conversation.id.toString()
            messageNodeDAO.deleteByConversation(conversationId)
            // Also update FTS
            messageNodeFtsDAO.deleteByConversation(conversationId)

            saveMessageNodes(conversationId, conversation.messageNodes)
        }
    }

    suspend fun deleteConversation(conversation: Conversation) {
        // 获取完整的 Conversation（包含 messageNodes）以正确清理文件
        val fullConversation = if (conversation.messageNodes.isEmpty()) {
            getConversationById(conversation.id) ?: conversation
        } else {
            conversation
        }
        database.withTransaction {
            // message_node 会通过 CASCADE 自动删除
            conversationDAO.delete(
                conversationToConversationEntity(conversation)
            )
            // Manually delete FTS entries as CASCADE doesn't work for FTS unless using triggers or contentEntity with limitations
            messageNodeFtsDAO.deleteByConversation(conversation.id.toString())
        }
        context.deleteChatFiles(fullConversation.files)
    }

    suspend fun deleteConversationOfAssistant(assistantId: Uuid) {
        getConversationsOfAssistant(assistantId).first().forEach { conversation ->
            deleteConversation(conversation)
        }
    }

    fun conversationToConversationEntity(conversation: Conversation): ConversationEntity {
        require(conversation.messageNodes.none { it.messages.any { message -> message.hasBase64Part() } })
        return ConversationEntity(
            id = conversation.id.toString(),
            title = conversation.title,
            nodes = "[]",  // nodes 现在存储在单独的表中
            createAt = conversation.createAt.toEpochMilli(),
            updateAt = conversation.updateAt.toEpochMilli(),
            assistantId = conversation.assistantId.toString(),
            truncateIndex = conversation.truncateIndex,
            chatSuggestions = JsonInstant.encodeToString(conversation.chatSuggestions),
        )
    }

    fun conversationEntityToConversation(
        conversationEntity: ConversationEntity,
        messageNodes: List<MessageNode>
    ): Conversation {
        return Conversation(
            id = Uuid.parse(conversationEntity.id),
            title = conversationEntity.title,
            messageNodes = messageNodes.filter { it.messages.isNotEmpty() },
            createAt = Instant.ofEpochMilli(conversationEntity.createAt),
            updateAt = Instant.ofEpochMilli(conversationEntity.updateAt),
            assistantId = Uuid.parse(conversationEntity.assistantId),
            truncateIndex = conversationEntity.truncateIndex,
            chatSuggestions = JsonInstant.decodeFromString(conversationEntity.chatSuggestions),
        )
    }

    private fun conversationSummaryToConversation(entity: LightConversationEntity): Conversation {
        return Conversation(
            id = Uuid.parse(entity.id),
            assistantId = Uuid.parse(entity.assistantId),
            title = entity.title,
            createAt = Instant.ofEpochMilli(entity.createAt),
            updateAt = Instant.ofEpochMilli(entity.updateAt),
            messageNodes = emptyList(),
        )
    }

    private suspend fun loadMessageNodes(conversationId: String): List<MessageNode> {
        return database.withTransaction {
            val nodes = mutableListOf<MessageNode>()
            var offset = 0
            val pageSize = 64
            while (true) {
                val page = messageNodeDAO.getNodesOfConversationPaged(conversationId, pageSize, offset)
                if (page.isEmpty()) break
                page.forEach { entity ->
                    nodes.add(
                        MessageNode(
                            id = Uuid.parse(entity.id),
                            messages = JsonInstant.decodeFromString<List<UIMessage>>(entity.messages),
                            selectIndex = entity.selectIndex
                        )
                    )
                }
                offset += page.size
            }
            nodes
        }
    }

    private suspend fun saveMessageNodes(conversationId: String, nodes: List<MessageNode>) {
        val entities = nodes.mapIndexed { index, node ->
            MessageNodeEntity(
                id = node.id.toString(),
                conversationId = conversationId,
                nodeIndex = index,
                messages = JsonInstant.encodeToString(node.messages),
                selectIndex = node.selectIndex
            )
        }
        messageNodeDAO.insertAll(entities)

        // Populate FTS
        val ftsEntities = nodes.map { node ->
            // Extract text content from messages
            val content = node.messages.joinToString("\n") { message ->
                message.content
            }
            MessageNodeFtsEntity(
                nodeId = node.id.toString(),
                conversationId = conversationId,
                content = content
            )
        }
        messageNodeFtsDAO.insertAll(ftsEntities)
    }

    suspend fun populateFtsIfNeeded() {
        val count = messageNodeFtsDAO.searchConversations("").first().size // This is inefficient to check count.
        // Better to check specific table count but DAO doesn't expose it.
        // Assume if user searches and gets nothing from FTS, it might be empty.
        // But `searchConversations` with empty string relies on LIKE.

        // Let's add a simple count method to FTS DAO or just verify logic.
        // We can't easily count FTS rows with current DAO.
        // I will add a count method to MessageNodeFtsDAO
        // Wait, I cannot modify interface easily without ensuring impl. Room generates it.

        // Let's try to just run the population logic.
        // It's safe to run if we check emptiness first.
        // I'll add `count()` to DAO.
        val ftsCount = messageNodeFtsDAO.count()
        if (ftsCount == 0) {
            val conversations = conversationDAO.getAll().first()
            conversations.forEach { conversation ->
                val nodes = loadMessageNodes(conversation.id)
                saveMessageNodesToFts(conversation.id, nodes)
            }
        }
    }

    private suspend fun saveMessageNodesToFts(conversationId: String, nodes: List<MessageNode>) {
        val ftsEntities = nodes.map { node ->
            val content = node.messages.joinToString("\n") { message ->
                message.content
            }
            MessageNodeFtsEntity(
                nodeId = node.id.toString(),
                conversationId = conversationId,
                content = content
            )
        }
        messageNodeFtsDAO.insertAll(ftsEntities)
    }
}

/**
 * 轻量级的会话查询结果，不包含 nodes 和 suggestions 字段
 */
data class LightConversationEntity(
    val id: String,
    val assistantId: String,
    val title: String,
    val createAt: Long,
    val updateAt: Long,
)
