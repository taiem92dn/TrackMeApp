package com.tngdev.trackmeapp.ui.recording

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.viewbinding.BuildConfig
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.tngdev.trackmeapp.AppPreferencesHelper
import com.tngdev.trackmeapp.R
import com.tngdev.trackmeapp.data.model.SessionWithLocations
import com.tngdev.trackmeapp.databinding.FragmentRecordingBinding
import com.tngdev.trackmeapp.ui.MainActivity
import com.tngdev.trackmeapp.util.MapUtils
import com.tngdev.trackmeapp.util.Utils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RecordingFragment : Fragment(), OnMapReadyCallback {

    companion object {
        fun newInstance() = RecordingFragment()

        private val TAG: String = RecordingFragment::class.java.simpleName

        private val DEFAULT_ZOOM = 15

        private const val REQUEST_PERMISSIONS_REQUEST_CODE = 13

        /**
         * Constant used in the location settings dialog.
         */
        private const val REQUEST_CHECK_SETTINGS = 0x1

        /**
         * The desired interval for location updates. Inexact. Updates may be more or less frequent.
         */
        private val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 2000

        /**
         * The fastest rate for active location updates. Exact. Updates will never be more frequent
         * than this value.
         */
        private val FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2
    }

    private lateinit var viewModel: RecordingViewModel

    @Inject
    lateinit var appPreferencesHelper: AppPreferencesHelper

    private var _binding: FragmentRecordingBinding? = null
    private val binding get() = _binding!!

    private var map: GoogleMap? = null


    private lateinit var mFusedLocationProviderClient : FusedLocationProviderClient

    // Stores parameters for requests to the FusedLocationProviderApi.
    private var mLocationRequest: LocationRequest? = null

    private var mIsStartingLocationUpdates: Boolean = false
    /**
     * Stores the types of location services the client is interested in using. Used for checking
     * settings to determine if the device has optimal location settings.
     */
    private var mLocationSettingsRequest: LocationSettingsRequest? = null

    private var mRouteLatLngs : ArrayList<LatLng> = ArrayList()

    private var mRoutePolyLines : ArrayList<Polyline> = ArrayList()

    /**
     * Provides access to the Location Settings API.
     */
    private var mSettingsClient : SettingsClient ?= null

    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
    private var mLastKnownLocation: Location? = null

    private var foregroundOnlyLocationServiceBound = false
    // Provides location updates for while-in-use feature.
    private var foregroundOnlyLocationService: ForegroundOnlyLocationService? = null

    // Listens for location broadcasts from ForegroundOnlyLocationService.
    private var foregroundOnlyBroadcastReceiver = ForegroundOnlyBroadcastReceiver()

    // Monitors connection to the while-in-use service.
    private val foregroundOnlyServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as ForegroundOnlyLocationService.LocalBinder
            foregroundOnlyLocationService = binder.service
            foregroundOnlyLocationServiceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            foregroundOnlyLocationService = null
            foregroundOnlyLocationServiceBound = false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRecordingBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(RecordingViewModel::class.java)

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context!!)
        mSettingsClient = LocationServices.getSettingsClient(context!!)

        createLocationRequest()
        buildLocationSettingsRequest()

        setupUI()

        initMap()
    }

    private fun initMap() {
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment

        mapFragment.getMapAsync(this)
    }

    private fun setupUI() {

        viewModel.currSession.observe(viewLifecycleOwner) {sessionWithLocations ->
            sessionWithLocations?.session?.apply {
                updateUISessionValue(distance, null)
                updateTimeValue(duration)
                if (isPause) {
                    binding.flButtonResume.visibility = View.VISIBLE
                    binding.flButtonStop.visibility = View.VISIBLE
                    binding.flButtonStart.visibility = View.GONE
                }
                else {
                    binding.tvTextOfFABStart.text = getString(R.string.pause_button_text)
                    binding.flButtonStart.visibility = View.VISIBLE
                    binding.flButtonStop.visibility = View.GONE
                    binding.flButtonResume.visibility = View.GONE
                }
            }

            sessionWithLocations?:let {
                binding.tvTextOfFABStart.text = getString(R.string.start_button_text)
                binding.flButtonStart.visibility = View.VISIBLE
                binding.flButtonStop.visibility = View.GONE
                binding.flButtonResume.visibility = View.GONE
            }
        }

        // observe to get latest value for isPausing
        viewModel.isPausing.observe(viewLifecycleOwner) {}

        binding.fabStart.setOnClickListener {
            if (checkPermissions()) {
                when {
                    appPreferencesHelper.isTrackingLocation -> {
                        handlePauseRecording()
                    }
                    else -> {
                        handleStartRecording()
                    }
                }
            }
        }

        binding.fabResume.setOnClickListener {
            if (viewModel.isPausing.value == true)
                handleResumeRecording()
        }

        binding.fabStop.setOnClickListener {
            if (viewModel.isPausing.value == true) {
                handleStopRecording()
            }
        }

        binding.tvHide.setOnClickListener {
            requireActivity().onBackPressed()
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        setupMap()
    }

    private fun setupMap() {

        MapUtils.setLocationEnabledWithPermission(this, map)
        getDeviceLocationUsePlayService()

        val observer = object : Observer<SessionWithLocations?> {
            override fun onChanged(t: SessionWithLocations?) {
                drawRouteTracking(t)
                viewModel.currSession.removeObserver(this)
            }
        }
        viewModel.currSession.observe(this, observer)
//        mMap?.let { MapUtils.zoomToCurrentPosition(it, mLastKnownLocation) }
    }

    private fun setUpRouteTracking(sessionWithLocations: SessionWithLocations) {
        if (mRoutePolyLines.isEmpty()) {
            appPreferencesHelper.currentSessionId ?.let {
                val latLngs = ArrayList<LatLng>()
                sessionWithLocations.apply {
                    if (locations.isNotEmpty()) {
                        for (location in locations) {
                            latLngs.add(LatLng(location.latitude, location.longitude))
                        }

                        renderTrackingRoute(latLngs)
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()

//        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        val serviceIntent = Intent(requireActivity(), ForegroundOnlyLocationService::class.java)
        requireActivity().bindService(serviceIntent, foregroundOnlyServiceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        if (foregroundOnlyLocationServiceBound) {
            requireActivity().unbindService(foregroundOnlyServiceConnection)
            foregroundOnlyLocationServiceBound = false
        }
//        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)

        super.onStop()
    }

    override fun onResume() {
        super.onResume()

        if (checkPermissions()) {
            checkLocationSetting()
        }

        if (appPreferencesHelper.isTrackingLocation && checkPermissions()) {
            startLocationUpdates()
        }
        else if (!checkPermissions()) {
            requestPermissions()
        }


        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
            foregroundOnlyBroadcastReceiver,
            IntentFilter(
                ForegroundOnlyLocationService.ACTION_UPDATE_SESSION_INFO)
        )

        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
            foregroundOnlyBroadcastReceiver,
            IntentFilter(
                ForegroundOnlyLocationService.ACTION_UPDATE_TIMER)
        )
    }

    override fun onPause() {
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(
            foregroundOnlyBroadcastReceiver
        )
        super.onPause()
    }

    private fun drawRouteTracking(sessionWithLocations: SessionWithLocations?) {
        // redraw route tracking
        if (map != null) {
            mRouteLatLngs.clear()
            for (polyLine in mRoutePolyLines)
                polyLine.remove()
            mRoutePolyLines.clear()
            sessionWithLocations?.let { setUpRouteTracking(it) }
        }
    }


    /**
     * Receiver for location broadcasts from [ForegroundOnlyLocationService].
     */
    private inner class ForegroundOnlyBroadcastReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            intent.let {
                when(intent.action) {
                    ForegroundOnlyLocationService.ACTION_UPDATE_SESSION_INFO -> {
                        val speed = intent.getDoubleExtra(ForegroundOnlyLocationService.EXTRA_SPEED, 0.0)
                        val distance = intent.getDoubleExtra(ForegroundOnlyLocationService.EXTRA_DISTANCE, 0.0)
                        val location = intent.getParcelableExtra<Location>(ForegroundOnlyLocationService.EXTRA_LOCATION)
                        updateUISessionValue(distance, speed)
//                        drawRouteTracking(viewModel.currSession.value)
                        location ?.let {
                            addPointToTrackingRoute(LatLng(location.latitude, location.longitude))
                            moveMapToCurrentLocation(it)
                        }
                    }
                    ForegroundOnlyLocationService.ACTION_UPDATE_TIMER -> {
                        val duration = intent.getLongExtra(ForegroundOnlyLocationService.EXTRA_DURATION, 0)
                        updateTimeValue(duration)
                    }
                    else -> {
                    }
                }
            }
        }
    }

    private fun moveMapToCurrentLocation(currentLocation: Location) {
        // check distance between center map and currentLocation, if > 100m, move map
        if (MapUtils.meterDistanceBetweenPoints(
                currentLocation.latitude,
                currentLocation.longitude,
            map?.cameraPosition?.target?.latitude ?: return,
            map?.cameraPosition?.target?.longitude ?: return) > 100
        ) {
            MapUtils.zoomToCurrentPosition(map, currentLocation)
        }
    }

    private fun handleStartRecording() {
        if (checkPermissions()) {
            startLocationUpdates()
        }
    }

    private fun handleResumeRecording() {
        if (checkPermissions()) {
            startLocationUpdates()
        }
    }

    private fun handleStopRecording() {
        // stop ForegroundOnlyLocationService
        context?.stopService(Intent(context, ForegroundOnlyLocationService::class.java))

        if (mRouteLatLngs.size > 0) {
            drawEndLocation(mRouteLatLngs[mRouteLatLngs.size - 1])
        }
//        resetUI()

        showLoading("Saving session")
        viewModel.stopCurrentSession()
        map?.snapshot(GoogleMap.SnapshotReadyCallback {
            viewModel.saveBitmapForCurrentSession(it)
        })

        viewModel.saveBitmapCompleted.observe(viewLifecycleOwner) {completed ->
            if (completed) {
                hideLoading()
                exitRecording()
            }
        }
    }

    private fun handlePauseRecording() {
        stopLocationUpdates()
    }

    @SuppressLint("SetTextI18n")
    fun updateUISessionValue(distance : Double, speed : Double? = null) {
        speed ?. let {
            binding.tvCurrSpeed.text = String.format("%.1f km/h", it)
        }
        binding.tvDistance.text = String.format("%.1f km", distance / 1000)

        Log.d(TAG, "updateUISessionValue")
    }

    fun updateTimeValue(duration : Long) {
        Log.d(TAG, "duration $duration")

        binding.tvTime.text = Utils.convertDurationToString(duration)
    }

    fun resetUI() {
        binding.tvCurrSpeed.text = "0.0 km/h"
        binding.tvDistance.text = "0.0 km"
        binding.tvTime.text ="00:00:00"
    }

    fun exitRecording() {
        requireActivity().finish()
        requireActivity().startActivity(Intent(requireActivity(), MainActivity::class.java))
    }

    /**
     * Prompts the user for permission to use the device location.
     */
    private fun checkPermissions() : Boolean {

        return ContextCompat.checkSelfPermission(requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    }

    private fun requestPermissions() {
        val shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(activity!!, Manifest.permission.ACCESS_FINE_LOCATION)

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.");
            showSnackbar("enable permission use to recording", "OK",  View.OnClickListener {
                // Request permission
                requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_PERMISSIONS_REQUEST_CODE)
            })
        } else {
            Log.i(TAG, "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION ),
                REQUEST_PERMISSIONS_REQUEST_CODE)
        }
    }

    /**
     * Gets the current location of the device, and positions the map's camera.
     */
    private fun getDeviceLocationUsePlayService() {

        if (ContextCompat.checkSelfPermission(context!!,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mFusedLocationProviderClient.lastLocation?.addOnCompleteListener(requireActivity()) {
                if (it.isSuccessful && it.result != null) {
                    mLastKnownLocation = it.result
                    Log.d(TAG, "last know location $mLastKnownLocation")
                    MapUtils.zoomToCurrentPosition(map, mLastKnownLocation)
                } else {
                    val locationCallback = object : LocationCallback() {
                        override fun onLocationResult(locationResult: LocationResult) {
                            locationResult.lastLocation?.let {
                                mLastKnownLocation = it
                                MapUtils.zoomToCurrentPosition(map, mLastKnownLocation)
                            }
                            mFusedLocationProviderClient.removeLocationUpdates(this)
                        }
                    }
                    mFusedLocationProviderClient.requestLocationUpdates(mLocationRequest, locationCallback, Looper.myLooper())
                }
            }
        }

    }

    private fun checkLocationSetting() {
        mSettingsClient?.checkLocationSettings(mLocationSettingsRequest)
            ?.addOnSuccessListener { locationSettingsResponse ->
                Log.i(TAG, "All location settings are satisfied.")
                getDeviceLocationUsePlayService()
            }
            ?.addOnFailureListener{
                val statusCode = (it as ApiException).statusCode
                when (statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                        Log.i(TAG, "Location settings are not satisfied. Attempting to upgrade " + "location settings ")
                        try {
                            // Show the dialog by calling startResolutionForResult(), and check the
                            // result in onActivityResult().
                            val rae = it as ResolvableApiException
                            rae.startResolutionForResult(activity, REQUEST_CHECK_SETTINGS)
                        } catch (e: IntentSender.SendIntentException) {
                            Log.i(TAG, "PendingIntent unable to execute request.")
                        }

                    }
                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                        val errorMessage = "Location settings are inadequate, and cannot be " + "fixed here. Fix in Settings."
                        Log.e(TAG, errorMessage)
                        Toast.makeText(context, R.string.error_setting_location, Toast.LENGTH_LONG).show()
                        appPreferencesHelper.isTrackingLocation = false
                    }
                }
            }
    }

    /**
     * Sets up the location request. Use to get lastLocation for map
     */
    private fun createLocationRequest() {
        mLocationRequest = LocationRequest()

        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        mLocationRequest?.interval = UPDATE_INTERVAL_IN_MILLISECONDS

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest?.fastestInterval = FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS

        mLocationRequest?.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    /**
     * Uses a [com.google.android.gms.location.LocationSettingsRequest.Builder] to build
     * a [com.google.android.gms.location.LocationSettingsRequest] that is used for checking
     * if a device has the needed location settings.
     */
    private fun buildLocationSettingsRequest() {
        val builder = LocationSettingsRequest.Builder()
        mLocationRequest?.let { builder.addLocationRequest(it) }
        mLocationSettingsRequest = builder.build()
    }

    private fun renderTrackingRoute(latLngs : List<LatLng>) {
        val rectLine = PolylineOptions().width(10f).color(Color.RED)
        for (latLng in latLngs) {
            rectLine.add(latLng)
        }
        drawStartLocation(latLngs[0])
        mRouteLatLngs.addAll(latLngs)
        mRoutePolyLines.add(map?.addPolyline(rectLine)!!)

    }

    private fun addPointToTrackingRoute(latLng : LatLng) {
        val rectLine = PolylineOptions().width(10f).color(Color.RED)
        if (mRouteLatLngs.size > 1) {
            if (latLng == mRouteLatLngs[mRouteLatLngs.size - 1]) return
            rectLine.add(mRouteLatLngs[mRouteLatLngs.size - 1])
            rectLine.add(latLng)

            mRoutePolyLines.add(map?.addPolyline(rectLine)!!)
        }
        else if (mRouteLatLngs.isEmpty()) {
            drawStartLocation(latLng)
        }
        mRouteLatLngs.add(latLng)
    }

    private fun drawStartLocation(latLng: LatLng) {
        val marker = MarkerOptions().position(latLng).icon(
            BitmapDescriptorFactory.
        fromBitmap(Utils.bitmapFromDrawable(requireContext(), R.drawable.ic_start)))
        map?.addMarker(marker)
    }

    private fun drawEndLocation(latLng: LatLng) {
        val marker = MarkerOptions().position(latLng).icon(Utils.bitmapDescriptorFromVector(requireContext(), R.drawable.ic_finish))
        map?.addMarker(marker)
    }


    /**
     * Requests location updates from the FusedLocationApi. Note: we don't call this unless location
     * runtime permission has been granted.
     */
    private fun startLocationUpdates() {
        if (appPreferencesHelper.isTrackingLocation) return

        mIsStartingLocationUpdates = true
        // Begin by checking if the device has the necessary location settings.
        mSettingsClient?.checkLocationSettings(mLocationSettingsRequest)
            ?.addOnSuccessListener { locationSettingsResponse ->

                Log.i(TAG, "All location settings are satisfied.")
                if (ContextCompat.checkSelfPermission(context!!,
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                    foregroundOnlyLocationService?.subscribeToLocationUpdates {
                        if (viewModel.isPausing.value == true) {
                            viewModel.resumeCurrentSession()
                        }
                        else {
                            viewModel.startNewSession()
                        }
                    }
            }
            ?.addOnFailureListener{
                val statusCode = (it as ApiException).statusCode
                when (statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                        mIsStartingLocationUpdates = false
                        Log.i(TAG, "Location settings are not satisfied. Attempting to upgrade " + "location settings ")
                        try {
                            // Show the dialog by calling startResolutionForResult(), and check the
                            // result in onActivityResult().
                            val rae = it as ResolvableApiException
                            rae.startResolutionForResult(activity, REQUEST_CHECK_SETTINGS)
                        } catch (e: IntentSender.SendIntentException) {
                            Log.i(TAG, "PendingIntent unable to execute request.")
                        }

                    }
                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                        mIsStartingLocationUpdates = false
                        val errorMessage = "Location settings are inadequate, and cannot be " + "fixed here. Fix in Settings."
                        Log.e(TAG, errorMessage)
                        Toast.makeText(context, R.string.error_setting_location, Toast.LENGTH_LONG).show()
                        appPreferencesHelper.isTrackingLocation = false
                    }
                }
            }


    }


    /**
     * Removes location updates from the FusedLocationApi.
     */
    private fun stopLocationUpdates() {
        if (!appPreferencesHelper.isTrackingLocation) {
            return
        }

        foregroundOnlyLocationService?.unsubscribeToLocationUpdates {
            viewModel.pauseCurrentSession()
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            // Check for the integer request code originally supplied to startResolutionForResult().
            REQUEST_CHECK_SETTINGS -> when (resultCode) {
                Activity.RESULT_OK -> {
                    mIsStartingLocationUpdates = false
                    Log.i(TAG, "User agreed to make required location settings changes.")
                    if (mIsStartingLocationUpdates) {
                        startLocationUpdates()
                    }
                    setupMap()
                }
                Activity.RESULT_CANCELED -> {
                    Log.i(TAG, "User chose not to make required location settings changes.")
                    appPreferencesHelper.isTrackingLocation = false
                    mIsStartingLocationUpdates = false
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.i(TAG, "onRequestPermissionResult");
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            when {
                grantResults.isEmpty() -> {
                    // If user interaction was interrupted, the permission request is cancelled and you
                    // receive empty arrays.
                    Log.i(TAG, "User interaction was cancelled.");
                }
                grantResults[0] == PackageManager.PERMISSION_GRANTED -> {
                    if (appPreferencesHelper.isTrackingLocation) {
                        Log.i(TAG, "Permission granted, updates requested, starting location updates")
                        startLocationUpdates()
                    }
                    setupMap()
                }
                else -> {
                    // Permission denied.

                    // Notify the user via a SnackBar that they have rejected a core permission for the
                    // app, which makes the Activity useless. In a real app, core permissions would
                    // typically be best requested during a welcome-screen flow.

                    // Additionally, it is important to remember that a permission might have been
                    // rejected without asking the user for permission (device policy or "Never ask
                    // again" prompts). Therefore, a user interface affordance is typically implemented
                    // when permissions are denied. Otherwise, your app could appear unresponsive to
                    // touches or interactions which have required permissions.
                    showSnackbar("Permission denied.",
                        "setting", View.OnClickListener{
                            // Build intent that displays the App settings screen.
                            val intent = Intent()
                            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS;
                            val uri = Uri.fromParts("package", BuildConfig.VERSION_NAME, null);
                            intent.data = uri
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                        })
                }
            }
        }
    }


    private fun showSnackbar(mainTextString : String, actionString : String,
                             listener : View.OnClickListener?) {
        activity?.let {
            Snackbar.make(
                requireView(),
                mainTextString,
                Snackbar.LENGTH_LONG)
                .setAction(actionString, listener).show()
        }
    }

    var mPdLoading : ProgressDialog?= null

    fun showLoading(message : String) {
        mPdLoading ?: let {
            mPdLoading = ProgressDialog.show(context, null, message)
        }
    }

    fun hideLoading() {
        mPdLoading?.dismiss()
    }

}