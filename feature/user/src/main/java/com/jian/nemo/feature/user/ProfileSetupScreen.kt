package com.jian.nemo.feature.user

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jian.nemo.core.ui.component.AvatarImage
import com.jian.nemo.core.ui.component.avatar.AvatarEditDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    onSetupComplete: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    var showAvatarDialog by remember { mutableStateOf(false) }
    var nickname by remember { mutableStateOf(uiState.user?.username ?: "") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    // Sync state to local var if it arrives later
    LaunchedEffect(uiState.user?.username) {
        if (nickname.isEmpty() && !uiState.user?.username.isNullOrEmpty()) {
            nickname = uiState.user?.username ?: ""
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
        onAvatarClick = { showAvatarDialog = true }
    )

    if (showAvatarDialog) {
        AvatarEditDialog(
            currentAvatarPath = uiState.avatarPath.takeIf { it.isNotEmpty() } ?: uiState.user?.avatarUrl,
            username = nickname.takeIf { it.isNotBlank() } ?: "用户",
            onDismiss = { showAvatarDialog = false },
            onAvatarChanged = { newAvatarPath ->
                showAvatarDialog = false
                viewModel.onAvatarChanged(newAvatarPath)
            }
        )
    }
}
