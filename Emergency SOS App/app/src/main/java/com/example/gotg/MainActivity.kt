package com.example.gotg

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.telephony.SmsManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var sensorManager: SensorManager
    private lateinit var shakeDetector: ShakeDetector
    private lateinit var voiceRecognitionService: VoiceRecognitionService
    private val contacts = mutableListOf<String>()
    private val smsManager: SmsManager by lazy {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }
    }

    private var isShakeEnabled = false
    private var isVoiceEnabled = false
    private var pendingCallContact = ""
    private var pendingSOS = false
    private var isVoiceButtonPressed by mutableStateOf(false)
    private var voiceResultHandled by mutableStateOf(false)
    private var pendingVoiceCallback: (() -> Unit)? = null

    private val emergencyServices = listOf(
        EmergencyService("Police", "911", "police"),
        EmergencyService("Medical", "911", "medical"),
        EmergencyService("Fire", "911", "fire")
    )

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[Manifest.permission.CALL_PHONE] == true -> {
                if (pendingCallContact.isNotEmpty()) {
                    initiateCall(pendingCallContact)
                    pendingCallContact = ""
                }
            }
            permissions[Manifest.permission.SEND_SMS] == true ||
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true -> {
                if (pendingSOS) {
                    sendSOS()
                    pendingSOS = false
                }
            }
            permissions[Manifest.permission.RECORD_AUDIO] == true -> {
                pendingVoiceCallback?.let { callback ->
                    voiceRecognitionService.startListening()
                    pendingVoiceCallback = null
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        setupShakeDetector()
        voiceRecognitionService = VoiceRecognitionService(this)
        loadContacts()

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(32.dp)
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Top Section - Settings
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            Text(
                                "Emergency SOS",
                                style = MaterialTheme.typography.headlineMedium,
                                textAlign = TextAlign.Center
                            )
                            
                            var shakeEnabled by remember { mutableStateOf(isShakeEnabled) }
                            
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Shake to Activate")
                                    Switch(
                                        checked = shakeEnabled,
                                        onCheckedChange = { enabled ->
                                            shakeEnabled = enabled
                                            isShakeEnabled = enabled
                                            updateShakeDetection(enabled)
                                        }
                                    )
                                }
                            }
                        }

                        // Middle Section - Emergency Services
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                "Emergency Services",
                                style = MaterialTheme.typography.titleMedium
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                emergencyServices.forEach { service ->
                                    EmergencyServiceButton(
                                        service = service,
                                        onClick = { initiateCall(service.number) }
                                    )
                                }
                            }
                        }

                        // Bottom Section - SOS and Contacts
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                EmergencyButton(
                                    onClick = { sendSOS() },
                                    icon = Icons.Default.Send,
                                    text = "Send SOS",
                                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                                )
                                
                                EmergencyButton(
                                    onClick = { callFirstContact() },
                                    icon = Icons.Default.Call,
                                    text = "Call Contact",
                                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                                )
                            }

                            // Voice Activation Toggle Button
                            var isVoiceActive by remember { mutableStateOf(false) }
                            Button(
                                onClick = {
                                    if (!isVoiceActive) {
                                        isVoiceActive = true
                                        voiceRecognitionService.startListening()
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Voice activated! Speak now, then click again to stop.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        isVoiceActive = false
                                        voiceRecognitionService.stopAndGetResult { result ->
                                            if (result != null && result.trim().equals("SOS", ignoreCase = true)) {
                                                sendSOS()
                                                Toast.makeText(
                                                    this@MainActivity,
                                                    "Passphrase matched! SOS sent.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } else {
                                                Toast.makeText(
                                                    this@MainActivity,
                                                    "No match. Try again.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isVoiceActive)
                                        MaterialTheme.colorScheme.tertiary
                                    else
                                        MaterialTheme.colorScheme.error
                                )
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Voice Activation",
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        if (isVoiceActive) "Stop & Check" else "Activate Voice",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                }
                            }
                            
                            Button(
                                onClick = { startActivity(Intent(this@MainActivity, ContactsActivity::class.java)) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Manage Contacts")
                            }
                        }
                    }
                }
            }
        }

        checkAndRequestPermissions()
    }

    private fun checkAndRequestVoicePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
        } else {
            startVoiceRecognition { sendSOS() }
        }
    }

    private fun startVoiceRecognition(onPhraseDetected: () -> Unit) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            pendingVoiceCallback = onPhraseDetected
            requestPermissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
        } else {
            voiceRecognitionService.startListening()
        }
    }

    @Composable
    private fun EmergencyServiceButton(
        service: EmergencyService,
        onClick: () -> Unit
    ) {
        val icon = when (service.icon) {
            "police" -> Icons.Default.Warning
            "medical" -> Icons.Default.Home
            "fire" -> Icons.Default.Call
            else -> Icons.Default.Send
        }

        Button(
            onClick = onClick,
            modifier = Modifier.size(100.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = service.name,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    service.name,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    @Composable
    private fun EmergencyButton(
        onClick: () -> Unit,
        icon: ImageVector,
        text: String,
        modifier: Modifier = Modifier
    ) {
        Button(
            onClick = onClick,
            modifier = modifier
                .height(120.dp)
                .padding(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = text,
                    modifier = Modifier.size(32.dp)
                )
                Text(text, style = MaterialTheme.typography.titleSmall)
            }
        }
    }

    private fun callFirstContact() {
        if (contacts.isEmpty()) {
            Toast.makeText(this, "Please add emergency contacts first", Toast.LENGTH_LONG).show()
            return
        }

        try {
            initiateCall(contacts[0])
        } catch (e: Exception) {
            Toast.makeText(this, "Error making call: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun initiateCall(phoneNumber: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) 
            != PackageManager.PERMISSION_GRANTED) {
            pendingCallContact = phoneNumber
            requestPermissionLauncher.launch(arrayOf(Manifest.permission.CALL_PHONE))
            return
        }

        try {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:${Uri.encode(phoneNumber)}")
            }
            startActivity(intent)
        } catch (e: SecurityException) {
            Toast.makeText(this, "Permission denied to make phone call", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupShakeDetector() {
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        shakeDetector = ShakeDetector {
            if (isShakeEnabled) {
                sendSOS()
            }
        }
    }

    private fun updateShakeDetection(enabled: Boolean) {
        if (enabled) {
            val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            sensorManager.registerListener(
                shakeDetector,
                accelerometer,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        } else {
            sensorManager.unregisterListener(shakeDetector)
        }
    }

    override fun onResume() {
        super.onResume()
        loadContacts()
        if (isShakeEnabled) {
            updateShakeDetection(true)
        }
        if (isVoiceEnabled) {
            checkAndRequestVoicePermission()
        }
    }

    override fun onPause() {
        super.onPause()
        updateShakeDetection(false)
        voiceRecognitionService.cancelListening()
    }

    private fun checkAndRequestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.SEND_SMS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.RECORD_AUDIO
        )

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest)
        }
    }

    private fun loadContacts() {
        val sharedPreferences = getSharedPreferences("EmergencyContacts", MODE_PRIVATE)
        contacts.clear()
        for (i in 1..3) {
            sharedPreferences.getString("contact${i}_number", null)?.let {
                if (it.isNotEmpty()) contacts.add(it)
            }
        }
    }

    private fun sendSOS() {
        if (contacts.isEmpty()) {
            Toast.makeText(this, "Please add emergency contacts first", Toast.LENGTH_LONG).show()
            return
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            pendingSOS = true
            requestPermissionLauncher.launch(arrayOf(
                Manifest.permission.SEND_SMS,
                Manifest.permission.ACCESS_FINE_LOCATION
            ))
            return
        }

        try {
            val cancellationTokenSource = CancellationTokenSource()
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.token)
                .addOnSuccessListener { location: Location? ->
                    location?.let {
                        val locationUri = Uri.parse("https://www.google.com/maps")
                            .buildUpon()
                            .appendPath("q")
                            .appendQueryParameter("q", "${it.latitude},${it.longitude}")
                            .build()
                            .toString()
                        
                        val message = "I need help. Here is my location: $locationUri"
                        
                        contacts.forEach { contact ->
                            smsManager.sendTextMessage(contact, null, message, null, null)
                        }
                        Toast.makeText(this, "SOS message sent to all contacts", Toast.LENGTH_LONG).show()
                    } ?: run {
                        Toast.makeText(this, "Could not get location", Toast.LENGTH_LONG).show()
                    }
                }
        } catch (e: Exception) {
            Toast.makeText(this, "Error sending SOS: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}