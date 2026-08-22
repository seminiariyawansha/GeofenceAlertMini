package com.example.geofencealertmini

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

class MainActivity : AppCompatActivity() {

    private lateinit var geofencingClient: GeofencingClient
    private lateinit var statusText: TextView
    private lateinit var activeGeofenceLabel: TextView
    private lateinit var logText: TextView

    // Sample fixed point — replace with real coordinates near you (e.g. University of Kelaniya)
    private val geofenceLat = 6.9724
    private val geofenceLng = 79.9200
    private val geofenceRadius = 200f // meters
    private val geofenceId = "KLN_CAMPUS_GEOFENCE"

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        if (fineGranted) {
            addGeofence()
        } else {
            Toast.makeText(this, "Location permission is required", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        activeGeofenceLabel = findViewById(R.id.activeGeofenceLabel)
        logText = findViewById(R.id.logText)
        geofencingClient = LocationServices.getGeofencingClient(this)

        val registerButton = findViewById<android.widget.Button>(R.id.registerGeofenceButton)
        registerButton.setOnClickListener {
            checkPermissionsAndRegister()
        }
    }

    private fun checkPermissionsAndRegister() {
        val permissionsNeeded = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissionsNeeded.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
        permissionLauncher.launch(permissionsNeeded.toTypedArray())
    }

    private fun addGeofence() {
        val geofence = Geofence.Builder()
            .setRequestId(geofenceId)
            .setCircularRegion(geofenceLat, geofenceLng, geofenceRadius)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
            .build()

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            return
        }

        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)
            .addOnSuccessListener {
                activeGeofenceLabel.text = "Campus — lat $geofenceLat, lng $geofenceLng, radius ${geofenceRadius}m"
                statusText.text = "Geofence registered"
                findViewById<android.view.View>(R.id.statusDot).setBackgroundColor(
                    resources.getColor(R.color.status_dot_active, theme)
                )
                appendLog("Geofence registered successfully")
            }
            .addOnFailureListener { e ->
                statusText.text = "Status: Registration failed"
                appendLog("Failed to register geofence: ${e.message}")
            }
    }

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(this, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    private fun appendLog(message: String) {
        logText.append("\n$message")
    }
}