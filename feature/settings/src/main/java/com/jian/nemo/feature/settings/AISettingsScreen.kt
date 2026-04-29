package com.jian.nemo.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.jian.nemo.core.designsystem.theme.*
import com.jian.nemo.core.ui.component.common.CommonHeader
import com.jian.nemo.feature.settings.components.PremiumCard
import com.jian.nemo.feature.settings.components.SettingsSectionTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AISettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AISettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val density = LocalDensity.current
    val statusBarHeight = with(density) { WindowInsets.statusBars.getTop(density).toDp() }
    val navigationBarHeight = with(density) { WindowInsets.navigationBars.getBottom(density).toDp() }

    Scaffold(
        topBar = {
            CommonHeader(
                title = "AI 工坊配置",
                onBack = onNavigateBack
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = navigationBarHeight + 16.dp
            )
        ) {
            item {
                SettingsSectionTitle("服务商设置")
                PremiumCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "选择 AI 平台",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        // 平台选择
                        val platforms = listOf(
                            "openai" to "OpenAI / ChatGPT",
                            "gemini" to "Google Gemini",
                            "claude" to "Anthropic Claude",
                            "deepseek" to "DeepSeek",
                            "custom" to "自定义 (OpenAI 兼容)"
                        )
                        
                        platforms.forEach { (id, name) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = uiState.platform == id,
                                    onClick = { viewModel.onEvent(AISettingsEvent.SetPlatform(id)) }
                                )
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                SettingsSectionTitle("API 凭据")
                PremiumCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        AISettingTextField(
                            label = "API Key",
                            value = uiState.apiKey,
                            onValueChange = { viewModel.onEvent(AISettingsEvent.SetApiKey(it)) },
                            placeholder = "输入您的 API Key",
                            icon = Icons.Rounded.VpnKey
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        AISettingTextField(
                            label = "Base URL (可选)",
                            value = uiState.baseUrl,
                            onValueChange = { viewModel.onEvent(AISettingsEvent.SetBaseUrl(it)) },
                            placeholder = "例如: https://api.openai.com/v1",
                            icon = Icons.Rounded.Link
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        AISettingTextField(
                            label = "模型名称",
                            value = uiState.model,
                            onValueChange = { viewModel.onEvent(AISettingsEvent.SetModel(it)) },
                            placeholder = "例如: gpt-3.5-turbo",
                            icon = Icons.Rounded.SmartToy
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                SettingsSectionTitle("练习偏好")
                PremiumCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "默认难度等级",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        val difficulties = listOf("N5", "N4", "N3", "N2", "N1")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            difficulties.forEach { level ->
                                val isSelected = uiState.difficulty == level
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.onEvent(AISettingsEvent.SetDifficulty(level)) },
                                    label = { Text(level) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        selectedLabelColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
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
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder, fontSize = 14.sp) },
            leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp)) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            ),
            singleLine = true
        )
    }
}
