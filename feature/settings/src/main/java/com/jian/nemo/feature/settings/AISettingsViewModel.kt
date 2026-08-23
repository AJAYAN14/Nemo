package com.jian.nemo.feature.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jian.nemo.core.domain.repository.SettingsRepository
import com.jian.nemo.core.domain.repository.AIConfig
import com.jian.nemo.core.data.util.AIClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

data class AITestResult(
    val success: Boolean,
    val message: String
)

data class AISettingsUiState(
    val configs: List<AIConfig> = emptyList(),
    val activeConfigId: String = "",
    val editingConfig: AIConfig? = null, // null 表示弹窗关闭，非 null 表示弹窗开启
    val difficulty: String = "N5",
    val isLoading: Boolean = true,
    val isTesting: Boolean = false,
    val testResult: AITestResult? = null
)

sealed interface AISettingsEvent {
    data class SelectActiveConfig(val id: String) : AISettingsEvent
    data class OpenEditModal(val id: String?) : AISettingsEvent // null = 新建
    object CloseEditModal : AISettingsEvent
    data class UpdateEditingConfig(val config: AIConfig) : AISettingsEvent
    object SaveConfig : AISettingsEvent
    data class DeleteConfig(val id: String? = null) : AISettingsEvent
    object TestConnection : AISettingsEvent
    object ClearTestResult : AISettingsEvent
    data class SetDifficulty(val difficulty: String) : AISettingsEvent
}

