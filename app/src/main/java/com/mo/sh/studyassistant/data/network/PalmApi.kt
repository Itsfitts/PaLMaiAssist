package ai.assist.palmai.app.data.network

import ai.assist.palmai.app.domain.model.PalmMessagesResponse
import ai.assist.palmai.app.domain.model.PalmTextPrompt
import ai.assist.palmai.app.domain.model.PalmTextResponse
import ai.assist.palmai.app.domain.model.PalmeMessagePrompt
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody


class PalmApi(
    private val client: HttpClient
) {

    suspend fun generateMessage(
         prompt: PalmeMessagePrompt,
         apiKey: String
    ): PalmMessagesResponse {
        return client.post(
            NetworkConstants.BASE_URL +
                    NetworkConstants.PALM_MESSAGES_MODEL +
                    apiKey
        ) {
            headers {
                append("Content-Type", "application/json")
            }
            setBody(prompt)
        }.body()
    }

    suspend fun generateText(
        prompt: PalmTextPrompt,
        apiKey: String
    ): PalmTextResponse {
        return client.post(
            NetworkConstants.BASE_URL +
                    NetworkConstants.PALM_TEXT_MODEL +
                    apiKey
        ) {
            headers {
                append("Content-Type", "application/json")
            }
            setBody(prompt)
        }.body()
    }

}