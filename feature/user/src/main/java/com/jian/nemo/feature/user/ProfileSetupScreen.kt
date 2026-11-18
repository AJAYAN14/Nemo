package com.jian.nemo.feature.user

import android.widget.Toast
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jian.nemo.core.ui.component.avatar.AvatarEditDialog
import com.jian.nemo.core.ui.util.toUserMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    onSetupComplete: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var nickname by remember { mutableStateOf(uiState.user?.username ?: "") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.user?.username) {
        if (nickname.isEmpty() && !uiState.user?.username.isNullOrEmpty()) {
            nickname = uiState.user?.username ?: ""
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }
    LaunchedEffect(uiState.avatarError) {
        uiState.avatarError?.let { failure ->
            Toast.makeText(context, failure.toUserMessage(context), Toast.LENGTH_LONG).show()
            viewModel.clearAvatarError()
        }
    }
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.clearSuccessMessage()
        }
    }

    com.jian.nemo.feature.user.component.ProfileSetupContent(
        nickname = nickname,
        onNicknameChange = { nickname = it },
        avatarPath = uiState.avatarPath.takeIf { it.isNotEmpty() } ?: uiState.user?.avatarUrl,
        isGoogleOnlyUser = uiState.user?.isOnlyGoogleIdentity == true,
        password = password,
        onPasswordChange = { password = it },
        isPasswordVisible = isPasswordVisible,
        onTogglePasswordVisibility = { isPasswordVisible = !isPasswordVisible },
        isLoading = uiState.isLoading,
        onSkip = onSetupComplete,
        onStartClick = {
            viewModel.completeProfileSetup(
                nickname = nickname,
                password = password.takeIf { it.isNotBlank() },
                onSuccess = onSetupComplete
            )
        },
        onAvatarClick = { viewModel.showDialog(UserDialogType.UPDATE_AVATAR) }
    )

    if (uiState.activeDialog == UserDialogType.UPDATE_AVATAR) {
        AvatarEditDialog(
            currentAvatarPath = uiState.avatarPath.takeIf { it.isNotEmpty() } ?: uiState.user?.avatarUrl,
            username = nickname.takeIf { it.isNotBlank() } ?: stringResource(R.string.user_profile_setup_default_username),
            isProcessing = uiState.isAvatarProcessing,
            pendingCameraLaunchUri = uiState.pendingCameraLaunchUri,
            onDismiss = { viewModel.dismissDialog() },
            onGalleryImageSelected = { uriString ->
                viewModel.onGalleryAvatarSelected(uriString)
            },
            onGalleryPickerUnavailable = {
                viewModel.onGalleryPickerUnavailable()
            },
            onCameraCaptureClick = {
                viewModel.onCameraCaptureRequested()
            },
            onCameraLaunchConsumed = {
                viewModel.onCameraLaunchConsumed()
            },
            onCameraLaunchFailed = {
                viewModel.onCameraLaunchFailed()
            },
            onCameraCaptureResult = { success ->
                viewModel.onCameraCaptureResult(success)
            },
            onPresetSelected = { presetPath ->
                viewModel.onPresetAvatarSelected(presetPath)
            },
            onRemoveAvatar = {
                viewModel.onRemoveAvatarRequested()
            }
        )
    }
}
