package ai.assist.palmai.app.data.repository

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import ai.assist.palmai.app.R
import ai.assist.palmai.app.app.App
import ai.assist.palmai.app.data.local.MessagesDao
import ai.assist.palmai.app.data.network.PalmApi
import ai.assist.palmai.app.domain.model.Chat
import ai.assist.palmai.app.domain.model.ChatWithMessages
import ai.assist.palmai.app.domain.model.Message
import ai.assist.palmai.app.domain.model.MessagePrompt
import ai.assist.palmai.app.domain.model.MessageSection
import ai.assist.palmai.app.domain.model.MessageType
import ai.assist.palmai.app.domain.model.NetworkResult
import ai.assist.palmai.app.domain.model.PalmMessage
import ai.assist.palmai.app.domain.model.PalmText
import ai.assist.palmai.app.domain.model.PalmTextPrompt
import ai.assist.palmai.app.domain.model.PalmeMessagePrompt
import ai.assist.palmai.app.domain.repository.ChatRepository
import ai.assist.palmai.app.presentation.common_components.AttachmentType
import ai.assist.palmai.app.util.MLManager
import ai.assist.palmai.app.util.PDFManager
import ai.assist.palmai.app.util.PromptUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ChatRepositoryImpl(
    private val messagesDao: MessagesDao,
    private val api: PalmApi,
    private val ml: MLManager,
    private val pdf: PDFManager
) : ChatRepository {

    private var latestChat: Chat? = null

    override suspend fun sendPrompt(
        content: String,
        section: MessageSection,
        apiKey: String,
        pdfUri: Uri?,
    ): NetworkResult = withContext(Dispatchers.IO) {

        if (apiKey.isBlank()){
            return@withContext NetworkResult.Error.Network("${App.getString(R.string.no_api_key_error)}\n${App.getString(R.string.api_key_creation_message)}")
        }
        val chat = Chat(
            section = section.ordinal
        )
        val cId = messagesDao.addChat(chat)
        val message = Message(
            chatId = cId,
            type = MessageType.User.ordinal,
            content = if (pdfUri != null) "" else content,
            time = System.currentTimeMillis(),
            attachment = pdfUri?.toString(),
            attachmentType = if (pdfUri != null) AttachmentType.Pdf.ordinal
            else null,
            contentIsPdf = pdfUri != null,
            attachmentFileName = if (pdfUri != null) DocumentFile.fromSingleUri(
                App.instance,
                pdfUri
            )?.name else ""
        )
        val mId = messagesDao.addMessage(message)

        val messageContent = if (pdfUri != null) {
            val result = pdf.extractText(pdfUri)
            if (result is PDFManager.PDFResult.Success) {
                messagesDao.updateMessage(
                    message.copy(
                        id = mId,
                        content = result.text
                    )
                )
                result.text
            } else {
                messagesDao.deleteChat(cId)
                return@withContext NetworkResult.Error.Attachment(result.text)
            }
        } else {
            content
        }

        try {
            val text = "${getSectionSystemPrompt(section)}\n\"$messageContent\""

            val response = api.generateText(
                prompt = PalmTextPrompt(
                    PalmText(
                        text = text
                    )
                ),
                apiKey = apiKey
            )
            if (response.candidates.isEmpty()) {
                messagesDao.deleteChat(cId)
                return@withContext NetworkResult.Error.Unknown(App.getString(R.string.unknown_error))
            }
            messagesDao.addMessage(
                Message(
                    chatId = cId,
                    type = MessageType.Bot.ordinal,
                    content = response.candidates.first().output,
                    time = System.currentTimeMillis(),
                    contentIsPdf = true,
                    pdfContentType = getPdfContentType(section),
                    attachmentFileName = "${getPdfContentType(section)}.pdf"
                )
            )
            messagesDao.updateChat(
                chat.copy(
                    id = cId,
                    done = true
                )
            )
            NetworkResult.Success
        } catch (e: IOException) {
            e.printStackTrace()
            messagesDao.deleteChat(cId)
            NetworkResult.Error.Network(App.getString(R.string.no_internet))
        } catch (e: Exception) {
            e.printStackTrace()
            messagesDao.deleteChat(cId)
            NetworkResult.Error.Unknown(App.getString(R.string.unknown_error))
        }
    }

    override suspend fun sendMessage(
        newMessageContent: String,
        apiKey: String,
        chat: ChatWithMessages?,
        imageUri: Uri?,
        pdfUri: Uri?,
    ): NetworkResult = withContext(Dispatchers.IO) {

        if (apiKey.isBlank()){
            return@withContext NetworkResult.Error.Network("${App.getString(R.string.no_api_key_error)}\n${App.getString(R.string.api_key_creation_message)}")
        }

        val cId = chat?.chat?.id ?: messagesDao.addChat(
            Chat(
                section = MessageSection.Tutor.ordinal
            )
        )
        val message = Message(
            chatId = cId,
            type = MessageType.User.ordinal,
            content = newMessageContent,
            time = System.currentTimeMillis(),
            attachment = imageUri?.toString() ?: pdfUri?.toString(),
            attachmentType = if (imageUri != null) AttachmentType.Image.ordinal
            else if (pdfUri != null) AttachmentType.Pdf.ordinal
            else null,
            contentIsPdf = false
        )
        val mId = messagesDao.addMessage(message)

        val attachmentText = if (imageUri != null) {
            val mlResult = ml.recognizeText(
                imageUri
            )
            if (mlResult is MLManager.MLResult.Success) {
                mlResult.text
            } else {
                messagesDao.deleteMessage(mId)
                return@withContext NetworkResult.Error.Attachment(mlResult.text)
            }
        } else {
            null
        }

        val attachmentContent = if (attachmentText != null) {
            val attachmentContent = buildString {
                appendLine()
                appendLine(PromptUtil.ATTACHMENT_TAG)
                appendLine(attachmentText)
                appendLine(PromptUtil.ATTACHMENT_TAG)
            }
            messagesDao.updateMessage(
                message.copy(
                    id = mId,
                    attachmentContent = attachmentContent
                )
            )
            attachmentContent
        } else {
            ""
        }

        try {
            val response = api.generateMessage(
                prompt = PalmeMessagePrompt(
                    MessagePrompt(
                        context = getSectionSystemPrompt(MessageSection.Tutor),
                        messages = chat?.messages?.map {
                            PalmMessage(it.content + it.attachmentContent)
                        }?.plus(PalmMessage(newMessageContent + attachmentContent)) ?: listOf(
                            PalmMessage(newMessageContent + attachmentContent)
                        )
                    )
                ),
                apiKey = apiKey
            )
            if (response.candidates.isEmpty()) {
                messagesDao.deleteMessage(mId)
                return@withContext NetworkResult.Error.Unknown(App.getString(R.string.unknown_error))
            }
            messagesDao.addMessage(
                Message(
                    chatId = cId,
                    type = MessageType.Bot.ordinal,
                    content = response.candidates.first().content,
                    time = System.currentTimeMillis()
                )
            )
            NetworkResult.Success
        } catch (e: IOException) {
            e.printStackTrace()
            messagesDao.deleteMessage(mId)
            NetworkResult.Error.Network(App.getString(R.string.no_internet))
        } catch (e: Exception) {
            e.printStackTrace()
            messagesDao.deleteMessage(mId)
            NetworkResult.Error.Unknown(App.getString(R.string.unknown_error))
        }
    }

    override fun getSectionChats(section: Int): Flow<List<ChatWithMessages>> {
        return messagesDao.allSectionChats(section = section)
            .map { chat ->
                latestChat = chat.firstOrNull()?.chat
                chat.map {
                    ChatWithMessages(
                        chat = it.chat,
                        // reverse messages to show latest message at the bottom in a reversed lazy column
                        messages = it.messages.sortedByDescending { message -> message.time }
                    )
                }
            }.flowOn(Dispatchers.IO)
    }

    override suspend fun clearChatContext() {
        withContext(Dispatchers.IO) {
            latestChat?.let {
                messagesDao.updateChat(
                    it.copy(
                        done = true
                    )
                )
            }
        }
    }

    override suspend fun resetAllChats(section: Int) {
        withContext(Dispatchers.IO) {
            messagesDao.deleteSectionChats(section)
        }
    }

    private fun getSectionSystemPrompt(section: MessageSection) =
        when (section) {
            MessageSection.Tutor -> PromptUtil.tutorSystemMessage
            MessageSection.Summarizer -> PromptUtil.summarizerSystemMessage
            MessageSection.Writer -> PromptUtil.writerSystemMessage
            MessageSection.Questions -> PromptUtil.questionGeneratorSystemMessage
        }

    private fun getPdfContentType(section: MessageSection) =
        when (section) {
            MessageSection.Summarizer -> App.getString(R.string.summary)
            MessageSection.Writer -> App.getString(R.string.essay)
            MessageSection.Questions -> App.getString(R.string.questions)
            else -> ""
        }

    override suspend fun writePdfFile(message: Message, uri: Uri) = withContext(Dispatchers.IO) {
        suspendCoroutine { continuation ->
            continuation.resume(
                pdf.writeTextToPdf(
                    message.content,
                    "${message.pdfContentType}.pdf",
                    uri
                )
            )
        }
    }

}