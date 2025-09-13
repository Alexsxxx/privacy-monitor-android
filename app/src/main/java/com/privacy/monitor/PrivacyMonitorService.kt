package com.privacy.monitor

import android.app.*
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraCharacteristics
import android.util.Log
import android.media.AudioManager
import android.media.AudioRecordingConfiguration
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class PrivacyMonitorService : Service() {
    
    private lateinit var cameraManager: CameraManager
    private lateinit var audioManager: AudioManager
    private lateinit var notificationManager: NotificationManagerCompat
    private val handler = Handler(Looper.getMainLooper())
    
    private var isCameraInUse = false
    private var isMicrophoneInUse = false
    private var isInitializing = true
    
    companion object {
        const val CHANNEL_ID = "privacy_monitor_channel"
        const val ALERT_CHANNEL_ID = "privacy_alerts_channel"
        const val NOTIFICATION_ID = 1001
        const val CAMERA_NOTIFICATION_ID = 1002  
        const val MIC_NOTIFICATION_ID = 1003
        const val COMBINED_NOTIFICATION_ID = 1004
    }
    
    private val cameraAvailabilityCallback = object : CameraManager.AvailabilityCallback() {
        override fun onCameraAvailable(cameraId: String) {
            val wasInUse = isCameraInUse
            isCameraInUse = false
            if (!isInitializing && wasInUse) {
                updateNotifications()
            }
        }
        
        override fun onCameraUnavailable(cameraId: String) {
            isCameraInUse = true
            if (!isInitializing) {
                updateNotifications()
            }
        }
    }
    
    private val audioRecordingCallback = object : AudioManager.AudioRecordingCallback() {
        override fun onRecordingConfigChanged(configs: MutableList<AudioRecordingConfiguration>?) {
            val isRecording = configs?.isNotEmpty() == true
            isMicrophoneInUse = isRecording
            
            updateNotifications()
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        
        try {
            cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            notificationManager = NotificationManagerCompat.from(this)
            
            createNotificationChannel()
            startMonitoring()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createForegroundNotification())
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            
            // Service channel (quiet)
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Privacy Monitor Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background monitoring service"
            }
            
            // Alert channel (loud with sound and vibration)
            val alertChannel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "Privacy Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Camera and microphone usage alerts"
                setSound(soundUri, null)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
                setBypassDnd(true)
                setShowBadge(true)
            }
            
            notificationManager.createNotificationChannel(serviceChannel)
            notificationManager.createNotificationChannel(alertChannel)
        }
    }
    
    private fun createForegroundNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Privacy Monitor Active")
            .setContentText("Monitoring camera and microphone usage")
            .setSmallIcon(android.R.drawable.ic_lock_power_off)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
    
    private fun startMonitoring() {
        try {
            cameraManager.registerAvailabilityCallback(cameraAvailabilityCallback, handler)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                audioManager.registerAudioRecordingCallback(audioRecordingCallback, handler)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        handler.postDelayed({
            isInitializing = false
        }, 1000)
    }
    
    private fun showCameraStatusNotification(inUse: Boolean) {
        val title = if (inUse) "Camera in Use" else "Camera Available"
        val text = if (inUse) {
            "An app is using your camera"
        } else {
            "Camera is no longer in use"
        }
        val icon = if (inUse) android.R.drawable.ic_menu_camera else android.R.drawable.ic_menu_view
        
        val notification = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(icon)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()
            
        notificationManager.notify(CAMERA_NOTIFICATION_ID, notification)
    }
    
    private fun showMicrophoneStatusNotification(inUse: Boolean) {
        val title = if (inUse) "Microphone in Use" else "Microphone Available"
        val text = if (inUse) {
            "An app is using your microphone"
        } else {
            "Microphone is no longer in use"
        }
        val icon = if (inUse) android.R.drawable.ic_btn_speak_now else android.R.drawable.ic_notification_clear_all
        
        val notification = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(icon)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()
            
        notificationManager.notify(MIC_NOTIFICATION_ID, notification)
    }
    
    private fun updateNotifications() {
        // Zrušíme staré jednotlivé notifikace
        notificationManager.cancel(CAMERA_NOTIFICATION_ID)
        notificationManager.cancel(MIC_NOTIFICATION_ID)
        notificationManager.cancel(COMBINED_NOTIFICATION_ID)
        
        when {
            isCameraInUse && isMicrophoneInUse -> {
                // Obě periférie se používají - kombinované oznámení
                showCombinedStatusNotification()
            }
            isCameraInUse -> {
                // Pouze kamera
                showCameraStatusNotification(true)
            }
            isMicrophoneInUse -> {
                // Pouze mikrofon
                showMicrophoneStatusNotification(true)
            }
            else -> {
                // Nic se nepoužívá - žádné oznámení
            }
        }
    }
    
    private fun showCombinedStatusNotification() {
        val title = "Camera & Microphone in Use"
        val text = "An app is using your camera and microphone"
        val icon = android.R.drawable.ic_menu_camera
        
        val notification = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(icon)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()
            
        notificationManager.notify(COMBINED_NOTIFICATION_ID, notification)
    }
    

    override fun onDestroy() {
        super.onDestroy()
        cameraManager.unregisterAvailabilityCallback(cameraAvailabilityCallback)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            audioManager.unregisterAudioRecordingCallback(audioRecordingCallback)
        }
    }
}