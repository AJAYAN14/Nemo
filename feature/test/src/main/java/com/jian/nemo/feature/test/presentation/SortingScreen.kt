package com.jian.nemo.feature.test.presentation

import android.annotation.SuppressLint
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.nemo.core.domain.model.ExplanationPayload
import com.jian.nemo.core.domain.model.SortableChar
import com.jian.nemo.core.domain.model.TestQuestion
import com.jian.nemo.feature.test.components.QuestionExplanationCard
import com.jian.nemo.feature.test.TestViewModel

/**
 * Macaron / Morandi Color Palette for chips
 */
private val MACARON_PALETTE = listOf(
    Color(0xFF818CF8), // Indigo 400
    Color(0xFFF472B6), // Pink 400
    Color(0xFFFBBF24), // Amber 400
    Color(0xFF34D399), // Emerald 400
    Color(0xFF22D3EE), // Cyan 400
    Color(0xFFA78BFA)  // Violet 400
)

private fun getMacaronColor(index: Int, charId: String): Color {
    val idHash = charId.hashCode()
    return MACARON_PALETTE[Math.abs(idHash) % MACARON_PALETTE.size]
}

enum class ChipStatus { NORMAL, CORRECT, WRONG }

@OptIn(ExperimentalLayoutApi::class, ExperimentalAnimationApi::class)
@Composable
fun SortingScreen(
    viewModel: TestViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val questions = uiState.questions

    // Removed the outer Scaffold that caused double-padding/EdgeToEdge loss.
    // Instead, using a Box with the gradient background as the outermost container.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF5F8FF), // Crystal Blue 50 - High Purity
                        Color.White
                    ),
                    startY = 0f,
                    endY = 1200f // Smoother transition
                )
            )
    ) {
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
            label = "sorting_question_transition"
        ) { targetIndex ->
            val currentQuestion = questions.getOrNull(targetIndex) as? TestQuestion.Sorting ?: return@AnimatedContent
            val shakeOffset = remember { androidx.compose.animation.core.Animatable(0f) }

            com.jian.nemo.feature.test.components.UnifiedTestScreen(
                headerContent = {
                    com.jian.nemo.feature.test.components.TestHeader(
                        onBack = { viewModel.confirmExitTest() },
                        timeLimitSeconds = uiState.timeLimitSeconds,
                        timeRemainingSeconds = uiState.timeRemainingSeconds,
                        word = currentQuestion.word,
                        onToggleFavorite = { wordId, isFavorite -> viewModel.toggleFavorite(wordId, isFavorite) },
                        onPause = { viewModel.pauseTest() }
                    )
                },
                progressContent = {
                    com.jian.nemo.feature.test.components.SimpleProgressIndicator(
                        current = uiState.questions.count { it.isAnswered },
                        total = uiState.questions.size
                    )
                },
                testContent = {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val isGrammarQuestion = currentQuestion.word.level.startsWith("Grammar:")

                        Text(
                            text = currentQuestion.word.chinese,
                            style = MaterialTheme.typography.displayMedium.copy(
                                 fontSize = 44.sp,
                                 fontWeight = FontWeight.Black,
                                 letterSpacing = (-1).sp
                            ),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                             modifier = Modifier.padding(top = 16.dp)
                        )
                        Text(
                            text = if (isGrammarQuestion) "选择字符，按正确顺序排列" else "选择假名，按正确顺序排列",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
                        )

                        AnswerContainer(
                            question = currentQuestion,
                            userAnswer = uiState.userAnswerChars,
                            shakeOffset = shakeOffset.value,
                            onDeselect = { char -> viewModel.deselectSortableChar(char) }
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        SortingFeedback(
                            question = currentQuestion,
                            shakeOffset = shakeOffset
                        )

                        if (currentQuestion.isAnswered && !isGrammarQuestion) {
                            Spacer(modifier = Modifier.height(16.dp))
                            QuestionExplanationCard(
                                payload = ExplanationPayload.WordSummary(
                                    japanese = currentQuestion.word.japanese,
                                    hiragana = currentQuestion.word.hiragana,
                                    meaning = currentQuestion.word.chinese
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        AnimatedVisibility(
                            visible = !currentQuestion.isAnswered,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut(),
                            label = "options_visibility"
                        ) {
                            Column {
                                Spacer(modifier = Modifier.height(16.dp))
                                OptionsContainer(
                                    options = currentQuestion.options,
                                    onSelect = { char -> viewModel.selectSortableChar(char) }
                                )
                            }
                        }
                    }
                },
                footerContent = {
                    // Removed the Surface container that acted as a background. 
                    // Made the footer area completely transparent as requested.
                    com.jian.nemo.feature.test.components.TestFooter(
                        onPrev = { viewModel.previousQuestion() },
                        onNext = { viewModel.nextQuestion() },
                        onSubmit = { viewModel.submitAnswer() },
                        onFinish = { viewModel.finishTest() },
                        canGoPrev = uiState.currentIndex > 0,
                        canSubmit = uiState.userAnswerChars.isNotEmpty(),
                        isAnswered = currentQuestion.isAnswered,
                        isLastQuestion = uiState.currentIndex == uiState.questions.size - 1,
                        submitText = "检查",
                        isAutoAdvancing = uiState.isAutoAdvancing
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AnswerContainer(
    question: TestQuestion.Sorting,
    userAnswer: List<SortableChar>,
    shakeOffset: Float,
    onDeselect: (SortableChar) -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5
    val containerColor by animateColorAsState(
        targetValue = when {
            question.isAnswered && question.isCorrect -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
            question.isAnswered && !question.isCorrect -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
            else -> if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) else Color(0xFFE0E7FF) // Clear Indigo 100
        },
        label = "answerContainerColor"
    )

    val borderColor = when {
        question.isAnswered && question.isCorrect -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
        question.isAnswered && !question.isCorrect -> MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
        else -> if (isDark) Color.White.copy(alpha = 0.1f) else Color(0xFFCBD5E1) // Physical Slate 300
    }

    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .offset(x = shakeOffset.dp)
            .background(containerColor, RoundedCornerShape(32.dp))
            .padding(20.dp)
            .heightIn(min = 100.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        if (userAnswer.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = "在此处构建答案",
                    color = Color(0xFF312E81).copy(alpha = 0.3f), // Indigo 900
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            userAnswer.forEachIndexed { index, char ->
                val rotation = remember(char.id) { ((-8..8).random()).toFloat() }
                
                SortableChip(
                    char = char,
                    isSelected = true,
                    backgroundColor = if (question.isAnswered) null else getMacaronColor(index, char.id),
                    modifier = Modifier.graphicsLayer { rotationZ = rotation },
                    onClick = { onDeselect(char) },
                    status = when {
                        !question.isAnswered -> ChipStatus.NORMAL
                        question.isCorrect -> ChipStatus.CORRECT
                        else -> ChipStatus.WRONG
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OptionsContainer(
    options: List<SortableChar>,
    onSelect: (SortableChar) -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5
    
    // UI/UX PRO MAX: Grounded Tray for Options
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f) 
                else Color(0xFFF8FAFF), // Crystal Indigo Base
                RoundedCornerShape(32.dp)
            )
            .padding(vertical = 24.dp, horizontal = 12.dp)
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            options.forEachIndexed { index, char ->
                Box {
                    SortableChip(
                        char = char,
                        isSelected = false,
                        backgroundColor = getMacaronColor(index, char.id),
                        enabled = !char.isSelected,
                        modifier = Modifier.graphicsLayer { 
                            alpha = if (char.isSelected) 0f else 1f 
                            scaleX = if (char.isSelected) 0.8f else 1f
                            scaleY = if (char.isSelected) 0.8f else 1f
                        },
                        onClick = { if (!char.isSelected) onSelect(char) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SortableChip(
    char: SortableChar,
    isSelected: Boolean,
    onClick: () -> Unit,
    status: ChipStatus = ChipStatus.NORMAL,
    backgroundColor: Color? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val hapticFeedback = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        label = "chipScale",
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    val targetBackgroundColor = when {
        status == ChipStatus.CORRECT -> Color(0xFF4CAF50)
        status == ChipStatus.WRONG -> Color(0xFFF44336)
        isSelected -> backgroundColor ?: MaterialTheme.colorScheme.primary
        else -> if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.White
    }

    val chipColor by animateColorAsState(
        targetValue = targetBackgroundColor,
        label = "chipColor",
        animationSpec = spring(stiffness = Spring.StiffnessLow)
    )

    val textColor = if (isSelected || status != ChipStatus.NORMAL) Color.White else MaterialTheme.colorScheme.onSurface
    val borderColor = if (isSelected || status != ChipStatus.NORMAL) Color.Transparent else if (isDark) Color.White.copy(alpha = 0.1f) else Color(0xFFF1F5F9)

    Box(
        modifier = modifier
            .graphicsLayer { 
                scaleX = scale
                scaleY = scale 
            }
            .clip(RoundedCornerShape(18.dp))
            .background(chipColor)
            .then(
                if (status == ChipStatus.NORMAL && !isSelected) 
                    Modifier.border(1.dp, borderColor, RoundedCornerShape(18.dp))
                else Modifier
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null
            ) {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = char.char.toString(),
            color = textColor,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SortingFeedback(
    question: TestQuestion.Sorting,
    shakeOffset: androidx.compose.animation.core.Animatable<Float, *>
) {
    val context = LocalContext.current
    LaunchedEffect(question.isAnswered, question.isCorrect) {
        if (question.isAnswered && !question.isCorrect) {
            @SuppressLint("MissingPermission")
            fun vibrateDevice() {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
                } else {
                    @Suppress("DEPRECATION") context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                }
                vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
            }
            vibrateDevice()
            repeat(3) {
                shakeOffset.animateTo(10f, animationSpec = keyframes { durationMillis = 50 })
                shakeOffset.animateTo(-10f, animationSpec = keyframes { durationMillis = 50 })
            }
            shakeOffset.animateTo(0f)
        }
    }

    AnimatedVisibility(
        visible = question.isAnswered,
        enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut()
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (question.isCorrect) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp, horizontal = 16.dp)
            ) {
                if (question.isCorrect) {
                    Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(48.dp))
                    Text("回答正确！", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.titleLarge)
                } else {
                    Icon(Icons.Filled.Cancel, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                    Text("回答错误", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("正确答案", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    Text(question.word.hiragana, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black))
                }
            }
        }
    }
}
