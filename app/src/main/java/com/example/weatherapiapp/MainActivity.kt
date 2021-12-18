package com.example.weatherapiapp

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import com.example.weatherapiapp.models.WeatherResponse
import com.example.weatherapiapp.network.WeatherService
import com.google.android.gms.location.*
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class MainActivity : AppCompatActivity() {

    private lateinit var mFusedLocationClient: FusedLocationProviderClient
//    private var mProgressDialog: Dialog? = null
    private var mLatitude: Double = 0.0
    private var mLongitude: Double = 0.0
//    private lateinit var mSharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this@MainActivity)

        if(!isLocationEnabled()){
            Toast.makeText(this@MainActivity,"Location is turned OFF.Please turn it on."
            ,Toast.LENGTH_SHORT).show()

            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        }else{
            Dexter.withActivity(this@MainActivity)
                .withPermissions(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
                .withListener(object : MultiplePermissionsListener{
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        if(report!!.areAllPermissionsGranted()){
                            requestLocationData()
                        }

                        if(report.isAnyPermissionPermanentlyDenied){
                            Toast.makeText(this@MainActivity,"You have denied permissions",
                            Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        permissions: MutableList<PermissionRequest>?,
                        token: PermissionToken?
                    ) {
                        showRationalDialogForPermissions()
                    }
                }).onSameThread()
                .check()
        }
    }

    private fun showRationalDialogForPermissions() {
        AlertDialog.Builder(this@MainActivity)
            .setMessage("It Looks like you have turned off permissions required for this feature. It can be enabled under Application Settings")
            .setPositiveButton(
                "GO TO SETTINGS"
            ) { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }.show()
    }


    /**
     * A function which is used to verify that the location or GPS is enable or not of the user's device.
     */
    private fun isLocationEnabled(): Boolean {

        // This provides access to the system location services.
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationData() {

        val mLocationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this@MainActivity)
        Looper.myLooper()?.let {
            mFusedLocationClient.requestLocationUpdates(
                mLocationRequest, mLocationCallback,
                it
            )
        }


    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {

            val mLastLocation: Location = locationResult.lastLocation
            mLatitude = mLastLocation.latitude
            Log.e("Current Latitude", "$mLatitude")
            mLongitude = mLastLocation.longitude
            Log.e("Current Longitude", "$mLongitude")

            getLocationWeatherDetails(mLatitude,mLongitude)
        }
    }

    private fun getLocationWeatherDetails(latitude: Double,longitude: Double){
        if(Constants.isNetworkAvailable(this@MainActivity)){
            val retrofit: Retrofit = Retrofit
                .Builder()
                .baseUrl(Constants.BASE_URL)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()

            val service: WeatherService = retrofit
                .create(WeatherService::class.java)

            val listCall: Call<WeatherResponse> = service.getWeather(
                latitude,longitude,Constants.METRIC_UNIT,Constants.APP_ID
            )

            listCall.enqueue(object : Callback<WeatherResponse>{
                override fun onResponse(
                    call: Call<WeatherResponse>,
                    response: Response<WeatherResponse>
                ) {
                    if(response.isSuccessful){
                        val weatherList: WeatherResponse? = response.body()
                        Log.i("Response Result","$weatherList")

                    }else{
                        when(response.code()){
                            400 -> {
                                Log.e("Error 400","Bad Connection")
                            }
                            404 ->{
                                Log.e("Error 404","Not Found")
                            }
                            else ->{
                                Log.e("Error","Genetic Error")
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                    Log.e("Erroorr", t.message.toString())
                }

            })

        }else{
            Toast.makeText(this@MainActivity,
                "No internet connection available.",
                Toast.LENGTH_SHORT).show()
        }
    }


}