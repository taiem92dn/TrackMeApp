package com.tngdev.trackmeapp.data.model

import androidx.room.Entity

@Entity(tableName = "locations")
data class Location(
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var speed: Float = 0f,
    var time: Long
){
}