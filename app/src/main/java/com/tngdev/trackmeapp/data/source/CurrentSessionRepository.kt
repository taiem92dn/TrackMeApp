package com.tngdev.trackmeapp.data.source

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.tngdev.trackmeapp.AppPreferencesHelper
import com.tngdev.trackmeapp.data.model.Location
import com.tngdev.trackmeapp.data.model.Session
import com.tngdev.trackmeapp.data.model.SessionWithLocations
import com.tngdev.trackmeapp.data.source.local.SessionDao
import com.tngdev.trackmeapp.util.MapUtils
import com.tngdev.trackmeapp.util.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CurrentSessionRepository @Inject constructor(
    private val sessionDao: SessionDao,
    private val appPreferencesHelper: AppPreferencesHelper
) {

    val currentSession: LiveData<SessionWithLocations?> = sessionDao.observeCurrentSession()

    suspend fun startNewSession() {
        val session = Session()
        sessionDao.insertSession(session)
        appPreferencesHelper.currentSessionId = session.id
    }

    suspend fun stopCurrentSession(sessionWithLocations: SessionWithLocations) {
        appPreferencesHelper.currentSessionId = null

        // update stop session to db
        withContext(Dispatchers.Default) {
            sessionWithLocations.apply {

                var avgSpeed = 0.0
                if (locations.isNotEmpty()) {
                    for (location in locations) {
                        avgSpeed += location.speed
                    }
                    avgSpeed /= locations.size
                }

                session.apply {
                    endTime = System.currentTimeMillis()
                    isPause = false
                    this.avgSpeed = avgSpeed
                }

                sessionDao.updateSession(session)
            }

        }
    }

    suspend fun resumeCurrentSession(session: Session) {
        appPreferencesHelper.currentSessionId = session.id
        // update to db
        session.apply {
            isPause = false
            isResumingAfterPause = true
            ignoreLocationsAfterPauseCount = Session.IGNORE_LOCATION_AFTER_PAUSED_NUMBER

            sessionDao.updateSession(this)
        }
    }

    suspend fun pauseCurrentSession(session: Session) {
        // update to db
        session.apply {
            isPause = true
            sessionDao.updateSession(this)
        }
    }

    suspend fun increaseDuration(session: Session) {
        session.duration += 1
        sessionDao.updateSession(session)
    }

    suspend fun insertLocation(session: Session, location: Location) {
        location.sessionId = session.id
        sessionDao.insertLocation(location)

        currentSession.value?.apply {

            if (session.isResumingAfterPause && session.ignoreLocationsAfterPauseCount == 0) {
                session.isResumingAfterPause = false
            }
            else if (session.ignoreLocationsAfterPauseCount > 0) {
                session.ignoreLocationsAfterPauseCount--
            }

            if (locations.size > 1) {
                val from = locations[locations.size-2]
                val to = locations[locations.size-1]
                val distance = MapUtils.meterDistanceBetweenPoints(from.latitude, from.longitude, to.latitude, to.longitude)
                Log.d("TAG", "insertLocation: $distance  " +
                        "${session.isResumingAfterPause} ${Utils.dateToStringWithSecond(location.time)}")
            }
            if (locations.size > 1 && !session.isResumingAfterPause) {
                val from = locations[locations.size-2]
                val to = locations[locations.size-1]
                val distance = MapUtils.meterDistanceBetweenPoints(from.latitude, from.longitude, to.latitude, to.longitude)
                session.distance += distance
                session.avgSpeed = session.distance / session.duration
            }

            sessionDao.updateSession(session)
        }
    }

    suspend fun updateCurrentSession(session: Session, thumbnailPath: String) {
        session.thumbnailPath = thumbnailPath
        sessionDao.updateSession(session)
    }
}