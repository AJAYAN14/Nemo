package com.jian.nemo.feature.user.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
        // 1. 您的称呼（选填，最多16字）
        AppStyleTextField(
            value = username,
            onValueChange = { if (it.length <= 16) onUsernameChange(it) },
            placeholder = "您的称呼（选填，最多16字）",
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
            placeholder = "设置密码（至少 8 位）",
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
 * 登录表单与注册表单组件
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
