package com.example.droidradar.database

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.Query

@Dao
interface PointMapDao {
    @Query("SELECT * FROM PointMap")
    fun getAll(): List<PointMap>

    @Insert
    fun insert(pointMap: List<PointMap>)
}