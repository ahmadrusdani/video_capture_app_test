package com.amd.simplevideocapturetest

import android.Manifest
import android.os.Build
import androidx.annotation.ColorRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor() : ViewModel() {

    val cameraPermissions = when {
        Build.VERSION.SDK_INT == Build.VERSION_CODES.TIRAMISU -> {
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
            )
        }
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
            )
        }
        Build.VERSION.SDK_INT == Build.VERSION_CODES.Q -> {
            arrayOf(
                Manifest.permission.ACCESS_MEDIA_LOCATION,
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
        else -> {
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }

    private val _isGranted = MutableStateFlow(false)
    private val _tintFilter = MutableStateFlow(0)
    val isGranted = _isGranted.asStateFlow()
    val tintFilter = _tintFilter.asStateFlow()

    fun updateStatusPermission(param: Boolean) = viewModelScope.launch {
        _isGranted.emit(param)
    }

    fun updateTintFilter(@ColorRes filter: Int) = viewModelScope.launch {
        if (filter == _tintFilter.value) {
            clearTintFilter()
            return@launch
        }
        _tintFilter.emit(filter)
    }

    fun clearTintFilter() = viewModelScope.launch {
        _tintFilter.emit(0)
    }


}