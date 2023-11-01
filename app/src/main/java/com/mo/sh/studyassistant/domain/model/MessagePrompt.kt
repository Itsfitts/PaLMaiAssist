package ai.assist.palmai.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class MessagePrompt(
    val context: String,
    val messages: List<PalmMessage>
)
