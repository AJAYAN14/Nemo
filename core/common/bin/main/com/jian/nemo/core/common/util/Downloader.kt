package com.jian.nemo.core.common.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 文件下载器
 */
@Singleton
class Downloader @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    /**
     * 下载文件并返回进度流 (0.0 到 1.0)
     */
    fun download(url: String, targetFile: File): Flow<DownloadState> = flow {
        emit(DownloadState.Downloading(0f))

        val request = Request.Builder().url(url).build()
        val response = okHttpClient.newCall(request).execute()

        if (!response.isSuccessful) {
            emit(DownloadState.Failed(Exception("下载失败: ${response.code}")))
            return@flow
        }

        val body = response.body ?: throw Exception("响应体为空")
        val totalBytes = body.contentLength()

        body.byteStream().use { input ->
            FileOutputStream(targetFile).use { output ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalRead = 0L

                var lastProgress = 0

                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    totalRead += bytesRead
                    if (totalBytes > 0) {
                        val currentProgress = ((totalRead.toFloat() / totalBytes) * 100).toInt()
                        if (currentProgress > lastProgress) {
                            lastProgress = currentProgress
                            emit(DownloadState.Downloading(totalRead.toFloat() / totalBytes))
                        }
                    }
                }
            }
        }

        emit(DownloadState.Success(targetFile))
    }.flowOn(Dispatchers.IO)
}

sealed class DownloadState {
    data class Downloading(val progress: Float) : DownloadState()
    data class Success(val file: File) : DownloadState()
    data class Failed(val error: Throwable) : DownloadState()
}
