package com.skymouse.skymouseclient.data

sealed interface CursorScaleQueryState {
    object Unknown : CursorScaleQueryState
    object Fetching : CursorScaleQueryState
    data class CursorScale(val scale: Int) : CursorScaleQueryState
}
