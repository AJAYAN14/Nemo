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
import com.jian.nemo.core.designsystem.theme.IosColors
import com.jian.nemo.core.ui.component.common.CommonHeader
import com.jian.nemo.core.ui.component.common.NemoScaffold
import com.jian.nemo.core.ui.animation.containerTransform

/**
 * 能力工坊界面 - 提供多种专项能力训练游戏
 */
@Composable
fun AbilityWorkshopScreen(
    onBack: () -> Unit,
    onNavigateToGame: (String) -> Unit = {}
) {
    NemoScaffold(
        modifier = Modifier
            .fillMaxSize()
            .containerTransform(
                key = "container_ability_workshop",
                shape = RoundedCornerShape(0.dp)
            ),
        title = "能力工坊",
        onBack = onBack,
        backgroundColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AbilityItemCard(
                title = "听力挑战",
                description = "聆听本地纯正单词发音，检验听辨与词义联想能力。",
                icon = Icons.Rounded.Headphones,
                iconColor = IosColors.Indigo,
                onClick = { onNavigateToGame("listening_comprehension") }
            )
            AbilityItemCard(
                title = "单词填空",
                description = "根据释义和句子提示，拼写填入被挖空的假名。",
                icon = Icons.Rounded.EditCalendar,
                iconColor = IosColors.Orange,
                onClick = { onNavigateToGame("word_cloze") }
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
