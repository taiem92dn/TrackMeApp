package com.tngdev.trackmeapp.di.module

import android.app.Application
import android.content.Context
import com.tngdev.trackmeapp.AppPreferencesHelper
import com.tngdev.trackmeapp.R
import com.tngdev.trackmeapp.util.security.ObscuredSharedPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import javax.inject.Singleton

@Module
@InstallIn(ApplicationComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideObscuredSharedPreferences(app: Application)
            = ObscuredSharedPreferences(app, app.getSharedPreferences(app.getString(R.string.app_name), Context.MODE_PRIVATE))

    @Provides
    @Singleton
    fun provideAppPreferences(sharedPreferences: ObscuredSharedPreferences)
            = AppPreferencesHelper(sharedPreferences)
}