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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Report
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.ui.unit.dp
import com.jian.nemo.feature.learning.presentation.components.dialogs.ContentReportDialog
import androidx.compose.animation.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.hilt.navigation.compose.hiltViewModel
import com.jian.nemo.feature.learning.presentation.components.common.LearningFinishedContent
import com.jian.nemo.feature.learning.presentation.components.common.DailyGoalMetContent
import com.jian.nemo.feature.learning.presentation.components.common.LearnHeader
import com.jian.nemo.feature.learning.presentation.components.common.WaitingContent
import com.jian.nemo.core.designsystem.theme.NemoSurfaceBackground
import com.jian.nemo.core.designsystem.theme.NemoSurfaceBackgroundDark
import com.jian.nemo.core.ui.component.common.NemoSnackbar
import com.jian.nemo.core.ui.component.common.NemoSnackbarType
import com.jian.nemo.feature.learning.presentation.components.dialogs.TypingPracticeDialog
import com.jian.nemo.feature.learning.presentation.components.cards.SRSLearningCard
import com.jian.nemo.feature.learning.presentation.components.cards.SRSGrammarCard
import com.jian.nemo.feature.learning.presentation.components.guide.RatingGuideScreen
import com.jian.nemo.feature.learning.presentation.components.srs.SRSActionArea
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
    var showUndoHint by rememberSaveable { mutableStateOf(false) }
    var showAutoRevealDurationDialog by rememberSaveable { mutableStateOf(false) }
    var showDelayDurationDialog by rememberSaveable { mutableStateOf(false) }

    val delayDurationLabel = when (uiState.showAnswerDelayMs) {
        2000L -> "2s"
        3000L -> "3s"
        4000L -> "4s"
        5000L -> "5s"
        else -> "${uiState.showAnswerDelayMs / 1000}s"
    }

    LaunchedEffect(uiState.undoEventTrigger) {
        if (uiState.canUndo) {
            showUndoHint = true
        }
    }

    var isMenuExpanded by remember { mutableStateOf(false) }

    // 动态高斯模糊半径 (0dp -> 14dp 平滑过渡)
    val animatedBlurRadius by animateDpAsState(
        targetValue = if (isMenuExpanded) 14.dp else 0.dp,
        animationSpec = tween(durationMillis = 200),
        label = "learningBlur"
    )

    Scaffold(
        containerColor = backgroundColor
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. 页面主体内容 (Z = 0，高斯模糊景深)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (animatedBlurRadius > 0.dp) Modifier.blur(radius = animatedBlurRadius)
                        else Modifier
                    )
                    .padding(padding)
                    .padding(horizontal = 16.dp)
            ) {
                // Header
                LearnHeader(
                    learningMode = uiState.learningMode,
                    completedCount = uiState.completedToday,
                    dailyGoal = uiState.dailyGoal,
                    totalCount = if (uiState.learningMode == LearningMode.Word)
                        uiState.wordList.size
                    else
                        uiState.grammarList.size,
                    onClose = onNavigateBack,
                    onSuspend = { viewModel.onEvent(LearningEvent.SuspendCurrent) },
                    onBury = { viewModel.onEvent(LearningEvent.BuryCurrent) },
                    onShowRatingGuide = { showRatingGuide = true },
                    isMenuExpanded = isMenuExpanded,
                    onToggleMenu = { isMenuExpanded = it },
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
                    canUndo = uiState.canUndo,
                    onUndo = { viewModel.onEvent(LearningEvent.Undo) },
                    onReportError = { viewModel.onEvent(LearningEvent.OpenReportErrorDialog) },
                    isDarkMode = uiState.isDarkMode,
                    onCycleDarkMode = { viewModel.onEvent(LearningEvent.CycleDarkMode) },
                    queueNewCount = uiState.queueNewCount,
                    queueLearningCount = uiState.queueLearningCount,
                    queueReviewCount = uiState.queueReviewCount,
                    queueRelearnCount = uiState.queueRelearnCount
                )

                // Content
                Box(modifier = Modifier.weight(1f)) {
                    when (uiState.status) {
                        LearningStatus.Loading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                NemoChasingDotsLoader()
                            }
                        }
                        LearningStatus.Waiting -> {
                            WaitingContent(
                                until = uiState.waitingUntil,
                                onContinue = { viewModel.onEvent(LearningEvent.ResumeFromWaiting) }
                            )
                        }
                        else -> {
                            LearningContent(
                                uiState = uiState,
                                onEvent = viewModel::onEvent,
                                backgroundColor = backgroundColor, // 传递背景色
                                onShowAnswerBlocked = { remainingSec ->
                                    showAnswerDelayHintSec = remainingSec
                                    showAnswerDelayHint = true
                                }
                            )
                        }
                    }
                }
            }

            // 2. 页面全屏高斯模糊景深遮罩 (Z = 1，12% 柔光暗化 + 200ms 淡入淡出)
            androidx.compose.animation.AnimatedVisibility(
                visible = isMenuExpanded,
                enter = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(200)),
                exit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(200))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.12f))
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null,
                            onClick = { isMenuExpanded = false }
                        )
                )
            }

            // 动态构建通知列表（最新产生的通知插入在最上方）
            val notificationList = remember(
                uiState.canUndo, showUndoHint,
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
                    if (uiState.canUndo && showUndoHint) {
                        add(
                            com.jian.nemo.core.ui.component.common.NemoNotificationData(
                                id = "undo_hint",
                                message = "点击撤销上一次评分",
                                type = NemoSnackbarType.INFO,
                                icon = Icons.AutoMirrored.Filled.Undo,
                                actionText = "撤销",
                                autoDismissMs = 4000L,
                                onDismiss = { showUndoHint = false },
                                onClick = {
                                    showUndoHint = false
                                    viewModel.onEvent(LearningEvent.Undo)
                                }
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
        }
    }
}

@Composable
fun LearningContent(
    uiState: LearningUiState,
    onEvent: (LearningEvent) -> Unit,
    backgroundColor: Color, // 显式指定 Color 类型
    onShowAnswerBlocked: (Int) -> Unit
) {
    if (uiState.learningMode == LearningMode.Word) {
        WordLearningContent(
            uiState = uiState,
            onEvent = onEvent,
            backgroundColor = backgroundColor, // 新增
            onShowAnswerBlocked = onShowAnswerBlocked
        )
    } else {
        GrammarLearningContent(
            uiState = uiState,
            onEvent = onEvent,
            backgroundColor = backgroundColor, // 新增
            onShowAnswerBlocked = onShowAnswerBlocked
        )
    }
}

@Composable
fun WordLearningContent(
    uiState: LearningUiState,
    onEvent: (LearningEvent) -> Unit,
    backgroundColor: Color, // 显式指定 Color 类型
    onShowAnswerBlocked: (Int) -> Unit
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
            Box(modifier = Modifier.weight(1f)) {
                 // Card Content
                 Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                 ) {
                     // 使用 HorizontalPager 实现手势滑动切换
                     val pagerState = rememberPagerState(
                         initialPage = uiState.currentIndex,
                         pageCount = { uiState.wordList.size }
                     )

                     // 最佳实践：优化手势冲突
                     // 1. 排除系统手势边缘 (System Gesture Exclusion)
                     // 2. 自定义 ViewConfiguration 指令
                     val viewConfiguration = LocalViewConfiguration.current
                     val customViewConfiguration = remember {
                         object : ViewConfiguration by viewConfiguration {
                             override val touchSlop: Float
                                 get() = viewConfiguration.touchSlop * 0.8f // 提高灵敏度
                         }
                     }

                     // 标志位：是否正在由 ViewModel 驱动的滚动
                     var isViewModelDriven by remember { mutableStateOf(false) }

                     // 同步 ViewModel 状态到 Pager（ViewModel 驱动）
                     LaunchedEffect(uiState.currentIndex) {
                         if (pagerState.currentPage != uiState.currentIndex) {
                             isViewModelDriven = true
                             pagerState.animateScrollToPage(uiState.currentIndex)
                             isViewModelDriven = false
                         }
                     }

                     // 同步 Pager 状态到 ViewModel（仅手势驱动时）
                     LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
                         if (!isViewModelDriven && !pagerState.isScrollInProgress && pagerState.currentPage != uiState.currentIndex) {
                             onEvent(LearningEvent.GoToIndex(pagerState.currentPage))
                         }
                     }

                     androidx.compose.runtime.CompositionLocalProvider(
                         LocalViewConfiguration provides customViewConfiguration
                     ) {
                         HorizontalPager(
                             state = pagerState,
                             modifier = Modifier
                                 .fillMaxSize()
                                 .systemGestureExclusion(), // 排除系统返回手势
                             beyondViewportPageCount = 1,
                             userScrollEnabled = !uiState.isAnswerShown
                         ) { page ->
                             // 使用 AnimatedContent 实现评分后的过渡动画
                             // 当 page 索引不变（例如一直是 0），但内容（wordList）发生改变时触发
                             val word = uiState.wordList.getOrNull(page)

                             AnimatedContent(
                                targetState = word,
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
                                 if (targetWord != null) {
                                     SRSLearningCard(
                                          word = targetWord,
                                          isAnswerShown = uiState.isAnswerShown && page == uiState.currentIndex,
                                          cardBadge = LearningItem.WordItem(targetWord).cardBadge,
                                          modifier = Modifier.fillMaxSize(),
                                          onPracticeClick = {
                                              showTypingDialog = true
                                          },
                                          onSpeakWord = { onEvent(LearningEvent.SpeakWord(targetWord.hiragana, targetWord.chinese)) },
                                          onSpeakExample = { japanese, chinese, id -> onEvent(LearningEvent.SpeakExample(japanese, chinese, id)) },
                                          playingAudioId = uiState.playingAudioId,
                                          isWhiteboardEnabled = uiState.isWhiteboardEnabled,
                                          autoRevealProgress = if (uiState.isAutoRevealAnswerEnabled && !uiState.isAnswerShown && page == uiState.currentIndex) autoRevealAnim.value else null
                                      )
                                 }
                             }
                         }
                     }
                 }

                 // 底部边缘自然消失渐变蒙层
                 Box(
                     modifier = Modifier
                         .align(Alignment.BottomCenter)
                         .fillMaxWidth()
                         .height(160.dp) // 渐变高度 160.dp
                         .background(
                             brush = Brush.verticalGradient(
                                 colors = listOf(
                                     Color.Transparent,
                                     backgroundColor.copy(alpha = 0.8f),
                                     backgroundColor
                                 )
                             )
                         )
                 )

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
                sessionDurationSeconds = uiState.sessionDurationSeconds,
                sessionMaxCombo = uiState.sessionMaxCombo,
                sessionNewCount = uiState.sessionNewCount,
                sessionReviewCount = uiState.sessionReviewCount,
                sessionRelearnCount = uiState.sessionRelearnCount,
                tomorrowReviewForecastCount = uiState.tomorrowReviewForecastCount,
                onConfirmBonus = { bonusCount ->
                    onEvent(LearningEvent.StartBonusLearning(bonusCount, LearningMode.Word))
                }
            )
        } else {
            LearningFinishedContent(
                title = "暂无单词任务",
                subtitle = "目前没有需要学习或复习的单词",
                completedToday = uiState.completedToday,
                dailyGoal = uiState.dailyGoal,
                sessionDurationSeconds = uiState.sessionDurationSeconds,
                sessionMaxCombo = uiState.sessionMaxCombo,
                sessionNewCount = uiState.sessionNewCount,
                sessionReviewCount = uiState.sessionReviewCount,
                sessionRelearnCount = uiState.sessionRelearnCount,
                tomorrowReviewForecastCount = uiState.tomorrowReviewForecastCount,
                onConfirmBonus = { bonusCount ->
                    onEvent(LearningEvent.StartBonusLearning(bonusCount, LearningMode.Word))
                }
            )
        }
    }
}

