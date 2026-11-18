package com.jian.nemo.core.ui.util

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.util.concurrent.Executors

class PosterExportHelperTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val mainExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "test-main-thread")
    }
    private val ioExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "test-io-thread")
    }
    private val mainDispatcher = mainExecutor.asCoroutineDispatcher()
    private val ioDispatcher = ioExecutor.asCoroutineDispatcher()

    @After
    fun tearDown() {
        mainExecutor.shutdownNow()
        ioExecutor.shutdownNow()
    }

    @Test
    fun saveToGallery_capturesOnMainAndWritesAndReleasesOnIoOnce() = runBlocking(mainDispatcher) {
        val mainThreadId = Thread.currentThread().id
        val executor = PosterExportExecutor(ioDispatcher = ioDispatcher, mainDispatcher = mainDispatcher)
        val events = mutableListOf<String>()
        var captureThreadId = -1L
        var writeThreadId = -1L
        var writeThreadName = ""
        var releaseThreadName = ""
        var releaseCount = 0

        val result = executor.saveToGallery(
            captureBitmap = {
                captureThreadId = Thread.currentThread().id
                events.add("capture")
                "fake-bitmap"
            },
            releaseBitmap = {
                releaseCount++
                releaseThreadName = Thread.currentThread().name
                events.add("release")
            },
            writeToGallery = {
                writeThreadId = Thread.currentThread().id
                writeThreadName = Thread.currentThread().name
                events.add("write")
            }
        )

        assertTrue(result.isSuccess)
        assertEquals(mainThreadId, captureThreadId)
        assertTrue(writeThreadId != mainThreadId)
        assertTrue(writeThreadName.startsWith("test-io-thread"))
        assertTrue(releaseThreadName.startsWith("test-io-thread"))
        assertEquals(1, releaseCount)
        assertEquals(listOf("capture", "write", "release"), events)
    }

    @Test
    fun sharePoster_switchesMainToIoAndBackToMainBeforeLaunchingShare() = runBlocking(mainDispatcher) {
        val mainThreadId = Thread.currentThread().id
        val executor = PosterExportExecutor(ioDispatcher = ioDispatcher, mainDispatcher = mainDispatcher)
        val events = mutableListOf<String>()
        var captureThreadId = -1L
        var cacheThreadId = -1L
        var cacheThreadName = ""
        var releaseThreadName = ""
        var releaseCount = 0
        var shareThreadId = -1L

        executor.sharePoster(
            captureBitmap = {
                captureThreadId = Thread.currentThread().id
                events.add("capture")
                "fake-bitmap"
            },
            releaseBitmap = {
                releaseCount++
                releaseThreadName = Thread.currentThread().name
                events.add("release")
            },
            writeToCache = {
                cacheThreadId = Thread.currentThread().id
                cacheThreadName = Thread.currentThread().name
                events.add("writeCache")
                "content://poster/share"
            },
            launchShare = { uri ->
                shareThreadId = Thread.currentThread().id
                events.add("share:$uri")
            }
        )

        assertEquals(mainThreadId, captureThreadId)
        assertTrue(cacheThreadId != mainThreadId)
        assertTrue(cacheThreadName.startsWith("test-io-thread"))
        assertTrue(releaseThreadName.startsWith("test-io-thread"))
        assertEquals(1, releaseCount)
        assertEquals(mainThreadId, shareThreadId)
        assertEquals(listOf("capture", "writeCache", "release", "share:content://poster/share"), events)
    }

    @Test
    fun saveToGallery_cancelledBeforeEnteringIo_stillReleasesBitmapOnce() = runBlocking(mainDispatcher) {
        val executor = PosterExportExecutor(ioDispatcher = ioDispatcher, mainDispatcher = mainDispatcher)
        var releaseCount = 0
        var writeInvoked = false
        var caughtCancellation = false

        val job = launch(mainDispatcher) {
            try {
                executor.saveToGallery(
                    captureBitmap = {
                        cancel()
                        "fake-bitmap"
                    },
                    releaseBitmap = { releaseCount++ },
                    writeToGallery = { writeInvoked = true }
                )
            } catch (e: CancellationException) {
                caughtCancellation = true
                throw e
            }
        }
        job.join()

        assertTrue(caughtCancellation)
        assertFalse(writeInvoked)
        assertEquals(1, releaseCount)
    }

    @Test
    fun sharePoster_cancelledDuringCacheWrite_rethrowsCancellationAndSkipsShare() = runBlocking(mainDispatcher) {
        val executor = PosterExportExecutor(ioDispatcher = ioDispatcher, mainDispatcher = mainDispatcher)
        var released = false
        var launchedShare = false
        var caughtCancellation = false

        val job = launch(mainDispatcher) {
            try {
                executor.sharePoster(
                    captureBitmap = { "fake-bitmap" },
                    releaseBitmap = { released = true },
                    writeToCache = { throw CancellationException("sheet closed") },
                    launchShare = { launchedShare = true }
                )
            } catch (e: CancellationException) {
                caughtCancellation = true
                throw e
            }
        }
        job.join()

        assertTrue(caughtCancellation)
        assertTrue(released)
        assertFalse(launchedShare)
    }

    @Test
    fun saveToGallery_calledOffMain_stillCapturesOnMainAndWritesOnIo() = runBlocking(ioDispatcher) {
        val ioThreadId = Thread.currentThread().id
        val executor = PosterExportExecutor(ioDispatcher = ioDispatcher, mainDispatcher = mainDispatcher)
        var captureThreadName = ""
        var writeThreadId = -1L

        val result = executor.saveToGallery(
            captureBitmap = {
                captureThreadName = Thread.currentThread().name
                "fake-bitmap"
            },
            releaseBitmap = {},
            writeToGallery = {
                writeThreadId = Thread.currentThread().id
            }
        )

        assertTrue(result.isSuccess)
        assertTrue(captureThreadName.startsWith("test-main-thread"))
        assertEquals(ioThreadId, writeThreadId)
    }

    @Test
    fun saveToGallery_writeFailure_returnsFailureAndStillReleasesBitmap() = runBlocking(mainDispatcher) {
        val executor = PosterExportExecutor(ioDispatcher = ioDispatcher, mainDispatcher = mainDispatcher)
        var released = false

        val result = executor.saveToGallery(
            captureBitmap = { "fake-bitmap" },
            releaseBitmap = { released = true },
            writeToGallery = { throw IOException("disk full") }
        )

        assertTrue(result.isFailure)
        assertEquals("disk full", result.exceptionOrNull()?.message)
        assertTrue(released)
    }

    @Test
    fun saveToGallery_cancelledDuringIo_rethrowsCancellationAndReleasesBitmap() = runBlocking(mainDispatcher) {
        val executor = PosterExportExecutor(ioDispatcher = ioDispatcher, mainDispatcher = mainDispatcher)
        var released = false
        var caughtCancellation = false

        val job = launch(mainDispatcher) {
            try {
                executor.saveToGallery(
                    captureBitmap = { "fake-bitmap" },
                    releaseBitmap = { released = true },
                    writeToGallery = { throw CancellationException("dialog dismissed") }
                )
            } catch (e: CancellationException) {
                caughtCancellation = true
                throw e
            }
        }
        job.join()

        assertTrue(caughtCancellation)
        assertTrue(released)
    }

    @Test
    fun writeAtomicPngFile_createsDirectoryAndReplacesExistingWithoutTempRemnants() = runTest {
        val targetDir = File(tempFolder.root, "nested/share_posters")
        val initialBytes = byteArrayOf(1, 2, 3, 4)
        val updatedBytes = byteArrayOf(9, 8, 7, 6, 5)

        val firstFile = PosterFileOps.writeAtomicPngFile(
            targetDir = targetDir,
            fileName = "poster.png",
            encodePng = { out ->
                out.write(initialBytes)
                true
            }
        )
        assertTrue(firstFile.exists())
        assertArrayEquals(initialBytes, firstFile.readBytes())

        val secondFile = PosterFileOps.writeAtomicPngFile(
            targetDir = targetDir,
            fileName = "poster.png",
            encodePng = { out ->
                out.write(updatedBytes)
                true
            }
        )
        assertEquals(firstFile.absolutePath, secondFile.absolutePath)
        assertArrayEquals(updatedBytes, secondFile.readBytes())
        assertEquals(listOf("poster.png"), targetDir.listFiles()?.map { it.name })
    }

    @Test
    fun writeAtomicPngFile_whenEncodeReturnsFalse_cleansTempAndPreservesPreviousFile() = runTest {
        val targetDir = tempFolder.newFolder("share_preserve")
        val existingFile = File(targetDir, "poster.png").apply {
            writeBytes(byteArrayOf(42, 43))
        }

        try {
            PosterFileOps.writeAtomicPngFile(
                targetDir = targetDir,
                fileName = "poster.png",
                encodePng = { out ->
                    out.write(byteArrayOf(0, 0, 0))
                    false
                }
            )
            fail("Expected IOException when encodePng returns false")
        } catch (expected: IOException) {
            assertTrue(expected.message?.contains("false") == true)
        }

        assertArrayEquals(byteArrayOf(42, 43), existingFile.readBytes())
        assertEquals(listOf("poster.png"), targetDir.listFiles()?.map { it.name })
    }

    @Test
    fun writeAtomicPngFile_whenCancelled_rethrowsCancellationAndDeletesTempFile() = runTest {
        val targetDir = tempFolder.newFolder("share_cancel")
        var caughtCancellation = false

        val job = launch {
            try {
                PosterFileOps.writeAtomicPngFile(
                    targetDir = targetDir,
                    fileName = "poster.png",
                    encodePng = { out ->
                        out.write(byteArrayOf(1, 2, 3))
                        cancel()
                        true
                    }
                )
            } catch (e: CancellationException) {
                caughtCancellation = true
                throw e
            }
        }
        job.join()

        assertTrue(caughtCancellation)
        assertEquals(0, targetDir.listFiles()?.size ?: 0)
    }

    @Test
    fun mediaStoreCommitter_happyPath_insertsPendingWritesAndMarksPublished() = runTest {
        val gateway = FakeMediaStoreGateway()
        val payload = byteArrayOf(10, 20, 30, 40)

        PosterMediaStoreCommitter.commitPng(
            gateway = gateway,
            displayName = "Nemo_123.png",
            relativePath = "Pictures/Nemo",
            encodePng = { out ->
                out.write(payload)
                true
            }
        )

        assertEquals(listOf("insertPending:Nemo_123.png@Pictures/Nemo", "open", "publish:uri://1"), gateway.calls)
        assertArrayEquals(payload, gateway.writtenBytes.toByteArray())
        assertFalse(gateway.deleted)
    }

    @Test
    fun mediaStoreCommitter_nullInsertUri_throwsIoException() = runTest {
        val gateway = FakeMediaStoreGateway(insertReturnsNull = true)

        try {
            PosterMediaStoreCommitter.commitPng(
                gateway = gateway,
                displayName = "Nemo_null.png",
                relativePath = "Pictures/Nemo",
                encodePng = { true }
            )
            fail("Expected IOException when insertPending returns null")
        } catch (expected: IOException) {
            assertTrue(expected.message?.contains("null URI") == true)
        }
        assertFalse(gateway.deleted)
    }

    @Test
    fun mediaStoreCommitter_nullOutputStream_deletesPendingRecordAndThrows() = runTest {
        val gateway = FakeMediaStoreGateway(openStreamReturnsNull = true)

        try {
            PosterMediaStoreCommitter.commitPng(
                gateway = gateway,
                displayName = "Nemo_nostream.png",
                relativePath = "Pictures/Nemo",
                encodePng = { true }
            )
            fail("Expected IOException when openOutputStream returns null")
        } catch (expected: IOException) {
            assertTrue(expected.message?.contains("openOutputStream") == true)
        }
        assertTrue(gateway.deleted)
        assertFalse(gateway.published)
    }

    @Test
    fun mediaStoreCommitter_encodeReturnsFalse_deletesPendingRecordAndDoesNotPublish() = runTest {
        val gateway = FakeMediaStoreGateway()

        try {
            PosterMediaStoreCommitter.commitPng(
                gateway = gateway,
                displayName = "Nemo_bad.png",
                relativePath = "Pictures/Nemo",
                encodePng = { false }
            )
            fail("Expected IOException when encodePng returns false")
        } catch (expected: IOException) {
            assertTrue(expected.message?.contains("false") == true)
        }
        assertTrue(gateway.deleted)
        assertFalse(gateway.published)
    }

    @Test
    fun mediaStoreCommitter_cancelledDuringWrite_deletesPendingRecordAndRethrows() = runTest {
        val gateway = FakeMediaStoreGateway()
        var caughtCancellation = false

        val job = launch {
            try {
                PosterMediaStoreCommitter.commitPng(
                    gateway = gateway,
                    displayName = "Nemo_cancel.png",
                    relativePath = "Pictures/Nemo",
                    encodePng = { out ->
                        out.write(byteArrayOf(7, 7))
                        cancel()
                        true
                    }
                )
            } catch (e: CancellationException) {
                caughtCancellation = true
                throw e
            }
        }
        job.join()

        assertTrue(caughtCancellation)
        assertTrue(gateway.deleted)
        assertFalse(gateway.published)
    }

    @Test
    fun mediaStoreCommitter_markPublishedReturnsFalse_deletesPendingRecordAndThrows() = runTest {
        val gateway = FakeMediaStoreGateway(markPublishedResult = false)

        try {
            PosterMediaStoreCommitter.commitPng(
                gateway = gateway,
                displayName = "Nemo_unpublishable.png",
                relativePath = "Pictures/Nemo",
                encodePng = { out ->
                    out.write(byteArrayOf(1, 2, 3))
                    true
                }
            )
            fail("Expected IOException when markPublished returns false")
        } catch (expected: IOException) {
            assertTrue(expected.message?.contains("publish") == true)
        }
        assertTrue(gateway.deleted)
        assertFalse(gateway.published)
    }

    @Test
    fun writeAtomicPngFile_rejectsBlankAndTraversalFileNames() = runTest {
        val targetDir = tempFolder.newFolder("invalid_names")
        val invalidNames = listOf("", "   ", "../escape.png", "sub/poster.png", "sub\\poster.png", "..")
        for (name in invalidNames) {
            try {
                PosterFileOps.writeAtomicPngFile(
                    targetDir = targetDir,
                    fileName = name,
                    encodePng = { true }
                )
                fail("Expected IllegalArgumentException for invalid file name: '$name'")
            } catch (_: IllegalArgumentException) {
                // 预期拒绝非法文件名
            }
        }
        assertEquals(0, targetDir.listFiles()?.size ?: 0)
    }

    @Test
    fun resolveUniqueFileName_appendsNumericSuffixWhenFileExists() {
        val targetDir = tempFolder.newFolder("unique_names")
        assertEquals("Nemo_100.png", PosterFileOps.resolveUniqueFileName(targetDir, "Nemo_100.png"))

        File(targetDir, "Nemo_100.png").writeBytes(byteArrayOf(1))
        assertEquals("Nemo_100_1.png", PosterFileOps.resolveUniqueFileName(targetDir, "Nemo_100.png"))

        File(targetDir, "Nemo_100_1.png").writeBytes(byteArrayOf(2))
        assertEquals("Nemo_100_2.png", PosterFileOps.resolveUniqueFileName(targetDir, "Nemo_100.png"))
    }

    @Test
    fun sharePoster_cancelledDuringCapture_stillReleasesBitmapOnceAndSkipsShare() = runBlocking(mainDispatcher) {
        val executor = PosterExportExecutor(ioDispatcher = ioDispatcher, mainDispatcher = mainDispatcher)
        var releaseCount = 0
        var writeInvoked = false
        var shareInvoked = false
        var caughtCancellation = false

        val job = launch(mainDispatcher) {
            try {
                executor.sharePoster(
                    captureBitmap = {
                        cancel()
                        "fake-bitmap"
                    },
                    releaseBitmap = { releaseCount++ },
                    writeToCache = {
                        writeInvoked = true
                        "uri://never"
                    },
                    launchShare = { shareInvoked = true }
                )
            } catch (e: CancellationException) {
                caughtCancellation = true
                throw e
            }
        }
        job.join()

        assertTrue(caughtCancellation)
        assertFalse(writeInvoked)
        assertFalse(shareInvoked)
        assertEquals(1, releaseCount)
    }

    @Test
    fun cleanupStaleTempFiles_deletesOnlyExpiredTempFiles() {
        val targetDir = tempFolder.newFolder("stale_cleanup")
        val now = 1_700_000_000_000L
        val staleTemp = File(targetDir, "poster_old.tmp").apply {
            writeBytes(byteArrayOf(1))
            setLastModified(now - 2 * 60 * 60 * 1000L)
        }
        val freshTemp = File(targetDir, "poster_fresh.tmp").apply {
            writeBytes(byteArrayOf(2))
            setLastModified(now - 5 * 60 * 1000L)
        }
        val normalPng = File(targetDir, "poster_keep.png").apply {
            writeBytes(byteArrayOf(3))
            setLastModified(now - 5 * 60 * 60 * 1000L)
        }

        PosterFileOps.cleanupStaleTempFiles(targetDir, nowMs = now, maxAgeMs = 60 * 60 * 1000L)

        assertFalse(staleTemp.exists())
        assertTrue(freshTemp.exists())
        assertTrue(normalPng.exists())
    }

    private class FakeMediaStoreGateway(
        private val insertReturnsNull: Boolean = false,
        private val openStreamReturnsNull: Boolean = false,
        private val markPublishedResult: Boolean = true
    ) : MediaStoreGateway<String> {
        val calls = mutableListOf<String>()
        val writtenBytes = ByteArrayOutputStream()
        var published = false
        var deleted = false

        override fun insertPending(displayName: String, relativePath: String): String? {
            calls.add("insertPending:$displayName@$relativePath")
            return if (insertReturnsNull) null else "uri://1"
        }

        override fun openOutputStream(uri: String): OutputStream? {
            calls.add("open")
            return if (openStreamReturnsNull) null else writtenBytes
        }

        override fun markPublished(uri: String): Boolean {
            calls.add("publish:$uri")
            if (markPublishedResult) {
                published = true
            }
            return markPublishedResult
        }

        override fun delete(uri: String) {
            calls.add("delete:$uri")
            deleted = true
        }
    }
}
