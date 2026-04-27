package com.jian.nemo.feature.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jian.nemo.core.common.Result
import com.jian.nemo.core.domain.model.User
import com.jian.nemo.core.domain.model.SyncReport
import com.jian.nemo.core.domain.repository.AuthRepository
import com.jian.nemo.core.domain.repository.SyncRepository
import com.jian.nemo.core.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.io.File
import javax.inject.Inject

import com.jian.nemo.core.domain.usecase.auth.*
import com.jian.nemo.core.domain.usecase.sync.RestoreDataUseCase
import com.jian.nemo.core.domain.usecase.settings.GetLastRestoreTimeUseCase
import com.jian.nemo.core.domain.usecase.settings.GetUserAvatarPathUseCase
import com.jian.nemo.core.domain.model.SyncProgress

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val updateUserProfileUseCase: UpdateUserProfileUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getUserFlowUseCase: GetUserFlowUseCase,
    private val restoreDataUseCase: RestoreDataUseCase,
    private val syncRepository: SyncRepository,
    private val getLastRestoreTimeUseCase: GetLastRestoreTimeUseCase,
    private val settingsRepository: SettingsRepository,
    private val getUserAvatarPathUseCase: GetUserAvatarPathUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {
    
    // 用于隔离登录/注册期间产生的全局状态扰动，防止过早跳转
    private var isAuthActionInProgress = false

    private val _uiState = MutableStateFlow(AuthUiState(isLoading = true))
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        observeSettings()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            getUserAvatarPathUseCase().collect { path ->
                _uiState.update { it.copy(avatarPath = path) }
            }
        }

        // 格式化同步/恢复时间逻辑
        viewModelScope.launch {
            settingsRepository.lastSyncTimeFlow.collect { time ->
                val timeText = if (time > 0) {
                    val date = java.util.Date(time)
                    val format = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault())
                    "上次同步：${format.format(date)}"
                } else null
                _uiState.update { it.copy(lastSyncTime = time, lastSyncTimeText = timeText) }
            }
        }

        viewModelScope.launch {
            getLastRestoreTimeUseCase().collect { time ->
                val timeText = if (time > 0) {
                    val date = java.util.Date(time)
                    val format = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault())
                    "上次恢复：${format.format(date)}"
                } else null
                _uiState.update { it.copy(lastRestoreTime = time, lastRestoreTimeText = timeText) }
            }
        }

        // 持续观察登录用户状态
        viewModelScope.launch {
            getUserFlowUseCase().collect { user ->
                // 如果正在执行登录/注册操作，忽略流中的快照，由操作自身处理
                if (isAuthActionInProgress) return@collect
                // 【修复】自动恢复/更新用户状态时，检查并同步远程头像配置到本地
                val fetchedUrl = user?.avatarUrl
                if (fetchedUrl != null && _uiState.value.avatarPath != fetchedUrl) {
                    settingsRepository.setUserAvatarPath(fetchedUrl)
                }
                
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isLoggedIn = user != null,
                        user = user
                    )
                }
            }
        }

        // 等待 Supabase session 状态确定后再标记 isAuthChecked
        // 避免 LoadingFromStorage 阶段 _userFlow 的 null 初始值导致误判为未登录
        viewModelScope.launch {
            authRepository.isSessionResolved.collect { resolved ->
                if (resolved) {
                    _uiState.update { it.copy(isAuthChecked = true) }
                }
            }
        }

        // 观察全局后台同步进度
        viewModelScope.launch {
            syncRepository.globalSyncProgress.collect { progress ->
                when (progress) {
                    is SyncProgress.Idle -> {
                        // Do nothing
                    }
                    is SyncProgress.Running -> {
                         val p = if (progress.total > 0) progress.current.toFloat() / progress.total else 0f
                         _uiState.update {
                             it.copy(
                                 isRestoreLoading = true,
                                 restoreProgress = p,
                                 restoreStatus = "${progress.section} (${progress.current}/${progress.total})"
                             )
                         }
                    }
                    is SyncProgress.Completed -> {
                         _uiState.update {
                            it.copy(
                                isRestoreLoading = false,
                                showRestoreSuccess = true,
                                restoreProgress = 1f,
                                restoreStatus = "同步完成"
                            )
                         }
                         delay(2000)
                         _uiState.update { it.copy(showRestoreSuccess = false) }
                    }
                    is SyncProgress.Failed -> {
                         _uiState.update {
                             it.copy(
                                 isRestoreLoading = false,
                                 error = progress.error,
                                 restoreProgress = 0f,
                                 restoreStatus = "同步失败"
                             )
                         }
                    }
                    else -> {}
                }
            }
        }
    }


    // --- UI Interactions ---

    fun onEmailChanged(value: String) {
        _uiState.update { it.copy(email = value) }
    }

    fun onUsernameChanged(value: String) {
        _uiState.update { it.copy(username = value) }
    }

    fun onPasswordChanged(value: String) {
        _uiState.update { it.copy(password = value) }
    }

    fun onConfirmPasswordChanged(value: String) {
        _uiState.update { it.copy(confirmPassword = value) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun toggleConfirmPasswordVisibility() {
        _uiState.update { it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible) }
    }

    fun toggleLoginMode() {
        _uiState.update { it.copy(isLoginMode = !it.isLoginMode, error = null, isFormAttempted = false) }
    }

    fun showDialog(dialogType: UserDialogType) {
        _uiState.update { it.copy(activeDialog = dialogType) }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(activeDialog = UserDialogType.NONE) }
    }

    fun onLoginClicked() {
        val state = _uiState.value
        _uiState.update { it.copy(isFormAttempted = true) }
        if (state.email.isNotBlank() && state.password.isNotBlank()) {
            login(state.email, state.password)
        }
    }

    fun onRegisterClicked(onPasswordMismatch: () -> Unit) {
        val state = _uiState.value
        _uiState.update { it.copy(isFormAttempted = true) }
        if (state.username.isNotBlank() && state.email.isNotBlank() && 
            state.password.length >= 6 && state.password == state.confirmPassword) {
            register(state.username, state.email, state.password)
        } else if (state.password != state.confirmPassword) {
            onPasswordMismatch()
        }
    }

    fun onAvatarChanged(newAvatarPath: String?) {
        if (newAvatarPath != null) {
            if (com.jian.nemo.core.ui.util.PresetAvatars.isPreset(newAvatarPath)) {
                updateAvatarUrl(newAvatarPath)
            } else {
                uploadAvatar(java.io.File(newAvatarPath))
            }
        } else {
            clearAvatar()
        }
        dismissDialog()
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    successMessage = null,
                    restoreMessage = null
                )
            }
            isAuthActionInProgress = true
            try {
                when (val result = loginUseCase(email, password)) {
                    is Result.Success -> {
                        // 【修复】登录成功后，如果云端有 avatarUrl，同步写回本地 Settings
                        val fetchedAvatarUrl = result.data.avatarUrl
                        if (!fetchedAvatarUrl.isNullOrEmpty()) {
                            settingsRepository.setUserAvatarPath(fetchedAvatarUrl)
                        }

                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isLoggedIn = true,
                                user = result.data,
                                // [Optimization] Set sync loading state immediately to let UI show indicator
                                isRestoreLoading = true,
                                restoreStatus = "正在准备同步...",
                                restoreProgress = 0f
                            )
                        }
                        // 登录成功后启动后台同步 (Smart Sync)
                        syncRepository.startBackgroundSync(result.data.id)
                    }
                    is Result.Error -> {
                        handleAuthError(result.exception, "登录失败")
                    }
                    else -> {}
                }
            } finally {
                isAuthActionInProgress = false
            }
        }
    }

    fun register(username: String, email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            isAuthActionInProgress = true
            try {
                when (val result = registerUseCase(username, email, password)) {
                    is Result.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isLoggedIn = true,
                                user = result.data
                            )
                        }
                    }
                    is Result.Error -> {
                        handleAuthError(result.exception, "注册失败")
                    }
                    else -> {}
                }
            } finally {
                isAuthActionInProgress = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            logoutUseCase()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isLoggedIn = false,
                    user = null,
                    avatarPath = ""
                )
            }
        }
    }

    fun uploadAvatar(file: File) {
        viewModelScope.launch {
             _uiState.update { it.copy(isLoading = true, error = null, successMessage = null) }
             when(val result = updateUserProfileUseCase.uploadAvatar(file)) {
                 is Result.Success -> {
                     val updatedUser = _uiState.value.user?.copy(avatarUrl = result.data)
                     _uiState.update {
                         it.copy(
                             isLoading = false,
                             user = updatedUser,
                             successMessage = "头像上传成功",
                             avatarPath = file.absolutePath
                         )
                     }
                    // 静默同步，不覆盖头像成功消息
                    syncToCloud(silent = true)
                 }
                 is Result.Error -> {
                     _uiState.update { it.copy(isLoading = false, error = result.exception.message ?: "头像上传失败") }
                 }
                 else -> {}
             }
        }
    }

    fun updateAvatarUrl(url: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, successMessage = null) }
            when (val result = updateUserProfileUseCase.updateUserAvatarUrl(url)) {
                is Result.Success -> {
                    val updatedUser = _uiState.value.user?.copy(avatarUrl = url)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            user = updatedUser,
                            successMessage = "头像更新成功",
                            avatarPath = url // 预设头像直接用协议串作为 path
                        )
                    }
                    // 静默同步，不覆盖头像成功消息
                    syncToCloud(silent = true)
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.exception.message ?: "头像更新失败") }
                }
                else -> {}
            }
        }
    }


    // New OTP-based password reset flows
    fun sendPasswordResetOtp(email: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when(val result = authRepository.sendPasswordResetOtp(email)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, successMessage = "验证码已发送") }
                    onSuccess()
                }
                is Result.Error -> {
                    val msg = result.exception.message ?: "发送验证码失败"
                    _uiState.update { it.copy(isLoading = false, error = msg) }
                    onError(msg)
                }
                else -> {}
            }
        }
    }

    fun verifyPasswordResetOtp(email: String, token: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when(val result = authRepository.verifyPasswordResetOtp(email, token)) {
                is Result.Success -> {
                    // Logic handled in repo (login), we just update UI
                    _uiState.update { it.copy(isLoading = false, successMessage = "验证成功") }
                    onSuccess()
                }
                is Result.Error -> {
                     val msg = result.exception.message ?: "验证失败"
                    _uiState.update { it.copy(isLoading = false, error = msg) }
                    onError(msg)
                }
                 else -> {}
            }
        }
    }

    fun completePasswordReset(password: String, onSuccess: () -> Unit, onError: (String) -> Unit = {}) {
         viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
             when(val result = authRepository.updatePassword(password)) {
                 is Result.Success -> {
                     _uiState.update { it.copy(isLoading = false, successMessage = "密码重置成功") }
                     onSuccess()
                 }
                 is Result.Error -> {
                      val msg = result.exception.message ?: "重置密码失败"
                     _uiState.update { it.copy(isLoading = false, error = msg) }
                     onError(msg)
                 }
                 else -> {}
             }
        }
    }

    fun updateUsername(newUsername: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, successMessage = null) }
            when (val result = updateUserProfileUseCase.updateUsername(newUsername)) {
                is Result.Success -> {
                    val updatedUser = _uiState.value.user?.copy(username = newUsername)
                    _uiState.update {
                        it.copy(isLoading = false, user = updatedUser, successMessage = "用户名更新成功")
                    }
                    onSuccess()
                }
                is Result.Error -> {
                    val msg = result.exception.message ?: "更新失败"
                    _uiState.update { it.copy(isLoading = false, error = msg) }
                    onError(msg)
                }
                else -> {}
            }
        }
    }

    fun updateEmail(newEmail: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, successMessage = null) }
            when (val result = authRepository.updateEmail(newEmail)) {
                is Result.Success -> {
                    // OTP sent
                    _uiState.update {
                        it.copy(isLoading = false, successMessage = "验证码已发送至新邮箱")
                    }
                    onSuccess()
                }
                is Result.Error -> {
                    val msg = result.exception.message ?: "发送验证码失败"
                    _uiState.update { it.copy(isLoading = false, error = msg) }
                    onError(msg)
                }
                else -> {}
            }
        }
    }

    fun verifyEmailUpdate(email: String, code: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, successMessage = null) }
            when (val result = authRepository.verifyEmailChangeOtp(email, code)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(isLoading = false, successMessage = "邮箱修改成功")
                    }
                    onSuccess()
                }
                is Result.Error -> {
                    val msg = result.exception.message ?: "验证失败"
                     _uiState.update { it.copy(isLoading = false, error = msg) }
                    onError(msg)
                }
                else -> {}
            }
        }
    }

    fun deleteAccount(password: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, successMessage = null) }
            when (val result = deleteAccountUseCase(password)) {
                is Result.Success -> {
                    logoutUseCase()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isLoggedIn = false,
                            user = null,
                            successMessage = "账号已成功销毁",
                            avatarPath = ""
                        )
                    }
                    onSuccess()
                }
                is Result.Error -> {
                    val msg = result.exception.message ?: "操作失败"
                    _uiState.update { it.copy(isLoading = false, error = msg) }
                    onError(msg)
                }
                else -> {}
            }
        }
    }

    fun syncToCloud(silent: Boolean = false) {
        viewModelScope.launch {
            // Set granular loading state, NOT global loading
            _uiState.update {
                it.copy(
                    isSyncLoading = true,
                    error = if (!silent) null else it.error,
                    successMessage = if (!silent) null else it.successMessage,
                    showSyncSuccess = false,
                    syncProgress = 0f,
                    syncStatus = "准备中..."
                )
            }

            val userId = _uiState.value.user?.id ?: return@launch
            syncRepository.performSync(userId, false).collect { progress ->
                when(progress) {
                    is SyncProgress.Running -> {
                         val p = if (progress.total > 0) progress.current.toFloat() / progress.total else 0f
                         _uiState.update {
                             it.copy(
                                 syncProgress = p,
                                 syncStatus = "${progress.section} (${progress.current}/${progress.total})"
                             )
                         }
                    }
                    is SyncProgress.Completed -> {
                         val stats = progress.report.stats
                         val detail = buildString {
                             append("同步成功: ")
                             if (stats.addedItems > 0) append("新增 ${stats.addedItems} 条, ")
                             if (stats.updatedItems > 0) append("更新 ${stats.updatedItems} 条")
                         }.removeSuffix(", ")

                         _uiState.update {
                            it.copy(
                                isSyncLoading = false,
                                showSyncSuccess = true,
                                successMessage = detail,
                                syncProgress = 1f,
                                syncStatus = "完成"
                            )
                         }
                         delay(2000)
                         _uiState.update { it.copy(showSyncSuccess = false) }
                    }
                    is SyncProgress.Failed -> {
                         _uiState.update {
                             it.copy(
                                 isSyncLoading = false,
                                 error = progress.error,
                                 syncProgress = 0f,
                                 syncStatus = "失败"
                             )
                         }
                    }
                    is SyncProgress.AlreadyRunning -> {
                         _uiState.update {
                             it.copy(
                                 isSyncLoading = false,
                                 successMessage = "同步已在后台进行中",
                                 syncStatus = "后台运行中"
                             )
                         }
                         delay(2000)
                         _uiState.update { it.copy(successMessage = null) }
                    }
                    else -> {}
                }
            }
        }
    }



    /** 用户点击「从云端恢复」→ 弹出确认对话框 */
    fun restoreFromCloud() {
        _uiState.update { it.copy(showRestoreConfirmDialog = true) }
    }

    /** 用户确认恢复 → 执行全量镜像恢复 */
    fun confirmRestoreAfterWarning() {
        _uiState.update { it.copy(showRestoreConfirmDialog = false) }
        viewModelScope.launch {
            _uiState.update { it.copy(isRestoreLoading = true, error = null, successMessage = null, showRestoreSuccess = false) }

            val startTime = System.currentTimeMillis()

            val userId = _uiState.value.user?.id
            if (userId == null) {
                _uiState.update { it.copy(isRestoreLoading = false, error = "未登录") }
                return@launch
            }

            restoreDataUseCase().collect { progress ->
                when (progress) {
                    is SyncProgress.Running -> {
                        val p = if (progress.total > 0) progress.current.toFloat() / progress.total else 0f
                        _uiState.update {
                            it.copy(
                                restoreProgress = p,
                                restoreStatus = "${progress.section} (${progress.current}/${progress.total})"
                            )
                        }
                    }
                    is SyncProgress.Completed -> {
                        val elapsedTime = System.currentTimeMillis() - startTime
                        if (elapsedTime < 800) {
                            delay(800 - elapsedTime)
                        }

                        _uiState.update {
                            it.copy(
                                isRestoreLoading = false,
                                showRestoreSuccess = true,
                                restoreProgress = 1f,
                                restoreStatus = "完成"
                            )
                        }

                        delay(2000)
                        _uiState.update { it.copy(showRestoreSuccess = false) }
                    }
                    is SyncProgress.Failed -> {
                        _uiState.update {
                            it.copy(
                                isRestoreLoading = false,
                                error = progress.error,
                                restoreProgress = 0f,
                                restoreStatus = "失败"
                            )
                        }
                    }
                    is SyncProgress.AlreadyRunning -> {
                        _uiState.update {
                            it.copy(
                                isRestoreLoading = false,
                                successMessage = "恢复任务已在运行",
                                restoreStatus = "已在运行"
                            )
                        }
                        delay(2000)
                        _uiState.update { it.copy(successMessage = null) }
                    }
                    else -> {}
                }
            }
        }
    }

    fun cancelRestoreConfirmation() {
        _uiState.update { it.copy(showRestoreConfirmDialog = false) }
    }

    fun deleteAllCloudSyncData(onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, successMessage = null) }
            val userId = _uiState.value.user?.id ?: return@launch
            if (syncRepository.deleteAllCloudData(userId)) {
                _uiState.update { it.copy(isLoading = false, successMessage = "已清空云端所有同步数据") }
                onSuccess()
            } else {
                val msg = "清理失败"
                _uiState.update { it.copy(isLoading = false, error = msg) }
                onError(msg)
            }
        }
    }

    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearRestoreMessage() {
        _uiState.update { it.copy(restoreMessage = null) }
    }

    fun clearAvatar() {
        viewModelScope.launch {
            updateUserProfileUseCase.clearAvatar()
            _uiState.update { it.copy(avatarPath = "") }
            syncToCloud(silent = true)
        }
    }



    private fun handleAuthError(exception: Throwable, defaultMessage: String) {
        val message = if (exception is com.jian.nemo.core.common.error.AuthException) {
            "错误(${exception.code}): ${exception.message ?: defaultMessage}"
        } else {
            exception.message ?: defaultMessage
        }
        _uiState.update { it.copy(isLoading = false, error = message) }
    }
}

