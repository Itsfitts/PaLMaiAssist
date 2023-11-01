package ai.assist.palmai.app.presentation.common_components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class AttachmentItem(
    @DrawableRes val icon: Int,
    @StringRes val text: Int,
    val type: AttachmentType
)
