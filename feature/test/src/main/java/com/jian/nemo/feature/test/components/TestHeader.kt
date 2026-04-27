package com.jian.nemo.feature.test.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.feature.test.presentation.theme.TestDanger
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jian.nemo.core.ui.component.common.NemoDropdownMenu
import com.jian.nemo.core.ui.component.common.NemoMenuItem

/**
 * 测试头部组件（包含返回按钮、倒计时和收藏按钮）
 * 复刻旧项目 com.jian.nemo.ui.screen.test.components.TestHeader
 */
import com.jian.nemo.core.domain.model.Grammar

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
                contentDescription = "返回菜单",
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
                color = if (isRunningOut) TestDanger else MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp),
                fontFamily = FontFamily.Monospace
            )
        }

        // 菜单入口按钮 (替代之前的收藏按钮)
        val isFavorite = word?.isFavorite == true || grammar?.isFavorite == true
        val itemId = word?.id ?: grammar?.id ?: 0

        Box {
            var expanded by remember { mutableStateOf(false) }

            IconButton(
                onClick = { expanded = true },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "更多选项",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(24.dp)
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

    // 添加底部边框
    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
}
