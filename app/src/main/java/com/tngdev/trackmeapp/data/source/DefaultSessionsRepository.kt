package com.tngdev.trackmeapp.data.source

import com.tngdev.trackmeapp.data.source.local.SessionDao
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultSessionsRepository @Inject constructor(
    private val sessionDao: SessionDao
){

    val historySessions = sessionDao.observeAllHistorySessions()

}