package com.tngdev.trackmeapp.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class SessionWithLocations(
   @Embedded val session: Session,
   @Relation(
       parentColumn = "id",
       entityColumn = "sessionId"
   )
   val locations: List<Location>
) {
}