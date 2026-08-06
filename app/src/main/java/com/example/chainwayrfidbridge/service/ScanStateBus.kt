package com.example.chainwayrfidbridge.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Lets the floating overlay bubble (a Service, no ViewModel of its own) reflect scan state
 * without duplicating any reader/network logic — ScanViewModel is still the only thing that
 * touches the reader or sends tags, this is just a read-only mirror of its outcome.
 */
object ScanStateBus {
    enum class BubbleState { IDLE, SCANNING, SUCCESS, ERROR }
    data class BubbleData(val state: BubbleState = BubbleState.IDLE, val tagCount: Int = 0)

    private val _data = MutableStateFlow(BubbleData())
    val data: StateFlow<BubbleData> = _data.asStateFlow()

    fun update(state: BubbleState, tagCount: Int) {
        _data.value = BubbleData(state, tagCount)
    }
}
