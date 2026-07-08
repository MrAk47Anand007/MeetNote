package com.meetnote.android.capture

sealed interface PlaybackCapturePermissionState {
    data object NotRequested : PlaybackCapturePermissionState
    data object Requesting : PlaybackCapturePermissionState
    data object Granted : PlaybackCapturePermissionState
    data object Denied : PlaybackCapturePermissionState
    data object Unsupported : PlaybackCapturePermissionState
}
