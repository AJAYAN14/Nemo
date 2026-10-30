package com.jian.nemo.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jian.nemo.core.domain.model.AppUpdateConfig
import com.jian.nemo.core.domain.repository.ConfigRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 关于界面 UI 状态
 */
data class AboutUiState(
    val isLoadingLog: Boolean = true,
    val updateConfig: AppUpdateConfig? = null,
    val errorMessage: String? = null
)

/**
 * 关于界面 ViewModel
 *
 * 负责从 Supabase 后端 (ConfigRepository) 异步获取真实的应用版本更新配置与更新日志，
 * 绝不使用客户端硬编码数据。
 */
@HiltViewModel
class AboutViewModel @Inject constructor(
    private val configRepository: ConfigRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AboutUiState())
    val uiState: StateFlow<AboutUiState> = _uiState.asStateFlow()

    init {
        fetchUpdateLog()
    }

    fun fetchUpdateLog() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingLog = true, errorMessage = null) }
            try {
                val config = configRepository.getUpdateConfig()
                _uiState.update {
                    it.copy(
                        isLoadingLog = false,
                        updateConfig = config,
                        errorMessage = if (config == null) "暂无云端更新日志" else null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingLog = false,
                        errorMessage = e.message ?: "获取更新日志失败"
                    )
                }
            }
        }
    }
}