@Composable
fun GrammarLearningContent(
    uiState: LearningUiState,
    onEvent: (LearningEvent) -> Unit,
    backgroundColor: Color, // 显式指定 Color 类型
    onShowAnswerBlocked: (Int) -> Unit
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
            Box(modifier = Modifier.weight(1f)) {
                 // Card Content
                 Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                 ) {
                     // 使用 HorizontalPager 实现手势滑动切换
                     val pagerState = rememberPagerState(
                         initialPage = uiState.currentGrammarIndex,
                         pageCount = { uiState.grammarList.size }
                     )

                     // 最佳实践：优化手势冲突
                     val viewConfiguration = LocalViewConfiguration.current
                     val customViewConfiguration = remember {
                         object : ViewConfiguration by viewConfiguration {
                             override val touchSlop: Float
                                 get() = viewConfiguration.touchSlop * 0.8f // 提高灵敏度
                         }
                     }

                     // 标志位：是否正在由 ViewModel 驱动的滚动
                     var isViewModelDriven by remember { mutableStateOf(false) }

                     // 同步 ViewModel 状态到 Pager（ViewModel 驱动）
                     LaunchedEffect(uiState.currentGrammarIndex) {
                         if (pagerState.currentPage != uiState.currentGrammarIndex) {
                             isViewModelDriven = true
                             pagerState.animateScrollToPage(uiState.currentGrammarIndex)
                             isViewModelDriven = false
                         }
                     }

                     // 同步 Pager 状态到 ViewModel（仅手势驱动时）
                     LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
                         if (!isViewModelDriven && !pagerState.isScrollInProgress && pagerState.currentPage != uiState.currentGrammarIndex) {
                             onEvent(LearningEvent.GoToIndex(pagerState.currentPage))
                         }
                     }

                     androidx.compose.runtime.CompositionLocalProvider(
                         LocalViewConfiguration provides customViewConfiguration
                     ) {
                         HorizontalPager(
                             state = pagerState,
                             modifier = Modifier
                                 .fillMaxSize()
                                 .systemGestureExclusion(),
                             beyondViewportPageCount = 1,
                             userScrollEnabled = !uiState.isAnswerShown
                         ) { page ->
                         val grammar = uiState.grammarList.getOrNull(page)

                         AnimatedContent(
                            targetState = grammar,
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
                             if (targetGrammar != null) {
                                  SRSGrammarCard(
                                      grammar = targetGrammar,
                                      isAnswerShown = uiState.isAnswerShown && page == uiState.currentGrammarIndex,
                                      cardBadge = LearningItem.GrammarItem(targetGrammar).cardBadge,
                                      modifier = Modifier.fillMaxSize(),
                                      onSpeakExample = { japanese, chinese, id -> onEvent(LearningEvent.SpeakExample(japanese, chinese, id)) },
                                      playingAudioId = uiState.playingAudioId,
                                      isWhiteboardEnabled = uiState.isWhiteboardEnabled,
                                      autoRevealProgress = if (uiState.isGrammarAutoRevealAnswerEnabled && !uiState.isAnswerShown && page == uiState.currentGrammarIndex) autoRevealAnim.value else null
                                  )
                             }
                         }
                     }
                 }
            }
                  // 底部边缘自然消失渐变蒙层
                  Box(
                      modifier = Modifier
                          .align(Alignment.BottomCenter)
                          .fillMaxWidth()
                          .height(160.dp) // 渐变高度 160.dp
                          .background(
                              brush = Brush.verticalGradient(
                                  colors = listOf(
                                      Color.Transparent,
                                      backgroundColor.copy(alpha = 0.8f),
                                      backgroundColor
                                  )
                              )
                          )
                  )

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
                sessionDurationSeconds = uiState.sessionDurationSeconds,
                sessionMaxCombo = uiState.sessionMaxCombo,
                sessionNewCount = uiState.sessionNewCount,
                sessionReviewCount = uiState.sessionReviewCount,
                sessionRelearnCount = uiState.sessionRelearnCount,
                tomorrowReviewForecastCount = uiState.tomorrowReviewForecastCount,
                onConfirmBonus = { bonusCount ->
                    onEvent(LearningEvent.StartBonusLearning(bonusCount, LearningMode.Grammar))
                }
            )
        } else {
            LearningFinishedContent(
                title = "暂无语法任务",
                subtitle = "目前没有需要学习或复习的语法",
                completedToday = uiState.completedToday,
                dailyGoal = uiState.dailyGoal,
                sessionDurationSeconds = uiState.sessionDurationSeconds,
                sessionMaxCombo = uiState.sessionMaxCombo,
                sessionNewCount = uiState.sessionNewCount,
                sessionReviewCount = uiState.sessionReviewCount,
                sessionRelearnCount = uiState.sessionRelearnCount,
                tomorrowReviewForecastCount = uiState.tomorrowReviewForecastCount,
                onConfirmBonus = { bonusCount ->
                    onEvent(LearningEvent.StartBonusLearning(bonusCount, LearningMode.Grammar))
                }
            )
        }
    }
}
