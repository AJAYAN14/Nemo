package com.jian.nemo.feature.library.presentation.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * 专项分类界面 ViewModel
 * 提供设置项中的图标风格流动观察 (SettingIconStyle)
 */
@HiltViewModel
class CategoryClassificationViewModel @Inject constructor(
    settingsRepository: SettingsRepository
) : ViewModel() {
    val settingIconStyle: StateFlow<SettingIconStyle> = settingsRepository.settingIconStyleFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SettingIconStyle.PLUMP_DUO
        )
}
