package com.tngdev.trackmeapp.ui.recording

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import com.tngdev.trackmeapp.data.model.Session
import com.tngdev.trackmeapp.data.model.SessionWithLocations
import com.tngdev.trackmeapp.data.source.CurrentSessionRepository
import kotlinx.coroutines.launch

class RecordingViewModel @ViewModelInject constructor(
    private val repository: CurrentSessionRepository
) : ViewModel() {

    private val _currSession: LiveData<SessionWithLocations?> = repository.currentSession
    val currSession: LiveData<SessionWithLocations?> = _currSession

//    private val _isTrackingLocation = MutableLiveData<Boolean>(false)
//    val isTrackingLocation = _isTrackingLocation

    val isPausing: LiveData<Boolean> = _currSession.map { it?.session?.isPause ?: false  }

    fun startNewSession() {
        viewModelScope.launch {
            repository.startNewSession()
        }
    }

    fun stopCurrentSession(thumbnailPath: String) {
        viewModelScope.launch {
            _currSession.value?.let {
                repository.stopCurrentSession(it, thumbnailPath)
            }
        }
    }

    fun resumeCurrentSession() {
        viewModelScope.launch {
            _currSession.value?.let {
                repository.resumeCurrentSession(it.session)
            }
        }
    }

    fun pauseCurrentSession() {
        viewModelScope.launch {
            _currSession.value?.let {
                repository.pauseCurrentSession(it.session)
            }
        }
    }
}