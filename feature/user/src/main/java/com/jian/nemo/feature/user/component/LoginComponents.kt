package com.jian.nemo.feature.user.component

import android.graphics.Paint
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.nemo.core.ui.component.animation.NemoChasingDotsLoader
import com.jian.nemo.core.ui.component.liquid.ColoredLiquidGlassButton
import com.jian.nemo.core.ui.component.liquid.FrostedLiquidGlassButton
import com.jian.nemo.core.ui.modifier.softCardShadow
import com.kyant.backdrop.Backdrop
import kotlin.math.cos
import kotlin.math.sin

/**
 * 当前获得输入焦点的表单输入框类型枚举
 */
enum class FocusedAuthField {
    LOGIN_EMAIL,
    LOGIN_PASSWORD,
    REGISTER_USERNAME,
    REGISTER_EMAIL,
    REGISTER_PASSWORD,
    REGISTER_CONFIRM_PASSWORD
}

/**
 * 顶部假名流动天幕背景（100% 纯 Jetpack Compose Canvas 原生高性能绘制）
 * 动态延伸铺展，底部带有深度平滑过渡渐变，彻底消除与表单之间的苍白断层
 */
@Composable
fun KanaWaveBackground(
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "KanaWaveFlowTransition")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 62.8318f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 60000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "kanaTime"
    )

    // 页面初次进入时 2.4 秒波浪式由上至下破晓展开动效（1:1 还原原版 INTRO_SECONDS = 2.4 动画）
    val introAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        introAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2400, easing = FastOutSlowInEasing)
        )
    }

    val pageBgColor = if (isDark) Color(0xFF0D0C11) else Color(0xFFF7F9FC)
    val density = LocalDensity.current

    // 原型同款经典平假名字库
    val kanaPool = remember {
        listOf("あ", "ま", "め", "ぬ", "の", "し", "な", "て", "こ", "く", "・")
    }

    // 浅色与深色的高审美渐变天光
    val skyGradients = if (isDark) {
        listOf(
            Color(0xFF0D0C11),
            Color(0xFF1E3FD8).copy(alpha = 0.95f),
            Color(0xFF2563EB).copy(alpha = 0.85f),
            Color(0xFF38BDF8).copy(alpha = 0.60f)
        )
    } else {
        listOf(
            Color(0xFF1D4ED8),
            Color(0xFF2563EB),
            Color(0xFF38BDF8),
            Color(0xFF7DD3FC),
            Color(0xFFBAE6FD)
        )
    }

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val intro = introAnim.value

            // 1. 绘制底层天蓝/深空流光径向与线性渐变（随展开动画从顶部舒展铺开）
            val currentEndY = height * (0.35f + 0.57f * intro)
            val bgGradient = Brush.verticalGradient(
                colors = skyGradients,
                startY = 0f,
                endY = currentEndY
            )
            drawRect(brush = bgGradient)

            // 2. 绘制交错错位的假名流粒子阵列
            val cellXPx = with(density) { 32.dp.toPx() }
            val cellYPx = with(density) { 30.dp.toPx() }
            val fontSizePx = with(density) { 15.sp.toPx() }

            val paint = Paint().apply {
                isAntiAlias = true
                textSize = fontSizePx
                textAlign = Paint.Align.CENTER
                isFakeBoldText = true
            }

            val rows = (height / cellYPx).toInt() + 2
            val cols = (width / cellXPx).toInt() + 4

            for (r in 0 until rows) {
                val rowOffset = if (r % 2 == 1) cellXPx * 0.5f else 0f
                val rowSpeed = 12f + (r % 4) * 5f
                val rowShift = (time * rowSpeed) % (cellXPx * cols)

                val baseY = (r + 0.8f) * cellYPx
                val rowOrder = (r.toFloat() / rows.coerceAtLeast(1))

                for (c in -2 until cols) {
                    val rawX = c * cellXPx + rowOffset - rowShift
                    val posX = if (rawX < -cellXPx) rawX + (cols + 2) * cellXPx else rawX

                    if (posX > width + cellXPx) continue

                    // 1:1 对齐原型 WebGL 着色器的地平线波浪展开算法 (Intro Reveal)
                    val waveFront = sin(c * 0.35f) * 0.06f
                    val order = (rowOrder + waveFront).coerceIn(0f, 1f)
                    val rawReveal = ((intro * 1.45f - order) / 0.45f).coerceIn(0f, 1f)
                    val reveal = rawReveal * rawReveal * (3.0f - 2.0f * rawReveal)

                    if (reveal <= 0.01f) continue

                    // 正弦水波起伏位移 + 入场轻微自上而下涌入位移
                    val waveY = sin(c * 0.45f + time * 1.8f + r * 0.35f) * 5.5f
                    val dropY = (1f - reveal) * (-14f)
                    val posY = baseY + waveY + dropY

                    val charIdx = ((c * 3 + r * 7 + (r * 11)) % kanaPool.size).let {
                        if (it < 0) it + kanaPool.size else it
                    }
                    val charText = kanaPool[charIdx]

                    // 渐变透明度（随高度、水波及展开进度自然融合）
                    val normalizedY = (baseY / height).coerceIn(0f, 1f)
                    val wavePulse = (cos(c * 0.35f + r * 0.5f + time * 2.2f) + 1f) * 0.5f
                    val baseAlpha = if (isDark) {
                        (0.25f + 0.65f * wavePulse) * (1f - normalizedY * 0.45f)
                    } else {
                        (0.35f + 0.60f * wavePulse) * (1f - normalizedY * 0.35f)
                    }

                    val finalAlpha = (baseAlpha * reveal).coerceIn(0f, 0.95f)
                    val alphaInt = (finalAlpha * 255).toInt()
                    val colorInt = if (isDark) {
                        android.graphics.Color.argb(alphaInt, 246, 248, 255)
                    } else {
                        android.graphics.Color.argb(alphaInt, 255, 255, 255)
                    }

                    paint.color = colorInt
                    drawContext.canvas.nativeCanvas.drawText(
                        charText,
                        posX,
                        posY,
                        paint
                    )
                }
            }

            // 3. 底部自然深度过渡渐变遮罩（高度延长至占后 45%，使天幕自然融于表单上方）
            val maskGradient = Brush.verticalGradient(
                colors = listOf(
                    pageBgColor.copy(alpha = 0f),
                    pageBgColor.copy(alpha = 0.35f),
                    pageBgColor.copy(alpha = 0.85f),
                    pageBgColor
                ),
                startY = height * 0.55f,
                endY = height
            )
            drawRect(brush = maskGradient)
        }
    }
}

