package com.jian.nemo.feature.user

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.ui.graphics.vector.ImageVector
import com.jian.nemo.core.designsystem.theme.screenBackground
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import com.jian.nemo.core.ui.component.AvatarImage
import com.jian.nemo.core.ui.component.avatar.AvatarEditDialog
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.luminance
import com.jian.nemo.feature.user.component.*
import com.jian.nemo.feature.user.component.*
import androidx.compose.animation.*
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.jian.nemo.core.ui.component.animation.NemoChasingDotsLoader
import com.jian.nemo.core.ui.component.common.CommonHeader
import com.jian.nemo.core.ui.component.liquid.LiquidButton
import com.jian.nemo.core.ui.modifier.softCardShadow
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountManagementScreen(
    onNavigateBack: () -> Unit,

    onLogoutSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    val useDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // 头像相关状态
    val avatarPath = uiState.avatarPath

    val scope = rememberCoroutineScope()
    


    // Handle Toasts
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.clearSuccessMessage()
        }
    }

    // Handle Logout Redirect
    LaunchedEffect(uiState.isLoggedIn, uiState.isLoading, uiState.isAuthChecked) {
        if (uiState.isAuthChecked && !uiState.isLoggedIn && !uiState.isLoading) {
            onLogoutSuccess()
        }
    }

    // Colors
    val backgroundColor = MaterialTheme.colorScheme.screenBackground
    // Premium Card Colors
    val cardBg = if (useDarkTheme) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surface

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. Common Header
            CommonHeader(
                title = "账户管理",
                onBack = onNavigateBack,
                backgroundColor = backgroundColor
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
            ) {
                // 2. Profile Header (Horizontal)
                ProfileHeaderSection(
                    uiState = uiState,
                    avatarPath = avatarPath,
                    onEditAvatar = { viewModel.showDialog(UserDialogType.UPDATE_AVATAR) }
                )

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Settings Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                // Group 1: Profile Settings
                PremiumSettingsGroup(title = "个人信息", cardBg = cardBg) {
                    PremiumSettingsItem(
                        icon = Icons.Rounded.Badge,
                        iconTint = Color(0xFF007AFF), // Blue
                        title = "修改用户名",
                        onClick = { viewModel.showDialog(UserDialogType.UPDATE_USERNAME) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    PremiumSettingsItem(
                        icon = Icons.Rounded.MailOutline,
                        iconTint = Color(0xFF34C759), // Green
                        title = "修改邮箱",
                        onClick = { viewModel.showDialog(UserDialogType.UPDATE_EMAIL) }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Group 2: CheckIn & Privacy (Security)
                PremiumSettingsGroup(title = "安全", cardBg = cardBg) {
                     PremiumSettingsItem(
                        icon = Icons.Rounded.LockReset,
                        iconTint = Color(0xFFFF9500), // Orange
                        title = "重置密码",
                        onClick = { viewModel.showDialog(UserDialogType.RESET_PASSWORD) }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))





                // Group 4: Danger Zone
                PremiumSettingsGroup(title = "危险区域", cardBg = cardBg) {
                    PremiumSettingsItem(
                        icon = Icons.Rounded.DeleteForever,
                        iconTint = MaterialTheme.colorScheme.error,
                        title = "注销账户",
                         titleColor = MaterialTheme.colorScheme.error,
                        onClick = { viewModel.showDialog(UserDialogType.DELETE_ACCOUNT) }
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))

                // Liquid Logout Button
                LiquidButton(
                    onClick = { viewModel.showDialog(UserDialogType.LOGOUT_CONFIRM) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    backgroundColor = MaterialTheme.colorScheme.error,
                    shape = CircleShape,
                    elevation = 6.dp
                ) {
                    Text(
                        text = "退出登录",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onError
                    )
                }

                 Spacer(modifier = Modifier.height(64.dp))
            }

        }
    }
    }

    // Dialogs Management
    when (uiState.activeDialog) {
        UserDialogType.RESET_PASSWORD -> {
            AccountResetPasswordDialog(
                userEmail = uiState.user?.email ?: "",
                onDismiss = { viewModel.dismissDialog() },
                onSendOtp = { email, onSuccess, onError -> viewModel.sendPasswordResetOtp(email, onSuccess, onError) },
                onVerifyOtp = { email, token, onSuccess, onError -> viewModel.verifyPasswordResetOtp(email, token, onSuccess, onError) },
                onResetPassword = { password, onSuccess, onError -> viewModel.completePasswordReset(password, onSuccess, onError) },
                useDarkTheme = useDarkTheme
            )
        }
        UserDialogType.UPDATE_USERNAME -> {
            AccountUpdateUsernameDialog(
                currentUsername = uiState.user?.username ?: "",
                onDismiss = { viewModel.dismissDialog() },
                onUpdateUsername = { newUsername, onSuccess, onError ->
                    viewModel.updateUsername(newUsername = newUsername, onSuccess = onSuccess, onError = onError)
                },
                useDarkTheme = useDarkTheme
            )
        }
        UserDialogType.UPDATE_EMAIL -> {
            AccountUpdateEmailDialog(
                currentEmail = uiState.user?.email ?: "",
                onDismiss = { viewModel.dismissDialog() },
                onUpdateEmail = { newEmail, onSuccess, onError -> viewModel.updateEmail(newEmail, onSuccess, onError) },
                onVerifyEmailUpdate = { email, code, onSuccess, onError -> viewModel.verifyEmailUpdate(email, code, onSuccess = { viewModel.dismissDialog(); onSuccess() }, onError = onError) },
                useDarkTheme = useDarkTheme
            )
        }
        UserDialogType.UPDATE_AVATAR -> {
            AvatarEditDialog(
                currentAvatarPath = avatarPath.takeIf { it.isNotEmpty() },
                username = uiState.user?.username ?: "用户",
                onDismiss = { viewModel.dismissDialog() },
                onAvatarChanged = { newAvatarPath ->
                    viewModel.onAvatarChanged(newAvatarPath)
                }
            )
        }
        UserDialogType.DELETE_ACCOUNT -> {
            DeleteAccountDialog(
                isOnlyGoogleIdentity = uiState.user?.isOnlyGoogleIdentity == true,
                userEmail = uiState.user?.email ?: "",
                onDismiss = { viewModel.dismissDialog() },
                onConfirmDelete = { password ->
                    viewModel.deleteAccount(
                        password = password,
                        onSuccess = { viewModel.dismissDialog() },
                        onError = { /* 对话框保持打开，错误信息通过 Toast 展示 */ }
                    )
                },
                useDarkTheme = useDarkTheme
            )
        }

        UserDialogType.LOGOUT_CONFIRM -> {
            LogoutWarningDialog(
                isLoading = uiState.isLoading,
                onDismiss = { viewModel.dismissDialog() },
                onConfirmLogout = {
                    viewModel.logout()
                    viewModel.dismissDialog()
                },
                useDarkTheme = useDarkTheme
            )
        }


        else -> {}
    }

}


@Composable
fun ProfileHeaderSection(
    uiState: AuthUiState,
    avatarPath: String,
    onEditAvatar: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp), // Add padding
        verticalAlignment = Alignment.CenterVertically
    ) {
         // User Avatar with Ring
         Box(
            contentAlignment = Alignment.BottomEnd,
            modifier = Modifier.clickable { onEditAvatar() }
         ) {
             Surface(
                 shape = CircleShape,
                 color = MaterialTheme.colorScheme.surface,
                 // Flat: No elevation, add border
                 border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
                 modifier = Modifier.size(80.dp) // Slightly smaller for horizontal layout
             ) {
                 Box(modifier = Modifier.padding(4.dp)) {
                    AvatarImage(
                        username = uiState.user?.username ?: "用户",
                        avatarPath = avatarPath.takeIf { it.isNotEmpty() },
                        size = 72.dp,
                        modifier = Modifier.fillMaxSize()
                    )
                 }
             }

             // Edit Badge
             Surface(
                 shape = CircleShape,
                 color = MaterialTheme.colorScheme.primary,
                 modifier = Modifier.size(24.dp).border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
             ) {
                 Icon(
                     imageVector = Icons.Rounded.Edit,
                     contentDescription = "Edit",
                     tint = MaterialTheme.colorScheme.onPrimary,
                     modifier = Modifier.padding(5.dp)
                 )
             }
         }

         Spacer(modifier = Modifier.width(20.dp))

         // Text Info
         Column(
             modifier = Modifier.weight(1f)
         ) {
             // Username
             Text(
                 text = uiState.user?.username ?: "未登录",
                 style = MaterialTheme.typography.headlineSmall.copy(
                     fontWeight = FontWeight.Bold
                 ),
                 color = MaterialTheme.colorScheme.onSurface
             )

             // Email
             Text(
                 text = uiState.user?.email ?: "未登录",
                 style = MaterialTheme.typography.bodyMedium,
                 color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                 modifier = Modifier.padding(top = 4.dp)
             )
         }
    }
}

@Composable
fun PremiumSettingsGroup(
    title: String,
    cardBg: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
        )

        val useDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f

        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
            modifier = Modifier
                .fillMaxWidth()
                .softCardShadow(borderRadius = 26.dp, isDark = useDarkTheme)
        ) {
            Column(
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
fun PremiumSettingsItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    subtitle: String? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon Container
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = iconTint.copy(alpha = 0.1f),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Text
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = titleColor
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Trailing Content (Chevron or Custom)
        if (trailingContent != null) {
            trailingContent()
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

