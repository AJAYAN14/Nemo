package com.jian.nemo.feature.test.presentation.ability

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.EditCalendar
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Schema
import androidx.compose.material.icons.rounded.Spellcheck
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.nemo.core.designsystem.shape.RoundedRectangle
import com.jian.nemo.core.designsystem.theme.SystemColors
import com.jian.nemo.core.designsystem.theme.screenBackground
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.ui.animation.containerTransform
import com.jian.nemo.core.ui.component.common.CommonHeader
import com.jian.nemo.core.ui.compositionlocal.LocalSettingIconStyle
import com.jian.nemo.feature.test.R
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

/**
 * 能力工坊界面 - 提供多种专项能力训练游戏
 */
@Composable
fun AbilityWorkshopScreen(
    onBack: () -> Unit,
    onNavigateToGame: (String) -> Unit = {}
) {
    val backgroundColor = MaterialTheme.colorScheme.screenBackground
    val backdrop = rememberLayerBackdrop()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .containerTransform(
                key = "container_ability_workshop",
                shape = RoundedRectangle(0.dp)
            )
    ) {
        // 1. 底层滚动内容（被 backdrop 采样源录制）
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = statusBarTop + 60.dp + 16.dp,
                bottom = navBarBottom + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                AbilityItemCard(
                    title = stringResource(R.string.test_ability_workshop_listening_title),
                    description = stringResource(R.string.test_ability_workshop_listening_desc),
                    icon = Icons.Rounded.Headphones,
                    iconColor = SystemColors.Indigo,
                    onClick = { onNavigateToGame("listening_comprehension") }
                )
            }
            item {
                AbilityItemCard(
                    title = stringResource(R.string.test_ability_workshop_cloze_title),
                    description = stringResource(R.string.test_ability_workshop_cloze_desc),
                    icon = Icons.Rounded.EditCalendar,
                    iconColor = SystemColors.Orange,
                    onClick = { onNavigateToGame("word_cloze") }
                )
            }
            item {
                AbilityItemCard(
                    title = stringResource(R.string.test_ability_workshop_verb_title),
                    description = stringResource(R.string.test_ability_workshop_verb_desc),
                    icon = Icons.Rounded.AutoFixHigh,
                    iconColor = SystemColors.Blue,
                    onClick = { onNavigateToGame("verb_conjugation") }
                )
            }
            item {
                AbilityItemCard(
                    title = stringResource(R.string.test_ability_workshop_synonym_title),
                    description = stringResource(R.string.test_ability_workshop_synonym_desc),
                    icon = Icons.Rounded.Schema,
                    iconColor = SystemColors.Cyan,
                    onClick = { onNavigateToGame("synonym_connection") }
                )
            }
            item {
                AbilityItemCard(
                    title = stringResource(R.string.test_ability_workshop_antonym_title),
                    description = stringResource(R.string.test_ability_workshop_antonym_desc),
                    icon = Icons.Rounded.SwapHoriz,
                    iconColor = SystemColors.Teal,
                    onClick = { onNavigateToGame("antonym_matching") }
                )
            }
            item {
                AbilityItemCard(
                    title = stringResource(R.string.test_ability_workshop_collocation_title),
                    description = stringResource(R.string.test_ability_workshop_collocation_desc),
                    icon = Icons.Rounded.Hub,
                    iconColor = SystemColors.Green,
                    onClick = { onNavigateToGame("collocation") }
                )
            }
            item {
                AbilityItemCard(
                    title = stringResource(R.string.test_ability_workshop_grammar_title),
                    description = stringResource(R.string.test_ability_workshop_grammar_desc),
                    icon = Icons.Rounded.Spellcheck,
                    iconColor = SystemColors.Purple,
                    onClick = { onNavigateToGame("grammar_correction") }
                )
            }
        }

        // 2. 悬浮顶栏与液态玻璃按钮（带柔和垂直渐变保护遮罩，读取 backdrop）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            backgroundColor.copy(alpha = 0.95f),
                            backgroundColor.copy(alpha = 0.75f),
                            backgroundColor.copy(alpha = 0.35f),
                            Color.Transparent
                        )
                    )
                )
                .padding(bottom = 8.dp)
        ) {
            CommonHeader(
                title = stringResource(R.string.test_dashboard_workshop_title),
                onBack = onBack,
                backgroundColor = Color.Transparent,
                backdrop = backdrop,
                backButtonSize = 48.dp
            )
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
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val iconStyle = LocalSettingIconStyle.current
    val isSoftTranslucent = (iconStyle == SettingIconStyle.SOFT_TRANSLUCENT)

    val containerBgColor = if (isSoftTranslucent) iconColor.copy(alpha = 0.15f) else iconColor
    val iconTint = if (isSoftTranslucent) iconColor else Color.White
    val cardBg = iconColor.copy(alpha = if (isDark) 0.08f else 0.05f)

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        label = "abilityCardScale",
        animationSpec = tween(150)
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedRectangle(20.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        color = cardBg,
        shape = RoundedRectangle(20.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标容器：严格遵守 2:3 黄金尺寸比例公理 (42dp 底框 x 28dp 图标，四周 7dp 留白)
            // G2 连续平滑曲率 RoundedRectangle(12.dp)
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(containerBgColor, RoundedRectangle(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
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

            // 箭头指示器（灰色中性次级控件）
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
            )
        }
    }
}
