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

    val currSession: LiveData<SessionWithLocations?> = repository.currentSession

    private val _saveBitmapCompleted: MutableLiveData<Boolean> = MutableLiveData(false)
    val saveBitmapCompleted: LiveData<Boolean> = _saveBitmapCompleted

    /**
     * Use to save thumbnail image to current session after stopping
     */
    private var savingSession: Session? = null

    val isPausing: LiveData<Boolean> = currSession.map { it?.session?.isPause ?: false  }

    fun startNewSession() {
        viewModelScope.launch {
            repository.startNewSession()
        }
    }

    fun stopCurrentSession() {
        viewModelScope.launch {
            currSession.value?.let {
                repository.stopCurrentSession(it)
                savingSession = it.session
            }
        }
    }

    fun updateCurrentSession(session: Session?, thumbnailPath: String) {
        viewModelScope.launch {
            session?.let {
                repository.updateCurrentSession(it, thumbnailPath)
            }
        }
    }

    fun resumeCurrentSession() {
        viewModelScope.launch {
            currSession.value?.let {
                repository.resumeCurrentSession(it.session)
            }
        }
    }

    fun pauseCurrentSession() {
        viewModelScope.launch {
            currSession.value?.let {
                repository.pauseCurrentSession(it.session)
            }
        }
    }

    fun saveBitmapForCurrentSession(bitmap: Bitmap?) {
        viewModelScope.launch {
            if (bitmap != null && savingSession != null) {

                val sessionId = savingSession?.id
                val filePath = withContext(Dispatchers.Default) {
                    sessionId?.let {
                        Utils.saveBitmapToFile(it, bitmap, application)
                    }
                }

                filePath?.let {
                    updateCurrentSession(savingSession, it)
                }
            }

            _saveBitmapCompleted.value = true
        }

    }
}