package com.jian.nemo.feature.user

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.hilt.navigation.compose.hiltViewModel
import com.jian.nemo.core.designsystem.shape.Capsule
import com.jian.nemo.core.designsystem.shape.RoundedRectangle
import com.jian.nemo.core.designsystem.theme.IosColors
import com.jian.nemo.core.designsystem.theme.screenBackground
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.ui.component.AvatarImage
import com.jian.nemo.core.ui.component.avatar.AvatarEditDialog
import com.jian.nemo.core.ui.component.common.CommonHeader
import com.jian.nemo.core.ui.component.liquid.ColoredLiquidGlassButton
import com.jian.nemo.core.ui.modifier.softCardShadow
import com.jian.nemo.feature.user.component.*
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

private const val ANIMATION_DURATION = 200
private const val PRESS_SCALE = 0.97f

val LocalSettingIconStyle = compositionLocalOf { SettingIconStyle.MD3 }

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

    val avatarPath = uiState.avatarPath

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

    LaunchedEffect(uiState.isLoggedIn, uiState.isLoading, uiState.isAuthChecked) {
        if (uiState.isAuthChecked && !uiState.isLoggedIn && !uiState.isLoading) {
            onLogoutSuccess()
        }
    }

    val backgroundColor = MaterialTheme.colorScheme.screenBackground
    val cardBg = if (useDarkTheme) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surface

    val backdrop = rememberLayerBackdrop()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    CompositionLocalProvider(LocalSettingIconStyle provides uiState.settingIconStyle) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop)
                    .verticalScroll(scrollState)
                    .padding(
                        top = statusBarTop + 60.dp + 16.dp,
                        bottom = navBarBottom + 24.dp
                    )
            ) {
                    ProfileHeaderSection(
                        uiState = uiState,
                        avatarPath = avatarPath,
                        onEditAvatar = { viewModel.showDialog(UserDialogType.UPDATE_AVATAR) }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        // Group 1: Profile Settings
                        PremiumSettingsGroup(
                            title = stringResource(R.string.user_account_title_profile_section),
                            cardBg = cardBg
                        ) {
                            PremiumSettingsItem(
                                icon = Icons.Rounded.Badge,
                                iconTint = if (useDarkTheme) IosColors.BlueDark else IosColors.Blue,
                                title = stringResource(R.string.user_account_btn_change_username),
                                onClick = { viewModel.showDialog(UserDialogType.UPDATE_USERNAME) }
                            )
                            if (uiState.user?.isOnlyGoogleIdentity != true) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 74.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                    thickness = 0.5.dp
                                )
                                PremiumSettingsItem(
                                    icon = Icons.Rounded.Mail,
                                    iconTint = if (useDarkTheme) IosColors.GreenDark else IosColors.Green,
                                    title = stringResource(R.string.user_account_btn_change_email),
                                    onClick = { viewModel.showDialog(UserDialogType.UPDATE_EMAIL) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Group 2: Security Settings
                        PremiumSettingsGroup(
                            title = stringResource(R.string.user_account_title_security_section),
                            cardBg = cardBg
                        ) {
                            PremiumSettingsItem(
                                icon = Icons.Rounded.LockReset,
                                iconTint = if (useDarkTheme) IosColors.OrangeDark else IosColors.Orange,
                                title = stringResource(R.string.user_account_btn_reset_password),
                                onClick = { viewModel.showDialog(UserDialogType.RESET_PASSWORD) }
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Group 3: Danger Zone
                        PremiumSettingsGroup(
                            title = stringResource(R.string.user_account_title_danger_section),
                            cardBg = cardBg
                        ) {
                            PremiumSettingsItem(
                                icon = Icons.Rounded.DeleteForever,
                                iconTint = if (useDarkTheme) IosColors.RedDark else IosColors.Red,
                                title = stringResource(R.string.user_account_btn_delete_account),
                                titleColor = MaterialTheme.colorScheme.error,
                                onClick = { viewModel.showDialog(UserDialogType.DELETE_ACCOUNT) }
                            )
                        }

                        Spacer(modifier = Modifier.height(36.dp))

                        ColoredLiquidGlassButton(
                            onClick = { viewModel.showDialog(UserDialogType.LOGOUT_CONFIRM) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            tint = if (useDarkTheme) IosColors.RedDark else IosColors.Red,
                            backdrop = backdrop,
                            shape = Capsule()
                        ) {
                            Text(
                                text = stringResource(R.string.user_account_btn_logout),
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(48.dp))
                    }
                }

                // 2. 悬浮顶栏与液态玻璃按钮（带柔和垂直渐变保护遮罩，读取 backdrop）
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    backgroundColor.copy(alpha = 0.95f),
                                    backgroundColor.copy(alpha = 0.75f),
                                    backgroundColor.copy(alpha = 0.35f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(bottom = 8.dp)
                ) {
                    CommonHeader(
                        title = stringResource(R.string.user_account_title_screen),
                        onBack = onNavigateBack,
                        backgroundColor = Color.Transparent,
                        backdrop = backdrop,
                        backButtonSize = 48.dp
                    )
                }
            }
        }

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
                username = uiState.user?.username ?: stringResource(R.string.user_account_default_username),
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
                        onError = { }
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
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            contentAlignment = Alignment.BottomEnd,
            modifier = Modifier.clickable { onEditAvatar() }
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
                modifier = Modifier.size(80.dp)
            ) {
                Box(modifier = Modifier.padding(4.dp)) {
                    AvatarImage(
                        username = uiState.user?.username ?: stringResource(R.string.user_account_default_username),
                        avatarPath = avatarPath.takeIf { it.isNotEmpty() },
                        size = 72.dp,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(24.dp)
                    .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = stringResource(R.string.user_account_desc_edit_avatar),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(5.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(20.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = uiState.user?.username ?: stringResource(R.string.user_account_label_not_logged_in),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .basicMarquee(iterations = Int.MAX_VALUE)
                )

                if (uiState.user?.isOnlyGoogleIdentity == true) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = GoogleIcon,
                        contentDescription = stringResource(R.string.user_account_desc_google_account),
                        tint = Color.Unspecified,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Text(
                text = uiState.user?.email ?: stringResource(R.string.user_account_label_not_logged_in),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 4.dp),
                maxLines = 1
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
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 0.dp, vertical = 8.dp)
        )

        val useDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f

        Card(
            shape = RoundedRectangle(24.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .softCardShadow(borderRadius = 24.dp, isDark = useDarkTheme)
        ) {
            Column(
                modifier = Modifier.padding(vertical = 4.dp)
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
    val iconStyle = LocalSettingIconStyle.current
    val isSoftTranslucent = (iconStyle == SettingIconStyle.MD3)
    val containerBgColor = if (isSoftTranslucent) iconTint.copy(alpha = 0.15f) else iconTint
    val actualTint = if (isSoftTranslucent) iconTint else Color.White

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) PRESS_SCALE else 1f,
        label = "settingsItemScale",
        animationSpec = tween(ANIMATION_DURATION)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(containerBgColor, RoundedRectangle(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = actualTint,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

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
