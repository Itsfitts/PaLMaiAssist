package ai.assist.palmai.app.domain.model

import kotlinx.serialization.Serializable


@Serializable
data class PalmMessagesResponse(
    val candidates: List<PalmMessage>
)