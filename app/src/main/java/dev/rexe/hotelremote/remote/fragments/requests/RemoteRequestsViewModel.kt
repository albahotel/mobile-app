package dev.rexe.hotelremote.remote.fragments.requests

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.rexe.hotelremote.managers.RequestsManager
import kotlinx.coroutines.launch

class RemoteRequestsViewModel : ViewModel() {
    private val _dropdownItems = MutableLiveData<ArrayList<RequestsManager.RequestType>>()
    val dropdownItems: LiveData<ArrayList<RequestsManager.RequestType>> get() = _dropdownItems

    private val _submitEnabled = MutableLiveData<Boolean>()
    val submitEnabled: LiveData<Boolean> get() = _submitEnabled

    private val _requestsListItems = MutableLiveData<ArrayList<RequestsManager.RequestObject>>()
    val requestsListItems: LiveData<ArrayList<RequestsManager.RequestObject>> get() = _requestsListItems

    fun setRequestTypes(types: ArrayList<RequestsManager.RequestType>) {
        viewModelScope.launch {
            _dropdownItems.postValue(types)
        }
    }

    fun setSubmitEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _submitEnabled.postValue(enabled)
        }
    }

    fun setRequests(requests: ArrayList<RequestsManager.RequestObject>) {
        viewModelScope.launch {
            _requestsListItems.postValue(requests)
        }
    }
}