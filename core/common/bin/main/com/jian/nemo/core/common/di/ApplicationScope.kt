package com.jian.nemo.core.common.di

import javax.inject.Qualifier

/**
 * 应用级别的 CoroutineScope 限定符
 * 用于注入全局的、生命周期与应用一致的 CoroutineScope
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
