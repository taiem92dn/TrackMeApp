/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tngdev.trackmeapp.ui.recording

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.location.Location
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.observe
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.*
import com.tngdev.trackmeapp.AppPreferencesHelper
import com.tngdev.trackmeapp.R
import com.tngdev.trackmeapp.data.source.CurrentSessionRepository
import com.tngdev.trackmeapp.ui.MainActivity
import com.tngdev.trackmeapp.util.Utils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Service tracks location when requested and updates Activity via binding. If Activity is
 * stopped/unbinds and tracking is enabled, the service promotes itself to a foreground service to
 * insure location updates aren't interrupted.
 *
 * For apps running in the background on O+ devices, location is computed much less than previous
 * versions. Please reference documentation for details.
 */
@AndroidEntryPoint
class ForegroundOnlyLocationService : LifecycleService() {
    /*
     * Checks whether the bound activity has really gone away (foreground service with notification
     * created) or simply orientation change (no-op).
     */
    private var configurationChange = false

    private var serviceRunningInForeground = false

    private val localBinder = LocalBinder()

    private lateinit var notificationManager: NotificationManager

    // FusedLocationProviderClient - Main class for receiving location updates.
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    // LocationRequest - Requirements for the location updates, i.e., how often you should receive
    // updates, the priority, etc.
    private lateinit var locationRequest: LocationRequest

    // LocationCallback - Called when FusedLocationProviderClient has a new Location.
    private lateinit var locationCallback: LocationCallback

    private val mHandler = Handler()

    private var mCurrentDuration : Long = 0

    private val mainScope: CoroutineScope = MainScope()

    @Inject
    lateinit var appPreferencesHelper: AppPreferencesHelper

