// MainActivity.kt
package com.martinmperez.apptracknotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.martinmperez.apptracknotes.ui.NoteViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff

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
    val auth = vm.authState.collectAsState().value
    val loggedIn = auth.isLoggedIn

    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            Row(Modifier.padding(16.dp)) {
                Text("AppTrack Notes", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.weight(1f))
                Button(onClick = { vm.syncNow() }, enabled = loggedIn) { Text("Sincronizar") }
            }
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AuthPanel(vm)
            HorizontalDivider()

            // 🔒 Deshabilitar alta si no hay login
            OutlinedTextField(
                value = title, onValueChange = { title = it },
                label = { Text("Título") },
                modifier = Modifier.fillMaxWidth(),
                enabled = loggedIn
            )
            OutlinedTextField(
                value = content, onValueChange = { content = it },
                label = { Text("Contenido") },
                modifier = Modifier.fillMaxWidth(),
                enabled = loggedIn
            )
            Button(
                onClick = {
                    if (title.isNotBlank() || content.isNotBlank()) {
                        vm.add(title.trim(), content.trim())
                        title = ""; content = ""
                    }
                },
                enabled = loggedIn
            ) { Text("Agregar") }

            if (!loggedIn) {
                Text(
                    "Iniciá sesión para crear notas.",
                    color = MaterialTheme.colorScheme.primary
                )
            }

            HorizontalDivider()

            notes.forEach { n ->
                ListItem(
                    headlineContent = { Text(n.title.ifBlank { "(Sin título)" }) },
                    supportingContent = { Text(n.content) },
                    trailingContent = {
                        TextButton(onClick = { vm.delete(n.id) }, enabled = loggedIn) {
                            Text("Borrar")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun AuthPanel(vm: NoteViewModel) {
    val state = vm.authState.collectAsState().value

    if (!state.isLoggedIn) {
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var showPassword by remember { mutableStateOf(false) }

        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Autenticación", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                // 👇 enmascara el texto
                visualTransformation = if (showPassword) VisualTransformation.None
                else PasswordVisualTransformation(),
                // 👇 teclado de contraseña (sin autocorrección)
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            imageVector = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (showPassword) "Ocultar contraseña" else "Mostrar contraseña"
                        )
                    }
                }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { vm.signUp(email.trim(), password) },
                    enabled = !state.loading
                ) { Text("Registrarse") }
                Button(
                    onClick = { vm.signIn(email.trim(), password) },
                    enabled = !state.loading
                ) { Text("Ingresar") }
            }
            if (state.error != null) {
                Text(state.error, color = MaterialTheme.colorScheme.error)
            }
        }
    } else {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Sesión: ${state.email ?: "(sin email)"}")
            TextButton(onClick = { vm.signOut() }) { Text("Salir") }
        }
    }
}
