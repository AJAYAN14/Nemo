package com.jian.nemo.feature.test.presentation.ability

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.BorderStroke
import com.jian.nemo.core.designsystem.theme.IosColors
import com.jian.nemo.core.ui.component.common.CommonHeader
import com.jian.nemo.core.ui.component.common.NemoGooeyToggle

/**
 * 能力工坊界面 - 提供多种专项能力训练游戏
 */
@Composable
fun AbilityWorkshopScreen(
    onBack: () -> Unit,
    onNavigateToGame: (String) -> Unit = {},
    viewModel: VerbConjugationViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.preloadCachePoolQuietly()
    }

    val isPregen by viewModel.isPregenEnabled.collectAsState()
    val cacheCounts by viewModel.gameCacheCounts.collectAsState()

    Scaffold(
        topBar = {
            CommonHeader(
                title = "能力工坊",
                onBack = onBack
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Bento 风 AI 预出题控制开关卡片
            PregenToggleCard(
                isEnabled = isPregen,
                cacheCounts = cacheCounts,
                onToggle = { viewModel.togglePregenEnabled(it) }
            )

            AbilityItemCard(
                title = "动词活用",
                description = "为真实句子选择自然的动词形式。",
                icon = Icons.Rounded.AutoFixHigh,
                iconColor = IosColors.Blue,
                onClick = { onNavigateToGame("verb_conjugation") }
            )
            AbilityItemCard(
                title = "近义词连接",
                description = "连接意思相近的单词。",
                icon = Icons.Rounded.Schema,
                iconColor = IosColors.Orange,
                onClick = { onNavigateToGame("synonym_connection") }
            )
            AbilityItemCard(
                title = "反义词消消乐",
                description = "匹配反义词，消除单词对。",
                icon = Icons.Rounded.SwapHoriz,
                iconColor = IosColors.Teal,
                onClick = { onNavigateToGame("antonym_matching") }
            )
            AbilityItemCard(
                title = "自然搭配",
                description = "组合听起来自然的词语搭配。",
                icon = Icons.Rounded.Hub,
                iconColor = IosColors.Green,
                onClick = { onNavigateToGame("collocation") }
            )
            AbilityItemCard(
                title = "语法纠错",
                description = "找出语法错误，并选择正确改法。",
                icon = Icons.Rounded.Spellcheck,
                iconColor = IosColors.Purple,
                onClick = { onNavigateToGame("grammar_correction") }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AbilityItemCard(
    title: String,
    description: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    // 借鉴图片中的浅色背景风格，使用极低透明度的图标颜色作为卡片背景
    val cardBg = iconColor.copy(alpha = 0.05f)
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
        color = cardBg,
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标容器
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(iconColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // 文本区域
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // 箭头指示器
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
private fun PregenToggleCard(
    isEnabled: Boolean,
    cacheCounts: Map<String, Int>,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧 AI 闪烁图标容器
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = IosColors.Blue.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = IosColors.Blue,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // 中间文字说明
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "AI 预生成缓存",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "开启后秒速答题，关闭可节省后台流量与 Token",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // 右侧招牌通用 Gooey 动画开关
                NemoGooeyToggle(
                    checked = isEnabled,
                    onCheckedChange = onToggle,
                    activeColor = IosColors.Blue,
                    inactiveColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }

            if (isEnabled) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                Spacer(modifier = Modifier.height(12.dp))

                // 各题型缓存进度胶囊
                val games = listOf(
                    Triple("动词活用", "verb_conjugation", IosColors.Blue),
                    Triple("近义词", "synonym_connection", IosColors.Orange),
                    Triple("反义词", "antonym_matching", IosColors.Teal),
                    Triple("自然搭配", "collocation", IosColors.Green),
                    Triple("语法纠错", "grammar_correction", IosColors.Purple)
                )

                // 第一排 Row：前三个
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    games.take(3).forEach { (label, key, color) ->
                        val count = cacheCounts[key] ?: 0
                        GameCacheBadge(
                            label = label,
                            count = count,
                            color = color,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 第二排 Row：后两个加一个弹性占位占满宽度
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    games.drop(3).forEach { (label, key, color) ->
                        val count = cacheCounts[key] ?: 0
                        GameCacheBadge(
                            label = label,
                            count = count,
                            color = color,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // 完美的弹性占位
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun GameCacheBadge(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    val isFull = count >= 20
    val activeColor = if (isFull) color else color.copy(alpha = 0.8f)
    val bgColor = if (count > 0) color.copy(alpha = 0.08f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f)
    val textColor = if (count > 0) activeColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    val borderStroke = if (count > 0) {
        BorderStroke(0.5.dp, color.copy(alpha = 0.15f))
    } else {
        BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = bgColor,
        border = borderStroke
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = textColor
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = "$count/20",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isFull) color else textColor
            )
        }
    }
}
