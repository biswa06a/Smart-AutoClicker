package com.buzbuz.smartautoclicker.actions

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val startButton = findViewById<Button>(R.id.startButton)
        val stopButton = findViewById<Button>(R.id.stopButton)

        startButton.setOnClickListener {
            requestAccessibilityPermission()
            startAutoClickerService()
        }

        stopButton.setOnClickListener {
            stopAutoClickerService()
        }
    }

    /** Starts the AutoClicker background service */
    private fun startAutoClickerService() {
        val serviceIntent = Intent(this, SmartAutoClickerService::class.java)
        startService(serviceIntent)
    }

    /** Stops the AutoClicker background service */
    private fun stopAutoClickerService() {
        val serviceIntent = Intent(this, SmartAutoClickerService::class.java)
        stopService(serviceIntent)
    }

    /** Requests Accessibility permission if not already granted */
    private fun requestAccessibilityPermission() {
        if (!isAccessibilityServiceEnabled(SmartAutoClickerService::class.java)) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }
    }

    /** Checks if Accessibility Service is enabled */
    private fun isAccessibilityServiceEnabled(service: Class<out AccessibilityService>): Boolean {
        val componentName = ComponentName(this, service)
        val enabledServices =
            Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
                ?: return false
        
        return enabledServices.split(":").contains(componentName.flattenToString())
    }
}
