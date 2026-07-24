package com.jian.nemo.feature.settings.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CallMerge
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.nemo.core.data.manager.ImportStrategy

/**
 * 恢复策略选择弹窗
 *
 * 用户在从云端恢复数据时，需要选择合并数据还是覆盖数据。
 * 已重新设计以提供更高级的视觉体验。
 */
@Composable
fun RestoreStrategyDialog(
    fileName: String,
    onConfirm: (ImportStrategy) -> Unit,
    onDismiss: () -> Unit
) {
    com.jian.nemo.core.ui.component.NemoDialog(
        onDismissRequest = onDismiss,
        title = "选择恢复方式",
        confirmText = null,
        dismissText = "取消",
        content = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "即将恢复备份: $fileName\n请选择以下数据处理方式：",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Merge Option
                StrategyOption(
                    title = "智能合并",
                    description = "保留本地较新的学习记录，仅合并云端进度。这是最安全的选项。",
                    icon = Icons.AutoMirrored.Rounded.CallMerge,
                    isRecommended = true,
                    onClick = { onConfirm(ImportStrategy.MERGE) }
                )
                
                // Replace Option
                StrategyOption(
                    title = "完全覆盖",
                    description = "清空本地所有学习进度，完全使用云端数据。适用于彻底重置。",
                    icon = Icons.Rounded.Warning,
                    isRecommended = false,
                    isDestructive = true,
                    onClick = { onConfirm(ImportStrategy.REPLACE) }
                )
            }
        }
    )
}

@Composable
private fun StrategyOption(
    title: String,
    description: String,
    icon: ImageVector,
    isRecommended: Boolean,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    val containerColor = if (isDestructive) {
        MaterialTheme.colorScheme.errorContainer
    } else if (isRecommended) {
        MaterialTheme.colorScheme.primaryContainer 
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = if (isDestructive) {
        MaterialTheme.colorScheme.onErrorContainer
    } else if (isRecommended) {
        MaterialTheme.colorScheme.onPrimaryContainer 
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isDestructive) MaterialTheme.colorScheme.error else if (isRecommended) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    if (isRecommended) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "推荐",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
