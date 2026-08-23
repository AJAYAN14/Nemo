@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
package com.jian.nemo.feature.settings

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.jian.nemo.core.ui.modifier.softCardShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jian.nemo.core.designsystem.theme.*
import com.jian.nemo.core.ui.component.animation.NemoChasingDotsLoader
import com.jian.nemo.core.ui.component.common.CommonHeader
import com.jian.nemo.core.ui.component.NemoDialog
import com.jian.nemo.core.ui.component.common.NemoSnackbar
import com.jian.nemo.core.ui.component.common.NemoSnackbarType
import com.jian.nemo.core.ui.component.liquid.LiquidButton
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.jian.nemo.feature.settings.components.PremiumCard
import com.jian.nemo.feature.settings.components.SettingsSectionTitle
import androidx.compose.ui.res.painterResource
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import com.jian.nemo.core.designsystem.R as DesignR
import com.jian.nemo.core.domain.repository.AIConfig

@Composable
fun AISettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AISettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val hazeState = remember { HazeState() }

    val navigationBarHeight = with(density) { WindowInsets.navigationBars.getBottom(this).toDp() }
    val statusBarHeight = with(density) { WindowInsets.statusBars.getTop(this).toDp() }
    var configToDelete by remember { mutableStateOf<AIConfig?>(null) }

    LaunchedEffect(uiState.testResult) {
        if (uiState.testResult != null) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .haze(hazeState)
        ) {
            Scaffold(
                topBar = {
                    CommonHeader(
                        title = "AI 模型配置",
                        onBack = onNavigateBack,
                        actions = {
                            val navGroupBg = if (isDark) Color.White.copy(alpha = 0.15f) else Color.White
                            LiquidButton(
                                onClick = { viewModel.onEvent(AISettingsEvent.OpenEditModal(null)) },
                                backgroundColor = navGroupBg,
                                shape = CircleShape,
                                isInteractive = true,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = "新建配置",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    )
                },
                containerColor = MaterialTheme.colorScheme.background
            ) { paddingValues ->
                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        NemoChasingDotsLoader(size = 40.dp)
                    }
                } else if (uiState.configs.isEmpty()) {
                    EmptyAIConfigView(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = paddingValues.calculateTopPadding() + 8.dp,
                            bottom = navigationBarHeight + 32.dp
                        )
                    ) {
                        item {
                            PremiumCard(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .background(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                                RoundedCornerShape(14.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.AutoAwesome,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(
                                            text = "AI 智能工坊",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "配置专属密钥，解锁AI单词智能解析、实时翻译与发音评估等强大功能。",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            SettingsSectionTitle(text = "配置列表")
                        }
                        items(uiState.configs, key = { it.id }) { config ->
                            val isActive = uiState.activeConfigId == config.id
                            AIConfigCard(
                                config = config,
                                isActive = isActive,
                                isDark = isDark,
                                onSelect = { viewModel.onEvent(AISettingsEvent.SelectActiveConfig(config.id)) },
                                onEdit = { viewModel.onEvent(AISettingsEvent.OpenEditModal(config.id)) },
                                onDelete = { configToDelete = config }
                            )
                        }
                    }
                }
            }
        }

        // 高保真底栏滑动弹窗
        // 1. 全屏高斯毛玻璃遮罩
        AnimatedVisibility(
            visible = uiState.editingConfig != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            val maskColor = if (isDark) {
                Color.Black.copy(alpha = 0.55f)
            } else {
                Color.Black.copy(alpha = 0.35f)
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeChild(state = hazeState)
                    .background(maskColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (!uiState.isTesting) {
                            viewModel.onEvent(AISettingsEvent.CloseEditModal)
                        }
                    }
            )
        }

        // 2. 底部向上滑入的表单卡片
        AnimatedVisibility(
            visible = uiState.editingConfig != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            val editingConfig = uiState.editingConfig
            if (editingConfig != null) {
                val isNew = uiState.configs.none { it.id == editingConfig.id }
                
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.9f)
                        .imePadding()
                        .softCardShadow(borderRadius = 28.dp, isDark = isDark),
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.15f else 0.2f)
                    ),
                    tonalElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding()
                    ) {
                        // 1. 弹窗 Header：沉浸式左对齐大标题 + 右侧圆角关闭按钮
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isNew) "新建 AI 配置" else "编辑 AI 配置",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        letterSpacing = (-0.5).sp
                                    ),
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "配置自定义大语言模型 API 凭证与端点",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                                )
                            }
                            
                            IconButton(
                                onClick = { viewModel.onEvent(AISettingsEvent.CloseEditModal) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        color = if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else NemoNeutrals.Gray100,
                                        shape = CircleShape
                                    ),
                                enabled = !uiState.isTesting
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "取消",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        HorizontalDivider(
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.12f else 0.18f)
                        )

                        // 2. 弹窗表单滚动主体
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // 分组 1：基础信息卡片
                            FormSectionCard(
                                title = "基础信息",
                                isDark = isDark
                            ) {
                                AISettingTextField(
                                    label = "配置别名",
                                    value = editingConfig.name,
                                    onValueChange = { viewModel.onEvent(AISettingsEvent.UpdateEditingConfig(editingConfig.copy(name = it))) },
                                    placeholder = "例如: 我的工作号 / 主用 Gemini"
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Column {
                                    Text(
                                        text = "AI 服务平台",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    PlatformDropdownSelector(
                                        selectedPlatform = editingConfig.platform,
                                        onPlatformSelected = { 
                                            val defaultModel = when(it) {
                                                "gemini" -> "gemini-3.1-pro-preview"
                                                "deepseek" -> "deepseek-v4-pro"
                                                "openai" -> "gpt-5.5"
                                                "claude" -> "claude-4.7-opus"
                                                "doubao" -> "doubao-pro-256k"
                                                "mimo" -> "mimo-v2.5-pro"
                                                else -> ""
                                            }
                                            viewModel.onEvent(AISettingsEvent.UpdateEditingConfig(
                                                editingConfig.copy(platform = it, model = defaultModel)
                                            )) 
                                        }
                                    )
                                }
                            }

                            // 分组 2：模型与凭证卡片
                            FormSectionCard(
                                title = "模型与凭证",
                                isDark = isDark
                            ) {
                                AISettingTextField(
                                    label = "模型标识码 (Model ID)",
                                    value = editingConfig.model,
                                    onValueChange = { viewModel.onEvent(AISettingsEvent.UpdateEditingConfig(editingConfig.copy(model = it))) },
                                    placeholder = "例如: gemini-3.1-pro-preview"
                                )

                                RecommendedModelChips(
                                    platform = editingConfig.platform,
                                    currentModel = editingConfig.model,
                                    onModelSelected = { viewModel.onEvent(AISettingsEvent.UpdateEditingConfig(editingConfig.copy(model = it))) }
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                AISettingTextField(
                                    label = "API Key 密钥",
                                    value = editingConfig.apiKey,
                                    onValueChange = { viewModel.onEvent(AISettingsEvent.UpdateEditingConfig(editingConfig.copy(apiKey = it))) },
                                    placeholder = "输入 API 令牌密钥",
                                    isPasswordField = true
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                AISettingTextField(
                                    label = "自定义代理端点 (可选)",
                                    value = editingConfig.baseUrl,
                                    onValueChange = { viewModel.onEvent(AISettingsEvent.UpdateEditingConfig(editingConfig.copy(baseUrl = it))) },
                                    placeholder = "例如: https://api.openai-proxy.com"
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        // 3. 底部固定操作栏 (Sticky Action Bar)
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.12f else 0.18f)
                        )

                        FormBottomActionBar(
                            isTesting = uiState.isTesting,
                            canTest = editingConfig.apiKey.isNotBlank(),
                            canSave = editingConfig.apiKey.isNotBlank() && editingConfig.name.isNotBlank(),
                            onTestClick = { viewModel.onEvent(AISettingsEvent.TestConnection) },
                            onSaveClick = { viewModel.onEvent(AISettingsEvent.SaveConfig) },
                            isDark = isDark
                        )
                    }
                }
            }
        }

        // 测试状态气泡提示
        uiState.testResult?.let { result ->
            NemoSnackbar(
                visible = true,
                message = if (result.success) "API 连接正常，测试成功" else result.message,
                type = if (result.success) NemoSnackbarType.SUCCESS else NemoSnackbarType.ERROR,
                icon = if (result.success) Icons.Rounded.TaskAlt else Icons.Rounded.ErrorOutline,
                onDismiss = { viewModel.onEvent(AISettingsEvent.ClearTestResult) },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = statusBarHeight + 8.dp)
            )
        }

        // 删除二次确认弹窗
        configToDelete?.let { config ->
            NemoDialog(
                onDismissRequest = { configToDelete = null },
                title = "确认删除此配置？",
                text = "您确认要删除 AI 配置 \"${config.name}\" 吗？删除后该配置的密钥和代理信息将永久丢失，且无法恢复。",
                confirmText = "确认删除",
                dismissText = "取消",
                isDangerous = true,
                onConfirm = {
                    viewModel.onEvent(AISettingsEvent.DeleteConfig(config.id))
                    configToDelete = null
                }
            )
        }
    }
}

