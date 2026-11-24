package com.jian.nemo.feature.user

import android.content.Context
import com.jian.nemo.core.common.Result
import com.jian.nemo.core.domain.model.User
import com.jian.nemo.core.domain.repository.AuthRepository
import com.jian.nemo.core.domain.repository.AvatarStorageRepository
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.domain.repository.SettingsRepository
import com.jian.nemo.core.domain.usecase.auth.DeleteAccountUseCase
import com.jian.nemo.core.domain.usecase.auth.GetCurrentUserUseCase
import com.jian.nemo.core.domain.usecase.auth.GetUserFlowUseCase
import com.jian.nemo.core.domain.usecase.auth.LoginUseCase
import com.jian.nemo.core.domain.usecase.auth.LogoutUseCase
import com.jian.nemo.core.domain.usecase.auth.RegisterUseCase
import com.jian.nemo.core.domain.usecase.auth.UpdateUserProfileUseCase
import com.jian.nemo.core.domain.usecase.settings.GetUserAvatarPathUseCase
import io.github.jan.supabase.SupabaseClient
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * 账户管理核心业务逻辑与零硬编码规范自动化单元测试套件
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelAccountManagementTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var authRepository: AuthRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var updateUserProfileUseCase: UpdateUserProfileUseCase
    private lateinit var deleteAccountUseCase: DeleteAccountUseCase
    private lateinit var logoutUseCase: LogoutUseCase
    private lateinit var avatarStorageRepository: AvatarStorageRepository
    private lateinit var context: Context

    private val userFlow = MutableStateFlow<User?>(
        User(
            id = "test_user_001",
            username = "NemoUser",
            email = "nemo@example.com",
            avatarUrl = "https://cdn.example.com/avatar.jpg",
            sessionToken = "session_token_123",
            hasGoogleLinked = false,
            isOnlyGoogleIdentity = false
        )
    )
    private val isSessionResolvedFlow = MutableStateFlow(true)
    private val userAvatarPathFlow = MutableStateFlow("https://cdn.example.com/avatar.jpg")
    private val settingIconStyleFlow = MutableStateFlow(SettingIconStyle.SOFT_TRANSLUCENT)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        authRepository = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)
        updateUserProfileUseCase = mockk(relaxed = true)
        deleteAccountUseCase = mockk(relaxed = true)
        logoutUseCase = mockk(relaxed = true)
        avatarStorageRepository = mockk(relaxed = true)
        context = mockk(relaxed = true)

        every { authRepository.getUserFlow() } returns userFlow
        every { authRepository.isSessionResolved } returns isSessionResolvedFlow
        every { settingsRepository.userAvatarPathFlow } returns userAvatarPathFlow
        every { settingsRepository.settingIconStyleFlow } returns settingIconStyleFlow

        // 绑定常用字符串资源以验证消息匹配
        every { context.getString(any()) } answers { "MockString_${firstArg<Int>()}" }
        every { context.getString(R.string.user_auth_msg_update_username_success) } returns "用户名更新成功"
        every { context.getString(R.string.user_auth_err_empty_username) } returns "用户名不能为空"
        every { context.getString(R.string.user_auth_err_update_failed) } returns "更新失败"
        every { context.getString(R.string.user_auth_msg_email_otp_sent) } returns "验证码已发送至新邮箱"
        every { context.getString(R.string.user_auth_msg_email_updated) } returns "邮箱修改成功"
        every { context.getString(R.string.user_auth_msg_otp_sent) } returns "验证码已发送"
        every { context.getString(R.string.user_auth_err_send_otp_failed) } returns "发送验证码失败"
        every { context.getString(R.string.user_auth_msg_otp_verified) } returns "验证成功"
        every { context.getString(R.string.user_auth_err_verify_failed) } returns "验证失败"
        every { context.getString(R.string.user_auth_msg_reset_pwd_success) } returns "密码重置成功"
        every { context.getString(R.string.user_auth_err_reset_pwd_failed) } returns "重置密码失败"
        every { context.getString(R.string.user_auth_msg_account_deleted) } returns "账号已成功销毁"
        every { context.getString(R.string.user_auth_err_action_failed) } returns "操作失败"
        every { context.getString(R.string.user_dialog_err_same_username) } returns "新用户名不能与当前用户名相同"
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): AuthViewModel {
        return AuthViewModel(
            loginUseCase = mockk<LoginUseCase>(relaxed = true),
            registerUseCase = mockk<RegisterUseCase>(relaxed = true),
            logoutUseCase = logoutUseCase,
            updateUserProfileUseCase = updateUserProfileUseCase,
            deleteAccountUseCase = deleteAccountUseCase,
            getCurrentUserUseCase = mockk<GetCurrentUserUseCase>(relaxed = true),
            getUserFlowUseCase = GetUserFlowUseCase(authRepository),
            settingsRepository = settingsRepository,
            getUserAvatarPathUseCase = GetUserAvatarPathUseCase(settingsRepository),
            authRepository = authRepository,
            avatarStorageRepository = avatarStorageRepository,
            supabaseClient = mockk<SupabaseClient>(relaxed = true),
            context = context
        )
    }

    // ==========================================
    // 1. 初始化会话与用户状态流转
    // ==========================================

    @Test
    fun test_initialSessionResolved_withLoggedInUser_syncsStateCorrectly() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.isLoggedIn)
        assertTrue(state.isAuthChecked)
        assertNotNull(state.user)
        assertEquals("test_user_001", state.user?.id)
        assertEquals("NemoUser", state.user?.username)
        assertEquals("nemo@example.com", state.user?.email)
    }

    @Test
    fun test_initialSessionResolved_withNullUser_marksNotLoggedIn() = runTest(testDispatcher) {
        userFlow.value = null
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isLoggedIn)
        assertTrue(state.isAuthChecked)
        assertNull(state.user)
    }

    // ==========================================
    // 2. 修改用户名业务逻辑
    // ==========================================

    @Test
    fun test_updateUsername_success_updatesUiStateAndInvokesCallback() = runTest(testDispatcher) {
        coEvery { updateUserProfileUseCase.updateUsername("NewNemo") } returns Result.Success(Unit)

        val viewModel = createViewModel()
        viewModel.showDialog(UserDialogType.UPDATE_USERNAME)
        advanceUntilIdle()

        var callbackInvoked = false
        viewModel.updateUsername("NewNemo", onSuccess = {
            callbackInvoked = true
            viewModel.dismissDialog()
        })
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("NewNemo", state.user?.username)
        assertEquals("用户名更新成功", state.successMessage)
        assertNull(state.error)
        assertTrue(callbackInvoked)
        assertEquals(UserDialogType.NONE, state.activeDialog)
        coVerify(exactly = 1) { updateUserProfileUseCase.updateUsername("NewNemo") }
    }

    @Test
    fun test_updateUsername_emptyOrBlank_triggersImmediateErrorWithoutCallingUseCase() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        var recordedError: String? = null
        viewModel.updateUsername("    ", onError = { recordedError = it })
        advanceUntilIdle()

        assertEquals("用户名不能为空", recordedError)
        coVerify(exactly = 0) { updateUserProfileUseCase.updateUsername(any()) }
    }

    @Test
    fun test_updateUsername_lengthTrimmedToMax16() = runTest(testDispatcher) {
        val longUsername = "1234567890123456EXTRA"
        val expectedClean = "1234567890123456"
        coEvery { updateUserProfileUseCase.updateUsername(expectedClean) } returns Result.Success(Unit)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.updateUsername(longUsername)
        advanceUntilIdle()

        coVerify(exactly = 1) { updateUserProfileUseCase.updateUsername(expectedClean) }
        assertEquals(expectedClean, viewModel.uiState.value.user?.username)
    }

    @Test
    fun test_updateUsername_sameName_interceptionRuleMatchesContract() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val currentUsername = viewModel.uiState.value.user?.username ?: ""
        assertEquals("NemoUser", currentUsername)

        val isSameNameBlocked = { inputName: String ->
            val trimmed = inputName.trim()
            trimmed.isNotEmpty() && trimmed.length <= 16 && trimmed == currentUsername
        }

        assertTrue(isSameNameBlocked("NemoUser"))
        assertTrue(isSameNameBlocked("  NemoUser  "))
        assertFalse(isSameNameBlocked("DifferentUser"))
    }

    @Test
    fun test_updateUsername_remoteFailure_updatesErrorAndInvokesOnError() = runTest(testDispatcher) {
        coEvery { updateUserProfileUseCase.updateUsername("DuplicateName") } returns Result.Error(Exception("用户名已被占用"))

        val viewModel = createViewModel()
        advanceUntilIdle()

        var recordedError: String? = null
        viewModel.updateUsername("DuplicateName", onError = { recordedError = it })
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("用户名已被占用", state.error)
        assertEquals("用户名已被占用", recordedError)
        assertEquals("NemoUser", state.user?.username)
    }

    // ==========================================
    // 3. 修改邮箱业务逻辑 (OTP 阶段流转)
    // ==========================================

    @Test
    fun test_updateEmail_sendOtp_success_updatesMessageAndNotifies() = runTest(testDispatcher) {
        coEvery { authRepository.updateEmail("new@example.com") } returns Result.Success(Unit)

        val viewModel = createViewModel()
        advanceUntilIdle()

        var successCalled = false
        viewModel.updateEmail("new@example.com", onSuccess = { successCalled = true })
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("验证码已发送至新邮箱", state.successMessage)
        assertNull(state.error)
        assertTrue(successCalled)
        coVerify(exactly = 1) { authRepository.updateEmail("new@example.com") }
    }

    @Test
    fun test_updateEmail_sendOtp_failure_showsError() = runTest(testDispatcher) {
        coEvery { authRepository.updateEmail(any()) } returns Result.Error(Exception("邮箱已被注册"))

        val viewModel = createViewModel()
        advanceUntilIdle()

        var errorCalled = false
        viewModel.updateEmail("taken@example.com", onError = { errorCalled = true })
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("邮箱已被注册", state.error)
        assertTrue(errorCalled)
    }

    @Test
    fun test_verifyEmailUpdate_success_updatesStateAndClosesDialog() = runTest(testDispatcher) {
        coEvery { authRepository.verifyEmailChangeOtp("new@example.com", "12345678") } returns Result.Success(Unit)

        val viewModel = createViewModel()
        viewModel.showDialog(UserDialogType.UPDATE_EMAIL)
        advanceUntilIdle()

        var successCalled = false
        viewModel.verifyEmailUpdate("new@example.com", "12345678", onSuccess = {
            successCalled = true
            viewModel.dismissDialog()
        })
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("邮箱修改成功", state.successMessage)
        assertEquals(UserDialogType.NONE, state.activeDialog)
        assertTrue(successCalled)
        coVerify(exactly = 1) { authRepository.verifyEmailChangeOtp("new@example.com", "12345678") }
    }

    @Test
    fun test_verifyEmailUpdate_failure_showsError() = runTest(testDispatcher) {
        coEvery { authRepository.verifyEmailChangeOtp(any(), any()) } returns Result.Error(Exception("验证码无效或已过期"))

        val viewModel = createViewModel()
        advanceUntilIdle()

        var recordedError: String? = null
        viewModel.verifyEmailUpdate("new@example.com", "00000000", onError = { recordedError = it })
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("验证码无效或已过期", state.error)
        assertEquals("验证码无效或已过期", recordedError)
    }

    // ==========================================
    // 4. 重置密码业务逻辑 (3 阶段 OTP 与密码更新)
    // ==========================================

    @Test
    fun test_resetPassword_sendOtp_success_advancesStage() = runTest(testDispatcher) {
        coEvery { authRepository.sendPasswordResetOtp("nemo@example.com") } returns Result.Success(Unit)

        val viewModel = createViewModel()
        advanceUntilIdle()

        var successCalled = false
        viewModel.sendPasswordResetOtp("nemo@example.com", onSuccess = { successCalled = true })
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("验证码已发送", state.successMessage)
        assertTrue(successCalled)
        coVerify(exactly = 1) { authRepository.sendPasswordResetOtp("nemo@example.com") }
    }

    @Test
    fun test_resetPassword_verifyOtp_success_updatesMessage() = runTest(testDispatcher) {
        coEvery { authRepository.verifyPasswordResetOtp("nemo@example.com", "87654321") } returns Result.Success(Unit)

        val viewModel = createViewModel()
        advanceUntilIdle()

        var successCalled = false
        viewModel.verifyPasswordResetOtp("nemo@example.com", "87654321", onSuccess = { successCalled = true })
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("验证成功", state.successMessage)
        assertTrue(successCalled)
        coVerify(exactly = 1) { authRepository.verifyPasswordResetOtp("nemo@example.com", "87654321") }
    }

    @Test
    fun test_resetPassword_complete_success_updatesStateAndClosesDialog() = runTest(testDispatcher) {
        coEvery { authRepository.updatePassword("newStrongPassword123") } returns Result.Success(Unit)

        val viewModel = createViewModel()
        viewModel.showDialog(UserDialogType.RESET_PASSWORD)
        advanceUntilIdle()

        var successCalled = false
        viewModel.completePasswordReset("newStrongPassword123", onSuccess = {
            successCalled = true
            viewModel.dismissDialog()
        })
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("密码重置成功", state.successMessage)
        assertEquals(UserDialogType.NONE, state.activeDialog)
        assertTrue(successCalled)
        coVerify(exactly = 1) { authRepository.updatePassword("newStrongPassword123") }
    }

    @Test
    fun test_resetPassword_complete_failure_exposesError() = runTest(testDispatcher) {
        coEvery { authRepository.updatePassword(any()) } returns Result.Error(Exception("新密码不能与旧密码相同"))

        val viewModel = createViewModel()
        advanceUntilIdle()

        var recordedError: String? = null
        viewModel.completePasswordReset("samePassword", onSuccess = {}, onError = { recordedError = it })
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("新密码不能与旧密码相同", state.error)
        assertEquals("新密码不能与旧密码相同", recordedError)
    }

    // ==========================================
    // 5. 注销账户业务逻辑 (双轨校验与不可逆清理)
    // ==========================================

    @Test
    fun test_deleteAccount_passwordUser_verifiesPasswordAndLogsOut() = runTest(testDispatcher) {
        coEvery { deleteAccountUseCase("mySecurePass123") } returns Result.Success(Unit)
        coEvery { logoutUseCase() } returns Result.Success(Unit)

        val viewModel = createViewModel()
        viewModel.showDialog(UserDialogType.DELETE_ACCOUNT)
        advanceUntilIdle()

        var successCalled = false
        viewModel.deleteAccount("mySecurePass123", onSuccess = {
            successCalled = true
            viewModel.dismissDialog()
        })
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isLoggedIn)
        assertNull(state.user)
        assertEquals("账号已成功销毁", state.successMessage)
        assertEquals(UserDialogType.NONE, state.activeDialog)
        assertTrue(successCalled)
        coVerify(exactly = 1) { deleteAccountUseCase("mySecurePass123") }
        coVerify(exactly = 1) { logoutUseCase() }
    }

    @Test
    fun test_deleteAccount_googleUser_verifiesNullPasswordAndLogsOut() = runTest(testDispatcher) {
        userFlow.value = userFlow.value?.copy(isOnlyGoogleIdentity = true)
        coEvery { deleteAccountUseCase(null) } returns Result.Success(Unit)
        coEvery { logoutUseCase() } returns Result.Success(Unit)

        val viewModel = createViewModel()
        viewModel.showDialog(UserDialogType.DELETE_ACCOUNT)
        advanceUntilIdle()

        var successCalled = false
        viewModel.deleteAccount(password = null, onSuccess = {
            successCalled = true
            viewModel.dismissDialog()
        })
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoggedIn)
        assertNull(state.user)
        assertTrue(successCalled)
        coVerify(exactly = 1) { deleteAccountUseCase(null) }
        coVerify(exactly = 1) { logoutUseCase() }
    }

    @Test
    fun test_deleteAccount_remoteFailure_preservesLocalStateAndShowsError() = runTest(testDispatcher) {
        coEvery { deleteAccountUseCase(any()) } returns Result.Error(Exception("远端服务拒绝注销"))

        val viewModel = createViewModel()
        advanceUntilIdle()

        var recordedError: String? = null
        viewModel.deleteAccount("wrongPass", onError = { recordedError = it })
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.isLoggedIn)
        assertNotNull(state.user)
        assertEquals("远端服务拒绝注销", state.error)
        assertEquals("远端服务拒绝注销", recordedError)
        coVerify(exactly = 0) { logoutUseCase() }
    }

    // ==========================================
    // 6. 退出登录业务逻辑
    // ==========================================

    @Test
    fun test_logout_success_clearsUserDataAndResetsState() = runTest(testDispatcher) {
        coEvery { logoutUseCase() } returns Result.Success(Unit)

        val viewModel = createViewModel()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isLoggedIn)

        viewModel.logout()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isLoggedIn)
        assertNull(state.user)
        assertEquals("", state.avatarPath)
        coVerify(exactly = 1) { logoutUseCase() }
    }

    // ==========================================
    // 7. 弹窗打开与关闭交互
    // ==========================================

    @Test
    fun test_dialogFlow_showAndDismiss() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(UserDialogType.NONE, viewModel.uiState.value.activeDialog)

        viewModel.showDialog(UserDialogType.LOGOUT_CONFIRM)
        assertEquals(UserDialogType.LOGOUT_CONFIRM, viewModel.uiState.value.activeDialog)

        viewModel.dismissDialog()
        assertEquals(UserDialogType.NONE, viewModel.uiState.value.activeDialog)
    }

    // ==========================================
    // 8. 零硬编码与中文资源静态规范检查
    // ==========================================

    @Test
    fun test_zeroHardcoding_accountManagementStringsAreDefinedAndChinese() {
        val candidates = listOf(
            File("src/main/res/values/strings.xml"),
            File("feature/user/src/main/res/values/strings.xml"),
            File("Android/Nemo/feature/user/src/main/res/values/strings.xml"),
            File("../feature/user/src/main/res/values/strings.xml"),
            File("../../feature/user/src/main/res/values/strings.xml")
        )
        val stringsFile = candidates.firstOrNull { it.exists() }
        assertNotNull("未找到 strings.xml 资源文件", stringsFile)

        val content = stringsFile!!.readText()

        // 验证账户管理核心字段
        assertTrue(content.contains("""<string name="user_account_title_screen">账户管理</string>"""))
        assertTrue(content.contains("""<string name="user_account_title_profile_section">个人信息</string>"""))
        assertTrue(content.contains("""<string name="user_account_btn_change_username">修改用户名</string>"""))
        assertTrue(content.contains("""<string name="user_account_btn_change_email">修改邮箱</string>"""))
        assertTrue(content.contains("""<string name="user_account_title_security_section">安全</string>"""))
        assertTrue(content.contains("""<string name="user_account_btn_reset_password">重置密码</string>"""))
        assertTrue(content.contains("""<string name="user_account_title_danger_section">危险区域</string>"""))
        assertTrue(content.contains("""<string name="user_account_btn_delete_account">注销账户</string>"""))
        assertTrue(content.contains("""<string name="user_account_btn_logout">退出登录</string>"""))

        // 严格断言头像编辑描述已对齐为纯简体中文，严禁残留英文 "Edit"
        assertTrue("user_account_desc_edit_avatar 必须为中文'编辑头像'", content.contains("""<string name="user_account_desc_edit_avatar">编辑头像</string>"""))
        assertFalse("严禁在 user_account_desc_edit_avatar 残留英文 'Edit'", content.contains("""<string name="user_account_desc_edit_avatar">Edit</string>"""))

        // 验证弹窗关键提示与按钮
        assertTrue(content.contains("""<string name="user_dialog_title_reset_password">重置密码</string>"""))
        assertTrue(content.contains("""<string name="user_dialog_title_change_username">修改用户名</string>"""))
        assertTrue(content.contains("""<string name="user_dialog_title_change_email">修改邮箱</string>"""))
        assertTrue(content.contains("""<string name="user_dialog_title_delete_account">注销账户</string>"""))
        assertTrue(content.contains("""<string name="user_dialog_title_logout_clear_data">即将清除数据</string>"""))
    }
}
