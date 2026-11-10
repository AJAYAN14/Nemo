package com.jian.nemo.feature.learning.presentation

import com.jian.nemo.core.designsystem.theme.screenBackground
import com.jian.nemo.core.designsystem.theme.LearningPalette

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Report
import androidx.compose.material.icons.rounded.Share
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import com.jian.nemo.core.ui.component.liquid.FrostedLiquidGlassButton
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.jian.nemo.feature.learning.R
import androidx.compose.runtime.CompositionLocalProvider
import com.jian.nemo.core.ui.compositionlocal.LocalSettingIconStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.jian.nemo.feature.learning.presentation.components.dialogs.ContentReportDialog
import androidx.compose.animation.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.jian.nemo.feature.learning.presentation.components.common.LearningFinishedContent
import com.jian.nemo.feature.learning.presentation.components.common.LearnHeader
import com.jian.nemo.feature.learning.presentation.components.common.LearnHeaderMoreMenu
import com.jian.nemo.feature.learning.presentation.components.common.LearnHeaderMoreMenuButton
import com.jian.nemo.feature.learning.presentation.components.common.LearnHeaderMoreMenuOverlay
import com.jian.nemo.core.ui.component.common.LocalLiquidMenuAnchorBounds
import com.jian.nemo.core.ui.component.common.LocalLiquidMenuMotionState
import com.jian.nemo.core.ui.component.liquid.menu.rememberLiquidMenuMotionState
import androidx.compose.ui.geometry.Rect
import com.jian.nemo.feature.learning.presentation.components.common.WaitingBackground
import com.jian.nemo.feature.learning.presentation.components.common.WaitingContent
import com.jian.nemo.core.designsystem.theme.NemoSurfaceBackground
import com.jian.nemo.core.designsystem.theme.NemoSurfaceBackgroundDark
import com.jian.nemo.core.ui.component.common.NemoSnackbarType
import com.jian.nemo.feature.learning.presentation.components.dialogs.TypingPracticeDialog
import com.jian.nemo.feature.learning.presentation.components.cards.SRSLearningCard
import com.jian.nemo.feature.learning.presentation.components.cards.SRSGrammarCard
import com.jian.nemo.feature.learning.presentation.components.guide.RatingGuideScreen
import com.jian.nemo.feature.learning.presentation.components.srs.SRSActionArea
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.jian.nemo.core.ui.component.animation.NemoChasingDotsLoader
import com.jian.nemo.core.ui.component.common.CommonHeader
import com.jian.nemo.core.ui.component.dialog.DurationPickerDialog


@Composable

