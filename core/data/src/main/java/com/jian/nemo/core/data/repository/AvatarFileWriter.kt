package com.jian.nemo.core.data.repository

import com.jian.nemo.core.domain.model.AvatarStorageFailure
import com.jian.nemo.core.domain.model.AvatarStorageResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 头像文件流式限流写入与原子落盘协作者
 *
 * 与 Android 框架类型解耦，便于在纯 JVM 环境下验证流式上限截断、取消清理与原子提交行为。
 */
@Singleton
class AvatarFileWriter @Inject constructor() {

    companion object {
        const val DEFAULT_MAX_AVATAR_BYTES: Long = 20L * 1024L * 1024L
        const val DEFAULT_MIN_FREE_SPACE_BYTES: Long = 5L * 1024L * 1024L
        private const val COPY_BUFFER_SIZE: Int = 8 * 1024
    }

    /**
     * 将输入流按分块流式校验并原子写入目标目录
     */
    suspend fun writeFromStream(
        targetDir: File,
        inputStream: InputStream,
        sourceDescriptor: String = "",
        knownSizeBytes: Long? = null,
        availableSpaceBytes: Long = targetDir.usableSpace,
        maxBytes: Long = DEFAULT_MAX_AVATAR_BYTES,
        minFreeSpaceBytes: Long = DEFAULT_MIN_FREE_SPACE_BYTES,
        fileNameProvider: () -> String = ::generateAvatarFileName,
        isValidImage: (File) -> Boolean
    ): AvatarStorageResult {
        if (knownSizeBytes != null && knownSizeBytes > maxBytes) {
            runCatching { inputStream.close() }
            return AvatarStorageResult.Failure(
                AvatarStorageFailure.ImageTooLarge(
                    actualBytes = knownSizeBytes,
                    maxBytes = maxBytes
                )
            )
        }

        val requiredBytes = if (knownSizeBytes != null && knownSizeBytes > 0L) {
            maxOf(minFreeSpaceBytes, knownSizeBytes)
        } else {
            minFreeSpaceBytes
        }

        if (availableSpaceBytes < requiredBytes) {
            runCatching { inputStream.close() }
            return AvatarStorageResult.Failure(
                AvatarStorageFailure.StorageFull(
                    availableBytes = availableSpaceBytes,
                    requiredBytes = requiredBytes
                )
            )
        }

        if (!targetDir.exists() && !targetDir.mkdirs()) {
            runCatching { inputStream.close() }
            return AvatarStorageResult.Failure(AvatarStorageFailure.SaveFailed())
        }

        val finalName = fileNameProvider()
        val tempFile = File(targetDir, "$finalName.tmp")
        val finalFile = File(targetDir, finalName)

        return try {
            var totalBytes = 0L
            var exceededMaxSize = false

            inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(COPY_BUFFER_SIZE)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val bytesRead = input.read(buffer)
                        if (bytesRead == -1) break
                        totalBytes += bytesRead
                        if (totalBytes > maxBytes) {
                            exceededMaxSize = true
                            break
                        }
                        output.write(buffer, 0, bytesRead)
                    }
                    if (!exceededMaxSize) {
                        output.flush()
                    }
                }
            }

            if (exceededMaxSize) {
                safeDelete(tempFile)
                return AvatarStorageResult.Failure(
                    AvatarStorageFailure.ImageTooLarge(
                        actualBytes = totalBytes,
                        maxBytes = maxBytes
                    )
                )
            }

            if (totalBytes <= 0L || !tempFile.exists() || tempFile.length() <= 0L || !isValidImage(tempFile)) {
                safeDelete(tempFile)
                return AvatarStorageResult.Failure(AvatarStorageFailure.InvalidImage)
            }

            commitAtomically(tempFile, finalFile)

            if (!finalFile.exists() || finalFile.length() <= 0L) {
                safeDelete(finalFile)
                return AvatarStorageResult.Failure(AvatarStorageFailure.SaveFailed())
            }

            AvatarStorageResult.Success(finalFile.absolutePath)
        } catch (e: CancellationException) {
            safeDelete(tempFile)
            safeDelete(finalFile)
            throw e
        } catch (e: SecurityException) {
            safeDelete(tempFile)
            AvatarStorageResult.Failure(AvatarStorageFailure.PermissionDenied)
        } catch (e: FileNotFoundException) {
            safeDelete(tempFile)
            AvatarStorageResult.Failure(
                AvatarStorageFailure.FileNotFound(sourceDescriptor.ifBlank { e.message.orEmpty() })
            )
        } catch (e: IOException) {
            safeDelete(tempFile)
            safeDelete(finalFile)
            AvatarStorageResult.Failure(AvatarStorageFailure.SaveFailed(e.message))
        } catch (e: Exception) {
            safeDelete(tempFile)
            safeDelete(finalFile)
            AvatarStorageResult.Failure(AvatarStorageFailure.Unknown(e.message))
        }
    }

    /**
     * 校验本地临时图片文件并持久化迁移到目标目录，完成后清理源文件
     */
    suspend fun persistLocalFile(
        targetDir: File,
        sourceFile: File,
        availableSpaceBytes: Long = targetDir.usableSpace,
        maxBytes: Long = DEFAULT_MAX_AVATAR_BYTES,
        minFreeSpaceBytes: Long = DEFAULT_MIN_FREE_SPACE_BYTES,
        fileNameProvider: () -> String = ::generateAvatarFileName,
        isValidImage: (File) -> Boolean
    ): AvatarStorageResult {
        if (!sourceFile.exists()) {
            return AvatarStorageResult.Failure(
                AvatarStorageFailure.FileNotFound(sourceFile.absolutePath)
            )
        }

        val fileLength = sourceFile.length()
        if (fileLength <= 0L) {
            safeDelete(sourceFile)
            return AvatarStorageResult.Failure(AvatarStorageFailure.InvalidImage)
        }

        val result = try {
            writeFromStream(
                targetDir = targetDir,
                inputStream = FileInputStream(sourceFile),
                sourceDescriptor = sourceFile.absolutePath,
                knownSizeBytes = fileLength,
                availableSpaceBytes = availableSpaceBytes,
                maxBytes = maxBytes,
                minFreeSpaceBytes = minFreeSpaceBytes,
                fileNameProvider = fileNameProvider,
                isValidImage = isValidImage
            )
        } finally {
            safeDelete(sourceFile)
        }
        return result
    }

    private fun commitAtomically(tempFile: File, finalFile: File) {
        if (finalFile.exists()) {
            safeDelete(finalFile)
        }
        if (!tempFile.renameTo(finalFile)) {
            tempFile.copyTo(finalFile, overwrite = true)
            safeDelete(tempFile)
        }
    }

    private fun safeDelete(file: File) {
        if (file.exists()) {
            runCatching { file.delete() }
        }
    }

    private fun generateAvatarFileName(): String {
        val suffix = UUID.randomUUID().toString().replace("-", "").take(8)
        return "avatar_${System.currentTimeMillis()}_$suffix.jpg"
    }
}
