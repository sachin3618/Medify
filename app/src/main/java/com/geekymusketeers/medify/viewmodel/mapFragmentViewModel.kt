package com.geekymusketeers.medify.viewmodel

import android.location.Location
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class mapFragmentViewModel: ViewModel() {
    val mLocation: MutableLiveData<Location> by lazy {
        MutableLiveData<Location>()
    }

}