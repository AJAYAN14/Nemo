package com.jian.nemo.feature.test.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import com.jian.nemo.core.designsystem.shape.RoundedRectangle
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import com.jian.nemo.core.designsystem.theme.screenBackground
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import com.jian.nemo.feature.test.R
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jian.nemo.core.ui.animation.containerTransform
import com.jian.nemo.core.ui.component.animation.NemoChasingDotsLoader
import com.jian.nemo.core.ui.component.common.CommonHeader
import com.jian.nemo.core.ui.component.liquid.ColoredLiquidGlassButton
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.jian.nemo.feature.test.presentation.settings.components.CustomQuestionCountDialog
import com.jian.nemo.feature.test.presentation.settings.components.CustomTimeLimitDialog
import com.jian.nemo.feature.test.domain.model.TestConfig
import com.jian.nemo.feature.test.domain.model.QuestionSource
import com.jian.nemo.feature.test.domain.model.TestContentType
import kotlinx.coroutines.launch

import com.jian.nemo.feature.test.presentation.settings.components.BasicSettingsSection
import com.jian.nemo.feature.test.presentation.settings.components.QuizSettingsSection
import com.jian.nemo.feature.test.presentation.settings.components.NemoActionPopupMenuItem
import com.jian.nemo.feature.test.presentation.settings.components.NemoPopupMenu
import com.jian.nemo.feature.test.presentation.settings.components.NemoPopupMenuDivider
import com.jian.nemo.feature.test.presentation.settings.components.NemoPopupMenuState
import com.jian.nemo.feature.test.presentation.settings.components.NemoPopupMenuItem
import com.jian.nemo.feature.test.presentation.settings.components.NemoSelectivePopupMenuItem


