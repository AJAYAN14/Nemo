package com.jian.nemo.feature.test.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import com.jian.nemo.feature.test.R
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.domain.model.Grammar
import com.jian.nemo.core.designsystem.theme.NemoDanger
import com.jian.nemo.core.designsystem.theme.IosColors
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.luminance
import com.jian.nemo.core.ui.component.common.NemoLiquidMenuButton
import com.jian.nemo.core.ui.component.common.NemoNativeLiquidMenuTrigger

/**
 * 测试头部组件（包含返回按钮、倒计时和操作菜单）
 *
 * 遵循单一官方原生图标与调色盘标准规范：
 * - 统一使用 Google Fonts 官方原生实心 Rounded 图标
 * - 配色体系全面收编至设计调色盘 IosColors (一套颜色体系)
 */
@Composable
fun TestHeader(
    onBack: () -> Unit,
    timeLimitSeconds: Int,
    timeRemainingSeconds: Int,
    word: Word?,
    grammar: Grammar? = null,
    onToggleFavorite: (Int, Boolean) -> Unit,
    onPause: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 返回按钮
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.test_header_cd_back_menu),
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(22.dp)
            )
        }

        // 添加倒计时显示
        if (timeLimitSeconds > 0) {
            val minutes = timeRemainingSeconds / 60
            val seconds = timeRemainingSeconds % 60
            val isRunningOut = timeRemainingSeconds < 60
            Text(
                text = "%02d:%02d".format(minutes, seconds),
                color = if (isRunningOut) NemoDanger else MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp),
                fontFamily = FontFamily.Monospace
            )
        }

        // 菜单入口按钮 (圆润三横平滑过渡到 ✕ 弹性动效，原生物理液态菜单引擎)
        val isFavorite = word?.isFavorite == true || grammar?.isFavorite == true
        val itemId = word?.id ?: grammar?.id ?: 0

        var menuExpanded by remember { mutableStateOf(false) }

        TestMenuOverlay(
            isVisible = menuExpanded,
            onDismiss = { menuExpanded = false },
            isFavorite = isFavorite,
            itemId = itemId,
            onToggleFavorite = onToggleFavorite,
            onPause = onPause,
            backdrop = null
        )

        val items = ActiveNativeTestMenuItems.value
        val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f

        if (items.isNotEmpty()) {
            NemoNativeLiquidMenuTrigger(
                items = items,
                buttonSize = 48.dp,
                colorScheme = if (isDarkTheme) "dark" else "light",
                menuWidth = 260.dp,
                onSelect = { item, _ ->
                    ActiveNativeTestMenuActionHandler.value?.invoke(item.id)
                },
                onOpenChange = { open ->
                    ActiveNativeTestMenuOpenState.value = open
                    menuExpanded = open
                }
            )
        } else {
            NemoLiquidMenuButton(
                isExpanded = menuExpanded,
                onClick = { menuExpanded = !menuExpanded },
                backdrop = null,
                buttonSize = 48.dp
            )
        }
    }

    // 添加底部边框
    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
}
