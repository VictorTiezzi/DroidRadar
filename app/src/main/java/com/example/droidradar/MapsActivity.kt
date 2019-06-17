package com.example.droidradar

import android.arch.persistence.room.Room
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.example.droidradar.database.PointMap


import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.droidradar.database.AppDatabase

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap

    private lateinit var db: AppDatabase

    private lateinit var list: List<PointMap>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "point-bd"
        ).allowMainThreadQueries().build()

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
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

        list = db.pointMapDao().getAll()
        if (list.isEmpty()){
            populate()
        }
        list = db.pointMapDao().getAll()

        mMap = googleMap

        // Add a marker in Sydney and move the camera

        for(index in 0 until list.size){
            val point = LatLng(list[index].lat, list[index].lng)
            mMap.addMarker(MarkerOptions().position(point))
        }

        //mMap.moveCamera(CameraUpdateFactory.newLatLng(fortaleza))
    }
}
