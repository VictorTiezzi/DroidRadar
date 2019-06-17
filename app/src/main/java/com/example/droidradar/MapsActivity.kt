package com.example.droidradar


import android.app.Activity
import android.arch.persistence.room.Room
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.widget.SeekBar
import com.example.droidradar.database.AppDatabase
import com.example.droidradar.database.PointMap
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private var mMap: GoogleMap? = null
    private lateinit var db: AppDatabase
    private lateinit var list: List<PointMap>

    private lateinit var listOfPoints: MutableList<Marker>
    private var circle: Circle? = null

    private var lastLocation: Location? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private var locationUpdateState = false

    private lateinit var mSeekBar: SeekBar
    private var radius: Double = 0.0

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val REQUEST_CHECK_SETTINGS = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mSeekBar = findViewById(R.id.map_seekBar)

        mSeekBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                radius = progress.toDouble()
                attCircle()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)
                lastLocation = p0.lastLocation
                attCircle()
            }
        }

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "point-bd"
        ).allowMainThreadQueries().build()

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        createLocationRequest()
    }

    fun attCircle(){
        if(circle != null) {
            circle!!.remove()
        }
        if(mMap != null && lastLocation != null){

            val currentLatLng = LatLng(lastLocation!!.latitude, lastLocation!!.longitude)
            val circleOptions = CircleOptions().center(currentLatLng).radius((radius * 100.0) + 1000.0)
            circle = mMap!!.addCircle(circleOptions)

            for(index in 0 until listOfPoints.size){
                val point = LatLng(listOfPoints[index].position.latitude,listOfPoints[index].position.longitude)
                listOfPoints[index].isVisible = (radius * 100) + 1000 >= getDistance(point,currentLatLng)
            }

        }
    }
    private fun rad(x: Double): Double {
        return x * Math.PI / 180
    }

    private fun getDistance(p1: LatLng, p2: LatLng): Double{
        val earthRadius = 6378137 // Earth’s mean radius in meter
        val dLat = rad(p2.latitude - p1.latitude)
        val dLong = rad(p2.longitude - p1.longitude)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(rad(p1.latitude)) * Math.cos(rad(p2.latitude)) *
                Math.sin(dLong / 2) * Math.sin(dLong / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return earthRadius * c // returns the distance in meter
    }

    fun populate() {
        val mlist: MutableList<PointMap> = arrayListOf()

        val file = assets.open("maparadar.in").bufferedReader(Charsets.UTF_8)

        var line = file.readLine()
        while (line != null){
            val stringList = line.split(",")

            val point = PointMap(
                id = null,
                lng = stringList[0].toDouble(),
                lat = stringList[1].toDouble(),
                type = stringList[2].toInt(),
                speed = stringList[3].toInt(),
                dirType = stringList[4].toInt(),
                direction = stringList[5].toInt()
            )

            mlist.add(point)
            line = file.readLine()
        }
        db.pointMapDao().insert(mlist)
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap!!.uiSettings.isZoomControlsEnabled = true
        mMap!!.uiSettings.isRotateGesturesEnabled = false
        mMap!!.uiSettings.isScrollGesturesEnabledDuringRotateOrZoom = false


        setUpMap()

        list = db.pointMapDao().getAll()
        if (list.isEmpty()){
            populate()
        }
        list = db.pointMapDao().getAll()



        val maxA = -2.465143
        val minA = -8.092522
        val maxB = -36.849999
        val minB = -41.626292
        listOfPoints = arrayListOf()

        for(index in 0 until list.size){
            if(list[index].lat > minA && list[index].lat < maxA
                && list[index].lng > minB && list[index].lng < maxB){

                val point = LatLng(list[index].lat, list[index].lng)
                val marker = MarkerOptions()
                    .position(point)
                    .rotation(list[index].direction.toFloat()+180)

                listOfPoints.add(mMap!!.addMarker(marker))
            }
        }

        //mMap.moveCamera(CameraUpdateFactory.newLatLng(fortaleza))
    }

    private fun setUpMap() {
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
            return
        }
        mMap!!.isMyLocationEnabled = true

        fusedLocationClient.lastLocation.addOnSuccessListener(this) { location ->
            // Got last known location. In some rare situations this can be null.
            // 3
            if (location != null) {
                lastLocation = location
                val currentLatLng = LatLng(location.latitude, location.longitude)
                mMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 12f))
            }
        }
    }
    private fun startLocationUpdates() {
        //1
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE)
            return
        }
        //2
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null /* Looper */)
    }

    private fun createLocationRequest() {
        // 1
        locationRequest = LocationRequest()
        // 2
        locationRequest.interval = 10000
        // 3
        locationRequest.fastestInterval = 5000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        // 4
        val client = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(builder.build())

        // 5
        task.addOnSuccessListener {
            locationUpdateState = true
            startLocationUpdates()
        }
        task.addOnFailureListener { e ->
            // 6
            if (e is ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    e.startResolutionForResult(this@MapsActivity,
                        REQUEST_CHECK_SETTINGS)
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == Activity.RESULT_OK) {
                locationUpdateState = true
                startLocationUpdates()
            }
        }
    }

    // 2
    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    // 3
    public override fun onResume() {
        super.onResume()
        if (!locationUpdateState) {
            startLocationUpdates()
        }
    }
}

