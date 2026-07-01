package com.jian.nemo.core.data.manager

import android.util.Log
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream

/**
 * 备份数据压缩工具
 * 使用 GZIP 压缩算法压缩备份数据，减少存储和传输成本
 */
object BackupCompression {
    private val TAG = "BackupCompression"



    /**
     * 解压数据
     * @param compressedData Base64编码的压缩数据字符串
     * @return 解压后的原始数据字符串
     */
    fun decompress(compressedData: String): String {
        return try {
            Log.d(TAG, "开始解压数据，Base64字符串长度: ${compressedData.length}")

            // Base64 解码
            val compressedBytes = try {
                android.util.Base64.decode(compressedData, android.util.Base64.NO_WRAP)
            } catch (e: Exception) {
                Log.e(TAG, "Base64 解码失败", e)
                throw RuntimeException("Base64 解码失败: ${e.message}。请检查数据格式是否正确。", e)
            }

            Log.d(TAG, "Base64 解码成功，压缩数据长度: ${compressedBytes.size} 字节")

            // 验证 GZIP 魔数
            if (compressedBytes.size < 2) {
                throw RuntimeException("压缩数据长度不足，无法验证 GZIP 格式")
            }

            val magic1 = compressedBytes[0].toInt() and 0xFF
            val magic2 = compressedBytes[1].toInt() and 0xFF
            if (magic1 != 0x1f || magic2 != 0x8b) {
                Log.e(TAG, "GZIP 魔数验证失败: 期望 [0x1f, 0x8b], 实际 [0x${magic1.toString(16)}, 0x${magic2.toString(16)}]")
                throw RuntimeException("不是有效的 GZIP 格式数据。魔数: [0x${magic1.toString(16)}, 0x${magic2.toString(16)}]")
            }

            Log.d(TAG, "GZIP 魔数验证通过，开始解压...")

            val inputStream = ByteArrayInputStream(compressedBytes)
            val outputStream = ByteArrayOutputStream()

            GZIPInputStream(inputStream).use { gzipIn ->
                gzipIn.copyTo(outputStream)
            }

            val decompressedBytes = outputStream.toByteArray()
            val result = decompressedBytes.toString(Charsets.UTF_8)

            Log.d(TAG, "数据解压完成: 压缩=${compressedBytes.size}字节, 解压后=${decompressedBytes.size}字节, 解压后字符串长度=${result.length}")

            result
        } catch (e: RuntimeException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "数据解压失败", e)
            throw RuntimeException("数据解压失败: ${e.javaClass.simpleName} - ${e.message}", e)
        }
    }

    /**
     * 检查数据是否已压缩
     * @param data 数据字符串
     * @return 如果数据是压缩的返回 true
     */
    fun isCompressed(data: String): Boolean {
        return try {
            if (data.isEmpty() || data.isBlank()) {
                return false
            }

            // 简单的 Base64 内容检查
            val base64Pattern = Regex("^[A-Za-z0-9+/=]+$")
            if (!base64Pattern.matches(data.trim())) {
                return false
            }

            // 尝试 Base64 解码
            val decoded = android.util.Base64.decode(data, android.util.Base64.NO_WRAP)

            if (decoded.size < 2) {
                return false
            }

            // GZIP 魔数检查
            val byte1 = decoded[0].toInt() and 0xFF
            val byte2 = decoded[1].toInt() and 0xFF
            byte1 == 0x1f && byte2 == 0x8b
        } catch (e: Exception) {
            false
        }
    }
}
