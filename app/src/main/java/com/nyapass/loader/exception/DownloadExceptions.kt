package com.nyapass.loader.exception

import android.content.Context
import com.nyapass.loader.R

/**
 * 下载异常基类
 * 所有下载相关的异常都继承自此类，便于统一处理和分类
 */
sealed class DownloadException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {

    /**
     * 获取用户友好的错误消息
     */
    abstract fun getLocalizedMessage(context: Context): String

    /**
     * 是否可以自动重试
     */
    abstract val isRetryable: Boolean

    // ============= 网络相关异常（可重试） =============

    /**
     * 网络连接超时
     */
    class NetworkTimeoutException(
        cause: Throwable? = null
    ) : DownloadException("Network connection timeout", cause) {
        override fun getLocalizedMessage(context: Context): String =
            context.getString(R.string.error_network_timeout)

        override val isRetryable: Boolean = true
    }

    /**
     * 网络不可用
     */
    class NetworkUnavailableException(
        cause: Throwable? = null
    ) : DownloadException("Network unavailable", cause) {
        override fun getLocalizedMessage(context: Context): String =
            context.getString(R.string.error_network_unavailable)

        override val isRetryable: Boolean = true
    }

    /**
     * 连接被重置
     */
    class ConnectionResetException(
        cause: Throwable? = null
    ) : DownloadException("Connection reset by peer", cause) {
        override fun getLocalizedMessage(context: Context): String =
            context.getString(R.string.error_connection_reset)

        override val isRetryable: Boolean = true
    }

    // ============= 服务器相关异常 =============

    /**
     * 服务器错误（5xx）- 可重试
     */
    class ServerErrorException(
        val code: Int,
        cause: Throwable? = null
    ) : DownloadException("Server error: HTTP $code", cause) {
        override fun getLocalizedMessage(context: Context): String =
            context.getString(R.string.error_server_error, code)

        override val isRetryable: Boolean = true
    }

    /**
     * 资源未找到（404）- 不可重试
     */
    class ResourceNotFoundException(
        val url: String,
        cause: Throwable? = null
    ) : DownloadException("Resource not found: $url", cause) {
        override fun getLocalizedMessage(context: Context): String =
            context.getString(R.string.error_resource_not_found)

        override val isRetryable: Boolean = false
    }

    /**
     * 请求被拒绝（403）- 不可重试
     */
    class AccessDeniedException(
        val url: String,
        cause: Throwable? = null
    ) : DownloadException("Access denied: $url", cause) {
        override fun getLocalizedMessage(context: Context): String =
            context.getString(R.string.error_access_denied)

        override val isRetryable: Boolean = false
    }

    /**
     * 请求过于频繁（429）- 可重试
     */
    class TooManyRequestsException(
        val retryAfterSeconds: Long? = null,
        cause: Throwable? = null
    ) : DownloadException("Too many requests", cause) {
        override fun getLocalizedMessage(context: Context): String =
            context.getString(R.string.error_too_many_requests)

        override val isRetryable: Boolean = true
    }

    // ============= 文件/存储相关异常 =============

    /**
     * 存储空间不足
     */
    class InsufficientStorageException(
        val availableBytes: Long,
        val requiredBytes: Long,
        cause: Throwable? = null
    ) : DownloadException(
        "Insufficient storage: available=$availableBytes, required=$requiredBytes",
        cause
    ) {
        override fun getLocalizedMessage(context: Context): String =
            context.getString(R.string.error_insufficient_storage)

        override val isRetryable: Boolean = false
    }

    /**
     * 存储权限被拒绝
     */
    class StoragePermissionDeniedException(
        cause: Throwable? = null
    ) : DownloadException("Storage permission denied", cause) {
        override fun getLocalizedMessage(context: Context): String =
            context.getString(R.string.error_storage_permission_denied)

        override val isRetryable: Boolean = false
    }

    /**
     * 文件创建失败
     */
    class FileCreationException(
        val filePath: String,
        cause: Throwable? = null
    ) : DownloadException("Failed to create file: $filePath", cause) {
        override fun getLocalizedMessage(context: Context): String =
            context.getString(R.string.error_file_creation_failed)

        override val isRetryable: Boolean = false
    }