enum class UserDialogType {
    NONE,
    RESET_PASSWORD,
    UPDATE_USERNAME,
    UPDATE_EMAIL,
    DELETE_ACCOUNT,
    DELETE_CLOUD_SYNC_DATA,
    LOGOUT_CONFIRM,
    UPDATE_AVATAR
}

data class AuthUiState(
    val isLoggedIn: Boolean = false,
    val user: User? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val restoreMessage: String? = null,
    val lastRestoreTime: Long = 0L,
    val lastSyncTime: Long = 0L,
    val lastRestoreTimeText: String? = null,
    val lastSyncTimeText: String? = null,
    val syncReport: SyncReport? = null,
    val avatarPath: String = "",
    val isAuthChecked: Boolean = false,

    // Form States
    val email: String = "",
    val username: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val isLoginMode: Boolean = true,
    val isFormAttempted: Boolean = false,

    // Dialog State
    val activeDialog: UserDialogType = UserDialogType.NONE,

    // UX Pro Max States
    val isSyncLoading: Boolean = false,
    val syncProgress: Float = 0f,
    val syncStatus: String = "",
    val showSyncSuccess: Boolean = false,
    val isRestoreLoading: Boolean = false,
    val showRestoreSuccess: Boolean = false,
    val showRestoreConfirmDialog: Boolean = false,
    val restoreProgress: Float = 0f,
    val restoreStatus: String = ""
) {
    val emailError: Boolean get() = isFormAttempted && email.isBlank()
    val passwordError: Boolean get() = isFormAttempted && (if (isLoginMode) password.isBlank() else password.length < 6)
    val usernameError: Boolean get() = isFormAttempted && !isLoginMode && username.isBlank()
    val confirmPasswordError: Boolean get() = isFormAttempted && !isLoginMode && confirmPassword != password
}

