package com.tngdev.trackmeapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "locations")
data class Location(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var speed: Float = 0f,
    var time: Long,
    var sessionId: String
){
}