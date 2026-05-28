package com.jian.nemo.feature.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jian.nemo.core.designsystem.theme.*
import com.jian.nemo.core.ui.component.common.CommonHeader
import com.jian.nemo.core.ui.component.animation.NemoChasingDotsLoader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 同步故障诊断界面
 * 符合 /ui-ux-pro-max 规范，优雅适配深色模式，提供报错监控、历史清除、诊断排查和手动检测重试。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncDiagnosticsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val hasError = uiState.syncErrorLogs.isNotEmpty()

    Scaffold(
        topBar = {
            CommonHeader(
                title = "同步故障诊断",
                onBack = onNavigateBack
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 6.dp,
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                ),
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Button(
                        onClick = { viewModel.onEvent(SettingsEvent.SyncData) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        enabled = !uiState.isLoading
                    ) {
                        AnimatedContent(
                            targetState = uiState.isLoading,
                            label = "SyncButtonAnimation"
                        ) { loading ->
                            if (loading) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    NemoChasingDotsLoader(size = 18.dp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "正在同步检测...",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 16.sp
                                    )
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Sync,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "重新检测并同步",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(vertical = 20.dp)
        ) {
            // 1. 诊断汇总状态大卡片
            item {
                DiagnosticOverviewCard(
                    hasError = hasError,
                    lastSyncTime = uiState.lastSyncTime
                )
            }

            // 2. 详细错误日志展示
            if (hasError) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 8.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "历史报错日志",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        TextButton(
                            onClick = {
                                viewModel.onEvent(SettingsEvent.ClearSyncError)
                                Toast.makeText(context, "报错日志已清除", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("清空所有", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                items(
                    items = uiState.syncErrorLogs,
                    key = { it.timestamp.toString() + it.message.hashCode() }
                ) { log ->
                    ErrorLogItemCard(
                        log = log,
                        onCopy = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            val clip = ClipData.newPlainText("Sync Error Log", "[${format.format(Date(log.timestamp))}] ${log.message}")
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "日志已复制到剪切板", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                    )
                }
            }

            // 3. 常见排查方案指引
            item {
                TroubleshootingGuideSection()
            }
        }
    }
}

/**
 * 诊断汇总状态大卡片
 */
@Composable
private fun DiagnosticOverviewCard(
    hasError: Boolean,
    lastSyncTime: Long
) {
    val cardBgColor = if (hasError) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
    } else {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
    }

    val iconColor = if (hasError) NemoDanger else Color(0xFF34C759)
    val stateTitle = if (hasError) "同步已中断" else "同步服务运行良好"
    val stateDesc = if (hasError) {
        "检测到最新一次同步过程中发生异常，可能影响多端数据的实时性。"
    } else {
        "所有本地数据和云端记录保持一致，未检测到运行故障。"
    }

    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(
            1.dp,
            if (hasError) MaterialTheme.colorScheme.error.copy(alpha = 0.2f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 环形背景图标
            Surface(
                shape = CircleShape,
                color = iconColor.copy(alpha = 0.12f),
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (hasError) Icons.Rounded.Cancel else Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stateTitle,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                ),
                color = if (hasError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stateDesc,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 同步时间小标签
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                border = BorderStroke(
                    0.5.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AccessTime,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (lastSyncTime > 0) {
                            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            "更新时间: ${format.format(Date(lastSyncTime))}"
                        } else {
                            "更新时间: 尚无同步记录"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

/**
 * 详细报错日志展示卡片 (Glassmorphism 风格)
 */
@Composable
private fun ErrorLogItemCard(
    log: com.jian.nemo.core.domain.model.SyncErrorLog,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = format.format(Date(log.timestamp)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                
                Surface(
                    shape = CircleShape,
                    color = Color.Transparent,
                    onClick = onCopy,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = "复制日志",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = log.message,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                ),
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

/**
 * 故障排查方案指引区域
 */
@Composable
private fun TroubleshootingGuideSection() {
    Column {
        Text(
            text = "故障排查建议",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TroubleshootingItem(
                icon = Icons.Rounded.WifiOff,
                iconColor = NemoOrange,
                title = "检查网络连通性",
                description = "多端同步涉及云端服务网络连接，请确保设备能够正常访问网络。如果您开启了 VPN 代理等可能导致网络请求拦截的全局设置，建议尝试关闭后重试。"
            )

            TroubleshootingItem(
                icon = Icons.Rounded.NoAccounts,
                iconColor = NemoIndigo,
                title = "验证登录与会话",
                description = "同步依赖于当前的登录账号状态，如果您的本地登录会话失效（例如在其他设备上退出了账号），同步也会被强制中止。您可以在“账户管理”里执行重新登录操作。"
            )

            TroubleshootingItem(
                icon = Icons.Rounded.NewReleases,
                iconColor = NemoPurple,
                title = "检查软件版本更新",
                description = "如果云端系统进行了架构变更，而您的设备版本过低，会导致无法同步以防止数据异常。请关注是否有最新版本更新并及时升级。"
            )
        }
    }
}

/**
 * 单个排查项目组件
 */
@Composable
private fun TroubleshootingItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    description: String
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = iconColor.copy(alpha = 0.1f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}
