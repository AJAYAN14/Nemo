package com.jian.nemo.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.hilt.navigation.compose.hiltViewModel
import com.jian.nemo.feature.settings.components.PremiumCard
import com.jian.nemo.feature.settings.components.UnsavedChangesDialog

/**
 * 记忆算法配置二级页面 (SRS Memory Algorithm Configuration)
 * 采用与设置主页一致的 Premium 设计风格
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedLearningSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // 同步本地状态进行即时编辑反馈
    var stepsInput by remember(uiState.learningSteps) { mutableStateOf(uiState.learningSteps) }
    var relearningStepsInput by remember(uiState.relearningSteps) { mutableStateOf(uiState.relearningSteps) }
    var limitInput by remember(uiState.learnAheadLimit) { mutableFloatStateOf(uiState.learnAheadLimit.toFloat()) }
    var leechThresholdInput by remember(uiState.leechThreshold) { mutableIntStateOf(uiState.leechThreshold.coerceIn(1, 12)) }
    var leechActionInput by remember(uiState.leechAction) {
        mutableStateOf(if (uiState.leechAction == "bury_today") "bury_today" else "skip")
    }

    // 判断是否有未保存更改
    val hasUnsavedChanges = remember(stepsInput, relearningStepsInput, limitInput, leechThresholdInput, leechActionInput, uiState) {
        stepsInput != uiState.learningSteps ||
        relearningStepsInput != uiState.relearningSteps ||
        limitInput.toInt() != uiState.learnAheadLimit ||
        leechThresholdInput != uiState.leechThreshold ||
        leechActionInput != (if (uiState.leechAction == "bury_today") "bury_today" else "skip")
    }

    var showExitConfirmation by remember { mutableStateOf(false) }

    // 拦截系统返回键
    BackHandler(enabled = hasUnsavedChanges) {
        showExitConfirmation = true
    }

    val accentColor = Color(0xFFAF52DE) // NemoPurple
    val density = LocalDensity.current
    val statusBarHeight = with(density) { WindowInsets.statusBars.getTop(density).toDp() }
    val navigationBarHeight = with(density) { WindowInsets.navigationBars.getBottom(density).toDp() }

    // 保存逻辑封装
    val onSaveAndExit = {
        if (stepsInput.isNotBlank() && relearningStepsInput.isNotBlank()) {
            viewModel.onEvent(SettingsEvent.SaveAdvancedLearningSettings(
                learningSteps = stepsInput,
                relearningSteps = relearningStepsInput,
                learnAheadLimit = limitInput.toInt(),
                leechThreshold = leechThresholdInput,
                leechAction = leechActionInput
            ))
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("记忆算法配置", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (hasUnsavedChanges) {
                            showExitConfirmation = true
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (showExitConfirmation) {
            UnsavedChangesDialog(
                onSaveAndExit = onSaveAndExit,
                onDiscardChanges = onNavigateBack,
                onDismiss = { showExitConfirmation = false },
                accentColor = accentColor
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. 风险提示项
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "参数说明",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "间隔重复算法（SRS）决定了您的复习频率。错误的设置可能导致不得不频繁进行无效复习，请谨慎修改。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            // 2. 学习复习步进
            com.jian.nemo.feature.settings.components.SettingsSectionTitle(text = "复习步进 (Steps)")
            PremiumCard {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    // Learning Steps
                    Column {
                        Text(
                            text = "新卡片学习阶段 (分钟)",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = stepsInput,
                            onValueChange = { stepsInput = it },
                            placeholder = { Text("1 10") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                        Text(
                            text = "默认 '1 10'，表示1分钟后和10分钟后分别进行初次复习。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    // Relearning Steps
                    Column {
                        Text(
                            text = "错误重学阶段 (分钟)",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = relearningStepsInput,
                            onValueChange = { relearningStepsInput = it },
                            placeholder = { Text("1 10") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                        Text(
                            text = "默认 '1 10'，表示忘记已学会卡片后进行的重学步进。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            // 3. 算法高级策略
            com.jian.nemo.feature.settings.components.SettingsSectionTitle(text = "高级复习策略")
            PremiumCard {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    // Learn Ahead Limit
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "提前复习阈值",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${limitInput.toInt()} 分钟",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = accentColor
                            )
                        }
                        Slider(
                            value = limitInput,
                            onValueChange = { limitInput = it },
                            valueRange = 0f..60f,
                            steps = 59,
                            colors = SliderDefaults.colors(
                                thumbColor = accentColor,
                                activeTrackColor = accentColor,
                                inactiveTrackColor = accentColor.copy(alpha = 0.2f)
                            )
                        )
                        Text(
                            text = "冷却时间低于此值时，允许立即开始复习。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }

                    // Leech Management
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Leech 阈值 (累计错误)",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { leechThresholdInput = (leechThresholdInput - 1).coerceAtLeast(1) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Rounded.RemoveCircleOutline, contentDescription = null, tint = accentColor)
                                }
                                Text(
                                    text = "$leechThresholdInput 次",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = accentColor,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )
                                IconButton(
                                    onClick = { leechThresholdInput = (leechThresholdInput + 1).coerceAtMost(12) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Rounded.AddCircleOutline, contentDescription = null, tint = accentColor)
                                }
                            }
                        }
                    }

                    // Leech Action
                    Column {
                        Text(
                            text = "Leech 处理行为",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Option 1: Skip
                        Surface(
                            onClick = { leechActionInput = "skip" },
                            color = if (leechActionInput == "skip") accentColor.copy(alpha = 0.1f) else Color.Transparent,
                            border = if (leechActionInput == "skip") null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = leechActionInput == "skip", onClick = { leechActionInput = "skip" })
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("冻结卡片 (Skip)", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                    Text("命中后不再进入常规复习，移至复学清单管理", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Option 2: Bury
                        Surface(
                            onClick = { leechActionInput = "bury_today" },
                            color = if (leechActionInput == "bury_today") accentColor.copy(alpha = 0.1f) else Color.Transparent,
                            border = if (leechActionInput == "bury_today") null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = leechActionInput == "bury_today", onClick = { leechActionInput = "bury_today" })
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("仅埋藏今日 (Bury)", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                    Text("今日复习中不再出现，明日自动回归队列", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            // 4. 保存按钮
            Button(
                onClick = {
                    if (stepsInput.isNotBlank() && relearningStepsInput.isNotBlank()) {
                         viewModel.onEvent(SettingsEvent.SaveAdvancedLearningSettings(
                             learningSteps = stepsInput,
                             relearningSteps = relearningStepsInput,
                             learnAheadLimit = limitInput.toInt(),
                             leechThreshold = leechThresholdInput,
                             leechAction = leechActionInput
                         ))
                         onNavigateBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Text("确认并应用配置", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
            
            Spacer(modifier = Modifier.height(navigationBarHeight + 16.dp))
        }
    }
}
