package com.jian.nemo.core.data.repository

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import com.jian.nemo.core.common.Result
import com.jian.nemo.core.common.network.Dispatcher
import com.jian.nemo.core.common.network.NemoDispatchers
import com.jian.nemo.core.domain.model.AvatarStorageFailure
import com.jian.nemo.core.domain.model.AvatarStorageResult
import com.jian.nemo.core.domain.model.CameraCaptureTarget
import com.jian.nemo.core.domain.repository.AvatarStorageRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 头像本地存储默认实现
 *
 * 所有跨进程 ContentResolver 取流、磁盘空间检查、FileProvider 准备及图片解码校验
 * 均在注入的 IO 调度器上执行。
 */
@Singleton
class DefaultAvatarStorageRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @Dispatcher(NemoDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
    private val avatarFileWriter: AvatarFileWriter
) : AvatarStorageRepository {

    companion object {
        private const val CAMERA_TEMP_DIR_NAME = "camera_avatars"
    }

    override suspend fun saveAvatarFromUri(uriString: String): AvatarStorageResult =
        withContext(ioDispatcher) {
            val uri = Uri.parse(uriString)
            try {
                val knownSize = queryKnownSizeBytes(uri)
                if (knownSize != null && knownSize > AvatarFileWriter.DEFAULT_MAX_AVATAR_BYTES) {
                    return@withContext AvatarStorageResult.Failure(
                        AvatarStorageFailure.ImageTooLarge(
                            actualBytes = knownSize,
                            maxBytes = AvatarFileWriter.DEFAULT_MAX_AVATAR_BYTES
                        )
                    )
                }

                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: return@withContext AvatarStorageResult.Failure(
                        AvatarStorageFailure.FileNotFound(uriString)
                    )

                avatarFileWriter.writeFromStream(
                    targetDir = context.filesDir,
                    inputStream = inputStream,
                    sourceDescriptor = uriString,
                    knownSizeBytes = knownSize,
                    isValidImage = ::isDecodableImage
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: SecurityException) {
                AvatarStorageResult.Failure(AvatarStorageFailure.PermissionDenied)
            } catch (e: FileNotFoundException) {
                AvatarStorageResult.Failure(AvatarStorageFailure.FileNotFound(uriString))
            } catch (e: Exception) {
                AvatarStorageResult.Failure(AvatarStorageFailure.Unknown(e.message))
            }
        }

    override suspend fun prepareCameraCaptureTarget(): Result<CameraCaptureTarget> =
        withContext(ioDispatcher) {
            try {
                val tempDir = File(context.cacheDir, CAMERA_TEMP_DIR_NAME)
                if (!tempDir.exists() && !tempDir.mkdirs()) {
                    return@withContext Result.Error(IllegalStateException("Unable to create camera cache directory"))
                }
                val suffix = UUID.randomUUID().toString().replace("-", "").take(8)
                val tempFile = File(tempDir, "temp_avatar_${System.currentTimeMillis()}_$suffix.jpg")
                val contentUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    tempFile
                )
                Result.Success(
                    CameraCaptureTarget(
                        filePath = tempFile.absolutePath,
                        contentUri = contentUri.toString()
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.Error(e)
            }
        }

    override suspend fun finalizeCameraCapture(tempFilePath: String): AvatarStorageResult =
        withContext(ioDispatcher) {
            val tempFile = File(tempFilePath)
            avatarFileWriter.persistLocalFile(
                targetDir = context.filesDir,
                sourceFile = tempFile,
                isValidImage = ::isDecodableImage
            )
        }

    override suspend fun cleanupTempCapture(tempFilePath: String) =
        withContext(ioDispatcher) {
            if (tempFilePath.isBlank()) return@withContext
            val tempFile = File(tempFilePath)
            if (tempFile.exists()) {
                runCatching { tempFile.delete() }
            }
            Unit
        }

    private fun queryKnownSizeBytes(uri: Uri): Long? {
        val queriedSize = runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.SIZE),
                null,
                null,
                null
            )?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex >= 0 && cursor.moveToFirst() && !cursor.isNull(sizeIndex)) {
                    cursor.getLong(sizeIndex)
                } else {
                    null
                }
            }
        }.getOrNull()

        if (queriedSize != null && queriedSize >= 0L) {
            return queriedSize
        }

        val afdSize = runCatching {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
                afd.length.takeIf { it >= 0L }
            }
        }.getOrNull()

        return afdSize
    }

    private fun isDecodableImage(file: File): Boolean {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(file.absolutePath, options)
        return options.outWidth > 0 && options.outHeight > 0
    }
}
