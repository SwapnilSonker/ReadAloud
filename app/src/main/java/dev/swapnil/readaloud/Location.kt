package dev.swapnil.readaloud

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.widget.doOnTextChanged
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import dev.swapnil.readaloud.databinding.ActivityLocationBinding


const val REQUEST_LOCATION = 0   //Request Code Location
const val REQUEST_CHECK_SETTINGS = 1
var LocationUpdateInterval: Long = 10000 //milliSec
var MIN_UPDATE_INTERVAL = 5000
var requestingLocationUpdates = false

class Location : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var binding: ActivityLocationBinding
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLocationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestPermissions()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult

                for (location in locationResult.locations) {
                    //Do something with location HERE
                    binding.tvLocation.text = "${location.latitude} ${location.longitude}"
                    binding.tvTime.text = java.util.Calendar.getInstance().time.toString()
                }
            }
        }

        binding.switchLocationUpdate.setOnCheckedChangeListener { _, isChecked ->
            if(checkFineLocationPermission() && checkCoarseLocationPermission()){
                if (isChecked) {
                    requestingLocationUpdates = true
                    createLocationRequest()
                } else {
                    requestingLocationUpdates = false
                    stopLocationUpdates()
                }
            }else{
                requestPermissions()
                binding.switchLocationUpdate.isChecked = false
                showPermissionExplanation()
            }

        }

        binding.etIntervalTime.doOnTextChanged { text, start, before, count ->
            if (text!!.isNotEmpty()) {
                val interval = Integer.parseInt(text.toString())
                if (interval >= MIN_UPDATE_INTERVAL) {
                    LocationUpdateInterval = interval.toLong()

                    if (::locationRequest.isInitialized) {
                        locationRequest.interval = LocationUpdateInterval
                        locationRequest.fastestInterval = LocationUpdateInterval
                        stopLocationUpdates()
                        createLocationRequest()
                    }
                }
            }

        }

    }

    //Need coarse location permission for Fine Location
    private fun checkCoarseLocationPermission() = ActivityCompat.checkSelfPermission(
        this, android.Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    private fun checkFineLocationPermission() = ActivityCompat.checkSelfPermission(
        this, android.Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    private fun checkApiVersion() =
        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M

    private fun requestPermissions() {
        if (checkApiVersion()) {

            val permissionList = mutableListOf<String>()

            if (!checkCoarseLocationPermission()) {
                permissionList.add(android.Manifest.permission.ACCESS_COARSE_LOCATION)
            }

            if (!checkFineLocationPermission()) {
                permissionList.add(android.Manifest.permission.ACCESS_FINE_LOCATION)
            }

            if (permissionList.isNotEmpty()) {
                ActivityCompat.requestPermissions(
                    this,
                    permissionList.toTypedArray(),
                    REQUEST_LOCATION
                )
            }
        }
    }

    private fun showPermissionExplanation() {
        Toast.makeText(this, "Need that Permission Bitch!", Toast.LENGTH_SHORT).show()
    }

    private fun createLocationRequest() {

        locationRequest = LocationRequest.create().apply {
            interval = LocationUpdateInterval
            fastestInterval = LocationUpdateInterval
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        //checks if GPS Enabled, and prompts for the same
        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener { locationSettingsResponse ->
            startLocationUpdates(locationRequest)
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    exception.startResolutionForResult(
                        this@Location,
                        REQUEST_CHECK_SETTINGS
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }
    }

    private fun startLocationUpdates(locationRequest: LocationRequest) {
        if (checkFineLocationPermission()) {
            binding.tvLocation.text = "Getting Location..."

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }else{
            requestPermissions()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == Activity.RESULT_OK) {
                createLocationRequest()
            } else {
                Toast.makeText(this, "GPS Error", Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)

        binding.tvLocation.text = "Location Updates Stopped"
    }

    override fun onResume() {
        super.onResume()
        if (requestingLocationUpdates) createLocationRequest()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_LOCATION) {
            for (i in grantResults.indices) {
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    if (checkApiVersion()) {
                        if (shouldShowRequestPermissionRationale(permissions[i])) {
                            showPermissionExplanation()
                        }
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }


}