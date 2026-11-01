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
import com.jian.nemo.core.designsystem.shape.RoundedRectangle
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.nemo.feature.user.R
import androidx.compose.ui.graphics.luminance
import com.jian.nemo.core.ui.component.animation.NemoChasingDotsLoader
import com.jian.nemo.core.ui.component.NemoDialog
import com.jian.nemo.core.designsystem.theme.IosColors
import com.jian.nemo.core.designsystem.theme.UserPalette
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
    useDarkTheme: Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f
) {
    var stage by remember { mutableIntStateOf(0) }
    var email by remember { mutableStateOf(userEmail) }
    var otp by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val bodyColor = if (useDarkTheme) UserPalette.DialogBodyDark else UserPalette.DialogBodyLight
    val errorColor = if (useDarkTheme) IosColors.RedDark else IosColors.Red
    val errInvalidEmail = stringResource(R.string.user_dialog_err_invalid_email)
    val errInvalidOtp = stringResource(R.string.user_dialog_err_invalid_otp)
    val errShortPassword = stringResource(R.string.user_dialog_err_short_password)

    com.jian.nemo.core.ui.component.NemoDialog(
        onDismissRequest = onDismiss,
        title = when (stage) {
            0 -> stringResource(R.string.user_dialog_title_reset_password)
            1 -> stringResource(R.string.user_dialog_title_input_otp)
            2 -> stringResource(R.string.user_dialog_title_set_new_password)
            else -> stringResource(R.string.user_dialog_title_reset_password)
        },
        confirmText = if (stage == 2) stringResource(R.string.user_dialog_title_reset_password) else stringResource(R.string.user_dialog_btn_next),
        dismissText = stringResource(R.string.user_dialog_btn_cancel),
        confirmButtonColor = if (useDarkTheme) IosColors.OrangeDark else IosColors.Orange,
        isLoading = isLoading,
        onConfirm = {
            errorMessage = null
            when (stage) {
                0 -> {
                    if (email.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        isLoading = true
                        onSendOtp(
                            email,
                            {
                                isLoading = false
                                stage = 1
                            },
                            { error ->
                                isLoading = false
                                errorMessage = error
                            }
                        )
                    } else {
                        errorMessage = errInvalidEmail
                    }
                }
                1 -> {
                    if (otp.length == 8) {
                        isLoading = true
                        onVerifyOtp(
                            email, otp,
                            {
                                isLoading = false
                                stage = 2
                            },
                            { error ->
                                isLoading = false
                                errorMessage = error
                            }
                        )
                    } else {
                        errorMessage = errInvalidOtp
                    }
                }
                2 -> {
                    if (newPassword.length >= 8) {
                        isLoading = true
                        onResetPassword(
                            newPassword,
                            {
                                isLoading = false
                                onDismiss()
                            },
                            { error ->
                                isLoading = false
                                errorMessage = error
                            }
                        )
                    } else {
                        errorMessage = errShortPassword
                    }
                }
            }
        },
        content = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (stage == 0) {
                    Text(
                        text = stringResource(R.string.user_dialog_msg_reset_pwd_send_otp),
                        fontSize = 15.sp,
                        color = bodyColor,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text(stringResource(R.string.user_dialog_label_email), fontSize = 14.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        singleLine = true,
                        shape = RoundedRectangle(20.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        isError = errorMessage != null
                    )
                } else if (stage == 1) {
                    Text(
                        text = stringResource(R.string.user_dialog_msg_input_otp_format, email),
                        fontSize = 15.sp,
                        color = bodyColor,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = otp,
                        onValueChange = { if (it.length <= 8) otp = it },
                        label = { Text(stringResource(R.string.user_dialog_label_otp), fontSize = 14.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        singleLine = true,
                        shape = RoundedRectangle(20.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        isError = errorMessage != null
                    )
                } else {
                    Text(
                        text = stringResource(R.string.user_dialog_msg_otp_verified),
                        fontSize = 15.sp,
                        color = bodyColor,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text(stringResource(R.string.user_dialog_label_new_password), fontSize = 14.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        singleLine = true,
                        shape = RoundedRectangle(20.dp),
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        isError = errorMessage != null
                    )
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = errorColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    )
}

@Composable
fun AccountUpdateUsernameDialog(
    currentUsername: String,
    onDismiss: () -> Unit,
    onUpdateUsername: (String, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit,
    useDarkTheme: Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f
) {
    var newUsername by remember { mutableStateOf(currentUsername) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val bodyColor = if (useDarkTheme) UserPalette.DialogBodyDark else UserPalette.DialogBodyLight
    val errorColor = if (useDarkTheme) IosColors.RedDark else IosColors.Red
    val errSameUsername = stringResource(R.string.user_dialog_err_same_username)
    val errUsernameLength = stringResource(R.string.user_dialog_err_username_length)

    com.jian.nemo.core.ui.component.NemoDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.user_dialog_title_change_username),
        confirmText = stringResource(R.string.user_dialog_btn_confirm_modify),
        dismissText = stringResource(R.string.user_dialog_btn_cancel),
        confirmButtonColor = if (useDarkTheme) IosColors.BlueDark else IosColors.Blue,
        confirmEnabled = newUsername.trim().isNotEmpty() && newUsername.trim().length <= 16 && newUsername.trim() != currentUsername,
        isLoading = isLoading,
        onConfirm = {
            val trimmed = newUsername.trim()
            if (trimmed.isNotEmpty() && trimmed.length <= 16) {
                if (trimmed != currentUsername) {
                    isLoading = true
                    errorMessage = null
                    onUpdateUsername(
                        trimmed,
                        {
                            isLoading = false
                            onDismiss()
                        },
                        { error ->
                            isLoading = false
                            errorMessage = error
                        }
                    )
                } else {
                    errorMessage = errSameUsername
                }
            } else {
                errorMessage = errUsernameLength
            }
        },
        content = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.user_dialog_msg_change_username),
                    fontSize = 15.sp,
                    color = bodyColor,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = newUsername,
                    onValueChange = { if (it.length <= 16) newUsername = it },
                    label = { Text(stringResource(R.string.user_dialog_label_new_username), fontSize = 14.sp) },
                    supportingText = {
                        Text(
                            text = stringResource(R.string.user_dialog_format_char_count, newUsername.length),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.End,
                            color = bodyColor,
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    singleLine = true,
                    shape = RoundedRectangle(20.dp),
                    isError = errorMessage != null
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = errorColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
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
    useDarkTheme: Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f
) {
    var stage by remember { mutableIntStateOf(0) }
    var newEmail by remember { mutableStateOf(currentEmail) }
    var otp by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val bodyColor = if (useDarkTheme) UserPalette.DialogBodyDark else UserPalette.DialogBodyLight
    val errorColor = if (useDarkTheme) IosColors.RedDark else IosColors.Red
    val errSameEmail = stringResource(R.string.user_dialog_err_same_email)
    val errInvalidEmail = stringResource(R.string.user_dialog_err_invalid_email)
    val errInvalidOtp = stringResource(R.string.user_dialog_err_invalid_otp)

    com.jian.nemo.core.ui.component.NemoDialog(
        onDismissRequest = onDismiss,
        title = if (stage == 0) stringResource(R.string.user_dialog_title_change_email) else stringResource(R.string.user_dialog_title_input_otp),
        confirmText = if (stage == 0) stringResource(R.string.user_dialog_btn_next) else stringResource(R.string.user_dialog_btn_confirm_modify),
        dismissText = stringResource(R.string.user_dialog_btn_cancel),
        confirmButtonColor = if (useDarkTheme) IosColors.GreenDark else IosColors.Green,
        isLoading = isLoading,
        onConfirm = {
            errorMessage = null
            if (stage == 0) {
                if (newEmail.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
                    if (newEmail != currentEmail) {
                        isLoading = true
                        onUpdateEmail(
                            newEmail,
                            {
                                isLoading = false
                                stage = 1
                            },
                            { error ->
                                isLoading = false
                                errorMessage = error
                            }
                        )
                    } else {
                        errorMessage = errSameEmail
                    }
                } else {
                    errorMessage = errInvalidEmail
                }
            } else {
                if (otp.length == 8) {
                    isLoading = true
                    onVerifyEmailUpdate(
                        newEmail, otp,
                        {
                            isLoading = false
                            onDismiss()
                        },
                        { error ->
                            isLoading = false
                            errorMessage = error
                        }
                    )
                } else {
                    errorMessage = errInvalidOtp
                }
            }
        },
        content = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (stage == 0) {
                    Text(
                        text = stringResource(R.string.user_dialog_msg_change_email_prompt),
                        fontSize = 15.sp,
                        color = bodyColor,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = newEmail,
                        onValueChange = { newEmail = it },
                        label = { Text(stringResource(R.string.user_dialog_label_new_email), fontSize = 14.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        singleLine = true,
                        shape = RoundedRectangle(20.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        isError = errorMessage != null
                    )
                } else {
                    Text(
                        text = stringResource(R.string.user_dialog_msg_input_otp_format, newEmail),
                        fontSize = 15.sp,
                        color = bodyColor,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = otp,
                        onValueChange = { if (it.length <= 8) otp = it },
                        label = { Text(stringResource(R.string.user_dialog_label_otp), fontSize = 14.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        singleLine = true,
                        shape = RoundedRectangle(20.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        isError = errorMessage != null
                    )
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = errorColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    )
}

@Composable
fun DeleteAccountDialog(
    onDismiss: () -> Unit,
    onConfirmDelete: (String) -> Unit,
    useDarkTheme: Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f
) {
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showFinalConfirm by remember { mutableStateOf(false) }

    val primaryColor = if (useDarkTheme) IosColors.RedDark else IosColors.Red
    val bodyColor = if (useDarkTheme) UserPalette.DialogBodyDark else UserPalette.DialogBodyLight
    val errEmptyPassword = stringResource(R.string.user_dialog_err_empty_password)

    com.jian.nemo.core.ui.component.NemoDialog(
        onDismissRequest = {
            if (showFinalConfirm) {
                showFinalConfirm = false
                errorMessage = null
            } else {
                onDismiss()
            }
        },
        title = if (showFinalConfirm) stringResource(R.string.user_dialog_title_final_confirm_delete) else stringResource(R.string.user_dialog_title_delete_user_account),
        isDangerous = true,
        confirmText = if (showFinalConfirm) stringResource(R.string.user_dialog_btn_confirm_delete_account) else stringResource(R.string.user_dialog_btn_next),
        dismissText = if (showFinalConfirm) stringResource(R.string.user_dialog_btn_back) else stringResource(R.string.user_dialog_btn_cancel),
        confirmEnabled = showFinalConfirm || password.isNotEmpty(),
        isLoading = isLoading,
        onConfirm = {
            if (!showFinalConfirm) {
                if (password.isNotEmpty()) {
                    showFinalConfirm = true
                    errorMessage = null
                } else {
                    errorMessage = errEmptyPassword
                }
            } else {
                isLoading = true
                errorMessage = null
                onConfirmDelete(password)
            }
        },
        content = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (!showFinalConfirm) {
                    Text(
                        text = stringResource(R.string.user_dialog_msg_delete_account_warning),
                        fontSize = 15.sp,
                        color = bodyColor,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = stringResource(R.string.user_dialog_msg_delete_details),
                        fontSize = 14.sp,
                        color = bodyColor,
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(stringResource(R.string.user_dialog_label_verify_password), fontSize = 14.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        singleLine = true,
                        shape = RoundedRectangle(20.dp),
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        )
                    )
                } else {
                    Text(
                        text = stringResource(R.string.user_dialog_msg_delete_account_final),
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
                        color = primaryColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    )
}

@Composable
fun LogoutWarningDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirmLogout: () -> Unit,
    useDarkTheme: Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f
) {
    com.jian.nemo.core.ui.component.NemoDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.user_dialog_title_logout_clear_data),
        text = stringResource(R.string.user_dialog_msg_logout_clear_data),
        isDangerous = true,
        confirmText = stringResource(R.string.user_dialog_btn_direct_logout),
        dismissText = stringResource(R.string.user_dialog_btn_cancel),
        isLoading = isLoading,
        onConfirm = onConfirmLogout
    )
}

@Composable
fun DeleteAccountDialog(
    isOnlyGoogleIdentity: Boolean,
    userEmail: String,
    onDismiss: () -> Unit,
    onConfirmDelete: (String?) -> Unit,
    useDarkTheme: Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f
) {
    var input by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showFinalConfirm by remember { mutableStateOf(false) }

    val primaryColor = if (useDarkTheme) IosColors.RedDark else IosColors.Red
    val containerColor = if (useDarkTheme) UserPalette.DialogContainerDark else Color.White
    val titleColor = if (useDarkTheme) Color.White else Color.Black
    val bodyColor = if (useDarkTheme) UserPalette.DialogBodyDark else UserPalette.DialogBodyLight
    val focusedBorderColor = primaryColor
    val unfocusedBorderColor = if (useDarkTheme) UserPalette.DialogBorderDark else UserPalette.DialogBorderLight
    val errorColor = primaryColor
    val errEmailMismatch = stringResource(R.string.user_dialog_err_email_mismatch)
    val errEmptyPassword = stringResource(R.string.user_dialog_err_empty_password)
    val selectionColors = androidx.compose.foundation.text.selection.TextSelectionColors(
        handleColor = primaryColor,
        backgroundColor = primaryColor.copy(alpha = 0.4f)
    )

    NemoDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.user_dialog_title_delete_account),
        isDangerous = true,
        confirmText = if (showFinalConfirm) stringResource(R.string.user_dialog_btn_confirm_delete) else stringResource(R.string.user_dialog_btn_next),
        dismissText = if (showFinalConfirm) stringResource(R.string.user_dialog_btn_back) else stringResource(R.string.user_dialog_btn_cancel),
        confirmEnabled = !isLoading && (showFinalConfirm || input.isNotEmpty()),
        isLoading = isLoading,
        onConfirm = {
            if (!showFinalConfirm) {
                if (isOnlyGoogleIdentity) {
                    if (input.trim() == userEmail) {
                        showFinalConfirm = true
                        errorMessage = null
                    } else {
                        errorMessage = errEmailMismatch
                    }
                } else {
                    if (input.isNotEmpty()) {
                        showFinalConfirm = true
                        errorMessage = null
                    } else {
                        errorMessage = errEmptyPassword
                    }
                }
            } else {
                isLoading = true
                errorMessage = null
                onConfirmDelete(if (isOnlyGoogleIdentity) null else input)
            }
        },
        onDismiss = {
            if (showFinalConfirm) {
                showFinalConfirm = false
                errorMessage = null
            } else {
                onDismiss()
            }
        },
        content = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (!showFinalConfirm) {
                    Text(
                        text = stringResource(R.string.user_dialog_msg_delete_warning),
                        fontSize = 15.sp,
                        color = bodyColor,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Normal
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = stringResource(R.string.user_dialog_msg_delete_details),
                        fontSize = 14.sp,
                        color = bodyColor,
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = if (isOnlyGoogleIdentity) stringResource(R.string.user_dialog_msg_delete_prompt_email) else stringResource(R.string.user_dialog_msg_delete_prompt_password),
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
                                text = if (isOnlyGoogleIdentity) stringResource(R.string.user_dialog_label_email) else stringResource(R.string.user_dialog_label_verify_password),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        singleLine = true,
                        shape = RoundedRectangle(26.dp),
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
                        text = stringResource(R.string.user_dialog_title_final_confirm),
                        fontSize = 18.sp,
                        color = primaryColor,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.user_dialog_msg_delete_final_confirm),
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
        }
    )
}
