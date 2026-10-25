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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.nemo.core.designsystem.icon.IsoluminantPalette
import com.jian.nemo.core.designsystem.icon.PlumpDuoIcon
import com.jian.nemo.core.designsystem.icon.PlumpIcon
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.designsystem.theme.IosColors
import com.jian.nemo.core.ui.animation.containerTransform
import com.jian.nemo.core.ui.component.common.CommonHeader
import com.jian.nemo.core.ui.compositionlocal.LocalSettingIconStyle

/**
 * 能力工坊界面 - 提供多种专项能力训练游戏
 */
@Composable
fun AbilityWorkshopScreen(
    onBack: () -> Unit,
    onNavigateToGame: (String) -> Unit = {}
) {
    val isPlumpDuo = LocalSettingIconStyle.current == SettingIconStyle.PLUMP_DUO

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .containerTransform(
                key = "container_ability_workshop",
                shape = RoundedCornerShape(0.dp)
            ),
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
            AbilityItemCard(
                title = "听力挑战",
                description = "聆听本地纯正单词发音，检验听辨与词义联想能力。",
                icon = Icons.Rounded.Headphones,
                iconColor = IosColors.Indigo,
                plumpIcon = PlumpIcon.AudioVolume,
                isoluminantColor = IsoluminantPalette.SkyIndigo,
                isPlumpDuo = isPlumpDuo,
                onClick = { onNavigateToGame("listening_comprehension") }
            )
            AbilityItemCard(
                title = "单词填空",
                description = "根据释义和句子提示，拼写填入被挖空的假名。",
                icon = Icons.Rounded.EditCalendar,
                iconColor = IosColors.Orange,
                plumpIcon = PlumpIcon.SpellCheck,
                isoluminantColor = IsoluminantPalette.WarmAmber,
                isPlumpDuo = isPlumpDuo,
                onClick = { onNavigateToGame("word_cloze") }
            )
            AbilityItemCard(
                title = "动词活用",
                description = "为真实句子选择自然的动词形式。",
                icon = Icons.Rounded.AutoFixHigh,
                iconColor = IosColors.Blue,
                plumpIcon = PlumpIcon.Sparkles,
                isoluminantColor = IsoluminantPalette.EmeraldGreen,
                isPlumpDuo = isPlumpDuo,
                onClick = { onNavigateToGame("verb_conjugation") }
            )
            AbilityItemCard(
                title = "近义词连接",
                description = "连接意思相近的单词。",
                icon = Icons.Rounded.Schema,
                iconColor = IosColors.Orange,
                plumpIcon = PlumpIcon.Link2,
                isoluminantColor = IsoluminantPalette.LakesideCyan,
                isPlumpDuo = isPlumpDuo,
                onClick = { onNavigateToGame("synonym_connection") }
            )
            AbilityItemCard(
                title = "反义词消消乐",
                description = "匹配反义词，消除单词对。",
                icon = Icons.Rounded.SwapHoriz,
                iconColor = IosColors.Teal,
                plumpIcon = PlumpIcon.Zap,
                isoluminantColor = IsoluminantPalette.GlazeMint,
                isPlumpDuo = isPlumpDuo,
                onClick = { onNavigateToGame("antonym_matching") }
            )
            AbilityItemCard(
                title = "自然搭配",
                description = "组合听起来自然的词语搭配。",
                icon = Icons.Rounded.Hub,
                iconColor = IosColors.Green,
                plumpIcon = PlumpIcon.Puzzle,
                isoluminantColor = IsoluminantPalette.LilacViolet,
                isPlumpDuo = isPlumpDuo,
                onClick = { onNavigateToGame("collocation") }
            )
            AbilityItemCard(
                title = "语法纠错",
                description = "找出语法错误，并选择正确改法。",
                icon = Icons.Rounded.Spellcheck,
                iconColor = IosColors.Purple,
                plumpIcon = PlumpIcon.BadgeCheck,
                isoluminantColor = IsoluminantPalette.CoralFlame,
                isPlumpDuo = isPlumpDuo,
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
    plumpIcon: PlumpIcon,
    isoluminantColor: Color,
    isPlumpDuo: Boolean,
    onClick: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val effectiveColor = if (isPlumpDuo) isoluminantColor else iconColor

    // 背景微光：默认方案 100% 保持原有 0.05f，Plump Duo 方案使用感知通透微光
    val cardBg = if (isPlumpDuo) {
        effectiveColor.copy(alpha = if (isDark) 0.10f else 0.06f)
    } else {
        iconColor.copy(alpha = 0.05f)
    }
    
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
            if (isPlumpDuo) {
                // Plump Duo 方案：彻底取消厚重实色方块贴纸底座，使用纯透明对齐容器，通透立体居中呈现
                Box(
                    modifier = Modifier.size(56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    PlumpDuoIcon(
                        icon = plumpIcon,
                        color = isoluminantColor,
                        size = 48.dp
                    )
                }
            } else {
                // 原有默认方案：100% 保留原有 56dp 实色方块底座与单色白图标
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
            
            // 箭头指示器（严格保持灰色中性控件，严禁滥用彩色）
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            )
        }
    }
}

