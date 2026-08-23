package com.jian.nemo.feature.test.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import com.jian.nemo.core.designsystem.theme.screenBackground
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jian.nemo.core.ui.animation.containerTransform
import com.jian.nemo.core.ui.component.animation.NemoChasingDotsLoader
import com.jian.nemo.core.ui.component.common.CommonHeader
import com.jian.nemo.core.ui.component.common.NemoScaffold
import com.jian.nemo.core.ui.component.liquid.LiquidButton
import com.jian.nemo.feature.test.presentation.settings.components.CustomQuestionCountDialog
import com.jian.nemo.feature.test.presentation.settings.components.CustomTimeLimitDialog
import com.jian.nemo.feature.test.domain.model.TestConfig
import com.jian.nemo.feature.test.domain.model.QuestionSource
import com.jian.nemo.feature.test.domain.model.TestContentType
import kotlinx.coroutines.launch

import com.jian.nemo.feature.test.presentation.settings.components.BasicSettingsSection
import com.jian.nemo.feature.test.presentation.settings.components.QuizSettingsSection
import com.jian.nemo.core.ui.component.sheet.NemoModalBottomSheet
import com.jian.nemo.feature.test.presentation.settings.components.TestSettingsBottomSheetContent


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

    // 🎯 动态页面标题
    val pageTitle = remember(testModeId) {
        when (testModeId) {
            "multiple_choice" -> "选择题设置"
            "typing" -> "手打题设置"
            "card_matching" -> "卡片题设置"
            "sorting" -> "排序题设置"
            else -> "测试设置"
        }
    }

    // BottomSheet 控制
    var showBottomSheet by remember { mutableStateOf(false) }
    var currentSetting by remember { mutableStateOf("") }

    // 自定义输入对话框
    var showCustomQuestionCountDialog by remember { mutableStateOf(false) }
    var showCustomTimeLimitDialog by remember { mutableStateOf(false) }

    // 防抖
    var lastClickTime by remember { mutableLongStateOf(0L) }

    // UI Host
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val backgroundColor = MaterialTheme.colorScheme.screenBackground

    // 监听错误信息 (Keep for compatibility if error is still used, but prefer messages)
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

    // Loop removed - logic moved to ViewModel

    // 选项数据准备 - 使用 remember 优化
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
                title = "我的错题",
                subtitle = "${uiState.wrongWordsCount} 词 · ${uiState.wrongGrammarsCount} 语法",
                isEnabled = (uiState.wrongWordsCount + uiState.wrongGrammarsCount) > 0
            ),
            com.jian.nemo.feature.test.presentation.settings.components.SingleSelectOptionItem(
                key = QuestionSource.FAVORITE.key,
                title = "我的收藏",
                subtitle = "${uiState.favoriteWordsCount} 词 · ${uiState.favoriteGrammarsCount} 语法",
                isEnabled = (uiState.favoriteWordsCount + uiState.favoriteGrammarsCount) > 0
            ),
            com.jian.nemo.feature.test.presentation.settings.components.SingleSelectOptionItem(
                key = QuestionSource.TODAY.key,
                title = "今日学习的内容",
                subtitle = "${uiState.todayLearnedCount} 词 · ${uiState.todayLearnedGrammarCount} 语法",
                isEnabled = (uiState.todayLearnedCount + uiState.todayLearnedGrammarCount) > 0
            ),
            com.jian.nemo.feature.test.presentation.settings.components.SingleSelectOptionItem(
                key = QuestionSource.TODAY_REVIEWED.key,
                title = "今日复习的内容",
                subtitle = "${uiState.todayReviewedCount} 词 · ${uiState.todayReviewedGrammarCount} 语法",
                isEnabled = (uiState.todayReviewedCount + uiState.todayReviewedGrammarCount) > 0
            ),
            com.jian.nemo.feature.test.presentation.settings.components.SingleSelectOptionItem(
                key = QuestionSource.LEARNED.key,
                title = "所有已学习过的内容",
                subtitle = "${uiState.allLearnedWordsCount} 词 · ${uiState.allLearnedGrammarsCount} 语法",
                isEnabled = (uiState.allLearnedWordsCount + uiState.allLearnedGrammarsCount) > 0
            ),
            com.jian.nemo.feature.test.presentation.settings.components.SingleSelectOptionItem(
                key = QuestionSource.ALL.key,
                title = "所有内容",
                subtitle = "${uiState.totalWordsCount} 词 · ${uiState.totalGrammarsCount} 语法",
                isEnabled = (uiState.totalWordsCount + uiState.totalGrammarsCount) > 0
            )
        )
    }

    val wrongAnswerRemovalOptions = remember {
        listOf(
            com.jian.nemo.feature.test.presentation.settings.components.SingleSelectOptionItem(
                key = 0,
                title = "不移除",
                subtitle = "做对后保留在错题本中，由您手动管理"
            ),
            com.jian.nemo.feature.test.presentation.settings.components.SingleSelectOptionItem(
                key = 3,
                title = "连续答对 3 次",
                subtitle = "在测试中连续答对 3 次后自动移出错题本"
            ),
            com.jian.nemo.feature.test.presentation.settings.components.SingleSelectOptionItem(
                key = 5,
                title = "连续答对 5 次",
                subtitle = "在测试中连续答对 5 次后自动移出错题本"
            ),
            com.jian.nemo.feature.test.presentation.settings.components.SingleSelectOptionItem(
                key = 7,
                title = "连续答对 7 次",
                subtitle = "在测试中连续答对 7 次后自动移出错题本"
            ),
            com.jian.nemo.feature.test.presentation.settings.components.SingleSelectOptionItem(
                key = 10,
                title = "连续答对 10 次",
                subtitle = "深度掌握标准，连续答对 10 次后自动移出错题本"
            )
        )
    }

    val wrongAnswerRemovalLabels = remember {
        mapOf(
            0 to "不移除",
            3 to "3次",
            5 to "5次",
            7 to "7次",
            10 to "10次"
        )
    }

    val contentTypeOptions = remember(isRestrictedMode, testModeId) {
        if (isRestrictedMode) {
            listOf(
                com.jian.nemo.feature.test.presentation.settings.components.SingleSelectOptionItem(
                    key = TestContentType.WORDS.key,
                    title = "仅测试单词",
                    subtitle = "仅抽取词汇库中的单词进行测试"
                )
            )
        } else {
            listOf(
                com.jian.nemo.feature.test.presentation.settings.components.SingleSelectOptionItem(
                    key = TestContentType.WORDS.key,
                    title = "仅测试单词",
                    subtitle = "仅抽取词汇库中的单词进行测试"
                ),
                com.jian.nemo.feature.test.presentation.settings.components.SingleSelectOptionItem(
                    key = TestContentType.GRAMMAR.key,
                    title = "仅测试语法",
                    subtitle = "仅抽取文法库中的语法条目进行测试"
                ),
                com.jian.nemo.feature.test.presentation.settings.components.SingleSelectOptionItem(
                    key = TestContentType.MIXED.key,
                    title = "单词和语法混合",
                    subtitle = "按设置比例混合抽取词汇与语法进行综合测试"
                )
            )
        }
    }
    val currentContentTypeLabel = remember(contentTypeOptions, config.testContentType) {
        contentTypeOptions.find { it.key == config.testContentType.key }?.title ?: "未知类型"
    }
    val allLevels = remember { listOf("N5", "N4", "N3", "N2", "N1") }

    // BottomSheet 逻辑
    if (showBottomSheet) {
        NemoModalBottomSheet(
            onDismissRequest = {
                viewModel.ensureValidLevels()
                showBottomSheet = false
            },
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = null // 由内容组件内置居中 Indicator 和关闭按钮
        ) {
            TestSettingsBottomSheetContent(
                currentSetting = currentSetting,
                config = config,
                uiState = uiState,
                testModeId = testModeId,
                questionCountOptions = questionCountOptions,
                timeLimitOptions = timeLimitOptions,
                questionSourceOptions = questionSourceOptions,
                wrongAnswerRemovalOptions = wrongAnswerRemovalOptions,
                contentTypeOptions = contentTypeOptions,
                allLevels = allLevels,
                onUpdateConfig = { viewModel.updateConfig(it) },
                isQuestionTypeSupported = { viewModel.isQuestionTypeSupported(it) },
                onToggleLevel = { level, isGrammar -> viewModel.toggleLevel(level, isGrammar) },
                onExclusiveSelectLevel = { level, isGrammar -> viewModel.exclusiveSelectLevel(level, isGrammar) },
                onToggleAllLevels = { isGrammar -> viewModel.toggleAllLevels(isGrammar) },
                onShowCustomQuestionCount = { showCustomQuestionCountDialog = true },
                onShowCustomTimeLimit = { showCustomTimeLimitDialog = true },
                onDismiss = {
                    viewModel.ensureValidLevels()
                    showBottomSheet = false
                },
                onSnackbar = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }
            )
        }
    }

    CustomQuestionCountDialog(showCustomQuestionCountDialog, config.questionCount, { showCustomQuestionCountDialog = false }) { count ->
        updateConfig { it.copy(questionCount = count) }
    }
    CustomTimeLimitDialog(showCustomTimeLimitDialog, config.timeLimitMinutes, { showCustomTimeLimitDialog = false }) { updateConfig { cfg -> cfg.copy(timeLimitMinutes = it) } }

    NemoScaffold(
        modifier = Modifier
            .fillMaxSize()
            .containerTransform(
                key = "container_test_${testModeId ?: "default"}",
                shape = RoundedCornerShape(0.dp)
            ),
        title = pageTitle,
        onBack = { if (isGenerating) starterViewModel.cancelGeneration() else onBack() },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        backgroundColor = backgroundColor
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(padding)
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                BasicSettingsSection(
                    config = config,
                    testModeId = testModeId,
                    uiState = uiState,
                    questionSourceOptions = questionSourceOptions,
                    wrongAnswerRemovalLabels = wrongAnswerRemovalLabels,
                    currentContentTypeLabel = currentContentTypeLabel,
                    onSettingClick = { setting ->
                        currentSetting = setting
                        showBottomSheet = true
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                QuizSettingsSection(
                    config = config,
                    testModeId = testModeId,
                    onUpdateConfig = { viewModel.updateConfig(it) }
                )

                Spacer(modifier = Modifier.height(120.dp))
            }

            // ===== 悬浮开始测试按钮 (Floating Overlay) =====
            Box(
                 modifier = Modifier
                     .align(Alignment.BottomCenter)
                     .fillMaxWidth()
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

                 LiquidButton(
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
                     isInteractive = !isGenerating,
                     modifier = Modifier
                         .fillMaxWidth()
                         .height(56.dp),
                     shape = RoundedCornerShape(28.dp),
                     backgroundColor = MaterialTheme.colorScheme.primary
                 ) {
                     Row(
                         horizontalArrangement = Arrangement.Center,
                         verticalAlignment = Alignment.CenterVertically
                     ) {
                         if (isGenerating) {
                             NemoChasingDotsLoader(size = 24.dp)
                             Spacer(Modifier.width(8.dp))
                             Text("准备中...", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                         } else {
                             Text("开始测试", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                         }
                     }
                 }
            }
        }
    }
}
