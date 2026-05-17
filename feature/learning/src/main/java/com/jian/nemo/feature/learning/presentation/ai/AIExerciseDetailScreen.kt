package com.jian.nemo.feature.learning.presentation.ai

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.nemo.core.designsystem.theme.*
import com.jian.nemo.core.domain.model.AIExerciseHistory
import com.jian.nemo.core.ui.component.common.CommonHeader
import com.jian.nemo.core.ui.component.speaker.SpeakerButton


/**
 * AI 练习详情全屏组件
 * 采用全屏 Surface 覆盖层实现，以获得完美的 Edge-to-Edge 沉浸式效果。
 * 设计参考 RatingGuideScreen。
 */
@Composable
fun AIExerciseDetailDialog(
    history: AIExerciseHistory,
    playingAudioId: String?,
    onSpeak: (String, String) -> Unit,
    onDismiss: () -> Unit
) {

    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f
    val backgroundColor = if (isDark) NemoSurfaceBackgroundDark else NemoSurfaceBackground

    // 拦截物理返回键
    BackHandler(onBack = onDismiss)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = backgroundColor
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Header
            CommonHeader(
                title = "练习详情",
                onBack = onDismiss,
                backgroundColor = backgroundColor
            )

            // 2. Scrollable Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding() // 处理系统导航栏高度
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Score Card
                ScoreCard(history.score, history.difficulty)

                Spacer(modifier = Modifier.height(24.dp))

                // Question & Answer
                DetailSectionCard(
                    title = if (history.type == "CN_TO_JP") "中文题目" else "日文题目",
                    content = history.question,
                    icon = Icons.Rounded.Info,
                    iconColor = NemoIndigo,
                    isSpeaking = playingAudioId == "question",
                    showSpeaker = history.type == "JP_TO_CN",
                    onSpeak = { onSpeak(history.question, "question") }
                )


                Spacer(modifier = Modifier.height(16.dp))

                DetailSectionCard(
                    title = "你的答案",
                    content = history.userAnswer,
                    icon = Icons.Rounded.Translate,
                    iconColor = Color(0xFF4CAF50) // Success Green
                )

                Spacer(modifier = Modifier.height(16.dp))

                DetailSectionCard(
                    title = "参考答案",
                    content = history.standardAnswer,
                    icon = Icons.Rounded.CheckCircle,
                    iconColor = NemoIndigo,
                    isSpeaking = playingAudioId == "standard",
                    showSpeaker = history.type == "CN_TO_JP",
                    onSpeak = { onSpeak(history.standardAnswer, "standard") }
                )


                Spacer(modifier = Modifier.height(16.dp))

                // AI Feedback
                DetailSectionCard(
                    title = "AI 评价与解析",
                    content = history.feedback,
                    icon = Icons.Rounded.Psychology,
                    iconColor = MaterialTheme.colorScheme.secondary
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun ScoreCard(score: Int, difficulty: String) {
    val colorScheme = MaterialTheme.colorScheme
    val scoreColor = when {
        score >= 80 -> NemoSecondary
        score >= 60 -> NemoYellow
        else -> NemoDanger
    }

    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = scoreColor.copy(alpha = 0.08f),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, scoreColor.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "AI 综合评分",
                    style = MaterialTheme.typography.labelMedium,
                    color = scoreColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (score >= 85) "表现卓越！" else if (score >= 60) "合格" else "需继续努力",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "题目难度: $difficulty",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
            }
            
            // Score Display
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "$score",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 48.sp
                    ),
                    color = scoreColor
                )
            }
        }
    }
}

@Composable
private fun DetailSectionCard(
    title: String,
    content: String,
    icon: ImageVector,
    iconColor: Color,
    showSpeaker: Boolean = false,
    isSpeaking: Boolean = false,
    onSpeak: () -> Unit = {}
) {

    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (isDark) colorScheme.surfaceContainerLow else Color.White,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(iconColor.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = iconColor
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                
                if (showSpeaker) {
                    SpeakerButton(
                        isPlaying = isSpeaking,
                        onClick = onSpeak,
                        size = 32.dp,
                        backgroundColor = iconColor.copy(alpha = 0.05f),
                        tint = iconColor
                    )
                }
            }

            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = content,
                style = MaterialTheme.typography.bodyLarge.copy(
                    lineHeight = 26.sp
                ),
                color = colorScheme.onSurfaceVariant
            )
        }
    }
}
