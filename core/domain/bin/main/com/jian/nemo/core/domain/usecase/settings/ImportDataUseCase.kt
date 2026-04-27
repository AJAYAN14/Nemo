package com.jian.nemo.core.domain.usecase.settings

import com.jian.nemo.core.domain.repository.DataExportRepository
import javax.inject.Inject

/**
 * 导入数据 UseCase
 */
class ImportDataUseCase @Inject constructor(
    private val repository: DataExportRepository
) {
    suspend operator fun invoke(uri: String): String {
        return repository.importDataFromUri(uri)
    }
}
