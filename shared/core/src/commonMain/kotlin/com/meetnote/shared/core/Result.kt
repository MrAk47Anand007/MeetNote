package com.meetnote.shared.core

sealed interface MeetNoteResult<out T> {
    data class Success<T>(val value: T) : MeetNoteResult<T>
    data class Failure(val message: String, val cause: Throwable? = null) : MeetNoteResult<Nothing>
}
