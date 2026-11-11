package com.jian.nemo.feature.settings

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jian.nemo.core.domain.model.AppUpdateConfig
import com.jian.nemo.core.domain.repository.ConfigRepository
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 关于界面 UI 状态
 */
data class AboutUiState(
    val isLoadingLog: Boolean = true,
    val updateConfig: AppUpdateConfig? = null,
    val errorMessage: String? = null,
    val checkUpdateOnStartup: Boolean = true,
    val versionName: String = "",
    val versionCode: Long = 1L,
    val isMetadataReady: Boolean = false
)

internal data class AboutLocalMetadata(
    val versionName: String,
    val versionCode: Long,
    val checkUpdateOnStartup: Boolean
)

internal interface AboutMetadataSource {
    fun getCachedMetadata(): AboutLocalMetadata?
    suspend fun loadMetadata(): AboutLocalMetadata
    suspend fun setCheckUpdateOnStartup(enabled: Boolean)
}

internal class AboutMetadataCacheStore {
    private var cachedAppVersion: Pair<String, Long>? = null
    private var cachedMetadata: AboutLocalMetadata? = null
    private var lastUserMutationValue: Boolean? = null
    private var pendingWrites: Int = 0
    private var mutationVersion: Long = 0L

    @Synchronized
    fun getCachedMetadata(): AboutLocalMetadata? = cachedMetadata

    @Synchronized
    fun getCachedAppVersion(): Pair<String, Long>? = cachedAppVersion

    @Synchronized
    fun snapshotReadGuard(): Pair<Long, Boolean> = mutationVersion to (pendingWrites > 0)

    @Synchronized
    fun beginUserMutation(enabled: Boolean): Long {
        mutationVersion++
        pendingWrites++
        lastUserMutationValue = enabled
        cachedMetadata = cachedMetadata?.copy(checkUpdateOnStartup = enabled)
        return mutationVersion
    }

    @Synchronized
    fun latestMutationValueOr(fallback: Boolean): Boolean = lastUserMutationValue ?: fallback

    @Synchronized
    fun currentMutationVersion(): Long = mutationVersion

    @Synchronized
    fun finishUserMutation() {
        if (pendingWrites > 0) {
            pendingWrites--
        }
    }

    @Synchronized
    fun commitLoaded(
        startVersion: Long,
        hadPendingWriteAtStart: Boolean,
        appVersion: Pair<String, Long>,
        loadedStartupCheck: Boolean
    ): AboutLocalMetadata {
        cachedAppVersion = appVersion
        val shouldPreferUserMutation =
            hadPendingWriteAtStart || mutationVersion != startVersion || pendingWrites > 0
        val effectiveStartupCheck = if (shouldPreferUserMutation) {
            lastUserMutationValue ?: cachedMetadata?.checkUpdateOnStartup ?: loadedStartupCheck
        } else {
            loadedStartupCheck
        }
        return AboutLocalMetadata(
            versionName = appVersion.first,
            versionCode = appVersion.second,
            checkUpdateOnStartup = effectiveStartupCheck
        ).also {
            cachedMetadata = it
        }
    }
}

internal class AndroidAboutMetadataSource internal constructor(
    private val loadVersionInfo: () -> Pair<String, Long>,
    private val readStartupCheck: () -> Boolean,
    private val writeStartupCheck: (Boolean) -> Unit,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val cacheStore: AboutMetadataCacheStore = processCacheStore
) : AboutMetadataSource {

    constructor(
        context: Context,
        ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    ) : this(
        loadVersionInfo = { readAppVersionInfo(context.applicationContext) },
        readStartupCheck = {
            context.applicationContext
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_CHECK_UPDATE_ON_STARTUP, true)
        },
        writeStartupCheck = { enabled ->
            context.applicationContext
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_CHECK_UPDATE_ON_STARTUP, enabled)
                .apply()
        },
        ioDispatcher = ioDispatcher,
        cacheStore = processCacheStore
    )

    private val writeMutex = Mutex()

    override fun getCachedMetadata(): AboutLocalMetadata? = cacheStore.getCachedMetadata()

    override suspend fun loadMetadata(): AboutLocalMetadata = withContext(ioDispatcher) {
        val (startVersion, hadPendingAtStart) = cacheStore.snapshotReadGuard()
        val appVersion = cacheStore.getCachedAppVersion() ?: loadVersionInfo()
        val loadedStartupCheck = readStartupCheck()
        cacheStore.commitLoaded(
            startVersion = startVersion,
            hadPendingWriteAtStart = hadPendingAtStart,
            appVersion = appVersion,
            loadedStartupCheck = loadedStartupCheck
        )
    }

    override suspend fun setCheckUpdateOnStartup(enabled: Boolean) {
        val mutationSeq = cacheStore.beginUserMutation(enabled)
        withContext(NonCancellable + ioDispatcher) {
            try {
                writeMutex.withLock {
                    if (cacheStore.currentMutationVersion() == mutationSeq) {
                        val finalValue = cacheStore.latestMutationValueOr(enabled)
                        writeStartupCheck(finalValue)
                    }
                }
            } finally {
                cacheStore.finishUserMutation()
            }
        }
    }

    companion object {
        private const val PREF_NAME = "nemo_app_prefs"
        private const val KEY_CHECK_UPDATE_ON_STARTUP = "check_update_on_startup"

        private val processCacheStore = AboutMetadataCacheStore()

        private fun readAppVersionInfo(appContext: Context): Pair<String, Long> {
            return try {
                val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    appContext.packageManager.getPackageInfo(
                        appContext.packageName,
                        PackageManager.PackageInfoFlags.of(0L)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    appContext.packageManager.getPackageInfo(appContext.packageName, 0)
                }
                val versionName = packageInfo.versionName ?: "Unknown"
                val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode.toLong()
                }
                versionName to versionCode
            } catch (_: Exception) {
                "Unknown" to 1L
            }
        }

        suspend fun preload(context: Context, ioDispatcher: CoroutineDispatcher = Dispatchers.IO) {
            AndroidAboutMetadataSource(context, ioDispatcher).loadMetadata()
        }
    }
}