    @Inject
    lateinit var repository: CurrentSessionRepository

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate()")

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        locationRequest = LocationRequest().apply {
            // Sets the desired interval for active location updates. This interval is inexact. You
            // may not receive updates at all if no location sources are available, or you may
            // receive them less frequently than requested. You may also receive updates more
            // frequently than requested if other applications are requesting location at a more
            // frequent interval.
            //
            // IMPORTANT NOTE: Apps running on Android 8.0 and higher devices (regardless of
            // targetSdkVersion) may receive updates less frequently than this interval when the app
            // is no longer in the foreground.
            interval = UPDATE_INTERVAL_IN_MILLISECONDS

            // Sets the fastest rate for active location updates. This interval is exact, and your
            // application will never receive updates more frequently than this value.
            fastestInterval = FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS

            // Sets the maximum time when batched location updates are delivered. Updates may be
            // delivered sooner than this interval.
            maxWaitTime = TimeUnit.MINUTES.toMillis(2)

            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                super.onLocationResult(locationResult)

                if (locationResult?.lastLocation != null) {

                    recordLocation(locationResult.lastLocation)

                    // Updates notification content if this service is running as a foreground
                    // service.
                    updateNotification()
                } else {
                    Log.d(TAG, "Location missing in callback.")
                }
            }
        }

        // observe to get latest value
        repository.currentSession.observe(this) {

        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.d(TAG, "onStartCommand()")

        // NOTE: If this method is called due to a configuration change in MainActivity,
        // we do nothing.
        if (!configurationChange && appPreferencesHelper.isTrackingLocation && !serviceRunningInForeground) {
            Log.d(TAG, "Start foreground service")
            val notification = generateNotification(mCurrentDuration)
            startForeground(NOTIFICATION_ID, notification)
            serviceRunningInForeground = true
        }

        val cancelLocationTrackingFromNotification =
            intent?.getBooleanExtra(EXTRA_CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION, false)
                ?: false

        if (cancelLocationTrackingFromNotification) {
            unsubscribeToLocationUpdates() {}
            stopSelf()
        }

        // Tells the system not to recreate the service after it's been killed.
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        Log.d(TAG, "onBind()")

        // MainActivity (client) comes into foreground and binds to service, so the service can
        // become a background services.
//        stopForeground(true)
//        serviceRunningInForeground = false
        configurationChange = false
        return localBinder
    }

    override fun onRebind(intent: Intent) {
        Log.d(TAG, "onRebind()")

        // MainActivity (client) returns to the foreground and rebinds to service, so the service
        // can become a background services.
//        stopForeground(true)
//        serviceRunningInForeground = false
        configurationChange = false
        super.onRebind(intent)
    }

    override fun onUnbind(intent: Intent): Boolean {
        Log.d(TAG, "onUnbind()")

        // Ensures onRebind() is called if RecordingActivity (client) rebinds.
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy()")
        stopForeground(true)
        serviceRunningInForeground = false
        stopTimer()
        mainScope.cancel()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        configurationChange = true
    }

    fun subscribeToLocationUpdates(complete: () -> Unit) {
        Log.d(TAG, "subscribeToLocationUpdates()")

        try {
            fusedLocationProviderClient.requestLocationUpdates(
                locationRequest, locationCallback, Looper.myLooper())?.addOnCompleteListener {
                appPreferencesHelper.isTrackingLocation = true

                // Binding to this service doesn't actually trigger onStartCommand(). That is needed to
                // ensure this Service can be promoted to a foreground service, i.e., the service needs to
                // be officially started (which we do here).
                startService(Intent(applicationContext, ForegroundOnlyLocationService::class.java))

                runTimer()

                complete()
            }
        } catch (unlikely: SecurityException) {
            Log.e(TAG, "Lost location permissions. Couldn't remove updates. $unlikely")
        }
    }

    fun unsubscribeToLocationUpdates(complete: () -> Unit) {
        Log.d(TAG, "unsubscribeToLocationUpdates()")

        try {
            val removeTask = fusedLocationProviderClient.removeLocationUpdates(locationCallback)
            removeTask.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    stopTimer()
                    complete()
                    Log.d(TAG, "Location Callback removed.")
                } else {
                    Log.d(TAG, "Failed to remove Location Callback.")
                }
            }

            appPreferencesHelper.isTrackingLocation = false

        } catch (unlikely: SecurityException) {
//            SharedPreferenceUtil.saveLocationTrackingPref(this, true)
            Log.e(TAG, "Lost location permissions. Couldn't remove updates. $unlikely")
        }
    }

    private fun runTimer() {
        mHandler.postDelayed({
            mainScope.launch {
                updateData()
                runTimer()
            }
        }, 1000)
    }

    private fun stopTimer() {
        // remove all callback
        mHandler.removeCallbacksAndMessages(null)
    }


    private fun updateData() {
        mainScope.launch {
            repository.currentSession.value?.apply {
                repository.increaseDuration(session)
                mCurrentDuration = session.duration
                sendBroadcastUpdate()
                updateNotification()
                Log.d(TAG, "session duration in service ${session.duration}")
            }
        }
    }

    private fun sendBroadcastUpdate() {
        val intent = Intent()
        intent.action = ACTION_UPDATE_TIMER
        intent.putExtra(EXTRA_DURATION, mCurrentDuration)
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
    }

    private fun updateNotification() {
        if (serviceRunningInForeground) {
            notificationManager.notify(
                NOTIFICATION_ID,
                generateNotification(mCurrentDuration))
        }
    }

    /*
     * Generates a BIG_TEXT_STYLE Notification that represent latest location.
     */
    private fun generateNotification(duration : Long): Notification {
        Log.d(TAG, "generateNotification()")

        // Main steps for building a BIG_TEXT_STYLE notification:
        //      0. Get data
        //      1. Create Notification Channel for O+
        //      2. Build the BIG_TEXT_STYLE
        //      3. Set up Intent / Pending Intent for notification
        //      4. Build and issue the notification

        // 0. Get data
        val mainNotificationText = Utils.convertDurationToString(duration)
        val titleText = getString(R.string.app_name)

        // 1. Create Notification Channel for O+ and beyond devices (26+).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID, titleText, NotificationManager.IMPORTANCE_DEFAULT)

            // Adds NotificationChannel to system. Attempting to create an
            // existing notification channel with its original values performs
            // no operation, so it's safe to perform the below sequence.
            notificationManager.createNotificationChannel(notificationChannel)
        }

        // 2. Build the BIG_TEXT_STYLE.
        val bigTextStyle = NotificationCompat.BigTextStyle()
            .bigText(mainNotificationText)