@HiltViewModel
class AISettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val aiClient: AIClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(AISettingsUiState())
    val uiState: StateFlow<AISettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            combine(
                settingsRepository.aiConfigListFlow,
                settingsRepository.aiActiveConfigIdFlow,
                settingsRepository.aiWorkshopDifficultyFlow
            ) { listJson, activeId, difficulty ->
                var configs = parseAiConfigs(listJson)
                var currentActiveId = activeId
                
                // 兼容性清理与迁移：如果仅有之前自动生成的空默认配置，进行清理
                if (configs.size == 1 && configs[0].id == "default_active_id" && configs[0].apiKey.isBlank()) {
                    configs = emptyList()
                    currentActiveId = ""
                    settingsRepository.setAiConfigList("")
                    settingsRepository.setAiActiveConfigId("")
                }

                // 兼容性迁移：如果列表为空，且旧版存储中存在有效的 API Key 时才创建初始配置
                if (configs.isEmpty()) {
                    val oldKey = settingsRepository.aiApiKeyFlow.first()
                    if (oldKey.isNotBlank()) {
                        val oldPlatform = settingsRepository.aiPlatformFlow.first()
                        val oldUrl = settingsRepository.aiBaseUrlFlow.first()
                        val oldModel = settingsRepository.aiModelFlow.first()
                        
                        val initialConfig = AIConfig(
                            id = "default_active_id",
                            name = when(oldPlatform) {
                                "openai" -> "OpenAI 默认配置"
                                "gemini" -> "Gemini 默认配置"
                                "deepseek" -> "DeepSeek 默认配置"
                                else -> "自定义 默认配置"
                            },
                            platform = oldPlatform.ifEmpty { "openai" },
                            model = oldModel,
                            apiKey = oldKey,
                            baseUrl = oldUrl
                        )
                        configs = listOf(initialConfig)
                        currentActiveId = initialConfig.id
                        
                        // 保存以触发同步
                        settingsRepository.setAiConfigList(Json.encodeToString(configs))
                        settingsRepository.setAiActiveConfigId(currentActiveId)
                    }
                }
                
                Triple(configs, currentActiveId, difficulty)
            }.collect { (configs, activeId, difficulty) ->
                _uiState.update { 
                    it.copy(
                        configs = configs,
                        activeConfigId = activeId,
                        difficulty = difficulty,
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun parseAiConfigs(json: String?): List<AIConfig> {
        if (json.isNullOrEmpty()) return emptyList()
        return try {
            Json.decodeFromString<List<AIConfig>>(json)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun onEvent(event: AISettingsEvent) {
        viewModelScope.launch {
            when (event) {
                is AISettingsEvent.SelectActiveConfig -> {
                    settingsRepository.setAiActiveConfigId(event.id)
                }
                is AISettingsEvent.OpenEditModal -> {
                    if (event.id == null) {
                        // 新建配置
                        val newConfig = AIConfig(
                            id = System.currentTimeMillis().toString(),
                            name = "",
                            platform = "openai",
                            model = "",
                            apiKey = "",
                            baseUrl = ""
                        )
                        _uiState.update { it.copy(editingConfig = newConfig, testResult = null) }
                    } else {
                        // 编辑配置
                        val config = _uiState.value.configs.find { it.id == event.id }
                        _uiState.update { it.copy(editingConfig = config, testResult = null) }
                    }
                }
                is AISettingsEvent.CloseEditModal -> {
                    _uiState.update { it.copy(editingConfig = null, testResult = null) }
                }
                is AISettingsEvent.UpdateEditingConfig -> {
                    _uiState.update { it.copy(editingConfig = event.config) }
                }
                is AISettingsEvent.SaveConfig -> {
                    val editing = _uiState.value.editingConfig ?: return@launch
                    val currentList = _uiState.value.configs.toMutableList()
                    val index = currentList.indexOfFirst { it.id == editing.id }
                    
                    val finalName = editing.name.trim().ifEmpty { "未命名配置" }
                    val finalConfig = editing.copy(name = finalName, apiKey = editing.apiKey.trim())
                    
                    // 别名与密钥重复性校验
                    val hasDuplicateName = currentList.any { 
                        it.id != finalConfig.id && it.name.trim().equals(finalConfig.name.trim(), ignoreCase = true) 
                    }
                    if (hasDuplicateName) {
                        _uiState.update { it.copy(testResult = AITestResult(false, "配置别名已存在，请换一个别名")) }
                        return@launch
                    }
                    val hasDuplicateKey = currentList.any { 
                        it.id != finalConfig.id && it.apiKey.trim() == finalConfig.apiKey.trim() 
                    }
                    if (hasDuplicateKey) {
                        _uiState.update { it.copy(testResult = AITestResult(false, "API 密钥已配置过，请勿重复添加")) }
                        return@launch
                    }
                    
                    if (index >= 0) {
                        currentList[index] = finalConfig
                    } else {
                        currentList.add(0, finalConfig)
                    }
                    
                    settingsRepository.setAiConfigList(Json.encodeToString(currentList))
                    if (currentList.size == 1 || _uiState.value.activeConfigId.isEmpty()) {
                        settingsRepository.setAiActiveConfigId(finalConfig.id)
                    }
                    _uiState.update { it.copy(editingConfig = null, testResult = null) }
                }
                is AISettingsEvent.DeleteConfig -> {
                    val configId = event.id ?: _uiState.value.editingConfig?.id ?: return@launch
                    val currentList = _uiState.value.configs.filter { it.id != configId }
                    
                    settingsRepository.setAiConfigList(Json.encodeToString(currentList))
                    if (_uiState.value.activeConfigId == configId) {
                        val nextActiveId = if (currentList.isNotEmpty()) currentList[0].id else ""
                        settingsRepository.setAiActiveConfigId(nextActiveId)
                    }
                    _uiState.update { it.copy(editingConfig = null, testResult = null) }
                }
                is AISettingsEvent.TestConnection -> {
                    val editing = _uiState.value.editingConfig ?: return@launch
                    if (editing.apiKey.isBlank()) {
                        _uiState.update { it.copy(testResult = AITestResult(false, "API Key 不能为空")) }
                        return@launch
                    }
                    
                    val currentList = _uiState.value.configs
                    val finalName = editing.name.trim().ifEmpty { "未命名配置" }
                    val finalConfig = editing.copy(name = finalName, apiKey = editing.apiKey.trim())
                    
                    // 别名与密钥重复性校验
                    val hasDuplicateName = currentList.any { 
                        it.id != finalConfig.id && it.name.trim().equals(finalConfig.name.trim(), ignoreCase = true) 
                    }
                    if (hasDuplicateName) {
                        _uiState.update { it.copy(testResult = AITestResult(false, "配置别名已存在，请换一个别名")) }
                        return@launch
                    }
                    val hasDuplicateKey = currentList.any { 
                        it.id != finalConfig.id && it.apiKey.trim() == finalConfig.apiKey.trim() 
                    }
                    if (hasDuplicateKey) {
                        _uiState.update { it.copy(testResult = AITestResult(false, "API 密钥已配置过，请勿重复添加")) }
                        return@launch
                    }

                    _uiState.update { it.copy(isTesting = true, testResult = null) }
                    
                    val result = aiClient.generateExercise(
                        platform = editing.platform,
                        apiKey = editing.apiKey,
                        baseUrl = editing.baseUrl.ifBlank { null },
                        model = editing.model,
                        difficulty = "N5"
                    )
                    
                    val testSuccess = result.isSuccess
                    
                    _uiState.update { it.copy(
                        isTesting = false,
                        testResult = if (testSuccess) {
                            AITestResult(true, "连接成功")
                        } else {
                            val msg = result.exceptionOrNull()?.message ?: "未知错误"
                            Log.e("AISettings", "验证失败: $msg")
                            AITestResult(false, msg)
                        }
                    ) }
                    
                    if (testSuccess) {
                        // 自动保存并关闭抽屉弹窗
                        val currentList = _uiState.value.configs.toMutableList()
                        val index = currentList.indexOfFirst { it.id == editing.id }
                        
                        val finalName = editing.name.trim().ifEmpty { "未命名配置" }
                        val finalConfig = editing.copy(name = finalName, apiKey = editing.apiKey.trim())
                        
                        if (index >= 0) {
                            currentList[index] = finalConfig
                        } else {
                            currentList.add(0, finalConfig)
                        }
                        
                        settingsRepository.setAiConfigList(Json.encodeToString(currentList))
                        if (currentList.size == 1 || _uiState.value.activeConfigId.isEmpty()) {
                            settingsRepository.setAiActiveConfigId(finalConfig.id)
                        }
                        _uiState.update { it.copy(editingConfig = null) }
                    }
                }
                is AISettingsEvent.ClearTestResult -> {
                    _uiState.update { it.copy(testResult = null) }
                }
                is AISettingsEvent.SetDifficulty -> {
                    settingsRepository.setAiWorkshopDifficulty(event.difficulty)
                }
            }
        }
    }
}
