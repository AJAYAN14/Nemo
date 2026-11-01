package com.jian.nemo.core.ui.component.update

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.jian.nemo.core.ui.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.nemo.core.domain.model.AppUpdateConfig

/**
 * 应用更新弹窗纯 UI 内容 (Pure UI)
 * 仅负责渲染，不包含 BottomSheet 逻辑或副作用
 */
@Composable
fun UpdateDialogContent(
    config: AppUpdateConfig,
    downloadProgress: Float,
    isDownloading: Boolean,
    isDownloaded: Boolean,
    onUpdateClick: () -> Unit,
    onInstallClick: () -> Unit,
    onDismissRequest: () -> Unit
) {
    val scrollState = rememberScrollState()
    val contentColor = MaterialTheme.colorScheme.onSurface
    val primaryColor = MaterialTheme.colorScheme.primary
    val accentColor = primaryColor
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.Start
    ) {
        // 1. Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Squircle Icon Container
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.RocketLaunch,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = stringResource(R.string.core_ui_update_title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = contentColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.core_ui_update_version_ready, config.versionName),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        HorizontalDivider(
            color = outlineVariant.copy(alpha = 0.2f),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 2. Content Body
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            // 更新日志区域
            if (config.updateLog.isNotBlank()) {
                 Text(
                    text = stringResource(R.string.core_ui_update_changelog_title),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Box(modifier = Modifier.heightIn(max = 240.dp)) {
                    Column(
                        modifier = Modifier
                            .verticalScroll(scrollState)
                            .fillMaxWidth()
                    ) {
                        // 兼容处理：Supabase 有时会返回转义的 \\n，需要替换为真实换行符
                        val rawLogs = config.updateLog.replace("\\n", "\n")
                        val logs = rawLogs.split("\n", "\\r\\n").filter { it.isNotBlank() }
                        for (line in logs) {
                            Row(
                                modifier = Modifier.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "•",
                                    color = accentColor,
                                    modifier = Modifier.padding(end = 8.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = line.trim().removePrefix("-").removePrefix("•").trim(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = contentColor,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // 3. 进度条系统
            AnimatedVisibility(visible = isDownloading || isDownloaded) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        val statusText = if (isDownloaded) stringResource(R.string.core_ui_update_status_downloaded) else stringResource(R.string.core_ui_update_status_downloading_full)
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${(downloadProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = accentColor
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { downloadProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = accentColor,
                        trackColor = accentColor.copy(alpha = 0.2f),
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // 4. 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // “暂不更新”按钮 (始终显示，除非正在下载)
                if (!isDownloading && !isDownloaded) {
                     TextButton(
                        onClick = onDismissRequest,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.core_ui_btn_later_update),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // 主操作按钮
                val primaryActionText = when {
                    isDownloaded -> stringResource(R.string.core_ui_update_btn_install_now)
                    isDownloading -> stringResource(R.string.core_ui_update_btn_downloading)
                    else -> stringResource(R.string.core_ui_update_btn_upgrade_now)
                }

                Button(
                    onClick = {
                        if (isDownloaded) onInstallClick() else onUpdateClick()
                    },
                    modifier = Modifier
                        .weight(1.5f)
                        .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !isDownloading || isDownloaded,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Text(
                        text = primaryActionText,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}
