package com.jian.nemo.core.domain.usecase.settings

import com.jian.nemo.core.common.Result
import com.jian.nemo.core.domain.repository.WordRepository
import com.jian.nemo.core.domain.repository.GrammarRepository
import com.jian.nemo.core.domain.repository.WrongAnswerRepository
import com.jian.nemo.core.domain.repository.GrammarWrongAnswerRepository
import com.jian.nemo.core.domain.repository.StudyRecordRepository
import com.jian.nemo.core.domain.repository.AuthRepository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 重置所有学习进度UseCase
 * 执行内容：
 * 1. 清除所有单词错题记录
 * 2. 清除所有语法错题记录
 * 3. 清除所有学习记录（StudyRecordEntity）
 * 4. 重置单词学习进度
 * 5. 重置语法学习进度
 *
 * 注意：
 * - 测试记录(TestRecord)通过WordRepository.resetAllProgress()一并清除
 * - 统计数据重置需要在SettingsRepository层面实现
 * - 本UseCase负责清除本地数据库数据
 */
class ResetProgressUseCase @Inject constructor(
    private val wrongAnswerRepository: WrongAnswerRepository,
    private val grammarWrongAnswerRepository: GrammarWrongAnswerRepository,
    private val studyRecordRepository: StudyRecordRepository,
    private val wordRepository: WordRepository,
    private val grammarRepository: GrammarRepository,
    private val authRepository: AuthRepository,
    private val settingsRepository: com.jian.nemo.core.domain.repository.SettingsRepository
) {

    /**
     * 执行重置操作
     *
     * @return Result<Unit> 成功或失败结果
     */
    suspend operator fun invoke(): Result<Unit> = withContext(Dispatchers.IO) {
        try {

            // 1. 清除所有单词错题记录
            wrongAnswerRepository.clearAll()

            // 2. 清除所有语法错题记录
            grammarWrongAnswerRepository.clearAll()

            // 3. 清除所有学习记录
            studyRecordRepository.deleteAll()

            // 4. 重置单词学习进度（同时会清除测试记录）
            wordRepository.resetAllProgress()

            // 5. 重置语法学习进度
            grammarRepository.resetAllProgress()

            // 6. 重置学习统计数据 (Streak, Lapses, Session)
            settingsRepository.resetLearningStats()


            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
