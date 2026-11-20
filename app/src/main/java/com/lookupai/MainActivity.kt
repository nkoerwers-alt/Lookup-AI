
package com.lookupai

import android.os.Build
import android.os.Bundle
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.CustomCredential
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    companion object {
        private const val GOOGLE_WEB_CLIENT_ID =
            "221027867375-rjbep5bfv4208ercuacb2htgrvdifvkf.apps.googleusercontent.com"
    }

    private lateinit var credentialManager: CredentialManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AIClient.initialize(applicationContext)
        credentialManager = CredentialManager.create(this)

        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = 0

        setContent {
            val viewModel = remember { ChatViewModel() }

            MaterialTheme {
                ChatScreen(
                    viewModel = viewModel,
                    onGoogleSignIn = {
                        signInWithGoogle(viewModel)
                    },
                    onSignOut = {
                        lifecycleScope.launch {
                            try {
                                credentialManager.clearCredentialState(
                                    androidx.credentials.ClearCredentialStateRequest()
                                )
                            } catch (_: Exception) {
                                // Local sign-out still proceeds.
                            }
                            viewModel.signOut()
                        }
                    }
                )
            }
        }
    }

    private suspend fun signInWithGoogle(viewModel: ChatViewModel) {
        val googleOption = GetGoogleIdOption.Builder()
            .setServerClientId(GOOGLE_WEB_CLIENT_ID)
            .setFilterByAuthorizedAccounts(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleOption)
            .build()

        val result = credentialManager.getCredential(
            context = this,
            request = request
        )

        val credential = result.credential

        if (
            credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleCredential =
                GoogleIdTokenCredential.createFrom(credential.data)

            AIClient.signInWithGoogle(googleCredential.idToken)
            viewModel.onGoogleSignedIn()
        } else {
            throw Exception("Google did not return a valid ID token.")
        }
    }
}
