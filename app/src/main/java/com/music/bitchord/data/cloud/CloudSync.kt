package com.music.bitchord.data.cloud

import android.util.Log
import com.music.bitchord.data.listentogether.ListenTogether
import com.music.bitchord.data.model.Song
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Keeps a copy of the person's profile and listening history in Supabase.
 *
 * Offline-first: the phone's own storage is always the source of truth and every
 * call here is fire-and-forget. A missing network, a missing table or a policy
 * that says no is logged and dropped, never shown and never thrown into the UI.
 *
 * Today this covers the profile name and the listening history in vivo, plus
 * the Mercury account's own sync — likes, local playlists and the syncable
 * settings — which runs the moment an email account is linked and at every
 * re-link after that.
 */
object CloudSync {

    private const val TAG = "MercurioCloud"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sessionLock = Mutex()

    private val _mercuryEmail = MutableStateFlow<String?>(null)

    /**
     * The email the current Supabase session is signed in with, or null while
     * this install has no Mercury account — which is the anonymous user
     * [userId] creates, a session with no email behind it.
     */
    val mercuryEmail: StateFlow<String?> = _mercuryEmail.asStateFlow()

    /** Set by [start] to the work a completed sign-in owes: see [onMercuryLinked]. */
    private var mercuryLinked = false

    /** Call once from `Application.onCreate`. Returns immediately; the work happens off the main thread. */
    fun start(context: android.content.Context) {
        if (!SupabaseProvider.isConfigured) return
        syncPrefs = context.applicationContext.getSharedPreferences(SYNC_PREF_NAME, android.content.Context.MODE_PRIVATE)
        scope.launch {
            // The Mercury account's email follows the auth session from here on:
            // a sign-up, a sign-in on another device and a sign-out all arrive
            // through the same collector, wherever in the app they were caused.
            runCatching { trackMercurySession() }
                .onFailure { Log.w(TAG, "start: ${it.message}") }
        }
        scope.launch {
            runCatching { syncProfileName() }
                .onFailure { Log.w(TAG, "start: ${it.message}") }
        }
    }

    /** Publishes [mercuryEmail] from the session, now and at every change. */
    private suspend fun trackMercurySession() {
        val auth = SupabaseProvider.client.auth
        auth.awaitInitialization()
        publishMercuryEmail(auth)
        auth.sessionStatus.collect { status ->
            publishMercuryEmail(auth, status)
        }
    }

    private suspend fun publishMercuryEmail(
        auth: io.github.jan.supabase.auth.Auth,
        status: SessionStatus = auth.sessionStatus.value,
    ) {
        val email = (status as? SessionStatus.Authenticated)?.session?.user?.email?.takeIf { it.isNotBlank() }
        val previous = _mercuryEmail.value
        _mercuryEmail.value = email
        if (email != null && previous != email) onMercuryLinked()
    }

    /**
     * Everything that is owed the moment a Mercury account is in hand: the
     * device's own data goes up, the account's data comes down, and from then
     * on the two move together. Runs off the main thread and swallows its own
     * failures — a sync that can't reach the network is retried by the next
     * change, never shown.
     */
    private fun onMercuryLinked() {
        if (mercuryLinked) return
        mercuryLinked = true
        scope.launch {
            runCatching { syncMercuryData() }
                .onFailure { Log.w(TAG, "onMercuryLinked: ${it.message}") }
        }
    }

    /**
     * Creates the Mercury account from this install's anonymous user, by giving
     * it an email and a password rather than making a new one — the history and
     * profile already uploaded under this id stay its own.
     *
     * An install whose anonymous user is gone (a prior sign-out) signs up
     * fresh, which is what a first-time account is anyway.
     */
    suspend fun createMercuryAccount(email: String, password: String): Result<Unit> = runCatching {
        val auth = SupabaseProvider.client.auth
        auth.awaitInitialization()
        val current = auth.currentUserOrNull()
        if (current != null && current.isAnonymous == true) {
            auth.updateUser(updateCurrentUser = true) {
                this.email = email
                this.password = password
            }
        } else {
            auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
        }
    }

