package com.jian.nemo.core.common.ext

import com.jian.nemo.core.common.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for Flow extension functions
 */
class FlowExtTest {

    @Test
    fun `asResult should emit Loading first`() = runTest {
        val flow: Flow<Int> = flow {
            emit(42)
        }

        val results = flow.asResult().toList()

        assertTrue("First emission should be Loading", results[0] is Result.Loading)
    }

    @Test
    fun `asResult should emit Success for successful emission`() = runTest {
        val flow: Flow<Int> = flow {
            emit(42)
        }

        val results = flow.asResult().toList()

        assertEquals(2, results.size)
        assertTrue(results[0] is Result.Loading)
        assertTrue(results[1] is Result.Success)
        assertEquals(42, (results[1] as Result.Success).data)
    }

    @Test
    fun `asResult should emit multiple Success values`() = runTest {
        val flow: Flow<Int> = flow {
            emit(1)
            emit(2)
            emit(3)
        }

        val results = flow.asResult().toList()

        assertEquals(4, results.size)
        assertTrue(results[0] is Result.Loading)
        assertTrue(results[1] is Result.Success)
        assertTrue(results[2] is Result.Success)
        assertTrue(results[3] is Result.Success)
        assertEquals(1, (results[1] as Result.Success).data)
        assertEquals(2, (results[2] as Result.Success).data)
        assertEquals(3, (results[3] as Result.Success).data)
    }

    @Test
    fun `asResult should catch exceptions and emit Error`() = runTest {
        val exception = IllegalArgumentException("Test exception")
        val flow: Flow<Int> = flow {
            throw exception
        }

        val results = flow.asResult().toList()

        assertEquals(2, results.size)
        assertTrue(results[0] is Result.Loading)
        assertTrue(results[1] is Result.Error)
        assertEquals(exception, (results[1] as Result.Error).exception)
    }

    @Test
    fun `asResult should catch exceptions after some emissions`() = runTest {
        val exception = RuntimeException("Test exception")
        val flow: Flow<Int> = flow {
            emit(1)
            emit(2)
            throw exception
        }

        val results = flow.asResult().toList()

        assertEquals(4, results.size)
        assertTrue(results[0] is Result.Loading)
        assertTrue(results[1] is Result.Success)
        assertTrue(results[2] is Result.Success)
        assertTrue(results[3] is Result.Error)
    }

    @Test
    fun `asResult should work with empty flow`() = runTest {
        val flow: Flow<Int> = flow { }

        val results = flow.asResult().toList()

        assertEquals(1, results.size)
        assertTrue(results[0] is Result.Loading)
    }
}
