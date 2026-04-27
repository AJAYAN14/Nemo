package com.jian.nemo.core.domain.usecase.settings

import com.jian.nemo.core.domain.repository.DataExportRepository
import javax.inject.Inject

/**
 * 导出数据 UseCase
 */
class ExportDataUseCase @Inject constructor(
    private val repository: DataExportRepository
) {
    suspend operator fun invoke(uri: String): Boolean {
        return repository.exportDataToUri(uri)
    }
}
