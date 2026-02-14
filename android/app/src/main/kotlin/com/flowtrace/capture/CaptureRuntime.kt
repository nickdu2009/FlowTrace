package com.flowtrace.capture

import com.flowtrace.domain.capture.CaptureConfig
import com.flowtrace.domain.capture.CaptureEngine
import com.flowtrace.domain.capture.CaptureEvent
import com.flowtrace.domain.capture.CaptureState
import com.flowtrace.domain.capture.SessionAggregator
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * CaptureRuntime wires together:
 * - CaptureEngine events
 * - bounded queue (backpressure)
 * - batch drain -> SessionAggregator
 *
 * MVP-0: in-memory aggregator only.
 */
@Singleton
class CaptureRuntime @Inject constructor(
  private val engine: CaptureEngine,
  private val aggregator: SessionAggregator,
) {

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  private val queue = Channel<CaptureEvent>(
    capacity = 2048,
    onBufferOverflow = BufferOverflow.DROP_OLDEST,
  )

  private val _state = MutableStateFlow(CaptureState.IDLE)
  val state: StateFlow<CaptureState> = _state.asStateFlow()

  private var started = false

  fun start() {
    if (started) return
    started = true

    _state.value = CaptureState.STARTING

    scope.launch {
      // Start engine
      engine.start(CaptureConfig())
        .onFailure { e ->
          Timber.e(e, "Engine start failed")
          _state.value = CaptureState.ERROR
        }

      // Mirror state
      launch {
        engine.state.collect { _state.value = it }
      }

      // Producer: engine events -> bounded queue
      launch {
        engine.events.collect { ev ->
          val result = queue.trySend(ev)
          if (result.isFailure) {
            // DROP_OLDEST should avoid failure in most cases, but still guard.
            Timber.w("Queue overflow; dropping event %s", ev::class.simpleName)
          }
        }
      }

      // Consumer: drain queue in batches
      launch {
        val batch = ArrayList<CaptureEvent>(256)
        while (true) {
          // block for first element
          val first = queue.receive()
          batch.add(first)

          // try drain quickly
          while (batch.size < 256) {
            val next = queue.tryReceive().getOrNull() ?: break
            batch.add(next)
          }

          // merge
          runCatching { aggregator.ingest(batch) }
            .onFailure { Timber.e(it, "Aggregator ingest failed") }

          batch.clear()

          // small yield to keep CPU reasonable
          delay(10)
        }
      }
    }
  }

  fun stop() {
    if (!started) return
    started = false
    scope.launch {
      withContext(Dispatchers.Default) {
        engine.stop()
        aggregator.clear()
        _state.value = CaptureState.IDLE
      }
    }
  }
}

