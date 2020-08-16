package com.tngdev.trackmeapp.data.source

import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.tngdev.trackmeapp.data.source.local.SessionDao
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultSessionsRepository @Inject constructor(
    private val sessionDao: SessionDao
){

    fun getHistorySessions(pageSize: Int) = Pager(
        PagingConfig(pageSize)
    ) {
        sessionDao.observeAllHistorySessions()
    }.flow

}