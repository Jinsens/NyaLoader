package com.nyapass.loader.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.nyapass.loader.MainActivity
import java.util.concurrent.ConcurrentHashMap

/**
 * 下载通知前台服务
 */
class DownloadNotificationService : Service() {
    
    private val notificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
    
    // 存储每个下载任务的通知ID
    private val taskNotificationIds = ConcurrentHashMap<Long, Int>()
    private var nextNotificationId = NOTIFICATION_ID_START
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_FOREGROUND -> {
                startForegroundService()
            }
            ACTION_UPDATE_PROGRESS -> {
                updateProgress(intent)
            }
            ACTION_DOWNLOAD_COMPLETE -> {
                showCompleteNotification(intent)
            }
            ACTION_DOWNLOAD_FAILED -> {
                showFailedNotification(intent)
            }
            ACTION_STOP_FOREGROUND -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    /**
     * 创建通知渠道
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 下载进度通知渠道
            val progressChannel = NotificationChannel(
                CHANNEL_ID_PROGRESS,
                "下载进度",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "显示文件下载进度"
                setShowBadge(false)
            }
            
            // 下载完成通知渠道
            val completeChannel = NotificationChannel(
                CHANNEL_ID_COMPLETE,
                "下载完成",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "下载完成提醒"
                setShowBadge(true)
            }
            
            notificationManager.createNotificationChannels(
                listOf(progressChannel, completeChannel)
            )
        }
    }
    
    /**
     * 启动前台服务
     */
    private fun startForegroundService() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID_PROGRESS)
            .setContentTitle("NyaLoader")
            .setContentText("下载服务运行中")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        
        startForeground(NOTIFICATION_ID_FOREGROUND, notification)
    }
    
    /**
     * 更新下载进度
     */
    private fun updateProgress(intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1)
        val fileName = intent.getStringExtra(EXTRA_FILE_NAME) ?: "未知文件"
        val progress = intent.getIntExtra(EXTRA_PROGRESS, 0)
        @Suppress("UNUSED_VARIABLE")
        val downloadedSize = intent.getLongExtra(EXTRA_DOWNLOADED_SIZE, 0)
        @Suppress("UNUSED_VARIABLE")
        val totalSize = intent.getLongExtra(EXTRA_TOTAL_SIZE, 0)
        val speed = intent.getLongExtra(EXTRA_SPEED, 0)
        
        if (taskId == -1L) return
        
        // 获取或创建通知ID
        val notificationId = taskNotificationIds.getOrPut(taskId) {
            nextNotificationId++
        }
        
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        // 格式化速度
        val speedText = if (speed > 0) {
            val speedMB = speed / 1024.0 / 1024.0
            if (speedMB >= 1) {
                String.format("%.2f MB/s", speedMB)
            } else {
                String.format("%.2f KB/s", speed / 1024.0)
            }
        } else {
            "计算中..."
        }
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID_PROGRESS)
            .setContentTitle(fileName)
            .setContentText("${progress}% - $speedText")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .build()
        
        notificationManager.notify(notificationId, notification)
    }
    
    /**
     * 显示下载完成通知
     */
    private fun showCompleteNotification(intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1)
        val fileName = intent.getStringExtra(EXTRA_FILE_NAME) ?: "未知文件"
        
        if (taskId == -1L) return
        
        val notificationId = taskNotificationIds[taskId] ?: return
        
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID_COMPLETE)
            .setContentTitle("下载完成")
            .setContentText(fileName)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        
        notificationManager.notify(notificationId, notification)
        taskNotificationIds.remove(taskId)
    }
    
    /**
     * 显示下载失败通知
     */
    private fun showFailedNotification(intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1)
        val fileName = intent.getStringExtra(EXTRA_FILE_NAME) ?: "未知文件"
        val error = intent.getStringExtra(EXTRA_ERROR) ?: "未知错误"
        
        if (taskId == -1L) return
        
        val notificationId = taskNotificationIds[taskId] ?: return
        
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID_COMPLETE)
            .setContentTitle("下载失败")
            .setContentText("$fileName - $error")
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        
        notificationManager.notify(notificationId, notification)
        taskNotificationIds.remove(taskId)
    }
    
    companion object {
        private const val CHANNEL_ID_PROGRESS = "download_progress"
        private const val CHANNEL_ID_COMPLETE = "download_complete"
        private const val NOTIFICATION_ID_FOREGROUND = 1
        private const val NOTIFICATION_ID_START = 100
        
        const val ACTION_START_FOREGROUND = "ACTION_START_FOREGROUND"
        const val ACTION_UPDATE_PROGRESS = "ACTION_UPDATE_PROGRESS"
        const val ACTION_DOWNLOAD_COMPLETE = "ACTION_DOWNLOAD_COMPLETE"
        const val ACTION_DOWNLOAD_FAILED = "ACTION_DOWNLOAD_FAILED"
        const val ACTION_STOP_FOREGROUND = "ACTION_STOP_FOREGROUND"
        
        const val EXTRA_TASK_ID = "EXTRA_TASK_ID"
        const val EXTRA_FILE_NAME = "EXTRA_FILE_NAME"
        const val EXTRA_PROGRESS = "EXTRA_PROGRESS"
        const val EXTRA_DOWNLOADED_SIZE = "EXTRA_DOWNLOADED_SIZE"
        const val EXTRA_TOTAL_SIZE = "EXTRA_TOTAL_SIZE"
        const val EXTRA_SPEED = "EXTRA_SPEED"
        const val EXTRA_ERROR = "EXTRA_ERROR"
        
        /**
         * 启动前台服务
         */
        fun startService(context: Context) {
            val intent = Intent(context, DownloadNotificationService::class.java).apply {
                action = ACTION_START_FOREGROUND
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        /**
         * 更新进度
         */
        fun updateProgress(
            context: Context,
            taskId: Long,
            fileName: String,
            progress: Int,
            downloadedSize: Long,
            totalSize: Long,
            speed: Long
        ) {
            val intent = Intent(context, DownloadNotificationService::class.java).apply {
                action = ACTION_UPDATE_PROGRESS
                putExtra(EXTRA_TASK_ID, taskId)
                putExtra(EXTRA_FILE_NAME, fileName)
                putExtra(EXTRA_PROGRESS, progress)
                putExtra(EXTRA_DOWNLOADED_SIZE, downloadedSize)
                putExtra(EXTRA_TOTAL_SIZE, totalSize)
                putExtra(EXTRA_SPEED, speed)
            }
            context.startService(intent)
        }
        
        /**
         * 通知下载完成
         */
        fun notifyComplete(context: Context, taskId: Long, fileName: String) {
            val intent = Intent(context, DownloadNotificationService::class.java).apply {
                action = ACTION_DOWNLOAD_COMPLETE
                putExtra(EXTRA_TASK_ID, taskId)
                putExtra(EXTRA_FILE_NAME, fileName)
            }
            context.startService(intent)
        }
        
        /**
         * 通知下载失败
         */
        fun notifyFailed(context: Context, taskId: Long, fileName: String, error: String) {
            val intent = Intent(context, DownloadNotificationService::class.java).apply {
                action = ACTION_DOWNLOAD_FAILED
                putExtra(EXTRA_TASK_ID, taskId)
                putExtra(EXTRA_FILE_NAME, fileName)
                putExtra(EXTRA_ERROR, error)
            }
            context.startService(intent)
        }
        
        /**
         * 停止服务
         */
        fun stopService(context: Context) {
            val intent = Intent(context, DownloadNotificationService::class.java).apply {
                action = ACTION_STOP_FOREGROUND
            }
            context.startService(intent)
        }
    }
}
