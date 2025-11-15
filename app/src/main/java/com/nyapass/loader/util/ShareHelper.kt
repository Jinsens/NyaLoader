package com.nyapass.loader.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import com.nyapass.loader.R
import com.nyapass.loader.data.model.DownloadTask
import java.io.File

/**
 * 分享工具类
 * 支持分享下载链接和已下载的文件
 *
 * @author 小花生FMR
 * @version 1.0.0
 */
object ShareHelper {
    private const val TAG = "ShareHelper"

    /**
     * 分享下载链接
     *
     * @param context 上下文
     * @param url 下载链接
     * @param fileName 文件名（可选，用于分享描述）
     */
    fun shareDownloadLink(context: Context, url: String, fileName: String? = null) {
        try {
            val shareText = if (!fileName.isNullOrBlank()) {
                "$fileName\n$url"
            } else {
                url
            }

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
                if (!fileName.isNullOrBlank()) {
                    putExtra(Intent.EXTRA_SUBJECT, fileName)
                }
            }

            val chooser = Intent.createChooser(
                intent,
                context.getString(R.string.share_link)
            )
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)

            Log.i(TAG, "分享链接: $url")
        } catch (e: Exception) {
            Log.e(TAG, "分享链接失败: ${e.message}", e)
        }
    }

    /**
     * 分享已下载的文件
     *
     * @param context 上下文
     * @param task 下载任务
     * @return 是否成功启动分享
     */
    fun shareFile(context: Context, task: DownloadTask): Boolean {
        return try {
            val uri = getFileUri(context, task) ?: run {
                Log.e(TAG, "无法获取文件URI")
                return false
            }

            val mimeType = getMimeType(task)

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(
                intent,
                context.getString(R.string.share_file)
            )
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)

            Log.i(TAG, "分享文件: ${task.fileName}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "分享文件失败: ${e.message}", e)
            false
        }
    }

    /**
     * 分享多个文件
     *
     * @param context 上下文
     * @param tasks 下载任务列表
     * @return 是否成功启动分享
     */
    fun shareMultipleFiles(context: Context, tasks: List<DownloadTask>): Boolean {
        return try {
            val uris = ArrayList<Uri>()
            var mimeType: String? = null

            for (task in tasks) {
                val uri = getFileUri(context, task)
                if (uri != null) {
                    uris.add(uri)
                    // 确定共同的MIME类型
                    val taskMimeType = getMimeType(task)
                    mimeType = when {
                        mimeType == null -> taskMimeType
                        mimeType == taskMimeType -> mimeType
                        mimeType.substringBefore("/") == taskMimeType.substringBefore("/") ->
                            "${mimeType.substringBefore("/")}/*"
                        else -> "*/*"
                    }
                }
            }

            if (uris.isEmpty()) {
                Log.e(TAG, "没有可分享的文件")
                return false
            }

            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = mimeType ?: "*/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(
                intent,
                context.getString(R.string.share_files, uris.size)
            )
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)

            Log.i(TAG, "分享 ${uris.size} 个文件")
            true
        } catch (e: Exception) {
            Log.e(TAG, "分享多个文件失败: ${e.message}", e)
            false
        }
    }

    /**
     * 获取文件URI
     */
    private fun getFileUri(context: Context, task: DownloadTask): Uri? {
        // 优先使用SAF URI
        val safUri = task.finalContentUri ?: task.filePath.takeIf { it.startsWith("content://") }
        if (safUri != null) {
            return try {
                val uri = Uri.parse(safUri)
                val document = DocumentFile.fromSingleUri(context, uri)
                if (document?.exists() == true) uri else null
            } catch (e: Exception) {
                Log.w(TAG, "SAF URI无效: $safUri")
                null
            }
        }

        // 使用FileProvider
        return try {
            val file = File(task.filePath)
            if (file.exists()) {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "FileProvider获取URI失败: ${e.message}")
            null
        }
    }

    /**
     * 获取MIME类型
     */
    private fun getMimeType(task: DownloadTask): String {
        // 优先使用任务中保存的MIME类型
        task.mimeType?.takeIf { it.isNotBlank() }?.let { return it }

        // 从文件扩展名推断
        val extension = task.fileName.substringAfterLast('.', "").lowercase()
        if (extension.isNotEmpty()) {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)?.let { return it }
        }

        return "application/octet-stream"
    }
}