    /**
     * 文件写入失败
     */
    class FileWriteException(
        val filePath: String,
        cause: Throwable? = null
    ) : DownloadException("Failed to write file: $filePath", cause) {
        override fun getLocalizedMessage(context: Context): String =
            context.getString(R.string.error_file_write_failed)

        override val isRetryable: Boolean = false
    }

    /**
     * 文件已存在
     */
    class FileAlreadyExistsException(
        val filePath: String,
        cause: Throwable? = null
    ) : DownloadException("File already exists: $filePath", cause) {
        override fun getLocalizedMessage(context: Context): String =
            context.getString(R.string.error_file_already_exists)

        override val isRetryable: Boolean = false
    }

    // ============= 安全相关异常 =============

    /**
     * SSL/TLS 证书错误
     */
    class SSLCertificateException(
        cause: Throwable? = null
    ) : DownloadException("SSL certificate verification failed", cause) {
        override fun getLocalizedMessage(context: Context): String =
            context.getString(R.string.error_ssl_certificate)

        override val isRetryable: Boolean = false
    }

    // ============= 下载任务相关异常 =============

    /**
     * 任务已取消
     */
    class TaskCancelledException(
        val taskId: Long
    ) : DownloadException("Task cancelled: $taskId") {
        override fun getLocalizedMessage(context: Context): String =
            context.getString(R.string.error_task_cancelled)

        override val isRetryable: Boolean = false
    }

    /**
     * 不支持断点续传
     */
    class RangeNotSupportedException(
        val url: String,
        cause: Throwable? = null
    ) : DownloadException("Range requests not supported: $url", cause) {
        override fun getLocalizedMessage(context: Context): String =
            context.getString(R.string.error_range_not_supported)

        override val isRetryable: Boolean = false
    }

    /**
     * 分片数据损坏
     */
    class PartCorruptedException(
        val taskId: Long,
        val partIndex: Int,
        cause: Throwable? = null
    ) : DownloadException("Part corrupted: task=$taskId, part=$partIndex", cause) {
        override fun getLocalizedMessage(context: Context): String =
            context.getString(R.string.error_part_corrupted)

        override val isRetryable: Boolean = true // 可以重新下载该分片
    }

    /**
     * 文件完整性校验失败
     */
    class IntegrityCheckFailedException(
        val expected: String,
        val actual: String,
        cause: Throwable? = null
    ) : DownloadException("Integrity check failed: expected=$expected, actual=$actual", cause) {
        override fun getLocalizedMessage(context: Context): String =
            context.getString(R.string.error_integrity_check_failed)

        override val isRetryable: Boolean = true // 可以重新下载
    }

    // ============= 通用异常 =============

    /**
     * 未知错误
     */
    class UnknownException(
        message: String = "Unknown error",
        cause: Throwable? = null
    ) : DownloadException(message, cause) {
        override fun getLocalizedMessage(context: Context): String =
            context.getString(R.string.error_unknown)

        override val isRetryable: Boolean = false
    }

    companion object {
        /**
         * 从 HTTP 状态码创建对应的异常
         */
        fun fromHttpCode(code: Int, url: String): DownloadException {
            return when (code) {
                403 -> AccessDeniedException(url)
                404 -> ResourceNotFoundException(url)
                429 -> TooManyRequestsException()
                in 500..599 -> ServerErrorException(code)
                else -> UnknownException("HTTP error: $code")
            }
        }

        /**
         * 从普通异常转换为 DownloadException
         */
        fun from(throwable: Throwable): DownloadException {
            return when (throwable) {
                is DownloadException -> throwable
                is java.net.SocketTimeoutException -> NetworkTimeoutException(throwable)
                is java.net.UnknownHostException -> NetworkUnavailableException(throwable)
                is java.net.ConnectException -> NetworkUnavailableException(throwable)
                is java.net.SocketException -> ConnectionResetException(throwable)
                is javax.net.ssl.SSLException -> SSLCertificateException(throwable)
                is java.io.IOException -> FileWriteException("unknown", throwable)
                else -> UnknownException(throwable.message ?: "Unknown error", throwable)
            }
        }
    }
}
