package com.tngdev.trackmeapp.data.model

import androidx.room.PrimaryKey
import java.util.*

data class Session(
    @PrimaryKey var id:String = UUID.randomUUID().toString(),
    var name: String = "",
    var distance: Double = 0.0,  /* meter */
    var avgSpeed: Double = 0.0,  /* meter / second */
    var startTime: Long = 0,
    var endTime: Long = 0,
    var duration: Long = 0, // second
    var isPause: Boolean = false,
    var isResumingAfterPause: Boolean = false,
    var thumbnailPath: String = ""

) {
}