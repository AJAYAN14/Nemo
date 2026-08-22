package com.jian.nemo.feature.test.presentation.settings.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 通用单选列表项数据模型
 *
 * @param key 选项唯一标识 (Key)
 * @param title 粗体主标题 (如 "今日学习的内容")
 * @param subtitle 小号浅灰辅助文本 (如 "30 词 · 0 语法" 或 规则说明)
 * @param isEnabled 是否可选。若为 false 则整行置灰且不可点击
 */
data class SingleSelectOptionItem<T>(
    val key: T,
    val title: String,
    val subtitle: String? = null,
    val isEnabled: Boolean = true
)

/**
 * 现代双行信息列表式半屏单选抽屉组件
 *
 * 特性：
 * 1. 顶部居中灰色 Pull Indicator（拖拽条）+ 右上角关闭按钮
 * 2. 双行信息卡片（粗体主标题 + 浅灰辅助文本，解耦无括号挤压）
 * 3. 选中态：主题色弱背景 (8% alpha) + 主题色高亮边框 + Checkmark 勾选图标
 * 4. 禁用态：置灰 (0.35f alpha) + 不可点击
 * 5. 交互：单选切换高亮，点击底部“确定”按钮生效并关闭
 * 6. 全面适配深浅色，跟随 App 整体配色规范
 */
@Composable
fun <T> SingleSelectBottomSheet(
    title: String,
    options: List<SingleSelectOptionItem<T>>,
    selectedKey: T,
    onConfirm: (T) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    confirmButtonText: String = "完成"
) {
    var tempSelectedKey by remember(selectedKey) { mutableStateOf(selectedKey) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 16.dp)
    ) {
        // 1. 顶部居中 Pull Indicator
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f))
            )
        }

        // 2. 标题栏：主标题靠左 + 右上角关闭图标
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 22.dp, end = 14.dp, top = 4.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp,
                    letterSpacing = (-0.4).sp,
                    lineHeight = 26.sp
                ),
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "关闭",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // 3. 双行信息选项列表
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(options, key = { it.key.toString() }) { item ->
                val isSelected = item.key == tempSelectedKey
                SingleSelectCardRow(
                    item = item,
                    isSelected = isSelected,
                    onClick = {
                        if (item.isEnabled) {
                            tempSelectedKey = item.key
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 4. 底部确定主操作按钮
        Button(
            onClick = {
                onConfirm(tempSelectedKey)
                onDismiss()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(
                text = confirmButtonText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * 独立的双行信息卡片行
 */
@Composable
private fun <T> SingleSelectCardRow(
    item: SingleSelectOptionItem<T>,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val themePrimary = MaterialTheme.colorScheme.primary
    val isEnabled = item.isEnabled

    val backgroundColor by animateColorAsState(
        targetValue = when {
            !isEnabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
            isSelected -> themePrimary.copy(alpha = 0.08f)
            else -> MaterialTheme.colorScheme.surface
        },
        label = "option_card_bg"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            !isEnabled -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
            isSelected -> themePrimary.copy(alpha = 0.85f)
            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        },
        label = "option_card_border"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                enabled = isEnabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = themePrimary.copy(alpha = 0.15f)),
                onClick = onClick
            ),
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        border = BorderStroke(if (isSelected) 1.5.dp else 0.8.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp)
                .alpha(if (isEnabled) 1f else 0.38f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 左侧：主标题 + 辅助副文本
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSelected) themePrimary else MaterialTheme.colorScheme.onSurface
                )

                if (!item.subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = item.subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = if (isSelected) themePrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                    )
                }
            }

            // 右侧：选中对勾
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(themePrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "已选中",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
