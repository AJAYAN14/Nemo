package com.jian.nemo.core.domain.repository

import com.jian.nemo.core.common.Result
import com.jian.nemo.core.domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/**
 * 用户认证 Repository 接口
 */
interface AuthRepository {
    /** Supabase session 状态是否已确定（已完成从存储加载） */
    val isSessionResolved: StateFlow<Boolean>

    /** 获取当前用户流 */
    fun getUserFlow(): Flow<User?>

    /** 登录 */
    suspend fun login(email: String, password: String): Result<User>

    /** 注册 */
    suspend fun register(username: String, email: String, password: String): Result<User>

    /** 登出 */
    suspend fun logout(): Result<Unit>

    /** 获取当前用户 */
    suspend fun getCurrentUser(): User?

    /** 上传头像 */
    suspend fun updateAvatar(file: File): Result<String>

    /** 更新用户头像URL (用于清除或设置URL) */
    suspend fun updateUserAvatarUrl(url: String?): Result<Unit>

    /** 请求重置密码邮件 */
    suspend fun requestPasswordReset(email: String): Result<Unit>

    /** 请求重置密码邮件 (OTP) */
    suspend fun sendPasswordResetOtp(email: String): Result<Unit>

    /** 验证重置密码 OTP */
    suspend fun verifyPasswordResetOtp(email: String, token: String): Result<Unit>

    /** 更新用户名 (映射为 Supabase 用户元数据 nickname) */
    suspend fun updateUsername(newUsername: String): Result<Unit>

    /** 更新用户邮箱 (发送 OTP) */
    suspend fun updateEmail(newEmail: String): Result<Unit>

    /** 验证更新邮箱 OTP */
    suspend fun verifyEmailChangeOtp(email: String, token: String): Result<Unit>

    /** 更新密码 */
    suspend fun updatePassword(newPassword: String): Result<Unit>

    /** 注销/销毁账户 */
    suspend fun deleteAccount(password: String): Result<Unit>
}