fun LearningScreen(
    viewModel: LearningViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    initialMode: LearningMode = LearningMode.Word
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(key1 = initialMode) {
        viewModel.onEvent(LearningEvent.StartLearning(initialMode))
    }

    // 🆕 监听生命周期与离开事件，确保“学习用时”只在用户停留于学习界面时计算
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> {
                    viewModel.onEvent(LearningEvent.ResumeTimer)
                }
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> {
                    viewModel.onEvent(LearningEvent.PauseTimer)
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.onEvent(LearningEvent.PauseTimer)
        }
    }

    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5
    val backgroundColor = MaterialTheme.colorScheme.screenBackground

    // 状态
    var showRatingGuide by rememberSaveable { mutableStateOf(false) }
    var showAnswerDelayHint by rememberSaveable { mutableStateOf(false) }
    var showAnswerDelayHintSec by rememberSaveable { mutableStateOf(1) }
    var showAutoRevealDurationDialog by rememberSaveable { mutableStateOf(false) }
    var showDelayDurationDialog by rememberSaveable { mutableStateOf(false) }

    val delayDurationLabel = when (uiState.showAnswerDelayMs) {
        2000L -> "2s"
        3000L -> "3s"
        4000L -> "4s"
        5000L -> "5s"
        else -> "${uiState.showAnswerDelayMs / 1000}s"
    }

    var isMenuExpanded by remember { mutableStateOf(false) }
    var showSharePosterDialog by remember { mutableStateOf(false) }
    val finishBackdrop = rememberLayerBackdrop()
    val waitingBackdrop = rememberLayerBackdrop()
    val learningBackdrop = rememberLayerBackdrop()
    var triggerWaitingBlast by remember { mutableStateOf<(() -> Unit)?>(null) }

    // 动态高斯模糊半径 (0dp -> 18dp 平滑过渡，展示分享海报时自动触发背景模糊遮罩)
    val animatedBlurRadius by animateDpAsState(
        targetValue = if (showSharePosterDialog) 18.dp else 0.dp,
        animationSpec = tween(durationMillis = 240),
        label = "learningBlur"
    )

    val isWaiting = uiState.status == LearningStatus.Waiting
    val isFinished = uiState.status != LearningStatus.Loading &&
            !isWaiting &&
            ((uiState.learningMode == LearningMode.Word && uiState.currentWord == null) ||
             (uiState.learningMode == LearningMode.Grammar && uiState.currentGrammar == null))
    val remainingTaskCount = if (uiState.learningMode == LearningMode.Word)
        uiState.wordList.size
    else
        uiState.grammarList.size

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val statusBarTopPx = with(density) { statusBarTop.toPx() }
    val defaultAnchor = remember(screenWidthPx, statusBarTopPx) {
        val buttonSizePx = with(density) { 48.dp.toPx() }
        val rightMarginPx = with(density) { 16.dp.toPx() }
        val topOffsetPx = statusBarTopPx + with(density) { 6.dp.toPx() }
        val left = screenWidthPx - rightMarginPx - buttonSizePx
        Rect(left = left, top = topOffsetPx, right = left + buttonSizePx, bottom = topOffsetPx + buttonSizePx)
    }
    val menuAnchorBounds = remember { mutableStateOf<Rect?>(defaultAnchor) }
    val liquidMenuMotionState = rememberLiquidMenuMotionState()

    CompositionLocalProvider(
        LocalSettingIconStyle provides uiState.settingIconStyle,
        LocalLiquidMenuAnchorBounds provides menuAnchorBounds,
        LocalLiquidMenuMotionState provides liquidMenuMotionState
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
        ) {
            // 1. 底层录制画布（包含底色、渐变与主体卡片内容，整体录入 learningBackdrop，消除透明空洞）
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(learningBackdrop)
            ) {
                // 顶部纯净中性明度光晕（贯穿系统状态栏，无彩色偏色，提供纯净黑白明度梯度以激发水晶水滴折射）
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(statusBarTop + 88.dp)
                        .alpha(if (isMenuExpanded) 0f else 1f)
                        .background(
                            Brush.verticalGradient(
                                colors = if (isDarkTheme) {
                                    listOf(
                                        Color.White.copy(alpha = 0.08f),
                                        Color.White.copy(alpha = 0.02f),
                                        Color.Transparent
                                    )
                                } else {
                                    listOf(
                                        Color.White.copy(alpha = 0.85f),
                                        Color.White.copy(alpha = 0.40f),
                                        Color.Transparent
                                    )
                                }
                            )
                        )
                )

                // 当结算完成时，在最外层绘制贯穿系统状态栏的顶部主题色渐变光晕
                if (isFinished) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = if (isDarkTheme) 0.22f else 0.14f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }

                // 冷却等待流光背景透明度渐变 (240ms 柔和淡入/淡出)
                val waitingAlpha by animateFloatAsState(
                    targetValue = if (isWaiting) 1f else 0f,
                    animationSpec = tween(durationMillis = 240),
                    label = "waitingBackgroundAlpha"
                )

                // 全屏铺设 WebGL 2 原生流光粒子背景（常驻预热消除冷启动延迟，非等待时自动 onPause 零能耗，等待时第 1 帧零延迟平滑呈现）
                WaitingBackground(
                    backdrop = waitingBackdrop,
                    isDark = isDarkTheme,
                    isActive = isWaiting,
                    onControllerReady = { trigger ->
                        triggerWaitingBlast = trigger
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(waitingAlpha)
                )

                // 页面主体内容 (Z = 0，全屏向上贯穿至状态栏，使得可滚动卡片向上滑动时能够穿透进入顶栏背后)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (animatedBlurRadius > 0.dp) Modifier.blur(radius = animatedBlurRadius)
                            else Modifier
                        )
                        .padding(bottom = navBarBottom)
                        .padding(horizontal = 16.dp)
                ) {
                    when (uiState.status) {
                        LearningStatus.Loading -> {
                            Box(modifier = Modifier.fillMaxSize().padding(top = statusBarTop + 60.dp), contentAlignment = Alignment.Center) {
                                NemoChasingDotsLoader()
                            }
                        }
                        LearningStatus.Waiting -> {
                            val pendingCount = if (uiState.learningMode == LearningMode.Word) uiState.wordList.size else uiState.grammarList.size
                            val activeQueueCount = (uiState.queueLearningCount + uiState.queueReviewCount + uiState.queueRelearnCount)
                                .let { if (it > 0) it else pendingCount }
                            WaitingContent(
                                until = uiState.waitingUntil,
                                onContinue = { viewModel.onEvent(LearningEvent.ResumeFromWaiting) },
                                onRest = onNavigateBack,
                                completedCount = uiState.completedThisSession,
                                queueCount = activeQueueCount,
                                isDark = isDarkTheme,
                                backdrop = waitingBackdrop,
                                onTriggerCenterBlast = triggerWaitingBlast,
                                showBackground = false,
                                modifier = Modifier.padding(top = statusBarTop + 60.dp)
                            )
                        }
                        else -> {
                            LearningContent(
                                uiState = uiState,
                                onEvent = viewModel::onEvent,
                                backgroundColor = backgroundColor,
                                showShareDialog = showSharePosterDialog,
                                onDismissShareDialog = { showSharePosterDialog = false },
                                onShowAnswerBlocked = { remainingSec ->
                                    showAnswerDelayHintSec = remainingSec
                                    showAnswerDelayHint = true
                                },
                                onBackClick = onNavigateBack,
                                topContentPadding = statusBarTop + 72.dp + 24.dp
                            )
                        }
                    }
                }
            }

            // 2. 悬浮固定顶栏与液态玻璃按钮（标杆四阶垂直渐变保护遮罩，读取 learningBackdrop 激活真实物理折射）
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
                if (isFinished) {
                    // 结算状态下：极简轻量化顶部导航（CommonHeader 架构，居中打卡徽章，右侧分享）
                    CommonHeader(
                        title = "",
                        onBack = onNavigateBack,
                        backgroundColor = Color.Transparent,
                        backdrop = learningBackdrop,
                        backButtonSize = 48.dp,
                        chromaticAberration = false,
                        centerContent = {
                            Surface(
                                shape = CircleShape,
                                color = if (isDarkTheme) LearningPalette.BannerOrangeBgDark else LearningPalette.BannerOrangeBgLight,
                                border = BorderStroke(1.dp, if (isDarkTheme) LearningPalette.BannerOrangeBorderDark else LearningPalette.BannerOrangeBorderLight)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(text = "🔥", fontSize = 13.sp)
                                    Text(
                                        text = stringResource(R.string.learn_screen_badge_daily_streak, uiState.dailyStreak),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isDarkTheme) LearningPalette.BannerOrangeTextDark else LearningPalette.BannerOrangeTextLight
                                    )
                                }
                            }
                        },
                        actions = {
                            FrostedLiquidGlassButton(
                                onClick = { showSharePosterDialog = true },
                                backdrop = learningBackdrop,
                                shape = CircleShape,
                                hasLens = true,
                                chromaticAberration = false,
                                contentPadding = PaddingValues(0.dp),
                                isInteractive = true,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_share_ios),
                                    contentDescription = stringResource(R.string.learn_screen_desc_share),
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    )
                } else {
                    // Header
                    LearnHeader(
                        learningMode = uiState.learningMode,
                        completedCount = uiState.completedToday,
                        dailyGoal = uiState.dailyGoal,
                        totalCount = remainingTaskCount,
                        onClose = onNavigateBack,
                        canUndo = uiState.canUndo,
                        onUndo = { viewModel.onEvent(LearningEvent.Undo) },
                        queueNewCount = uiState.queueNewCount,
                        queueLearningCount = uiState.queueLearningCount,
                        queueReviewCount = uiState.queueReviewCount,
                        queueRelearnCount = uiState.queueRelearnCount,
                        backdrop = learningBackdrop,
                        menu = {
                            if (!showRatingGuide) {
                                LearnHeaderMoreMenuButton(
                                    isExpanded = isMenuExpanded,
                                    onClick = { isMenuExpanded = !isMenuExpanded },
                                    backdrop = learningBackdrop
                                )
                            }
                        }
                    )
                }
            }

            // 2. 页面全屏手势遮罩与液态玻璃悬浮操作菜单 (同级兄弟节点采样 learningBackdrop，背景保持完全透明清晰)
            LearnHeaderMoreMenuOverlay(
                isVisible = isMenuExpanded,
                backdrop = learningBackdrop,
                anchorBounds = menuAnchorBounds.value,
                onDismiss = { isMenuExpanded = false },
                remainingCount = remainingTaskCount,
                onSuspend = { viewModel.onEvent(LearningEvent.SuspendCurrent) },
                onBury = { viewModel.onEvent(LearningEvent.BuryCurrent) },
                onReportError = { viewModel.onEvent(LearningEvent.OpenReportErrorDialog) },
                onShowRatingGuide = {
                    isMenuExpanded = false
                    showRatingGuide = true
                },
                isAutoAudioEnabled = uiState.isAutoAudioEnabled,
                onToggleAutoAudio = if (uiState.learningMode == LearningMode.Word) {
                    { viewModel.onEvent(LearningEvent.ToggleAutoPlayAudio(it)) }
                } else null,
                isShowAnswerDelayEnabled = uiState.isShowAnswerDelayEnabled,
                onToggleShowAnswerDelay = { viewModel.onEvent(LearningEvent.ToggleShowAnswerDelay(it)) },
                showAnswerDelayDurationLabel = delayDurationLabel,
                onCycleShowAnswerDelayDuration = { viewModel.onEvent(LearningEvent.CycleShowAnswerDelayDuration) },
                onOpenShowAnswerDelayDialog = {
                    isMenuExpanded = false
                    showDelayDurationDialog = true
                },
                isAutoRevealAnswerEnabled = if (uiState.learningMode == LearningMode.Word) uiState.isAutoRevealAnswerEnabled else uiState.isGrammarAutoRevealAnswerEnabled,
                onToggleAutoRevealAnswer = {
                    if (uiState.learningMode == LearningMode.Word) {
                        viewModel.onEvent(LearningEvent.ToggleAutoRevealAnswer(it))
                    } else {
                        viewModel.onEvent(LearningEvent.ToggleGrammarAutoRevealAnswer(it))
                    }
                },
                autoRevealAnswerDurationLabel = "${(if (uiState.learningMode == LearningMode.Word) uiState.autoRevealAnswerMs else uiState.grammarAutoRevealAnswerMs) / 1000}s",
                onCycleAutoRevealAnswerDuration = {
                    if (uiState.learningMode == LearningMode.Word) {
                        viewModel.onEvent(LearningEvent.CycleAutoRevealAnswerDuration)
                    } else {
                        viewModel.onEvent(LearningEvent.CycleGrammarAutoRevealAnswerDuration)
                    }
                },
                onOpenAutoRevealAnswerDialog = {
                    isMenuExpanded = false
                    showAutoRevealDurationDialog = true
                },
                isWhiteboardEnabled = uiState.isWhiteboardEnabled,
                onToggleWhiteboard = { viewModel.onEvent(LearningEvent.ToggleWhiteboard(it)) },
                isDarkMode = uiState.isDarkMode,
                onSelectDarkMode = { viewModel.onEvent(LearningEvent.SetDarkMode(it)) },
                onCycleDarkMode = { viewModel.onEvent(LearningEvent.CycleDarkMode) }
            )

            // 动态构建通知列表（最新产生的通知插入在最上方）
            val notificationList = remember(
                uiState.successMessage,
                uiState.error,
                showAnswerDelayHint, showAnswerDelayHintSec
            ) {
                buildList {
                    if (showAnswerDelayHint) {
                        add(
                            com.jian.nemo.core.ui.component.common.NemoNotificationData(
                                id = "delay_hint",
                                message = context.getString(R.string.learn_screen_delay_hint, showAnswerDelayHintSec),
                                type = NemoSnackbarType.WARNING,
                                icon = Icons.Rounded.AccessTime,
                                autoDismissMs = 4000L,
                                onDismiss = { showAnswerDelayHint = false }
                            )
                        )
                    }
                    if (uiState.error != null) {
                        add(
                            com.jian.nemo.core.ui.component.common.NemoNotificationData(
                                id = "error_${uiState.error}",
                                message = uiState.error ?: "",
                                type = NemoSnackbarType.ERROR,
                                icon = Icons.Rounded.Report,
                                autoDismissMs = 4000L,
                                onDismiss = { viewModel.onEvent(LearningEvent.ClearErrorMessage) }
                            )
                        )
                    }
                    if (uiState.successMessage != null) {
                        add(
                            com.jian.nemo.core.ui.component.common.NemoNotificationData(
                                id = "success_${uiState.successMessage}",
                                message = uiState.successMessage ?: "",
                                type = NemoSnackbarType.SUCCESS,
                                icon = Icons.Rounded.CheckCircle,
                                autoDismissMs = 4000L,
                                onDismiss = { viewModel.onEvent(LearningEvent.ClearSuccessMessage) }
                            )
                        )
                    }
                }
            }

            // 顶部垂直堆叠通知容器 (消除位置重叠与硬编码 72.dp)
            com.jian.nemo.core.ui.component.common.NemoNotificationStackHost(
                notifications = notificationList,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 8.dp)
            )

            // 评分说明覆盖层 (Edge-to-Edge with Animation)
            AnimatedVisibility(
                visible = showRatingGuide,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
            ) {
                RatingGuideScreen(
                    onDismiss = { showRatingGuide = false }
                )
            }

            // 内容报错确认弹窗 (UI/UX Pro Max Style)
            if (uiState.showReportErrorDialog) {
                ContentReportDialog(
                    learningMode = uiState.learningMode,
                    onDismiss = { viewModel.onEvent(LearningEvent.CancelReportErrorDialog) },
                    onConfirm = { errorType, desc -> viewModel.onEvent(LearningEvent.ReportContentError(errorType, desc)) }
                )
            }

            if (showAutoRevealDurationDialog) {
                val currentSec = ((if (uiState.learningMode == LearningMode.Word) uiState.autoRevealAnswerMs else uiState.grammarAutoRevealAnswerMs) / 1000).toInt()
                DurationPickerDialog(
                    title = stringResource(R.string.learn_screen_dialog_auto_reveal_title),
                    initialSeconds = currentSec,
                    onDismissRequest = { showAutoRevealDurationDialog = false },
                    onConfirm = { seconds ->
                        showAutoRevealDurationDialog = false
                        val ms = seconds * 1000L
                        if (uiState.learningMode == LearningMode.Word) {
                            viewModel.onEvent(LearningEvent.SetAutoRevealAnswerMs(ms))
                            viewModel.onEvent(LearningEvent.ToggleAutoRevealAnswer(true))
                        } else {
                            viewModel.onEvent(LearningEvent.SetGrammarAutoRevealAnswerMs(ms))
                            viewModel.onEvent(LearningEvent.ToggleGrammarAutoRevealAnswer(true))
                        }
                    }
                )
            }

            if (showDelayDurationDialog) {
                val currentSec = (uiState.showAnswerDelayMs / 1000).toInt()
                DurationPickerDialog(
                    title = stringResource(R.string.learn_screen_dialog_delay_title),
                    initialSeconds = currentSec,
                    onDismissRequest = { showDelayDurationDialog = false },
                    onConfirm = { seconds ->
                        showDelayDurationDialog = false
                        viewModel.onEvent(LearningEvent.SetShowAnswerDelayMs(seconds * 1000L))
                        viewModel.onEvent(LearningEvent.ToggleShowAnswerDelay(true))
                    }
                )
            }
        }
    }
}

