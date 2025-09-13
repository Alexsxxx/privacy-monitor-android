package com.privacy.monitor

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    
    private lateinit var statusText: TextView
    private lateinit var switchText: TextView
    private lateinit var monitoringSwitch: Switch
    private lateinit var permissionsButton: Button
    private lateinit var sharedPreferences: SharedPreferences
    
    private val PERMISSIONS_REQUEST_CODE = 1001
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA,
        Manifest.permission.POST_NOTIFICATIONS
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        sharedPreferences = getSharedPreferences("PrivacyMonitor", MODE_PRIVATE)
        
        initViews()
        setupListeners()
        checkPermissions()
        restoreMonitoringState()
    }
    
    private fun initViews() {
        statusText = findViewById(R.id.statusText)
        switchText = findViewById(R.id.switchText)
        monitoringSwitch = findViewById(R.id.monitoringSwitch)
        permissionsButton = findViewById(R.id.permissionsButton)
    }
    
    private fun setupListeners() {
        monitoringSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                startMonitoringService()
                switchText.text = "Disable Monitoring"
            } else {
                stopMonitoringService()
                switchText.text = "Enable Monitoring"
            }
            saveMonitoringState(isChecked)
            updateStatus()
        }
        
        permissionsButton.setOnClickListener {
            requestPermissions()
        }
    }
    
    private fun checkPermissions() {
        val missingPermissions = REQUIRED_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (missingPermissions.isEmpty()) {
            permissionsButton.isEnabled = false
            permissionsButton.text = "Permissions Granted"
            monitoringSwitch.isEnabled = true
        } else {
            permissionsButton.isEnabled = true
            permissionsButton.text = "Grant Permissions"
            monitoringSwitch.isEnabled = false
        }
        
        updateStatus()
    }
    
    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            REQUIRED_PERMISSIONS,
            PERMISSIONS_REQUEST_CODE
        )
    }
    
    private fun startMonitoringService() {
        val serviceIntent = Intent(this, PrivacyMonitorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }
    
    private fun stopMonitoringService() {
        val serviceIntent = Intent(this, PrivacyMonitorService::class.java)
        stopService(serviceIntent)
    }
    
    private fun updateStatus() {
        val hasPermissions = REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
        
        when {
            !hasPermissions -> {
                statusText.text = "Permissions required to monitor privacy"
            }
            monitoringSwitch.isChecked -> {
                statusText.text = "Privacy monitoring is active"
            }
            else -> {
                statusText.text = "Privacy monitoring is disabled"
            }
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            checkPermissions()
        }
    }
    
    override fun onResume() {
        super.onResume()
        checkPermissions()
    }
    
    private fun saveMonitoringState(isEnabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean("monitoring_enabled", isEnabled)
            .apply()
    }
    
    private fun restoreMonitoringState() {
        val wasEnabled = sharedPreferences.getBoolean("monitoring_enabled", false)
        if (wasEnabled && hasAllPermissions()) {
            monitoringSwitch.isChecked = true
            switchText.text = "Disable Monitoring"
            startMonitoringService()
            updateStatus()
        } else {
            switchText.text = "Enable Monitoring"
        }
    }
    
    private fun hasAllPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}