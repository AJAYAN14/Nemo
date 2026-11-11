package com.jian.nemo.core.data.repository

import android.content.Context
import com.jian.nemo.core.domain.model.AppUpdateConfig
import com.jian.nemo.core.domain.repository.ConfigRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ConfigRepository 的 Supabase 实现
 */
@Singleton
class ConfigRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient,
    @ApplicationContext private val context: Context
) : ConfigRepository {

    companion object {
        private const val PREFS_NAME = "nemo_config_cache"
        private const val KEY_CONFIG_JSON = "cached_update_config_json"
        private const val KEY_CACHED_FOR_VERSION = "cached_for_version_code"
    }

    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }
    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    @Volatile
    private var memoryCache: AppUpdateConfig? = null

    override suspend fun getUpdateConfig(forceRefresh: Boolean): AppUpdateConfig? {
        if (!forceRefresh) {
            memoryCache?.let { return it }
        }
        return mutex.withLock {
            if (!forceRefresh) {
                memoryCache?.let { return@withLock it }
            }
            withContext(Dispatchers.IO) {
                try {
                    // 从 app_config 表中获取配置，按 version_code 倒序排列取最新一条
                    val config = supabaseClient.postgrest["app_config"]
                        .select {
                            order("version_code", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                            limit(1)
                        }
                        .decodeSingleOrNull<AppUpdateConfig>()
                    if (config != null) {
                        memoryCache = config
                        saveDiskCache(config)
                    }
                    config
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
        }
    }

    override suspend fun getCachedUpdateConfig(): AppUpdateConfig? {
        memoryCache?.let { return it }
        return withContext(Dispatchers.IO) {
            loadValidDiskCache()
        }
    }

    private fun saveDiskCache(config: AppUpdateConfig) {
        try {
            val encoded = json.encodeToString(AppUpdateConfig.serializer(), config)
            prefs.edit()
                .putString(KEY_CONFIG_JSON, encoded)
                .putInt(KEY_CACHED_FOR_VERSION, getCurrentVersionCode())
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadValidDiskCache(): AppUpdateConfig? {
        val raw = prefs.getString(KEY_CONFIG_JSON, null) ?: return null
        return try {
            val cached = json.decodeFromString(AppUpdateConfig.serializer(), raw)
            val currentVersion = getCurrentVersionCode()
            val cachedForVersion = prefs.getInt(KEY_CACHED_FOR_VERSION, -1)
            // 未发生应用版本升级或缓存已覆盖当前版本时，判定本地缓存有效
            if (cachedForVersion == currentVersion || cached.versionCode >= currentVersion) {
                cached
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun getCurrentVersionCode(): Int {
        return try {
            val packageInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
            } else {
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode
            }
        } catch (e: Exception) {
            1 // 默认降级版本号
        }
    }
}
