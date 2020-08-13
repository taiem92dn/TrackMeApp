package com.tngdev.trackmeapp.util

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng


object MapUtils {

    fun checkPermissionLocation(pActivity: Activity) {
        val MY_PERMISSION_ACCESS_COURSE_LOCATION = 11
        if (ActivityCompat.checkSelfPermission(pActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(pActivity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            ActivityCompat.requestPermissions(pActivity, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                    MY_PERMISSION_ACCESS_COURSE_LOCATION)
        }
    }

    fun checkPermissionLocation(pFragment: Fragment) {
        val MY_PERMISSION_ACCESS_COURSE_LOCATION = 11
        if (pFragment.context?.let { ActivityCompat.checkSelfPermission(it, Manifest.permission.ACCESS_FINE_LOCATION) } != PackageManager.PERMISSION_GRANTED
                && pFragment.context?.let { ActivityCompat.checkSelfPermission(it, Manifest.permission.ACCESS_COARSE_LOCATION) } != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            pFragment.requestPermissions(arrayOf<String>(Manifest.permission.ACCESS_COARSE_LOCATION),
                    MY_PERMISSION_ACCESS_COURSE_LOCATION)
        }
    }

    fun setLocationEnabledWithPermission(pActivity: Activity, pGoogleMap: GoogleMap): Boolean {

        if (ActivityCompat.checkSelfPermission(pActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(pActivity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.

            return false
        }

        pGoogleMap.isMyLocationEnabled = true
        return true
    }

    fun setLocationEnabledWithPermission(pFragment: Fragment, pGoogleMap: GoogleMap?): Boolean {

        if (pFragment.activity?.let { ActivityCompat.checkSelfPermission(it, Manifest.permission.ACCESS_FINE_LOCATION) } != PackageManager.PERMISSION_GRANTED
                && pFragment.activity?.let { ActivityCompat.checkSelfPermission(it, Manifest.permission.ACCESS_COARSE_LOCATION) } != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.

            return false
        }

        pGoogleMap?.isMyLocationEnabled = true
        return true
    }

    fun requestLocationPermission(pFragment: Fragment) {}


    fun zoomToCurrentPosition(mMap: GoogleMap, currentLocation: Location?) {
        if (currentLocation == null) {
            Log.d("TAG", "current location null")
            return
        }

        val latLng = LatLng(currentLocation.latitude, currentLocation.longitude)
        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 15f)

        //mMap.addMarker(new MarkerOptions().position(latLng).title("You are here!").icon(BitmapDescriptorFactory.fromResource(R.drawable.pin_ico)));
        mMap.moveCamera(cameraUpdate)
    }

    fun zoomToPosition(mMap: GoogleMap, latLng: LatLng) {
        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 15f)

        mMap.animateCamera(cameraUpdate, object : GoogleMap.CancelableCallback {
            override fun onFinish() {}

            override fun onCancel() {}
        })
    }

    fun meterDistanceBetweenPoints(lat_a: Double, lng_a: Double, lat_b: Double, lng_b: Double): Double {
        val pk = (180f / Math.PI).toDouble()

        val a1 = lat_a / pk
        val a2 = lng_a / pk
        val b1 = lat_b / pk
        val b2 = lng_b / pk

        val t1 = Math.cos(a1.toDouble()) * Math.cos(a2.toDouble()) * Math.cos(b1.toDouble()) * Math.cos(b2.toDouble())
        val t2 = Math.cos(a1.toDouble()) * Math.sin(a2.toDouble()) * Math.cos(b1.toDouble()) * Math.sin(b2.toDouble())
        val t3 = Math.sin(a1.toDouble()) * Math.sin(b1.toDouble())
        val tt = Math.acos(t1 + t2 + t3)

        return if (tt.isNaN())  0.0 else 6366000 * tt
    }
}