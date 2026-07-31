package com.jian.nemo.feature.test.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jian.nemo.core.domain.model.Grammar
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.ui.component.common.NemoDropdownMenu
import com.jian.nemo.core.ui.component.common.NemoMenuItem
import com.jian.nemo.core.ui.component.liquid.LiquidButton
import com.jian.nemo.core.ui.modifier.softCardShadow
import com.jian.nemo.feature.test.presentation.theme.TestDanger

/**
 * 测试头部中间内容：倒计时显示（液态按钮容器）
 *
 * 供 CommonHeader 的 centerContent 插槽使用。
 * 当 timeLimitSeconds > 0 时显示倒计时，时间 < 60 秒时文字变红。
 */
@Composable
fun TestHeaderCenterContent(
    timeLimitSeconds: Int,
    timeRemainingSeconds: Int
) {
    if (timeLimitSeconds <= 0) return

    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5
    val navGroupBg = if (isDarkTheme) Color.White.copy(alpha = 0.15f) else Color.White

    val minutes = timeRemainingSeconds / 60
    val seconds = timeRemainingSeconds % 60
    val isRunningOut = timeRemainingSeconds < 60

    LiquidButton(
        onClick = { /* 倒计时按钮不触发操作 */ },
        backgroundColor = navGroupBg,
        shape = CircleShape,
        elevation = 0.dp,
        isInteractive = false,
        modifier = Modifier
            .softCardShadow(borderRadius = 22.dp, isDark = isDarkTheme)
    ) {
        Text(
            text = "%02d:%02d".format(minutes, seconds),
            color = if (isRunningOut) TestDanger else MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

/**
 * 测试头部右侧操作区：菜单按钮（液态按钮 + 下拉菜单）
 *
 * 供 CommonHeader 的 actions 插槽使用。
 * 包含收藏/取消收藏与暂停测试两个菜单项。
 */
@Composable
fun TestHeaderActions(
    word: Word?,
    grammar: Grammar? = null,
    onToggleFavorite: (Int, Boolean) -> Unit,
    onPause: () -> Unit
) {
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5
    val navGroupBg = if (isDarkTheme) Color.White.copy(alpha = 0.15f) else Color.White

    val isFavorite = word?.isFavorite == true || grammar?.isFavorite == true
    val itemId = word?.id ?: grammar?.id ?: 0

    Box {
        var expanded by remember { mutableStateOf(false) }

        LiquidButton(
            onClick = { expanded = true },
            backgroundColor = navGroupBg,
            shape = CircleShape,
            elevation = 0.dp,
            isInteractive = true,
            modifier = Modifier
                .softCardShadow(borderRadius = 22.dp, isDark = isDarkTheme)
                .size(44.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "更多选项",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        NemoDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            // 收藏选项 (保留业务逻辑)
            if (itemId != 0) {
                NemoMenuItem(
                    text = if (isFavorite) "取消收藏" else "收藏",
                    onClick = {
                        expanded = false
                        onToggleFavorite(itemId, !isFavorite)
                    },
                    leadingIcon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder
                )
            }

            // 暂停测试选项
            NemoMenuItem(
                text = "暂停测试",
                onClick = {
                    expanded = false
                    onPause()
                },
                leadingIcon = Icons.Rounded.Pause
            )
        }
    }
}
