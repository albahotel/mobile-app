package dev.rexe.hotelremote.remote.fragments.requests

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.rexe.hotelremote.managers.RequestsManager
import kotlinx.coroutines.launch

class RemoteRequestsViewModel : ViewModel() {
    private val _dropdownItems = MutableLiveData<Array<RequestsManager.RequestType>>()
    val dropdownItems: LiveData<Array<RequestsManager.RequestType>> get() = _dropdownItems

    fun setRequestTypes(types: Array<RequestsManager.RequestType>) {
        viewModelScope.launch {
            _dropdownItems.postValue(types)
        }
    }
}