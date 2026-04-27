package com.jian.nemo.core.domain.usecase.settings

import com.jian.nemo.core.common.Result
import com.jian.nemo.core.domain.repository.WordRepository
import com.jian.nemo.core.domain.repository.GrammarRepository
import com.jian.nemo.core.domain.repository.WrongAnswerRepository
import com.jian.nemo.core.domain.repository.GrammarWrongAnswerRepository
import com.jian.nemo.core.domain.repository.StudyRecordRepository
import com.jian.nemo.core.domain.repository.AuthRepository
import com.jian.nemo.core.domain.repository.SyncRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 重置所有学习进度UseCase
 * 同步仓库接口 - 定义云端同步与恢复的相关业务逻辑
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
 * - 云端同步数据删除通过SyncRepository实现
 * - 本UseCase负责清除本地数据库数据和云端同步数据
 */
class ResetProgressUseCase @Inject constructor(
    private val wrongAnswerRepository: WrongAnswerRepository,
    private val grammarWrongAnswerRepository: GrammarWrongAnswerRepository,
    private val studyRecordRepository: StudyRecordRepository,
    private val wordRepository: WordRepository,
    private val grammarRepository: GrammarRepository,
    private val syncRepository: SyncRepository,
    private val authRepository: AuthRepository,
    private val settingsRepository: com.jian.nemo.core.domain.repository.SettingsRepository
) {

    /**
     * 执行重置操作
     *
     * @param deleteCloud 同时物理删除该用户在云端的所有同步数据
（仅登录有效）
     * @return Result<Unit> 成功或失败结果
     */
    suspend operator fun invoke(includeCloud: Boolean = false): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 0. 获取当前用户信息（需要在删除本地数据前获取）
            val currentUser = authRepository.getCurrentUser()

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

            // 7. 删除云端所有同步记录（如果已登录且用户选择清除）
            if (includeCloud && currentUser != null) {
                syncRepository.deleteAllCloudData(currentUser.id)
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