@Composable
fun AIConfigCard(
    config: AIConfig,
    isActive: Boolean,
    isDark: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val platformColor = when (config.platform) {
        "gemini" -> Color(0xFF6E56CF) // 高贵蓝紫
        "deepseek" -> Color(0xFF1E88E5) // 科技蓝
        "openai" -> Color(0xFF10A37F) // 薄荷绿
        "claude" -> Color(0xFFE05C2B) // 橙红色
        "doubao" -> Color(0xFF00B4D8) // 活力青绿
        "mimo" -> Color(0xFFFF6700) // 小米橙
        else -> Color(0xFF757575) // 自定义中性灰
    }
    
    val platformName = when (config.platform) {
        "gemini" -> "Gemini"
        "deepseek" -> "DeepSeek"
        "openai" -> "ChatGPT"
        "claude" -> "Claude"
        "doubao" -> "Doubao"
        "mimo" -> "Mimo"
        else -> "自定义平台"
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        label = "scale",
        animationSpec = tween(200)
    )

    val containerColor = if (isDark) {
        if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
        else MaterialTheme.colorScheme.surfaceContainer
    } else {
        Color.White
    }

    val borderColor = if (isActive) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.15f else 0.2f)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null // 去除默认 ripple，改用 scale 微手感
            ) {
                onSelect()
            },
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = BorderStroke(
            width = if (isActive) 1.5.dp else 1.dp,
            color = borderColor
        ),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // platform-specific rounded icon container
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(platformColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                val painterId = when (config.platform) {
                    "gemini" -> DesignR.drawable.ic_gemini
                    "deepseek" -> DesignR.drawable.ic_deepseek
                    "openai" -> DesignR.drawable.ic_openai
                    "mimo" -> DesignR.drawable.ic_mimo
                    "claude" -> DesignR.drawable.ic_claude
                    "doubao" -> DesignR.drawable.ic_doubao
                    else -> null
                }
                
                if (painterId != null) {
                    Icon(
                        painter = painterResource(id = painterId),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = platformColor
                    )
                } else {
                    val initialChar = (config.platform.firstOrNull() ?: config.name.firstOrNull() ?: '?')
                        .toString()
                        .uppercase()
                    Text(
                        text = initialChar,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = platformColor
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // middle details column
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = config.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    if (isActive) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                .border(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "当前启用",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "$platformName  ·  ${config.model}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "编辑配置",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "删除配置",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
fun FormSectionCard(
    title: String,
    isDark: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    val containerColor = if (isDark) {
        MaterialTheme.colorScheme.surfaceContainer
    } else {
        NemoNeutrals.Gray50.copy(alpha = 0.7f)
    }
    val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.12f else 0.18f)

    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(
                letterSpacing = 0.5.sp
            ),
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = containerColor,
            border = BorderStroke(1.dp, borderColor)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                content = content
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlatformDropdownSelector(
    selectedPlatform: String,
    onPlatformSelected: (String) -> Unit
) {
    val platforms = listOf(
        Triple("gemini", "Gemini", DesignR.drawable.ic_gemini),
        Triple("deepseek", "DeepSeek", DesignR.drawable.ic_deepseek),
        Triple("openai", "ChatGPT", DesignR.drawable.ic_openai),
        Triple("claude", "Claude", DesignR.drawable.ic_claude),
        Triple("doubao", "Doubao", DesignR.drawable.ic_doubao),
        Triple("mimo", "Mimo", DesignR.drawable.ic_mimo),
        Triple("custom", "自定义平台 (OpenAI 格式)", Icons.Rounded.Cable)
    )

    var expanded by remember { mutableStateOf(false) }
    val currentPlatform = platforms.find { it.first == selectedPlatform } ?: Triple("custom", "自定义平台 (OpenAI 格式)", Icons.Rounded.Cable)

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val inputBgColor = if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else Color.White

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            readOnly = true,
            value = currentPlatform.second,
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.25f else 0.35f),
                focusedContainerColor = inputBgColor,
                unfocusedContainerColor = inputBgColor,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            leadingIcon = {
                val icon = currentPlatform.third
                if (icon is Int) {
                    Icon(
                        painter = painterResource(id = icon),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color.Unspecified
                    )
                } else if (icon is androidx.compose.ui.graphics.vector.ImageVector) {
                    val color = when (currentPlatform.first) {
                        "claude" -> Color(0xFFE05C2B)
                        "doubao" -> Color(0xFF00B4D8)
                        "mimo" -> Color(0xFFFF6700)
                        else -> MaterialTheme.colorScheme.primary
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = color
                    )
                }
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            }
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else Color.White)
        ) {
            platforms.forEach { (id, name, icon) ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (icon is Int) {
                                Icon(
                                    painter = painterResource(id = icon),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = Color.Unspecified
                                )
                            } else if (icon is androidx.compose.ui.graphics.vector.ImageVector) {
                                val color = when (id) {
                                    "claude" -> Color(0xFFE05C2B)
                                    "doubao" -> Color(0xFF00B4D8)
                                    "mimo" -> Color(0xFFFF6700)
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = color
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(name, style = MaterialTheme.typography.bodyLarge)
                        }
                    },
                    onClick = {
                        onPlatformSelected(id)
                        expanded = false
                    },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecommendedModelChips(
    platform: String,
    currentModel: String,
    onModelSelected: (String) -> Unit
) {
    val suggestedModels = when (platform) {
        "gemini" -> listOf("gemini-3.1-pro-preview", "gemini-3-flash-preview", "gemini-3.1-flash-lite-preview")
        "deepseek" -> listOf("deepseek-v4-pro", "deepseek-v4-flash")
        "openai" -> listOf("gpt-5.5", "gpt-5.5-instant", "gpt-5.5-cyber")
        "claude" -> listOf("claude-4.7-opus", "claude-4.6-sonnet", "claude-4.5-haiku")
        "doubao" -> listOf("doubao-pro-256k", "doubao-lite-256k")
        "mimo" -> listOf("mimo-v2.5-pro", "mimo-v2.5", "mimo-v2-pro", "mimo-v2-flash")
        else -> emptyList()
    }

    val platformColor = when (platform) {
        "gemini" -> Color(0xFF6E56CF)
        "deepseek" -> Color(0xFF1E88E5)
        "openai" -> Color(0xFF10A37F)
        "claude" -> Color(0xFFE05C2B)
        "doubao" -> Color(0xFF00B4D8)
        "mimo" -> Color(0xFFFF6700)
        else -> MaterialTheme.colorScheme.primary
    }

    val haptic = LocalHapticFeedback.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    if (suggestedModels.isNotEmpty()) {
        Column(modifier = Modifier.padding(top = 10.dp)) {
            Text(
                text = "推荐预设：",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                suggestedModels.forEach { model ->
                    val isSelected = currentModel == model
                    
                    val chipBgColor = if (isSelected) {
                        platformColor.copy(alpha = 0.15f)
                    } else {
                        if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else Color.White
                    }
                    
                    val chipBorderColor = if (isSelected) {
                        platformColor
                    } else {
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.2f else 0.3f)
                    }
                    
                    val chipTextColor = if (isSelected) {
                        platformColor
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(chipBgColor)
                            .border(
                                width = if (isSelected) 1.2.dp else 1.dp,
                                color = chipBorderColor,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onModelSelected(model)
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = model,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = chipTextColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FormBottomActionBar(
    isTesting: Boolean,
    canTest: Boolean,
    canSave: Boolean,
    onTestClick: () -> Unit,
    onSaveClick: () -> Unit,
    isDark: Boolean
) {
    val haptic = LocalHapticFeedback.current
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isDark) MaterialTheme.colorScheme.surface else Color.White)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 次按钮：验证连接 (占 38% 宽度)
        OutlinedButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onTestClick()
            },
            enabled = canTest && !isTesting,
            modifier = Modifier
                .weight(0.38f)
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(
                width = 1.dp,
                color = if (canTest && !isTesting) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.15f else 0.3f)
                }
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.4f) else NemoNeutrals.Gray50,
                contentColor = MaterialTheme.colorScheme.primary,
                disabledContainerColor = Color.Transparent,
                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            ),
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            if (isTesting) {
                NemoChasingDotsLoader(size = 16.dp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "测试中...", 
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Bolt,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "验证连接", 
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // 主按钮：保存配置 (占 62% 宽度)
        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onSaveClick()
            },
            enabled = canSave && !isTesting,
            modifier = Modifier
                .weight(0.62f)
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp
            )
        ) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "保存配置", 
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun AISettingTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isPasswordField: Boolean = false
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val inputBgColor = if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else Color.White
    var passwordVisible by remember { mutableStateOf(false) }
    
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { 
                Text(
                    text = placeholder, 
                    style = MaterialTheme.typography.bodyMedium, 
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                ) 
            },
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.25f else 0.35f),
                focusedContainerColor = inputBgColor,
                unfocusedContainerColor = inputBgColor,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            singleLine = true,
            visualTransformation = if (isPasswordField && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            trailingIcon = if (isPasswordField) {
                {
                    val image = if (passwordVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = image,
                            contentDescription = if (passwordVisible) "隐藏密钥" else "显示密钥",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else null
        )
    }
}

/**
 * 与复学清单/错题本保持一致的高级空状态设计
 */
@Composable
private fun EmptyAIConfigView(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                modifier = Modifier.size(100.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "暂无 AI 模型配置",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "点击右上角「+」添加您的第一个 AI 模型配置",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 2,
                modifier = Modifier.padding(horizontal = 32.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

