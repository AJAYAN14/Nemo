package com.jian.nemo.feature.user.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.jian.nemo.core.ui.component.animation.NemoChasingDotsLoader
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    emailError: Boolean = false, // Added error state
    passwordError: Boolean = false // Added error state
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        GitHubStyleTextField(
            value = email,
            onValueChange = onEmailChange,
            placeholder = "邮箱",
            leadingIcon = Icons.Default.Email,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(emailFocusRequester),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            isError = emailError,
            enabled = !isLoading
        )

        GitHubStyleTextField(
            value = password,
            onValueChange = onPasswordChange,
            placeholder = "密码",
            leadingIcon = Icons.Default.Lock,
            trailingIcon = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
            onTrailingIconClick = onTogglePasswordVisibility,
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(passwordFocusRequester),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done // [UX] Done action
            ),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                onDone = { onLogin() } // [UX] Trigger login
            ),
            isError = passwordError,
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(8.dp))

        // [Fix] Enforce Brand Blue for Button
        Button(
            onClick = onLogin,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(50),
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF0E68FF),
                contentColor = Color.White
            )
        ) {
            if (isLoading) {
                NemoChasingDotsLoader(size = 24.dp)
            } else {
                Text(
                    text = "立即登录",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        TextButton(
            onClick = onForgotPasswordClick,
            modifier = Modifier.align(Alignment.End),
            enabled = !isLoading
        ) {
            Text(
                text = "忘记密码？",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF0E68FF) // [Fix] Brand Blue text
            )
        }
    }
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        GitHubStyleTextField(
            value = username,
            onValueChange = onUsernameChange,
            placeholder = "用户名",
            leadingIcon = Icons.Default.Person,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(usernameFocusRequester),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            isError = usernameError,
            enabled = !isLoading
        )

        GitHubStyleTextField(
            value = email,
            onValueChange = onEmailChange,
            placeholder = "电子邮箱",
            leadingIcon = Icons.Default.Email,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            isError = emailError,
            enabled = !isLoading
        )

        GitHubStyleTextField(
            value = password,
            onValueChange = onPasswordChange,
            placeholder = "密码 (不少于6位)",
            leadingIcon = Icons.Default.Lock,
            trailingIcon = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
            onTrailingIconClick = onTogglePasswordVisibility,
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(passwordFocusRequester),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            ),
            isError = passwordError,
            enabled = !isLoading
        )

        GitHubStyleTextField(
            value = confirmPassword,
            onValueChange = onConfirmPasswordChange,
            placeholder = "确认密码",
            leadingIcon = Icons.Default.Lock,
            trailingIcon = if (isConfirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
            onTrailingIconClick = onToggleConfirmPasswordVisibility,
            visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(confirmPasswordFocusRequester),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done // [UX] Done action
            ),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                onDone = { onRegister() } // [UX] Trigger register
            ),
            isError = confirmPasswordError,
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(8.dp))

        // [Fix] Enforce Brand Blue for Button
        Button(
            onClick = onRegister,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(50),
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF0E68FF),
                contentColor = Color.White
            )
        ) {
            if (isLoading) {
                NemoChasingDotsLoader(size = 24.dp)
            } else {
                Text(
                    text = "创建账户",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun GitHubStyleTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    trailingIcon: ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: androidx.compose.foundation.text.KeyboardActions = androidx.compose.foundation.text.KeyboardActions.Default,
    isError: Boolean = false, // [UX] Visual Error State
    enabled: Boolean = true   // [UX] Loading State
) {
    // [Fix] Enforce Brand Blue everywhere instead of Theme Primary
    val brandBlue = Color(0xFF0E68FF)
    // [Fix] Dark Mode Logic
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()

    val textColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
    val placeholderColor = if (isDark) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
    val iconColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
    val trailingIconColor = if (isDark) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant

    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val errorColor = MaterialTheme.colorScheme.error

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        isError = isError,
        textStyle = LocalTextStyle.current.copy(
            fontSize = 16.sp,
            color = textColor,
            fontWeight = FontWeight.Medium
        ),
        placeholder = {
            Text(
                text = placeholder,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = placeholderColor
            )
        },
        singleLine = true,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        shape = RoundedCornerShape(15.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = if (isError) errorColor else brandBlue,
            unfocusedBorderColor = if (isError) errorColor else onSurfaceVariant.copy(alpha = 0.5f),
            cursorColor = brandBlue,
            errorBorderColor = errorColor,
            errorCursorColor = errorColor
        ),
        leadingIcon = {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = if (isError) errorColor else iconColor,
                modifier = Modifier.size(20.dp)
            )
        },
        trailingIcon = {
             // [UX] Crossfade Animation for Trailing Icon
             androidx.compose.animation.Crossfade(targetState = trailingIcon) { targetIcon ->
                 if (targetIcon != null) {
                     IconButton(
                         onClick = { onTrailingIconClick?.invoke() },
                         enabled = enabled
                     ) {
                         Icon(
                             imageVector = targetIcon,
                             contentDescription = null,
                             tint = if (isError) errorColor else trailingIconColor,
                             modifier = Modifier.size(20.dp)
                         )
                     }
                 }
             }
        }
    )
}
