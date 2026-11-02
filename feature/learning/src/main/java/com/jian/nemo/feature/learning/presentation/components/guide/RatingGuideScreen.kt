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
import com.jian.nemo.core.designsystem.shape.Capsule
import com.jian.nemo.core.designsystem.shape.RoundedRectangle
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import com.jian.nemo.feature.learning.R
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.ui.graphics.Brush
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.jian.nemo.core.ui.component.common.CommonHeader

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

    val backdrop = rememberLayerBackdrop()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = statusBarTop + 60.dp + 16.dp,
                    bottom = navBarBottom + 24.dp
                ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedRectangle(16.dp),
                    color = if (isDark) colorScheme.surfaceContainerLow else Color.White,
                    shadowElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.rating_guide_core_principle_title),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = textMain
                        )
                        Text(
                            text = stringResource(R.string.rating_guide_core_principle_content),
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
                    title = stringResource(R.string.rating_guide_new_learn_title),
                    againText = stringResource(R.string.rating_guide_new_again_desc),
                    hardText = stringResource(R.string.rating_guide_new_hard_desc),
                    goodText = stringResource(R.string.rating_guide_new_good_desc),
                    easyText = stringResource(R.string.rating_guide_new_easy_desc)
                )

                RatingGuideSection(
                    isDark = isDark,
                    cardColor = surfaceColor,
                    titleColor = textMain,
                    bodyColor = textMuted,
                    title = stringResource(R.string.rating_guide_review_title),
                    againText = stringResource(R.string.rating_guide_review_again_desc),
                    hardText = stringResource(R.string.rating_guide_review_hard_desc),
                    goodText = stringResource(R.string.rating_guide_review_good_desc),
                    easyText = stringResource(R.string.rating_guide_review_easy_desc)
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedRectangle(16.dp),
                    color = adviceBg
                ) {
                    Text(
                        text = stringResource(R.string.rating_guide_practical_tip),
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
                        shape = Capsule(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.rating_guide_btn_understood),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }

            Spacer(modifier = Modifier.height(12.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
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
                title = stringResource(R.string.rating_guide_title),
                onBack = onDismiss,
                backgroundColor = Color.Transparent,
                backdrop = backdrop,
                backButtonSize = 48.dp
            )
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
        shape = RoundedRectangle(24.dp),
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

            val againLabel = stringResource(R.string.learn_srs_rating_again)
            val hardLabel = stringResource(R.string.learn_srs_rating_hard)
            val goodLabel = stringResource(R.string.learn_srs_rating_good)
            val easyLabel = stringResource(R.string.learn_srs_rating_easy_alt)

            GuideLineBadge(isDark = isDark, label = againLabel, description = againText, bodyColor = bodyColor)
            GuideLineBadge(isDark = isDark, label = hardLabel, description = hardText, bodyColor = bodyColor)
            GuideLineBadge(isDark = isDark, label = goodLabel, description = goodText, bodyColor = bodyColor)
            GuideLineBadge(isDark = isDark, label = easyLabel, description = easyText, bodyColor = bodyColor)
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
    val againLabel = stringResource(R.string.learn_srs_rating_again)
    val hardLabel = stringResource(R.string.learn_srs_rating_hard)
    val goodLabel = stringResource(R.string.learn_srs_rating_good)
    val easyLabelAlt = stringResource(R.string.learn_srs_rating_easy_alt)
    val easyLabel = stringResource(R.string.learn_srs_rating_easy)

    val (tagTextColor, tagBgColor) = when (label) {
        againLabel -> if (isDark) RatingGuideBadgeRoseTextDark to RatingGuideBadgeRoseBgDark else RatingGuideBadgeRoseText to RatingGuideBadgeRoseBg
        hardLabel -> if (isDark) RatingGuideBadgeOrangeTextDark to RatingGuideBadgeOrangeBgDark else RatingGuideBadgeOrangeText to RatingGuideBadgeOrangeBg
        goodLabel -> if (isDark) RatingGuideBadgeBlueTextDark to RatingGuideBadgeBlueBgDark else RatingGuideBadgeBlueText to RatingGuideBadgeBlueBg
        easyLabelAlt, easyLabel -> if (isDark) RatingGuideBadgeEmeraldTextDark to RatingGuideBadgeEmeraldBgDark else RatingGuideBadgeEmeraldText to RatingGuideBadgeEmeraldBg
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
                    shape = RoundedRectangle(8.dp)
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
