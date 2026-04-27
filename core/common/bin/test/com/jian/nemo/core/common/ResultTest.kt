package com.jian.nemo.core.common

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for the Result class and its extension functions
 */
class ResultTest {

    @Test
    fun `Success result should have correct properties`() {
        val result: Result<Int> = Result.Success(42)

        assertTrue(result.isSuccess)
        assertFalse(result.isError)
        assertFalse(result.isLoading)
        assertEquals(42, result.getOrNull())
        assertNull(result.exceptionOrNull())
    }

    @Test
    fun `Error result should have correct properties`() {
        val exception = IllegalArgumentException("Test error")
        val result: Result<Int> = Result.Error(exception)

        assertFalse(result.isSuccess)
        assertTrue(result.isError)
        assertFalse(result.isLoading)
        assertNull(result.getOrNull())
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `Loading result should have correct properties`() {
        val result: Result<Int> = Result.Loading

        assertFalse(result.isSuccess)
        assertFalse(result.isError)
        assertTrue(result.isLoading)
        assertNull(result.getOrNull())
        assertNull(result.exceptionOrNull())
    }

    @Test
    fun `map should transform success value`() {
        val result: Result<Int> = Result.Success(10)
        val mapped = result.map { it * 2 }

        assertTrue(mapped.isSuccess)
        assertEquals(20, mapped.getOrNull())
    }

    @Test
    fun `map should preserve error`() {
        val exception = IllegalArgumentException("Test")
        val result: Result<Int> = Result.Error(exception)
        val mapped = result.map { it * 2 }

        assertTrue(mapped.isError)
        assertEquals(exception, mapped.exceptionOrNull())
    }

    @Test
    fun `map should preserve loading`() {
        val result: Result<Int> = Result.Loading
        val mapped = result.map { it * 2 }

        assertTrue(mapped.isLoading)
    }

    @Test
    fun `mapSuccess should work as alias for map`() {
        val result: Result<String> = Result.Success("hello")
        val mapped = result.mapSuccess { it.uppercase() }

        assertTrue(mapped.isSuccess)
        assertEquals("HELLO", mapped.getOrNull())
    }

    @Test
    fun `flatMap should unwrap nested result`() {
        val result: Result<Int> = Result.Success(10)
        val flatMapped = result.flatMap { value ->
            if (value > 5) Result.Success(value * 2)
            else Result.Error(IllegalArgumentException("Too small"))
        }

        assertTrue(flatMapped.isSuccess)
        assertEquals(20, flatMapped.getOrNull())
    }

    @Test
    fun `flatMap should return error from transform`() {
        val result: Result<Int> = Result.Success(3)
        val flatMapped = result.flatMap { value ->
            if (value > 5) Result.Success(value * 2)
            else Result.Error(IllegalArgumentException("Too small"))
        }

        assertTrue(flatMapped.isError)
    }

    @Test
    fun `flatMap should preserve original error`() {
        val exception = IllegalArgumentException("Original error")
        val result: Result<Int> = Result.Error(exception)
        val flatMapped = result.flatMap { Result.Success(it * 2) }

        assertTrue(flatMapped.isError)
        assertEquals(exception, flatMapped.exceptionOrNull())
    }

    @Test
    fun `onSuccess should execute action for success`() {
        var executed = false
        var value = 0

        val result: Result<Int> = Result.Success(42)
        result.onSuccess {
            executed = true
            value = it
        }

        assertTrue(executed)
        assertEquals(42, value)
    }

    @Test
    fun `onSuccess should not execute for error`() {
        var executed = false

        val result: Result<Int> = Result.Error(IllegalArgumentException())
        result.onSuccess { executed = true }

        assertFalse(executed)
    }

    @Test
    fun `onError should execute action for error`() {
        var executed = false
        var caughtException: Throwable? = null

        val exception = IllegalArgumentException("Test")
        val result: Result<Int> = Result.Error(exception)
        result.onError {
            executed = true
            caughtException = it
        }

        assertTrue(executed)
        assertEquals(exception, caughtException)
    }

    @Test
    fun `onError should not execute for success`() {
        var executed = false

        val result: Result<Int> = Result.Success(42)
        result.onError { executed = true }

        assertFalse(executed)
    }

    @Test
    fun `onLoading should execute action for loading`() {
        var executed = false

        val result: Result<Int> = Result.Loading
        result.onLoading { executed = true }

        assertTrue(executed)
    }

    @Test
    fun `onLoading should not execute for success`() {
        var executed = false

        val result: Result<Int> = Result.Success(42)
        result.onLoading { executed = true }

        assertFalse(executed)
    }

    @Test
    fun `chaining onSuccess and onError should work correctly`() {
        var successExecuted = false
        var errorExecuted = false

        val result: Result<Int> = Result.Success(42)
        result
            .onSuccess { successExecuted = true }
            .onError { errorExecuted = true }

        assertTrue(successExecuted)
        assertFalse(errorExecuted)
    }

    @Test
    fun `fold should execute onSuccess for success result`() {
        var successValue = 0
        var errorOccurred = false

        val result: Result<Int> = Result.Success(42)
        result.fold(
            onSuccess = { successValue = it },
            onError = { errorOccurred = true }
        )

        assertEquals(42, successValue)
        assertFalse(errorOccurred)
    }

    @Test
    fun `fold should execute onError for error result`() {
        var successExecuted = false
        var caughtException: Throwable? = null

        val exception = IllegalArgumentException("Test")
        val result: Result<Int> = Result.Error(exception)
        result.fold(
            onSuccess = { successExecuted = true },
            onError = { caughtException = it }
        )

        assertFalse(successExecuted)
        assertEquals(exception, caughtException)
    }

    @Test
    fun `fold should do nothing for loading result`() {
        var successExecuted = false
        var errorExecuted = false

        val result: Result<Int> = Result.Loading
        result.fold(
            onSuccess = { successExecuted = true },
            onError = { errorExecuted = true }
        )

        assertFalse(successExecuted)
        assertFalse(errorExecuted)
    }
}
