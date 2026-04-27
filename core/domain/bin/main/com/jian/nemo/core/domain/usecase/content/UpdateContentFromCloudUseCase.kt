package com.jian.nemo.core.domain.usecase.content

import com.jian.nemo.core.domain.repository.ContentRepository
import com.jian.nemo.core.domain.repository.ContentUpdateApplier
import com.jian.nemo.core.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * 检查云端词库版本，若有更新则下载并合并到本地
 *
 * @return 本次应用的云端版本号（未更新返回 null）
 */
class UpdateContentFromCloudUseCase @Inject constructor(
    private val contentRepository: ContentRepository,
    private val contentUpdateApplier: ContentUpdateApplier,
    private val settingsRepository: SettingsRepository
) {
    private val levels = listOf("N1", "N2", "N3", "N4", "N5")

    suspend fun run(): Int? {
        val remoteVersion = contentRepository.getRemoteContentVersion() ?: return null
        val lastVersion = settingsRepository.getLastContentVersion()
        if (remoteVersion <= lastVersion) return null

        for (level in levels) {
            contentRepository.downloadWordJson(level)?.let { json ->
                contentUpdateApplier.applyWordsFromJson(level, json)
            }
            contentRepository.downloadGrammarJson(level)?.let { json ->
                contentUpdateApplier.applyGrammarsFromJson(level, json)
            }
        }

        settingsRepository.setLastContentVersion(remoteVersion)
        return remoteVersion
    }
}
