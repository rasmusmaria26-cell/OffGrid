package com.servify.resqmesh

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.servify.resqmesh.ui.OffGridApp
import com.servify.resqmesh.ui.theme.OffGridTheme

class MainActivity : ComponentActivity() {

    companion object {
        private const val PREFS_NAME = "offgrid_prefs"
        private const val KEY_USERNAME = "username"
    }

    private val prefs by lazy { getSharedPreferences(PREFS_NAME, MODE_PRIVATE) }

    private val nearbyManager by lazy {
        // Load persisted name, or generate a fresh one if first launch
        val savedName = prefs.getString(KEY_USERNAME, "")
        val name = if (!savedName.isNullOrBlank()) savedName
                   else NearbyManager.generateNickname()
        NearbyManager(this, name)
    }

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Permissions denied. App may not work.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAndRequestPermissions()

        setContent {
            OffGridApp(
                nearbyManager = nearbyManager,
                onNameConfirmed = { name ->
                    // Persist name to SharedPreferences whenever user confirms on onboarding
                    prefs.edit().putString(KEY_USERNAME, name).apply()
                }
            )
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            requestPermissionsLauncher.launch(missingPermissions.toTypedArray())
        }
    }
}