    /** Signs in with the Mercury account's email, replacing whatever session was here. */
    suspend fun signInMercury(email: String, password: String): Result<Unit> = runCatching {
        val auth = SupabaseProvider.client.auth
        auth.awaitInitialization()
        auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    /** Signs out of the Mercury account. The next sync goes back to being anonymous. */
    suspend fun signOutMercury(): Result<Unit> = runCatching {
        mercuryLinked = false
        SupabaseProvider.client.auth.signOut()
    }

    /** Emails the account holder the way back in when the password is gone. */
    suspend fun resetMercuryPassword(email: String): Result<Unit> = runCatching {
        val auth = SupabaseProvider.client.auth
        auth.awaitInitialization()
        auth.resetPasswordForEmail(email)
    }

    /** Mirrors a name the person just typed. */
    fun saveName(name: String) {
        val clean = name.trim()
        if (clean.isEmpty() || !SupabaseProvider.isConfigured) return
        scope.launch {
            runCatching {
                val uid = userId() ?: return@launch
                writeName(uid, clean)
            }.onFailure { Log.w(TAG, "saveName: ${it.message}") }
        }
    }

    /** Appends one counted listen. Called once per play by the stats recorder. */
    fun recordPlay(song: Song) {
        if (!SupabaseProvider.isConfigured || song.videoId.isBlank()) return
        scope.launch {
            runCatching {
                val uid = userId() ?: return@launch
                SupabaseProvider.client.from("listening_history").insert(
                    HistoryWrite(
                        userId = uid,
                        videoId = song.videoId,
                        title = song.title,
                        artist = song.artist,
                        thumbnailUrl = song.thumbnailUrl,
                    ),
                )
            }.onFailure { Log.w(TAG, "recordPlay: ${it.message}") }
        }
    }

    // ---- internals -------------------------------------------------------------

    /**
     * This install's user id, signing in anonymously the first time. The lock and
     * the wait for the stored session matter: reading the session before it has
     * loaded would look like "signed out" and create a second user on every launch.
     */
    private suspend fun userId(): String? = sessionLock.withLock {
        val auth = SupabaseProvider.client.auth
        auth.awaitInitialization()
        auth.currentUserOrNull()?.id ?: run {
            auth.signInAnonymously()
            auth.currentUserOrNull()?.id
        }
    }

    /** Local name wins and is pushed up; with no local name, a cloud one is restored. */
    private suspend fun syncProfileName() {
        val uid = userId() ?: return
        val local = ListenTogether.nickname().trim()
        if (local.isNotEmpty()) {
            writeName(uid, local)
            return
        }
        val remote = SupabaseProvider.client.from("profiles")
            .select(Columns.list("id", "display_name")) { filter { eq("id", uid) } }
            .decodeSingleOrNull<ProfileRow>()
            ?.displayName
            ?.trim()
            .orEmpty()
        if (remote.isNotEmpty()) ListenTogether.setNickname(remote)
    }

    private suspend fun writeName(uid: String, name: String) {
        SupabaseProvider.client.from("profiles").upsert(ProfileNameWrite(id = uid, displayName = name))
    }

    // ---- The Mercury account's own sync ----------------------------------------

    private const val SYNC_PREF_NAME = "bitchord_cloud_sync"
    private const val KEY_SETTINGS_SYNCED_AT = "settings_synced_at"

    private var syncPrefs: android.content.SharedPreferences? = null

    private fun settingsMarker(): String? =
        syncPrefs?.getString(KEY_SETTINGS_SYNCED_AT, null)

    /**
     * The whole Mercury sync: the device's likes, playlists and settings go up,
     * the account's come down, and neither side is ever the only one that
     * matters. Local wins where both know a thing; the cloud is what a second
     * device reads.
     *
     * Everything here is idempotent — every write is an upsert keyed by what
     * the row actually is — so a failed sync and a retried one leave the same
     * rows behind.
     */
    suspend fun syncMercuryData() {
        if (!SupabaseProvider.isConfigured) return
        val uid = userId() ?: return
        // One failing table never takes the others down with it.
        runCatching { syncFavorites(uid) }.onFailure { Log.w(TAG, "syncFavorites: ${it.message}") }
        runCatching { syncPlaylists(uid) }.onFailure { Log.w(TAG, "syncPlaylists: ${it.message}") }
        runCatching { syncSettings(uid) }.onFailure { Log.w(TAG, "syncSettings: ${it.message}") }
    }

    private suspend fun syncFavorites(uid: String) {
        val client = SupabaseProvider.client
        val local = com.music.bitchord.data.LocalLikes.songs.value
        if (local.isNotEmpty()) {
            client.from("favorites").upsert(
                local.map {
                    FavoriteWrite(
                        userId = uid,
                        videoId = it.videoId,
                        title = it.title,
                        artist = it.artist,
                        thumbnailUrl = it.thumbnailUrl,
                    )
                },
                onConflict = "user_id,video_id",
            )
            local.forEach { com.music.bitchord.data.LocalLikes.markPushed(it.videoId) }
        }
        val remote = runCatching {
            client.from("favorites").select().decodeList<FavoriteRow>()
        }.getOrDefault(emptyList())
        val known = com.music.bitchord.data.LocalLikes.ids.value
        remote
            .filter { it.videoId.isNotBlank() && it.videoId !in known }
            .forEach {
                com.music.bitchord.data.LocalLikes.restore(
                    Song(videoId = it.videoId, title = it.title, artist = it.artist.orEmpty(), thumbnailUrl = it.thumbnailUrl),
                )
            }
    }

    private suspend fun syncPlaylists(uid: String) {
        val client = SupabaseProvider.client
        val local = com.music.bitchord.data.spotify.LocalPlaylistStore.playlists.value
        local.forEach { playlist ->
            val row = client.from("playlists")
                .upsert(PlaylistRow(userId = uid, externalId = playlist.id, name = playlist.title), onConflict = "user_id,external_id") {
                    select()
                }
                .decodeSingleOrNull<PlaylistRow>() ?: return@forEach
            val remoteId = row.id ?: return@forEach
            if (playlist.songs.isNotEmpty()) {
                client.from("playlist_tracks").upsert(
                    playlist.songs.mapIndexed { index, song ->
                        PlaylistTrackWrite(
                            playlistId = remoteId,
                            position = index,
                            videoId = song.videoId,
                            title = song.title,
                            artist = song.artist,
                            thumbnailUrl = song.thumbnailUrl,
                        )
                    },
                    onConflict = "playlist_id,video_id",
                )
            }
        }
        val remote = runCatching {
            client.from("playlists").select().decodeList<PlaylistRow>()
        }.getOrDefault(emptyList())
        val localIds = local.map { it.id }.toSet()
        remote.filter { it.externalId !in localIds }.forEach { row ->
            val id = row.id ?: return@forEach
            val tracks = runCatching {
                client.from("playlist_tracks")
                    .select { filter { eq("playlist_id", id) }; order("position", io.github.jan.supabase.postgrest.query.Order.ASCENDING) }
                    .decodeList<PlaylistTrackRow>()
            }.getOrDefault(emptyList())
            com.music.bitchord.data.spotify.LocalPlaylistStore.savePlaylist(
                title = row.name,
                songs = tracks.map {
                    Song(
                        videoId = it.videoId,
                        title = it.title,
                        artist = it.artist.orEmpty(),
                        thumbnailUrl = it.thumbnailUrl,
                    )
                },
                id = row.externalId,
            )
        }
    }

    private suspend fun syncSettings(uid: String) {
        val client = SupabaseProvider.client
        val remote = runCatching {
            client.from("settings").select().decodeSingleOrNull<SettingsRow>()
        }.getOrNull()
        val marker = settingsMarker()
        if (marker == null) {
            // First link on this device: an account that already has settings
            // is restored from them; a fresh account takes what the device has.
            val data = remote?.data
            if (data != null) {
                runCatching {
                    com.music.bitchord.data.settings.AppSettings.importSyncedSettings(
                        org.json.JSONObject(data.toString()),
                    )
                }
                remote.updatedAt?.let { setSettingsMarker(it) } ?: setSettingsMarker(null)
            } else {
                pushSettings(uid)
                setSettingsMarker(null)
            }
            return
        }
        // Otherwise the newer side wins: a snapshot written since this device
        // last synced comes down, anything else goes up as the current one.
        if (remote != null && remote.data != null && isAfter(remote.updatedAt, marker)) {
            runCatching {
                com.music.bitchord.data.settings.AppSettings.importSyncedSettings(
                    org.json.JSONObject(remote.data.toString()),
                )
            }
            setSettingsMarker(remote.updatedAt)
        } else {
            pushSettings(uid)
            setSettingsMarker(null)
        }
    }

    /** ISO timestamps, read defensively: an unreadable remote stamp never beats the local marker. */
    private fun isAfter(remote: String?, local: String): Boolean {
        val remoteInstant = remote?.let {
            runCatching { java.time.Instant.parse(it.replace("+00:00", "Z")) }.getOrNull()
        }
        val localInstant = runCatching { java.time.Instant.parse(local) }.getOrNull()
        return remoteInstant != null && localInstant != null && remoteInstant.isAfter(localInstant)
    }

    /** Pushes the device's syncable settings as one snapshot, replacing the remote row. */
    private suspend fun pushSettings(uid: String) {
        val json = runCatching {
            kotlinx.serialization.json.Json.parseToJsonElement(
                com.music.bitchord.data.settings.AppSettings.exportSyncedSettings().toString(),
            )
        }.getOrNull() as? kotlinx.serialization.json.JsonObject ?: return
        SupabaseProvider.client.from("settings").upsert(
            SettingsWrite(userId = uid, data = json),
            onConflict = "user_id",
        )
    }

    private fun setSettingsMarker(at: String? = java.time.Instant.now().toString()) {
        val value = at ?: java.time.Instant.now().toString()
        syncPrefs?.edit()?.putString(KEY_SETTINGS_SYNCED_AT, value)?.apply()
    }
}
