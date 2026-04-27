package com.jian.nemo.core.domain.model.sync

/**
 * 同步模式
 */
enum class SyncMode {
    /**
     * 双向同步（常规同步/同步逻辑）
     * 包含：拉取并合并远程变更 -> 推送本地变更
     */
    TWO_WAY,

    /**
     * 仅拉取（强制恢复逻辑）
     * 行为：忽略增量时间戳（全量拉取）并跳过推送阶段，防止本地错误数据污染云端
     */
    PULL_ONLY
}
