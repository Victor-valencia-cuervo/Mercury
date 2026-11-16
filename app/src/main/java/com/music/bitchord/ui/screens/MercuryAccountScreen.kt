package com.music.bitchord.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.R
import com.music.bitchord.data.cloud.CloudSync
import com.music.bitchord.data.cloud.SupabaseProvider
import com.music.bitchord.ui.theme.stableStatusBarInsets
import kotlinx.coroutines.launch

/**
 * The Mercury account: an email and a password on the app's own backend
 * (Supabase), which is what carries the device's likes, playlists and
 * settings to other devices.
 *
 * Creating the account upgrades the anonymous user this install already
 * syncs as, so nothing uploaded before the email existed has to move.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MercuryAccountScreen(
    onNavigateUp: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val email by CloudSync.mercuryEmail.collectAsStateWithLifecycle()
    var mode by remember { mutableStateOf(MercuryMode.SIGN_IN) }
    var emailInput by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<Int?>(null) }
    var info by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.mercury_account)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            com.music.bitchord.ui.icons.SfIcons.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                windowInsets = stableStatusBarInsets(),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = stringResource(R.string.mercury_account_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 20.dp),
            )

            val linked = email != null
            if (linked) {
                SettingsGroup {
                    SettingsRow(
                        title = email.orEmpty(),
                        subtitle = stringResource(R.string.mercury_signed_in_as),
                    )
                }
                Text(
                    text = stringResource(R.string.mercury_sync_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp, bottom = 20.dp),
                )
                SettingsGroup {
                    DestructiveRow(
                        label = stringResource(R.string.mercury_sign_out),
                        onClick = {
                            scope.launch {
                                busy = true
                                CloudSync.signOutMercury()
                                    .onFailure { error = R.string.mercury_error_generic }
                                busy = false
                            }
                        },
                    )
                }
            } else {
                if (!SupabaseProvider.isConfigured) {
                    Text(
                        text = stringResource(R.string.mercury_not_configured),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    return@Column
                }

                ModeTabs(
                    signIn = mode == MercuryMode.SIGN_IN,
                    onChange = {
                        mode = it
                        error = null
                        info = null
                    },
                )

                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it.trim() },
                    label = { Text(stringResource(R.string.mercury_email)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.mercury_password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
                if (mode == MercuryMode.CREATE) {
                    OutlinedTextField(
                        value = repeat,
                        onValueChange = { repeat = it },
                        label = { Text(stringResource(R.string.mercury_repeat_password)) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        isError = repeat.isNotEmpty() && repeat != password,
                        supportingText = if (repeat.isNotEmpty() && repeat != password) {
                            { Text(stringResource(R.string.mercury_passwords_differ)) }
                        } else {
                            null
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    )
                }

                error?.let {
                    Text(
                        text = stringResource(it),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 14.dp),
                    )
                }
                info?.let {
                    Text(
                        text = stringResource(it),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 14.dp),
                    )
                }

                Button(
                    onClick = {
                        if (busy) return@Button
                        error = null
                        info = null
                        val mail = emailInput.trim()
                        if (!mail.contains("@")) {
                            error = R.string.mercury_error_email
                            return@Button
                        }
                        if (password.length < 6) {
                            error = R.string.mercury_error_password
                            return@Button
                        }
                        if (mode == MercuryMode.CREATE && password != repeat) {
                            error = R.string.mercury_passwords_differ
                            return@Button
                        }
                        scope.launch {
                            busy = true
                            when (mode) {
                                MercuryMode.SIGN_IN -> CloudSync.signInMercury(mail, password)
                                    .onFailure {
                                        error = R.string.mercury_error_sign_in
                                        if ((it.message ?: "").contains("not confirmed", true)) {
                                            error = R.string.mercury_error_not_confirmed
                                        }
                                    }
                                    .onSuccess { onNavigateUp() }
                                MercuryMode.CREATE -> CloudSync.createMercuryAccount(mail, password)
                                    .onFailure {
                                        val message = it.message.orEmpty()
                                        error = when {
                                            message.contains("already registered", true) ||
                                                message.contains("user_already_exists", true) ->
                                                R.string.mercury_error_exists
                                            message.contains("confirmation", true) ->
                                                R.string.mercury_error_not_confirmed
                                            else -> R.string.mercury_error_generic
                                        }
                                    }
                                    .onSuccess { onNavigateUp() }
                            }
                            busy = false
                        }
                    },
                    enabled = !busy && emailInput.isNotBlank() && password.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                ) {
                    Text(
                        stringResource(
                            if (mode == MercuryMode.SIGN_IN) {
                                R.string.mercury_sign_in
                            } else {
                                R.string.mercury_create_account
                            },
                        ),
                    )
                }

                if (mode == MercuryMode.SIGN_IN) {
                    TextButton(
                        onClick = {
                            val mail = emailInput.trim()
                            if (!mail.contains("@")) {
                                error = R.string.mercury_error_email
                                return@TextButton
                            }
                            scope.launch {
                                busy = true
                                error = null
                                CloudSync.resetMercuryPassword(mail)
                                    .onSuccess { info = R.string.mercury_reset_sent }
                                    .onFailure { error = R.string.mercury_error_generic }
                                busy = false
                            }
                        },
                        modifier = Modifier.padding(top = 6.dp),
                    ) {
                        Text(stringResource(R.string.mercury_forgot_password))
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

private enum class MercuryMode { SIGN_IN, CREATE }

@Composable
private fun ModeTabs(
    signIn: Boolean,
    onChange: (MercuryMode) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (signIn) {
            Button(
                onClick = { onChange(MercuryMode.SIGN_IN) },
                shape = RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.mercury_sign_in))
            }
        } else {
            OutlinedButton(
                onClick = { onChange(MercuryMode.SIGN_IN) },
                shape = RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.mercury_sign_in))
            }
        }
        if (signIn) {
            OutlinedButton(
                onClick = { onChange(MercuryMode.CREATE) },
                shape = RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.mercury_create_account))
            }
        } else {
            Button(
                onClick = { onChange(MercuryMode.CREATE) },
                shape = RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.mercury_create_account))
            }
        }
    }
    Text(
        text = stringResource(
            if (signIn) R.string.mercury_sign_in_hint else R.string.mercury_create_hint,
        ),
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
    )
}
