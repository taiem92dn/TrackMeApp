package com.tngdev.trackmeapp.util

import com.tngdev.trackmeapp.util.security.ObscuredSharedPreferences
import com.tngdev.trackmeapp.util.security.SecurityUtil


open class BaseAppPreferencesHelper(sharedPreferences: ObscuredSharedPreferences) {

    companion object {

        val PREF_FIRST_USE_APP : String = SecurityUtil.sha256("PREF_FIRST_USE_APP")

    }

    protected val mSharedPreferences : ObscuredSharedPreferences = sharedPreferences

    private var mFirstUseApp : Boolean = false
        set(value) {
            field = value
            mSharedPreferences.edit().putBoolean(PREF_FIRST_USE_APP, mFirstUseApp).commit()
        }

    init {
        mFirstUseApp = mSharedPreferences.getBoolean(PREF_FIRST_USE_APP, true)
    }

    fun isFirstUseApp() = mFirstUseApp

    fun setKey(key : String, value : Boolean) {
        mSharedPreferences.edit().putBoolean(key, value).commit()
    }

    fun setKey(key : String, value : Long) {
        mSharedPreferences.edit().putLong(key, value).commit()
    }

    fun setKey(key : String, value : String?) {
        mSharedPreferences.edit().putString(key, value).commit()
    }

    fun deleteKey(key: String) {
        mSharedPreferences.edit().remove(key).commit()
    }

}