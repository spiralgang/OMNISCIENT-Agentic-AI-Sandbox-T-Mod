package com.example.ai

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AgentState {
    IDLE,
    ANALYZING,
    PLANNING,
    EXECUTING,
    VERIFYING,
    WEB_NAVIGATING,
    ERROR
}

class FSMOrchestrator {
    private val _currentState = MutableStateFlow(AgentState.IDLE)
    val currentState: StateFlow<AgentState> = _currentState.asStateFlow()

    fun transitionTo(newState: AgentState) {
        android.util.Log.d("FSM", "Transition: ${currentState.value} -> $newState")
        _currentState.value = newState
    }

    // Minified communication context for token saving
    fun encodeContext(): String {
        // e.g. 'st:ANA' (ANALYZING)
        return "st:${_currentState.value.name.take(3)}" 
    }
}
