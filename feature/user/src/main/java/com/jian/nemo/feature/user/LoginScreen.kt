package com.jian.nemo.feature.user

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.jian.nemo.core.ui.component.liquid.ColoredLiquidGlassButton
import com.jian.nemo.core.ui.component.liquid.FrostedLiquidGlassButton
import com.jian.nemo.core.ui.modifier.softCardShadow
import com.jian.nemo.feature.user.component.*
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import io.github.jan.supabase.compose.auth.composeAuth
import io.github.jan.supabase.compose.auth.composable.rememberSignInWithGoogle
import io.github.jan.supabase.compose.auth.composable.NativeSignInResult

/**
 * 页面展示阶段枚举（两段式流转：欢迎封面 -> 登录/注册表单）
 */
enum class AuthStage {
    WELCOME,
    LOGIN,
    REGISTER
}

/**
 * 全新纯 Jetpack Compose (JC) 登录与注册主界面
 * 无论处于欢迎封面还是登录/注册表单，内容统一放到底部（Bottom-Pinned，贴底对齐）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit = {},
    onLoginSuccess: (isNewUser: Boolean) -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val backdrop = rememberLayerBackdrop()

    var stage by remember { mutableStateOf(AuthStage.WELCOME) }
    var focusedField by remember { mutableStateOf<FocusedAuthField?>(null) }
    var showRegisterCaptchaDialog by remember { mutableStateOf(false) }
    var loginFailedAttempts by remember { mutableIntStateOf(0) }
    var showLoginCaptchaDialog by remember { mutableStateOf(false) }

    LaunchedEffect(stage) {
        focusedField = null
    }

    // 焦点管理
    val emailFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    val registerUsernameRequester = remember { FocusRequester() }
    val registerPasswordRequester = remember { FocusRequester() }
    val registerConfirmPasswordRequester = remember { FocusRequester() }

    // 主题色彩体系（严格对齐 app.tsx 规范）
    val pageBgColor = if (isDark) Color(0xFF0D0C11) else Color(0xFFF7F9FC)
    val titleTextColor = if (isDark) Color.White else Color(0xFF0F172A)
    val descTextColor = if (isDark) Color.White.copy(alpha = 0.60f) else Color(0xFF64748B)

    val primaryBtnBg = if (isDark) Color(0xFFF4F4F6) else Color(0xFF0F172A)
    val primaryBtnText = if (isDark) Color(0xFF0D0C11) else Color.White

    val secondaryBtnBg = if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFF0F172A).copy(alpha = 0.06f)
    val secondaryBtnText = if (isDark) Color.White.copy(alpha = 0.90f) else Color(0xFF0F172A)

    val capsuleBg = if (isDark) Color.White.copy(alpha = 0.10f) else Color(0xFF0F172A).copy(alpha = 0.08f)
    val capsuleText = if (isDark) Color(0xFF7DD3FC) else Color(0xFF2563EB)

    val backBtnBg = if (isDark) Color.White.copy(alpha = 0.10f) else Color(0xFF0F172A).copy(alpha = 0.08f)
    val backBtnTint = if (isDark) Color.White else Color(0xFF0F172A)

    // Google 快捷登录鉴权通道
    val googleSignInState = viewModel.supabaseClient.composeAuth.rememberSignInWithGoogle(
        onResult = { result ->
            when (result) {
                is NativeSignInResult.Success -> {
                    Toast.makeText(context, "Google授权成功，正在同步登录状态...", Toast.LENGTH_SHORT).show()
                }
                is NativeSignInResult.Error -> {
                    Toast.makeText(context, "Google登录失败: ${result.message}", Toast.LENGTH_LONG).show()
                }
                is NativeSignInResult.NetworkError -> {
                    Toast.makeText(context, "网络错误: ${result.message}", Toast.LENGTH_LONG).show()
                }
                is NativeSignInResult.ClosedByUser -> {
                    Toast.makeText(context, "用户取消登录", Toast.LENGTH_SHORT).show()
                }
                else -> Unit
            }
        },
        fallback = {
            Toast.makeText(context, "当前设备不支持原生 Google 登录", Toast.LENGTH_LONG).show()
        }
    )

    // 监听登录成功
    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) {
            loginFailedAttempts = 0
            onLoginSuccess(uiState.user?.isNewUser == true)
        }
    }

    // 错误与成功提示
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            if (stage == AuthStage.LOGIN) {
                loginFailedAttempts++
            }
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

    // 表单模式下拦截系统返回键，退回欢迎封面
    BackHandler(enabled = stage != AuthStage.WELCOME) {
        stage = AuthStage.WELCOME
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBgColor)
    ) {
        val screenHeight = maxHeight
        val density = LocalDensity.current
        val imeInsets = WindowInsets.ime
        val imeBottomDp = with(density) { imeInsets.getBottom(density).toDp() }

        // 仅在输入框获得焦点且软键盘弹出时，按当前选中输入框计算精确最小抬升量
        val targetLift = if (imeBottomDp > 0.dp) {
            when (focusedField) {
                FocusedAuthField.LOGIN_EMAIL -> (imeBottomDp + 16.dp - 280.dp).coerceAtLeast(0.dp)
                FocusedAuthField.LOGIN_PASSWORD -> (imeBottomDp + 16.dp - 212.dp).coerceAtLeast(0.dp)
                FocusedAuthField.REGISTER_USERNAME -> 0.dp
                FocusedAuthField.REGISTER_EMAIL -> 0.dp
                FocusedAuthField.REGISTER_PASSWORD -> (imeBottomDp + 16.dp - 266.dp).coerceAtLeast(0.dp)
                FocusedAuthField.REGISTER_CONFIRM_PASSWORD -> (imeBottomDp + 16.dp - 200.dp).coerceAtLeast(0.dp)
                null -> 0.dp
            }
        } else {
            0.dp
        }

        val animatedLift by animateDpAsState(
            targetValue = targetLift,
            animationSpec = tween(durationMillis = 250),
            label = "ImeFocusedLift"
        )

        // 顶部假名天幕随底部表单内容高度动态自适应延伸，彻底消除中间空白断层
        val targetWaveHeight = when (stage) {
            AuthStage.WELCOME -> screenHeight * 0.70f
            AuthStage.LOGIN -> (screenHeight - 310.dp).coerceAtLeast(360.dp)
            AuthStage.REGISTER -> (screenHeight - 390.dp).coerceAtLeast(300.dp)
        }
        val waveHeight by animateDpAsState(
            targetValue = targetWaveHeight,
            animationSpec = tween(durationMillis = 400),
            label = "WaveHeightAnimation"
        )

        // 1. 底层：纯 Jetpack Compose Canvas 原生假名天幕（绑定 backdrop 透镜采样源）
        KanaWaveBackground(
            isDark = isDark,
            modifier = Modifier
                .fillMaxWidth()
                .height(waveHeight)
                .align(Alignment.TopCenter)
                .layerBackdrop(backdrop)
        )

        // 2. 顶部操作栏：实验室磨砂带底色液态玻璃返回按钮（48dp 外径 + 30dp 图标，注入 backdrop 透镜折射，置顶 zIndex(10f)）
        if (stage != AuthStage.WELCOME) {
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .align(Alignment.TopStart)
                    .zIndex(10f)
            ) {
                FrostedLiquidGlassButton(
                    onClick = { stage = AuthStage.WELCOME },
                    backdrop = backdrop,
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                            contentDescription = "返回封面",
                            tint = backBtnTint,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }
        }

        // 3. 核心主内容区：统一彻底放到底部（Bottom-Pinned，贴底对齐）
        // 彻底移除外层全局 .imePadding()，由当前激活输入框焦点驱动最小抬升，下方按钮自然隐于键盘后！
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.Start
        ) {
            // 顶部弹性占位：将下方的一切内容稳稳推到屏幕最底部！
            Spacer(modifier = Modifier.weight(1f))

            // 动态切换核心内容区域（欢迎封面 vs 登录表单 vs 注册表单）
            AnimatedContent(
                targetState = stage,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) + slideInVertically(animationSpec = tween(300)) { it / 10 } togetherWith
                    fadeOut(animationSpec = tween(200)) + slideOutVertically(animationSpec = tween(200)) { -it / 10 }
                },
                label = "AuthStageAnimation",
                modifier = Modifier.fillMaxWidth()
            ) { currentStage ->
                when (currentStage) {
                    AuthStage.WELCOME -> {
                        // 【欢迎封面视图】：标语与按钮放到底部
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 28.dp)
                                .padding(bottom = 24.dp)
                        ) {
                            Text(
                                text = "在流动之间，\n精准掌握日语。",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = titleTextColor,
                                lineHeight = 40.sp,
                                letterSpacing = (-0.5).sp
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "Nemo 将沉浸式语言美学与 FSRS 科学记忆算法融合，助您轻松攻克假名、词汇、语法与动词变形。",
                                fontSize = 14.sp,
                                color = descTextColor,
                                lineHeight = 22.sp
                            )

                            Spacer(modifier = Modifier.height(28.dp))

                            // 主操作大按钮：有颜色选用「彩色液态玻璃按钮」
                            val welcomePrimaryTint = if (isDark) Color(0xFF2563EB) else Color(0xFF0E68FF)
                            ColoredLiquidGlassButton(
                                onClick = {
                                    stage = AuthStage.LOGIN
                                    if (!uiState.isLoginMode) viewModel.toggleLoginMode()
                                },
                                backdrop = backdrop,
                                tint = welcomePrimaryTint,
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "登录账号",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Rounded.ChevronRight,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // 次操作按钮：没颜色选用「带底色磨砂液态玻璃按钮」
                            FrostedLiquidGlassButton(
                                onClick = {
                                    stage = AuthStage.REGISTER
                                    if (uiState.isLoginMode) viewModel.toggleLoginMode()
                                },
                                backdrop = backdrop,
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "注册新账号",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = secondaryBtnText
                                    )
                                }
                            }
                        }
                    }

                    AuthStage.LOGIN -> {
                        // 【登录表单视图】：彻底放到底部，包含横向双按钮【忘记密码】与【立即登录 >】
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 28.dp)
                                .padding(bottom = 24.dp)
                        ) {
                            // 标题栏：左侧主副标题，右侧红框区域【去注册 >】切换胶囊（用户明确要求：去注册按钮是蓝色的）
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "欢迎回来",
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = titleTextColor,
                                        letterSpacing = (-0.5).sp
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = "使用邮箱密码或 Google 账号继续",
                                        fontSize = 13.sp,
                                        color = descTextColor
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // 用户红框标注区域：【去注册】切换胶囊（带底色磨砂液态玻璃，仅保留文字）
                                FrostedLiquidGlassButton(
                                    onClick = {
                                        stage = AuthStage.REGISTER
                                        if (uiState.isLoginMode) viewModel.toggleLoginMode()
                                    },
                                    backdrop = backdrop,
                                    shape = RoundedCornerShape(50),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp)
                                ) {
                                    Text(
                                        text = "去注册",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = capsuleText
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            AppLoginForm(
                                email = uiState.email,
                                password = uiState.password,
                                isPasswordVisible = uiState.isPasswordVisible,
                                isDark = isDark,
                                onEmailChange = viewModel::onEmailChanged,
                                onPasswordChange = viewModel::onPasswordChanged,
                                onTogglePasswordVisibility = viewModel::togglePasswordVisibility,
                                onLogin = {
                                    if (loginFailedAttempts >= 3) {
                                        showLoginCaptchaDialog = true
                                    } else {
                                        viewModel.onLoginClicked()
                                    }
                                },
                                isLoading = uiState.isLoading,
                                emailFocusRequester = emailFocusRequester,
                                passwordFocusRequester = passwordFocusRequester,
                                onForgotPasswordClick = {
                                    viewModel.showDialog(UserDialogType.RESET_PASSWORD)
                                },
                                emailError = uiState.emailError,
                                passwordError = uiState.passwordError,
                                onGoogleLoginClick = {
                                    googleSignInState.startFlow()
                                },
                                onFocusFieldChange = { focusedField = it },
                                backdrop = backdrop
                            )
                        }
                    }

                    AuthStage.REGISTER -> {
                        // 【注册表单视图】：彻底放到底部，包含四个输入框与全宽【立即注册 >】
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 28.dp)
                                .padding(bottom = 24.dp)
                        ) {
                            // 标题栏：左侧主副标题，右侧【去登录 >】切换胶囊（同款蓝色彩色液态玻璃）
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "开启旅程",
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = titleTextColor,
                                        letterSpacing = (-0.5).sp
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = "开启您的沉浸式日语假名习得之旅",
                                        fontSize = 13.sp,
                                        color = descTextColor
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // 标题右侧：【去登录】切换胶囊（同款带底色磨砂液态玻璃，仅保留文字）
                                FrostedLiquidGlassButton(
                                    onClick = {
                                        stage = AuthStage.LOGIN
                                        if (!uiState.isLoginMode) viewModel.toggleLoginMode()
                                    },
                                    backdrop = backdrop,
                                    shape = RoundedCornerShape(50),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp)
                                ) {
                                    Text(
                                        text = "去登录",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = capsuleText
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            AppRegisterForm(
                                username = uiState.username,
                                email = uiState.email,
                                password = uiState.password,
                                confirmPassword = uiState.confirmPassword,
                                isPasswordVisible = uiState.isPasswordVisible,
                                isConfirmPasswordVisible = uiState.isConfirmPasswordVisible,
                                isDark = isDark,
                                onUsernameChange = viewModel::onUsernameChanged,
                                onEmailChange = viewModel::onEmailChanged,
                                onPasswordChange = viewModel::onPasswordChanged,
                                onConfirmPasswordChange = viewModel::onConfirmPasswordChanged,
                                onTogglePasswordVisibility = viewModel::togglePasswordVisibility,
                                onToggleConfirmPasswordVisibility = viewModel::toggleConfirmPasswordVisibility,
                                onRegister = {
                                    if (uiState.email.isBlank()) {
                                        Toast.makeText(context, "请输入电子邮箱地址", Toast.LENGTH_SHORT).show()
                                        return@AppRegisterForm
                                    }
                                    if (uiState.password.length < 6) {
                                        Toast.makeText(context, "密码长度不能少于 6 位", Toast.LENGTH_SHORT).show()
                                        return@AppRegisterForm
                                    }
                                    if (uiState.password != uiState.confirmPassword) {
                                        Toast.makeText(context, "两次输入的密码不一致", Toast.LENGTH_SHORT).show()
                                        return@AppRegisterForm
                                    }
                                    showRegisterCaptchaDialog = true
                                },
                                isLoading = uiState.isLoading,
                                usernameFocusRequester = registerUsernameRequester,
                                passwordFocusRequester = registerPasswordRequester,
                                confirmPasswordFocusRequester = registerConfirmPasswordRequester,
                                usernameError = uiState.usernameError,
                                emailError = uiState.emailError,
                                passwordError = uiState.passwordError,
                                confirmPasswordError = uiState.confirmPasswordError,
                                onGoogleLoginClick = {
                                    googleSignInState.startFlow()
                                },
                                onFocusFieldChange = { focusedField = it },
                                backdrop = backdrop
                            )
                        }
                    }
                }
            }

            // 底部平滑自适应安全抬升 Spacer（保证当前选中输入框在键盘上方，而绝不顶起整块区域）
            Spacer(modifier = Modifier.height(animatedLift))
        }
    }

    // 找回密码重置弹窗
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

    // 注册安全验证码弹窗
    if (showRegisterCaptchaDialog) {
        RegisterCaptchaDialog(
            onDismissRequest = { showRegisterCaptchaDialog = false },
            onSuccess = {
                showRegisterCaptchaDialog = false
                val finalUsername = uiState.username.ifBlank { uiState.email.substringBefore('@') }
                viewModel.register(finalUsername, uiState.email, uiState.password)
            },
            isDark = isDark,
            title = "安全验证",
            description = "请根据下方图片输入 6 位验证码，防止恶意注册：",
            confirmText = "确认并注册"
        )
    }

    // 登录输错 3 次安全验证码弹窗
    if (showLoginCaptchaDialog) {
        RegisterCaptchaDialog(
            onDismissRequest = { showLoginCaptchaDialog = false },
            onSuccess = {
                showLoginCaptchaDialog = false
                viewModel.onLoginClicked()
            },
            isDark = isDark,
            title = "安全验证",
            description = "密码错误次数过多，请输入 6 位验证码后继续登录：",
            confirmText = "确认并登录"
        )
    }
}