/**
 * Material 3 (MD3) 官方标准输入框组件（带 14dp 圆角与项目弥散软阴影）
 */
@Composable
fun AppStyleTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    trailingIcon: ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    isError: Boolean = false,
    enabled: Boolean = true
) {
    val containerBg = if (isDark) Color(0xFF16151B) else Color.White
    val focusedBorder = if (isDark) Color.White.copy(alpha = 0.65f) else Color(0xFF0F172A)
    val unfocusedBorder = if (isDark) Color.White.copy(alpha = 0.15f) else Color(0xFFE2E8F0)
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val placeholderColor = if (isDark) Color.White.copy(alpha = 0.40f) else Color(0xFF94A3B8)
    val iconColor = if (isDark) Color.White.copy(alpha = 0.50f) else Color(0xFF94A3B8)

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .softCardShadow(borderRadius = 14.dp, isDark = isDark),
        enabled = enabled,
        singleLine = true,
        isError = isError,
        shape = RoundedCornerShape(14.dp),
        textStyle = TextStyle(
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
        ),
        placeholder = {
            Text(
                text = placeholder,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = placeholderColor
            )
        },
        leadingIcon = {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(19.dp)
            )
        },
        trailingIcon = if (trailingIcon != null) {
            {
                IconButton(
                    onClick = { onTrailingIconClick?.invoke() },
                    enabled = enabled
                ) {
                    Icon(
                        imageVector = trailingIcon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        } else null,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = containerBg,
            unfocusedContainerColor = containerBg,
            disabledContainerColor = containerBg,
            errorContainerColor = containerBg,
            focusedBorderColor = focusedBorder,
            unfocusedBorderColor = unfocusedBorder,
            focusedTextColor = textColor,
            unfocusedTextColor = textColor,
            cursorColor = textColor
        )
    )
}

