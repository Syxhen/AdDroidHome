package com.syxhen.addroidhome.ui.connection

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.syxhen.addroidhome.ui.ServerConnectionUiState
import com.syxhen.addroidhome.ui.components.AppLogoBadge
import com.syxhen.addroidhome.ui.icons.visibility
import com.syxhen.addroidhome.ui.icons.visibility_off
import com.syxhen.addroidhome.ui.theme.AdDroidHomeTheme

@Composable
fun ServerConnectionScreen(
    state: ServerConnectionUiState,
    onServerUrlChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTrustSelfSignedCertsChange: (Boolean) -> Unit,
    onConnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 460.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                AppLogoBadge(
                    size = 84.dp,
                    shape = CircleShape,
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "AdDroid Home",
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = "Connect to your AdGuard Home server to monitor DNS stats and control protection.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        OutlinedTextField(
                            value = state.serverUrl,
                            onValueChange = onServerUrlChange,
                            label = { Text("Server URL") },
                            placeholder = { Text("https://adguard.home or 192.168.1.2:3000") },
                            singleLine = true,
                            enabled = !state.isConnecting,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Next,
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) },
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        OutlinedTextField(
                            value = state.username,
                            onValueChange = onUsernameChange,
                            label = { Text("Username") },
                            placeholder = { Text("admin") },
                            singleLine = true,
                            enabled = !state.isConnecting,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next,
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) },
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        OutlinedTextField(
                            value = state.password,
                            onValueChange = onPasswordChange,
                            label = { Text("Password") },
                            singleLine = true,
                            enabled = !state.isConnecting,
                            visualTransformation = if (passwordVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) visibility_off else visibility,
                                        contentDescription = if (passwordVisible) {
                                            "Hide password"
                                        } else {
                                            "Show password"
                                        },
                                    )
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    if (state.canConnect) onConnect()
                                },
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Trust self-signed HTTPS certificates",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Text(
                                    text = "For homelab servers using a self-signed TLS cert. Plain HTTP is allowed for LAN addresses.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = state.trustSelfSignedCerts,
                                onCheckedChange = onTrustSelfSignedCertsChange,
                                enabled = !state.isConnecting,
                            )
                        }

                        AnimatedVisibility(visible = state.errorMessage != null) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = state.errorMessage.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(12.dp),
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                onConnect()
                            },
                            enabled = state.canConnect,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .widthIn(max = 320.dp)
                                .fillMaxWidth()
                                .heightIn(min = 50.dp),
                        ) {
                            if (state.isConnecting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                            } else {
                                Text("Connect")
                            }
                        }
                    }
                }

                Text(
                    text = "Credentials are encrypted on-device with Android Keystore.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Preview(name = "Connection Light", showBackground = true)
@Composable
private fun ServerConnectionScreenPreview() {
    AdDroidHomeTheme(dynamicColor = false) {
        ServerConnectionScreen(
            state = ServerConnectionUiState(
                serverUrl = "https://adguard.home.local",
                username = "admin",
            ),
            onServerUrlChange = {},
            onUsernameChange = {},
            onPasswordChange = {},
            onTrustSelfSignedCertsChange = {},
            onConnect = {},
        )
    }
}

@Preview(name = "Connection Dark", showBackground = true, widthDp = 412, heightDp = 915)
@Composable
private fun ServerConnectionDarkPreview() {
    AdDroidHomeTheme(darkTheme = true, dynamicColor = false) {
        ServerConnectionScreen(
            state = ServerConnectionUiState(
                serverUrl = "http://192.168.1.2:3000",
                username = "admin",
                password = "demo-password",
                trustSelfSignedCerts = false,
            ),
            onServerUrlChange = {},
            onUsernameChange = {},
            onPasswordChange = {},
            onTrustSelfSignedCertsChange = {},
            onConnect = {},
        )
    }
}

@Preview(name = "Connection Error Dark", showBackground = true)
@Composable
private fun ServerConnectionErrorDarkPreview() {
    AdDroidHomeTheme(darkTheme = true, dynamicColor = false) {
        ServerConnectionScreen(
            state = ServerConnectionUiState(
                serverUrl = "http://192.168.1.2:3000",
                username = "admin",
                password = "secret",
                trustSelfSignedCerts = true,
                errorMessage = "Authentication failed. Check your username and password.",
            ),
            onServerUrlChange = {},
            onUsernameChange = {},
            onPasswordChange = {},
            onTrustSelfSignedCertsChange = {},
            onConnect = {},
        )
    }
}