@Composable
fun LearningContent(
    uiState: LearningUiState,
    onEvent: (LearningEvent) -> Unit,
    backgroundColor: Color, // 显式指定 Color 类型
    showShareDialog: Boolean = false,
    onDismissShareDialog: () -> Unit = {},
    onShowAnswerBlocked: (Int) -> Unit,
    onBackClick: () -> Unit = {},
    topContentPadding: androidx.compose.ui.unit.Dp = 0.dp
) {
    if (uiState.learningMode == LearningMode.Word) {
        WordLearningContent(
            uiState = uiState,
            onEvent = onEvent,
            backgroundColor = backgroundColor, // 新增
            showShareDialog = showShareDialog,
            onDismissShareDialog = onDismissShareDialog,
            onShowAnswerBlocked = onShowAnswerBlocked,
            onBackClick = onBackClick,
            topContentPadding = topContentPadding
        )
    } else {
        GrammarLearningContent(
            uiState = uiState,
            onEvent = onEvent,
            backgroundColor = backgroundColor, // 新增
            showShareDialog = showShareDialog,
            onDismissShareDialog = onDismissShareDialog,
            onShowAnswerBlocked = onShowAnswerBlocked,
            onBackClick = onBackClick,
            topContentPadding = topContentPadding
        )
    }
}