/**
 * 关于界面 ViewModel
 *
 * 负责异步读取本地版本与更新开关配置，并从 ConfigRepository 获取应用版本更新配置与更新日志。
 */
@HiltViewModel
class AboutViewModel internal constructor(
    private val configRepository: ConfigRepository,
    settingsRepository: SettingsRepository,
    private val metadataSource: AboutMetadataSource,
    private val ioDispatcher: CoroutineDispatcher,
    private val stringProvider: (Int) -> String,
    coroutineScopeOverride: CoroutineScope? = null
) : ViewModel() {

    @Inject
    constructor(
        configRepository: ConfigRepository,
        settingsRepository: SettingsRepository,
        application: android.app.Application
    ) : this(
        configRepository = configRepository,
        settingsRepository = settingsRepository,
        metadataSource = AndroidAboutMetadataSource(application, Dispatchers.IO),
        ioDispatcher = Dispatchers.IO,
        stringProvider = { resId -> application.getString(resId) }
    )

    private val activeScope: CoroutineScope = coroutineScopeOverride ?: viewModelScope

    val settingIconStyle: StateFlow<SettingIconStyle> = settingsRepository.settingIconStyleFlow
        .stateIn(
            scope = activeScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SettingIconStyle.VIBRANT_SOLID
        )

    private val _uiState = MutableStateFlow(createInitialUiState())
    val uiState: StateFlow<AboutUiState> = _uiState.asStateFlow()

    @Volatile
    private var hasUserUpdatedStartupCheck = false

    init {
        loadLocalMetadata()
        loadUpdateLog()
    }

    private fun createInitialUiState(): AboutUiState {
        val cached = metadataSource.getCachedMetadata()
        return if (cached != null) {
            AboutUiState(
                checkUpdateOnStartup = cached.checkUpdateOnStartup,
                versionName = cached.versionName,
                versionCode = cached.versionCode,
                isMetadataReady = true
            )
        } else {
            AboutUiState()
        }
    }

    private fun loadLocalMetadata() {
        activeScope.launch(ioDispatcher) {
            val metadata = metadataSource.loadMetadata()
            _uiState.update { state ->
                state.copy(
                    checkUpdateOnStartup = if (hasUserUpdatedStartupCheck) state.checkUpdateOnStartup else metadata.checkUpdateOnStartup,
                    versionName = metadata.versionName,
                    versionCode = metadata.versionCode,
                    isMetadataReady = true
                )
            }
        }
    }

    fun setCheckUpdateOnStartup(enabled: Boolean) {
        hasUserUpdatedStartupCheck = true
        _uiState.update { it.copy(checkUpdateOnStartup = enabled) }
        // 同步起跑并由数据源内部 NonCancellable 护航，防止快速返回退出界面丢失写入
        activeScope.launch(start = CoroutineStart.UNDISPATCHED) {
            metadataSource.setCheckUpdateOnStartup(enabled)
        }
    }

    private fun loadUpdateLog() {
        activeScope.launch {
            val cached = configRepository.getCachedUpdateConfig()
            if (cached != null) {
                _uiState.update {
                    it.copy(
                        isLoadingLog = false,
                        updateConfig = cached,
                        errorMessage = null
                    )
                }
                return@launch
            }
            fetchUpdateLog(forceRefresh = false)
        }
    }

    fun fetchUpdateLog(forceRefresh: Boolean = false) {
        activeScope.launch {
            _uiState.update { it.copy(isLoadingLog = true, errorMessage = null) }
            try {
                val config = configRepository.getUpdateConfig(forceRefresh = forceRefresh)
                _uiState.update {
                    it.copy(
                        isLoadingLog = false,
                        updateConfig = config,
                        errorMessage = if (config == null) stringProvider(R.string.settings_about_label_no_log) else null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingLog = false,
                        errorMessage = e.message ?: stringProvider(R.string.settings_about_label_fetch_failed)
                    )
                }
            }
        }
    }

    companion object {
        /**
         * 在设置页后台错峰预热版本号与更新开关元数据，消除进入关于页首帧的同步读取。
         */
        suspend fun preloadMetadata(context: Context) {
            AndroidAboutMetadataSource.preload(context)
        }
    }
}
