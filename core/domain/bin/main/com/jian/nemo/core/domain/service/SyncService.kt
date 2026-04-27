package com.jian.nemo.core.domain.service

/**
 * 同步服务接口
 * 负责响应业务事件并决策是否触发同步
 */
interface SyncService {
    /** 学习完成 */
    fun onLearningCompleted()

    /** 测试完成 */
    fun onTestCompleted()

    /** 应用进入前台 */
    fun onAppForeground()

    /** 应用进入后台 */
    fun onAppBackground()
}
