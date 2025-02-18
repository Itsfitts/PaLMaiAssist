package ai.assist.palmai.app.domain.model

import kotlinx.serialization.Serializable


@Serializable
data class PalmeMessagePrompt(
    val prompt: MessagePrompt,
    val candidate_count: Int = 1
)