/**
 * 登录表单内容：邮箱、密码、横向双按钮（忘记密码 + 立即登录）、Google登录
 * 全部按钮均注入弥散软阴影
 */
@Composable
fun AppLoginForm(
    email: String,
    password: String,
    isPasswordVisible: Boolean,
    isDark: Boolean,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onLogin: () -> Unit,
    isLoading: Boolean,
    emailFocusRequester: FocusRequester,
    passwordFocusRequester: FocusRequester,
    onForgotPasswordClick: () -> Unit,
    emailError: Boolean = false,
    passwordError: Boolean = false,
    onGoogleLoginClick: () -> Unit,
    onFocusFieldChange: (FocusedAuthField?) -> Unit = {},
    backdrop: Backdrop? = null
) {
    val primaryGlassTint = if (isDark) Color(0xFF2563EB) else Color(0xFF0E68FF)

    val secondaryBtnText = if (isDark) Color.White.copy(alpha = 0.90f) else Color(0xFF1E293B)
    val googleBtnText = if (isDark) Color.White else Color(0xFF1E293B)

    val dividerColor = if (isDark) Color.White.copy(alpha = 0.10f) else Color(0xFFE2E8F0)
    val dividerTextColor = if (isDark) Color.White.copy(alpha = 0.35f) else Color(0xFF94A3B8)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. 电子邮箱输入框（MD3 标准组件 + 弥散阴影）
        AppStyleTextField(
            value = email,
            onValueChange = onEmailChange,
            placeholder = "电子邮箱地址",
            leadingIcon = Icons.Rounded.Mail,
            isDark = isDark,
            isError = emailError,
            enabled = !isLoading,
            modifier = Modifier
                .focusRequester(emailFocusRequester)
                .onFocusChanged { if (it.isFocused) onFocusFieldChange(FocusedAuthField.LOGIN_EMAIL) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            )
        )

        // 2. 密码输入框（MD3 标准组件 + 弥散阴影）
        AppStyleTextField(
            value = password,
            onValueChange = onPasswordChange,
            placeholder = "输入登录密码",
            leadingIcon = Icons.Rounded.Lock,
            trailingIcon = if (isPasswordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
            onTrailingIconClick = onTogglePasswordVisibility,
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            isDark = isDark,
            isError = passwordError,
            enabled = !isLoading,
            modifier = Modifier
                .focusRequester(passwordFocusRequester)
                .onFocusChanged { if (it.isFocused) onFocusFieldChange(FocusedAuthField.LOGIN_PASSWORD) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { onLogin() }
            )
        )

        Spacer(modifier = Modifier.height(2.dp))

        // 3. 核心横向双按钮：左侧带底色磨砂液态玻璃「忘记密码」，右侧彩色液态玻璃「立即登录 >」
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：没颜色选用「带底色磨砂液态玻璃按钮」
            FrostedLiquidGlassButton(
                onClick = { if (!isLoading) onForgotPasswordClick() },
                backdrop = backdrop,
                isInteractive = !isLoading,
                shape = RoundedCornerShape(15.dp),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "忘记密码",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = secondaryBtnText
                    )
                }
            }

            // 右侧：有颜色选用「彩色液态玻璃主按钮」
            ColoredLiquidGlassButton(
                onClick = { if (!isLoading) onLogin() },
                backdrop = backdrop,
                tint = primaryGlassTint,
                isInteractive = !isLoading,
                shape = RoundedCornerShape(15.dp),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .weight(1.35f)
                    .height(52.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isLoading) {
                        NemoChasingDotsLoader(size = 22.dp, color = Color.White)
                    } else {
                        Text(
                            text = "立即登录",
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
            }
        }

        // 4. 分隔线「或」
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = dividerColor,
                thickness = 1.dp
            )
            Text(
                text = "或",
                modifier = Modifier.padding(horizontal = 14.dp),
                color = dividerTextColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal
            )
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = dividerColor,
                thickness = 1.dp
            )
        }

        // 5. 没颜色选用「带底色磨砂液态玻璃按钮」Google 快捷登录
        FrostedLiquidGlassButton(
            onClick = { if (!isLoading) onGoogleLoginClick() },
            backdrop = backdrop,
            isInteractive = !isLoading,
            shape = RoundedCornerShape(15.dp),
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = GoogleIcon,
                    contentDescription = "Google",
                    modifier = Modifier.size(20.dp),
                    tint = Color.Unspecified
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "使用 Google 账号登录",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = googleBtnText
                )
            }
        }
    }
}

