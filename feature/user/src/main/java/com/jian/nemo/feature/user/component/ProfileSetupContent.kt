package com.jian.nemo.feature.user.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.nemo.core.ui.component.AvatarImage
import com.jian.nemo.core.ui.component.liquid.ColoredLiquidGlassButton
import com.jian.nemo.core.ui.component.liquid.FrostedLiquidGlassButton
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

/**
 * 完善用户资料内容页面（方案 A：极致一体化卡片风）
 * 风格全面对齐 Nemo 新版登录注册页，具备假名天幕物理折射采样、一体化相机徽标头像、
 * 48dp 磨砂液态玻璃跳过按钮、彩色液态玻璃开始体验按钮及确认密码严格校验。
 */
@Composable
fun ProfileSetupContent(
    nickname: String,
    onNicknameChange: (String) -> Unit,
    avatarPath: String?,
    isGoogleOnlyUser: Boolean,
    password: String,
    onPasswordChange: (String) -> Unit,
    isPasswordVisible: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    isLoading: Boolean,
    onSkip: () -> Unit,
    onStartClick: () -> Unit,
    onAvatarClick: () -> Unit
) {
    val backdrop = rememberLayerBackdrop()
    val isDark = isSystemInDarkTheme()
    val focusManager = LocalFocusManager.current

    // 二次确认密码状态与显隐
    var confirmPassword by remember { mutableStateOf("") }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }

    // 校验逻辑
    val hasPassword = password.isNotEmpty()
    val passwordMismatch = isGoogleOnlyUser && hasPassword && confirmPassword.isNotEmpty() && password != confirmPassword
    val passwordTooShort = isGoogleOnlyUser && hasPassword && password.length < 6
    val isPasswordValid = !isGoogleOnlyUser || !hasPassword || (password.length >= 6 && password == confirmPassword)
    val canStart = !isLoading && nickname.isNotBlank() && isPasswordValid

    // 配色定义
    val backgroundColor = if (isDark) Color(0xFF100F14) else MaterialTheme.colorScheme.background
    val titleTextColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subtitleTextColor = if (isDark) Color.White.copy(alpha = 0.50f) else Color(0xFF64748B)
    val labelTextColor = if (isDark) Color.White.copy(alpha = 0.70f) else Color(0xFF334155)
    val primaryGlassTint = if (isDark) Color(0xFF2563EB) else Color(0xFF0E68FF)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // 1. 底层：纯 Jetpack Compose Canvas 原生假名天幕（作为液态玻璃 backdrop 折射采样的真实物理图层）
        KanaWaveBackground(
            isDark = isDark,
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .align(Alignment.TopCenter)
                .layerBackdrop(backdrop)
        )

        // 2. 主体内容区：移除全局 imePadding，按钮位于自然滚动流底部，在输入聚焦时自然隐于键盘后
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 顶部标题与液态玻璃操作栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 16.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "完善名片",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = titleTextColor,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "开启沉浸式日语假名习得之旅",
                        fontSize = 13.sp,
                        color = subtitleTextColor
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // 右侧【跳过】按钮：48dp 高度带物理折射的磨砂液态玻璃胶囊按钮
                FrostedLiquidGlassButton(
                    onClick = onSkip,
                    backdrop = backdrop,
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = "跳过",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) Color.White.copy(alpha = 0.90f) else Color(0xFF1E293B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 头像区域（一体化徽标与微光环设计）
            Box(
                modifier = Modifier
                    .size(116.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onAvatarClick() },
                contentAlignment = Alignment.Center
            ) {
                // 外层细腻微光环
                Surface(
                    shape = CircleShape,
                    color = Color.Transparent,
                    border = BorderStroke(
                        1.5.dp,
                        if (isDark) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                    ),
                    modifier = Modifier.fillMaxSize()
                ) {}

                // 头像本体
                Surface(
                    shape = CircleShape,
                    border = BorderStroke(2.dp, if (isDark) Color(0xFF1E1D24) else Color.White),
                    shadowElevation = 3.dp,
                    modifier = Modifier.size(104.dp)
                ) {
                    AvatarImage(
                        avatarPath = avatarPath,
                        username = nickname.takeIf { it.isNotBlank() } ?: "用户",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // 右下角相机编辑徽标 (Camera Badge)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-2).dp, y = (-2).dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .border(2.dp, if (isDark) Color(0xFF100F14) else Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "更换头像",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "点击更换个性头像",
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = subtitleTextColor
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 表单输入区域
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // 用户名称字段
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "用户名称",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = labelTextColor,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )

                    AppStyleTextField(
                        value = nickname,
                        onValueChange = onNicknameChange,
                        placeholder = "设置您的专属昵称",
                        leadingIcon = Icons.Rounded.Person,
                        isDark = isDark,
                        enabled = !isLoading,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = if (isGoogleOnlyUser) ImeAction.Next else ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (!isGoogleOnlyUser && canStart) {
                                    focusManager.clearFocus()
                                    onStartClick()
                                }
                            }
                        )
                    )
                }

                // 独立密码字段（仅在 Google Only 用户时展示选填）
                if (isGoogleOnlyUser) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "独立密码 (选填)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = labelTextColor,
                            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                        )

                        AppStyleTextField(
                            value = password,
                            onValueChange = onPasswordChange,
                            placeholder = "设置登录密码（至少 6 位）",
                            leadingIcon = Icons.Rounded.Lock,
                            trailingIcon = if (isPasswordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            onTrailingIconClick = onTogglePasswordVisibility,
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            isDark = isDark,
                            enabled = !isLoading,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Next
                            )
                        )
                    }

                    // 再次输入密码（确认密码字段）
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "确认密码",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = labelTextColor,
                            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                        )

                        AppStyleTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            placeholder = "请再次输入密码以进行确认",
                            leadingIcon = Icons.Rounded.Lock,
                            trailingIcon = if (isConfirmPasswordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            onTrailingIconClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible },
                            visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            isDark = isDark,
                            enabled = !isLoading && hasPassword,
                            isError = passwordMismatch,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (canStart) {
                                        focusManager.clearFocus()
                                        onStartClick()
                                    }
                                }
                            )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        if (passwordMismatch) {
                            Text(
                                text = "两次输入的密码不一致",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        } else if (passwordTooShort) {
                            Text(
                                text = "密码长度至少需要 6 位",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        } else {
                            Text(
                                text = "设置独立密码后，未来亦可使用邮箱密码快捷登录",
                                fontSize = 12.sp,
                                color = subtitleTextColor,
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // 核心主按钮【开始体验】：彩色液态玻璃按钮（物理透镜折射 + 色相注入）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                ColoredLiquidGlassButton(
                    onClick = {
                        if (canStart) {
                            focusManager.clearFocus()
                            onStartClick()
                        }
                    },
                    backdrop = backdrop,
                    tint = primaryGlassTint,
                    isInteractive = canStart,
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "保存并开启",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = if (canStart) 1f else 0.40f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = if (canStart) 1f else 0.40f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}


