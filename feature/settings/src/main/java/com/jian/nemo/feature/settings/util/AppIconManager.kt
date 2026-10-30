package com.jian.nemo.feature.settings.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.jian.nemo.core.domain.repository.AppIconStyle

/**
 * 桌面启动图标管理工具
 *
 * 通过动态启停 activity-alias 实现桌面图标平滑切换
 */
object AppIconManager {
    private const val TAG = "AppIconManager"
    private const val PACKAGE_NAME = "com.jian.nemo"
    private const val ALIAS_DEFAULT = "com.jian.nemo.MainActivityDefault"
    private const val ALIAS_MONOCHROME = "com.jian.nemo.MainActivityMonochrome"

    fun switchIcon(context: Context, targetStyle: AppIconStyle) {
        val packageManager = context.packageManager
        val defaultComponent = ComponentName(PACKAGE_NAME, ALIAS_DEFAULT)
        val monochromeComponent = ComponentName(PACKAGE_NAME, ALIAS_MONOCHROME)

        val (enableComponent, disableComponent) = when (targetStyle) {
            AppIconStyle.DEFAULT -> defaultComponent to monochromeComponent
            AppIconStyle.MONOCHROME -> monochromeComponent to defaultComponent
        }

        try {
            // 先启用目标组件，防止瞬态无可用别名
            packageManager.setComponentEnabledSetting(
                enableComponent,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )

            // 再禁用原组件
            packageManager.setComponentEnabledSetting(
                disableComponent,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
            Log.d(TAG, "Successfully switched app icon to $targetStyle")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to switch app icon to $targetStyle", e)
        }
    }
}