/**
 * 注册表单内容：昵称、邮箱、密码、确认密码、全宽【立即注册 >】、Google快捷注册
 * 全部按钮均注入弥散软阴影
 */
@Composable
fun AppRegisterForm(
    username: String,
    email: String,
    password: String,
    confirmPassword: String,
    isPasswordVisible: Boolean,
    isConfirmPasswordVisible: Boolean,
    isDark: Boolean,
    onUsernameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onToggleConfirmPasswordVisibility: () -> Unit,
    onRegister: () -> Unit,
    isLoading: Boolean,
    usernameFocusRequester: FocusRequester,
    passwordFocusRequester: FocusRequester,
    confirmPasswordFocusRequester: FocusRequester,
    usernameError: Boolean = false,
    emailError: Boolean = false,
    passwordError: Boolean = false,
    confirmPasswordError: Boolean = false,
    onGoogleLoginClick: () -> Unit,
    onFocusFieldChange: (FocusedAuthField?) -> Unit = {},
    backdrop: Backdrop? = null
) {
    val primaryGlassTint = if (isDark) Color(0xFF2563EB) else Color(0xFF0E68FF)
    val googleBtnText = if (isDark) Color.White else Color(0xFF1E293B)

    val dividerColor = if (isDark) Color.White.copy(alpha = 0.10f) else Color(0xFFE2E8F0)
    val dividerTextColor = if (isDark) Color.White.copy(alpha = 0.35f) else Color(0xFF94A3B8)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. 您的称呼（选填）
        AppStyleTextField(
            value = username,
            onValueChange = onUsernameChange,
            placeholder = "您的称呼（选填）",
            leadingIcon = Icons.Rounded.Person,
            isDark = isDark,
            isError = usernameError,
            enabled = !isLoading,
            modifier = Modifier
                .focusRequester(usernameFocusRequester)
                .onFocusChanged { if (it.isFocused) onFocusFieldChange(FocusedAuthField.REGISTER_USERNAME) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            )
        )

        // 2. 电子邮箱地址
        AppStyleTextField(
            value = email,
            onValueChange = onEmailChange,
            placeholder = "电子邮箱地址",
            leadingIcon = Icons.Rounded.Mail,
            isDark = isDark,
            isError = emailError,
            enabled = !isLoading,
            modifier = Modifier.onFocusChanged { if (it.isFocused) onFocusFieldChange(FocusedAuthField.REGISTER_EMAIL) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            )
        )

        // 3. 设置密码
        AppStyleTextField(
            value = password,
            onValueChange = onPasswordChange,
            placeholder = "设置密码（至少 6 位）",
            leadingIcon = Icons.Rounded.Lock,
            trailingIcon = if (isPasswordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
            onTrailingIconClick = onTogglePasswordVisibility,
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            isDark = isDark,
            isError = passwordError,
            enabled = !isLoading,
            modifier = Modifier
                .focusRequester(passwordFocusRequester)
                .onFocusChanged { if (it.isFocused) onFocusFieldChange(FocusedAuthField.REGISTER_PASSWORD) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            )
        )

        // 4. 确认密码
        AppStyleTextField(
            value = confirmPassword,
            onValueChange = onConfirmPasswordChange,
            placeholder = "确认密码",
            leadingIcon = Icons.Rounded.Lock,
            trailingIcon = if (isConfirmPasswordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
            onTrailingIconClick = onToggleConfirmPasswordVisibility,
            visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            isDark = isDark,
            isError = confirmPasswordError,
            enabled = !isLoading,
            modifier = Modifier
                .focusRequester(confirmPasswordFocusRequester)
                .onFocusChanged { if (it.isFocused) onFocusFieldChange(FocusedAuthField.REGISTER_CONFIRM_PASSWORD) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { onRegister() }
            )
        )

        Spacer(modifier = Modifier.height(2.dp))

        // 5. 有颜色选用「彩色液态玻璃主按钮」立即注册
        ColoredLiquidGlassButton(
            onClick = { if (!isLoading) onRegister() },
            backdrop = backdrop,
            tint = primaryGlassTint,
            isInteractive = !isLoading,
            shape = RoundedCornerShape(15.dp),
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isLoading) {
                    NemoChasingDotsLoader(size = 22.dp, color = Color.White)
                } else {
                    Text(
                        text = "立即注册",
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
        }

        // 6. 分隔线「或」
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = dividerColor,
                thickness = 1.dp
            )
            Text(
                text = "或",
                modifier = Modifier.padding(horizontal = 14.dp),
                color = dividerTextColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal
            )
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = dividerColor,
                thickness = 1.dp
            )
        }

        // 7. 没颜色选用「带底色磨砂液态玻璃按钮」Google 账号快捷注册
        FrostedLiquidGlassButton(
            onClick = { if (!isLoading) onGoogleLoginClick() },
            backdrop = backdrop,
            isInteractive = !isLoading,
            shape = RoundedCornerShape(15.dp),
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = GoogleIcon,
                    contentDescription = "Google",
                    modifier = Modifier.size(20.dp),
                    tint = Color.Unspecified
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "使用 Google 账号快捷注册",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = googleBtnText
                )
            }
        }
    }
}

