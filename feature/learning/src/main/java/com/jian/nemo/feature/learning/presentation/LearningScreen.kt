package com.jian.nemo.feature.learning.presentation

import com.jian.nemo.core.designsystem.theme.screenBackground

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
import androidx.compose.material.icons.filled.AccessTime
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
import androidx.compose.ui.res.painterResource
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
import com.jian.nemo.core.ui.component.common.NemoAppleDropdownMenu
import com.jian.nemo.feature.learning.presentation.components.common.LearningFinishedContent
import com.jian.nemo.feature.learning.presentation.components.common.LearnHeader
import com.jian.nemo.feature.learning.presentation.components.common.LearnHeaderMoreMenuButton
import com.jian.nemo.feature.learning.presentation.components.common.LearnHeaderMoreMenuItems
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
import com.jian.nemo.core.ui.component.dialog.DurationPickerDialog


@Composable

fun LearningScreen(
    viewModel: LearningViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    initialMode: LearningMode = LearningMode.Word
) {
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
    var menuAnchorBounds by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
    var showSharePosterDialog by remember { mutableStateOf(false) }
    val finishBackdrop = rememberLayerBackdrop()
    val waitingBackdrop = rememberLayerBackdrop()
    val screenBackdrop = rememberLayerBackdrop()
    var triggerWaitingBlast by remember { mutableStateOf<(() -> Unit)?>(null) }

    // 动态高斯模糊半径 (仅在展示分享海报弹窗时触发背景模糊遮罩)
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

    CompositionLocalProvider(LocalSettingIconStyle provides uiState.settingIconStyle) {
        Scaffold(
            containerColor = backgroundColor
        ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // 当结算完成时，在最外层（不受 Scaffold padding 限制）绘制贯穿系统状态栏的顶部主题色渐变光晕
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

            // 1. 页面主体内容 (Z = 0，高斯模糊景深)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(screenBackdrop)
                    .then(
                        if (animatedBlurRadius > 0.dp) Modifier.blur(radius = animatedBlurRadius)
                        else Modifier
                    )
                    .padding(padding)
                    .padding(horizontal = 16.dp)
            ) {
                if (isFinished) {
                    // 结算状态下：极简轻量化顶部导航（接入真实液态凸透镜折射，去除残留进度条与 0 0 0 0）
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 左侧：返回按钮（液态凸透镜物理折射）
                        FrostedLiquidGlassButton(
                            onClick = onNavigateBack,
                            backdrop = finishBackdrop,
                            shape = CircleShape,
                            hasLens = true,
                            contentPadding = PaddingValues(0.dp),
                            isInteractive = true,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                contentDescription = "返回",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        // 中间：已连续打卡胶囊徽章（HTML 同款设计，橙底暖色）
                        Surface(
                            shape = CircleShape,
                            color = if (isDarkTheme) Color(0xFF2E1F14) else Color(0xFFFFF7ED),
                            border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF6B3A1E) else Color(0xFFFFEDD5))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(text = "🔥", fontSize = 13.sp)
                                Text(
                                    text = "已连续打卡 ${uiState.dailyStreak} 天",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDarkTheme) Color(0xFFFF8A50) else Color(0xFFEA580C)
                                )
                            }
                        }

                        // 右侧：分享按钮（液态凸透镜物理折射，使用热力图同款 ic_share_ios）
                        FrostedLiquidGlassButton(
                            onClick = { showSharePosterDialog = true },
                            backdrop = finishBackdrop,
                            shape = CircleShape,
                            hasLens = true,
                            contentPadding = PaddingValues(0.dp),
                            isInteractive = true,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_share_ios),
                                contentDescription = "分享",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                } else {
                    val remainingTaskCount = if (uiState.learningMode == LearningMode.Word)
                        uiState.wordList.size
                    else
                        uiState.grammarList.size

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
                        backdrop = if (isWaiting) waitingBackdrop else null,
                        menu = {
                            LearnHeaderMoreMenuButton(
                                onClick = { isMenuExpanded = true },
                                onBoundsChanged = { menuAnchorBounds = it },
                                backdrop = if (isWaiting) waitingBackdrop else null
                            )
                        }
                    )
                }

                // Content（结算时作为 finishBackdrop 的光学采样源）
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .then(if (isFinished) Modifier.layerBackdrop(finishBackdrop) else Modifier)
                ) {
                    when (uiState.status) {
                        LearningStatus.Loading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                                showBackground = false
                            )
                        }
                        else -> {
                            LearningContent(
                                uiState = uiState,
                                onEvent = viewModel::onEvent,
                                backgroundColor = backgroundColor, // 传递背景色
                                showShareDialog = showSharePosterDialog,
                                onDismissShareDialog = { showSharePosterDialog = false },
                                onShowAnswerBlocked = { remainingSec ->
                                    showAnswerDelayHintSec = remainingSec
                                    showAnswerDelayHint = true
                                },
                                onBackClick = onNavigateBack
                            )
                        }
                    }
                }
            }

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
                                message = "等待中，请先回想 (${showAnswerDelayHintSec}s)",
                                type = NemoSnackbarType.WARNING,
                                icon = Icons.Default.AccessTime,
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
                    title = "自动翻面时长",
                    initialSeconds = currentSec,
                    onDismissRequest = { showAutoRevealDurationDialog = false },
                    onConfirm = { seconds ->
                        showAutoRevealDurationDialog = false
                        val ms = seconds * 1000L
                        if (uiState.learningMode == LearningMode.Word) {
                            viewModel.onEvent(LearningEvent.SetAutoRevealAnswerMs(ms))
                        } else {
                            viewModel.onEvent(LearningEvent.SetGrammarAutoRevealAnswerMs(ms))
                        }
                    }
                )
            }

            if (showDelayDurationDialog) {
                val currentSec = (uiState.showAnswerDelayMs / 1000).toInt()
                DurationPickerDialog(
                    title = "显示答案等待时长",
                    initialSeconds = currentSec,
                    onDismissRequest = { showDelayDurationDialog = false },
                    onConfirm = { seconds ->
                        showDelayDurationDialog = false
                        viewModel.onEvent(LearningEvent.SetShowAnswerDelayMs(seconds * 1000L))
                    }
                )
            }

            // 顶层同窗口苹果毛玻璃菜单（Cresto Demo 同款架构，处于同一 Window 树，彻底根除跨 Window 闪退）
            val activeRemainingCount = if (uiState.learningMode == LearningMode.Word)
                uiState.wordList.size
            else
                uiState.grammarList.size

            NemoAppleDropdownMenu(
                expanded = isMenuExpanded,
                anchorBounds = menuAnchorBounds,
                backdrop = if (isWaiting) waitingBackdrop else screenBackdrop,
                onDismiss = { isMenuExpanded = false }
            ) {
                LearnHeaderMoreMenuItems(
                    remainingCount = activeRemainingCount,
                    onSuspend = { viewModel.onEvent(LearningEvent.SuspendCurrent) },
                    onBury = { viewModel.onEvent(LearningEvent.BuryCurrent) },
                    onReportError = { viewModel.onEvent(LearningEvent.OpenReportErrorDialog) },
                    onShowRatingGuide = { showRatingGuide = true },
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
                    onCycleDarkMode = { viewModel.onEvent(LearningEvent.CycleDarkMode) }
                )
            }
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
    onBackClick: () -> Unit = {}
) {
    if (uiState.learningMode == LearningMode.Word) {
        WordLearningContent(
            uiState = uiState,
            onEvent = onEvent,
            backgroundColor = backgroundColor, // 新增
            showShareDialog = showShareDialog,
            onDismissShareDialog = onDismissShareDialog,
            onShowAnswerBlocked = onShowAnswerBlocked,
            onBackClick = onBackClick
        )
    } else {
        GrammarLearningContent(
            uiState = uiState,
            onEvent = onEvent,
            backgroundColor = backgroundColor, // 新增
            showShareDialog = showShareDialog,
            onDismissShareDialog = onDismissShareDialog,
            onShowAnswerBlocked = onShowAnswerBlocked,
            onBackClick = onBackClick
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
    onBackClick: () -> Unit = {}
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
        // SubHeader and Progress Bar are now moved to LearnHeader common component for pixel-perfect match with HTML top bar.

        Spacer(modifier = Modifier.height(4.dp)) // Add some spacing from header

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
                        targetState = uiState.currentWord,
                        transitionSpec = {
                            if (uiState.slideDirection == SlideDirection.FORWARD) {
                                (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                    slideOutHorizontally { width -> -width } + fadeOut())
                            } else {
                                (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                                    slideOutHorizontally { width -> width } + fadeOut())
                            }.using(
                                SizeTransform(clip = false)
                            )
                        },
                        label = "WordCardTransition"
                     ) { targetWord ->
                         SRSLearningCard(
                             word = targetWord,
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
                             autoRevealProgress = if (uiState.isAutoRevealAnswerEnabled && !uiState.isAnswerShown) autoRevealAnim.value else null
                         )
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
        } else if (uiState.shouldShowDailyGoalMet) {
            LearningFinishedContent(
                title = "今日任务达成！",
                subtitle = "坚持就是胜利，明天继续加油",
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
                onBackClick = onBackClick
            )
        } else if (uiState.sessionReviewCount + uiState.sessionNewCount + uiState.sessionRelearnCount > 0) {
            LearningFinishedContent(
                title = "本组微会话完成！",
                subtitle = "已连续打卡 ${uiState.dailyStreak} 天，可点击下方加餐继续出清积压",
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
                onBackClick = onBackClick
            )
        } else {
            LearningFinishedContent(
                title = "暂无单词任务",
                subtitle = "目前没有需要学习或复习的单词",
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
                onBackClick = onBackClick
            )
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
    onBackClick: () -> Unit = {}
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
        Spacer(modifier = Modifier.height(4.dp)) // Add some spacing from header

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
                        targetState = uiState.currentGrammar,
                        transitionSpec = {
                            if (uiState.slideDirection == SlideDirection.FORWARD) {
                                (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                    slideOutHorizontally { width -> -width } + fadeOut())
                            } else {
                                (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                                    slideOutHorizontally { width -> width } + fadeOut())
                            }.using(
                                SizeTransform(clip = false)
                            )
                        },
                        label = "GrammarCardTransition"
                     ) { targetGrammar ->
                         SRSGrammarCard(
                             grammar = targetGrammar,
                             isAnswerShown = uiState.isAnswerShown,
                             cardBadge = LearningItem.GrammarItem(targetGrammar).cardBadge,
                             modifier = Modifier.fillMaxSize(),
                             onSpeakExample = { japanese, chinese, id -> onEvent(LearningEvent.SpeakExample(japanese, chinese, id)) },
                             playingAudioId = uiState.playingAudioId,
                             isWhiteboardEnabled = uiState.isWhiteboardEnabled,
                             autoRevealProgress = if (uiState.isGrammarAutoRevealAnswerEnabled && !uiState.isAnswerShown) autoRevealAnim.value else null
                         )
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
        } else if (uiState.shouldShowDailyGoalMet) {
            LearningFinishedContent(
                title = "今日任务达成！",
                subtitle = "坚持就是胜利，明天继续加油",
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
                onBackClick = onBackClick
            )
        } else if (uiState.sessionReviewCount + uiState.sessionNewCount + uiState.sessionRelearnCount > 0) {
            LearningFinishedContent(
                title = "本组微会话完成！",
                subtitle = "已连续打卡 ${uiState.dailyStreak} 天，可点击下方加餐继续出清积压",
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
                onBackClick = onBackClick
            )
        } else {
            LearningFinishedContent(
                title = "暂无语法任务",
                subtitle = "目前没有需要学习或复习的语法",
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
                onBackClick = onBackClick
            )
        }
    }
}
