package dev.rexe.hotelremote.remote.fragments.general

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.rexe.hotelremote.managers.RequestsManager
import kotlinx.coroutines.launch

class RemoteGeneralViewModel : ViewModel() {
    private val _lightStatus = MutableLiveData<Boolean>()
    val lightStatus: LiveData<Boolean> get() = _lightStatus

    private val _doorLockStatus = MutableLiveData<Boolean>()
    val doorLockStatus: LiveData<Boolean> get() = _doorLockStatus

    private val _dndMode = MutableLiveData<Boolean>()
    val dndMode: LiveData<Boolean> get() = _dndMode

    fun setLightEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _lightStatus.postValue(enabled)
        }
    }

    fun setDoorLockStatus(enabled: Boolean) {
        viewModelScope.launch {
            _doorLockStatus.postValue(enabled)
        }
    }

    fun setDNDMode(enabled: Boolean) {
        viewModelScope.launch {
            _dndMode.postValue(enabled)
        }
    }
}