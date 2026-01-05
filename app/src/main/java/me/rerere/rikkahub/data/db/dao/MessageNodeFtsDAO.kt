package me.rerere.rikkahub.data.db.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import me.rerere.rikkahub.data.db.entity.ConversationEntity
import me.rerere.rikkahub.data.db.entity.MessageNodeFtsEntity
import me.rerere.rikkahub.data.repository.LightConversationEntity

@Dao
interface MessageNodeFtsDAO {
    @Insert
    suspend fun insertAll(entities: List<MessageNodeFtsEntity>)

    @Query("DELETE FROM message_node_fts WHERE conversation_id = :conversationId")
    suspend fun deleteByConversation(conversationId: String)

    @Query("DELETE FROM message_node_fts")
    suspend fun deleteAll()

    @Query("SELECT count(*) FROM message_node_fts")
    suspend fun count(): Int

    // Hybrid query: Title (LIKE) OR Content (FTS)
    @Query("""
        SELECT DISTINCT c.id, c.assistant_id as assistantId, c.title, c.create_at as createAt, c.update_at as updateAt
        FROM conversationentity c
        WHERE c.title LIKE '%' || :query || '%'
        OR c.id IN (
            SELECT conversation_id
            FROM message_node_fts
            WHERE message_node_fts MATCH :ftsQuery
        )
        ORDER BY c.update_at DESC
    """)
    fun searchConversationsPaging(query: String, ftsQuery: String): PagingSource<Int, LightConversationEntity>

    @Query("""
        SELECT DISTINCT c.*
        FROM conversationentity c
        WHERE c.title LIKE '%' || :query || '%'
        OR c.id IN (
            SELECT conversation_id
            FROM message_node_fts
            WHERE message_node_fts MATCH :ftsQuery
        )
        ORDER BY c.update_at DESC
    """)
    fun searchConversations(query: String, ftsQuery: String): Flow<List<ConversationEntity>>
}
