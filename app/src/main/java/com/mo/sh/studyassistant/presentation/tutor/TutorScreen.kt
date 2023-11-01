package ai.assist.palmai.app.presentation.tutor

import android.graphics.Color
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import ai.assist.palmai.app.R
import ai.assist.palmai.app.domain.model.MessageSection
import ai.assist.palmai.app.presentation.MainViewModel
import ai.assist.palmai.app.presentation.common_components.AttachmentType
import ai.assist.palmai.app.presentation.common_components.BaseChatSurface

@Composable
fun TutorScreen(
    sharedImageUri: Uri?,
    sharedText: String?,
    viewModel: MainViewModel
) {
    LaunchedEffect(Unit) {
        viewModel.updateSection(MessageSection.Tutor)
    }
    val context = LocalContext.current
    var text by rememberSaveable {
        mutableStateOf("")
    }
    var imageUri by remember {
        mutableStateOf<Uri?>(null)
    }
    var pdfUri by remember {
        mutableStateOf<Uri?>(null)
    }
    val chats by viewModel.chats.collectAsState()
    val networkState by viewModel.loadingState.collectAsState()
    val cropImageLauncher = rememberLauncherForActivityResult(CropImageContract()) {
        if (it.isSuccessful) {
            imageUri = it.uriContent
            pdfUri = null
        }
    }
    LaunchedEffect(Unit) {
        sharedImageUri?.let {
            cropImageLauncher.launch(
                CropImageContractOptions(
                    uri = it,
                    cropImageOptions = CropImageOptions(
                        guidelinesColor = Color.GREEN,
                        borderLineColor = Color.GREEN,
                        borderCornerColor = Color.GREEN,
                        guidelinesThickness = 0.8f,
                        toolbarColor = Color.BLACK
                    )
                )
            )
        }
        sharedText?.let {
            text = it
        }
    }

    BaseChatSurface(
        textFieldText = text,
        title = stringResource(R.string.personal_tutor_title),
        selectedImageUri = imageUri,
        selectedPdfUri = pdfUri,
        chats = chats,
        loadingState = networkState,
        visibleAttachmentItems = listOf(
            AttachmentType.Image,
        ),
        onWritePdf = { _, _ -> },
        onReset = {
            viewModel.resetChat()
        },
        onDeleteAll = {
            viewModel.clearAllChats()
        },
        onImageSelected = {
            it?.let { uri ->
                cropImageLauncher.launch(
                    CropImageContractOptions(
                        uri = uri,
                        cropImageOptions = CropImageOptions(
                            guidelinesColor = Color.GREEN,
                            borderLineColor = Color.GREEN,
                            borderCornerColor = Color.GREEN,
                            guidelinesThickness = 0.8f,
                            toolbarColor = Color.BLACK
                        )
                    )
                )
            } ?: run {
                imageUri = null
            }
        },
        onPdfSelected = {},
        onTextChanged = { text = it },
        textFieldHint = stringResource(R.string.tutor_chat_hint),
        onSubmit = {
            if (it.isBlank()) {
                Toast.makeText(
                    context,
                    context.getString(R.string.text_cant_be_empty),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                viewModel.sendMessage(
                    it,
                    if (
                        chats.isEmpty() ||
                        chats.lastOrNull()?.chat?.done == true
                    ) null else chats.last(),
                    imageUri,
                    pdfUri
                )
                imageUri = null
                pdfUri = null
            }
            text = ""
        }
    )
}