package com.example.droidradar.database

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

@Entity
data class PointMap (
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,
    val lat: Double,
    val lng: Double,
    val type: Int,
    val speed: Int,
    val dirType: Int,
    val direction: Int
)