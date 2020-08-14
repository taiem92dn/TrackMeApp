package com.tngdev.trackmeapp.ui.recording

import android.app.Application
import android.graphics.Bitmap
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import com.tngdev.trackmeapp.data.model.Session
import com.tngdev.trackmeapp.data.model.SessionWithLocations
import com.tngdev.trackmeapp.data.source.CurrentSessionRepository
import com.tngdev.trackmeapp.util.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RecordingViewModel @ViewModelInject constructor(
    private val repository: CurrentSessionRepository,
    private val application: Application
) : ViewModel() {

    private val _currSession: LiveData<SessionWithLocations?> = repository.currentSession
    val currSession: LiveData<SessionWithLocations?> = _currSession

    private val _saveBitmapCompleted: MutableLiveData<Boolean> = MutableLiveData(false)
    val saveBitmapCompleted: LiveData<Boolean> = _saveBitmapCompleted

//    private val _isTrackingLocation = MutableLiveData<Boolean>(false)
//    val isTrackingLocation = _isTrackingLocation

    val isPausing: LiveData<Boolean> = _currSession.map { it?.session?.isPause ?: false  }

    fun startNewSession() {
        viewModelScope.launch {
            repository.startNewSession()
        }
    }

    fun stopCurrentSession() {
        viewModelScope.launch {
            _currSession.value?.let {
                repository.stopCurrentSession(it)
            }
        }
    }

    fun updateCurrentSession(thumbnailPath: String) {
        viewModelScope.launch {
            _currSession.value?.let {
                repository.updateCurrentSession(it.session, thumbnailPath)
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

    fun saveBitmapForCurrentSession(bitmap: Bitmap?) {
        viewModelScope.launch {
            if (bitmap != null && currSession.value != null) {

                val filePath = withContext(Dispatchers.Default) {
                    Utils.saveBitmapToFile(_currSession.value!!.session.id, bitmap, application)
                }

                filePath?.let {
                    updateCurrentSession(it)
                }
            }

            _saveBitmapCompleted.value = true
        }

    }
}