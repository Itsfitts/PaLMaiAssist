package ai.assist.palmai.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class PalmTextResponse(
    val candidates: List<PalmOutputText>
)
