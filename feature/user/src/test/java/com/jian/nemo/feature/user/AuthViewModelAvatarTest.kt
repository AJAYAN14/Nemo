package com.jian.nemo.feature.user

import android.content.Context
import com.jian.nemo.core.common.Result
import com.jian.nemo.core.domain.model.AvatarStorageFailure
import com.jian.nemo.core.domain.model.AvatarStorageResult
import com.jian.nemo.core.domain.model.CameraCaptureTarget
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
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelAvatarTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeAvatarStorage: FakeAvatarStorageRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var updateUserProfileUseCase: UpdateUserProfileUseCase
    private lateinit var context: Context

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeAvatarStorage = FakeAvatarStorageRepository()
        authRepository = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)
        updateUserProfileUseCase = mockk(relaxed = true)
        context = mockk(relaxed = true)

        every { authRepository.getUserFlow() } returns MutableStateFlow(
            User(id = "u1", username = "NemoUser", email = "nemo@example.com", sessionToken = "token_1")
        )
        every { authRepository.isSessionResolved } returns MutableStateFlow(true)
        every { settingsRepository.userAvatarPathFlow } returns MutableStateFlow("")
        every { settingsRepository.settingIconStyleFlow } returns MutableStateFlow(SettingIconStyle.SOFT_TRANSLUCENT)
        every { context.getString(R.string.user_auth_msg_avatar_upload_success) } returns "上传成功"
        every { context.getString(R.string.user_auth_err_avatar_upload_failed) } returns "上传失败"
        every { context.getString(R.string.user_auth_msg_avatar_update_success) } returns "更新成功"
        every { context.getString(R.string.user_auth_err_avatar_update_failed) } returns "更新失败"
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): AuthViewModel {
        return AuthViewModel(
            loginUseCase = mockk<LoginUseCase>(relaxed = true),
            registerUseCase = mockk<RegisterUseCase>(relaxed = true),
            logoutUseCase = mockk<LogoutUseCase>(relaxed = true),
            updateUserProfileUseCase = updateUserProfileUseCase,
            deleteAccountUseCase = mockk<DeleteAccountUseCase>(relaxed = true),
            getCurrentUserUseCase = mockk<GetCurrentUserUseCase>(relaxed = true),
            getUserFlowUseCase = GetUserFlowUseCase(authRepository),
            settingsRepository = settingsRepository,
            getUserAvatarPathUseCase = GetUserAvatarPathUseCase(settingsRepository),
            authRepository = authRepository,
            avatarStorageRepository = fakeAvatarStorage,
            supabaseClient = mockk<SupabaseClient>(relaxed = true),
            context = context
        )
    }

    @Test
    fun onGalleryAvatarSelected_success_uploadsAndClosesDialog() = runTest(testDispatcher) {
        val savedFile = File("avatar_1.jpg").absoluteFile
        val savedPath = savedFile.absolutePath
        fakeAvatarStorage.nextSaveResult = AvatarStorageResult.Success(savedPath)
        coEvery { updateUserProfileUseCase.uploadAvatar(savedFile) } returns Result.Success("https://cdn.example/u1.jpg")

        val viewModel = createViewModel()
        viewModel.showDialog(UserDialogType.UPDATE_AVATAR)
        advanceUntilIdle()

        viewModel.onGalleryAvatarSelected("content://media/picker/1")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isAvatarProcessing)
        assertEquals(UserDialogType.NONE, state.activeDialog)
        assertEquals(savedPath, state.avatarPath)
        assertEquals("上传成功", state.successMessage)
        assertNull(state.avatarError)
    }

    @Test
    fun onGalleryAvatarSelected_oversizeFailure_keepsDialogOpenAndExposesAvatarError() = runTest(testDispatcher) {
        val failure = AvatarStorageFailure.ImageTooLarge(21L * 1024L * 1024L, 20L * 1024L * 1024L)
        fakeAvatarStorage.nextSaveResult = AvatarStorageResult.Failure(failure)

        val viewModel = createViewModel()
        viewModel.showDialog(UserDialogType.UPDATE_AVATAR)
        advanceUntilIdle()

        viewModel.onGalleryAvatarSelected("content://media/picker/oversize")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isAvatarProcessing)
        assertEquals(UserDialogType.UPDATE_AVATAR, state.activeDialog)
        assertEquals(failure, state.avatarError)
    }

    @Test
    fun onGalleryAvatarSelected_blocksReentryAndDialogDismissWhileProcessing() = runTest(testDispatcher) {
        val gate = CompletableDeferred<AvatarStorageResult>()
        fakeAvatarStorage.saveDeferred = gate
        coEvery { updateUserProfileUseCase.uploadAvatar(any()) } returns Result.Success("https://cdn.example/u1.jpg")

        val viewModel = createViewModel()
        viewModel.showDialog(UserDialogType.UPDATE_AVATAR)
        advanceUntilIdle()

        viewModel.onGalleryAvatarSelected("content://media/picker/1")
        testDispatcher.scheduler.runCurrent()

        assertTrue(viewModel.uiState.value.isAvatarProcessing)

        viewModel.onGalleryAvatarSelected("content://media/picker/2")
        viewModel.dismissDialog()
        testDispatcher.scheduler.runCurrent()

        assertEquals(1, fakeAvatarStorage.saveCalls)
        assertEquals(UserDialogType.UPDATE_AVATAR, viewModel.uiState.value.activeDialog)

        gate.complete(AvatarStorageResult.Success("/tmp/avatar.jpg"))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isAvatarProcessing)
        assertEquals(UserDialogType.NONE, viewModel.uiState.value.activeDialog)
    }

    @Test
    fun cameraFlow_cancelCleansTempFile_andSuccessFinalizesAndUploads() = runTest(testDispatcher) {
        val tempPath = File("temp_1.jpg").absolutePath
        val finalFile = File("avatar_final.jpg").absoluteFile
        val finalPath = finalFile.absolutePath
        fakeAvatarStorage.nextCameraTarget = Result.Success(
            CameraCaptureTarget(filePath = tempPath, contentUri = "content://com.jian.nemo.fileprovider/cache/temp_1.jpg")
        )
        fakeAvatarStorage.nextFinalizeResult = AvatarStorageResult.Success(finalPath)
        coEvery { updateUserProfileUseCase.uploadAvatar(finalFile) } returns Result.Success("https://cdn.example/u1.jpg")

        val viewModel = createViewModel()
        viewModel.showDialog(UserDialogType.UPDATE_AVATAR)
        advanceUntilIdle()

        viewModel.onCameraCaptureRequested()
        advanceUntilIdle()
        assertEquals("content://com.jian.nemo.fileprovider/cache/temp_1.jpg", viewModel.uiState.value.pendingCameraLaunchUri)

        viewModel.onCameraLaunchConsumed()
        assertNull(viewModel.uiState.value.pendingCameraLaunchUri)

        viewModel.onCameraCaptureResult(success = false)
        advanceUntilIdle()
        assertEquals(listOf(tempPath), fakeAvatarStorage.cleanedPaths)

        viewModel.onCameraCaptureRequested()
        advanceUntilIdle()
        viewModel.onCameraLaunchConsumed()
        viewModel.onCameraCaptureResult(success = true)
        advanceUntilIdle()

        assertEquals(finalPath, viewModel.uiState.value.avatarPath)
        assertEquals(UserDialogType.NONE, viewModel.uiState.value.activeDialog)
    }

    private class FakeAvatarStorageRepository : AvatarStorageRepository {
        var saveCalls: Int = 0
        var saveDeferred: CompletableDeferred<AvatarStorageResult>? = null
        var nextSaveResult: AvatarStorageResult = AvatarStorageResult.Success("/files/default.jpg")
        var nextCameraTarget: Result<CameraCaptureTarget> = Result.Success(
            CameraCaptureTarget("/cache/temp.jpg", "content://test/temp.jpg")
        )
        var nextFinalizeResult: AvatarStorageResult = AvatarStorageResult.Success("/files/camera.jpg")
        val cleanedPaths = mutableListOf<String>()

        override suspend fun saveAvatarFromUri(uriString: String): AvatarStorageResult {
            saveCalls++
            return saveDeferred?.await() ?: nextSaveResult
        }

        override suspend fun prepareCameraCaptureTarget(): Result<CameraCaptureTarget> = nextCameraTarget

        override suspend fun finalizeCameraCapture(tempFilePath: String): AvatarStorageResult = nextFinalizeResult

        override suspend fun cleanupTempCapture(tempFilePath: String) {
            cleanedPaths += tempFilePath
        }
    }
}
