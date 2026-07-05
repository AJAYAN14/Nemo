package com.jian.nemo.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 用户实体
 *
 * 对应表: users
 * 用于存储用户信息（Supabase Auth 用户本地缓存）
 */
@Entity(tableName = "users")
data class UserEntity(
    /**
     * 用户ID (Supabase Auth 用户 UUID)
     */
    @PrimaryKey
    val id: String,

    /**
     * 用户名
     */
    val username: String,

    /**
     * 邮箱
     */
    val email: String,

    /**
     * Session access token（可选，用于本地标识已登录）
     */
    val sessionToken: String,

    /**
     * 头像 (Base64编码)
     */
    val avatar: String? = null,

    /**
     * 创建时间
     */
    val createdAt: Long = System.currentTimeMillis(),

    /**
     * 更新时间
     */
    val updatedAt: Long = System.currentTimeMillis(),

    /**
     * 是否绑定了 Google 账号
     */
    val hasGoogleLinked: Boolean = false,

    /**
     * 是否仅通过 Google 身份登录（没有其他登录方式）
     */
    val isOnlyGoogleIdentity: Boolean = false
)
