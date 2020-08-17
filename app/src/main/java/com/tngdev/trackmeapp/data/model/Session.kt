package com.tngdev.trackmeapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey var id:String = UUID.randomUUID().toString(),
    var name: String = "",
    var distance: Double = 0.0,  /* meter */
    var avgSpeed: Double = 0.0,  /* meter / second */
    var startTime: Long = System.currentTimeMillis(),
    var endTime: Long = 0,
    var duration: Long = 0, // second
    var isPause: Boolean = false,
    var isResumingAfterPause: Boolean = false,
    /**
     * Use to ignore some locations after resuming from paused status
     */
    var ignoreLocationsAfterPauseCount: Int = 0,
    var thumbnailPath: String = "",
    /**
     * Use to create LatLngBound for zoom map camera fit all routes
     */
    var minLat: Double? = null,
    var minLng: Double? = null,
    var maxLat: Double? = null,
    var maxLng: Double? = null

) {

    companion object {
        const val IGNORE_LOCATION_AFTER_PAUSED_NUMBER = 5
    }
}