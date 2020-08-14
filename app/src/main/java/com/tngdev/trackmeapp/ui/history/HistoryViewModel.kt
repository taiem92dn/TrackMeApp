package com.tngdev.trackmeapp.ui.history

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.ViewModel
import com.tngdev.trackmeapp.data.source.DefaultSessionsRepository


class HistoryViewModel @ViewModelInject constructor(
    private val repository: DefaultSessionsRepository
) : ViewModel() {

    val historySessions = repository.historySessions


}