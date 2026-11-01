package com.example.sheshield.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.sheshield.data.supabase.SupabaseAuthManager
import com.example.sheshield.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(nav: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    val loggedIn = remember { mutableStateOf(SupabaseAuthManager.isLoggedIn(context)) }
    val userEmail = remember { mutableStateOf(SupabaseAuthManager.getUserId(context) ?: "Authenticated User") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Supabase Cloud Account", fontFamily = FontFamily.Serif, color = SheBrown) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = SheBrown)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SheCream)
            )
        },
        containerColor = SheCream
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (loggedIn.value) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("You are connected to Supabase", fontWeight = FontWeight.Bold, color = SheBrown, fontSize = 18.sp)
                        Text("User ID: ${userEmail.value}", color = SheBrownSoft)
                        Button(
                            onClick = {
                                SupabaseAuthManager.signOut(context)
                                loggedIn.value = false
                                message = "Logged out successfully."
                                isError = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SheRed)
                        ) {
                            Text("Sign Out", color = SheCream)
                        }
                    }
                }
            } else {
                Text(
                    "Sign in or create an account to synchronize your emergency incidents and alerts securely with Supabase cloud storage.",
                    color = SheBrownSoft,
                    fontSize = 14.sp
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (isLoading) {
                    CircularProgressIndicator(color = SheAccent)
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                if (email.isBlank() || password.isBlank()) {
                                    message = "Please enter email and password."
                                    isError = true
                                } else {
                                    isLoading = true
                                    message = null
                                    scope.launch {
                                        val res = withContext(Dispatchers.IO) {
                                            SupabaseAuthManager.signIn(context, email.trim(), password)
                                        }
                                        isLoading = false
                                        res.fold(
                                            onSuccess = {
                                                message = it
                                                isError = false
                                                loggedIn.value = true
                                            },
                                            onFailure = {
                                                message = it.localizedMessage ?: "Login failed"
                                                isError = true
                                            }
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = SheAccent)
                        ) {
                            Text("Sign In", color = SheCream)
                        }

                        OutlinedButton(
                            onClick = {
                                if (email.isBlank() || password.isBlank()) {
                                    message = "Please enter email and password."
                                    isError = true
                                } else {
                                    isLoading = true
                                    message = null
                                    scope.launch {
                                        val res = withContext(Dispatchers.IO) {
                                            SupabaseAuthManager.signUp(context, email.trim(), password)
                                        }
                                        isLoading = false
                                        res.fold(
                                            onSuccess = {
                                                message = it
                                                isError = false
                                                if (SupabaseAuthManager.isLoggedIn(context)) {
                                                    loggedIn.value = true
                                                }
                                            },
                                            onFailure = {
                                                message = it.localizedMessage ?: "Sign up failed"
                                                isError = true
                                            }
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Sign Up", color = SheBrown)
                        }
                    }
                }
            }

            message?.let {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isError) SheRed.copy(alpha = 0.1f) else SheGreen.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = it,
                        color = if (isError) SheRed else SheGreen,
                        modifier = Modifier.padding(16.dp),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
