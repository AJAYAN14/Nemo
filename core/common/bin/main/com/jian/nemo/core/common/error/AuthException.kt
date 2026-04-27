package com.jian.nemo.core.common.error

/**
 * Authentication related exception to decouple from specific SDK (Supabase / LeanCloud)
 */
class AuthException(
    override val message: String?,
    val code: Int = -1,
    cause: Throwable? = null
) : Exception(message, cause)
