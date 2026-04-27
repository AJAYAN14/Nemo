package com.jian.nemo

import android.content.Context
import android.content.Intent
import android.util.Log
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jian.nemo.core.common.util.DownloadState
import com.jian.nemo.core.common.util.Downloader
import com.jian.nemo.core.domain.model.AppUpdateConfig
import com.jian.nemo.core.domain.repository.ConfigRepository
import com.jian.nemo.core.domain.repository.AudioRepository
import com.jian.nemo.core.domain.repository.AuthRepository
import com.jian.nemo.core.domain.model.User
import com.jian.nemo.core.domain.repository.TtsEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val configRepository: ConfigRepository,
    private val authRepository: AuthRepository,
    private val downloader: Downloader,
    private val audioRepository: AudioRepository
) : ViewModel() {

    companion object {
        private const val TAG = "MainViewModel"
    }

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _updateCheckEvents = kotlinx.coroutines.channels.Channel<UpdateCheckEvent>(kotlinx.coroutines.channels.Channel.BUFFERED)
    val updateCheckEvents = _updateCheckEvents.receiveAsFlow()

    init {
        viewModelScope.launch {
            authRepository.getUserFlow().collect { user ->
                _uiState.update { it.copy(currentUser = user) }
            }
        }

        viewModelScope.launch {
            audioRepository.ttsEvents.collect { event ->
                if (event is TtsEvent.GoogleTtsMissing) {
                    _uiState.update { it.copy(showGoogleTtsDialog = true) }
                }
            }
        }
    }

    fun checkUpdate(currentVersionCode: Int, isManual: Boolean = false) {
        viewModelScope.launch {
            try {
                val config = configRepository.getUpdateConfig()
                if (config != null && config.versionCode > currentVersionCode) {
                    _uiState.update { it.copy(updateConfig = config) }
                } else if (isManual) {
                    _updateCheckEvents.send(UpdateCheckEvent.NoUpdateAvailable)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (isManual) {
                    _updateCheckEvents.send(UpdateCheckEvent.Error(e.message ?: "检查更新失败"))
                }
            }
        }
    }

    fun startDownload(context: Context) {
        val config = _uiState.value.updateConfig ?: return
        val targetFile = File(context.cacheDir, "nemo2_update_${config.versionCode}.apk")

        if (targetFile.exists() && _uiState.value.isDownloaded) {
            installApk(context, targetFile)
            return
        }

            val appContext = context.applicationContext
            viewModelScope.launch {
                try {
                    // Show initial notification
                    com.jian.nemo.util.NotificationHelper.showProgress(appContext, 0)

                    downloader.download(config.downloadUrl, targetFile).collect { state ->
                        when (state) {
                            is DownloadState.Downloading -> {
                                val progress = (state.progress * 100).toInt()
                                com.jian.nemo.util.NotificationHelper.showProgress(appContext, progress)

                                _uiState.update {
                                    it.copy(
                                        isDownloading = true,
                                        downloadProgress = state.progress
                                    )
                                }
                            }
                            is DownloadState.Success -> {
                                com.jian.nemo.util.NotificationHelper.showSuccess(appContext, state.file)

                                _uiState.update {
                                    it.copy(
                                        isDownloading = false,
                                        isDownloaded = true,
                                        downloadProgress = 1f,
                                        apkFile = state.file
                                    )
                                }
                                installApk(appContext, state.file)
                            }
                            is DownloadState.Failed -> {
                                com.jian.nemo.util.NotificationHelper.cancel(appContext)
                                _uiState.update { it.copy(isDownloading = false) }
                                _updateCheckEvents.send(UpdateCheckEvent.Error("下载失败: ${state.error?.message ?: "未知错误"}"))
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MainViewModel", "下载更新异常", e)
                    com.jian.nemo.util.NotificationHelper.cancel(appContext)
                    _uiState.update { it.copy(isDownloading = false) }
                    _updateCheckEvents.send(UpdateCheckEvent.Error("下载异常: ${e.message}"))
                }
            }
        }

    fun downloadGoogleTts(context: Context) {
        val url = "http://lc-1fRZPgwY.cn-n1.lcfile.com/YEnJ98iak3w41gVUgxaiGEWXVkrVTHCa/Google%E8%AF%AD%E9%9F%B3%E8%AF%86%E5%88%AB%E5%92%8C%E8%AF%AD%E9%9F%B3%E5%90%88%E6%88%90.apk"
        val targetFile = File(context.cacheDir, "google_tts.apk")

        val appContext = context.applicationContext
        viewModelScope.launch {
            try {
                // Show initial notification
                com.jian.nemo.util.NotificationHelper.showProgress(appContext, 0, "正在下载 Google 语音服务...")

                downloader.download(url, targetFile).collect { state ->
                    when (state) {
                        is DownloadState.Downloading -> {
                            val progress = (state.progress * 100).toInt()
                            com.jian.nemo.util.NotificationHelper.showProgress(appContext, progress, "正在下载 Google 语音服务...")

                            _uiState.update {
                                it.copy(
                                    isDownloadingGoogleTts = true,
                                    googleTtsDownloadProgress = state.progress
                                )
                            }
                        }
                        is DownloadState.Success -> {
                            com.jian.nemo.util.NotificationHelper.showSuccess(
                                appContext,
                                state.file,
                                "Google 语音服务下载完成",
                                "点击立即安装"
                            )

                            _uiState.update {
                                it.copy(
                                    isDownloadingGoogleTts = false,
                                    googleTtsDownloadProgress = 1f
                                )
                            }
                            installApk(appContext, state.file)
                            dismissGoogleTtsDialog()
                        }
                        is DownloadState.Failed -> {
                            com.jian.nemo.util.NotificationHelper.cancel(appContext)
                            _uiState.update { it.copy(isDownloadingGoogleTts = false) }
                            _updateCheckEvents.send(UpdateCheckEvent.Error("下载失败: ${state.error?.message ?: "未知错误"}"))
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "下载 Google TTS 异常", e)
                com.jian.nemo.util.NotificationHelper.cancel(appContext)
                _uiState.update { it.copy(isDownloadingGoogleTts = false) }
                _updateCheckEvents.send(UpdateCheckEvent.Error("下载异常: ${e.message}"))
            }
        }
    }

    fun installApk(context: Context, file: File? = _uiState.value.apkFile) {
        if (file == null || !file.exists()) {
            viewModelScope.launch {
                _updateCheckEvents.send(UpdateCheckEvent.Error("安装失败：找不到文件"))
            }
            return
        }

        val appContext = context.applicationContext
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    FileProvider.getUriForFile(appContext, "${appContext.packageName}.fileprovider", file)
                } else {
                    Uri.fromFile(file)
                }
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            appContext.startActivity(intent)
        } catch (e: Exception) {
            Log.e("MainViewModel", "安装过程捕获到异常", e)
            viewModelScope.launch {
                _updateCheckEvents.send(UpdateCheckEvent.Error("安装程序启动失败: ${e.message}"))
            }
        }
    }

    fun dismissUpdate() {
        _uiState.update { it.copy(updateConfig = null) }
    }

    fun dismissGoogleTtsDialog() {
        _uiState.update { it.copy(showGoogleTtsDialog = false) }
    }
}

data class MainUiState(
    val updateConfig: AppUpdateConfig? = null,
    val isDownloading: Boolean = false,
    val isDownloaded: Boolean = false,
    val downloadProgress: Float = 0f,
    val apkFile: File? = null,
    val showGoogleTtsDialog: Boolean = false,
    val isDownloadingGoogleTts: Boolean = false,
    val googleTtsDownloadProgress: Float = 0f,
    val currentUser: User? = null
)

sealed interface UpdateCheckEvent {
    data object NoUpdateAvailable : UpdateCheckEvent
    data class Error(val message: String) : UpdateCheckEvent
}
