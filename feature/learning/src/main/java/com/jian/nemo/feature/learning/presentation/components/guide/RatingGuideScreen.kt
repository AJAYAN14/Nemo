package com.jian.nemo.feature.learning.presentation.components.guide

import com.jian.nemo.core.designsystem.theme.screenBackground

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.jian.nemo.core.designsystem.theme.NemoSurfaceBackground
import com.jian.nemo.core.designsystem.theme.NemoSurfaceBackgroundDark
import com.jian.nemo.core.designsystem.theme.NemoSurfaceCard
import com.jian.nemo.core.designsystem.theme.NemoSurfaceCardDark
import com.jian.nemo.core.designsystem.theme.RatingGuideAdviceBg
import com.jian.nemo.core.designsystem.theme.RatingGuideAdviceText
import com.jian.nemo.core.designsystem.theme.RatingGuideBadgeBlueBg
import com.jian.nemo.core.designsystem.theme.RatingGuideBadgeBlueBgDark
import com.jian.nemo.core.designsystem.theme.RatingGuideBadgeBlueText
import com.jian.nemo.core.designsystem.theme.RatingGuideBadgeBlueTextDark
import com.jian.nemo.core.designsystem.theme.RatingGuideBadgeEmeraldBg
import com.jian.nemo.core.designsystem.theme.RatingGuideBadgeEmeraldBgDark
import com.jian.nemo.core.designsystem.theme.RatingGuideBadgeEmeraldText
import com.jian.nemo.core.designsystem.theme.RatingGuideBadgeEmeraldTextDark
import com.jian.nemo.core.designsystem.theme.RatingGuideBadgeOrangeBg
import com.jian.nemo.core.designsystem.theme.RatingGuideBadgeOrangeBgDark
import com.jian.nemo.core.designsystem.theme.RatingGuideBadgeOrangeText
import com.jian.nemo.core.designsystem.theme.RatingGuideBadgeOrangeTextDark
import com.jian.nemo.core.designsystem.theme.RatingGuideBadgeRoseBg
import com.jian.nemo.core.designsystem.theme.RatingGuideBadgeRoseBgDark
import com.jian.nemo.core.designsystem.theme.RatingGuideBadgeRoseText
import com.jian.nemo.core.designsystem.theme.RatingGuideBadgeRoseTextDark
import com.jian.nemo.core.designsystem.theme.RatingGuideBodyText
import com.jian.nemo.core.designsystem.theme.RatingGuideCoreText
import com.jian.nemo.core.designsystem.theme.RatingGuidePrimaryButton
import com.jian.nemo.core.designsystem.theme.RatingGuideTitleText
import com.jian.nemo.core.ui.component.common.CommonHeader
import com.jian.nemo.core.ui.component.common.NemoScaffold

@Composable
fun RatingGuideScreen(
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f

    val backgroundColor = MaterialTheme.colorScheme.screenBackground
    val surfaceColor = if (isDark) colorScheme.surfaceContainer else NemoSurfaceCard
    val textMain = if (isDark) colorScheme.onSurface else RatingGuideTitleText
    val textSub = if (isDark) colorScheme.onSurfaceVariant else RatingGuideCoreText
    val textMuted = if (isDark) colorScheme.onSurfaceVariant.copy(alpha = 0.75f) else RatingGuideBodyText
    val adviceBg = if (isDark) colorScheme.surfaceContainerHigh else RatingGuideAdviceBg
    val adviceText = if (isDark) colorScheme.onSurfaceVariant else RatingGuideAdviceText
    val buttonColor = if (isDark) colorScheme.primary else RatingGuidePrimaryButton

    NemoScaffold(
        title = "评分说明",
        onBack = onDismiss,
        backgroundColor = backgroundColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDark) colorScheme.surfaceContainerLow else Color.White,
                    shadowElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "核心原则",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = textMain
                        )
                        Text(
                            text = "按回忆难度打分，不按是否看过答案打分。",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                lineHeight = 21.sp
                            ),
                            color = textSub
                        )
                    }
                }

                RatingGuideSection(
                    isDark = isDark,
                    cardColor = surfaceColor,
                    titleColor = textMain,
                    bodyColor = textMuted,
                    title = "新学（第一次接触）",
                    againText = "重来：完全想不起来，或需要重新看讲解。",
                    hardText = "困难：能回忆一点，但很吃力、很慢，容易错。",
                    goodText = "良好：能正常回忆，速度一般。",
                    easyText = "容易：几乎秒回，且很有把握。"
                )

                RatingGuideSection(
                    isDark = isDark,
                    cardColor = surfaceColor,
                    titleColor = textMain,
                    bodyColor = textMuted,
                    title = "复习（学过的卡片）",
                    againText = "重来：这次没想起来，或答错。",
                    hardText = "困难：想起来了，但明显比预期更费劲。",
                    goodText = "良好：正常想起，符合日常复习状态。",
                    easyText = "容易：非常轻松，建议拉长下次间隔。"
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = adviceBg
                ) {
                    Text(
                        text = "实用建议：拿不准时优先选“良好”；只有明显吃力再选“困难”，别把“重来”当保守选项常点。",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            lineHeight = 21.sp
                        ),
                        modifier = Modifier.padding(16.dp),
                        color = adviceText
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = buttonColor,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = "我知道了",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
    }
}

@Composable
private fun RatingGuideSection(
    isDark: Boolean,
    cardColor: Color,
    titleColor: Color,
    bodyColor: Color,
    title: String,
    againText: String,
    hardText: String,
    goodText: String,
    easyText: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = cardColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = titleColor
            )

            GuideLineBadge(isDark = isDark, label = "重来", description = againText, bodyColor = bodyColor)
            GuideLineBadge(isDark = isDark, label = "困难", description = hardText, bodyColor = bodyColor)
            GuideLineBadge(isDark = isDark, label = "良好", description = goodText, bodyColor = bodyColor)
            GuideLineBadge(isDark = isDark, label = "容易", description = easyText, bodyColor = bodyColor)
        }
    }
}

@Composable
private fun GuideLineBadge(
    isDark: Boolean,
    label: String,
    description: String,
    bodyColor: Color
) {
    val (tagTextColor, tagBgColor) = when (label) {
        "重来" -> if (isDark) RatingGuideBadgeRoseTextDark to RatingGuideBadgeRoseBgDark else RatingGuideBadgeRoseText to RatingGuideBadgeRoseBg
        "困难" -> if (isDark) RatingGuideBadgeOrangeTextDark to RatingGuideBadgeOrangeBgDark else RatingGuideBadgeOrangeText to RatingGuideBadgeOrangeBg
        "良好" -> if (isDark) RatingGuideBadgeBlueTextDark to RatingGuideBadgeBlueBgDark else RatingGuideBadgeBlueText to RatingGuideBadgeBlueBg
        "容易", "简单" -> if (isDark) RatingGuideBadgeEmeraldTextDark to RatingGuideBadgeEmeraldBgDark else RatingGuideBadgeEmeraldText to RatingGuideBadgeEmeraldBg
        else -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = tagTextColor,
            modifier = Modifier
                .background(
                    color = tagBgColor,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                lineHeight = 20.sp
            ),
            color = bodyColor,
            modifier = Modifier.weight(1f)
        )
    }
}
