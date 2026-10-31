package com.jian.nemo.feature.settings.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.jian.nemo.core.domain.repository.AppIconStyle

/**
 * 桌面启动图标管理工具
 *
 * 采用方案 A：紧凑原子同步替换
 * 启用新别名并立即禁用旧别名，手机桌面 Launcher 会在同一个包状态变更通知中
 * 判定为“原地就地更新”，绝不改变应用在手机桌面上的原位置与文件夹排版。
 */
object AppIconManager {
    private const val TAG = "AppIconManager"
    private const val PACKAGE_NAME = "com.jian.nemo"
    private const val ALIAS_DEFAULT = "com.jian.nemo.MainActivityDefault"
    private const val ALIAS_MONOCHROME = "com.jian.nemo.MainActivityMonochrome"
    private const val ALIAS_FROG_CHEER = "com.jian.nemo.MainActivityFrogCheer"
    private const val ALIAS_FROG_NINJA = "com.jian.nemo.MainActivityFrogNinja"

    fun switchIcon(context: Context, targetStyle: AppIconStyle) {
        val packageManager = context.packageManager
        val components = mapOf(
            AppIconStyle.DEFAULT to ComponentName(PACKAGE_NAME, ALIAS_DEFAULT),
            AppIconStyle.MONOCHROME to ComponentName(PACKAGE_NAME, ALIAS_MONOCHROME),
            AppIconStyle.FROG_CHEER to ComponentName(PACKAGE_NAME, ALIAS_FROG_CHEER),
            AppIconStyle.FROG_NINJA to ComponentName(PACKAGE_NAME, ALIAS_FROG_NINJA)
        )

        val targetComponent = components[targetStyle] ?: return
        val disableComponents = components.filterKeys { it != targetStyle }.values

        try {
            // 同步紧凑原子操作：先启用目标组件，并立即禁用旧组件
            // 手机桌面在单次广播处理中将其识别为“就地替换”，牢牢锁住桌面原位置
            packageManager.setComponentEnabledSetting(
                targetComponent,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )

            for (component in disableComponents) {
                packageManager.setComponentEnabledSetting(
                    component,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            }
            Log.d(TAG, "Successfully atomic-switched app icon to $targetStyle (locked in place)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to atomic-switch app icon to $targetStyle", e)
        }
    }
}
