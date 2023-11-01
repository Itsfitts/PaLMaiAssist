package ai.assist.palmai.app.domain.model

import kotlinx.serialization.Serializable


@Serializable
data class PalmMessage(
    val content: String
)
