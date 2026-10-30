package com.jian.nemo.feature.learning.presentation.delegate

import com.jian.nemo.feature.learning.presentation.LearningUiState

/**
 * 统一状态更新接口 (系统设计 §3.2.M9)
 *
 * 全部 delegate 通过此接口原子更新 _uiState，
 * 禁止直接持有 MutableStateFlow 引用，保证状态更新入口唯一、可追踪。
 * 并发安全由 StateFlow.update 的 CAS 循环保证。
 */
interface StateUpdater {
    /** 原子更新 _uiState */
    fun update(transform: (LearningUiState) -> LearningUiState)

    /** 获取当前状态快照 */
    val currentState: LearningUiState
}
