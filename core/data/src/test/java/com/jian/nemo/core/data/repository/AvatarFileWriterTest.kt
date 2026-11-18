package com.jian.nemo.core.data.repository

import com.jian.nemo.core.domain.model.AvatarStorageFailure
import com.jian.nemo.core.domain.model.AvatarStorageResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream

class AvatarFileWriterTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val writer = AvatarFileWriter()

    @Test
    fun writeFromStream_validImage_commitsAtomicallyAndLeavesNoTempFile() = runTest {
        val targetDir = tempFolder.newFolder("avatars_ok")
        val payload = ByteArray(16 * 1024) { (it % 127).toByte() }

        val result = writer.writeFromStream(
            targetDir = targetDir,
            inputStream = ByteArrayInputStream(payload),
            availableSpaceBytes = 50L * 1024L * 1024L,
            fileNameProvider = { "avatar_test.jpg" },
            isValidImage = { true }
        )

        assertTrue(result is AvatarStorageResult.Success)
        val savedPath = (result as AvatarStorageResult.Success).filePath
        val savedFile = File(savedPath)
        assertTrue(savedFile.exists())
        assertEquals(payload.size.toLong(), savedFile.length())
        assertFalse(File(targetDir, "avatar_test.jpg.tmp").exists())
    }

    @Test
    fun writeFromStream_exactLimit_succeeds() = runTest {
        val targetDir = tempFolder.newFolder("avatars_exact")
        val limit = 64L * 1024L
        val payload = ByteArray(limit.toInt()) { 1 }

        val result = writer.writeFromStream(
            targetDir = targetDir,
            inputStream = ZeroAvailableInputStream(payload),
            availableSpaceBytes = 50L * 1024L * 1024L,
            maxBytes = limit,
            minFreeSpaceBytes = 1024L,
            fileNameProvider = { "exact.jpg" },
            isValidImage = { true }
        )

        assertTrue(result is AvatarStorageResult.Success)
        assertEquals(limit, File((result as AvatarStorageResult.Success).filePath).length())
    }

    @Test
    fun writeFromStream_exceedsLimitWithZeroAvailable_abortsAndDeletesTempFile() = runTest {
        val targetDir = tempFolder.newFolder("avatars_oversize")
        val limit = 32L * 1024L
        val payload = ByteArray(limit.toInt() + 1024) { 2 }

        val result = writer.writeFromStream(
            targetDir = targetDir,
            inputStream = ZeroAvailableInputStream(payload),
            availableSpaceBytes = 50L * 1024L * 1024L,
            maxBytes = limit,
            minFreeSpaceBytes = 1024L,
            fileNameProvider = { "oversize.jpg" },
            isValidImage = { true }
        )

        assertTrue(result is AvatarStorageResult.Failure)
        val failure = (result as AvatarStorageResult.Failure).reason
        assertTrue(failure is AvatarStorageFailure.ImageTooLarge)
        assertTrue((failure as AvatarStorageFailure.ImageTooLarge).actualBytes > limit)
        assertEquals(0, targetDir.listFiles()?.size ?: 0)
    }

    @Test
    fun writeFromStream_emptyOrInvalidImage_failsAndCleansTempFile() = runTest {
        val targetDir = tempFolder.newFolder("avatars_invalid")

        val emptyResult = writer.writeFromStream(
            targetDir = targetDir,
            inputStream = ByteArrayInputStream(ByteArray(0)),
            availableSpaceBytes = 50L * 1024L * 1024L,
            isValidImage = { true }
        )
        assertEquals(AvatarStorageResult.Failure(AvatarStorageFailure.InvalidImage), emptyResult)

        val corruptedResult = writer.writeFromStream(
            targetDir = targetDir,
            inputStream = ByteArrayInputStream(ByteArray(128) { 9 }),
            availableSpaceBytes = 50L * 1024L * 1024L,
            isValidImage = { false }
        )
        assertEquals(AvatarStorageResult.Failure(AvatarStorageFailure.InvalidImage), corruptedResult)
        assertEquals(0, targetDir.listFiles()?.size ?: 0)
    }

    @Test
    fun writeFromStream_insufficientStorage_returnsStorageFull() = runTest {
        val targetDir = tempFolder.newFolder("avatars_full")

        val result = writer.writeFromStream(
            targetDir = targetDir,
            inputStream = ByteArrayInputStream(ByteArray(1024)),
            availableSpaceBytes = 1024L,
            minFreeSpaceBytes = 5L * 1024L * 1024L,
            isValidImage = { true }
        )

        assertTrue(result is AvatarStorageResult.Failure)
        assertTrue((result as AvatarStorageResult.Failure).reason is AvatarStorageFailure.StorageFull)
    }

    @Test
    fun writeFromStream_cancelledMidCopy_rethrowsCancellationAndDeletesTempFile() = runTest {
        val targetDir = tempFolder.newFolder("avatars_cancel")
        var caughtCancellation = false

        val job = launch {
            try {
                writer.writeFromStream(
                    targetDir = targetDir,
                    inputStream = CancellingInputStream(onSecondRead = { cancel() }),
                    availableSpaceBytes = 50L * 1024L * 1024L,
                    fileNameProvider = { "cancelled.jpg" },
                    isValidImage = { true }
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
    fun persistLocalFile_validCapture_persistsToTargetAndDeletesTempSource() = runTest {
        val cacheDir = tempFolder.newFolder("camera_cache")
        val targetDir = tempFolder.newFolder("avatars_persisted")
        val sourceFile = File(cacheDir, "temp_avatar.jpg").apply {
            writeBytes(ByteArray(4096) { 7 })
        }

        val result = writer.persistLocalFile(
            targetDir = targetDir,
            sourceFile = sourceFile,
            availableSpaceBytes = 50L * 1024L * 1024L,
            fileNameProvider = { "camera_saved.jpg" },
            isValidImage = { true }
        )

        assertTrue(result is AvatarStorageResult.Success)
        assertFalse(sourceFile.exists())
        val persisted = File((result as AvatarStorageResult.Success).filePath)
        assertTrue(persisted.exists())
        assertEquals(4096L, persisted.length())
    }

    private class ZeroAvailableInputStream(data: ByteArray) : InputStream() {
        private val delegate = ByteArrayInputStream(data)
        override fun read(): Int = delegate.read()
        override fun read(b: ByteArray, off: Int, len: Int): Int = delegate.read(b, off, len)
        override fun available(): Int = 0
    }

    private class CancellingInputStream(
        private val onSecondRead: () -> Unit
    ) : InputStream() {
        private var reads = 0
        override fun read(): Int = 1
        override fun read(b: ByteArray, off: Int, len: Int): Int {
            reads++
            if (reads == 2) {
                onSecondRead()
            }
            val count = minOf(len, 4096)
            b.fill(1, off, off + count)
            return count
        }
    }
}
