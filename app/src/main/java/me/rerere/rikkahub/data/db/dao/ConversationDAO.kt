package me.rerere.rikkahub.data.db.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import me.rerere.rikkahub.data.db.entity.ConversationEntity
import me.rerere.rikkahub.data.repository.LightConversationEntity

@Dao
interface ConversationDAO {
    @Query("SELECT * FROM conversationentity ORDER BY update_at DESC")
    fun getAll(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversationentity ORDER BY update_at DESC")
    fun getAllPaging(): PagingSource<Int, ConversationEntity>

    @Query("SELECT * FROM conversationentity WHERE assistant_id = :assistantId ORDER BY update_at DESC")
    fun getConversationsOfAssistant(assistantId: String): Flow<List<ConversationEntity>>

    @Query("SELECT id, assistant_id as assistantId, title, create_at as createAt, update_at as updateAt FROM conversationentity WHERE assistant_id = :assistantId ORDER BY update_at DESC")
    fun getConversationsOfAssistantPaging(assistantId: String): PagingSource<Int, LightConversationEntity>

    @Query("SELECT * FROM conversationentity WHERE assistant_id = :assistantId ORDER BY update_at DESC LIMIT :limit")
    suspend fun getRecentConversationsOfAssistant(assistantId: String, limit: Int): List<ConversationEntity>

    @Query("SELECT DISTINCT c.* FROM conversationentity c LEFT JOIN message_node n ON c.id = n.conversation_id WHERE c.title LIKE '%' || :searchText || '%' OR n.messages LIKE '%' || :searchText || '%' ORDER BY c.update_at DESC")
    fun searchConversations(searchText: String): Flow<List<ConversationEntity>>

    @Query("SELECT DISTINCT c.id, c.assistant_id as assistantId, c.title, c.create_at as createAt, c.update_at as updateAt FROM conversationentity c LEFT JOIN message_node n ON c.id = n.conversation_id WHERE c.title LIKE '%' || :searchText || '%' OR n.messages LIKE '%' || :searchText || '%' ORDER BY c.update_at DESC")
    fun searchConversationsPaging(searchText: String): PagingSource<Int, LightConversationEntity>

    @Query("SELECT DISTINCT c.* FROM conversationentity c LEFT JOIN message_node n ON c.id = n.conversation_id WHERE c.assistant_id = :assistantId AND (c.title LIKE '%' || :searchText || '%' OR n.messages LIKE '%' || :searchText || '%') ORDER BY c.update_at DESC")
    fun searchConversationsOfAssistant(assistantId: String, searchText: String): Flow<List<ConversationEntity>>

    @Query("SELECT DISTINCT c.id, c.assistant_id as assistantId, c.title, c.create_at as createAt, c.update_at as updateAt FROM conversationentity c LEFT JOIN message_node n ON c.id = n.conversation_id WHERE c.assistant_id = :assistantId AND (c.title LIKE '%' || :searchText || '%' OR n.messages LIKE '%' || :searchText || '%') ORDER BY c.update_at DESC")
    fun searchConversationsOfAssistantPaging(assistantId: String, searchText: String): PagingSource<Int, LightConversationEntity>

    @Query("SELECT * FROM conversationentity WHERE id = :id")
    fun getConversationFlowById(id: String): Flow<ConversationEntity?>

    @Query("SELECT * FROM conversationentity WHERE id = :id")
    suspend fun getConversationById(id: String): ConversationEntity?

    @Insert
    suspend fun insert(conversation: ConversationEntity)

    @Update
    suspend fun update(conversation: ConversationEntity)

    @Delete
    suspend fun delete(conversation: ConversationEntity)

    @Query("UPDATE conversationentity SET nodes = '[]' WHERE id = :id")
    suspend fun resetConversationNodes(id: String)

    @Query("DELETE FROM conversationentity WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM conversationentity")
    suspend fun deleteAll()
}
