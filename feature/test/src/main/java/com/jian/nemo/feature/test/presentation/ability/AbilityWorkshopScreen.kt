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
import androidx.compose.ui.res.stringResource
import com.jian.nemo.feature.test.R
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
                title = stringResource(R.string.test_dashboard_workshop_title),
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
                title = stringResource(R.string.test_ability_workshop_listening_title),
                description = stringResource(R.string.test_ability_workshop_listening_desc),
                icon = Icons.Rounded.Headphones,
                iconColor = IosColors.Indigo,
                plumpIcon = PlumpIcon.AudioVolume,
                isoluminantColor = IsoluminantPalette.SkyIndigo,
                isPlumpDuo = isPlumpDuo,
                onClick = { onNavigateToGame("listening_comprehension") }
            )
            AbilityItemCard(
                title = stringResource(R.string.test_ability_workshop_cloze_title),
                description = stringResource(R.string.test_ability_workshop_cloze_desc),
                icon = Icons.Rounded.EditCalendar,
                iconColor = IosColors.Orange,
                plumpIcon = PlumpIcon.SpellCheck,
                isoluminantColor = IsoluminantPalette.WarmAmber,
                isPlumpDuo = isPlumpDuo,
                onClick = { onNavigateToGame("word_cloze") }
            )
            AbilityItemCard(
                title = stringResource(R.string.test_ability_workshop_verb_title),
                description = stringResource(R.string.test_ability_workshop_verb_desc),
                icon = Icons.Rounded.AutoFixHigh,
                iconColor = IosColors.Blue,
                plumpIcon = PlumpIcon.Sparkles,
                isoluminantColor = IsoluminantPalette.EmeraldGreen,
                isPlumpDuo = isPlumpDuo,
                onClick = { onNavigateToGame("verb_conjugation") }
            )
            AbilityItemCard(
                title = stringResource(R.string.test_ability_workshop_synonym_title),
                description = stringResource(R.string.test_ability_workshop_synonym_desc),
                icon = Icons.Rounded.Schema,
                iconColor = IosColors.Orange,
                plumpIcon = PlumpIcon.Link2,
                isoluminantColor = IsoluminantPalette.LakesideCyan,
                isPlumpDuo = isPlumpDuo,
                onClick = { onNavigateToGame("synonym_connection") }
            )
            AbilityItemCard(
                title = stringResource(R.string.test_ability_workshop_antonym_title),
                description = stringResource(R.string.test_ability_workshop_antonym_desc),
                icon = Icons.Rounded.SwapHoriz,
                iconColor = IosColors.Teal,
                plumpIcon = PlumpIcon.Zap,
                isoluminantColor = IsoluminantPalette.GlazeMint,
                isPlumpDuo = isPlumpDuo,
                onClick = { onNavigateToGame("antonym_matching") }
            )
            AbilityItemCard(
                title = stringResource(R.string.test_ability_workshop_collocation_title),
                description = stringResource(R.string.test_ability_workshop_collocation_desc),
                icon = Icons.Rounded.Hub,
                iconColor = IosColors.Green,
                plumpIcon = PlumpIcon.Puzzle,
                isoluminantColor = IsoluminantPalette.LilacViolet,
                isPlumpDuo = isPlumpDuo,
                onClick = { onNavigateToGame("collocation") }
            )
            AbilityItemCard(
                title = stringResource(R.string.test_ability_workshop_grammar_title),
                description = stringResource(R.string.test_ability_workshop_grammar_desc),
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