/**
 * 兼容旧接口的 LoginForm 与 RegisterForm
 */
@Composable
fun LoginForm(
    email: String,
    password: String,
    isPasswordVisible: Boolean,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onLogin: () -> Unit,
    isLoading: Boolean,
    emailFocusRequester: FocusRequester,
    passwordFocusRequester: FocusRequester,
    onForgotPasswordClick: () -> Unit,
    emailError: Boolean = false,
    passwordError: Boolean = false,
    onGoogleLoginClick: () -> Unit
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    AppLoginForm(
        email = email,
        password = password,
        isPasswordVisible = isPasswordVisible,
        isDark = isDark,
        onEmailChange = onEmailChange,
        onPasswordChange = onPasswordChange,
        onTogglePasswordVisibility = onTogglePasswordVisibility,
        onLogin = onLogin,
        isLoading = isLoading,
        emailFocusRequester = emailFocusRequester,
        passwordFocusRequester = passwordFocusRequester,
        onForgotPasswordClick = onForgotPasswordClick,
        emailError = emailError,
        passwordError = passwordError,
        onGoogleLoginClick = onGoogleLoginClick
    )
}

@Composable
fun RegisterForm(
    username: String,
    email: String,
    password: String,
    confirmPassword: String,
    isPasswordVisible: Boolean,
    isConfirmPasswordVisible: Boolean,
    onUsernameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onToggleConfirmPasswordVisibility: () -> Unit,
    onRegister: () -> Unit,
    isLoading: Boolean,
    usernameFocusRequester: FocusRequester,
    passwordFocusRequester: FocusRequester,
    confirmPasswordFocusRequester: FocusRequester,
    usernameError: Boolean = false,
    emailError: Boolean = false,
    passwordError: Boolean = false,
    confirmPasswordError: Boolean = false
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    AppRegisterForm(
        username = username,
        email = email,
        password = password,
        confirmPassword = confirmPassword,
        isPasswordVisible = isPasswordVisible,
        isConfirmPasswordVisible = isConfirmPasswordVisible,
        isDark = isDark,
        onUsernameChange = onUsernameChange,
        onEmailChange = onEmailChange,
        onPasswordChange = onPasswordChange,
        onConfirmPasswordChange = onConfirmPasswordChange,
        onTogglePasswordVisibility = onTogglePasswordVisibility,
        onToggleConfirmPasswordVisibility = onToggleConfirmPasswordVisibility,
        onRegister = onRegister,
        isLoading = isLoading,
        usernameFocusRequester = usernameFocusRequester,
        passwordFocusRequester = passwordFocusRequester,
        confirmPasswordFocusRequester = confirmPasswordFocusRequester,
        usernameError = usernameError,
        emailError = emailError,
        passwordError = passwordError,
        confirmPasswordError = confirmPasswordError,
        onGoogleLoginClick = {}
    )
}
