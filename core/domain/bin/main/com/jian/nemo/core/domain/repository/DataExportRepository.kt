package com.jian.nemo.core.domain.repository

interface DataExportRepository {
    /**
     * 导出数据到指定 URI (String format)
     * @return 是否成功
     */
    suspend fun exportDataToUri(uriString: String): Boolean

    /**
     * 从指定 URI (String format) 导入数据
     * @return 导入结果描述 (Result message)
     */
    suspend fun importDataFromUri(uriString: String): String
}
