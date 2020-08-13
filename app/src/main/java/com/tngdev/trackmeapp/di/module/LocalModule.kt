package com.tngdev.trackmeapp.di.module

import android.app.Application
import androidx.room.Room
import com.tngdev.trackmeapp.data.source.local.AppDatabase
import com.tngdev.trackmeapp.data.source.local.SessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import javax.inject.Singleton

@Module
@InstallIn(ApplicationComponent::class)
object LocalModule {

    @Provides
    @Singleton
    fun provideAppDB(app: Application): AppDatabase {
        return Room.databaseBuilder(app, AppDatabase::class.java, "TrackMeDB").build()
    }

    @Provides
    @Singleton
    fun provideSessionDao(appDatabase: AppDatabase): SessionDao {
        return appDatabase.sessionDao()
    }
}