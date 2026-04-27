package com.jian.nemo.core.common.ext

import com.jian.nemo.core.common.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/**
 * Converts a Flow<T> to Flow<Result<T>>, wrapping emissions in Result.Success,
 * starting with Result.Loading, and catching any exceptions as Result.Error.
 *
 * This is the primary way to convert repository Flow emissions into a format
 * suitable for ViewModel consumption with proper error handling.
 *
 * Example usage:
 * ```
 * class GetWordsUseCase {
 *     operator fun invoke(): Flow<Result<List<Word>>> {
 *         return wordRepository.getWords()
 *             .asResult() // Converts Flow<List<Word>> to Flow<Result<List<Word>>>
 *     }
 * }
 * ```
 *
 * @return Flow<Result<T>> with automatic error handling and loading state
 */
fun <T> Flow<T>.asResult(): Flow<Result<T>> {
    return this
        .map<T, Result<T>> { Result.Success(it) }
        .onStart { emit(Result.Loading) }
        .catch { emit(Result.Error(it)) }
}