@Composable
fun WordLearningContent(
    uiState: LearningUiState,
    onEvent: (LearningEvent) -> Unit,
    backgroundColor: Color, // 显式指定 Color 类型
    showShareDialog: Boolean = false,
    onDismissShareDialog: () -> Unit = {},
    onShowAnswerBlocked: (Int) -> Unit,
    onBackClick: () -> Unit = {},
    topContentPadding: androidx.compose.ui.unit.Dp = 0.dp
) {
    // 跟打练习对话框状态
    var showTypingDialog by remember { mutableStateOf(false) }

    val autoRevealAnim = remember { androidx.compose.animation.core.Animatable(1f) }

    LaunchedEffect(
        uiState.currentIndex,
        uiState.isAnswerShown,
        uiState.status,
        uiState.isAutoRevealAnswerEnabled,
        uiState.autoRevealAnswerMs
    ) {
        if (uiState.isAutoRevealAnswerEnabled && !uiState.isAnswerShown && uiState.status == LearningStatus.Learning) {
            autoRevealAnim.snapTo(1f)
            autoRevealAnim.animateTo(
                targetValue = 0f,
                animationSpec = androidx.compose.animation.core.tween(
                    durationMillis = uiState.autoRevealAnswerMs.toInt(),
                    easing = androidx.compose.animation.core.LinearEasing
                )
            )
            onEvent(LearningEvent.ShowAnswer)
        } else {
            autoRevealAnim.snapTo(1f)
        }
    }

    // 跟打练习对话框
    if (showTypingDialog && uiState.currentWord != null) {
        TypingPracticeDialog(
            word = uiState.currentWord,
            onDismiss = { showTypingDialog = false }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (uiState.currentWord != null) {
            val cardBackdrop = rememberLayerBackdrop()
            Box(modifier = Modifier.weight(1f)) {
                 // Card Content
                 Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .layerBackdrop(cardBackdrop),
                    contentAlignment = Alignment.TopCenter
                 ) {
                     // 单卡封闭模式：禁止左右手势翻页，仅由评分与撤销驱动卡片切换动效
                     AnimatedContent(
                        targetState = uiState.currentWord to uiState.sessionProcessedCount,
                        transitionSpec = {
                            val animSpec = tween<Float>(durationMillis = 220, easing = FastOutSlowInEasing)
                            val offsetSpec = tween<IntOffset>(durationMillis = 220, easing = FastOutSlowInEasing)
                            if (uiState.slideDirection == SlideDirection.FORWARD) {
                                (slideInHorizontally(animationSpec = offsetSpec) { width -> width } + fadeIn(animationSpec = animSpec)).togetherWith(
                                    slideOutHorizontally(animationSpec = offsetSpec) { width -> -width } + fadeOut(animationSpec = animSpec))
                            } else {
                                (slideInHorizontally(animationSpec = offsetSpec) { width -> -width } + fadeIn(animationSpec = animSpec)).togetherWith(
                                    slideOutHorizontally(animationSpec = offsetSpec) { width -> width } + fadeOut(animationSpec = animSpec))
                            }.using(
                                SizeTransform(clip = false)
                            )
                        },
                        label = "WordCardTransition"
                     ) { (targetWord, _) ->
                         if (targetWord != null) {
                         SRSLearningCard(
                             word = targetWord,
                             progress = if (uiState.dailyGoal > 0) uiState.completedToday.toFloat() / uiState.dailyGoal else 0f,
                             queueNewCount = uiState.queueNewCount,
                             queueLearningCount = uiState.queueLearningCount,
                             queueReviewCount = uiState.queueReviewCount,
                             queueRelearnCount = uiState.queueRelearnCount,
                             isAnswerShown = uiState.isAnswerShown,
                             cardBadge = LearningItem.WordItem(targetWord).cardBadge,
                             modifier = Modifier.fillMaxSize(),
                             onPracticeClick = {
                                 showTypingDialog = true
                             },
                             onSpeakWord = { onEvent(LearningEvent.SpeakWord(targetWord.hiragana, targetWord.chinese)) },
                             onSpeakExample = { japanese, chinese, id -> onEvent(LearningEvent.SpeakExample(japanese, chinese, id)) },
                             playingAudioId = uiState.playingAudioId,
                             isWhiteboardEnabled = uiState.isWhiteboardEnabled,
                             autoRevealProgress = if (uiState.isAutoRevealAnswerEnabled && !uiState.isAnswerShown) autoRevealAnim.value else null,
                             topPadding = topContentPadding
                         )
                         }
                     }
                 }

                 // SRS Action Area (Bottom)
                 SRSActionArea(
                     isAnswerShown = uiState.isAnswerShown,
                     isShowAnswerDelayEnabled = uiState.isShowAnswerDelayEnabled,
                     showAnswerAvailableAt = uiState.showAnswerAvailableAt,
                     ratingIntervals = uiState.ratingIntervals,
                     onShowAnswer = { onEvent(LearningEvent.ShowAnswer) },
                     onShowAnswerBlocked = onShowAnswerBlocked,
                     onRate = { quality -> onEvent(LearningEvent.RateWord(quality)) },
                     modifier = Modifier.align(Alignment.BottomCenter)
                 )
            }
        } else {
            val finishTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 60.dp + 16.dp
            if (uiState.shouldShowDailyGoalMet) {
                LearningFinishedContent(
                    title = stringResource(R.string.learn_screen_finish_daily_goal_title),
                    subtitle = stringResource(R.string.learn_screen_finish_daily_goal_subtitle),
                    completedToday = uiState.completedToday,
                    dailyGoal = uiState.dailyGoal,
                    dailyStreak = uiState.dailyStreak,
                    sessionDurationSeconds = uiState.sessionDurationSeconds,
                    sessionMaxCombo = uiState.sessionMaxCombo,
                    sessionNewCount = uiState.sessionNewCount,
                    sessionReviewCount = uiState.sessionReviewCount,
                    sessionRelearnCount = uiState.sessionRelearnCount,
                    tomorrowReviewForecastCount = uiState.tomorrowReviewForecastCount,
                    todayDueReviewCount = uiState.todayDueReviewCount,
                    showShareDialog = showShareDialog,
                    onDismissShareDialog = onDismissShareDialog,
                    onConfirmBonus = { bonusCount, bonusMode ->
                        onEvent(LearningEvent.StartBonusLearning(count = bonusCount, bonusMode = bonusMode, mode = LearningMode.Word))
                    },
                    onBackClick = onBackClick,
                    topContentPadding = finishTopPadding
                )
            } else if (uiState.sessionReviewCount + uiState.sessionNewCount + uiState.sessionRelearnCount > 0) {
                val remainingWords = (uiState.dailyGoal - uiState.completedToday).coerceAtLeast(0)
                LearningFinishedContent(
                    title = stringResource(R.string.learn_screen_finish_micro_session_title),
                    subtitle = stringResource(R.string.learn_screen_finish_micro_session_words_subtitle, uiState.dailyStreak, remainingWords),
                    completedToday = uiState.completedToday,
                    dailyGoal = uiState.dailyGoal,
                    dailyStreak = uiState.dailyStreak,
                    sessionDurationSeconds = uiState.sessionDurationSeconds,
                    sessionMaxCombo = uiState.sessionMaxCombo,
                    sessionNewCount = uiState.sessionNewCount,
                    sessionReviewCount = uiState.sessionReviewCount,
                    sessionRelearnCount = uiState.sessionRelearnCount,
                    tomorrowReviewForecastCount = uiState.tomorrowReviewForecastCount,
                    todayDueReviewCount = uiState.todayDueReviewCount,
                    showShareDialog = showShareDialog,
                    onDismissShareDialog = onDismissShareDialog,
                    onConfirmBonus = { bonusCount, bonusMode ->
                        onEvent(LearningEvent.StartBonusLearning(count = bonusCount, bonusMode = bonusMode, mode = LearningMode.Word))
                    },
                    onBackClick = onBackClick,
                    showBonusCard = false,
                    showNextGroupButton = true,
                    onNextGroupClick = {
                        onEvent(LearningEvent.StartLearning(mode = LearningMode.Word))
                    },
                    topContentPadding = finishTopPadding
                )
            } else {
                LearningFinishedContent(
                    title = stringResource(R.string.learn_screen_finish_no_words_title),
                    subtitle = stringResource(R.string.learn_screen_finish_no_words_subtitle),
                    completedToday = uiState.completedToday,
                    dailyGoal = uiState.dailyGoal,
                    dailyStreak = uiState.dailyStreak,
                    sessionDurationSeconds = uiState.sessionDurationSeconds,
                    sessionMaxCombo = uiState.sessionMaxCombo,
                    sessionNewCount = uiState.sessionNewCount,
                    sessionReviewCount = uiState.sessionReviewCount,
                    sessionRelearnCount = uiState.sessionRelearnCount,
                    tomorrowReviewForecastCount = uiState.tomorrowReviewForecastCount,
                    todayDueReviewCount = uiState.todayDueReviewCount,
                    showShareDialog = showShareDialog,
                    onDismissShareDialog = onDismissShareDialog,
                    onConfirmBonus = { bonusCount, bonusMode ->
                        onEvent(LearningEvent.StartBonusLearning(count = bonusCount, bonusMode = bonusMode, mode = LearningMode.Word))
                    },
                    onBackClick = onBackClick,
                    topContentPadding = finishTopPadding
                )
            }
        }
    }
}

