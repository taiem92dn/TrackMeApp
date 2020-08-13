package com.tngdev.trackmeapp.data.source.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.tngdev.trackmeapp.data.model.Location
import com.tngdev.trackmeapp.data.model.Session

@Database(entities = [Session::class, Location::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase(){
    abstract fun sessionDao() : SessionDao
}