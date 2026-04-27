package com.jian.nemo.core.common

/**
 * A generic sealed class that represents the result of an operation.
 * Used throughout the domain layer for error handling.
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable) : Result<Nothing>()
    data object Loading : Result<Nothing>()

    val isSuccess: Boolean
        get() = this is Success

    val isError: Boolean
        get() = this is Error

    val isLoading: Boolean
        get() = this is Loading

    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }

    fun exceptionOrNull(): Throwable? = when (this) {
        is Error -> exception
        else -> null
    }
}

/**
 * Extension function to transform Success data
 */
inline fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> = when (this) {
    is Result.Success -> Result.Success(transform(data))
    is Result.Error -> Result.Error(exception)
    is Result.Loading -> Result.Loading
}

/**
 * Extension function to handle both success and error cases
 */
inline fun <T> Result<T>.fold(
    onSuccess: (T) -> Unit,
    onError: (Throwable) -> Unit
) {
    when (this) {
        is Result.Success -> onSuccess(data)
        is Result.Error -> onError(exception)
        is Result.Loading -> { /* do nothing */ }
    }
}

/**
 * Maps the successful result to another type
 * Alias for the existing map function to match documentation naming
 */
inline fun <T, R> Result<T>.mapSuccess(transform: (T) -> R): Result<R> = map(transform)

/**
 * Flat maps the result to another result, avoiding nested Result<Result<T>>
 */
inline fun <T, R> Result<T>.flatMap(transform: (T) -> Result<R>): Result<R> = when (this) {
    is Result.Success -> transform(data)
    is Result.Error -> Result.Error(exception)
    is Result.Loading -> Result.Loading
}

/**
 * Suspendable map function for async transformations
 */
suspend inline fun <T, R> Result<T>.suspendMap(
    crossinline transform: suspend (T) -> R
): Result<R> = when (this) {
    is Result.Success -> Result.Success(transform(data))
    is Result.Error -> Result.Error(exception)
    is Result.Loading -> Result.Loading
}

/**
 * Executes the given action if the result is successful
 * @return the original result for chaining
 */
inline fun <T> Result<T>.onSuccess(action: (T) -> Unit): Result<T> {
    if (this is Result.Success) {
        action(data)
    }
    return this
}

/**
 * Executes the given action if the result is an error
 * @return the original result for chaining
 */
inline fun <T> Result<T>.onError(action: (Throwable) -> Unit): Result<T> {
    if (this is Result.Error) {
        action(exception)
    }
    return this
}

/**
 * Executes the given action if the result is in loading state
 * @return the original result for chaining
 */
inline fun <T> Result<T>.onLoading(action: () -> Unit): Result<T> {
    if (this is Result.Loading) {
        action()
    }
    return this
}
