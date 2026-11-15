package com.music.bitchord.data.cloud

import android.util.Log
import com.music.bitchord.data.listentogether.ListenTogether
import com.music.bitchord.data.model.Song
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
 * Today this covers the profile name and the listening history. The heart in
 * this app is the YouTube Music account's like, which needs a Google sign-in, so
 * favourites are deliberately not mirrored here yet (the `favorites` table
 * exists in the schema for when Mercurio has its own).
 */
object CloudSync {

    private const val TAG = "MercurioCloud"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sessionLock = Mutex()

    /** Call once from `Application.onCreate`. Returns immediately; the work happens off the main thread. */
    fun start() {
        if (!SupabaseProvider.isConfigured) return
        scope.launch {
            runCatching { syncProfileName() }
                .onFailure { Log.w(TAG, "start: ${it.message}") }
        }
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
}
