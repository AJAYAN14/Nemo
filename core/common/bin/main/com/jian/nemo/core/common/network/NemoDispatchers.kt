package com.jian.nemo.core.common.network

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class Dispatcher(val nemoDispatcher: NemoDispatchers)

enum class NemoDispatchers {
    Default,
    Main,
    IO,
}
