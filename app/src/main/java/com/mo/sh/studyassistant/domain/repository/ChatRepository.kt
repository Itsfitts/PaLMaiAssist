package ai.assist.palmai.app.domain.repository

import android.net.Uri
import ai.assist.palmai.app.domain.model.ChatWithMessages
import ai.assist.palmai.app.domain.model.Message
import ai.assist.palmai.app.domain.model.MessageSection
import ai.assist.palmai.app.domain.model.NetworkResult
import kotlinx.coroutines.flow.Flow

interface ChatRepository {

    suspend fun sendPrompt(content: String, section: MessageSection, apiKey: String, pdfUri: Uri?): NetworkResult

    suspend fun sendMessage(newMessageContent: String, apiKey: String, chat: ChatWithMessages?, imageUri: Uri?, pdfUri: Uri?): NetworkResult

    fun getSectionChats(section: Int): Flow<List<ChatWithMessages>>

    suspend fun clearChatContext()

    suspend fun resetAllChats(section: Int)

    suspend fun writePdfFile(message: Message, uri: Uri): Boolean

}