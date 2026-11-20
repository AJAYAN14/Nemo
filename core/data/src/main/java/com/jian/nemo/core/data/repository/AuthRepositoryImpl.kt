package com.jian.nemo.core.data.repository

import com.jian.nemo.core.common.Result
import com.jian.nemo.core.common.error.AuthException
import com.jian.nemo.core.data.local.dao.UserDao
import com.jian.nemo.core.data.local.entity.UserEntity
import com.jian.nemo.core.domain.model.User
import com.jian.nemo.core.domain.repository.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import io.ktor.client.HttpClient
import io.ktor.client.statement.bodyAsText
import io.ktor.http.content.TextContent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.header
import io.ktor.http.contentType
import io.ktor.http.ContentType
import java.io.File
import javax.inject.Inject
import com.jian.nemo.core.common.di.ApplicationScope

/**
 * AuthRepository implementation using Supabase (Auth + Storage).
 * Replaces former LeanCloud-based implementation.
 */
class AuthRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient,
    private val userDao: UserDao,
    private val database: com.jian.nemo.core.data.local.NemoDatabase,
    private val settingsRepository: com.jian.nemo.core.domain.repository.SettingsRepository,
    private val dataSeedService: com.jian.nemo.core.data.util.DataSeedService,
    @ApplicationScope private val externalScope: CoroutineScope
) : AuthRepository {

    private val _isSessionResolved = MutableStateFlow(false)
    override val isSessionResolved: StateFlow<Boolean> = _isSessionResolved.asStateFlow()

    init {
        externalScope.launch {
            try {
                userDao.getCurrentUserSnapshot()
                _isSessionResolved.value = true
            } catch (e: Exception) {
                _isSessionResolved.value = true
            }

            supabase.auth.sessionStatus.collect { status ->
                when (status) {
                    is io.github.jan.supabase.auth.status.SessionStatus.Authenticated -> {
                        val userInfo = supabase.auth.currentUserOrNull()
                        if (userInfo != null) {
                            saveUserToLocal(userInfo)
                        }
                        _isSessionResolved.value = true
                    }
                    is io.github.jan.supabase.auth.status.SessionStatus.NotAuthenticated -> {
                        _isSessionResolved.value = true
                    }
                    else -> {
                        // Loading or transient states
                    }
                }
            }
        }
    }

    override fun getUserFlow(): Flow<User?> = userDao.getCurrentUser()
        .map { it?.toDomainModel() }
        .distinctUntilChanged()

    override suspend fun login(email: String, password: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            val userInfo = supabase.auth.currentUserOrNull() ?: throw IllegalStateException("No user after sign in")
            saveUserToLocal(userInfo)
            val user = userInfo.toDomainModel()
            // 确保基础数据已填充（应对 clearAllTables 后重新登录的场景）
            dataSeedService.ensureDataSeeded()
            Result.Success(user)
        } catch (e: Exception) {
            Result.Error(e.toAuthException())
        }
    }

    override suspend fun register(username: String, email: String, password: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            supabase.auth.signUpWith(Email) {
                this.email = email
                this.password = password
                data = buildJsonObject {
                    put("nickname", username)
                }
            }
            val userInfo = supabase.auth.currentUserOrNull() ?: throw IllegalStateException("No user after sign up")
            // Update nickname if not set (e.g. when email confirmation is required)
            if (userInfo.userMetadata?.get("nickname") == null) {
                supabase.auth.updateUser {
                    data = buildJsonObject {
                        put("nickname", username)
                    }
                }
            }
            val current = supabase.auth.currentUserOrNull() ?: userInfo
            saveUserToLocal(current)
            val user = current.toDomainModel().copy(username = username)
            // 确保基础数据已填充（应对 clearAllTables 后重新注册的场景）
            dataSeedService.ensureDataSeeded()
            Result.Success(user)
        } catch (e: Exception) {
            Result.Error(e.toAuthException())
        }
    }

    override suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 1. 云端登出
            try {
                supabase.auth.signOut()
            } catch (e: Exception) {
                // 忽略云端登出失败，确保本地清理执行
                e.printStackTrace()
            }

            // 2. 本地核清理 (单用户模式隔离策略)
            // 清除所有数据库表 (包括 User, Word, StudyRecord...)
            database.clearAllTables()

            // 清除 DataStore 中的用户数据 (保留设备配置)
            settingsRepository.clearUserData()

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.toAuthException())
        }
    }

    override suspend fun getCurrentUser(): User? = withContext(Dispatchers.IO) {
        val cached = userDao.getCurrentUserSnapshot()
        if (cached != null) {
            return@withContext cached.toDomainModel()
        }
        try {
            // 等待 Supabase Auth 插件从存储中加载 Session
            supabase.auth.awaitInitialization()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        supabase.auth.currentUserOrNull()?.toDomainModel()
    }

    override suspend fun updateAvatar(file: File): Result<String> = withContext(Dispatchers.IO) {
        try {
            val userInfo = supabase.auth.currentUserOrNull() ?: throw IllegalStateException("User not logged in")
            val path = "${userInfo.id}.jpg"
            val bytes = file.readBytes()
            supabase.storage.from(AVATARS_BUCKET).upload(path, bytes) { upsert = true }
            val publicUrl = supabase.storage.from(AVATARS_BUCKET).publicUrl(path)
            val timestamp = System.currentTimeMillis()
            val versionedUrl = "$publicUrl?t=$timestamp"
            updateUserAvatarUrl(versionedUrl)
            Result.Success(versionedUrl)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(e.toAuthException())
        }
    }

    override suspend fun updateUserAvatarUrl(url: String?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val userInfo = supabase.auth.currentUserOrNull() ?: throw IllegalStateException("User not logged in")
            supabase.auth.updateUser {
                data = buildJsonObject {
                    put("avatar", url ?: "")
                }
            }
            userDao.updateUserAvatar(userInfo.id, url)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.toAuthException())
        }
    }

    override suspend fun sendPasswordResetOtp(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Use OTP provider to strictly request a 6-digit code (Email OTP)
            supabase.auth.signInWith(OTP) {
                this.email = email
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.toAuthException())
        }
    }

    override suspend fun verifyPasswordResetOtp(email: String, token: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // For Email OTP login, the type is usually EMAIL (or sometimes MAGIC_LINK in some contexts, but EMAIL is standard for 'email' type verification)
            supabase.auth.verifyEmailOtp(
                type = io.github.jan.supabase.auth.OtpType.Email.EMAIL,
                email = email,
                token = token
            )
            // Verification logs the user in
             val userInfo = supabase.auth.currentUserOrNull() ?: throw IllegalStateException("No user after verification")
             saveUserToLocal(userInfo)
             Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.toAuthException())
        }
    }

    override suspend fun requestPasswordReset(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        // Deprecated in favor of sendPasswordResetOtp for mobile
        sendPasswordResetOtp(email)
    }

    override suspend fun updateUsername(newUsername: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val userInfo = supabase.auth.currentUserOrNull() ?: throw IllegalStateException("User not logged in")
            supabase.auth.updateUser {
                data = buildJsonObject {
                    put("nickname", newUsername)
                }
            }
            saveUserToLocal(supabase.auth.currentUserOrNull() ?: userInfo)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.toAuthException())
        }
    }

    override suspend fun updateEmail(newEmail: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Update email request triggers an OTP to be sent to the new email
            supabase.auth.updateUser { this.email = newEmail }
            // Note: Unlike sign up, update user might not immediately change email until verified.
            // We just return success indicating OTP sent.
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.toAuthException())
        }
    }

    override suspend fun verifyEmailChangeOtp(email: String, token: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            supabase.auth.verifyEmailOtp(
                type = io.github.jan.supabase.auth.OtpType.Email.EMAIL_CHANGE,
                email = email,
                token = token
            )
            // After verification, the email is actually updated in Supabase.
            // We should refresh local user info.
            val userInfo = supabase.auth.currentUserOrNull() ?: throw IllegalStateException("No user after verification")
            saveUserToLocal(userInfo)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.toAuthException())
        }
    }

    override suspend fun updatePassword(newPassword: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            supabase.auth.updateUser { this.password = newPassword }
            // No need to save user local for password change usually, but good to refresh
            saveUserToLocal(supabase.auth.currentUserOrNull()!!)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.toAuthException())
        }
    }

    override suspend fun deleteAccount(password: String?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val userInfo = supabase.auth.currentUserOrNull() ?: throw IllegalStateException("User not logged in")
            val email = userInfo.email ?: throw IllegalStateException("User email is null")
            // Re-verify password if provided
            if (!password.isNullOrEmpty()) {
                supabase.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
            }
            // Call Edge Function to perform physical deletion on server side
            try {
                supabase.functions.invoke("delete-user")
            } catch (e: Exception) {
                // If cloud function fails, we abort the local deletion to keep states consistent,
                // or we could proceed if we want to allow 'local only' deletion as fallback.
                // Here we choose to strict mode: server must succeed.
                throw AuthException("服务器删除失败: ${e.message}", cause = e)
            }

            // If server deletion succeeded (or we decide to ignore server error),
            // clean up local state.
            supabase.auth.signOut()
            userDao.deleteAll()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.toAuthException())
        }
    }


    private suspend fun saveUserToLocal(userInfo: UserInfo) {
        val nickname = userInfo.userMetadata?.stringOrNull("nickname")?.takeIf { it.isNotEmpty() }
            ?: userInfo.userMetadata?.stringOrNull("full_name")?.takeIf { it.isNotEmpty() }
            ?: userInfo.userMetadata?.stringOrNull("name")?.takeIf { it.isNotEmpty() }
            ?: userInfo.email?.substringBefore('@').orEmpty()
            
        val avatar = userInfo.userMetadata?.stringOrNull("avatar")?.takeIf { it.isNotEmpty() }
            ?: userInfo.userMetadata?.stringOrNull("avatar_url")?.takeIf { it.isNotEmpty() }
            ?: userInfo.userMetadata?.stringOrNull("picture")?.takeIf { it.isNotEmpty() }
            
        val session = supabase.auth.currentSessionOrNull()
        val hasGoogleLinked = userInfo.identities?.any { it.provider == "google" } == true
        val isOnlyGoogleIdentity = userInfo.identities?.size == 1 && hasGoogleLinked
        val userEntity = UserEntity(
            id = userInfo.id,
            username = nickname,
            email = userInfo.email ?: "",
            sessionToken = session?.accessToken ?: "",
            avatar = avatar,
            hasGoogleLinked = hasGoogleLinked,
            isOnlyGoogleIdentity = isOnlyGoogleIdentity
        )
        userDao.insert(userEntity)
    }

    private fun Exception.toAuthException(): Exception {
        return when {
            this is AuthException -> this
            message?.contains("rate limit", ignoreCase = true) == true ->
                AuthException(
                    message = "邮件发送过于频繁，请稍后再试。若为开发环境，可在 Supabase 控制台关闭「确认邮箱」以避免此限制。",
                    code = -1,
                    cause = this
                )
            message?.contains("Bearer token", ignoreCase = true) == true ->
                AuthException(
                    message = "登录状态已过期或网络异常，请重新登录后再尝试此操作。",
                    code = 401,
                    cause = this
                )
            else -> AuthException(
                message = message ?: "Authentication failed",
                code = -1,
                cause = this
            )
        }
    }

    private fun UserInfo.toDomainModel(): User {
        val nickname = userMetadata?.stringOrNull("nickname")?.takeIf { it.isNotEmpty() }
            ?: userMetadata?.stringOrNull("full_name")?.takeIf { it.isNotEmpty() }
            ?: userMetadata?.stringOrNull("name")?.takeIf { it.isNotEmpty() }
            ?: email?.substringBefore('@').orEmpty()
            
        val avatarUrl = userMetadata?.stringOrNull("avatar")?.takeIf { it.isNotEmpty() }
            ?: userMetadata?.stringOrNull("avatar_url")?.takeIf { it.isNotEmpty() }
            ?: userMetadata?.stringOrNull("picture")?.takeIf { it.isNotEmpty() }
            
        val session = supabase.auth.currentSessionOrNull()
        val hasGoogleLinked = identities?.any { it.provider == "google" } == true
        val isOnlyGoogleIdentity = identities?.size == 1 && hasGoogleLinked
        
        // 判断是否为新用户 (创建时间在两分钟以内)
        val createdAtInstant = createdAt
        val isNewUser = createdAtInstant != null && 
            (kotlinx.datetime.Clock.System.now() - createdAtInstant).inWholeSeconds < 120

        return User(
            id = id,
            username = nickname,
            email = email,
            avatarUrl = avatarUrl,
            sessionToken = session?.accessToken ?: "",
            hasGoogleLinked = hasGoogleLinked,
            isOnlyGoogleIdentity = isOnlyGoogleIdentity,
            isNewUser = isNewUser
        )
    }

    private fun com.jian.nemo.core.data.local.entity.UserEntity.toDomainModel(): User {
        return User(
            id = id,
            username = username,
            email = email,
            avatarUrl = avatar,
            sessionToken = sessionToken,
            hasGoogleLinked = hasGoogleLinked,
            isOnlyGoogleIdentity = isOnlyGoogleIdentity
        )
    }

    companion object {
        private const val AVATARS_BUCKET = "avatars"
        /** 启动时 Session 解析的最大等待时间（毫秒）。超过此时间将从本地缓存降级恢复。 */
        private const val SESSION_RESOLVE_TIMEOUT_MS = 5000L
    }
}

private fun JsonObject.stringOrNull(key: String): String? =
    get(key)?.jsonPrimitive?.contentOrNull
