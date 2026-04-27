package com.jian.nemo.feature.test.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jian.nemo.core.ui.component.common.CommonHeader
import com.jian.nemo.feature.test.presentation.settings.components.CustomQuestionCountDialog
import com.jian.nemo.feature.test.presentation.settings.components.CustomTimeLimitDialog
import com.jian.nemo.feature.test.domain.model.TestConfig
import com.jian.nemo.feature.test.domain.model.QuestionSource
import com.jian.nemo.feature.test.domain.model.TestContentType
import kotlinx.coroutines.launch

import com.jian.nemo.feature.test.presentation.settings.components.BasicSettingsSection
import com.jian.nemo.feature.test.presentation.settings.components.QuizSettingsSection
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
            "comprehensive" -> "综合测试设置"
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
    val backgroundColor = MaterialTheme.colorScheme.background

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
    val questionSourceOptions = remember(uiState.todayLearnedCount, uiState.todayLearnedGrammarCount) {
        listOf(
            "我的错题" to QuestionSource.WRONG.key,
            "我的收藏" to QuestionSource.FAVORITE.key,
            "今日学习的内容 (${uiState.todayLearnedCount}词 / ${uiState.todayLearnedGrammarCount}语法)" to QuestionSource.TODAY.key,
            "今日复习的内容 (${uiState.todayReviewedCount}词 / ${uiState.todayReviewedGrammarCount}语法)" to QuestionSource.TODAY_REVIEWED.key,
            "所有已学习过的内容" to QuestionSource.LEARNED.key,
            "所有内容" to QuestionSource.ALL.key
        )
    }
    val wrongAnswerRemovalOptions = remember { listOf(0, 3, 5, 7, 10) }
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
            listOf("仅测试单词" to TestContentType.WORDS.key)
        } else if (testModeId == "comprehensive") {
            listOf("仅测试单词" to TestContentType.WORDS.key, "单词和语法混合" to TestContentType.MIXED.key)
        } else {
            listOf("仅测试单词" to TestContentType.WORDS.key, "仅测试语法" to TestContentType.GRAMMAR.key, "单词和语法混合" to TestContentType.MIXED.key)
        }
    }
    val currentContentTypeLabel = remember(contentTypeOptions, config.testContentType) {
        contentTypeOptions.find { it.second == config.testContentType.key }?.first ?: "未知类型"
    }
    val allLevels = remember { listOf("N5", "N4", "N3", "N2", "N1") }

    // BottomSheet 逻辑
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                viewModel.ensureValidLevels()
                showBottomSheet = false
            },
            containerColor = MaterialTheme.colorScheme.surface,
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
                wrongAnswerRemovalLabels = wrongAnswerRemovalLabels,
                contentTypeOptions = contentTypeOptions,
                allLevels = allLevels,
                onUpdateConfig = { viewModel.updateConfig(it) },
                onUpdateQuestionDistribution = { viewModel.updateQuestionDistribution(it) },
                onQuestionTypeCountChange = { key, count -> viewModel.updateComprehensiveQuestionCount(key, count) },
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
        if (testModeId == "comprehensive") viewModel.updateQuestionDistribution(count)
    }
    CustomTimeLimitDialog(showCustomTimeLimitDialog, config.timeLimitMinutes, { showCustomTimeLimitDialog = false }) { updateConfig { cfg -> cfg.copy(timeLimitMinutes = it) } }

    Scaffold(
        topBar = {
            CommonHeader(
                title = pageTitle,
                onBack = { if (isGenerating) starterViewModel.cancelGeneration() else onBack() },
                backgroundColor = backgroundColor
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = backgroundColor
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
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
                    onUpdateConfig = { viewModel.updateConfig(it) }
                )

                Spacer(modifier = Modifier.height(100.dp))
            }

            // ===== 悬浮开始测试按钮 (Floating Overlay) =====
            Box(
                 modifier = Modifier
                     .align(Alignment.BottomCenter)
                     .fillMaxWidth()
                     .padding(20.dp)
                     .safeDrawingPadding()
             ) {
                 LaunchedEffect(Unit) {
                    starterViewModel.navigationEvent.collect { event -> onNavigate(event) }
                }

                LaunchedEffect(Unit) {
                    starterViewModel.errorEvent.collect { error ->
                        snackbarHostState.showSnackbar(error)
                    }
                }

                 Button(
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
                     enabled = !isGenerating,
                     modifier = Modifier.fillMaxWidth().height(56.dp).shadow(8.dp, RoundedCornerShape(24.dp)),
                     shape = RoundedCornerShape(24.dp),
                     colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                 ) {
                     if (isGenerating) {
                         CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                         Spacer(Modifier.width(8.dp))
                         Text("准备中...", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                     } else {
                         Text("开始测试", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                     }
                 }
            }
        }
    }
}
