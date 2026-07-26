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
import com.jian.nemo.core.ui.component.common.NemoSnackbar
import com.jian.nemo.core.ui.component.common.NemoSnackbarType
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.jian.nemo.feature.settings.components.PremiumCard
import com.jian.nemo.feature.settings.components.SettingsSectionTitle
import androidx.compose.ui.res.painterResource
import com.jian.nemo.core.designsystem.R as DesignR
import com.jian.nemo.core.domain.repository.AIConfig

@Composable
fun AISettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AISettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val navigationBarHeight = with(LocalDensity.current) { WindowInsets.navigationBars.getBottom(this).toDp() }
    val haptic = LocalHapticFeedback.current
    val statusBarHeight = with(LocalDensity.current) { WindowInsets.statusBars.getTop(this).toDp() }

    var configToDelete by remember { mutableStateOf<AIConfig?>(null) }

    LaunchedEffect(uiState.testResult) {
        if (uiState.testResult != null) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                    CommonHeader(
                        title = "AI 模型配置",
                        onBack = onNavigateBack,
                        actions = {
                            IconButton(
                                onClick = { viewModel.onEvent(AISettingsEvent.OpenEditModal(null)) }
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
                }
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
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(bottom = navigationBarHeight + 32.dp)
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

                    if (uiState.configs.isEmpty()) {
                        item {
                            PremiumCard(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp, vertical = 40.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // 优雅的 AI 平台微标联合行展示
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy((-8).dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val brands = listOf(
                                            DesignR.drawable.ic_gemini to Color(0xFF6E56CF),
                                            DesignR.drawable.ic_deepseek to Color(0xFF1E88E5),
                                            DesignR.drawable.ic_openai to Color(0xFF10A37F)
                                        )
                                        brands.forEach { (painterId, color) ->
                                            Box(
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .border(2.dp, MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
                                                    .background(color.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    painter = painterResource(id = painterId),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(20.dp),
                                                    tint = color
                                                )
                                            }
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(24.dp))
                                    
                                    Text(
                                        text = "暂无任何 AI 配置",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "点击右上角 + 号新建您的首套模型密钥，支持 Gemini、DeepSeek、OpenAI 等主流大模型。",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
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
        // 1. 全屏半透明遮罩
        AnimatedVisibility(
            visible = uiState.editingConfig != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
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
                        .fillMaxHeight(0.85f)
                        .imePadding()
                        .softCardShadow(borderRadius = 24.dp, isDark = isDark),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
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
                        // 弹窗 Header
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            IconButton(
                                onClick = { viewModel.onEvent(AISettingsEvent.CloseEditModal) },
                                modifier = Modifier.align(Alignment.CenterStart),
                                enabled = !uiState.isTesting
                            ) {
                                Icon(Icons.Rounded.Close, contentDescription = "取消")
                            }
                            
                            Text(
                                text = if (isNew) "新建 AI 配置" else "编辑 AI 配置",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.align(Alignment.Center)
                            )
                            
                            TextButton(
                                onClick = { viewModel.onEvent(AISettingsEvent.SaveConfig) },
                                modifier = Modifier.align(Alignment.CenterEnd),
                                enabled = !uiState.isTesting && editingConfig.apiKey.isNotBlank()
                            ) {
                                Text("保存", fontWeight = FontWeight.Bold)
                            }
                        }

                        HorizontalDivider(
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.12f else 0.2f)
                        )

                        // 弹窗表单滚动主体
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            // 1. 配置别名
                            AISettingTextField(
                                label = "配置别名",
                                value = editingConfig.name,
                                onValueChange = { viewModel.onEvent(AISettingsEvent.UpdateEditingConfig(editingConfig.copy(name = it))) },
                                placeholder = "例如: 我的工作号 / 主用 Gemini",
                                icon = Icons.Rounded.Edit
                            )

                            // 2. 选择服务平台
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.Hub,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "AI 服务平台",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                PlatformDropdownSelector(
                                    selectedPlatform = editingConfig.platform,
                                    onPlatformSelected = { 
                                        // 切换平台时同时清空并适配默认模型
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

                            // 3. 模型标识码
                            Column {
                                AISettingTextField(
                                    label = "模型标识码",
                                    value = editingConfig.model,
                                    onValueChange = { viewModel.onEvent(AISettingsEvent.UpdateEditingConfig(editingConfig.copy(model = it))) },
                                    placeholder = "例如: gemini-3.1-pro-preview",
                                    icon = Icons.Rounded.Psychology
                                )
                                RecommendedModelChips(
                                    platform = editingConfig.platform,
                                    currentModel = editingConfig.model,
                                    onModelSelected = { viewModel.onEvent(AISettingsEvent.UpdateEditingConfig(editingConfig.copy(model = it))) }
                                )
                            }

                            // 4. API 密钥
                            AISettingTextField(
                                label = "API Key 密钥",
                                value = editingConfig.apiKey,
                                onValueChange = { viewModel.onEvent(AISettingsEvent.UpdateEditingConfig(editingConfig.copy(apiKey = it))) },
                                placeholder = "输入 API 令牌密钥",
                                icon = Icons.Rounded.VpnKey,
                                isPasswordField = true
                            )

                            // 5. 代理地址 (可选)
                            AISettingTextField(
                                label = "自定义代理地址 (可选)",
                                value = editingConfig.baseUrl,
                                onValueChange = { viewModel.onEvent(AISettingsEvent.UpdateEditingConfig(editingConfig.copy(baseUrl = it))) },
                                placeholder = "例如: https://api.openai-proxy.com",
                                icon = Icons.Rounded.Dns
                            )

                            // 6. 验证连接并保存操作区
                            TestConnectionSection(
                                isTesting = uiState.isTesting,
                                testResult = uiState.testResult,
                                canTest = editingConfig.apiKey.isNotBlank(),
                                onTestClick = { viewModel.onEvent(AISettingsEvent.TestConnection) }
                            )


                        }
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
            AlertDialog(
                onDismissRequest = { configToDelete = null },
                title = {
                    Text(
                        text = "确认删除此配置？",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                text = {
                    Text(
                        text = "您确认要删除 AI 配置 \"${config.name}\" 吗？删除后该配置的密钥和代理信息将永久丢失，且无法恢复。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.onEvent(AISettingsEvent.DeleteConfig(config.id))
                            configToDelete = null
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("确认删除", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { configToDelete = null }
                    ) {
                        Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                shape = RoundedCornerShape(24.dp),
                containerColor = if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else Color.White,
                properties = androidx.compose.ui.window.DialogProperties(
                    usePlatformDefaultWidth = true
                )
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
    val inputBgColor = if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else NemoNeutrals.Gray50

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
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.2f else 0.3f),
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
        Column(modifier = Modifier.padding(top = 16.dp)) {
            Text(
                text = "推荐模型",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                suggestedModels.forEach { model ->
                    val isSelected = currentModel == model
                    
                    val chipBgColor = if (isSelected) {
                        platformColor.copy(alpha = 0.12f)
                    } else {
                        if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else NemoNeutrals.Gray50
                    }
                    
                    val chipBorderColor = if (isSelected) {
                        platformColor
                    } else {
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.15f else 0.2f)
                    }
                    
                    val chipTextColor = if (isSelected) {
                        platformColor
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(chipBgColor)
                            .border(
                                width = if (isSelected) 1.2.dp else 1.dp,
                                color = chipBorderColor,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onModelSelected(model)
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = model,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
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
fun TestConnectionSection(
    isTesting: Boolean,
    testResult: AITestResult?,
    canTest: Boolean,
    onTestClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        label = "scale",
        animationSpec = tween(200)
    )

    Column(modifier = Modifier.padding(top = 8.dp)) {
        Button(
            onClick = onTestClick,
            interactionSource = interactionSource,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
            enabled = canTest && !isTesting,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 2.dp,
                pressedElevation = 0.dp
            )
        ) {
            if (isTesting) {
                NemoChasingDotsLoader(size = 20.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Text("正在验证连接...", fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Rounded.Bolt, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("验证网络连接", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AISettingTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isPasswordField: Boolean = false
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val inputBgColor = if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else NemoNeutrals.Gray50
    var passwordVisible by remember { mutableStateOf(false) }
    
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline) },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.2f else 0.3f),
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
                        Icon(image, contentDescription = if (passwordVisible) "隐藏密钥" else "显示密钥")
                    }
                }
            } else null
        )
    }
}
