package com.jian.nemo.feature.user

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jian.nemo.feature.user.component.*
import com.jian.nemo.core.ui.component.animation.AnimatedWordBackground
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.activity.compose.BackHandler

import io.github.jan.supabase.compose.auth.composeAuth
import io.github.jan.supabase.compose.auth.composable.rememberSignInWithGoogle
import io.github.jan.supabase.compose.auth.composable.NativeSignInResult

/**
 * 包含动画背景和登录/注册 Tab 切换
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: (isNewUser: Boolean) -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    // [Fix] Enforce Brand Blue for header background to ensure vibration in Dark Mode
    // and fix "dirty gray-black" text issue by forcing White text on Blue background.
    val loginBackgroundColor = Color(0xFF0E68FF)
    val loginTextColor = Color.White

    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    val emailFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    val confirmPasswordFocusRequester = remember { FocusRequester() }

    val googleSignInState = viewModel.supabaseClient.composeAuth.rememberSignInWithGoogle(
        onResult = { result ->
            android.util.Log.d("GoogleLogin", "NativeSignInResult: ${result::class.simpleName}")
            when (result) {
                is NativeSignInResult.Success -> {
                    android.util.Log.d("GoogleLogin", "Google sign in success!")
                    Toast.makeText(context, "Google授权成功，正在同步登录状态...", Toast.LENGTH_SHORT).show()
                }
                is NativeSignInResult.Error -> {
                    android.util.Log.e("GoogleLogin", "Error: ${result.message}")
                    Toast.makeText(context, "Google登录失败: ${result.message}", Toast.LENGTH_LONG).show()
                }
                is NativeSignInResult.NetworkError -> {
                    Toast.makeText(context, "网络错误: ${result.message}", Toast.LENGTH_LONG).show()
                }
                is NativeSignInResult.ClosedByUser -> {
                    Toast.makeText(context, "用户取消登录", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    android.util.Log.d("GoogleLogin", "Other result: $result")
                }
            }
        },
        fallback = {
            android.util.Log.e("GoogleLogin", "Fallback called! Credential Manager failed or not supported.")
            Toast.makeText(context, "当前设备不支持原生 Google 登录", Toast.LENGTH_LONG).show()
        }
    )

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) {
            onLoginSuccess(uiState.user?.isNewUser == true)
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            viewModel.clearSuccessMessage()
        }
    }


    LaunchedEffect(Unit) {
        emailFocusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(loginBackgroundColor) // Changed to fixed Brand Blue
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f) // Adjust weight to give more space nicely
        ) {
            // 日语单词背景动画 (Keep as requested)
            AnimatedWordBackground(
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { coordinates ->
                        containerSize = coordinates.size
                    },
                containerSize = containerSize,
                contentColor = loginTextColor // [Fix] Force White text for floating words
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 64.dp, start = 32.dp, end = 32.dp), // Increased top padding
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = if (uiState.isLoginMode) "欢迎回来" else "创建账户",
                    fontSize = 32.sp, // Increased size
                    fontWeight = FontWeight.ExtraBold, // Increased weight
                    letterSpacing = (-1).sp, // Tighter spacing for modern look
                    color = loginTextColor // Changed to fixed White
                )

                Text(
                    text = if (uiState.isLoginMode) "我们想念您！登录以开始学习" else "加入我们,开始您的学习之旅",
                    fontSize = 14.sp, // Refined size
                    fontWeight = FontWeight.Medium,
                    color = loginTextColor.copy(alpha = 0.85f), // Softer White
                    modifier = Modifier.padding(top = 12.dp),
                    lineHeight = 20.sp
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(3.5f)
                .imePadding(),
            shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp), // 26dp Corner Radius
            colors = CardDefaults.cardColors(containerColor = surfaceColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp) // Added shadow for depth
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp), // Optimized padding
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = if (uiState.isLoginMode) "登录" else "注册",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = onSurfaceColor,
                    modifier = Modifier.padding(top = 40.dp, bottom = 24.dp)
                )

                // Tab Switcher with 26dp radius
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(26.dp) // [Fix] Reverted to 26dp as requested
                        )
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TabButton(
                        text = "登录",
                        isSelected = uiState.isLoginMode,
                        onClick = {
                            if (!uiState.isLoginMode) viewModel.toggleLoginMode()
                        }
                    )

                    TabButton(
                        text = "注册",
                        isSelected = !uiState.isLoginMode,
                        onClick = {
                            if (uiState.isLoginMode) viewModel.toggleLoginMode()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp)) // More breathing room

                AnimatedContent(
                    targetState = uiState.isLoginMode,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) + slideInVertically(animationSpec = tween(300)) { it / 10 } togetherWith
                        fadeOut(animationSpec = tween(200)) + slideOutVertically(animationSpec = tween(200)) { -it / 10 }
                    },
                    label = "AuthForm"
                ) { loginMode ->
                    if (loginMode) {
                        LoginForm(
                            email = uiState.email,
                            password = uiState.password,
                            isPasswordVisible = uiState.isPasswordVisible,
                            onEmailChange = viewModel::onEmailChanged,
                            onPasswordChange = viewModel::onPasswordChanged,
                            onTogglePasswordVisibility = viewModel::togglePasswordVisibility,
                            onLogin = viewModel::onLoginClicked,
                            isLoading = uiState.isLoading,
                            emailFocusRequester = emailFocusRequester,
                            passwordFocusRequester = passwordFocusRequester,
                            onForgotPasswordClick = { viewModel.showDialog(UserDialogType.RESET_PASSWORD) },
                            emailError = uiState.emailError,
                            passwordError = uiState.passwordError,
                            onGoogleLoginClick = {
                                googleSignInState.startFlow()
                            }
                        )
                    } else {
                        RegisterForm(
                            username = uiState.username,
                            email = uiState.email,
                            password = uiState.password,
                            confirmPassword = uiState.confirmPassword,
                            isPasswordVisible = uiState.isPasswordVisible,
                            isConfirmPasswordVisible = uiState.isConfirmPasswordVisible,
                            onUsernameChange = viewModel::onUsernameChanged,
                            onEmailChange = viewModel::onEmailChanged,
                            onPasswordChange = viewModel::onPasswordChanged,
                            onConfirmPasswordChange = viewModel::onConfirmPasswordChanged,
                            onTogglePasswordVisibility = viewModel::togglePasswordVisibility,
                            onToggleConfirmPasswordVisibility = viewModel::toggleConfirmPasswordVisibility,
                            onRegister = {
                                viewModel.onRegisterClicked {
                                    Toast.makeText(context, "两次输入的密码不一致", Toast.LENGTH_SHORT).show()
                                }
                            },
                            isLoading = uiState.isLoading,
                            usernameFocusRequester = emailFocusRequester,
                            passwordFocusRequester = passwordFocusRequester,
                            confirmPasswordFocusRequester = confirmPasswordFocusRequester,
                            usernameError = uiState.usernameError,
                            emailError = uiState.emailError,
                            passwordError = uiState.passwordError,
                            confirmPasswordError = uiState.confirmPasswordError
                        )
                    }
                }
            }
        }
    }

    // --- 阻塞式同步进度指示器已移除 ---

    if (uiState.activeDialog == UserDialogType.RESET_PASSWORD) {
        AccountResetPasswordDialog(
            userEmail = uiState.email,
            onDismiss = { viewModel.dismissDialog() },
            onSendOtp = { resetEmail, onSuccess, onError ->
                viewModel.sendPasswordResetOtp(resetEmail, onSuccess, onError)
            },
            onVerifyOtp = { resetEmail, token, onSuccess, onError ->
                viewModel.verifyPasswordResetOtp(resetEmail, token, onSuccess, onError)
            },
            onResetPassword = { newPassword, onSuccess, onError ->
                viewModel.completePasswordReset(
                    password = newPassword,
                    onSuccess = {
                        onSuccess()
                        viewModel.dismissDialog()
                    },
                    onError = onError
                )
            }
        )
    }
}

@Composable
private fun RowScope.TabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // [Fix] Enforce Brand Blue
    val brandBlue = Color(0xFF0E68FF)

    Button(
        onClick = onClick,
        modifier = Modifier.weight(1f).height(44.dp),
        shape = RoundedCornerShape(22.dp), // [Fix] Reverted to 22dp as requested
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) brandBlue else Color.Transparent, // [Fix] Force Brand Blue
            contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface // [Fix] Force White text
        ),
        contentPadding = PaddingValues(horizontal = 16.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

