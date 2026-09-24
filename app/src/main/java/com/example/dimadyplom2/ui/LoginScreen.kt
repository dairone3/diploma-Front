package com.example.dimadyplom2.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.dimadyplom2.api.ApiFactory
import com.example.dimadyplom2.api.AuthException
import com.example.dimadyplom2.services.UserService
import kotlinx.coroutines.launch

/**
 * Login/registration form. Shows login/password fields with Sign Up / Sign In (both go
 * through [UserService]). On success the session is stored, which [AuthGate] observes to
 * swap this screen out for the app.
 */
@Composable
fun LoginScreen(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val userService = remember { UserService(ApiFactory.createAuthApi()) }

    var loginTxt by remember { mutableStateOf("") }
    var passwordTxt by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    fun onSignUpClick() {
        scope.launch {
            loading = true
            error = null
            try {
                userService.signUp(loginTxt, passwordTxt)
            } catch (e: AuthException) {
                error = e.message
            } catch (e: Exception) {
                error = "Something went wrong. Please try again."
            } finally {
                loading = false
            }
        }
    }

    fun onSignInClick() {
        scope.launch {
            loading = true
            error = null
            try {
                userService.signIn(loginTxt, passwordTxt)
            } catch (e: AuthException) {
                error = e.message
            } catch (e: Exception) {
                error = "Something went wrong. Please try again."
            } finally {
                loading = false
            }
        }
    }

    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = loginTxt,
            onValueChange = { loginTxt = it },
            placeholder = { Text("Login") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = passwordTxt,
            onValueChange = { passwordTxt = it },
            placeholder = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { onSignUpClick() },
                enabled = !loading && loginTxt.isNotBlank() && passwordTxt.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) { Text("Sign Up") }

            Button(
                onClick = { onSignInClick() },
                enabled = !loading && loginTxt.isNotBlank() && passwordTxt.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) { Text("Sign In") }
        }

        if (loading) {
            Spacer(Modifier.height(16.dp))
            CircularProgressIndicator()
        }
        val currentError = error
        if (currentError != null) {
            Spacer(Modifier.height(16.dp))
            Text(currentError, color = MaterialTheme.colorScheme.error)
        }
    }
}

