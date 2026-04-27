package com.jian.nemo.feature.test.domain.timer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 计时器状态
 */
data class TimerState(
    val timeLimitSeconds: Int = 0,
    val timeRemainingSeconds: Int = 0,
    val isRunning: Boolean = false
)

/**
 * 测试计时器管理器
 *
 * 职责：管理测试的倒计时功能
 * 提取自：TestViewModel.kt 行1170-1197
 */
@Singleton
class TestTimerManager @Inject constructor() {

    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private var timerJob: Job? = null

    /**
     * 开始计时
     *
     * @param scope 协程作用域
     * @param seconds 总秒数
     * @param onTimeUp 时间到回调
     */
    fun start(
        scope: CoroutineScope,
        seconds: Int,
        onTimeUp: () -> Unit
    ) {
        stop()

        _timerState.value = TimerState(
            timeLimitSeconds = seconds,
            timeRemainingSeconds = seconds,
            isRunning = true
        )

        timerJob = scope.launch {
            while (_timerState.value.timeRemainingSeconds > 0) {
                delay(1000)
                _timerState.value = _timerState.value.copy(
                    timeRemainingSeconds = _timerState.value.timeRemainingSeconds - 1
                )
            }
            // 时间到
            _timerState.value = _timerState.value.copy(isRunning = false)
            onTimeUp()
        }
    }

    /**
     * 暂停计时
     */
    fun pause() {
        stop()
    }

    /**
     * 停止计时
     */
    fun stop() {
        timerJob?.cancel()
        timerJob = null
        _timerState.value = _timerState.value.copy(isRunning = false)
    }

    /**
     * 恢复计时
     *
     * @param scope 协程作用域
     * @param onTimeUp 时间到回调
     */
    fun resume(
        scope: CoroutineScope,
        onTimeUp: () -> Unit
    ) {
        if (_timerState.value.isRunning || _timerState.value.timeRemainingSeconds <= 0) return

        _timerState.value = _timerState.value.copy(isRunning = true)

        timerJob = scope.launch {
            while (_timerState.value.timeRemainingSeconds > 0) {
                delay(1000)
                _timerState.value = _timerState.value.copy(
                    timeRemainingSeconds = _timerState.value.timeRemainingSeconds - 1
                )
            }
            // 时间到
            _timerState.value = _timerState.value.copy(isRunning = false)
            onTimeUp()
        }
    }

    /**
     * 重置计时器
     */
    fun reset() {
        stop()
        _timerState.value = TimerState()
    }

    /**
     * 获取当前剩余时间
     */
    fun getRemainingSeconds(): Int = _timerState.value.timeRemainingSeconds

    /**
     * 检查是否正在运行
     */
    fun isRunning(): Boolean = _timerState.value.isRunning
}
