package com.tngdev.trackmeapp.ui.history

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.tngdev.trackmeapp.data.source.DefaultSessionsRepository


class HistoryViewModel @ViewModelInject constructor(
    private val repository: DefaultSessionsRepository
) : ViewModel() {

    val historySessions = repository.getHistorySessions(5).cachedIn(viewModelScope)


}