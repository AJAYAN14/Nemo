package com.jian.nemo.feature.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import com.jian.nemo.core.designsystem.theme.IosColors
import com.jian.nemo.core.designsystem.theme.SettingsPalette
import com.jian.nemo.core.designsystem.theme.UserPalette
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jian.nemo.core.designsystem.icon.IsoluminantPalette
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.ui.compositionlocal.LocalSettingIconStyle
import com.jian.nemo.core.ui.component.common.CommonHeader
import com.jian.nemo.core.ui.component.liquid.FrostedLiquidGlassButton
import com.jian.nemo.feature.settings.components.FloatingLiquidIsland
import com.jian.nemo.feature.settings.components.LeechSentinelCard
import com.jian.nemo.feature.settings.components.LiquidMemoryHud
import com.jian.nemo.feature.settings.components.RetentionPrecisionRuler
import com.jian.nemo.feature.settings.components.StepPipelineEditor
import com.jian.nemo.feature.settings.components.TemporalHorizonSection
import com.jian.nemo.feature.settings.components.UnsavedChangesDialog
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlin.math.roundToInt

/**
 * 记忆算法调校视界 (SRS Memory Algorithm Studio)
 *
 * 全新重构版本：
 * 1. 深度融合 Nemo 标志性的「底层贯穿录制 + 悬浮液态玻璃顶栏 + 柔和多阶垂直渐变遮罩 (Backdrop Lens + Gradient Mask)」；
 * 2. 多维度语义色彩系统：赋予每个卡片模块贴合其物理隐喻的专属色系，告别单一紫色；
 * 3. 顶部操作栏集成一键「恢复默认设置」功能（仅非默认状态下显示，采用 30/48 比例与 Replay 图标）；
 * 4. 时间胶囊序号像素级绝对居中，阈值计数器与目标保留率均配备大号字号与机械翻牌物理动效；
 * 5. 悬浮操作岛改用全透明无底色液态玻璃（ClearLiquidGlassButton），实时透镜折射。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedLearningSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // 本地即时响应状态
    var stepsInput by remember(uiState.learningSteps) { mutableStateOf(uiState.learningSteps) }
    var relearningStepsInput by remember(uiState.relearningSteps) { mutableStateOf(uiState.relearningSteps) }
    var limitInput by remember(uiState.learnAheadLimit) { mutableFloatStateOf(uiState.learnAheadLimit.toFloat()) }
    var leechThresholdInput by remember(uiState.leechThreshold) { mutableIntStateOf(uiState.leechThreshold.coerceIn(1, 12)) }
    var leechActionInput by remember(uiState.leechAction) {
        mutableStateOf(if (uiState.leechAction == "bury_today") "bury_today" else "skip")
    }
    var retentionInput by remember(uiState.targetRetention) { mutableFloatStateOf(uiState.targetRetention) }
    var batchSizeInput by remember(uiState.sessionBatchSize) { mutableIntStateOf(uiState.sessionBatchSize) }
    var maxReviewsInput by remember(uiState.maxDailyReviews) { mutableIntStateOf(uiState.maxDailyReviews) }
    var strategyInput by remember(uiState.backlogNewItemStrategy) { mutableStateOf(uiState.backlogNewItemStrategy) }

    // 弹窗状态管理
    var showExitConfirmation by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    // 判断当前配置是否为默认黄金推荐值（不是默认值才显示恢复按钮）
    val isDefaultSettings = remember(
        stepsInput,
        relearningStepsInput,
        limitInput,
        leechThresholdInput,
        leechActionInput,
        retentionInput,
        batchSizeInput,
        maxReviewsInput,
        strategyInput
    ) {
        stepsInput.trim() == "1 10" &&
        relearningStepsInput.trim() == "1 10" &&
        limitInput.toInt() == 20 &&
        leechThresholdInput == 5 &&
        leechActionInput == "skip" &&
        (retentionInput * 100).roundToInt() == 90 &&
        batchSizeInput == 20 &&
        maxReviewsInput == 0 &&
        strategyInput == "taste"
    }

    // 判断是否有未保存的更改
    val hasUnsavedChanges = remember(
        stepsInput,
        relearningStepsInput,
        limitInput,
        leechThresholdInput,
        leechActionInput,
        retentionInput,
        batchSizeInput,
        maxReviewsInput,
        strategyInput,
        uiState
    ) {
        stepsInput.trim() != uiState.learningSteps.trim() ||
        relearningStepsInput.trim() != uiState.relearningSteps.trim() ||
        limitInput.toInt() != uiState.learnAheadLimit ||
        leechThresholdInput != uiState.leechThreshold ||
        leechActionInput != (if (uiState.leechAction == "bury_today") "bury_today" else "skip") ||
        (retentionInput * 100).toInt() != (uiState.targetRetention * 100).toInt() ||
        batchSizeInput != uiState.sessionBatchSize ||
        maxReviewsInput != uiState.maxDailyReviews ||
        strategyInput != uiState.backlogNewItemStrategy
    }

    // 拦截系统返回手势与按键
    BackHandler(enabled = hasUnsavedChanges) {
        showExitConfirmation = true
    }

    val isPlumpDuo = LocalSettingIconStyle.current == SettingIconStyle.PLUMP_DUO

    // 多维度语义色彩体系：支持原有专属原色与 OKLCH 等亮度调色板双方案自适应
    val retentionColor = if (isPlumpDuo) IsoluminantPalette.LilacViolet else IosColors.Purple
    val learningStepColor = if (isPlumpDuo) IsoluminantPalette.SkyIndigo else UserPalette.PrimaryGlassTintLight
    val relearningStepColor = if (isPlumpDuo) IsoluminantPalette.WarmAmber else IosColors.OrangeDark
    val leechColor = if (isPlumpDuo) IsoluminantPalette.CoralFlame else SettingsPalette.CoralRose
    val temporalColor = if (isPlumpDuo) IsoluminantPalette.LakesideCyan else SettingsPalette.CyanMint
    val backlogColor = if (isPlumpDuo) IsoluminantPalette.EmeraldGreen else IosColors.Green

    val backgroundColor = MaterialTheme.colorScheme.background
    val backdrop = rememberLayerBackdrop()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // 保存逻辑封装
    val onSaveAndExit = {
        if (stepsInput.isNotBlank() && relearningStepsInput.isNotBlank()) {
            viewModel.onEvent(
                SettingsEvent.SaveAdvancedLearningSettings(
                    learningSteps = stepsInput.trim(),
                    relearningSteps = relearningStepsInput.trim(),
                    learnAheadLimit = limitInput.toInt(),
                    leechThreshold = leechThresholdInput,
                    leechAction = leechActionInput,
                    targetRetention = retentionInput,
                    sessionBatchSize = batchSizeInput,
                    maxDailyReviews = maxReviewsInput,
                    backlogNewItemStrategy = strategyInput
                )
            )
            onNavigateBack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // 1. 底层页面滚动内容（被 backdrop 采样源录制，全屏贯穿至状态栏）
        Column(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 顶部弹性占位：未滑动时避开顶栏，向上滚动时平滑穿入顶栏遮罩与液态玻璃下方
            Spacer(modifier = Modifier.height(statusBarTop + 60.dp + 12.dp))

            // 1. 流体记忆视界 HUD 仿真卡片 (动态衰减曲面 + SloshGauge 负荷蓄水池)
            LiquidMemoryHud(
                targetRetention = retentionInput,
                accentColor = retentionColor
            )

            // 2. 目标保留率磁吸精密标尺 (Modern Fractional Picker - 锁定水平手势与微调按钮)
            RetentionPrecisionRuler(
                targetRetention = retentionInput,
                onRetentionChange = { retentionInput = it },
                accentColor = retentionColor
            )

            // 3. 初次学习阶梯时空管道 (Step Pipeline) — 探索蓝
            StepPipelineEditor(
                title = "新卡片初学阶梯",
                subtitle = "新知识首次记忆时的短时巩固序列 (单位: 分钟)",
                stepsString = stepsInput,
                onStepsChange = { stepsInput = it },
                accentColor = learningStepColor
            )

            // 4. 遗忘重学阶梯时空管道 (Relearning Steps) — 暖琥珀橙
            StepPipelineEditor(
                title = "遗忘重学阶梯",
                subtitle = "遗忘卡片重新激活时的强化复习序列 (单位: 分钟)",
                stepsString = relearningStepsInput,
                onStepsChange = { relearningStepsInput = it },
                accentColor = relearningStepColor
            )

            // 5. 积压排产与微会话控制 — 活力翠绿 (方案 2.4 核心落地)
            com.jian.nemo.feature.settings.components.BacklogControllerCard(
                sessionBatchSize = batchSizeInput,
                onBatchSizeChange = { batchSizeInput = it },
                maxDailyReviews = maxReviewsInput,
                onMaxReviewsChange = { maxReviewsInput = it },
                strategy = strategyInput,
                onStrategyChange = { strategyInput = it },
                accentColor = backlogColor
            )

            // 6. 水蛭防爆哨兵 (机械翻牌大号步进器 + 橡胶果冻分段器) — 守护玫瑰红
            LeechSentinelCard(
                leechThreshold = leechThresholdInput,
                onThresholdChange = { leechThresholdInput = it },
                leechAction = leechActionInput,
                onActionChange = { leechActionInput = it },
                accentColor = leechColor
            )

            // 7. 提前复习时间视界 — 极光薄荷青
            TemporalHorizonSection(
                limitMinutes = limitInput,
                onLimitChange = { limitInput = it },
                accentColor = temporalColor
            )

            // 底部预留悬浮操作岛和手势区安全高度
            Spacer(modifier = Modifier.height(navBarBottom + 84.dp))
        }

        // 2. 悬浮顶栏与液态玻璃按钮（带柔和多阶垂直渐变保护遮罩，读取 backdrop 物理透镜折射）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        0.0f to backgroundColor,
                        0.70f to backgroundColor.copy(alpha = 0.98f),
                        0.88f to backgroundColor.copy(alpha = 0.60f),
                        1.0f to Color.Transparent
                    )
                )
                .padding(bottom = 20.dp)
        ) {
            CommonHeader(
                title = "记忆算法调校",
                onBack = {
                    if (hasUnsavedChanges) {
                        showExitConfirmation = true
                    } else {
                        onNavigateBack()
                    }
                },
                backgroundColor = Color.Transparent,
                backdrop = backdrop,
                actions = {
                    // 右侧一键「恢复默认」流体玻璃按钮：仅当非默认配置时显示，严格采用 30/48 比例与环形 Replay 图标
                    AnimatedVisibility(
                        visible = !isDefaultSettings,
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut()
                    ) {
                        FrostedLiquidGlassButton(
                            onClick = { showResetConfirmDialog = true },
                            backdrop = backdrop,
                            shape = CircleShape,
                            contentPadding = PaddingValues(0.dp),
                            isInteractive = true,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Replay,
                                contentDescription = "恢复默认设置",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }
            )
        }

        // 3. 底部流体悬浮操作岛 (有修改时以果冻弹簧滑出，应用按钮为全透明无底色液态玻璃)
        FloatingLiquidIsland(
            visible = hasUnsavedChanges,
            onApply = onSaveAndExit,
            backdrop = backdrop,
            accentColor = retentionColor,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = navBarBottom + 8.dp)
        )

        // 4. 恢复默认二次确认弹窗 (采用项目通用统一弹窗 NemoDialog)
        if (showResetConfirmDialog) {
            com.jian.nemo.core.ui.component.NemoDialog(
                onDismissRequest = { showResetConfirmDialog = false },
                title = "恢复默认算法配置",
                confirmText = "确认恢复",
                dismissText = "取消",
                confirmButtonColor = retentionColor,
                onConfirm = {
                    stepsInput = "1 10"
                    relearningStepsInput = "1 10"
                    limitInput = 20f
                    leechThresholdInput = 5
                    leechActionInput = "skip"
                    retentionInput = 0.90f
                    batchSizeInput = 20
                    maxReviewsInput = 0
                    strategyInput = "taste"
                    showResetConfirmDialog = false
                },
                content = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "确定要将记忆算法配置恢复为系统黄金推荐值吗？",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("• 新卡初学阶段: 1 10 分钟", style = MaterialTheme.typography.labelSmall)
                                Text("• 遗忘重学阶段: 1 10 分钟", style = MaterialTheme.typography.labelSmall)
                                Text("• 提前复习容差: 20 分钟", style = MaterialTheme.typography.labelSmall)
                                Text("• 顽固卡片判定阈值: 5 次", style = MaterialTheme.typography.labelSmall)
                                Text("• 顽固卡片处置行为: 冻结卡片", style = MaterialTheme.typography.labelSmall)
                                Text("• FSRS 目标保留率: 90%", style = MaterialTheme.typography.labelSmall)
                                Text("• 单组微会话容量: 20 词", style = MaterialTheme.typography.labelSmall)
                                Text("• 每日复习上限: 不设限制", style = MaterialTheme.typography.labelSmall)
                                Text("• 积压新词策略: 微量尝鲜", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            )
        }

        // 5. 未保存退出确认弹窗
        if (showExitConfirmation) {
            UnsavedChangesDialog(
                onSaveAndExit = onSaveAndExit,
                onDiscardChanges = onNavigateBack,
                onDismiss = { showExitConfirmation = false },
                accentColor = retentionColor
            )
        }
    }
}
