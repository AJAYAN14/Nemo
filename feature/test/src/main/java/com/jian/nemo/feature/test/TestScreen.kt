package com.jian.nemo.feature.test

import androidx.compose.animation.AnimatedContent
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jian.nemo.core.domain.model.TestMode
import com.jian.nemo.core.domain.model.TestQuestion
import com.jian.nemo.feature.test.presentation.TypingQuestionPage
import com.jian.nemo.feature.test.presentation.MultipleChoiceQuestionPage

/**
 * 测试界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestScreen(
    level: String,
    mode: TestMode,
    questionType: com.jian.nemo.core.domain.model.QuestionType,
    contentType: String = "words",
    source: String = "today",
    onNavigateBack: () -> Unit,
    viewModel: TestViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // 启动时生成题目
    LaunchedEffect(level, mode, contentType, source) {
        viewModel.startTest(level, mode, questionType = questionType, contentType = contentType, source = source)
    }

    // 监听副作用 (音效与震动)
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is TestEffect.PlaySound -> {
                    if (effect.isCorrect) {
                        com.jian.nemo.core.ui.util.SoundEffectPlayer.playCorrect(context)
                    } else {
                        com.jian.nemo.core.ui.util.SoundEffectPlayer.playError(context)
                    }
                }
                is TestEffect.Vibrate -> {
                    // 答错时震动80ms
                    // Suppress Permission Check: Manifest declared in app module
                    @SuppressLint("MissingPermission")
                    fun vibrateDevice() {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                            val vibrator = vibratorManager.defaultVibrator
                            vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
                        } else {
                            @Suppress("DEPRECATION")
                            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
                            } else {
                                @Suppress("DEPRECATION")
                                vibrator.vibrate(80)
                            }
                        }
                    }
                    vibrateDevice()
                }
            }
        }
    }

    // 退出确认对话框（复刻旧项目TestScreen.kt L76-104）
    if (uiState.showExitConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelExitTest() },
            containerColor = MaterialTheme.colorScheme.surface, // 去除默认紫色背景
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
            title = {
                Text(
                    text = "确认退出",
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = "确定要退出测试吗？当前进度将丢失。",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        // 保存当前测试进度（错题）
                        viewModel.actualExitTest()
                        // 关闭弹窗并返回
                        viewModel.cancelExitTest()
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("确认退出", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.cancelExitTest() }
                ) {
                    Text("继续测试")
                }
            }
        )
    }

    // 暂停对话框
    if (uiState.showPauseDialog) {
        com.jian.nemo.feature.test.components.PauseTestDialog(
            onConfirm = { viewModel.resumeTest() }
        )
    }

    // 处理系统返回键（复刻旧项目TestScreen.kt L107-109）
    androidx.activity.compose.BackHandler(
        enabled = uiState.isTestActive && !uiState.showExitConfirmation && !uiState.showPauseDialog
    ) {
        viewModel.confirmExitTest()
    }

    TestScreenContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onRetakeTest = viewModel::retakeTest,
        viewModel = viewModel
    )
}

/**
 * 测试界面（无状态，便于测试）
 */
@Composable
fun TestScreenContent(
    uiState: TestUiState,
    onNavigateBack: () -> Unit,
    onRetakeTest: () -> Unit,
    viewModel: TestViewModel
) {
    when {
        uiState.isLoading -> {
            // 加载中
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        uiState.error != null -> {
            // 错误提示
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = uiState.error ?: "",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = onNavigateBack) {
                    Text("返回")
                }
            }
        }

        uiState.showResult -> {
            // 测试结果
            TestResultScreen(
                result = uiState.testResult!!,
                onRetakeTest = onRetakeTest,
                onExit = onNavigateBack
            )
        }

        else -> {
            // 测试进行中
            val currentQuestion = uiState.currentQuestion

            if (currentQuestion != null) {
                // 使用AnimatedContent实现题目切换动画
                AnimatedContent(
                    targetState = uiState.currentIndex,
                    transitionSpec = {
                        if (targetState > initialState) {
                            slideInHorizontally(animationSpec = tween(300)) { width -> width } togetherWith
                                    slideOutHorizontally(animationSpec = tween(300)) { width -> -width }
                        } else {
                            slideInHorizontally(animationSpec = tween(300)) { width -> -width } togetherWith
                                    slideOutHorizontally(animationSpec = tween(300)) { width -> width }
                        }
                    },
                    label = "question_transition"
                ) { targetIndex ->
                    val question = uiState.questions.getOrNull(targetIndex)
                    if (question != null) {
                        when (question) {
                            is TestQuestion.MultipleChoice -> {
                                // 使用选择题界面
                                MultipleChoiceQuestionPage(viewModel = viewModel)
                            }
                            is TestQuestion.Typing -> {
                                // 使用手打题界面
                                TypingQuestionPage(viewModel = viewModel)
                            }
                            is TestQuestion.CardMatching -> {
                                // 使用卡片题界面
                                com.jian.nemo.feature.test.presentation.cardmatching.CardMatchingScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = onNavigateBack
                                )
                            }
                            is TestQuestion.Sorting -> {
                                // 使用排序题界面
                                com.jian.nemo.feature.test.presentation.SortingScreen(
                                    viewModel = viewModel
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


