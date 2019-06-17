package com.example.droidradar.database

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase

@Database(entities = [PointMap::class], version = 3)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pointMapDao(): PointMapDao
}