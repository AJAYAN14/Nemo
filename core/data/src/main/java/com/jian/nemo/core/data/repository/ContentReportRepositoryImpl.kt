package com.jian.nemo.core.data.repository

import com.jian.nemo.core.common.Result
import com.jian.nemo.core.data.model.ContentReportDto
import com.jian.nemo.core.domain.repository.AuthRepository
import com.jian.nemo.core.domain.repository.ContentReportRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 内容报告 Repository 实现
 * 使用 Supabase Postgrest 提交数据
 */
@Singleton
class ContentReportRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val authRepository: AuthRepository
) : ContentReportRepository {

    override suspend fun reportContentError(itemId: Int, itemType: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val userId = authRepository.getCurrentUser()?.id
            
            val reportDto = ContentReportDto(
                itemId = itemId,
                itemType = itemType,
                userId = userId
            )

            // 提交或更新到 Supabase 'content_reports' 表 (幂等处理)
            supabaseClient.postgrest["content_reports"].upsert(reportDto)

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
