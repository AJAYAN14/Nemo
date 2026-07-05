package com.jian.nemo.feature.user.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.nemo.core.ui.component.animation.NemoChasingDotsLoader
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.Alignment
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.style.TextAlign

@Composable
fun AccountResetPasswordDialog(
    userEmail: String,
    onDismiss: () -> Unit,
    onSendOtp: (String, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit,
    onVerifyOtp: (String, String, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit,
    onResetPassword: (String, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit,
    useDarkTheme: Boolean = isSystemInDarkTheme()
) {
    // Stage: 0=Email, 1=OTP, 2=NewPassword
    var stage by remember { mutableIntStateOf(0) }
    var email by remember { mutableStateOf(userEmail) }
    var otp by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // UI/UX Pro Max Colors & Styles (Yellow/Orange Theme for Reset Password)
    val primaryColor = if (useDarkTheme) Color(0xFFFF9F0A) else Color(0xFFFF9500)
    val containerColor = if (useDarkTheme) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)
    val titleColor = if (useDarkTheme) Color.White else Color.Black
    val bodyColor = if (useDarkTheme) Color(0xFF8E8E93) else Color(0xFF6E6E73)
    val focusedBorderColor = primaryColor
    val unfocusedBorderColor = if (useDarkTheme) Color(0xFF3A3A3C) else Color(0xFFC6C6C8)
    val errorColor = if (useDarkTheme) Color(0xFFFF453A) else Color(0xFFFF3B30)
    val selectionColors = androidx.compose.foundation.text.selection.TextSelectionColors(
        handleColor = primaryColor,
        backgroundColor = primaryColor.copy(alpha = 0.4f)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
        containerColor = containerColor,
        titleContentColor = titleColor,
        textContentColor = bodyColor,
        iconContentColor = primaryColor,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(26.dp),
        icon = {
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = when(stage) {
                    0 -> "重置密码"
                    1 -> "输入验证码"
                    2 -> "设置新密码"
                    else -> ""
                },
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = titleColor,
                letterSpacing = (-0.5).sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (stage == 0) {
                    Text(
                        text = "我们将向您的邮箱发送验证码。",
                        fontSize = 15.sp,
                        color = bodyColor,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Normal
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = {
                            Text(
                                "邮箱地址",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        singleLine = true,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(26.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = titleColor,
                            unfocusedTextColor = titleColor,
                            disabledTextColor = bodyColor.copy(alpha = 0.38f),
                            errorTextColor = errorColor,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            errorContainerColor = Color.Transparent,
                            cursorColor = primaryColor,
                            errorCursorColor = errorColor,
                            selectionColors = selectionColors,
                            focusedBorderColor = focusedBorderColor,
                            unfocusedBorderColor = unfocusedBorderColor,
                            disabledBorderColor = unfocusedBorderColor.copy(alpha = 0.12f),
                            errorBorderColor = errorColor,
                            focusedLabelColor = primaryColor,
                            unfocusedLabelColor = bodyColor,
                            disabledLabelColor = bodyColor.copy(alpha = 0.38f),
                            errorLabelColor = errorColor
                        )
                    )
                } else if (stage == 1) {
                    Text(
                        text = "请输入发送至 $email 的 8 位验证码。",
                        fontSize = 15.sp,
                        color = bodyColor,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Normal
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    OutlinedTextField(
                        value = otp,
                        onValueChange = { if (it.length <= 8) otp = it },
                        label = {
                            Text(
                                "验证码",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        singleLine = true,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(26.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = titleColor,
                            unfocusedTextColor = titleColor,
                            disabledTextColor = bodyColor.copy(alpha = 0.38f),
                            errorTextColor = errorColor,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            errorContainerColor = Color.Transparent,
                            cursorColor = primaryColor,
                            errorCursorColor = errorColor,
                            selectionColors = selectionColors,
                            focusedBorderColor = focusedBorderColor,
                            unfocusedBorderColor = unfocusedBorderColor,
                            disabledBorderColor = unfocusedBorderColor.copy(alpha = 0.12f),
                            errorBorderColor = errorColor,
                            focusedLabelColor = primaryColor,
                            unfocusedLabelColor = bodyColor,
                            disabledLabelColor = bodyColor.copy(alpha = 0.38f),
                            errorLabelColor = errorColor
                        )
                    )
                } else {
                    Text(
                        text = "验证成功，请设置您的新密码。",
                        fontSize = 15.sp,
                        color = bodyColor,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Normal
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = {
                            Text(
                                "新密码",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        singleLine = true,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(26.dp),
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = titleColor,
                            unfocusedTextColor = titleColor,
                            disabledTextColor = bodyColor.copy(alpha = 0.38f),
                            errorTextColor = errorColor,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            errorContainerColor = Color.Transparent,
                            cursorColor = primaryColor,
                            errorCursorColor = errorColor,
                            selectionColors = selectionColors,
                            focusedBorderColor = focusedBorderColor,
                            unfocusedBorderColor = unfocusedBorderColor,
                            disabledBorderColor = unfocusedBorderColor.copy(alpha = 0.12f),
                            errorBorderColor = errorColor,
                            focusedLabelColor = primaryColor,
                            unfocusedLabelColor = bodyColor,
                            disabledLabelColor = bodyColor.copy(alpha = 0.38f),
                            errorLabelColor = errorColor
                        )
                    )
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = "Error",
                            tint = errorColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = errorMessage ?: "",
                            color = errorColor,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    errorMessage = null
                    when(stage) {
                        0 -> {
                             if (email.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                                 isLoading = true
                                 onSendOtp(email,
                                     { // onSuccess
                                         isLoading = false
                                         stage = 1
                                     },
                                     { error -> // onError
                                         isLoading = false
                                         errorMessage = error
                                     }
                                 )
                             } else {
                                 errorMessage = "请输入有效的邮箱地址"
                             }
                        }
                        1 -> {
                            if (otp.length == 8) {
                                isLoading = true
                                onVerifyOtp(email, otp,
                                    { // onSuccess
                                        isLoading = false
                                        stage = 2
                                    },
                                    { error -> // onError
                                        isLoading = false
                                        errorMessage = error
                                    }
                                )
                            } else {
                                errorMessage = "请输入8位验证码"
                            }
                        }
                        2 -> {
                            if (newPassword.length >= 8) {
                                isLoading = true
                                onResetPassword(newPassword,
                                    { // onSuccess
                                        isLoading = false
                                        onDismiss() // Close dialog
                                    },
                                    { error -> // onError
                                        isLoading = false
                                        errorMessage = error
                                    }
                                )
                            } else {
                                errorMessage = "密码长度至少为 8 位"
                            }
                        }
                    }
                },
                enabled = !isLoading,
                shape = androidx.compose.foundation.shape.CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor,
                    contentColor = Color.White,
                    disabledContainerColor = if (useDarkTheme) Color(0xFF3A3A3C) else Color(0xFFE5E5EA),
                    disabledContentColor = if (useDarkTheme) Color(0xFF636366) else Color(0xFFAEAEB2)
                ),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
            ) {
                if (isLoading) {
                    NemoChasingDotsLoader(size = 18.dp)
                } else {
                    Text(
                        text = if (stage == 2) "重置密码" else "下一步",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading,
                shape = androidx.compose.foundation.shape.CircleShape,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = bodyColor,
                    disabledContentColor = bodyColor.copy(alpha = 0.38f)
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "取消",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    )
}
// Helper to control stage from outside if needed, but for now we keep state internal to dialog for simplicity
// except we need a way to tell dialog "Success, move to next step".
// Since we can't easily change the architecture to return Result in callback,
// we will rely on a slightly different approach:
// The Callers will be updated to accept a callback that returns Boolean or similar?
// No, standard MVVM: ViewModel exposes State, Dialog observes State.
// But this dialog is likely just functional stateless component used in a Screen.
// Let's check where it's used. "AccountManagementScreen" probably.


@Composable
fun AccountUpdateUsernameDialog(
    currentUsername: String,
    onDismiss: () -> Unit,
    onUpdateUsername: (String, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit,
    useDarkTheme: Boolean = isSystemInDarkTheme()
) {
    var newUsername by remember { mutableStateOf(currentUsername) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // UI/UX Pro Max Colors & Styles (Direct Color overrides, no MaterialTheme)
    val primaryColor = if (useDarkTheme) Color(0xFF0A84FF) else Color(0xFF007AFF)
    val containerColor = if (useDarkTheme) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)
    val titleColor = if (useDarkTheme) Color.White else Color.Black
    val bodyColor = if (useDarkTheme) Color(0xFF8E8E93) else Color(0xFF6E6E73)
    val focusedBorderColor = primaryColor
    val unfocusedBorderColor = if (useDarkTheme) Color(0xFF3A3A3C) else Color(0xFFC6C6C8)
    val errorColor = if (useDarkTheme) Color(0xFFFF453A) else Color(0xFFFF3B30)
    val selectionColors = androidx.compose.foundation.text.selection.TextSelectionColors(
        handleColor = primaryColor,
        backgroundColor = primaryColor.copy(alpha = 0.4f)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = containerColor,
        titleContentColor = titleColor,
        textContentColor = bodyColor,
        iconContentColor = primaryColor,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(26.dp),
        icon = {
            Icon(
                imageVector = Icons.Rounded.Person,
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = "修改用户名",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = titleColor,
                letterSpacing = (-0.5).sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "请输入新的用户名，这将是您在应用中的显示名称。",
                    fontSize = 15.sp,
                    color = bodyColor,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Normal
                )
                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = newUsername,
                    onValueChange = { newUsername = it },
                    label = {
                        Text(
                            text = "用户名",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    singleLine = true,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(26.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = titleColor,
                        unfocusedTextColor = titleColor,
                        disabledTextColor = bodyColor.copy(alpha = 0.38f),
                        errorTextColor = errorColor,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        errorContainerColor = Color.Transparent,
                        cursorColor = primaryColor,
                        errorCursorColor = errorColor,
                        selectionColors = selectionColors,
                        focusedBorderColor = focusedBorderColor,
                        unfocusedBorderColor = unfocusedBorderColor,
                        disabledBorderColor = unfocusedBorderColor.copy(alpha = 0.12f),
                        errorBorderColor = errorColor,
                        focusedLabelColor = primaryColor,
                        unfocusedLabelColor = bodyColor,
                        disabledLabelColor = bodyColor.copy(alpha = 0.38f),
                        errorLabelColor = errorColor
                    ),
                    isError = errorMessage != null
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = "Error",
                            tint = errorColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = errorMessage ?: "",
                            color = errorColor,
                            style = MaterialTheme.typography.bodySmall, // Keeping typography as style, overriding color
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newUsername.isNotEmpty() && newUsername.length >= 2) {
                        if (newUsername != currentUsername) {
                            isLoading = true
                            errorMessage = null
                            onUpdateUsername(
                                newUsername,
                                { // onSuccess
                                    isLoading = false
                                    onDismiss()
                                },
                                { error -> // onError
                                    isLoading = false
                                    errorMessage = error
                                }
                            )
                        } else {
                            errorMessage = "新用户名不能与当前用户名相同"
                        }
                    } else {
                        errorMessage = "用户名长度至少为2位"
                    }
                },
                enabled = !isLoading && newUsername.isNotEmpty(),
                shape = androidx.compose.foundation.shape.CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor,
                    contentColor = Color.White,
                    disabledContainerColor = if (useDarkTheme) Color(0xFF3A3A3C) else Color(0xFFE5E5EA),
                    disabledContentColor = if (useDarkTheme) Color(0xFF636366) else Color(0xFFAEAEB2)
                ),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
            ) {
                if (isLoading) {
                    NemoChasingDotsLoader(size = 18.dp)
                } else {
                    Text(
                        text = "确认修改",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading,
                shape = androidx.compose.foundation.shape.CircleShape,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = bodyColor,
                    disabledContentColor = bodyColor.copy(alpha = 0.38f)
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "取消",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    )
}

@Composable
fun AccountUpdateEmailDialog(
    currentEmail: String,
    onDismiss: () -> Unit,
    onUpdateEmail: (String, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit,
    onVerifyEmailUpdate: (String, String, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit,
    useDarkTheme: Boolean = isSystemInDarkTheme()
) {
    // Stage: 0=NewEmail, 1=OTP
    var stage by remember { mutableIntStateOf(0) }
    var newEmail by remember { mutableStateOf(currentEmail) }
    var otp by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // UI/UX Pro Max Colors & Styles (Green Theme for Email)
    val primaryColor = if (useDarkTheme) Color(0xFF30D158) else Color(0xFF34C759)
    val containerColor = if (useDarkTheme) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)
    val titleColor = if (useDarkTheme) Color.White else Color.Black
    val bodyColor = if (useDarkTheme) Color(0xFF8E8E93) else Color(0xFF6E6E73)
    val focusedBorderColor = primaryColor
    val unfocusedBorderColor = if (useDarkTheme) Color(0xFF3A3A3C) else Color(0xFFC6C6C8)
    val errorColor = if (useDarkTheme) Color(0xFFFF453A) else Color(0xFFFF3B30)
    val selectionColors = androidx.compose.foundation.text.selection.TextSelectionColors(
        handleColor = primaryColor,
        backgroundColor = primaryColor.copy(alpha = 0.4f)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
        containerColor = containerColor,
        titleContentColor = titleColor,
        textContentColor = bodyColor,
        iconContentColor = primaryColor,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(26.dp),
        icon = {
            Icon(
                imageVector = Icons.Rounded.Email,
                contentDescription = null,
                tint = primaryColor,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = if (stage == 0) "修改邮箱" else "输入验证码",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = titleColor,
                letterSpacing = (-0.5).sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (stage == 0) {
                    Text(
                        text = "请输入新的邮箱地址，我们将发送验证码。",
                        fontSize = 15.sp,
                        color = bodyColor,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Normal
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    OutlinedTextField(
                        value = newEmail,
                        onValueChange = { newEmail = it },
                        label = {
                            Text(
                                "新邮箱",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        singleLine = true,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(26.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = titleColor,
                            unfocusedTextColor = titleColor,
                            disabledTextColor = bodyColor.copy(alpha = 0.38f),
                            errorTextColor = errorColor,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            errorContainerColor = Color.Transparent,
                            cursorColor = primaryColor,
                            errorCursorColor = errorColor,
                            selectionColors = selectionColors,
                            focusedBorderColor = focusedBorderColor,
                            unfocusedBorderColor = unfocusedBorderColor,
                            disabledBorderColor = unfocusedBorderColor.copy(alpha = 0.12f),
                            errorBorderColor = errorColor,
                            focusedLabelColor = primaryColor,
                            unfocusedLabelColor = bodyColor,
                            disabledLabelColor = bodyColor.copy(alpha = 0.38f),
                            errorLabelColor = errorColor
                        ),
                        isError = errorMessage != null
                    )
                } else {
                    Text(
                        text = "请输入发送至 $newEmail 的 8 位验证码。",
                        fontSize = 15.sp,
                        color = bodyColor,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Normal
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    OutlinedTextField(
                        value = otp,
                        onValueChange = { if (it.length <= 8) otp = it },
                        label = {
                            Text(
                                "验证码",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        singleLine = true,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(26.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = titleColor,
                            unfocusedTextColor = titleColor,
                            disabledTextColor = bodyColor.copy(alpha = 0.38f),
                            errorTextColor = errorColor,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            errorContainerColor = Color.Transparent,
                            cursorColor = primaryColor,
                            errorCursorColor = errorColor,
                            selectionColors = selectionColors,
                            focusedBorderColor = focusedBorderColor,
                            unfocusedBorderColor = unfocusedBorderColor,
                            disabledBorderColor = unfocusedBorderColor.copy(alpha = 0.12f),
                            errorBorderColor = errorColor,
                            focusedLabelColor = primaryColor,
                            unfocusedLabelColor = bodyColor,
                            disabledLabelColor = bodyColor.copy(alpha = 0.38f),
                            errorLabelColor = errorColor
                        ),
                        isError = errorMessage != null
                    )
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = "Error",
                            tint = errorColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = errorMessage ?: "",
                            color = errorColor,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    errorMessage = null
                    if (stage == 0) {
                        if (newEmail.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
                            if (newEmail != currentEmail) {
                                isLoading = true
                                onUpdateEmail(newEmail,
                                    { // onSuccess
                                        isLoading = false
                                        stage = 1
                                    },
                                    { error -> // onError
                                        isLoading = false
                                        errorMessage = error
                                    }
                                )
                            } else {
                                errorMessage = "新邮箱不能与当前邮箱相同"
                            }
                        } else {
                            errorMessage = "请输入有效的邮箱地址"
                        }
                    } else {
                        if (otp.length == 8) {
                            isLoading = true
                            onVerifyEmailUpdate(newEmail, otp,
                                { // onSuccess
                                    isLoading = false
                                    onDismiss()
                                },
                                { error -> // onError
                                    isLoading = false
                                    errorMessage = error
                                }
                            )
                        } else {
                            errorMessage = "请输入8位验证码"
                        }
                    }
                },
                enabled = !isLoading,
                shape = androidx.compose.foundation.shape.CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor,
                    contentColor = Color.White,
                    disabledContainerColor = if (useDarkTheme) Color(0xFF3A3A3C) else Color(0xFFE5E5EA),
                    disabledContentColor = if (useDarkTheme) Color(0xFF636366) else Color(0xFFAEAEB2)
                ),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
            ) {
                if (isLoading) {
                    NemoChasingDotsLoader(size = 18.dp)
                } else {
                    Text(
                        text = if (stage == 0) "下一步" else "确认修改",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading,
                shape = androidx.compose.foundation.shape.CircleShape,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = bodyColor,
                    disabledContentColor = bodyColor.copy(alpha = 0.38f)
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "取消",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    )
}

@Composable
fun DeleteAccountDialog(
    onDismiss: () -> Unit,
    onConfirmDelete: (String) -> Unit,
    useDarkTheme: Boolean = isSystemInDarkTheme()
) {
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showFinalConfirm by remember { mutableStateOf(false) }

    // UI/UX Pro Max Colors & Styles (Red Theme for Delete Account)
    val primaryColor = if (useDarkTheme) Color(0xFFFF453A) else Color(0xFFFF3B30)
    val containerColor = if (useDarkTheme) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)
    val titleColor = if (useDarkTheme) Color.White else Color.Black
    val bodyColor = if (useDarkTheme) Color(0xFF8E8E93) else Color(0xFF6E6E73)
    val focusedBorderColor = primaryColor
    val unfocusedBorderColor = if (useDarkTheme) Color(0xFF3A3A3C) else Color(0xFFC6C6C8)
    val errorColor = primaryColor // Red for error too
    val selectionColors = androidx.compose.foundation.text.selection.TextSelectionColors(
        handleColor = primaryColor,
        backgroundColor = primaryColor.copy(alpha = 0.4f)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = containerColor,
        titleContentColor = titleColor,
        textContentColor = bodyColor,
        iconContentColor = primaryColor,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(26.dp),
        icon = {
            Icon(
                imageVector = Icons.Rounded.Warning,
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = "删除账户",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = primaryColor, // Red Title for danger
                letterSpacing = (-0.5).sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (!showFinalConfirm) {
                    Text(
                        text = "删除账户是不可逆的操作，将永久删除您的所有数据，包括：",
                        fontSize = 15.sp,
                        color = bodyColor,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Normal
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "• 学习进度和统计数据\n• 测试记录和错题记录\n• 个人设置和偏好\n• 头像和用户信息",
                        fontSize = 14.sp,
                        color = bodyColor,
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "请输入您的密码以确认删除：",
                        fontSize = 15.sp,
                        color = bodyColor,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = {
                            Text(
                                "密码",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        singleLine = true,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(26.dp),
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = titleColor,
                            unfocusedTextColor = titleColor,
                            disabledTextColor = bodyColor.copy(alpha = 0.38f),
                            errorTextColor = errorColor,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            errorContainerColor = Color.Transparent,
                            cursorColor = primaryColor,
                            errorCursorColor = errorColor,
                            selectionColors = selectionColors,
                            focusedBorderColor = focusedBorderColor,
                            unfocusedBorderColor = unfocusedBorderColor,
                            disabledBorderColor = unfocusedBorderColor.copy(alpha = 0.12f),
                            errorBorderColor = errorColor,
                            focusedLabelColor = primaryColor,
                            unfocusedLabelColor = bodyColor,
                            disabledLabelColor = bodyColor.copy(alpha = 0.38f),
                            errorLabelColor = errorColor
                        )
                    )
                } else {
                    Text(
                        text = "最终确认",
                        fontSize = 18.sp,
                        color = primaryColor,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "您确定要永久删除您的账户吗？\n\n此操作无法撤销！",
                        fontSize = 16.sp,
                        color = primaryColor,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 24.sp
                    )
                }

                errorMessage?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = it,
                        color = errorColor,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!showFinalConfirm) {
                        if (password.isNotEmpty()) {
                            showFinalConfirm = true
                            errorMessage = null
                        } else {
                            errorMessage = "请输入密码"
                        }
                    } else {
                        isLoading = true
                        errorMessage = null
                        onConfirmDelete(password)
                    }
                },
                enabled = !isLoading && (showFinalConfirm || password.isNotEmpty()),
                shape = androidx.compose.foundation.shape.CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor,
                    contentColor = Color.White,
                    disabledContainerColor = if (useDarkTheme) Color(0xFF3A3A3C) else Color(0xFFE5E5EA),
                    disabledContentColor = if (useDarkTheme) Color(0xFF636366) else Color(0xFFAEAEB2)
                ),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
            ) {
                if (isLoading) {
                    NemoChasingDotsLoader(size = 18.dp)
                } else {
                    Text(
                        text = if (showFinalConfirm) "确认删除" else "下一步",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (showFinalConfirm) {
                        showFinalConfirm = false
                        errorMessage = null
                    } else {
                        onDismiss()
                    }
                },
                enabled = !isLoading,
                shape = androidx.compose.foundation.shape.CircleShape,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = bodyColor,
                    disabledContentColor = bodyColor.copy(alpha = 0.38f)
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = if (showFinalConfirm) "返回" else "取消",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    )
}

@Composable
fun LogoutWarningDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirmLogout: () -> Unit,
    useDarkTheme: Boolean
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = if (useDarkTheme) Color(0xFF1C1C1E) else Color.White,
            tonalElevation = 0.dp,
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            color = if (useDarkTheme) Color(0xFF2C2C2E) else Color(0xFFF2F2F7),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.WarningAmber,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = if (useDarkTheme) Color(0xFFFF453A) else Color(0xFFFF3B30)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "即将清除数据",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    textAlign = TextAlign.Center,
                    color = if (useDarkTheme) Color.White else Color.Black
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "退出登录将永久删除本机的学习进度和统计数据。此操作不可恢复。",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 22.sp
                    ),
                    textAlign = TextAlign.Center,
                    color = if (useDarkTheme) Color(0xFF8E8E93) else Color(0xFF636366)
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = onConfirmLogout,
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (useDarkTheme) Color(0xFFFF453A) else Color(0xFFFF3B30),
                        contentColor = Color.White,
                        disabledContainerColor = Color.Gray
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    if (isLoading) {
                        NemoChasingDotsLoader(size = 20.dp)
                    } else {
                        Text("直接退出", fontWeight = FontWeight.Bold)
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    enabled = !isLoading,
                    modifier = Modifier.height(44.dp),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFF8E8E93)
                    )
                ) {
                    Text("取消", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun DeleteAccountDialog(
    isOnlyGoogleIdentity: Boolean,
    userEmail: String,
    onDismiss: () -> Unit,
    onConfirmDelete: (String?) -> Unit,
    useDarkTheme: Boolean = isSystemInDarkTheme()
) {
    var input by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showFinalConfirm by remember { mutableStateOf(false) }

    // UI/UX Pro Max Colors & Styles (Red Theme for Delete Account)
    val primaryColor = if (useDarkTheme) Color(0xFFFF453A) else Color(0xFFFF3B30)
    val containerColor = if (useDarkTheme) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)
    val titleColor = if (useDarkTheme) Color.White else Color.Black
    val bodyColor = if (useDarkTheme) Color(0xFF8E8E93) else Color(0xFF6E6E73)
    val focusedBorderColor = primaryColor
    val unfocusedBorderColor = if (useDarkTheme) Color(0xFF3A3A3C) else Color(0xFFC6C6C8)
    val errorColor = primaryColor // Red for error too
    val selectionColors = androidx.compose.foundation.text.selection.TextSelectionColors(
        handleColor = primaryColor,
        backgroundColor = primaryColor.copy(alpha = 0.4f)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = containerColor,
        titleContentColor = titleColor,
        textContentColor = bodyColor,
        iconContentColor = primaryColor,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(26.dp),
        icon = {
            Icon(
                imageVector = Icons.Rounded.Warning,
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = "注销账户",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = primaryColor, // Red Title for danger
                letterSpacing = (-0.5).sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (!showFinalConfirm) {
                    Text(
                        text = "注销账户是不可逆的操作，将永久删除您的所有数据，包括：",
                        fontSize = 15.sp,
                        color = bodyColor,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Normal
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "• 学习进度和统计数据\n• 测试记录和错题记录\n• 个人设置和偏好\n• 头像和用户信息",
                        fontSize = 14.sp,
                        color = bodyColor,
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = if (isOnlyGoogleIdentity) "请输入当前邮箱以确认注销：" else "请输入您的密码以确认注销：",
                        fontSize = 15.sp,
                        color = bodyColor,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        label = {
                            Text(
                                text = if (isOnlyGoogleIdentity) "邮箱" else "密码",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        singleLine = true,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(26.dp),
                        visualTransformation = if (isOnlyGoogleIdentity) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (isOnlyGoogleIdentity) KeyboardType.Email else KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = titleColor,
                            unfocusedTextColor = titleColor,
                            disabledTextColor = bodyColor.copy(alpha = 0.38f),
                            errorTextColor = errorColor,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            errorContainerColor = Color.Transparent,
                            cursorColor = primaryColor,
                            errorCursorColor = errorColor,
                            selectionColors = selectionColors,
                            focusedBorderColor = focusedBorderColor,
                            unfocusedBorderColor = unfocusedBorderColor,
                            disabledBorderColor = unfocusedBorderColor.copy(alpha = 0.12f),
                            errorBorderColor = errorColor,
                            focusedLabelColor = primaryColor,
                            unfocusedLabelColor = bodyColor,
                            disabledLabelColor = bodyColor.copy(alpha = 0.38f),
                            errorLabelColor = errorColor
                        )
                    )
                } else {
                    Text(
                        text = "最终确认",
                        fontSize = 18.sp,
                        color = primaryColor,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "您确定要永久注销您的账户吗？\n\n此操作无法撤销！",
                        fontSize = 16.sp,
                        color = primaryColor,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 24.sp
                    )
                }

                errorMessage?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = it,
                        color = errorColor,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!showFinalConfirm) {
                        if (isOnlyGoogleIdentity) {
                            if (input.trim() == userEmail) {
                                showFinalConfirm = true
                                errorMessage = null
                            } else {
                                errorMessage = "邮箱输入不匹配，请重新输入"
                            }
                        } else {
                            if (input.isNotEmpty()) {
                                showFinalConfirm = true
                                errorMessage = null
                            } else {
                                errorMessage = "请输入密码"
                            }
                        }
                    } else {
                        isLoading = true
                        errorMessage = null
                        onConfirmDelete(if (isOnlyGoogleIdentity) null else input)
                    }
                },
                enabled = !isLoading && (showFinalConfirm || input.isNotEmpty()),
                shape = androidx.compose.foundation.shape.CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor,
                    contentColor = Color.White,
                    disabledContainerColor = if (useDarkTheme) Color(0xFF3A3A3C) else Color(0xFFE5E5EA),
                    disabledContentColor = if (useDarkTheme) Color(0xFF636366) else Color(0xFFAEAEB2)
                ),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
            ) {
                if (isLoading) {
                    NemoChasingDotsLoader(size = 18.dp)
                } else {
                    Text(
                        text = if (showFinalConfirm) "确认注销" else "下一步",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (showFinalConfirm) {
                        showFinalConfirm = false
                        errorMessage = null
                    } else {
                        onDismiss()
                    }
                },
                enabled = !isLoading,
                shape = androidx.compose.foundation.shape.CircleShape,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = bodyColor,
                    disabledContentColor = bodyColor.copy(alpha = 0.38f)
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = if (showFinalConfirm) "返回" else "取消",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    )
}

@Composable
fun UnlinkGoogleDialog(
    isLoading: Boolean,
    isOnlyGoogleIdentity: Boolean,
    onDismiss: () -> Unit,
    onConfirmUnlink: () -> Unit,
    onGoToSetPassword: () -> Unit,
    useDarkTheme: Boolean
) {
    if (isLoading) {
        Dialog(onDismissRequest = {}) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(Color.Transparent, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                NemoChasingDotsLoader()
            }
        }
        return
    }

    val primaryColor = if (isOnlyGoogleIdentity) (if (useDarkTheme) Color(0xFFFF9F0A) else Color(0xFFFF9500)) 
                       else (if (useDarkTheme) Color(0xFFFF453A) else Color(0xFFFF3B30))
    val containerColor = if (useDarkTheme) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)
    val titleColor = if (useDarkTheme) Color.White else Color.Black
    val bodyColor = if (useDarkTheme) Color(0xFF8E8E93) else Color(0xFF6E6E73)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = containerColor,
        titleContentColor = titleColor,
        textContentColor = bodyColor,
        iconContentColor = primaryColor,
        shape = RoundedCornerShape(26.dp),
        icon = {
            Icon(
                imageVector = if (isOnlyGoogleIdentity) Icons.Rounded.Warning else Icons.Rounded.LinkOff,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = primaryColor
            )
        },
        title = {
            Text(
                text = if (isOnlyGoogleIdentity) "账号风险警告" else "确认解绑",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = titleColor,
                letterSpacing = (-0.5).sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Text(
                text = if (isOnlyGoogleIdentity) 
                    "系统检测到您当前仅使用了 Google 快捷登录，尚未设置密码。\n\n如果现在解绑，您将无法再次登录此账号！建议您先设置密码。" 
                else 
                    "解绑后将无法通过此 Google 账号快捷登录，确定要解绑吗？",
                fontSize = 15.sp,
                color = bodyColor,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            if (isOnlyGoogleIdentity) {
                Button(
                    onClick = onGoToSetPassword,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
                ) {
                    Text("去设置密码", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Button(
                    onClick = onConfirmUnlink,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
                ) {
                    Text("确认解绑", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = CircleShape,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = bodyColor
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = if (isOnlyGoogleIdentity) "我再想想" else "取消",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    )
}
