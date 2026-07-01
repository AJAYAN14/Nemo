package com.jian.nemo

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Nemo 2.0 的应用程序类
 * 负责在整个应用中启用 Hilt 依赖注入
 */
@HiltAndroidApp
class NemoApplication : Application() {


    override fun onCreate() {
        super.onCreate()
        // Supabase 通过 Hilt (SupabaseModule) 进行初始化；此处无需进行 SDK 初始化。
    }
}
