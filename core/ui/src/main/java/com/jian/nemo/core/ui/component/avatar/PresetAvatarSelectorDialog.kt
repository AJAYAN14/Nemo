package com.jian.nemo.core.ui.component.avatar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.jian.nemo.core.ui.R
import com.jian.nemo.core.ui.component.NemoDialog
import com.jian.nemo.core.ui.util.PresetAvatars

/**
 * 预设头像选择对话框
 */
@Composable
fun PresetAvatarSelectorDialog(
    onDismiss: () -> Unit,
    onPresetSelected: (PresetAvatars.PresetAvatar) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedPreset by remember { mutableStateOf<PresetAvatars.PresetAvatar?>(null) }

    NemoDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.core_ui_preset_selector_dialog_title),
        confirmText = stringResource(R.string.core_ui_dialog_btn_confirm),
        dismissText = stringResource(R.string.core_ui_dialog_btn_cancel),
        confirmEnabled = selectedPreset != null,
        onConfirm = {
            selectedPreset?.let { onPresetSelected(it) }
            onDismiss()
        },
        content = {
            Column {
                Text(
                    text = stringResource(R.string.core_ui_preset_selector_dialog_subtitle),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // 预设头像网格
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.height(280.dp)
                ) {
                    items(PresetAvatars.presets) { preset ->
                        PresetAvatarItem(
                            preset = preset,
                            isSelected = preset == selectedPreset,
                            onClick = { selectedPreset = preset }
                        )
                    }
                }
            }
        }
    )
}

/**
 * 预设头像项
 */
@Composable
private fun PresetAvatarItem(
    preset: PresetAvatars.PresetAvatar,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)  // ✅ 保持正方形比例，避免变形
            .clip(CircleShape)
            .then(
                if (isSelected) {
                    Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                } else {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), CircleShape)
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // 渐变背景
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isSelected) 3.dp else 1.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = preset.colors
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            // 显示emoji（如果有）
            preset.emoji?.let { emoji ->
                Text(
                    text = emoji,
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