//            .setBigContentTitle(titleText)

        // 3. Set up main Intent/Pending Intents for notification.
        val launchActivityIntent = Intent(this, MainActivity::class.java)
        launchActivityIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        launchActivityIntent.action = Intent.ACTION_MAIN
        launchActivityIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        launchActivityIntent.putExtra(EXTRA_FROM_NOTIFICATION, true)

        val cancelIntent = Intent(this, ForegroundOnlyLocationService::class.java)
        cancelIntent.putExtra(EXTRA_CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION, true)

        val servicePendingIntent = PendingIntent.getService(
            this, 0, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val activityPendingIntent = PendingIntent.getActivity(
            this, 0, launchActivityIntent, 0)
//        val activityPendingIntent = TaskStackBuilder.create(this).run {
//            addNextIntentWithParentStack(launchActivityIntent)
//            getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
//        }

        // 4. Build and issue the notification.
        // Notification Channel Id is ignored for Android pre O (26).
        val notificationCompatBuilder =
            NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)

        return notificationCompatBuilder
            .setStyle(bigTextStyle)
//            .setContentTitle(titleText)
            .setContentText(mainNotificationText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setOngoing(true)
            .setNotificationSilent()
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(activityPendingIntent)
//            .addAction(
//                R.drawable.ic_launch, getString(R.string.launch_activity),
//                activityPendingIntent
//            )
//            .addAction(
//                R.drawable.ic_cancel,
//                getString(R.string.stop_button_text),
//                servicePendingIntent
//            )
            .build()
    }

    private fun recordLocation(location : Location) {
        mainScope.launch {
            var distance = 0.0
            repository.currentSession.value?.apply {
                val locationEntity = com.tngdev.trackmeapp.data.model.Location(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    time = location.time,
                    speed = location.speed,
                    sessionId = session.id
                )

                repository.insertLocation(session, locationEntity)
                distance = session.distance
            }

            updateUISessionInfo( distance, location.speed.toDouble(), location)
        }
    }

    private fun updateUISessionInfo(distance : Double, speed : Double, location: Location) {
        val intent = Intent()
        intent.action = ACTION_UPDATE_SESSION_INFO
        intent.putExtra(EXTRA_DISTANCE, distance) // to km
        intent.putExtra(EXTRA_SPEED, Utils.convertMpsToKmh(speed)) // to km/h
        intent.putExtra(EXTRA_LOCATION, location)
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
    }

    /**
     * Class used for the client Binder.  Since this service runs in the same process as its
     * clients, we don't need to deal with IPC.
     */
    inner class LocalBinder : Binder() {
        internal val service: ForegroundOnlyLocationService
            get() = this@ForegroundOnlyLocationService
    }

    companion object {
        private val TAG = ForegroundOnlyLocationService::class.java.simpleName


        /**
         * The desired interval for location updates. Inexact. Updates may be more or less frequent.
         */
        private val UPDATE_INTERVAL_IN_MILLISECONDS: Long = TimeUnit.SECONDS.toMillis(5)

        /**
         * The fastest rate for active location updates. Exact. Updates will never be more frequent
         * than this value.
         */
        private val FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2

        private val PACKAGE_NAME = ForegroundOnlyLocationService::class.java.`package`?.name

        internal val ACTION_STOP_SERVICE = "$PACKAGE_NAME.action.ACTION_STOP_SERVICE"
        internal val ACTION_UPDATE_TIMER = "$PACKAGE_NAME.action.ACTION_UPDATE_TIMER"
        internal val EXTRA_DURATION = "$PACKAGE_NAME.extra.DURATION"
        val EXTRA_FROM_NOTIFICATION = "$PACKAGE_NAME.extra.FROM_NOTIFICATION"

        internal val ACTION_UPDATE_SESSION_INFO = "$PACKAGE_NAME.UPDATE_SESSION_INFO"
        internal val EXTRA_DISTANCE = "${PACKAGE_NAME}.extra.DISTANCE"
        internal val EXTRA_SPEED = "${PACKAGE_NAME}.extra.SPEED"
        internal val EXTRA_LOCATION = "${PACKAGE_NAME}.extra.LOCATION"

        internal val ACTION_FOREGROUND_ONLY_LOCATION_BROADCAST =
            "$PACKAGE_NAME.action.FOREGROUND_ONLY_LOCATION_BROADCAST"

        private val EXTRA_CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION =
            "$PACKAGE_NAME.extra.CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION"

        private const val NOTIFICATION_ID = 12345678

        private const val NOTIFICATION_CHANNEL_ID = "workout_session_channel_01"
    }
}