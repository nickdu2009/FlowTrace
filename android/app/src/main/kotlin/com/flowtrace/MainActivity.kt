package com.flowtrace

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dagger.hilt.android.AndroidEntryPoint
import com.flowtrace.ui.theme.FlowTraceTheme
import com.flowtrace.ui.MainViewModel
import com.flowtrace.domain.capture.CaptureState

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
  private val vm: MainViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      FlowTraceTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          Scaffold { innerPadding ->
            Box(
              modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
              contentAlignment = Alignment.Center
            ) {
              MainScreen(vm = vm)
            }
          }
        }
      }
    }
  }
}

@Composable
fun MainScreen(vm: MainViewModel, modifier: Modifier = Modifier) {
  val state by vm.state.collectAsState()
  val sessions by vm.sessions.collectAsState()

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(16.dp)
  ) {
    Text(text = "Capture: $state", style = MaterialTheme.typography.titleMedium)
    Row(modifier = Modifier.padding(top = 12.dp)) {
      Button(
        onClick = { vm.start() },
        enabled = state == CaptureState.IDLE || state == CaptureState.ERROR,
      ) { Text("Start") }
      Box(modifier = Modifier.padding(horizontal = 8.dp))
      Button(
        onClick = { vm.stop() },
        enabled = state == CaptureState.RUNNING || state == CaptureState.STARTING,
      ) { Text("Stop") }
    }

    Text(
      text = "Sessions: ${sessions.size}",
      modifier = Modifier.padding(top = 16.dp),
      style = MaterialTheme.typography.titleSmall
    )

    LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
      items(sessions) { s ->
        Text(
          text = "${s.method ?: "-"} ${s.host ?: "-"} ${s.path ?: ""}  (${s.statusCode ?: "-"})",
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.padding(vertical = 6.dp)
        )
      }
    }
  }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
  FlowTraceTheme {
    Text("Preview")
  }
}

