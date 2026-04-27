package com.jian.nemo.feature.learning.presentation

/**
 * 学习界面 UI 事件
 * 用户所有交互都封装为事件
 */
sealed interface LearningEvent {
    /**
     * 开始学习会话
     * @param level N1-N5等级
     * @param mode 学习模式（单词/语法），默认为单词
     */
    data class StartLearning(
        val level: String,
        val mode: LearningMode = LearningMode.Word
    ) : LearningEvent

    /**
     * 切换学习模式（单词↔️语法）
     */
    data class ChangeLearningMode(
        val mode: LearningMode
    ) : LearningEvent

    /**
     * 切换学习等级
     */
    data class ChangeLevel(
        val level: String
    ) : LearningEvent

    /**
     * 翻转卡片（显示/隐藏答案）
     */
    data object FlipCard : LearningEvent

    /**
     * 显示答案
     */
    data object ShowAnswer : LearningEvent

    /**
     * 单词评分 (0-5)
     */
    data class RateWord(val quality: Int) : LearningEvent

    /**
     * 语法评分 (0-5)
     */
    data class RateGrammar(val quality: Int) : LearningEvent

    /**
     * 导航到下一个
     */
    data object NavigateNext : LearningEvent

    /**
     * 导航到上一个
     */
    data object NavigatePrev : LearningEvent

    /**
     * 跳转到指定索引（用于手势滑动同步）
     */
    data class GoToIndex(val index: Int) : LearningEvent



    /**
     * 显示打字练习
     */
    data object ShowTypingPractice : LearningEvent

    /**
     * 隐藏打字练习
     */
    data object HideTypingPractice : LearningEvent

    /**
     * 重试（错误后）
     */
    data object Retry : LearningEvent

    /**
     * 播放发音
     */
    data class SpeakWord(val text: String) : LearningEvent

    /**
     * 朗读例句（日语 + 中文翻译）
     * @param id 唯一标识符，用于控制声纹动画
     */
    data class SpeakExample(val japanese: String, val chinese: String, val id: String) : LearningEvent

    /**
     * 撤销上一次评分
     */
    data object Undo : LearningEvent

    /**
     * 关闭撤销提示（自动消失或手动关闭）
     */
    data object DismissUndo : LearningEvent

    /**
     * 从等待状态恢复学习（Learn Ahead Limit 到期或用户点击继续）
     */
    data object ResumeFromWaiting : LearningEvent

    data object SuspendCurrent : LearningEvent

    /**
     * 今日暂缓此项 (Bury)
     */
    data object BuryCurrent : LearningEvent

    /**
     * 退出学习
     */
    data object ExitLearning : LearningEvent

    // ========== 语法学习专用事件 ==========

    /**
     * 切换语法详情可见性
     */
    data object ToggleGrammarDetail : LearningEvent

    /**
     * 加载上一个语法
     */
    data object LoadPreviousGrammar : LearningEvent

    /**
     * 加载下一个语法
     */
    data object LoadNextGrammar : LearningEvent

    /**
     * 掌握当前语法
     */
    data object MasterGrammar : LearningEvent

    /**
     * 切换自动朗读 (翻面自动播放音频)
     */
    data class ToggleAutoPlayAudio(val enabled: Boolean) : LearningEvent

    /**
     * 切换显示答案等待（防秒翻）
     */
    data class ToggleShowAnswerDelay(val enabled: Boolean) : LearningEvent

    /**
     * 切换显示答案等待时长（循环档位）
     */
    data object CycleShowAnswerDelayDuration : LearningEvent

    /**
     * 报告当前条目内容错误
     */
    data object ReportContentError : LearningEvent

    /**
     * 弹出报告错误的确认对话框
     */
    data object OpenReportErrorDialog : LearningEvent

    /**
     * 取消报告错误对话框
     */
    data object CancelReportErrorDialog : LearningEvent

    /**
     * 清理成功信息提示
     */
    data object ClearSuccessMessage : LearningEvent

    /**
     * 清理错误信息提示
     */
    data object ClearErrorMessage : LearningEvent

    /**
     * 设置显示模式
     * @param enabled true=深色, false=浅色, null=跟随系统
     */
    data class SetDarkMode(val enabled: Boolean?) : LearningEvent
    /**
     * 循环切换显示模式
     */
    data object CycleDarkMode : LearningEvent
}
