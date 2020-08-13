package com.tngdev.trackmeapp

import com.tngdev.trackmeapp.util.BaseAppPreferencesHelper
import com.tngdev.trackmeapp.util.security.ObscuredSharedPreferences

class AppPreferencesHelper(sharedPreferences: ObscuredSharedPreferences) :
   BaseAppPreferencesHelper(sharedPreferences) {

   companion object {
       const val IS_TRACKING_LOCATION = "IS_TRACKING_LOCATION"
       const val CURRENT_SESSION_ID = "CURRENT_SESSION_ID"

   }

   var isTrackingLocation : Boolean = false
      set(value) {
         field = value
         setKey(IS_TRACKING_LOCATION, value)
      }

   var currentSessionId : String? = null
      set(value) {
         field = value
         value ?.let {
            setKey(CURRENT_SESSION_ID, value)
         }
         ?:let {
            deleteKey(CURRENT_SESSION_ID)
         }
      }

   init {
      isTrackingLocation = mSharedPreferences.getBoolean(IS_TRACKING_LOCATION, false)
      currentSessionId = mSharedPreferences.getString(CURRENT_SESSION_ID, null)
   }
}