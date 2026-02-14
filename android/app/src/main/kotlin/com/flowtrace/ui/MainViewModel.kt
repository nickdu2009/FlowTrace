package com.flowtrace.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowtrace.capture.CaptureRuntime
import com.flowtrace.domain.capture.CaptureState
import com.flowtrace.domain.capture.SessionAggregator
import com.flowtrace.domain.session.SessionSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class MainViewModel @Inject constructor(
  private val runtime: CaptureRuntime,
  aggregator: SessionAggregator,
) : ViewModel() {

  val state: StateFlow<CaptureState> = runtime.state

  val sessions: StateFlow<List<SessionSummary>> = aggregator.observeSummaries()
    .map { it }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

  fun start() = runtime.start()
  fun stop() = runtime.stop()
}