@Composable
fun GrammarLearningContent(
    uiState: LearningUiState,
    onEvent: (LearningEvent) -> Unit,
    backgroundColor: Color, // 显式指定 Color 类型
    showShareDialog: Boolean = false,
    onDismissShareDialog: () -> Unit = {},
    onShowAnswerBlocked: (Int) -> Unit,
    onBackClick: () -> Unit = {},
    topContentPadding: androidx.compose.ui.unit.Dp = 0.dp
) {
    val autoRevealAnim = remember { androidx.compose.animation.core.Animatable(1f) }

    LaunchedEffect(
        uiState.currentGrammarIndex,
        uiState.isAnswerShown,
        uiState.status,
        uiState.isGrammarAutoRevealAnswerEnabled,
        uiState.grammarAutoRevealAnswerMs
    ) {
        if (uiState.isGrammarAutoRevealAnswerEnabled && !uiState.isAnswerShown && uiState.status == LearningStatus.Learning) {
            autoRevealAnim.snapTo(1f)
            autoRevealAnim.animateTo(
                targetValue = 0f,
                animationSpec = androidx.compose.animation.core.tween(
                    durationMillis = uiState.grammarAutoRevealAnswerMs.toInt(),
                    easing = androidx.compose.animation.core.LinearEasing
                )
            )
            onEvent(LearningEvent.ShowAnswer)
        } else {
            autoRevealAnim.snapTo(1f)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (uiState.currentGrammar != null) {
            val cardBackdrop = rememberLayerBackdrop()
            Box(modifier = Modifier.weight(1f)) {
                 // Card Content
                 Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .layerBackdrop(cardBackdrop),
                    contentAlignment = Alignment.TopCenter
                 ) {
                     // 单卡封闭模式：禁止左右手势翻页，仅由评分与撤销驱动卡片切换动效
                     AnimatedContent(
                        targetState = uiState.currentGrammar to uiState.sessionProcessedCount,
                        transitionSpec = {
                            val animSpec = tween<Float>(durationMillis = 220, easing = FastOutSlowInEasing)
                            val offsetSpec = tween<IntOffset>(durationMillis = 220, easing = FastOutSlowInEasing)
                            if (uiState.slideDirection == SlideDirection.FORWARD) {
                                (slideInHorizontally(animationSpec = offsetSpec) { width -> width } + fadeIn(animationSpec = animSpec)).togetherWith(
                                    slideOutHorizontally(animationSpec = offsetSpec) { width -> -width } + fadeOut(animationSpec = animSpec))
                            } else {
                                (slideInHorizontally(animationSpec = offsetSpec) { width -> -width } + fadeIn(animationSpec = animSpec)).togetherWith(
                                    slideOutHorizontally(animationSpec = offsetSpec) { width -> width } + fadeOut(animationSpec = animSpec))
                            }.using(
                                SizeTransform(clip = false)
                            )
                        },
                        label = "GrammarCardTransition"
                     ) { (targetGrammar, _) ->
                         if (targetGrammar != null) {
                             SRSGrammarCard(
                                 grammar = targetGrammar,
                                 progress = if (uiState.dailyGoal > 0) uiState.completedToday.toFloat() / uiState.dailyGoal else 0f,
                                 queueNewCount = uiState.queueNewCount,
                                 queueLearningCount = uiState.queueLearningCount,
                                 queueReviewCount = uiState.queueReviewCount,
                                 queueRelearnCount = uiState.queueRelearnCount,
                                 isAnswerShown = uiState.isAnswerShown,
                                 cardBadge = LearningItem.GrammarItem(targetGrammar).cardBadge,
                                 modifier = Modifier.fillMaxSize(),
                                 onSpeakExample = { japanese, chinese, id -> onEvent(LearningEvent.SpeakExample(japanese, chinese, id)) },
                                 playingAudioId = uiState.playingAudioId,
                                 isWhiteboardEnabled = uiState.isWhiteboardEnabled,
                                 autoRevealProgress = if (uiState.isGrammarAutoRevealAnswerEnabled && !uiState.isAnswerShown) autoRevealAnim.value else null,
                                 topPadding = topContentPadding
                             )
                         }
                     }
                 }


                  // SRS Action Area (Bottom)
                  SRSActionArea(
                      isAnswerShown = uiState.isAnswerShown,
                      isShowAnswerDelayEnabled = uiState.isShowAnswerDelayEnabled,
                      showAnswerAvailableAt = uiState.showAnswerAvailableAt,
                      ratingIntervals = uiState.ratingIntervals,
                      onShowAnswer = { onEvent(LearningEvent.ShowAnswer) },
                      onShowAnswerBlocked = onShowAnswerBlocked,
                      onRate = { quality -> onEvent(LearningEvent.RateGrammar(quality)) },
                      modifier = Modifier.align(Alignment.BottomCenter)
                  )
            }
        } else {
            val finishTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 60.dp + 16.dp
            if (uiState.shouldShowDailyGoalMet) {
                LearningFinishedContent(
                    title = stringResource(R.string.learn_screen_finish_daily_goal_title),
                    subtitle = stringResource(R.string.learn_screen_finish_daily_goal_subtitle),
                    completedToday = uiState.completedToday,
                    dailyGoal = uiState.dailyGoal,
                    dailyStreak = uiState.dailyStreak,
                    sessionDurationSeconds = uiState.sessionDurationSeconds,
                    sessionMaxCombo = uiState.sessionMaxCombo,
                    sessionNewCount = uiState.sessionNewCount,
                    sessionReviewCount = uiState.sessionReviewCount,
                    sessionRelearnCount = uiState.sessionRelearnCount,
                    tomorrowReviewForecastCount = uiState.tomorrowReviewForecastCount,
                    todayDueReviewCount = uiState.todayDueReviewCount,
                    showShareDialog = showShareDialog,
                    onDismissShareDialog = onDismissShareDialog,
                    onConfirmBonus = { bonusCount, bonusMode ->
                        onEvent(LearningEvent.StartBonusLearning(count = bonusCount, bonusMode = bonusMode, mode = LearningMode.Grammar))
                    },
                    onBackClick = onBackClick,
                    topContentPadding = finishTopPadding
                )
            } else if (uiState.sessionReviewCount + uiState.sessionNewCount + uiState.sessionRelearnCount > 0) {
                val remainingGrammars = (uiState.dailyGoal - uiState.completedToday).coerceAtLeast(0)
                LearningFinishedContent(
                    title = stringResource(R.string.learn_screen_finish_micro_session_title),
                    subtitle = stringResource(R.string.learn_screen_finish_micro_session_grammars_subtitle, uiState.dailyStreak, remainingGrammars),
                    completedToday = uiState.completedToday,
                    dailyGoal = uiState.dailyGoal,
                    dailyStreak = uiState.dailyStreak,
                    sessionDurationSeconds = uiState.sessionDurationSeconds,
                    sessionMaxCombo = uiState.sessionMaxCombo,
                    sessionNewCount = uiState.sessionNewCount,
                    sessionReviewCount = uiState.sessionReviewCount,
                    sessionRelearnCount = uiState.sessionRelearnCount,
                    tomorrowReviewForecastCount = uiState.tomorrowReviewForecastCount,
                    todayDueReviewCount = uiState.todayDueReviewCount,
                    showShareDialog = showShareDialog,
                    onDismissShareDialog = onDismissShareDialog,
                    onConfirmBonus = { bonusCount, bonusMode ->
                        onEvent(LearningEvent.StartBonusLearning(count = bonusCount, bonusMode = bonusMode, mode = LearningMode.Grammar))
                    },
                    onBackClick = onBackClick,
                    showBonusCard = false,
                    showNextGroupButton = true,
                    onNextGroupClick = {
                        onEvent(LearningEvent.StartLearning(mode = LearningMode.Grammar))
                    },
                    topContentPadding = finishTopPadding
                )
            } else {
                LearningFinishedContent(
                    title = stringResource(R.string.learn_screen_finish_no_grammars_title),
                    subtitle = stringResource(R.string.learn_screen_finish_no_grammars_subtitle),
                    completedToday = uiState.completedToday,
                    dailyGoal = uiState.dailyGoal,
                    dailyStreak = uiState.dailyStreak,
                    sessionDurationSeconds = uiState.sessionDurationSeconds,
                    sessionMaxCombo = uiState.sessionMaxCombo,
                    sessionNewCount = uiState.sessionNewCount,
                    sessionReviewCount = uiState.sessionReviewCount,
                    sessionRelearnCount = uiState.sessionRelearnCount,
                    tomorrowReviewForecastCount = uiState.tomorrowReviewForecastCount,
                    todayDueReviewCount = uiState.todayDueReviewCount,
                    showShareDialog = showShareDialog,
                    onDismissShareDialog = onDismissShareDialog,
                    onConfirmBonus = { bonusCount, bonusMode ->
                        onEvent(LearningEvent.StartBonusLearning(count = bonusCount, bonusMode = bonusMode, mode = LearningMode.Grammar))
                    },
                    onBackClick = onBackClick,
                    topContentPadding = finishTopPadding
                )
            }
        }
    }
}
