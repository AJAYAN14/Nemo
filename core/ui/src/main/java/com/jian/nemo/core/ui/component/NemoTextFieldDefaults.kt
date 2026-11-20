package com.jian.nemo.core.ui.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.jian.nemo.core.designsystem.theme.NemoInputFillDark
import com.jian.nemo.core.designsystem.theme.NemoInputFillLight

/**
 * 输入框通用样式配置
 */
object NemoTextFieldDefaults {

    @Composable
    fun colors(
        isDark: Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f,
        containerColor: Color = if (isDark) NemoInputFillDark else NemoInputFillLight,
        focusedBorderColor: Color = Color.Transparent,
        unfocusedBorderColor: Color = Color.Transparent,
        disabledBorderColor: Color = Color.Transparent,
        errorBorderColor: Color = MaterialTheme.colorScheme.error,
        focusedTextColor: Color = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor: Color = MaterialTheme.colorScheme.onSurface,
        disabledTextColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        errorTextColor: Color = MaterialTheme.colorScheme.onSurface,
        cursorColor: Color = MaterialTheme.colorScheme.primary,
        errorCursorColor: Color = MaterialTheme.colorScheme.error,
        focusedPlaceholderColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        unfocusedPlaceholderColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        disabledPlaceholderColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
        errorPlaceholderColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        focusedLeadingIconColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        unfocusedLeadingIconColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedTrailingIconColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        unfocusedTrailingIconColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
    ): TextFieldColors {
        return OutlinedTextFieldDefaults.colors(
            focusedTextColor = focusedTextColor,
            unfocusedTextColor = unfocusedTextColor,
            disabledTextColor = disabledTextColor,
            errorTextColor = errorTextColor,
            focusedContainerColor = containerColor,
            unfocusedContainerColor = containerColor,
            disabledContainerColor = containerColor,
            errorContainerColor = containerColor,
            cursorColor = cursorColor,
            errorCursorColor = errorCursorColor,
            focusedBorderColor = focusedBorderColor,
            unfocusedBorderColor = unfocusedBorderColor,
            disabledBorderColor = disabledBorderColor,
            errorBorderColor = errorBorderColor,
            focusedLeadingIconColor = focusedLeadingIconColor,
            unfocusedLeadingIconColor = unfocusedLeadingIconColor,
            focusedTrailingIconColor = focusedTrailingIconColor,
            unfocusedTrailingIconColor = unfocusedTrailingIconColor,
            focusedPlaceholderColor = focusedPlaceholderColor,
            unfocusedPlaceholderColor = unfocusedPlaceholderColor,
            disabledPlaceholderColor = disabledPlaceholderColor,
            errorPlaceholderColor = errorPlaceholderColor
        )
    }

    @Composable
    fun filledColors(
        isDark: Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f,
        containerColor: Color = if (isDark) NemoInputFillDark else NemoInputFillLight,
        focusedIndicatorColor: Color = Color.Transparent,
        unfocusedIndicatorColor: Color = Color.Transparent,
        disabledIndicatorColor: Color = Color.Transparent,
        errorIndicatorColor: Color = MaterialTheme.colorScheme.error,
        focusedTextColor: Color = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor: Color = MaterialTheme.colorScheme.onSurface,
        disabledTextColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        errorTextColor: Color = MaterialTheme.colorScheme.onSurface,
        cursorColor: Color = MaterialTheme.colorScheme.primary,
        errorCursorColor: Color = MaterialTheme.colorScheme.error,
        focusedPlaceholderColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        unfocusedPlaceholderColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        disabledPlaceholderColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
        errorPlaceholderColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        focusedLabelColor: Color = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        disabledLabelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
        errorLabelColor: Color = MaterialTheme.colorScheme.error,
        focusedLeadingIconColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        unfocusedLeadingIconColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedTrailingIconColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        unfocusedTrailingIconColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
    ): TextFieldColors {
        return TextFieldDefaults.colors(
            focusedTextColor = focusedTextColor,
            unfocusedTextColor = unfocusedTextColor,
            disabledTextColor = disabledTextColor,
            errorTextColor = errorTextColor,
            focusedContainerColor = containerColor,
            unfocusedContainerColor = containerColor,
            disabledContainerColor = containerColor,
            errorContainerColor = containerColor,
            cursorColor = cursorColor,
            errorCursorColor = errorCursorColor,
            focusedIndicatorColor = focusedIndicatorColor,
            unfocusedIndicatorColor = unfocusedIndicatorColor,
            disabledIndicatorColor = disabledIndicatorColor,
            errorIndicatorColor = errorIndicatorColor,
            focusedLeadingIconColor = focusedLeadingIconColor,
            unfocusedLeadingIconColor = unfocusedLeadingIconColor,
            focusedTrailingIconColor = focusedTrailingIconColor,
            unfocusedTrailingIconColor = unfocusedTrailingIconColor,
            focusedPlaceholderColor = focusedPlaceholderColor,
            unfocusedPlaceholderColor = unfocusedPlaceholderColor,
            disabledPlaceholderColor = disabledPlaceholderColor,
            errorPlaceholderColor = errorPlaceholderColor,
            focusedLabelColor = focusedLabelColor,
            unfocusedLabelColor = unfocusedLabelColor,
            disabledLabelColor = disabledLabelColor,
            errorLabelColor = errorLabelColor
        )
    }
}
