package com.jian.nemo.core.ui.component.update

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.nemo.core.designsystem.shape.Capsule
import com.jian.nemo.core.designsystem.shape.RoundedRectangle
import com.jian.nemo.core.domain.model.AppUpdateConfig
import com.jian.nemo.core.ui.R

/**
 * 应用更新弹窗纯 UI 内容
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
    val isAppDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val contentColor = MaterialTheme.colorScheme.onSurface
    val primaryColor = MaterialTheme.colorScheme.primary
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val isMandatory = config.isForce || !config.canClose
    val haptic = LocalHapticFeedback.current

    val animatedProgress by animateFloatAsState(
        targetValue = downloadProgress.coerceIn(0f, 1f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "DownloadProgress"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        // 1. 顶部 Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(primaryColor.copy(alpha = 0.12f), RoundedRectangle(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.RocketLaunch,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = stringResource(R.string.core_ui_update_title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
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

        Spacer(modifier = Modifier.height(16.dp))

        HorizontalDivider(
            color = outlineVariant.copy(alpha = 0.15f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 2. 更新日志区域 (最小高度 96dp，最大高度 220dp，超出平滑滚动)
        if (config.updateLog.isNotBlank()) {
            Text(
                text = stringResource(R.string.core_ui_update_changelog_title),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 96.dp, max = 220.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                ) {
                    val rawLogs = config.updateLog.replace("\\n", "\n")
                    val logs = rawLogs.split("\n", "\r\n").filter { it.isNotBlank() }
                    for (line in logs) {
                        Row(
                            modifier = Modifier.padding(vertical = 3.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "•",
                                color = primaryColor,
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

            Spacer(modifier = Modifier.height(20.dp))
        }

        // 3. 底部操作区 (胶囊按钮 / 胶囊进度条)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                isDownloading -> {
                    // 下载中状态：双按钮合体为胶囊进度条，文字双层高精反色裁切
                    val percentInt = (animatedProgress * 100).toInt().coerceIn(0, 100)
                    val statusText = stringResource(R.string.core_ui_update_status_downloading_full) + " $percentInt%"

                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(Capsule())
                            .background(
                                if (isAppDark) primaryColor.copy(alpha = 0.16f) else primaryColor.copy(alpha = 0.10f),
                                Capsule()
                            )
                            .border(
                                width = 1.dp,
                                color = primaryColor.copy(alpha = 0.20f),
                                shape = Capsule()
                            )
                    ) {
                        val totalWidth = maxWidth

                        // 底层：未填充区域文本 (适配主题主色)
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                ),
                                color = primaryColor
                            )
                        }

                        // 顶层：活动进度条及其内部反色文本 (高对比 onPrimary)
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(totalWidth * animatedProgress)
                                .clipToBounds()
                                .background(primaryColor)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(totalWidth),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = statusText,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }
                isDownloaded -> {
                    // 下载完成状态：单个“立即安装”胶囊按钮
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(Capsule())
                            .background(primaryColor, Capsule())
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onInstallClick()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.core_ui_update_btn_install_now),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
                else -> {
                    // 未下载状态
                    if (isMandatory) {
                        // 强制更新：单个“立即升级”全宽胶囊按钮
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(Capsule())
                                .background(primaryColor, Capsule())
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onUpdateClick()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.core_ui_update_btn_upgrade_now),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                ),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    } else {
                        // 建议更新：双胶囊按钮
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 取消按钮
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(Capsule())
                                    .background(
                                        if (isAppDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f),
                                        Capsule()
                                    )
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onDismissRequest()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.core_ui_btn_later_update),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 15.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // 立即升级按钮
                            Box(
                                modifier = Modifier
                                    .weight(1.3f)
                                    .fillMaxHeight()
                                    .clip(Capsule())
                                    .background(primaryColor, Capsule())
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onUpdateClick()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.core_ui_update_btn_upgrade_now),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
