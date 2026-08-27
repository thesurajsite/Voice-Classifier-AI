package com.app.voiceclassifier.ui.adduser

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.voiceclassifier.data.audio.AudioRecorder
import kotlinx.coroutines.launch

private const val SENTENCE_TO_READ = "The quick brown fox jumps over the lazy dog. Please read this sentence clearly and naturally for twenty seconds. Keep your voice steady and speak at a normal pace."

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateUserScreen(
    onBack: () -> Unit,
    viewModel: AddUserViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val recorder = remember { AudioRecorder(context) }

    var userName by remember { mutableStateOf("") }
    var audioFloats by remember { mutableStateOf<FloatArray?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var elapsed by remember { mutableStateOf(0) }
    var hasRecorded by remember { mutableStateOf(false) }

    val isSaving by viewModel.isSaving.collectAsState()
    val saveError by viewModel.saveError.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(context, "Microphone permission required", Toast.LENGTH_SHORT).show()
        }
    }

    // Show error toast
    saveError?.let { msg ->
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        viewModel.clearError()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add User") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = userName,
                onValueChange = { userName = it },
                label = { Text("User Name") },
                placeholder = { Text("Enter name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Please read aloud:",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "\"$SENTENCE_TO_READ\"",
                        style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Recording UI
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isRecording) {
                            CircularProgressIndicator(
                                progress = { elapsed / 20f },
                                modifier = Modifier.size(96.dp),
                                strokeWidth = 6.dp
                            )
                        }
                        FloatingActionButton(
                            onClick = {
                                if (isRecording) return@FloatingActionButton
                                // Check permission
                                if (!recorder.hasPermission()) {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    return@FloatingActionButton
                                }
                                // Start recording
                                isRecording = true
                                elapsed = 0
                                hasRecorded = false
                                audioFloats = null
                                scope.launch {
                                    try {
                                        val result = recorder.record20Seconds { sec ->
                                            elapsed = sec
                                        }
                                        audioFloats = result
                                        hasRecorded = true
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Recording failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isRecording = false
                                        elapsed = 20
                                    }
                                }
                            },
                            modifier = Modifier.size(80.dp),
                            containerColor = when {
                                isRecording -> MaterialTheme.colorScheme.error
                                hasRecorded -> MaterialTheme.colorScheme.secondary
                                else -> MaterialTheme.colorScheme.primary
                            }
                        ) {
                            Icon(
                                imageVector = when {
                                    isRecording -> Icons.Filled.Stop
                                    hasRecorded -> Icons.Filled.Check
                                    else -> Icons.Filled.Mic
                                },
                                contentDescription = if (isRecording) "Recording" else "Record",
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = when {
                            isRecording -> String.format("%02d / 20s recording...", elapsed)
                            hasRecorded -> "Recording complete (20s) - tap to re-record"
                            else -> "Tap to record 20 seconds"
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )

                    if (isRecording) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Reading: \"$SENTENCE_TO_READ\"",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (hasRecorded && !isRecording) {
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                audioFloats = null
                                hasRecorded = false
                                elapsed = 0
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Re-record")
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    val floats = audioFloats
                    if (floats == null) {
                        Toast.makeText(context, "Please record audio first", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    viewModel.saveUser(userName, floats) {
                        Toast.makeText(context, "User saved", Toast.LENGTH_SHORT).show()
                        onBack()
                    }
                },
                enabled = userName.isNotBlank() && audioFloats != null && !isRecording && !isSaving,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.size(8.dp))
                    Text("Saving...")
                } else {
                    Text("Save User")
                }
            }

            if (isSaving) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Generating embedding with Titanet...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