/**
 * 测试设置界面 - UI/UX Pro Max 扁平化分组风格
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestSettingsScreen(
    testModeId: String? = null,
    onBack: () -> Unit,
    onNavigate: (com.jian.nemo.feature.test.presentation.settings.model.TestNavigationEvent) -> Unit,
    viewModel: TestSettingsViewModel = hiltViewModel(),
    starterViewModel: TestStarterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isGenerating by starterViewModel.isGenerating.collectAsState()
    val config = uiState.testConfig
    val isRestrictedMode = testModeId in listOf("typing", "card_matching", "sorting")

    // Helper function to update config
    fun updateConfig(block: (TestConfig) -> TestConfig) {
        viewModel.updateConfig(block(config))
    }

    val titleMultipleChoice = stringResource(R.string.test_settings_title_multiple_choice)
    val titleTyping = stringResource(R.string.test_settings_title_typing)
    val titleCardMatching = stringResource(R.string.test_settings_title_card_matching)
    val titleSorting = stringResource(R.string.test_settings_title_sorting)
    val titleDefault = stringResource(R.string.test_settings_title_default)

    val labelWrongSource = stringResource(R.string.test_source_wrong_title)
    val labelFavoriteSource = stringResource(R.string.test_source_favorite_title)
    val labelTodaySource = stringResource(R.string.test_source_today_title)
    val labelTodayReviewedSource = stringResource(R.string.test_source_today_reviewed_title)
    val labelLearnedSource = stringResource(R.string.test_source_learned_title)
    val labelAllSource = stringResource(R.string.test_source_all_title)
    val formatWordsGrammars = stringResource(R.string.test_source_words_grammars_format)

    val labelRemovalNoneTitle = stringResource(R.string.test_removal_none_title)
    val labelRemovalNoneSubtitle = stringResource(R.string.test_removal_none_subtitle)
    val formatRemovalCountTitle = stringResource(R.string.test_removal_count_format_title)
    val formatRemovalCountSubtitle = stringResource(R.string.test_removal_count_format_subtitle)
    val labelRemovalCount10Subtitle = stringResource(R.string.test_removal_count_10_subtitle)
    val formatRemovalTimes = stringResource(R.string.test_removal_times_format)

    val labelWordsContentType = stringResource(R.string.test_content_type_words_title)
    val labelWordsContentTypeSubtitle = stringResource(R.string.test_content_type_words_subtitle)
    val labelGrammarContentType = stringResource(R.string.test_content_type_grammar_title)
    val labelGrammarContentTypeSubtitle = stringResource(R.string.test_content_type_grammar_subtitle)
    val labelMixedContentType = stringResource(R.string.test_content_type_mixed_title)
    val labelMixedContentTypeSubtitle = stringResource(R.string.test_content_type_mixed_subtitle)
    val labelUnknownContentType = stringResource(R.string.test_content_type_unknown)

    val customText = stringResource(R.string.test_sheet_btn_custom)
    val allLevelsText = stringResource(R.string.test_settings_levels_all)
    val formatQuestionCount = stringResource(R.string.test_settings_format_question_count)
    val timeLimitUnlimitedText = stringResource(R.string.test_settings_time_limit_unlimited)
    val formatTimeLimitMinutes = stringResource(R.string.test_settings_time_limit_minutes_format)

    // 🎯 动态页面标题
    val pageTitle = remember(testModeId) {
        when (testModeId) {
            "multiple_choice" -> titleMultipleChoice
            "typing" -> titleTyping
            "card_matching" -> titleCardMatching
            "sorting" -> titleSorting
            else -> titleDefault
        }
    }

    // 液态气泡悬浮菜单状态控制
    var popupMenuState by remember { mutableStateOf(NemoPopupMenuState()) }
    var activeSettingType by remember { mutableStateOf("") }

    // 自定义输入对话框
    var showCustomQuestionCountDialog by remember { mutableStateOf(false) }
    var showCustomTimeLimitDialog by remember { mutableStateOf(false) }

    // 防抖
    var lastClickTime by remember { mutableLongStateOf(0L) }

    // UI Host
    val backdrop = rememberLayerBackdrop()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val backgroundColor = MaterialTheme.colorScheme.screenBackground
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // 监听错误信息
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    // 监听消息列表
    uiState.messages.firstOrNull()?.let { msg ->
        LaunchedEffect(msg.id) {
            val duration = if (msg.priority == MessagePriority.High) SnackbarDuration.Long else SnackbarDuration.Short
            val result = snackbarHostState.showSnackbar(
                message = msg.message,
                actionLabel = msg.actionLabel,
                duration = duration,
                withDismissAction = true
            )
            if (result == SnackbarResult.ActionPerformed) {
                msg.onAction?.invoke()
            }
            viewModel.dismissMessage(msg.id)
        }
    }

    LaunchedEffect(testModeId) {
        viewModel.setTestModeId(testModeId)
    }

    // 选项数据准备
    val questionCountOptions = remember { listOf(10, 15, 20, 25, 30, 40) }
    val timeLimitOptions = remember { listOf(0, 5, 10, 15, 30) }

    val questionSourceOptions = remember(
        uiState.wrongWordsCount, uiState.wrongGrammarsCount,
        uiState.favoriteWordsCount, uiState.favoriteGrammarsCount,
        uiState.todayLearnedCount, uiState.todayLearnedGrammarCount,
        uiState.todayReviewedCount, uiState.todayReviewedGrammarCount,
        uiState.allLearnedWordsCount, uiState.allLearnedGrammarsCount,
        uiState.totalWordsCount, uiState.totalGrammarsCount
    ) {
        listOf(
            com.jian.nemo.feature.test.presentation.settings.components.SingleSelectOptionItem(
                key = QuestionSource.WRONG.key,
                title = labelWrongSource,
                subtitle = String.format(formatWordsGrammars, uiState.wrongWordsCount, uiState.wrongGrammarsCount),
                isEnabled = (uiState.wrongWordsCount + uiState.wrongGrammarsCount) > 0
            ),
            com.jian.nemo.feature.test.presentation.settings.components.SingleSelectOptionItem(
                key = QuestionSource.FAVORITE.key,
                title = labelFavoriteSource,
                subtitle = String.format(formatWordsGrammars, uiState.favoriteWordsCount, uiState.favoriteGrammarsCount),
                isEnabled = (uiState.favoriteWordsCount + uiState.favoriteGrammarsCount) > 0
            ),
            com.jian.nemo.feature.test.presentation.settings.components.SingleSelectOptionItem(
                key = QuestionSource.TODAY.key,
                title = labelTodaySource,
                subtitle = String.format(formatWordsGrammars, uiState.todayLearnedCount, uiState.todayLearnedGrammarCount),
                isEnabled = (uiState.todayLearnedCount + uiState.todayLearnedGrammarCount) > 0
            ),
            com.jian.nemo.feature.test.presentation.settings.components.SingleSelectOptionItem(
                key = QuestionSource.TODAY_REVIEWED.key,
                title = labelTodayReviewedSource,
                subtitle = String.format(formatWordsGrammars, uiState.todayReviewedCount, uiState.todayReviewedGrammarCount),
                isEnabled = (uiState.todayReviewedCount + uiState.todayReviewedGrammarCount) > 0
            ),
            com.jian.nemo.feature.test.presentation.settings.components.SingleSelectOptionItem(
                key = QuestionSource.LEARNED.key,
                title = labelLearnedSource,
                subtitle = String.format(formatWordsGrammars, uiState.allLearnedWordsCount, uiState.allLearnedGrammarsCount),
                isEnabled = (uiState.allLearnedWordsCount + uiState.allLearnedGrammarsCount) > 0
            ),
            com.jian.nemo.feature.test.presentation.settings.components.SingleSelectOptionItem(
                key = QuestionSource.ALL.key,
                title = labelAllSource,
                subtitle = String.format(formatWordsGrammars, uiState.totalWordsCount, uiState.totalGrammarsCount),
                isEnabled = (uiState.totalWordsCount + uiState.totalGrammarsCount) > 0
            )
        )
    }

    val wrongAnswerRemovalLabels = remember {
        mapOf(
            0 to labelRemovalNoneTitle,
            3 to String.format(formatRemovalCountTitle, 3),
            5 to String.format(formatRemovalCountTitle, 5),
            7 to String.format(formatRemovalCountTitle, 7),
            10 to String.format(formatRemovalCountTitle, 10)
        )
    }

    val contentTypeOptions = remember(isRestrictedMode, testModeId) {
        if (isRestrictedMode) {
            listOf(
                com.jian.nemo.feature.test.presentation.settings.components.SingleSelectOptionItem(
                    key = TestContentType.WORDS.key,
                    title = labelWordsContentType,
                    subtitle = labelWordsContentTypeSubtitle
                )
            )
        } else {
            listOf(
                com.jian.nemo.feature.test.presentation.settings.components.SingleSelectOptionItem(
                    key = TestContentType.WORDS.key,
                    title = labelWordsContentType,
                    subtitle = labelWordsContentTypeSubtitle
                ),
                com.jian.nemo.feature.test.presentation.settings.components.SingleSelectOptionItem(
                    key = TestContentType.GRAMMAR.key,
                    title = labelGrammarContentType,
                    subtitle = labelGrammarContentTypeSubtitle
                ),
                com.jian.nemo.feature.test.presentation.settings.components.SingleSelectOptionItem(
                    key = TestContentType.MIXED.key,
                    title = labelMixedContentType,
                    subtitle = labelMixedContentTypeSubtitle
                )
            )
        }
    }
    val currentContentTypeLabel = remember(contentTypeOptions, config.testContentType) {
        contentTypeOptions.find { it.key == config.testContentType.key }?.title ?: labelUnknownContentType
    }
    val allLevels = remember { listOf("N5", "N4", "N3", "N2", "N1") }

    // 动态构建当前激活的气泡菜单项
    val currentMenuItems = remember(activeSettingType, config, uiState, isRestrictedMode) {
        when (activeSettingType) {
            "questionCount" -> {
                val list = mutableListOf<NemoPopupMenuItem>()
                questionCountOptions.forEach { count ->
                    list.add(
                        NemoSelectivePopupMenuItem(
                            text = String.format(formatQuestionCount, count),
                            isSelected = { config.questionCount == count },
                            dismissOnClick = true,
                            onClick = { updateConfig { it.copy(questionCount = count) } }
                        )
                    )
                }
                list.add(NemoPopupMenuDivider)
                list.add(
                    NemoActionPopupMenuItem(
                        text = customText,
                        onClick = { showCustomQuestionCountDialog = true }
                    )
                )
                list
            }

            "timeLimit" -> {
                val list = mutableListOf<NemoPopupMenuItem>()
                timeLimitOptions.forEach { minutes ->
                    val label = if (minutes == 0) {
                        timeLimitUnlimitedText
                    } else {
                        String.format(formatTimeLimitMinutes, minutes)
                    }
                    list.add(
                        NemoSelectivePopupMenuItem(
                            text = label,
                            isSelected = { config.timeLimitMinutes == minutes },
                            dismissOnClick = true,
                            onClick = { updateConfig { it.copy(timeLimitMinutes = minutes) } }
                        )
                    )
                }
                list.add(NemoPopupMenuDivider)
                list.add(
                    NemoActionPopupMenuItem(
                        text = customText,
                        onClick = { showCustomTimeLimitDialog = true }
                    )
                )
                list
            }

            "questionSource" -> {
                listOf(
                    QuestionSource.TODAY to labelTodaySource,
                    QuestionSource.WRONG to labelWrongSource,
                    QuestionSource.FAVORITE to labelFavoriteSource,
                    QuestionSource.TODAY_REVIEWED to labelTodayReviewedSource,
                    QuestionSource.LEARNED to labelLearnedSource,
                    QuestionSource.ALL to labelAllSource
                ).map { (source, label) ->
                    NemoSelectivePopupMenuItem(
                        text = label,
                        isSelected = { config.questionSource == source },
                        dismissOnClick = true,
                        onClick = { updateConfig { it.copy(questionSource = source) } }
                    )
                }
            }

            "wrongAnswerRemoval" -> {
                listOf(0, 3, 5, 7, 10).map { count ->
                    val label = wrongAnswerRemovalLabels[count] ?: ""
                    NemoSelectivePopupMenuItem(
                        text = label,
                        isSelected = { config.wrongAnswerRemovalThreshold == count },
                        dismissOnClick = true,
                        onClick = { updateConfig { it.copy(wrongAnswerRemovalThreshold = count) } }
                    )
                }
            }

            "contentType" -> {
                val types = if (isRestrictedMode) {
                    listOf(TestContentType.WORDS)
                } else {
                    listOf(TestContentType.WORDS, TestContentType.GRAMMAR, TestContentType.MIXED)
                }
                types.map { type ->
                    val label = when (type) {
                        TestContentType.WORDS -> labelWordsContentType
                        TestContentType.GRAMMAR -> labelGrammarContentType
                        TestContentType.MIXED -> labelMixedContentType
                    }
                    NemoSelectivePopupMenuItem(
                        text = label,
                        isSelected = { config.testContentType == type },
                        dismissOnClick = true,
                        onClick = { updateConfig { it.copy(testContentType = type) } }
                    )
                }
            }

            "wordLevel" -> {
                val list = mutableListOf<NemoPopupMenuItem>()
                list.add(
                    NemoSelectivePopupMenuItem(
                        text = allLevelsText,
                        isSelected = { uiState.isAllWordLevelsSelected },
                        dismissOnClick = false,
                        onClick = { viewModel.toggleAllLevels(false) }
                    )
                )
                list.add(NemoPopupMenuDivider)
                allLevels.forEach { level ->
                    list.add(
                        NemoSelectivePopupMenuItem(
                            text = level,
                            isSelected = { level in config.selectedWordLevels },
                            dismissOnClick = false,
                            onClick = { viewModel.toggleLevel(level, false) }
                        )
                    )
                }
                list
            }

            "grammarLevel" -> {
                val list = mutableListOf<NemoPopupMenuItem>()
                list.add(
                    NemoSelectivePopupMenuItem(
                        text = allLevelsText,
                        isSelected = { uiState.isAllGrammarLevelsSelected },
                        dismissOnClick = false,
                        onClick = { viewModel.toggleAllLevels(true) }
                    )
                )
                list.add(NemoPopupMenuDivider)
                allLevels.forEach { level ->
                    list.add(
                        NemoSelectivePopupMenuItem(
                            text = level,
                            isSelected = { level in config.selectedGrammarLevels },
                            dismissOnClick = false,
                            onClick = { viewModel.toggleLevel(level, true) }
                        )
                    )
                }
                list
            }

            else -> emptyList()
        }
    }

    CustomQuestionCountDialog(
        showCustomQuestionCountDialog,
        config.questionCount,
        { showCustomQuestionCountDialog = false }
    ) { count ->
        updateConfig { it.copy(questionCount = count) }
    }

    CustomTimeLimitDialog(
        showCustomTimeLimitDialog,
        config.timeLimitMinutes,
        { showCustomTimeLimitDialog = false }
    ) {
        updateConfig { cfg -> cfg.copy(timeLimitMinutes = it) }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .containerTransform(
                key = "container_test_${testModeId ?: "default"}",
                shape = RoundedRectangle(0.dp)
            ),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = backgroundColor
    ) { _ ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
        ) {
            // 1. 底层滚动内容（被 backdrop 采样源录制）
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                // 顶部弹性占位：避开悬浮顶栏与状态栏，向上滚动时平滑穿入顶栏遮罩与液态玻璃下方
                Spacer(modifier = Modifier.height(statusBarTop + 56.dp + 16.dp))

                BasicSettingsSection(
                    config = config,
                    testModeId = testModeId,
                    uiState = uiState,
                    questionSourceOptions = questionSourceOptions,
                    wrongAnswerRemovalLabels = wrongAnswerRemovalLabels,
                    currentContentTypeLabel = currentContentTypeLabel,
                    onSettingClick = { setting, bounds ->
                        activeSettingType = setting
                        popupMenuState = NemoPopupMenuState(
                            isVisible = true,
                            anchorBounds = bounds,
                            items = emptyList() // items 由 remember(activeSettingType, ...) 绑定
                        )
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                QuizSettingsSection(
                    config = config,
                    testModeId = testModeId,
                    onUpdateConfig = { viewModel.updateConfig(it) }
                )

                // 底部弹性占位：预留悬浮按钮与导航栏高度
                Spacer(modifier = Modifier.height(navBarBottom + 96.dp))
            }

            // 2. 悬浮顶栏与液态玻璃按钮（带柔和多阶垂直渐变保护遮罩，读取 backdrop 物理透镜折射，对齐热力图）
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
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
                    title = pageTitle,
                    onBack = { if (isGenerating) starterViewModel.cancelGeneration() else onBack() },
                    backgroundColor = Color.Transparent,
                    backdrop = backdrop,
                    backButtonSize = 48.dp
                )
            }

            // 3. 悬浮开始测试按钮 (带底部渐变保护遮罩与 navigationBarsPadding)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                backgroundColor.copy(alpha = 0.6f),
                                backgroundColor.copy(alpha = 0.95f)
                            )
                        )
                    )
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                 LaunchedEffect(Unit) {
                    starterViewModel.navigationEvent.collect { event -> onNavigate(event) }
                }

                LaunchedEffect(Unit) {
                    starterViewModel.errorEvent.collect { error ->
                        snackbarHostState.showSnackbar(error)
                    }
                }

                 ColoredLiquidGlassButton(
                     onClick = {
                         val currentTime = System.currentTimeMillis()
                         if (currentTime - lastClickTime > 500) {
                             lastClickTime = currentTime
                             scope.launch {
                                 when (testModeId) {
                                     "typing" -> starterViewModel.startTypingTest(config)
                                     "card_matching" -> starterViewModel.startMatchingTest(config)
                                     "sorting" -> starterViewModel.startSortingTest(config)
                                     "multiple_choice" -> starterViewModel.startMultipleChoiceTest(config)
                                     else -> starterViewModel.startTest(config, com.jian.nemo.core.domain.model.TestMode.RANDOM)
                                 }
                             }
                         }
                     },
                     backdrop = backdrop,
                     tint = MaterialTheme.colorScheme.primary,
                     isInteractive = !isGenerating,
                     shape = RoundedRectangle(28.dp),
                     modifier = Modifier
                         .fillMaxWidth()
                         .height(56.dp)
                 ) {
                     Row(
                         horizontalArrangement = Arrangement.Center,
                         verticalAlignment = Alignment.CenterVertically
                     ) {
                         if (isGenerating) {
                             NemoChasingDotsLoader(size = 24.dp)
                             Spacer(Modifier.width(8.dp))
                             Text(stringResource(R.string.test_common_btn_preparing), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                         } else {
                             Text(stringResource(R.string.test_common_btn_start_test), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                         }
                     }
                 }
            }

            // 4. 液态气泡悬浮菜单 (就地停靠、物理折射、微触觉震动)
            NemoPopupMenu(
                state = popupMenuState.copy(items = currentMenuItems),
                backdrop = backdrop,
                onDismiss = {
                    viewModel.ensureValidLevels()
                    popupMenuState = popupMenuState.copy(isVisible = false)
                    activeSettingType = ""
                }
            )
        }
    }
}
