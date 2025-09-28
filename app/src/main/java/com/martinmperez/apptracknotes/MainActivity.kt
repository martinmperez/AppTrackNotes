// MainActivity.kt
package com.martinmperez.apptracknotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.martinmperez.apptracknotes.ui.NoteViewModel

class MainActivity : ComponentActivity() {
    private val vm: NoteViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                NotesScreen(vm)
            }
        }
    }
}

@Composable
fun NotesScreen(vm: NoteViewModel) {
    val notes = vm.notes.collectAsState().value
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            Row(Modifier.padding(16.dp)) {
                Text("AppTrack Notes", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.weight(1f))
                Button(onClick = { vm.syncNow() }) { Text("Sincronizar") }
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Título") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("Contenido") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Button(onClick = {
                if (title.isNotBlank() || content.isNotBlank()) {
                    vm.add(title.trim(), content.trim())
                    title = ""; content = ""
                }
            }) { Text("Agregar") }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            notes.forEach { n ->
                ListItem(
                    headlineContent = { Text(n.title.ifBlank { "(Sin título)" }) },
                    supportingContent = { Text(n.content) },
                    trailingContent = { TextButton(onClick = { vm.delete(n.id) }) { Text("Borrar") } },
                    modifier = Modifier.fillMaxWidth()
                )
                HorizontalDivider()
            }
        }
    }
}
