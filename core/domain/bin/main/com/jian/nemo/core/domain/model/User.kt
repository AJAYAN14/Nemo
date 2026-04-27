package com.jian.nemo.core.domain.model

/**
 * 用户领域模型
 */
data class User(
    val id: String,
    val username: String,
    val email: String?,
    val avatarUrl: String? = null,
    val sessionToken: String
)
