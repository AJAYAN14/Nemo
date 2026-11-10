package com.jian.nemo.feature.settings.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.jian.nemo.core.domain.repository.AppIconStyle

/**
 * 桌面启动图标管理工具
 *
 * 紧凑原子同步替换：启用目标别名并立即禁用其余别名，
 * 触发系统 Launcher 就地刷新，锁定桌面排版位置。
 */
object AppIconManager {
    private const val TAG = "AppIconManager"
    private const val PACKAGE_NAME = "com.jian.nemo"

    private const val ALIAS_DEFAULT = "com.jian.nemo.MainActivityDefault"
    private const val ALIAS_BLACK = "com.jian.nemo.MainActivityBlack"
    private const val ALIAS_GREEN = "com.jian.nemo.MainActivityGreen"
    private const val ALIAS_ORANGE = "com.jian.nemo.MainActivityOrange"
    private const val ALIAS_PINK = "com.jian.nemo.MainActivityPink"
    private const val ALIAS_PURPLE = "com.jian.nemo.MainActivityPurple"
    private const val ALIAS_RED = "com.jian.nemo.MainActivityRed"
    private const val ALIAS_YELLOW = "com.jian.nemo.MainActivityYellow"

    // 历史遗留别名白名单（用于平滑迁移并自动禁用）
    private val LEGACY_ALIASES = listOf(
        "com.jian.nemo.MainActivityMonochrome",
        "com.jian.nemo.MainActivityFrogCheer",
        "com.jian.nemo.MainActivityFrogNinja"
    )

    fun switchIcon(context: Context, targetStyle: AppIconStyle) {
        val packageManager = context.packageManager
        val components = mapOf(
            AppIconStyle.DEFAULT to ComponentName(PACKAGE_NAME, ALIAS_DEFAULT),
            AppIconStyle.BLACK to ComponentName(PACKAGE_NAME, ALIAS_BLACK),
            AppIconStyle.GREEN to ComponentName(PACKAGE_NAME, ALIAS_GREEN),
            AppIconStyle.ORANGE to ComponentName(PACKAGE_NAME, ALIAS_ORANGE),
            AppIconStyle.PINK to ComponentName(PACKAGE_NAME, ALIAS_PINK),
            AppIconStyle.PURPLE to ComponentName(PACKAGE_NAME, ALIAS_PURPLE),
            AppIconStyle.RED to ComponentName(PACKAGE_NAME, ALIAS_RED),
            AppIconStyle.YELLOW to ComponentName(PACKAGE_NAME, ALIAS_YELLOW)
        )

        val targetComponent = components[targetStyle] ?: return
        val disableComponents = components.filterKeys { it != targetStyle }.values.toMutableList()
        // 同时追加历史遗留别名，确保彻底清理
        LEGACY_ALIASES.forEach { alias ->
            disableComponents.add(ComponentName(PACKAGE_NAME, alias))
        }

        try {
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
            Log.d(TAG, "Successfully atomic-switched app icon to $targetStyle")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to atomic-switch app icon to $targetStyle", e)
        }
    }
}
