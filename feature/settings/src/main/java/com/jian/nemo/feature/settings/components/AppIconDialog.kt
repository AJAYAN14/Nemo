package com.jian.nemo.feature.settings.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties

/**
 * 应用图标选择对话框
 */
@Composable
fun AppIconDialog(
    currentIcon: String,
    onDismiss: () -> Unit,
    onIconSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        tonalElevation = 0.dp,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .padding(24.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp)),
        title = {
            Text(
                "更换应用图标",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
        },
        text = {
            Column {
                Text(
                    text = "选择一个图标样式，切换后桌面图标可能需要几秒钟才会更新。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(28.dp))
                
                // 使用 FlowRow 或更好的网格布局
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        AppIconOption(
                            name = "Nemo",
                            displayName = "默认",
                            isSelected = currentIcon == "Nemo",
                            modifier = Modifier.weight(1f),
                            onClick = { onIconSelect("Nemo") }
                        )
                        AppIconOption(
                            name = "Gold",
                            displayName = "金典",
                            isSelected = currentIcon == "Gold",
                            modifier = Modifier.weight(1f),
                            onClick = { onIconSelect("Gold") }
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        AppIconOption(
                            name = "Daruma",
                            displayName = "达摩",
                            isSelected = currentIcon == "Daruma",
                            modifier = Modifier.weight(1f),
                            onClick = { onIconSelect("Daruma") }
                        )
                        AppIconOption(
                            name = "Zen",
                            displayName = "简约",
                            isSelected = currentIcon == "Zen",
                            modifier = Modifier.weight(1f),
                            onClick = { onIconSelect("Zen") }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // 扁平化的按钮样式
                Button(
                    onClick = { onIconSelect("Nemo") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp)
                ) {
                    Text("恢复系统默认图标", fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.padding(bottom = 8.dp, end = 8.dp)
            ) {
                Text("关闭", fontWeight = FontWeight.SemiBold)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface // 确保浅色模式下为白色/表面色
    )
}

@Composable
private fun AppIconOption(
    name: String,
    displayName: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else Color.Transparent

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        // 图标预览 (移除海拔)
        Surface(
            modifier = Modifier.size(64.dp),
            shape = RoundedCornerShape(14.dp),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            val context = LocalContext.current
            val baseName = name.lowercase()
            
            val bgResId = remember(baseName) {
                context.resources.getIdentifier(
                    "ic_launcher_${baseName}_background",
                    "drawable",
                    context.packageName
                )
            }
            val fgResId = remember(baseName) {
                val resId = context.resources.getIdentifier(
                    "ic_launcher_${baseName}_foreground",
                    "drawable",
                    context.packageName
                )
                if (resId != 0) resId else {
                    context.resources.getIdentifier(
                        "ic_launcher_${baseName}_foreground",
                        "mipmap",
                        context.packageName
                    )
                }
            }
            
            Box(contentAlignment = Alignment.Center) {
                // 背景图层
                if (bgResId != 0) {
                    Image(
                        painter = painterResource(id = bgResId),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                // 前景图层 (Logo)
                if (fgResId != 0) {
                    Icon(
                        painter = painterResource(id = fgResId),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = displayName,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}
