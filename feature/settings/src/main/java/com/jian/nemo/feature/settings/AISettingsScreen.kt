@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
package com.jian.nemo.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jian.nemo.core.designsystem.theme.*
import com.jian.nemo.core.ui.component.common.CommonHeader
import com.jian.nemo.feature.settings.components.PremiumCard
import com.jian.nemo.feature.settings.components.SettingsSectionTitle
import com.jian.nemo.core.domain.model.AIExercise
import androidx.compose.ui.res.painterResource
import com.jian.nemo.core.designsystem.R as DesignR
import kotlinx.serialization.json.Json

@Composable
fun AISettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AISettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val navigationBarHeight = with(LocalDensity.current) { WindowInsets.navigationBars.getBottom(this).toDp() }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                CommonHeader(
                    title = "AI 配置中心",
                    onBack = onNavigateBack
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp)
        ) {
            // 1. 平台选择区
            item {
                SettingsSectionTitle(text = "选择 AI 服务平台")
                Spacer(modifier = Modifier.height(8.dp))
                
                PlatformSelectionGrid(
                    selectedPlatform = uiState.platform,
                    onPlatformSelected = { viewModel.onEvent(AISettingsEvent.SetPlatform(it)) }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 2. 配置详情区
            item {
                SettingsSectionTitle(text = "接口配置详情")
                Spacer(modifier = Modifier.height(8.dp))
                
                PremiumCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // API Key
                        AISettingTextField(
                            label = "API Key",
                            value = uiState.apiKey,
                            onValueChange = { viewModel.onEvent(AISettingsEvent.SetApiKey(it)) },
                            placeholder = "输入认证令牌",
                            icon = Icons.Rounded.VpnKey
                        )
                        
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.2f else 0.5f)
                        )
                        
                        // Base URL
                        AISettingTextField(
                            label = "代理地址 (可选)",
                            value = uiState.baseUrl,
                            onValueChange = { viewModel.onEvent(AISettingsEvent.SetBaseUrl(it)) },
                            placeholder = "例如: https://api.openai-proxy.com",
                            icon = Icons.Rounded.Dns
                        )
                        
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.2f else 0.5f)
                        )

                        // Model
                        AISettingTextField(
                            label = "模型标识码",
                            value = uiState.model,
                            onValueChange = { viewModel.onEvent(AISettingsEvent.SetModel(it)) },
                            placeholder = "例如: gemini-3-flash-preview",
                            icon = Icons.Rounded.Psychology
                        )
                        
                        // 推荐模型 Chips
                        RecommendedModelChips(
                            platform = uiState.platform,
                            currentModel = uiState.model,
                            onModelSelected = { viewModel.onEvent(AISettingsEvent.SetModel(it)) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 3. 测试与验证
            item {
                SettingsSectionTitle(text = "测试与验证")
                Spacer(modifier = Modifier.height(8.dp))
                
                TestConnectionSection(
                    isTesting = uiState.isTesting,
                    testResult = uiState.testResult,
                    canTest = uiState.apiKey.isNotBlank(),
                    onTestClick = { viewModel.onEvent(AISettingsEvent.TestConnection) }
                )
                Spacer(modifier = Modifier.height(navigationBarHeight + 32.dp))
            }
        }
    }
}

@Composable
fun PlatformSelectionGrid(
    selectedPlatform: String,
    onPlatformSelected: (String) -> Unit
) {
    val platforms = listOf(
        Triple("gemini", "Gemini", DesignR.drawable.ic_gemini),
        Triple("deepseek", "DeepSeek", DesignR.drawable.ic_deepseek),
        Triple("openai", "OpenAI", DesignR.drawable.ic_openai),
        Triple("custom", "自定义", Icons.Rounded.Cable)
    )

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val unselectedColor = if (isDark) MaterialTheme.colorScheme.surfaceContainer else Color.White

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        platforms.forEach { (id, name, icon) ->
            val isSelected = selectedPlatform == id
            Surface(
                onClick = { onPlatformSelected(id) },
                modifier = Modifier.fillMaxWidth(0.48f),
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else unselectedColor,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.15f else 0.2f)
                ),
                shadowElevation = if (isDark) 0.dp else 2.dp,
                tonalElevation = 0.dp // 遵循规范：深色模式靠颜色表现深度，不靠阴影/色调提升
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (icon is Int) {
                        Icon(
                            painter = painterResource(id = icon),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Color.Unspecified
                        )
                    } else if (icon is androidx.compose.ui.graphics.vector.ImageVector) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = name,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
        "gemini" -> listOf("gemini-3-flash-preview", "gemini-3.1-pro-preview", "gemini-3.1-flash-lite-preview")
        "deepseek" -> listOf("deepseek-v4-pro", "deepseek-v4-flash")
        "openai" -> listOf("gpt-5.5", "gpt-5.4-pro", "gpt-5.4-thinking")
        else -> emptyList()
    }

    if (suggestedModels.isNotEmpty()) {
        Column(modifier = Modifier.padding(top = 16.dp)) {
            Text(
                text = "常用推荐",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                suggestedModels.forEach { model ->
                    val isSelected = currentModel == model
                    FilterChip(
                        selected = isSelected,
                        onClick = { onModelSelected(model) },
                        label = { Text(model, style = MaterialTheme.typography.bodySmall) },
                        shape = RoundedCornerShape(8.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
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
    Column {
        Button(
            onClick = onTestClick,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            enabled = canTest && !isTesting,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            if (isTesting) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                Spacer(modifier = Modifier.width(12.dp))
                Text("正在验证连接...")
            } else {
                Icon(Icons.Rounded.Bolt, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("测试连接并保存", fontWeight = FontWeight.Bold)
            }
        }

        testResult?.let { result ->
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = (if (result.success) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error).copy(alpha = 0.08f),
                border = BorderStroke(1.dp, (if (result.success) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error).copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (result.success) Icons.Rounded.TaskAlt else Icons.Rounded.ErrorOutline,
                        contentDescription = null,
                        tint = if (result.success) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (result.success) "API 连接正常，配置已生效" else "配置验证失败：${result.message}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (result.success) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
                    )
                }
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
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
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
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.2f else 0.3f),
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f), // 使用 surfaceContainer 替代 surfaceVariant
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f),
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            singleLine = true
        )
    }
}
