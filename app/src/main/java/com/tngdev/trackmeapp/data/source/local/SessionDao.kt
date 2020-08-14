package com.tngdev.trackmeapp.data.source.local

import androidx.lifecycle.LiveData
import androidx.room.*
import com.tngdev.trackmeapp.data.model.Location
import com.tngdev.trackmeapp.data.model.Session
import com.tngdev.trackmeapp.data.model.SessionWithLocations

@Dao
interface SessionDao {

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getSessionById(id: String): Session?

    @Query("SELECT * FROM sessions WHERE endTime != 0 ORDER BY startTime DESC")
    fun observeAllHistorySessions(): LiveData<List<Session>>

    @Transaction
    @Query("SELECT * FROM sessions WHERE endTime == 0")
    fun observeCurrentSession(): LiveData<SessionWithLocations?>

    @Update
    suspend fun updateSession(session: Session)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: Session)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: Location)
}