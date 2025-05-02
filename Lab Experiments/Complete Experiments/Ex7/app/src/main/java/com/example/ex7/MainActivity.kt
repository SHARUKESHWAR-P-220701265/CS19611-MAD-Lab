package com.example.ex7

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Locale

class MainActivity : AppCompatActivity(), LocationListener {

    private lateinit var ll: TextView
    private lateinit var addr: TextView
    private lateinit var locationManager: LocationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ll = findViewById(R.id.lola)
        addr = findViewById(R.id.addr)
        val getloc = findViewById<Button>(R.id.getLoc)

        // Request location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                101
            )
        }

        getloc.setOnClickListener {
            Log.d("ButtonClick", "Get Location Clicked")
            getLocation()
            Toast.makeText(this, "Requesting Location...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getLocation() {
        try {
            locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
            ) {
                // Request updates from both GPS and Network
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    5000,
                    0f,
                    this,
                    mainLooper
                )

                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    5000,
                    0f,
                    this,
                    mainLooper
                )

                // Check last known location as fallback
                val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (lastLocation != null) {
                    Log.d("LastKnownLocation", "Lat: ${lastLocation.latitude}, Lon: ${lastLocation.longitude}")
                    onLocationChanged(lastLocation)
                } else {
                    Log.d("LastKnownLocation", "No last known location available")
                }

            } else {
                Toast.makeText(this, "Permission not granted", Toast.LENGTH_SHORT).show()
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    override fun onLocationChanged(location: Location) {
        Log.d("LocationChanged", "Location updated!")

        val text = "Latitude: ${location.latitude}\nLongitude: ${location.longitude}"
        ll.text = text

        try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addressList = geocoder.getFromLocation(location.latitude, location.longitude, 1)

            if (!addressList.isNullOrEmpty()) {
                val fullAddress = addressList[0].getAddressLine(0)
                addr.text = fullAddress
                Log.d("Geocoder", "Address: $fullAddress")
            } else {
                addr.text = "Address not found"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            addr.text = "Geocoder error"
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onProviderEnabled(provider: String) {
        Log.d("ProviderEnabled", "Enabled: $provider")
    }

    override fun onProviderDisabled(provider: String) {
        Log.d("ProviderDisabled", "Disabled: $provider")
        Toast.makeText(this, "Please enable GPS and Internet", Toast.LENGTH_SHORT).show()
    }

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
}
