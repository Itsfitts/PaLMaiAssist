package ai.assist.palmai.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import ai.assist.palmai.app.domain.model.Chat
import ai.assist.palmai.app.domain.model.ChatWithMessages
import ai.assist.palmai.app.domain.model.Message
import kotlinx.coroutines.flow.Flow

@Dao
interface MessagesDao {

    @Insert
    suspend fun addChat(chat: Chat): Long

    @Insert
    suspend fun addMessage(message: Message): Long
    @Transaction
    @Query("SELECT * FROM chats WHERE section = :section ORDER BY time DESC")
    fun allSectionChats(section: Int): Flow<List<ChatWithMessages>>

    @Update
    suspend fun updateMessage(message: Message)

    @Update
    suspend fun updateChat(chat: Chat)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteMessage(id: Long)

    @Query("DELETE FROM chats WHERE id = :id")
    suspend fun deleteChat(id: Long)

    @Query("DELETE FROM chats WHERE section = :section")
    suspend fun deleteSectionChats(section: Int)
}